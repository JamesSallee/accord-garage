package app.accordgarage.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.accordgarage.R

@Immutable
data class GarageColors(
    val bg: Color,
    val bar: Color,
    val surface: Color,
    val surface2: Color,
    val line: Color,
    val line2: Color,
    val text: Color,
    val text2: Color,
    val muted: Color,
    val accent: Color,
    val accentFill: Color,
    val onAccentFill: Color,
    val amber: Color,
    val green: Color,
    val cyan: Color,
    val paint: Color,
    val isDark: Boolean,
)

val DarkGarage = GarageColors(
    bg = Color(0xFF0B0D10),
    bar = Color(0xFF0E1115),
    surface = Color(0xFF12161B),
    surface2 = Color(0xFF1A1F26),
    line = Color(0xFF1E242C),
    line2 = Color(0xFF2A3038),
    text = Color(0xFFE8EAED),
    text2 = Color(0xFFC9CDD2),
    muted = Color(0xFF8A9099),
    accent = Color(0xFFF2555A),
    accentFill = Color(0xFFC42A2F),
    onAccentFill = Color.White,
    amber = Color(0xFFE8A33D),
    green = Color(0xFF4CC38A),
    cyan = Color(0xFF5CC8E0),
    paint = Color(0xFFB3121B),
    isDark = true,
)

/** High-contrast "sunlight" theme for working outdoors. */
val LightGarage = GarageColors(
    bg = Color(0xFFF4F5F7),
    bar = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFE9ECEF),
    line = Color(0xFFD5D9DE),
    line2 = Color(0xFFBFC5CC),
    text = Color(0xFF0B0D10),
    text2 = Color(0xFF262C33),
    muted = Color(0xFF535A63),
    accent = Color(0xFFB3121B),
    accentFill = Color(0xFFB3121B),
    onAccentFill = Color.White,
    amber = Color(0xFF8F5300),
    green = Color(0xFF1B6B43),
    cyan = Color(0xFF006A80),
    paint = Color(0xFFB3121B),
    isDark = false,
)

val LocalGarage = staticCompositionLocalOf { DarkGarage }

val Inter = FontFamily(
    Font(R.font.inter_400, FontWeight.Normal),
    Font(R.font.inter_500, FontWeight.Medium),
    Font(R.font.inter_600, FontWeight.SemiBold),
)

val Mono = FontFamily(
    Font(R.font.jetbrains_mono_400, FontWeight.Normal),
    Font(R.font.jetbrains_mono_500, FontWeight.Medium),
)

object Type {
    val label = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 1.8.sp)
    val body = TextStyle(fontFamily = Inter, fontSize = 15.sp, lineHeight = 22.sp)
    val bodySmall = TextStyle(fontFamily = Inter, fontSize = 13.sp, lineHeight = 18.sp)
    val title = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 30.sp, letterSpacing = (-0.2).sp)
    val heading = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp)
    val rowTitle = TextStyle(fontFamily = Inter, fontSize = 15.sp, lineHeight = 20.sp)
    val mono = TextStyle(fontFamily = Mono, fontSize = 12.sp, lineHeight = 17.sp)
    val monoSmall = TextStyle(fontFamily = Mono, fontSize = 11.sp, lineHeight = 15.sp)
    val readout = TextStyle(fontFamily = Mono, fontSize = 44.sp, lineHeight = 48.sp, letterSpacing = (-0.8).sp)
    val tileValue = TextStyle(fontFamily = Mono, fontSize = 24.sp, lineHeight = 28.sp)
}

@Composable
fun GarageTheme(light: Boolean, content: @Composable () -> Unit) {
    val g = if (light) LightGarage else DarkGarage
    val scheme = if (light) {
        lightColorScheme(
            primary = g.accentFill, onPrimary = g.onAccentFill, background = g.bg, onBackground = g.text,
            surface = g.surface, onSurface = g.text, surfaceVariant = g.surface2, onSurfaceVariant = g.muted,
            outline = g.line2, secondary = g.text2, error = g.accent,
        )
    } else {
        darkColorScheme(
            primary = g.accent, onPrimary = Color.White, background = g.bg, onBackground = g.text,
            surface = g.surface, onSurface = g.text, surfaceVariant = g.surface2, onSurfaceVariant = g.muted,
            outline = g.line2, secondary = g.text2, error = g.accent,
            surfaceContainerHigh = g.surface2, surfaceContainer = g.surface,
        )
    }
    CompositionLocalProvider(LocalGarage provides g) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
