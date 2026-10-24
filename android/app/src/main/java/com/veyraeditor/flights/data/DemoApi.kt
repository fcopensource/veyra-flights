package com.veyraeditor.flights.data

import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Dummy flights made on the phone, for testing the app with no server or Duffel key
 * (the same rules as server/src/demo.js). The app labels them as demo: they aren't real fares.
 */
class DemoApi(private val latency: Long = 600) : FlightsApi {
    private class Port(val code: String, val name: String, val city: String, val country: String, val lat: Double, val lon: Double) {
        val airport get() = Airport(code, name, city)
    }

    private val airports = listOf(
        Port("DEL", "Indira Gandhi International", "New Delhi", "IN", 28.556, 77.1), Port("BOM", "Chhatrapati Shivaji Maharaj International", "Mumbai", "IN", 19.089, 72.868),
        Port("BLR", "Kempegowda International", "Bengaluru", "IN", 13.198, 77.706), Port("MAA", "Chennai International", "Chennai", "IN", 12.99, 80.169),
        Port("HYD", "Rajiv Gandhi International", "Hyderabad", "IN", 17.24, 78.429), Port("CCU", "Netaji Subhas Chandra Bose International", "Kolkata", "IN", 22.654, 88.447),
        Port("GOI", "Dabolim", "Goa", "IN", 15.381, 73.831), Port("GOX", "Manohar International", "Goa", "IN", 15.744, 73.86), Port("COK", "Cochin International", "Kochi", "IN", 10.152, 76.402),
        Port("AMD", "Sardar Vallabhbhai Patel International", "Ahmedabad", "IN", 23.077, 72.635), Port("PNQ", "Pune", "Pune", "IN", 18.582, 73.92), Port("JAI", "Jaipur International", "Jaipur", "IN", 26.824, 75.812),
        Port("LKO", "Chaudhary Charan Singh International", "Lucknow", "IN", 26.761, 80.889), Port("IXC", "Chandigarh International", "Chandigarh", "IN", 30.673, 76.788), Port("SXR", "Srinagar International", "Srinagar", "IN", 33.987, 74.774),
        Port("ATQ", "Sri Guru Ram Dass Jee International", "Amritsar", "IN", 31.709, 74.797), Port("TRV", "Thiruvananthapuram International", "Thiruvananthapuram", "IN", 8.482, 76.92), Port("VNS", "Lal Bahadur Shastri International", "Varanasi", "IN", 25.452, 82.859),
        Port("DXB", "Dubai International", "Dubai", "AE", 25.253, 55.364), Port("AUH", "Zayed International", "Abu Dhabi", "AE", 24.433, 54.651), Port("DOH", "Hamad International", "Doha", "QA", 25.273, 51.608),
        Port("SIN", "Changi", "Singapore", "SG", 1.364, 103.991), Port("BKK", "Suvarnabhumi", "Bangkok", "TH", 13.69, 100.75), Port("KUL", "Kuala Lumpur International", "Kuala Lumpur", "MY", 2.746, 101.71),
        Port("HKG", "Hong Kong International", "Hong Kong", "HK", 22.308, 113.918), Port("NRT", "Narita International", "Tokyo", "JP", 35.772, 140.393), Port("HND", "Haneda", "Tokyo", "JP", 35.553, 139.78),
        Port("ICN", "Incheon International", "Seoul", "KR", 37.46, 126.441), Port("SYD", "Sydney Kingsford Smith", "Sydney", "AU", -33.946, 151.177), Port("MEL", "Melbourne", "Melbourne", "AU", -37.669, 144.841),
        Port("LHR", "Heathrow", "London", "GB", 51.47, -0.454), Port("LGW", "Gatwick", "London", "GB", 51.148, -0.19), Port("CDG", "Charles de Gaulle", "Paris", "FR", 49.01, 2.548),
        Port("FRA", "Frankfurt", "Frankfurt", "DE", 50.033, 8.571), Port("AMS", "Schiphol", "Amsterdam", "NL", 52.31, 4.768), Port("IST", "Istanbul", "Istanbul", "TR", 41.262, 28.742),
        Port("ZRH", "Zurich", "Zurich", "CH", 47.465, 8.549), Port("MAD", "Adolfo Suárez Madrid–Barajas", "Madrid", "ES", 40.472, -3.561), Port("FCO", "Leonardo da Vinci–Fiumicino", "Rome", "IT", 41.8, 12.239),
        Port("JFK", "John F. Kennedy International", "New York", "US", 40.641, -73.778), Port("EWR", "Newark Liberty International", "New York", "US", 40.69, -74.174), Port("SFO", "San Francisco International", "San Francisco", "US", 37.62, -122.379),
        Port("LAX", "Los Angeles International", "Los Angeles", "US", 33.942, -118.408), Port("ORD", "O'Hare International", "Chicago", "US", 41.974, -87.907), Port("YYZ", "Toronto Pearson", "Toronto", "CA", 43.677, -79.624),
        Port("JNB", "O. R. Tambo International", "Johannesburg", "ZA", -26.139, 28.246), Port("NBO", "Jomo Kenyatta International", "Nairobi", "KE", -1.319, 36.928), Port("CMB", "Bandaranaike International", "Colombo", "LK", 7.18, 79.884),
        Port("KTM", "Tribhuvan International", "Kathmandu", "NP", 27.697, 85.359), Port("DAC", "Hazrat Shahjalal International", "Dhaka", "BD", 23.843, 90.398), Port("MLE", "Velana International", "Malé", "MV", 4.192, 73.529),
    )
    private val byCode = airports.associateBy { it.code }
    private val domestic = listOf("6E" to "IndiGo", "AI" to "Air India", "IX" to "Air India Express", "QP" to "Akasa Air", "SG" to "SpiceJet")
    private val international = listOf("EK" to "Emirates", "QR" to "Qatar Airways", "SQ" to "Singapore Airlines", "AI" to "Air India", "LH" to "Lufthansa",
        "BA" to "British Airways", "EY" to "Etihad Airways", "TK" to "Turkish Airlines", "6E" to "IndiGo")
    private val hubs = mapOf("EK" to "DXB", "QR" to "DOH", "SQ" to "SIN", "LH" to "FRA", "BA" to "LHR", "EY" to "AUH", "TK" to "IST", "AI" to "DEL", "6E" to "DEL", "IX" to "DEL", "QP" to "BOM", "SG" to "DEL")
    private val cabins = mapOf("economy" to (1.0 to "Economy"), "premium_economy" to (1.7 to "Premium Economy"), "business" to (3.4 to "Business"), "first" to (5.5 to "First"))
    private val offers = mutableMapOf<String, Offer>()

