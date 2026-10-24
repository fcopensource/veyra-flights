package com.veyraeditor.flights.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.AirplaneTicket
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.Luggage
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.veyraeditor.flights.data.Booking
import com.veyraeditor.flights.data.Offer
import com.veyraeditor.flights.data.PassengerForm
import com.veyraeditor.flights.data.Slice
import com.veyraeditor.flights.data.Sort
import com.veyraeditor.flights.data.airlinesIn
import com.veyraeditor.flights.data.dateLabel
import com.veyraeditor.flights.data.formatDuration
import com.veyraeditor.flights.data.formatPrice
import com.veyraeditor.flights.data.layoverMinutes
import com.veyraeditor.flights.data.passengersLabel
import com.veyraeditor.flights.data.sortOffers
import com.veyraeditor.flights.data.stopsLabel
import com.veyraeditor.flights.data.timeOf
import com.veyraeditor.flights.ui.theme.Amber
import com.veyraeditor.flights.ui.theme.HeroGradient
import com.veyraeditor.flights.ui.theme.Mint
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Bar(title: String, subtitle: String? = null, onBack: (() -> Unit)?) {
    TopAppBar(
        title = { Column { Text(title, style = MaterialTheme.typography.titleLarge); if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
        navigationIcon = { if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
    )
}

@Composable
private fun Message(icon: ImageVector, title: String, body: String, action: String? = null, onAction: () -> Unit = {}) {
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(72.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (action != null) { Spacer(Modifier.height(18.dp)); Button(onClick = onAction) { Text(action) } }
    }
}

// ---------- Results ----------

@Composable
fun ResultsScreen(vm: AppViewModel, onBack: () -> Unit, onOpen: (Offer) -> Unit) {
    val form by vm.form.collectAsStateWithLifecycle()
    val results by vm.results.collectAsStateWithLifecycle()
    val sort by vm.sort.collectAsStateWithLifecycle()
    val filters by vm.filters.collectAsStateWithLifecycle()
    val title = "${form.from?.city ?: ""} → ${form.to?.city ?: ""}"
    val subtitle = buildString {
        append(dateLabel(form.depart.toString())); if (form.roundTrip) append(" – ${dateLabel(form.returnDate.toString())}")
        append(" · ${passengersLabel(form.adults, form.children, form.infants)}")
    }
    Scaffold(topBar = { Bar(title, subtitle, onBack) }, containerColor = MaterialTheme.colorScheme.background) { pad ->
        when (val r = results) {
            Results.Idle, Results.Loading -> LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { Text("Finding the best flights…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(5) { SkeletonCard() }
            }
            is Results.Error -> Box(Modifier.padding(pad)) { Message(Icons.Outlined.WifiOff, "Couldn't load flights", r.message, "Try again") { vm.search() } }
            is Results.Ready -> {
                val shown = vm.shown(r, sort, filters)
                val cheapest = sortOffers(r.offers, Sort.Cheapest).firstOrNull()?.id
                val fastest = sortOffers(r.offers, Sort.Fastest).firstOrNull()?.id
                LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item { ModeBanner(r.mode) }
                    item {
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Sort.entries.forEach { s -> FilterChip(selected = sort == s, onClick = { vm.setSort(s) }, label = { Text(s.label) }) }
                        }
                    }
                    item {
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = filters.nonstopOnly, onClick = { vm.setFilters(filters.copy(nonstopOnly = !filters.nonstopOnly)) }, label = { Text("Nonstop") })
                            FilterChip(selected = filters.departBefore == 12, onClick = { vm.setFilters(filters.copy(departBefore = if (filters.departBefore == 12) null else 12)) }, label = { Text("Morning") })
                            airlinesIn(r.offers).forEach { (airline, count) ->
                                val on = airline.code in filters.airlines
                                FilterChip(selected = on, onClick = { vm.setFilters(filters.copy(airlines = if (on) filters.airlines - airline.code else filters.airlines + airline.code)) },
                                    label = { Text("${airline.name} · $count") })
                            }
                        }
                    }
                    item { Text("${shown.size} of ${r.offers.size} flights", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    if (shown.isEmpty()) item {
                        Box(Modifier.height(320.dp)) {
                            if (r.offers.isEmpty()) Message(Icons.Outlined.SearchOff, "No flights found", "Try other dates or nearby airports.")
                            else Message(Icons.Outlined.SearchOff, "Nothing matches the filters", "Clear a filter to see more flights.", "Clear filters") { vm.setFilters(com.veyraeditor.flights.data.Filters()) }
                        }
                    }
                    items(shown, key = { it.id }) { offer ->
                        FlightCard(offer, tag = when (offer.id) { cheapest -> "Cheapest"; fastest -> "Fastest"; else -> null }) { vm.select(offer); onOpen(offer) }
                    }
                }
            }
        }
    }
}

// ---------- Itinerary (flight details and trips) ----------

@Composable
fun Itinerary(slice: Slice, label: String) {
    Card(shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text("${dateLabel(slice.depart)} · ${formatDuration(slice.durationMinutes)} · ${stopsLabel(slice.stops)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
            slice.segments.forEachIndexed { i, seg ->
                if (i > 0) {
                    val wait = layoverMinutes(slice.segments[i - 1].arrive, seg.depart)
                    Surface(color = Amber.copy(alpha = 0.12f), shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                        Text("${formatDuration(wait)} layover in ${seg.from.city.ifEmpty { seg.from.code }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(10.dp))
                    }
                }
                Row {
                    Column(Modifier.width(56.dp)) {
                        Text(timeOf(seg.depart), style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(30.dp))
                        Text(timeOf(seg.arrive), style = MaterialTheme.typography.titleMedium)
                    }
                    Column(Modifier.padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(10.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        Box(Modifier.width(2.dp).height(44.dp).background(MaterialTheme.colorScheme.outlineVariant))
                        Box(Modifier.size(10.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                    }
                    Column(Modifier.weight(1f)) {
                        Text("${seg.from.city.ifEmpty { seg.from.code }} (${seg.from.code})", style = MaterialTheme.typography.titleSmall)
                        Text("${seg.carrier} · ${seg.flight}${if (seg.aircraft.isNotEmpty()) " · ${seg.aircraft}" else ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatDuration(seg.durationMinutes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text("${seg.to.city.ifEmpty { seg.to.code }} (${seg.to.code})", style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun Fact(icon: ImageVector, text: String, good: Boolean? = null) {
    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = when (good) { true -> Mint; false -> MaterialTheme.colorScheme.onSurfaceVariant; null -> MaterialTheme.colorScheme.primary }, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PriceBar(label: String, price: String, button: String, busy: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    Surface(shadowElevation = 12.dp, color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(price, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            }
            Button(onClick = onClick, enabled = enabled && !busy, modifier = Modifier.height(52.dp), shape = MaterialTheme.shapes.medium) {
                if (busy) { CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary); Spacer(Modifier.width(10.dp)) }
                Text(button)
            }
        }
    }
}

@Composable
fun OfferScreen(vm: AppViewModel, onBack: () -> Unit, onContinue: () -> Unit) {
    val offer = vm.selected.collectAsStateWithLifecycle().value ?: return
    val changed by vm.priceChanged.collectAsStateWithLifecycle()
    val form by vm.form.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { Bar(offer.airline.name, offer.cabin.ifEmpty { null }, onBack) },
        bottomBar = { PriceBar("Total for ${passengersLabel(form.adults, form.children, form.infants)}", formatPrice(offer.price), "Continue", onClick = onContinue) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (changed) Surface(color = Amber.copy(alpha = 0.15f), shape = MaterialTheme.shapes.medium) {
                Text("The price changed since you searched. The new total is ${formatPrice(offer.price)}.", Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
            }
            offer.slices.forEachIndexed { i, s -> Itinerary(s, if (offer.slices.size > 1) (if (i == 0) "OUTBOUND" else "RETURN") else "FLIGHT") }
            Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp)) {
                    Text("What's included", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Fact(Icons.Outlined.WorkOutline, "${offer.baggage.carryOn} cabin bag per person", true)
                    Fact(Icons.Outlined.Luggage, if (offer.baggage.checked > 0) "${offer.baggage.checked} checked bag per person" else "No checked bag (add one with the airline)", offer.baggage.checked > 0)
                    Fact(Icons.Outlined.Replay, if (offer.refundable) "Refundable before departure" else "Not refundable", offer.refundable)
                    Fact(Icons.Outlined.EventRepeat, if (offer.changeable) "Changes allowed (fees may apply)" else "No changes", offer.changeable)
                }
            }
            if (offer.demo) ModeBanner("demo")
        }
    }
}

// ---------- Passengers and booking ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassengersScreen(vm: AppViewModel, onBack: () -> Unit, onBooked: () -> Unit) {
    val offer = vm.selected.collectAsStateWithLifecycle().value ?: return
    val forms by vm.passengers.collectAsStateWithLifecycle()
    val errors by vm.errors.collectAsStateWithLifecycle()
    val booking by vm.booking.collectAsStateWithLifecycle()
    val mode by vm.mode.collectAsStateWithLifecycle()
    var birthFor by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(booking) { if (booking is BookingState.Done) onBooked() }

    Scaffold(
        topBar = { Bar("Who's flying?", "${forms.size} traveller${if (forms.size == 1) "" else "s"} · names as on passports", onBack) },
        bottomBar = { PriceBar("Total", formatPrice(offer.price), if (mode == "live") "Book" else "Book (test)", busy = booking is BookingState.Sending) { vm.book() } },
        containerColor = MaterialTheme.colorScheme.background,
    ) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ModeBanner(mode)
            (booking as? BookingState.Error)?.let {
                Surface(color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f), shape = MaterialTheme.shapes.medium) {
                    Text(it.message, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
            forms.forEachIndexed { i, p ->
                PassengerCard(i, p, errors.getOrNull(i) ?: emptyMap(), onChange = { change -> vm.updatePassenger(i, change) }, onBirth = { birthFor = i })
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    birthFor?.let { i ->
        val state = rememberDatePickerState(initialSelectedDateMillis = forms[i].born.takeIf { it.isNotEmpty() }?.let { LocalDate.parse(it).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() })
        DatePickerDialog(
            onDismissRequest = { birthFor = null },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { ms -> vm.updatePassenger(i) { it.copy(born = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate().toString()) } }
                    birthFor = null
                }) { Text("Done") }
            },
            dismissButton = { TextButton(onClick = { birthFor = null }) { Text("Cancel") } },
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun PassengerCard(index: Int, p: PassengerForm, errors: Map<String, String>, onChange: ((PassengerForm) -> PassengerForm) -> Unit, onBirth: () -> Unit) {
    Card(shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(if (index == 0) "Traveller 1 · main contact" else "Traveller ${index + 1}", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("mr" to "Mr", "ms" to "Ms", "mrs" to "Mrs", "miss" to "Miss").forEach { (id, label) ->
                    FilterChip(selected = p.title == id, onClick = { onChange { it.copy(title = id) } }, label = { Text(label) })
                }
            }
            errors["title"]?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            Field("First and middle names", p.given, errors["given"], KeyboardCapitalization.Words) { v -> onChange { it.copy(given = v) } }
            Field("Last name", p.family, errors["family"], KeyboardCapitalization.Words) { v -> onChange { it.copy(family = v) } }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                PickerField("Date of birth", p.born.takeIf { it.isNotEmpty() }?.let { dateLabel(it, "d MMM yyyy") } ?: "", "Choose", Modifier.weight(1f),
                    leading = { Icon(Icons.Outlined.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary) }, onClick = onBirth)
                Column {
                    Text("Gender", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = p.gender == "f", onClick = { onChange { it.copy(gender = "f") } }, label = { Text("F") })
                        FilterChip(selected = p.gender == "m", onClick = { onChange { it.copy(gender = "m") } }, label = { Text("M") })
                    }
                }
            }
            listOfNotNull(errors["born"], errors["gender"]).forEach { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            if (index == 0) {
                Field("Email", p.email, errors["email"], KeyboardCapitalization.None, KeyboardType.Email) { v -> onChange { it.copy(email = v) } }
                Field("Phone, with country code", p.phone, errors["phone"], KeyboardCapitalization.None, KeyboardType.Phone, placeholder = "+91 98765 43210") { v -> onChange { it.copy(phone = v) } }
            }
        }
    }
}

@Composable
private fun Field(label: String, value: String, error: String?, caps: KeyboardCapitalization, type: KeyboardType = KeyboardType.Text, placeholder: String? = null, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, isError = error != null,
        supportingText = error?.let { { Text(it) } }, placeholder = placeholder?.let { { Text(it) } },
        singleLine = true, shape = MaterialTheme.shapes.medium,
        keyboardOptions = KeyboardOptions(capitalization = caps, keyboardType = type),
        modifier = Modifier.fillMaxWidth(),
    )
}

// ---------- Booked ----------

@Composable
fun ConfirmationScreen(vm: AppViewModel, onTrips: () -> Unit, onHome: () -> Unit) {
    val booking = (vm.booking.collectAsStateWithLifecycle().value as? BookingState.Done)?.booking ?: return
    val pop = remember { Animatable(0.4f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) }
    val clipboard = LocalClipboardManager.current
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().background(HeroGradient).padding(top = 72.dp, bottom = 40.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.scale(pop.value).size(84.dp).background(Color.White, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Check, null, tint = Mint, modifier = Modifier.size(50.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text(if (booking.demo) "Demo booking made" else "You're booked!", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                Text("${booking.offer.slices.first().from.city} → ${booking.offer.slices.first().to.city}", style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.9f))
            }
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Booking reference", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(booking.reference, style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Monospace, letterSpacing = 3.sp))
                    }
                    IconButton(onClick = { clipboard.setText(AnnotatedString(booking.reference)) }) { Icon(Icons.Outlined.ContentCopy, "Copy the reference") }
                }
            }
            TripSummary(booking)
            Button(onClick = onTrips, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("See my trips") }
            OutlinedButton(onClick = onHome, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Search more flights") }
        }
    }
}

@Composable
private fun TripSummary(b: Booking) {
    Card(shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AirlineBadge(b.offer.airline.code, 34); Spacer(Modifier.width(10.dp))
                Text(b.offer.airline.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text(formatPrice(b.price), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            b.offer.slices.forEach { RouteLine(it) }
            HorizontalDivider()
            Text(b.passengers.joinToString(", ") { it.name }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ---------- My trips ----------

@Composable
fun TripsScreen(vm: AppViewModel, onOpen: (String) -> Unit, onSearch: () -> Unit) {
    val trips by vm.trips.collectAsStateWithLifecycle()
    Scaffold(topBar = { Bar("My trips", if (trips.isEmpty()) null else "${trips.size} booking${if (trips.size == 1) "" else "s"}", null) }, containerColor = MaterialTheme.colorScheme.background) { pad ->
        if (trips.isEmpty()) Box(Modifier.padding(pad)) { Message(Icons.Outlined.AirplaneTicket, "No trips yet", "Flights you book show up here, even without a connection.", "Find a flight", onSearch) }
        else LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(trips, key = { it.id }) { b ->
                Card(onClick = { onOpen(b.id) }, shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("${b.offer.slices.first().from.city} → ${b.offer.slices.first().to.city}", style = MaterialTheme.typography.titleMedium)
                                Text("${dateLabel(b.offer.slices.first().depart)} · ${b.reference}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (b.demo) Tag("Demo", Amber)
                        }
                        RouteLine(b.offer.slices.first())
                    }
                }
            }
        }
    }
}

@Composable
fun TripScreen(vm: AppViewModel, id: String, onBack: () -> Unit) {
    val trips by vm.trips.collectAsStateWithLifecycle()
    val b = trips.firstOrNull { it.id == id } ?: return
    var confirm by remember { mutableStateOf(false) }
    Scaffold(topBar = { Bar(b.reference, "${b.offer.airline.name} · ${formatPrice(b.price)}", onBack) }, containerColor = MaterialTheme.colorScheme.background) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            b.offer.slices.forEachIndexed { i, s -> Itinerary(s, if (b.offer.slices.size > 1) (if (i == 0) "OUTBOUND" else "RETURN") else "FLIGHT") }
            Text("Travellers: ${b.passengers.joinToString(", ") { it.name }}", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = { confirm = true }) { Icon(Icons.Outlined.Delete, null); Spacer(Modifier.width(6.dp)); Text("Remove from this phone") }
        }
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text("Remove this trip?") },
        text = { Text("It's only removed from this phone. The booking itself isn't cancelled.") },
        confirmButton = { TextButton(onClick = { vm.removeTrip(b.id); confirm = false; onBack() }) { Text("Remove") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Keep") } },
    )
}
