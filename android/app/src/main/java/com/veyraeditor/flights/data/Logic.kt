package com.veyraeditor.flights.data

import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Currency
import java.util.Locale

/* Pure helpers for showing flights: sorting, filters, prices, times. Tested in app/src/test. */

enum class Sort(val label: String) { Best("Best"), Cheapest("Cheapest"), Fastest("Fastest"), Earliest("Earliest") }

fun Offer.totalMinutes() = slices.sumOf { it.durationMinutes }
fun Offer.maxStops() = slices.maxOfOrNull { it.stops } ?: 0

/** Cheapest, fastest, earliest, or "best": a mix of price and time where an hour is worth about 8% of the cheapest fare. */
fun sortOffers(list: List<Offer>, sort: Sort): List<Offer> = when (sort) {
    Sort.Cheapest -> list.sortedWith(compareBy({ it.price.amount }, { it.totalMinutes() }))
    Sort.Fastest -> list.sortedWith(compareBy({ it.totalMinutes() }, { it.price.amount }))
    Sort.Earliest -> list.sortedWith(compareBy({ it.slices.firstOrNull()?.depart ?: "" }, { it.price.amount }))
    Sort.Best -> {
        val cheapest = list.minOfOrNull { it.price.amount }?.takeIf { it > 0 } ?: 1.0
        list.sortedBy { it.price.amount + it.totalMinutes() / 60.0 * cheapest * 0.08 + it.maxStops() * cheapest * 0.05 }
    }
}

data class Filters(val nonstopOnly: Boolean = false, val airlines: Set<String> = emptySet(), val departBefore: Int? = null)

fun filterOffers(list: List<Offer>, f: Filters): List<Offer> = list.filter { o ->
    (!f.nonstopOnly || o.maxStops() == 0) &&
        (f.airlines.isEmpty() || o.airline.code in f.airlines) &&
        (f.departBefore == null || (o.slices.firstOrNull()?.let { hourOf(it.depart) } ?: 0) < f.departBefore)
}

/** Airlines in the results, with how many flights each has, most first. */
fun airlinesIn(list: List<Offer>): List<Pair<Airline, Int>> =
    list.groupBy { it.airline.code }.map { (_, offers) -> offers.first().airline to offers.size }.sortedByDescending { it.second }

/** 155 → "2h 35m", 45 → "45m". */
fun formatDuration(minutes: Int): String {
    val h = minutes / 60; val m = minutes % 60
    return when { h == 0 -> "${m}m"; m == 0 -> "${h}h"; else -> "${h}h ${m}m" }
}

/** ₹5,432 / $129.50 / €89, with no ".00" on whole amounts. */
fun formatPrice(price: Money, locale: Locale = Locale.getDefault()): String {
    val f = NumberFormat.getCurrencyInstance(locale)
    runCatching { f.currency = Currency.getInstance(price.currency) }
    val whole = price.amount % 1.0 == 0.0
    f.maximumFractionDigits = if (whole) 0 else 2
    f.minimumFractionDigits = if (whole) 0 else 2
    return f.format(price.amount)
}

private fun parse(iso: String): LocalDateTime? = runCatching { LocalDateTime.parse(iso.take(19)) }.getOrNull()
fun timeOf(iso: String): String = parse(iso)?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: ""
fun hourOf(iso: String): Int = parse(iso)?.hour ?: 0
/** "+1" when a flight lands the next day. */
fun dayShift(depart: String, arrive: String): String {
    val a = parse(depart)?.toLocalDate(); val b = parse(arrive)?.toLocalDate()
    val days = if (a != null && b != null) ChronoUnit.DAYS.between(a, b) else 0
    return if (days > 0) "+$days" else ""
}
fun dateLabel(iso: String, pattern: String = "EEE, d MMM"): String =
    runCatching { LocalDate.parse(iso.take(10)).format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault())) }.getOrDefault(iso)

/** The wait between two flights. */
fun layoverMinutes(arrive: String, nextDepart: String): Int {
    val a = parse(arrive); val b = parse(nextDepart)
    return if (a != null && b != null) ChronoUnit.MINUTES.between(a, b).toInt() else 0
}

fun stopsLabel(stops: Int) = when (stops) { 0 -> "Nonstop"; 1 -> "1 stop"; else -> "$stops stops" }

fun passengersLabel(adults: Int, children: Int, infants: Int): String = buildList {
    add(if (adults == 1) "1 adult" else "$adults adults")
    if (children > 0) add(if (children == 1) "1 child" else "$children children")
    if (infants > 0) add(if (infants == 1) "1 infant" else "$infants infants")
}.joinToString(", ")

// ---------- Checking a traveller's details before booking ----------

private val NAME = Regex("^[\\p{L} '\\-]{1,40}$")
private val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
private val PHONE = Regex("^\\+\\d{7,15}$")

/** Field → message for whatever's missing or wrong. The first traveller also needs an email and phone. */
fun checkPassenger(p: PassengerForm, lead: Boolean, today: LocalDate = LocalDate.now()): Map<String, String> = buildMap {
    if (p.title.isBlank()) put("title", "Choose a title")
    if (!NAME.matches(p.given.trim())) put("given", if (p.given.isBlank()) "Add the first name" else "Use letters only")
    if (!NAME.matches(p.family.trim())) put("family", if (p.family.isBlank()) "Add the last name" else "Use letters only")
    val born = runCatching { LocalDate.parse(p.born) }.getOrNull()
    if (born == null) put("born", "Add the date of birth") else if (born.isAfter(today)) put("born", "That's in the future")
    if (p.gender.isBlank()) put("gender", "Choose as on the passport")
    if (lead || p.email.isNotBlank()) if (!EMAIL.matches(p.email.trim())) put("email", "Add a valid email")
    if (lead || p.phone.isNotBlank()) if (!PHONE.matches(p.phone.replace(" ", "").replace("-", ""))) put("phone", "Include the country code, like +91")
}
