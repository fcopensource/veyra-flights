package com.veyraeditor.flights.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FlightLand
import androidx.compose.material.icons.outlined.FlightTakeoff
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.veyraeditor.flights.data.Cabin
import com.veyraeditor.flights.data.Place
import com.veyraeditor.flights.data.dateLabel
import com.veyraeditor.flights.data.passengersLabel
import com.veyraeditor.flights.ui.theme.HeroGradient
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(vm: AppViewModel, onSearch: () -> Unit) {
    val form by vm.form.collectAsStateWithLifecycle()
    val recent by vm.recent.collectAsStateWithLifecycle()
    val mode by vm.mode.collectAsStateWithLifecycle()
    var picking by remember { mutableStateOf<String?>(null) } // "from" or "to"
    var dateFor by remember { mutableStateOf<String?>(null) }  // "depart" or "return"
    var travellers by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // Hero
        Box(Modifier.fillMaxWidth().background(HeroGradient).statusBarsPadding().padding(start = 22.dp, end = 22.dp, top = 28.dp, bottom = 86.dp)) {
            Column {
                Text("VEYRA FLIGHTS", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Where to next?", style = MaterialTheme.typography.displaySmall, color = Color.White)
                Text("Compare flights from hundreds of airlines.", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.88f))
            }
        }
        // Search card, overlapping the hero
        Card(
            Modifier.padding(horizontal = 16.dp).offset(y = (-64).dp).fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(selected = !form.roundTrip, onClick = { vm.update { it.copy(roundTrip = false) } }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text("One way") }
                    SegmentedButton(selected = form.roundTrip, onClick = { vm.update { it.copy(roundTrip = true) } }, shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text("Round trip") }
                }
                Box {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PickerField("From", form.from?.let { "${it.city} (${it.code})" } ?: "", "City or airport", Modifier.fillMaxWidth(),
                            leading = { Icon(Icons.Outlined.FlightTakeoff, null, tint = MaterialTheme.colorScheme.primary) }) { picking = "from" }
                        PickerField("To", form.to?.let { "${it.city} (${it.code})" } ?: "", "City or airport", Modifier.fillMaxWidth(),
                            leading = { Icon(Icons.Outlined.FlightLand, null, tint = MaterialTheme.colorScheme.primary) }) { picking = "to" }
                    }
                    SmallFloatingActionButton(onClick = vm::swap, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 10.dp),
                        containerColor = MaterialTheme.colorScheme.primaryContainer) { Icon(Icons.Rounded.SwapVert, "Swap from and to") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PickerField("Depart", dateLabel(form.depart.toString()), "", Modifier.weight(1f),
                        leading = { Icon(Icons.Outlined.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary) }) { dateFor = "depart" }
                    if (form.roundTrip) PickerField("Return", dateLabel(form.returnDate.toString()), "", Modifier.weight(1f),
                        leading = { Icon(Icons.Outlined.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary) }) { dateFor = "return" }
                }
                PickerField("Travellers and cabin", "${passengersLabel(form.adults, form.children, form.infants)} · ${form.cabin.label}", "", Modifier.fillMaxWidth(),
                    leading = { Icon(Icons.Outlined.Person, null, tint = MaterialTheme.colorScheme.primary) }) { travellers = true }
                Button(onClick = { vm.search(); onSearch() }, enabled = vm.canSearch, modifier = Modifier.fillMaxWidth().height(54.dp), shape = MaterialTheme.shapes.medium) {
                    Icon(Icons.Rounded.Search, null); Spacer(Modifier.width(8.dp)); Text("Search flights")
                }
            }
        }
        Column(Modifier.offset(y = (-48).dp).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ModeBanner(mode)
            if (recent.isNotEmpty()) {
                Text("Recent searches", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 6.dp))
                recent.forEach { q ->
                    Surface(onClick = { vm.useRecent(q) }, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.History, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("${q.from.city} → ${q.to.city}", style = MaterialTheme.typography.titleSmall)
                                Text("${q.from.code} – ${q.to.code} · ${passengersLabel(q.adults, q.children, q.infants)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    picking?.let { which ->
        AirportPicker(title = if (which == "from") "Flying from" else "Flying to", vm = vm, onDismiss = { picking = null }) { place ->
            vm.update { if (which == "from") it.copy(from = place) else it.copy(to = place) }
            picking = null
        }
    }
    dateFor?.let { which ->
        val initial = if (which == "depart") form.depart else form.returnDate
        val earliest = if (which == "depart") LocalDate.now() else form.depart
        val state = rememberDatePickerState(
            initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isBefore(earliest)
                override fun isSelectableYear(year: Int) = year in LocalDate.now().year..LocalDate.now().year + 1
            },
        )
        DatePickerDialog(
            onDismissRequest = { dateFor = null },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { ms ->
                        val d = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                        vm.update { if (which == "depart") it.copy(depart = d) else it.copy(returnDate = d) }
                    }
                    dateFor = null
                }) { Text("Done") }
            },
            dismissButton = { TextButton(onClick = { dateFor = null }) { Text("Cancel") } },
        ) { DatePicker(state = state, showModeToggle = false) }
    }
    if (travellers) {
        ModalBottomSheet(onDismissRequest = { travellers = false }) {
            Column(Modifier.padding(horizontal = 22.dp).padding(bottom = 28.dp)) {
                Text("Travellers", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Stepper("Adults", "12 years and over", form.adults, 1, 9 - form.children) { n -> vm.update { it.copy(adults = n, infants = it.infants.coerceAtMost(n)) } }
                HorizontalDivider()
                Stepper("Children", "2 to 11 years", form.children, 0, 9 - form.adults) { n -> vm.update { it.copy(children = n) } }
                HorizontalDivider()
                Stepper("Infants", "Under 2, on a lap", form.infants, 0, form.adults) { n -> vm.update { it.copy(infants = n) } }
                Spacer(Modifier.height(16.dp))
                Text("Cabin", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Cabin.entries.take(2).forEach { c -> FilterChip(selected = form.cabin == c, onClick = { vm.update { it.copy(cabin = c) } }, label = { Text(c.label) }) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Cabin.entries.drop(2).forEach { c -> FilterChip(selected = form.cabin == c, onClick = { vm.update { it.copy(cabin = c) } }, label = { Text(c.label) }) }
                }
                Spacer(Modifier.height(12.dp))
                Button(onClick = { travellers = false }, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Done") }
            }
        }
    }
}

/** Full-screen search for an airport or city. */
@Composable
fun AirportPicker(title: String, vm: AppViewModel, onDismiss: () -> Unit, onPick: (Place) -> Unit) {
    var query by remember { mutableStateOf("") }
    var places by remember { mutableStateOf<List<Place>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    LaunchedEffect(query) { vm.suggest(query) { list, e -> places = list; error = e } }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.statusBarsPadding().imePadding()) {
                Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
                    Text(title, style = MaterialTheme.typography.titleLarge)
                }
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    placeholder = { Text("Search a city or airport, like Goa or DEL") },
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    singleLine = true, shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).focusRequester(focus),
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(16.dp)) }
                LazyColumn(Modifier.fillMaxSize().padding(top = 8.dp)) {
                    items(places, key = { it.code + it.type }) { p ->
                        Row(Modifier.fillMaxWidth().clickable { onPick(p) }.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(48.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                Text(p.code, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(p.city.ifEmpty { p.name }, style = MaterialTheme.typography.titleMedium)
                                Text(p.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            if (p.country.isNotEmpty()) Text(p.country, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
