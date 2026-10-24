package com.veyraeditor.flights.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.addJsonObject
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/** A problem worth showing to the person, in plain words. */
class ApiException(message: String) : Exception(message)

/** Talks to the Veyra Flights server. */
class Api(private val baseUrl: String, private val appKey: String = "") {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS) // flight searches can take a while
        .build()
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    @Serializable private data class PlacesReply(val places: List<Place>)
    @Serializable private data class SearchReply(val mode: String, val offers: List<Offer>)
    @Serializable private data class OfferReply(val offer: Offer)
    @Serializable private data class BookingReply(val booking: Booking)
    @Serializable private data class ErrorReply(val error: String)

    private suspend fun call(method: String, path: String, body: JsonObject? = null): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(baseUrl.trimEnd('/') + path).apply {
            if (appKey.isNotEmpty()) header("X-App-Key", appKey)
            if (body != null) method(method, body.toString().toRequestBody("application/json".toMediaType())) else method(method, null)
        }.build()
        try {
            client.newCall(request).execute().use { res ->
                val text = res.body.string()
                if (!res.isSuccessful) {
                    val message = runCatching { json.decodeFromString<ErrorReply>(text).error }.getOrNull()
                    throw ApiException(message ?: "Something went wrong (${res.code}). Try again.")
                }
                text
            }
        } catch (e: ApiException) {
            throw e
        } catch (e: UnknownHostException) {
            throw ApiException("You're offline. Check your connection and try again.")
        } catch (e: SocketTimeoutException) {
            throw ApiException("The flight service is taking too long. Try again.")
        } catch (e: IOException) {
            throw ApiException("Can't reach Veyra Flights right now. Try again in a moment.")
        }
    }

    suspend fun health(): Health = json.decodeFromString(call("GET", "/health"))

    suspend fun places(query: String): List<Place> {
        val url = "http://x/api/places".toHttpUrl().newBuilder().addQueryParameter("q", query).build()
        return json.decodeFromString<PlacesReply>(call("GET", "/api/places?${url.encodedQuery}")).places
    }

    suspend fun search(q: SearchQuery): Pair<String, List<Offer>> {
        val body = buildJsonObject {
            put("from", q.from.code); put("to", q.to.code); put("depart", q.depart)
            q.returnDate?.let { put("return", it) }
            put("adults", q.adults); put("children", q.children); put("infants", q.infants); put("cabin", q.cabin)
        }
        val reply = json.decodeFromString<SearchReply>(call("POST", "/api/search", body))
        return reply.mode to reply.offers
    }

    suspend fun offer(id: String): Offer = json.decodeFromString<OfferReply>(call("GET", "/api/offers/$id")).offer

    suspend fun book(offerId: String, passengers: List<PassengerForm>): Booking {
        val body = buildJsonObject {
            put("offerId", offerId)
            putJsonArray("passengers") {
                for (p in passengers) addJsonObject {
                    put("title", p.title); put("given", p.given.trim()); put("family", p.family.trim()); put("born", p.born); put("gender", p.gender)
                    if (p.email.isNotBlank()) put("email", p.email.trim())
                    if (p.phone.isNotBlank()) put("phone", p.phone.trim())
                }
            }
        }
        return json.decodeFromString<BookingReply>(call("POST", "/api/bookings", body)).booking
    }
}
