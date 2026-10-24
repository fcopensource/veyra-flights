package com.veyraeditor.flights.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* Veyra Flights: indigo and violet, with the Veyra cyan-to-fuchsia gradient for hero moments. */

val Indigo = Color(0xFF5B5BF0)
val Violet = Color(0xFF8B5CF6)
val Cyan = Color(0xFF22D3EE)
val Fuchsia = Color(0xFFD946EF)
val Mint = Color(0xFF34D399)
val Amber = Color(0xFFF59E0B)

val HeroGradient = Brush.linearGradient(listOf(Color(0xFF0EA5E9), Indigo, Violet, Fuchsia))

private val Light = lightColorScheme(
    primary = Indigo, onPrimary = Color.White, primaryContainer = Color(0xFFE4E4FF), onPrimaryContainer = Color(0xFF16166B),
    secondary = Violet, onSecondary = Color.White, secondaryContainer = Color(0xFFEDE4FF), onSecondaryContainer = Color(0xFF2E1065),
    tertiary = Color(0xFF0891B2), tertiaryContainer = Color(0xFFCFF7FE),
    background = Color(0xFFF6F7FB), onBackground = Color(0xFF111827),
    surface = Color.White, onSurface = Color(0xFF111827), surfaceVariant = Color(0xFFEEF0F6), onSurfaceVariant = Color(0xFF5B6475),
    surfaceContainer = Color(0xFFF1F2F8), surfaceContainerHigh = Color(0xFFE9EBF3), outline = Color(0xFFCBD0DC), outlineVariant = Color(0xFFE3E6EE),
    error = Color(0xFFDC2626),
)
private val Dark = darkColorScheme(
    primary = Color(0xFF8B8BFF), onPrimary = Color(0xFF0B0B3B), primaryContainer = Color(0xFF2B2B8C), onPrimaryContainer = Color(0xFFE4E4FF),
    secondary = Color(0xFFB79BFF), onSecondary = Color(0xFF240A5B), secondaryContainer = Color(0xFF3B2280), onSecondaryContainer = Color(0xFFEDE4FF),
    tertiary = Cyan, tertiaryContainer = Color(0xFF0E4B5A),
    background = Color(0xFF0B0F19), onBackground = Color(0xFFE6E8EF),
    surface = Color(0xFF121826), onSurface = Color(0xFFE6E8EF), surfaceVariant = Color(0xFF1B2232), onSurfaceVariant = Color(0xFF9AA3B5),
    surfaceContainer = Color(0xFF161D2C), surfaceContainerHigh = Color(0xFF1D2536), outline = Color(0xFF3A4458), outlineVariant = Color(0xFF263044),
    error = Color(0xFFF87171),
)

private val Type = Typography(
    displaySmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.8).sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp), medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(22.dp), extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun VeyraFlightsTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Dark else Light, typography = Type, shapes = AppShapes, content = content)
}
