package com.veyraeditor.flights.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.veyraeditor.flights.data.Api
import com.veyraeditor.flights.data.Booking
import com.veyraeditor.flights.data.Cabin
import com.veyraeditor.flights.data.Filters
import com.veyraeditor.flights.data.Offer
import com.veyraeditor.flights.data.PassengerForm
import com.veyraeditor.flights.data.Place
import com.veyraeditor.flights.data.SearchQuery
import com.veyraeditor.flights.data.Sort
import com.veyraeditor.flights.data.TripsStore
import com.veyraeditor.flights.data.checkPassenger
import com.veyraeditor.flights.data.filterOffers
import com.veyraeditor.flights.data.sortOffers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class SearchForm(
    val from: Place? = null,
    val to: Place? = null,
    val roundTrip: Boolean = false,
    val depart: LocalDate = LocalDate.now().plusDays(7),
    val returnDate: LocalDate = LocalDate.now().plusDays(14),
    val adults: Int = 1,
    val children: Int = 0,
    val infants: Int = 0,
    val cabin: Cabin = Cabin.Economy,
) {
    fun toQuery() = SearchQuery(from!!, to!!, depart.toString(), if (roundTrip) returnDate.toString() else null, adults, children, infants, cabin.id)
}

sealed interface Results {
    data object Idle : Results
    data object Loading : Results
    data class Error(val message: String) : Results
    data class Ready(val mode: String, val offers: List<Offer>) : Results
}

sealed interface BookingState {
    data object Idle : BookingState
    data object Sending : BookingState
    data class Error(val message: String) : BookingState
    data class Done(val booking: Booking) : BookingState
}

class AppViewModel(private val api: Api, private val store: TripsStore) : ViewModel() {
    private val _form = MutableStateFlow(SearchForm())
    val form = _form.asStateFlow()
    private val _results = MutableStateFlow<Results>(Results.Idle)
    val results = _results.asStateFlow()
    private val _sort = MutableStateFlow(Sort.Best)
    val sort = _sort.asStateFlow()
    private val _filters = MutableStateFlow(Filters())
    val filters = _filters.asStateFlow()
    private val _selected = MutableStateFlow<Offer?>(null)
    val selected = _selected.asStateFlow()
    private val _priceChanged = MutableStateFlow(false)
    val priceChanged = _priceChanged.asStateFlow()
    private val _passengers = MutableStateFlow<List<PassengerForm>>(emptyList())
    val passengers = _passengers.asStateFlow()
    private val _errors = MutableStateFlow<List<Map<String, String>>>(emptyList())
    val errors = _errors.asStateFlow()
    private val _booking = MutableStateFlow<BookingState>(BookingState.Idle)
    val booking = _booking.asStateFlow()
    /** "demo", "test" or "live", from the server; null until it answers. */
    private val _mode = MutableStateFlow<String?>(null)
    val mode = _mode.asStateFlow()

    val trips: StateFlow<List<Booking>> = store.trips.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val recent: StateFlow<List<SearchQuery>> = store.recent.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch { runCatching { api.health() }.onSuccess { _mode.value = it.mode } }
    }

    // ---------- Search form ----------

    fun update(change: (SearchForm) -> SearchForm) = _form.update { f ->
        val next = change(f)
        // Keep the return after the departure.
        if (next.returnDate.isBefore(next.depart)) next.copy(returnDate = next.depart.plusDays(1)) else next
    }
    fun swap() = _form.update { it.copy(from = it.to, to = it.from) }
    fun useRecent(q: SearchQuery) = _form.update {
        it.copy(from = q.from, to = q.to, adults = q.adults, children = q.children, infants = q.infants,
            cabin = Cabin.entries.firstOrNull { c -> c.id == q.cabin } ?: Cabin.Economy)
    }
    val canSearch get() = _form.value.let { it.from != null && it.to != null && it.from.code != it.to.code }

    private var placesJob: Job? = null
    /** Airport suggestions as the person types, after a short pause. */
    fun suggest(query: String, onResult: (List<Place>, String?) -> Unit) {
        placesJob?.cancel()
        placesJob = viewModelScope.launch {
            delay(220)
            if (query.trim().length < 2) { onResult(emptyList(), null); return@launch }
            runCatching { api.places(query.trim()) }
                .onSuccess { onResult(it, null) }
                .onFailure { onResult(emptyList(), it.message) }
        }
    }

    // ---------- Results ----------

    private var searchJob: Job? = null
    fun search() {
        if (!canSearch) return
        val q = _form.value.toQuery()
        _results.value = Results.Loading
        _filters.value = Filters()
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            store.addRecent(q)
            runCatching { api.search(q) }
                .onSuccess { (mode, offers) -> _mode.value = mode; _results.value = Results.Ready(mode, offers) }
                .onFailure { _results.value = Results.Error(it.message ?: "Something went wrong.") }
        }
    }
    fun setSort(s: Sort) { _sort.value = s }
    fun setFilters(f: Filters) { _filters.value = f }
    fun shown(r: Results.Ready, s: Sort, f: Filters) = sortOffers(filterOffers(r.offers, f), s)

    // ---------- A chosen flight ----------

    fun select(offer: Offer) {
        _selected.value = offer
        _priceChanged.value = false
        _booking.value = BookingState.Idle
        _passengers.value = List(offer.passengerIds.size.coerceAtLeast(1)) { PassengerForm() }
        _errors.value = List(_passengers.value.size) { emptyMap() }
        // Fares change: check the latest price in the background.
        viewModelScope.launch {
            runCatching { api.offer(offer.id) }.onSuccess { latest ->
                if (_selected.value?.id == offer.id) {
                    _priceChanged.value = latest.price.amount != offer.price.amount
                    _selected.value = latest
                }
            }
        }
    }

    fun updatePassenger(index: Int, change: (PassengerForm) -> PassengerForm) {
        _passengers.update { list -> list.mapIndexed { i, p -> if (i == index) change(p) else p } }
        // Clear a field's message once it's been fixed.
        _errors.update { list -> list.mapIndexed { i, e -> if (i == index && e.isNotEmpty()) checkPassenger(_passengers.value[i], i == 0).filterKeys { it in e } else e } }
    }

    fun book() {
        val offer = _selected.value ?: return
        val forms = _passengers.value
        val problems = forms.mapIndexed { i, p -> checkPassenger(p, i == 0) }
        _errors.value = problems
        if (problems.any { it.isNotEmpty() }) return
        _booking.value = BookingState.Sending
        viewModelScope.launch {
            runCatching { api.book(offer.id, forms) }
                .onSuccess { b -> store.addTrip(b); _booking.value = BookingState.Done(b) }
                .onFailure { _booking.value = BookingState.Error(it.message ?: "The booking didn't go through.") }
        }
    }
    fun resetBooking() { _booking.value = BookingState.Idle }
    fun removeTrip(id: String) = viewModelScope.launch { store.removeTrip(id) }

    class Factory(private val api: Api, private val store: TripsStore) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AppViewModel(api, store) as T
    }
}
