package com.veyraeditor.flights.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.store by preferencesDataStore(name = "veyra_flights")

/** Bookings and recent searches, kept on the phone so trips open without a connection. */
class TripsStore(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val tripsKey = stringPreferencesKey("trips")
    private val recentKey = stringPreferencesKey("recent_searches")

    val trips: Flow<List<Booking>> = context.store.data.map { prefs ->
        prefs[tripsKey]?.let { runCatching { json.decodeFromString<List<Booking>>(it) }.getOrNull() } ?: emptyList()
    }
    val recent: Flow<List<SearchQuery>> = context.store.data.map { prefs ->
        prefs[recentKey]?.let { runCatching { json.decodeFromString<List<SearchQuery>>(it) }.getOrNull() } ?: emptyList()
    }

    suspend fun addTrip(booking: Booking) = context.store.edit { prefs ->
        val list = prefs[tripsKey]?.let { runCatching { json.decodeFromString<List<Booking>>(it) }.getOrNull() } ?: emptyList()
        prefs[tripsKey] = json.encodeToString(listOf(booking) + list.filter { it.id != booking.id })
    }

    suspend fun removeTrip(id: String) = context.store.edit { prefs ->
        val list = prefs[tripsKey]?.let { runCatching { json.decodeFromString<List<Booking>>(it) }.getOrNull() } ?: emptyList()
        prefs[tripsKey] = json.encodeToString(list.filter { it.id != id })
    }

    /** The last five searches, newest first, without repeats of the same route. */
    suspend fun addRecent(q: SearchQuery) = context.store.edit { prefs ->
        val list = prefs[recentKey]?.let { runCatching { json.decodeFromString<List<SearchQuery>>(it) }.getOrNull() } ?: emptyList()
        prefs[recentKey] = json.encodeToString((listOf(q) + list.filter { it.from.code != q.from.code || it.to.code != q.to.code }).take(5))
    }
}