    override suspend fun health() = Health(ok = true, mode = "demo", version = "app")

    override suspend fun places(query: String): List<Place> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        fun score(a: Port) = when {
            a.code.lowercase() == q -> 0
            a.city.lowercase().startsWith(q) -> 1
            a.code.lowercase().startsWith(q) -> 2
            a.name.lowercase().contains(q) || a.city.lowercase().contains(q) -> 3
            else -> 9
        }
        return airports.map { score(it) to it }.filter { it.first < 9 }.sortedWith(compareBy({ it.first }, { it.second.city })).take(8)
            .map { (_, a) -> Place(a.code, a.name, a.city, a.country) }
    }

    override suspend fun search(q: SearchQuery): Pair<String, List<Offer>> {
        delay(latency) // feels like a real search
        val from = byCode[q.from.code] ?: return "demo" to emptyList()
        val to = byCode[q.to.code] ?: return "demo" to emptyList()
        if (from == to) throw ApiException("Pick two different places.")
        val rnd = Random("${q.from.code}${q.to.code}${q.depart}${q.returnDate ?: ""}${q.cabin}".hashCode())
        val isDomestic = from.country == "IN" && to.country == "IN"
        val carriers = if (isDomestic) domestic else international
        val distance = km(from, to)
        val inr = from.country == "IN"
        val (cabinFactor, cabinName) = cabins[q.cabin] ?: cabins.getValue("economy")
        val payers = q.adults + q.children * 0.75 + q.infants * 0.1
        val list = (0 until 8 + rnd.nextInt(8)).map { i ->
            val carrier = carriers[rnd.nextInt(carriers.size)]
            val stops = if (distance > 2200 && rnd.nextDouble() < 0.55) 1 else if (distance > 900 && rnd.nextDouble() < 0.2) 1 else 0
            val out = slice(rnd, from, to, q.depart, carrier, stops, 5 * 60 + rnd.nextInt(17 * 60 / 5) * 5)
            val back = q.returnDate?.let { slice(rnd, to, from, it, carrier, stops, 6 * 60 + rnd.nextInt(15 * 60 / 5) * 5) }
            val usd = (35 + distance * 0.075 * (0.8 + rnd.nextDouble() * 0.7) - stops * 25) * cabinFactor * (if (back != null) 1.85 else 1.0) * payers
            val amount = if (inr) (usd * 84 / 10).roundToInt() * 10.0 else (usd * 100).roundToInt() / 100.0
            val premium = q.cabin == "business" || q.cabin == "first"
            Offer(
                id = "demo_${q.from.code}${q.to.code}_${q.depart.replace("-", "")}_${i}_${rnd.nextInt(1_000_000).toString(36)}",
                price = Money(amount, if (inr) "INR" else "USD"),
                airline = Airline(carrier.second, carrier.first),
                slices = listOfNotNull(out, back),
                cabin = cabinName,
                baggage = Baggage(checked = if (isDomestic && q.cabin == "economy") (if (rnd.nextDouble() < 0.75) 1 else 0) else 1 + (if (premium) 1 else 0), carryOn = 1),
                refundable = rnd.nextDouble() < 0.35 || q.cabin != "economy",
                changeable = rnd.nextDouble() < 0.7,
                expiresAt = Instant.now().plus(30, ChronoUnit.MINUTES).toString(),
                passengerIds = (1..q.passengers).map { "pas_demo_$it" },
                demo = true,
            ).also { offers[it.id] = it }
        }
        return "demo" to list
    }

    override suspend fun offer(id: String): Offer = offers[id] ?: throw ApiException("This flight is no longer available. Search again.")

    override suspend fun book(offerId: String, passengers: List<PassengerForm>): Booking {
        delay(latency)
        val offer = offer(offerId)
        val rnd = Random((offerId + passengers.joinToString { it.family }).hashCode())
        val letters = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val reference = (1..6).map { letters[rnd.nextInt(letters.length)] }.joinToString("")
        return Booking(
            id = "ord_demo_${reference.lowercase()}", reference = reference, createdAt = Instant.now().toString(), price = offer.price,
            passengers = passengers.map { BookedPassenger("${it.given.trim()} ${it.family.trim()}") }, offer = offer, demo = true,
        )
    }

    private fun km(a: Port, b: Port): Double {
        val r = Math.PI / 180
        val h = sin((b.lat - a.lat) * r / 2).pow(2) + cos(a.lat * r) * cos(b.lat * r) * sin((b.lon - a.lon) * r / 2).pow(2)
        return 6371 * 2 * asin(sqrt(h))
    }

    private fun local(date: String, minutes: Int) = LocalDate.parse(date).atStartOfDay().plusMinutes(minutes.toLong()).toString().let { if (it.length == 16) "$it:00" else it }

    private fun slice(rnd: Random, from: Port, to: Port, date: String, carrier: Pair<String, String>, stops: Int, start: Int): Slice {
        val hub = if (stops > 0) hubs[carrier.first]?.let { byCode[it] } else null
        val legs = if (hub != null && hub != from && hub != to) listOf(from to hub, hub to to) else listOf(from to to)
        var t = start
        val segments = legs.mapIndexed { i, (a, b) ->
            val distance = km(a, b)
            val flying = (distance / 780 * 60 + 28 + rnd.nextDouble() * 12).roundToInt()
            val aircraft = if (distance > 3500) (if (rnd.nextBoolean()) "Boeing 787-9" else "Airbus A350-900") else (if (rnd.nextBoolean()) "Airbus A320neo" else "Boeing 737-8")
            Segment("${carrier.first}${100 + rnd.nextInt(899)}", carrier.second, carrier.first, a.airport, b.airport, local(date, t), local(date, t + flying), flying, aircraft)
                .also { t += flying + if (i < legs.lastIndex) 70 + rnd.nextInt(150) else 0 } // a layover between legs
        }
        return Slice(from.airport, to.airport, segments.first().depart, segments.last().arrive, t - start, segments.size - 1, segments)
    }
}
