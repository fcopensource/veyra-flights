package com.veyraeditor.flights

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.veyraeditor.flights.data.Api
import com.veyraeditor.flights.data.TripsStore
import com.veyraeditor.flights.ui.AppViewModel
import com.veyraeditor.flights.ui.theme.VeyraFlightsTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Books a flight from start to finish against a running server (demo mode is fine: `npm start` in server/),
* saving a picture of every screen to app/build/screens. Skipped when no server is running.
 * Runs as Android 34 because Robolectric needs Java 21 for newer versions.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalRoborazziApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel7)
class AppFlowTest {
    @get:Rule val rule = createComposeRule()
    private val server = System.getProperty("veyra.server") ?: "http://127.0.0.1:8787"

    private fun shot(name: String) {
        rule.waitForIdle()
        captureScreenRoboImage("build/screens/$name.png")
    }

    private fun waitFor(text: String, substring: Boolean = false) =
        rule.waitUntilAtLeastOneExists(hasText(text, substring = substring), timeoutMillis = 15_000)

    @Test
    fun bookAFlight() {
        val api = Api(server)
        assumeTrue("no server at $server", runCatching { runBlocking { api.health() } }.isSuccess)
        val vm = AppViewModel(api, TripsStore(ApplicationProvider.getApplicationContext()))
        rule.setContent { VeyraFlightsTheme { VeyraFlightsApp(vm) } }

        waitFor("Where to next?")
        shot("1-search")

        rule.onNodeWithText("From").performClick()
        waitFor("Search a city or airport, like Goa or DEL")
        rule.onNodeWithText("Search a city or airport, like Goa or DEL").performTextInput("delhi")
        waitFor("Indira Gandhi International")
        shot("2-airport-picker")
        rule.onNodeWithText("Indira Gandhi International").performClick()

        rule.onNodeWithText("To").performClick()
        waitFor("Search a city or airport, like Goa or DEL")
        rule.onNodeWithText("Search a city or airport, like Goa or DEL").performTextInput("mumbai")
        waitFor("Chhatrapati Shivaji Maharaj International")
        rule.onNodeWithText("Chhatrapati Shivaji Maharaj International").performClick()
        waitFor("New Delhi (DEL)")
        waitFor("Mumbai (BOM)")
        shot("3-search-filled")

        rule.onNodeWithText("Search flights").performClick()
        waitFor(" flights", substring = true)
        waitFor("₹", substring = true)
        shot("4-results")

        rule.onAllNodes(hasText("₹", substring = true))[0].performClick()
        waitFor("Continue")
        shot("5-flight")
        rule.onNodeWithText("Continue").performClick()

        waitFor("Who's flying?")
        rule.onNodeWithText("Ms").performClick()
        rule.onNodeWithText("First and middle names").performTextInput("Asha")
        rule.onNodeWithText("Last name").performTextInput("Verma")
        vm.updatePassenger(0) { it.copy(born = "1992-05-14") } // the date picker itself is Material's own
        rule.onNodeWithText("F").performClick()
        rule.onNodeWithText("Email").performTextInput("asha@example.com")
        rule.onNodeWithText("Phone, with country code").performTextInput("+91 98765 43210")
        shot("6-passengers")

        rule.onNodeWithText("Book (test)").performClick()
        waitFor("Booking reference")
        shot("7-booked")

        rule.onNodeWithText("See my trips").performClick()
        waitFor("New Delhi → Mumbai")
        shot("8-my-trips")
    }
}
