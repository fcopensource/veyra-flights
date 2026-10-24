package com.veyraeditor.flights

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AirplaneTicket
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.veyraeditor.flights.data.Api
import com.veyraeditor.flights.data.TripsStore
import com.veyraeditor.flights.ui.AppViewModel
import com.veyraeditor.flights.ui.ConfirmationScreen
import com.veyraeditor.flights.ui.OfferScreen
import com.veyraeditor.flights.ui.PassengersScreen
import com.veyraeditor.flights.ui.ResultsScreen
import com.veyraeditor.flights.ui.SearchScreen
import com.veyraeditor.flights.ui.TripScreen
import com.veyraeditor.flights.ui.TripsScreen
import com.veyraeditor.flights.ui.theme.VeyraFlightsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val factory = AppViewModel.Factory(Api(BuildConfig.API_URL), TripsStore(applicationContext))
        setContent {
            VeyraFlightsTheme {
                VeyraFlightsApp(viewModel(factory = factory))
            }
        }
    }
}

/** The whole app: the search and trips tabs and the booking flow. */
@Composable
fun VeyraFlightsApp(vm: AppViewModel) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val tabs = route == "search" || route == "trips"
    fun tab(to: String) = nav.navigate(to) { popUpTo(nav.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (tabs) NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(selected = route == "search", onClick = { tab("search") }, icon = { Icon(Icons.Outlined.TravelExplore, null) }, label = { Text("Search") })
                NavigationBarItem(selected = route == "trips", onClick = { tab("trips") }, icon = { Icon(Icons.Outlined.AirplaneTicket, null) }, label = { Text("My trips") })
            }
        },
    ) { pad ->
        NavHost(
            nav, startDestination = "search", modifier = Modifier.padding(bottom = if (tabs) pad.calculateBottomPadding() else androidx.compose.ui.unit.Dp(0f)),
            enterTransition = { slideInHorizontally { it / 5 } + fadeIn() }, exitTransition = { fadeOut() },
            popEnterTransition = { fadeIn() }, popExitTransition = { slideOutHorizontally { it / 5 } + fadeOut() },
        ) {
            composable("search") { SearchScreen(vm) { nav.navigate("results") } }
            composable("results") { ResultsScreen(vm, onBack = { nav.popBackStack() }) { nav.navigate("offer") } }
            composable("offer") { OfferScreen(vm, onBack = { nav.popBackStack() }) { nav.navigate("passengers") } }
            composable("passengers") {
                PassengersScreen(vm, onBack = { nav.popBackStack() }) { nav.navigate("done") { popUpTo("search") } }
            }
            composable("done") {
                ConfirmationScreen(vm,
                    onTrips = { vm.resetBooking(); nav.navigate("trips") { popUpTo("search") } },
                    onHome = { vm.resetBooking(); nav.popBackStack("search", inclusive = false) })
            }
            composable("trips") { TripsScreen(vm, onOpen = { nav.navigate("trip/$it") }) { tab("search") } }
            composable("trip/{id}") { e -> TripScreen(vm, e.arguments?.getString("id") ?: "") { nav.popBackStack() } }
        }
    }
}
