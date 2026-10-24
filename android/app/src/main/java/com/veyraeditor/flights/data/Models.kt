package com.veyraeditor.flights.data

import kotlinx.serialization.Serializable

/* The shapes the Veyra Flights server sends (see server/src/normalize.js). */

@Serializable
data class Place(val code: String, val name: String = "", val city: String = "", val country: String = "", val type: String = "airport")

@Serializable
data class Money(val amount: Double, val currency: String)

@Serializable
data class Airport(val code: String, val name: String = "", val city: String = "")

@Serializable
data class Segment(
    val flight: String,
    val carrier: String,
    val carrierCode: String = "",
    val from: Airport,
    val to: Airport,
    val depart: String,
    val arrive: String,
    val durationMinutes: Int = 0,
    val aircraft: String = "",
)

@Serializable
data class Slice(
    val from: Airport,
    val to: Airport,
    val depart: String,
    val arrive: String,
    val durationMinutes: Int,
    val stops: Int,
    val segments: List<Segment>,
)

@Serializable
data class Airline(val name: String, val code: String, val logo: String = "")

@Serializable
data class Baggage(val checked: Int = 0, val carryOn: Int = 0)

@Serializable
data class Offer(
    val id: String,
    val price: Money,
    val airline: Airline,
    val slices: List<Slice>,
    val cabin: String = "",
    val baggage: Baggage = Baggage(),
    val refundable: Boolean = false,
    val changeable: Boolean = false,
    val expiresAt: String = "",
    val passengerIds: List<String> = emptyList(),
    val demo: Boolean = false,
)

@Serializable
data class BookedPassenger(val name: String)

@Serializable
data class Booking(
    val id: String,
    val reference: String,
    val createdAt: String = "",
    val price: Money,
    val passengers: List<BookedPassenger>,
    val offer: Offer,
    val demo: Boolean = false,
)

@Serializable
data class Health(val ok: Boolean, val mode: String, val version: String = "")

enum class Cabin(val id: String, val label: String) {
    Economy("economy", "Economy"), PremiumEconomy("premium_economy", "Premium Economy"), Business("business", "Business"), First("first", "First");
}

@Serializable
data class SearchQuery(
    val from: Place,
    val to: Place,
    val depart: String,
    val returnDate: String? = null,
    val adults: Int = 1,
    val children: Int = 0,
    val infants: Int = 0,
    val cabin: String = Cabin.Economy.id,
) {
    val passengers get() = adults + children + infants
}

/** What the person types for one traveller. */
data class PassengerForm(
    val title: String = "",
    val given: String = "",
    val family: String = "",
    val born: String = "",
    val gender: String = "",
    val email: String = "",
    val phone: String = "",
)
