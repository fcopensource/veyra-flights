package com.veyraeditor.flights

import com.veyraeditor.flights.data.Airline
import com.veyraeditor.flights.data.Airport
import com.veyraeditor.flights.data.Filters
import com.veyraeditor.flights.data.Money
import com.veyraeditor.flights.data.Offer
import com.veyraeditor.flights.data.PassengerForm
import com.veyraeditor.flights.data.Segment
import com.veyraeditor.flights.data.Slice
import com.veyraeditor.flights.data.Sort
import com.veyraeditor.flights.data.airlinesIn
import com.veyraeditor.flights.data.checkPassenger
import com.veyraeditor.flights.data.dayShift
import com.veyraeditor.flights.data.filterOffers
import com.veyraeditor.flights.data.formatDuration
import com.veyraeditor.flights.data.formatPrice
import com.veyraeditor.flights.data.layoverMinutes
import com.veyraeditor.flights.data.passengersLabel
import com.veyraeditor.flights.data.sortOffers
import com.veyraeditor.flights.data.stopsLabel
import com.veyraeditor.flights.data.timeOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class LogicTest {
    private fun offer(id: String, price: Double, minutes: Int, stops: Int = 0, airline: String = "6E", depart: String = "2026-12-20T09:00:00") = Offer(
        id = id, price = Money(price, "INR"), airline = Airline("Airline $airline", airline),
        slices = listOf(Slice(Airport("DEL"), Airport("GOI"), depart, "2026-12-20T12:00:00", minutes, stops,
            List(stops + 1) { Segment("$airline$it", "Airline", airline, Airport("DEL"), Airport("GOI"), depart, "2026-12-20T12:00:00") })),
    )
    private val list = listOf(
        offer("cheap-slow", 4000.0, 600, stops = 1, airline = "SG", depart = "2026-12-20T05:30:00"),
        offer("mid", 5200.0, 150, airline = "6E", depart = "2026-12-20T14:00:00"),
        offer("fast-pricey", 9000.0, 140, airline = "AI", depart = "2026-12-20T08:00:00"),
    )

    @Test fun sorting() {
        assertEquals(listOf("cheap-slow", "mid", "fast-pricey"), sortOffers(list, Sort.Cheapest).map { it.id })
        assertEquals(listOf("fast-pricey", "mid", "cheap-slow"), sortOffers(list, Sort.Fastest).map { it.id })
        assertEquals(listOf("cheap-slow", "fast-pricey", "mid"), sortOffers(list, Sort.Earliest).map { it.id })
        assertEquals("a nonstop at a fair price beats a cheap 10-hour trip with a stop", "mid", sortOffers(list, Sort.Best).first().id)
    }

    @Test fun filters() {
        assertEquals(listOf("mid", "fast-pricey"), filterOffers(list, Filters(nonstopOnly = true)).map { it.id })
        assertEquals(listOf("fast-pricey"), filterOffers(list, Filters(airlines = setOf("AI"))).map { it.id })
        assertEquals(listOf("cheap-slow", "fast-pricey"), filterOffers(list, Filters(departBefore = 12)).map { it.id })
        assertEquals(3, airlinesIn(list).size)
    }

    @Test fun formatting() {
        assertEquals("2h 35m", formatDuration(155))
        assertEquals("45m", formatDuration(45))
        assertEquals("3h", formatDuration(180))
        assertEquals("₹5,432", formatPrice(Money(5432.0, "INR"), Locale("en", "IN")))
        assertEquals("$129.50", formatPrice(Money(129.5, "USD"), Locale.US))
        assertEquals("06:05", timeOf("2026-12-20T06:05:00"))
        assertEquals("+1", dayShift("2026-12-20T22:00:00", "2026-12-21T06:00:00"))
        assertEquals("", dayShift("2026-12-20T06:00:00", "2026-12-20T09:00:00"))
        assertEquals(95, layoverMinutes("2026-12-20T08:05:00", "2026-12-20T09:40:00"))
        assertEquals("Nonstop", stopsLabel(0)); assertEquals("2 stops", stopsLabel(2))
        assertEquals("2 adults, 1 child", passengersLabel(2, 1, 0))
    }

    @Test fun passengerChecks() {
        val today = LocalDate.parse("2026-10-24")
        val good = PassengerForm("ms", "Anika", "Rao", "1994-05-02", "f", "anika@example.com", "+91 98765 43210")
        assertTrue(checkPassenger(good, lead = true, today = today).isEmpty())
        assertTrue("others don't need contact details", checkPassenger(good.copy(email = "", phone = ""), lead = false, today = today).isEmpty())
        val bad = checkPassenger(PassengerForm(given = "R2D2", born = "2030-01-01", email = "nope", phone = "98765"), lead = true, today = today)
        assertEquals(setOf("title", "given", "family", "born", "gender", "email", "phone"), bad.keys)
        assertEquals("Use letters only", bad["given"])
        assertEquals("That's in the future", bad["born"])
    }
}
