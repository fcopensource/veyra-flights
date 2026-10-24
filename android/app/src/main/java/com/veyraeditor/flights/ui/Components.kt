package com.veyraeditor.flights.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Luggage
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veyraeditor.flights.data.Offer
import com.veyraeditor.flights.data.Slice
import com.veyraeditor.flights.data.dayShift
import com.veyraeditor.flights.data.formatDuration
import com.veyraeditor.flights.data.formatPrice
import com.veyraeditor.flights.data.stopsLabel
import com.veyraeditor.flights.data.timeOf
import com.veyraeditor.flights.ui.theme.Amber
import com.veyraeditor.flights.ui.theme.Mint
import kotlin.math.abs

/** A round badge with the airline's code, in a steady colour for each airline. */
@Composable
fun AirlineBadge(code: String, size: Int = 40) {
    val hue = (abs(code.hashCode()) % 360).toFloat()
    val top = Color.hsl(hue, 0.65f, 0.55f); val bottom = Color.hsl((hue + 40) % 360, 0.7f, 0.42f)
    Box(Modifier.size(size.dp).clip(CircleShape).background(Brush.linearGradient(listOf(top, bottom))), contentAlignment = Alignment.Center) {
        Text(code.take(2), color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size * 0.36f).sp)
    }
}

/** 06:00 ———•——— 10:10  with the duration above the line and a dot for each stop. */
@Composable
fun RouteLine(slice: Slice, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val line = MaterialTheme.colorScheme.outline
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(horizontalAlignment = Alignment.Start) {
            Text(timeOf(slice.depart), style = MaterialTheme.typography.titleLarge)
            Text(slice.from.code, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(formatDuration(slice.durationMinutes), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Canvas(Modifier.fillMaxWidth().height(14.dp)) {
                val y = size.height / 2
                drawLine(line, Offset(0f, y), Offset(size.width, y), strokeWidth = 2.dp.toPx(), pathEffect = if (slice.stops == 0) null else PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
                drawCircle(accent, 4.dp.toPx(), Offset(0f, y)); drawCircle(accent, 4.dp.toPx(), Offset(size.width, y))
                for (i in 1..slice.stops) drawCircle(Amber, 4.5.dp.toPx(), Offset(size.width * i / (slice.stops + 1), y))
            }
            Text(stopsLabel(slice.stops), style = MaterialTheme.typography.labelMedium, color = if (slice.stops == 0) Mint else Amber, fontWeight = FontWeight.SemiBold)
        }
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.Top) {
                Text(timeOf(slice.arrive), style = MaterialTheme.typography.titleLarge)
                val shift = dayShift(slice.depart, slice.arrive)
                if (shift.isNotEmpty()) Text(shift, style = MaterialTheme.typography.labelMedium, color = Amber, modifier = Modifier.padding(start = 2.dp))
            }
            Text(slice.to.code, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** One flight in the results. */
@Composable
fun FlightCard(offer: Offer, tag: String?, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AirlineBadge(offer.airline.code, 34)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(offer.airline.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(offer.slices.first().segments.joinToString(" · ") { it.flight }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (tag != null) Tag(tag)
            }
            offer.slices.forEach { RouteLine(it) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.WorkOutline, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(" ${offer.baggage.carryOn}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(10.dp))
                Icon(Icons.Outlined.Luggage, null, Modifier.size(16.dp), tint = if (offer.baggage.checked > 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline)
                Text(if (offer.baggage.checked > 0) " ${offer.baggage.checked}" else " No bag", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (offer.refundable) { Spacer(Modifier.width(10.dp)); Text("Refundable", style = MaterialTheme.typography.bodySmall, color = Mint, fontWeight = FontWeight.SemiBold) }
                Spacer(Modifier.weight(1f))
                Text(formatPrice(offer.price), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun Tag(text: String, color: Color = MaterialTheme.colorScheme.primary) {
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.14f)) {
        Text(text, color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

/** Says clearly when the flights shown aren't real fares. */
@Composable
fun ModeBanner(mode: String?, modifier: Modifier = Modifier) {
    val (text, color) = when (mode) {
        "demo" -> "Demo flights: sample fares to try the app. Bookings aren't real." to Amber
        "test" -> "Test mode: real flights, but bookings are test bookings and nothing is charged." to MaterialTheme.colorScheme.tertiary
        else -> return
    }
    Row(
        modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(color.copy(alpha = 0.14f)).border(1.dp, color.copy(alpha = 0.35f), MaterialTheme.shapes.medium).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Info, null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** A placeholder card that shimmers while results load. */
@Composable
fun SkeletonCard() {
    val t = rememberInfiniteTransition(label = "shimmer")
    val x by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart), label = "x")
    val base = MaterialTheme.colorScheme.surfaceVariant
    val shine = MaterialTheme.colorScheme.surface
    val brush = Brush.linearGradient(listOf(base, shine, base), start = Offset(x * 1200f - 400f, 0f), end = Offset(x * 1200f, 200f))
    Card(shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(34.dp).clip(CircleShape).background(brush))
                Spacer(Modifier.width(10.dp))
                Box(Modifier.width(120.dp).height(14.dp).clip(RoundedCornerShape(7.dp)).background(brush))
            }
            Box(Modifier.fillMaxWidth().height(34.dp).clip(RoundedCornerShape(10.dp)).background(brush))
            Row { Spacer(Modifier.weight(1f)); Box(Modifier.width(90.dp).height(22.dp).clip(RoundedCornerShape(8.dp)).background(brush)) }
        }
    }
}

/** − 2 + */
@Composable
fun Stepper(label: String, hint: String, value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FilledTonalIconButton(onClick = { onChange(value - 1) }, enabled = value > min) { Icon(Icons.Rounded.Remove, "Fewer $label") }
        Text("$value", style = MaterialTheme.typography.titleLarge, modifier = Modifier.width(36.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        FilledTonalIconButton(onClick = { onChange(value + 1) }, enabled = value < max) { Icon(Icons.Rounded.Add, "More $label") }
    }
}

/** A tappable field that looks like a text box: label above, value below. */
@Composable
fun PickerField(label: String, value: String, placeholder: String, modifier: Modifier = Modifier, leading: @Composable (() -> Unit)? = null, onClick: () -> Unit) {
    Row(
        modifier.clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceContainer).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) { leading(); Spacer(Modifier.width(10.dp)) }
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value.ifEmpty { placeholder }, style = MaterialTheme.typography.titleMedium, color = if (value.isEmpty()) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
