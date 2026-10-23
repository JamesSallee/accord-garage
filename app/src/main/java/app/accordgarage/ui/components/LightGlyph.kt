package app.accordgarage.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.accordgarage.content.LampColor
import app.accordgarage.content.Symbol
import app.accordgarage.content.WarningLight
import app.accordgarage.ui.theme.LocalGarage
import app.accordgarage.ui.theme.Mono

@Composable
fun lampColor(c: LampColor): Color {
    val g = LocalGarage.current
    return when (c) {
        LampColor.RED -> g.accent
        LampColor.AMBER -> g.amber
        LampColor.GREEN -> g.green
        LampColor.BLUE -> g.cyan
    }
}

/** The light's symbol, or the word printed on the lamp, drawn in the lamp's color unless [tint] is given. */
@Composable
fun LightGlyph(light: WarningLight, size: Dp, modifier: Modifier = Modifier, tint: Color? = null) {
    val color = tint ?: lampColor(light.color)
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        val symbol = light.symbol
        if (symbol != null) {
            Canvas(Modifier.size(size)) { drawSymbol(symbol, color) }
        } else {
            val word = light.word.orEmpty()
            val lines = word.lines()
            val longest = lines.maxOf { it.length }.coerceAtLeast(1)
            // Fit the longest line to the box width; mono glyphs are about 0.6 em wide.
            val fontSize = minOf(size.value * 0.42f, size.value * 0.92f / (longest * 0.6f), size.value * 0.8f / lines.size)
            Text(
                word,
                color = color,
                fontFamily = Mono,
                fontWeight = FontWeight.Medium,
                fontSize = fontSize.sp,
                lineHeight = (fontSize * 1.1f).sp,
                textAlign = TextAlign.Center,
                maxLines = lines.size,
            )
        }
    }
}

/** Draws on a 24×24 grid scaled to the canvas. */
private fun DrawScope.drawSymbol(symbol: Symbol, color: Color) {
    val u = size.minDimension / 24f
    fun p(x: Float, y: Float) = Offset(x * u, y * u)
    val stroke = Stroke(width = 1.6f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
    fun line(x1: Float, y1: Float, x2: Float, y2: Float) = drawLine(color, p(x1, y1), p(x2, y2), 1.6f * u, StrokeCap.Round)
    fun path(build: Path.() -> Unit, fill: Boolean = false) {
        val path = Path().apply(build)
        if (fill) drawPath(path, color) else drawPath(path, color, style = stroke)
    }
    fun Path.m(x: Float, y: Float) = moveTo(x * u, y * u)
    fun Path.l(x: Float, y: Float) = lineTo(x * u, y * u)
    fun Path.q(cx: Float, cy: Float, x: Float, y: Float) = quadraticBezierTo(cx * u, cy * u, x * u, y * u)

    when (symbol) {
        Symbol.OIL -> {
            path({ m(8f, 10f); l(15f, 10f); l(20.5f, 7.5f); l(16.5f, 15.5f); l(8f, 15.5f); close() })
            line(11.5f, 10f, 11.5f, 8f); line(10f, 8f, 13f, 8f)
            path({ m(8f, 11f); l(4.5f, 9.5f); l(3.5f, 12f) })
            drawCircle(color, 1.1f * u, p(21f, 11.5f))
        }
        Symbol.BATTERY -> {
            drawRoundRect(color, p(3f, 7.5f), Size(18f * u, 12f * u), CornerRadius(1.2f * u), style = stroke)
            drawRect(color, p(6f, 5f), Size(3f * u, 2.5f * u))
            drawRect(color, p(15f, 5f), Size(3f * u, 2.5f * u))
            line(6f, 13.5f, 9.5f, 13.5f)
            line(14.5f, 13.5f, 18f, 13.5f); line(16.25f, 11.75f, 16.25f, 15.25f)
        }
        Symbol.ENGINE -> {
            path({
                m(5f, 10f); l(7f, 10f); l(7f, 8f); l(10f, 8f); l(10f, 6f); l(15f, 6f); l(15f, 8f); l(17f, 8f)
                l(18.5f, 10f); l(20f, 10f); l(20f, 8.5f); l(22f, 8.5f); l(22f, 16.5f); l(20f, 16.5f); l(20f, 15f)
                l(18.5f, 15f); l(16.5f, 18f); l(8f, 18f); l(6.5f, 16f); l(5f, 16f); close()
            })
            line(5f, 13f, 2f, 13f); line(2f, 10.5f, 2f, 15.5f)
        }
        Symbol.TIRE -> {
            path({ m(6f, 5f); q(2.5f, 11f, 5.5f, 17f); l(18.5f, 17f); q(21.5f, 11f, 18f, 5f) })
            listOf(6.5f, 10f, 14f, 17.5f).forEach { x -> line(x, 17f, x - 0.8f, 19.5f) }
            line(12f, 7.5f, 12f, 12f)
            drawCircle(color, 1f * u, p(12f, 14.5f))
        }
        Symbol.FUEL -> {
            drawRoundRect(color, p(4f, 4f), Size(10f * u, 16f * u), CornerRadius(1f * u), style = stroke)
            drawRect(color, p(6f, 6f), Size(6f * u, 4f * u))
            line(3f, 20f, 15f, 20f)
            path({ m(14f, 9f); l(17f, 9f); l(18.5f, 10.5f); l(18.5f, 16.5f); q(18.5f, 18f, 19.5f, 18f); q(20.5f, 18f, 20.5f, 16.5f); l(20.5f, 8f); l(18.5f, 5.5f) })
        }
        Symbol.WRENCH -> {
            line(5.5f, 18.5f, 14f, 10f)
            path({ m(13.2f, 7.2f); q(15f, 3.5f, 19f, 4.6f); l(16.4f, 7.2f); l(16.8f, 9.2f); l(18.8f, 9.6f); l(21.4f, 7f); q(22.5f, 11f, 18.8f, 12.8f); q(16f, 13.6f, 14f, 10f); q(12.6f, 8.6f, 13.2f, 7.2f) })
            drawCircle(color, 0.9f * u, p(5.5f, 18.5f))
        }
        Symbol.KEY -> {
            drawCircle(color, 3.6f * u, p(7.5f, 12f), style = stroke)
            line(11.1f, 12f, 21f, 12f)
            line(17.5f, 12f, 17.5f, 15f); line(20.5f, 12f, 20.5f, 14.5f)
        }
        Symbol.HIGH_BEAM -> {
            path({ m(13f, 6f); q(21f, 6f, 21f, 12f); q(21f, 18f, 13f, 18f); close() })
            listOf(7f, 10f, 14f, 17f).forEach { y -> line(3f, y, 9.5f, y) }
        }
        Symbol.LIGHTS_ON -> {
            path({ m(9.5f, 8f); q(5f, 8f, 5f, 12f); q(5f, 16f, 9.5f, 16f); close() })
            path({ m(14.5f, 8f); q(19f, 8f, 19f, 12f); q(19f, 16f, 14.5f, 16f); close() })
            line(11.5f, 8.5f, 11.5f, 15.5f); line(12.5f, 8.5f, 12.5f, 15.5f)
            listOf(9f, 12f, 15f).forEach { y -> line(1f, y + (y - 12f) * 0.3f, 3.5f, y); line(20.5f, y, 23f, y + (y - 12f) * 0.3f) }
        }
        Symbol.TURN -> {
            path({ m(2f, 12f); l(7f, 7f); l(7f, 10f); l(10.5f, 10f); l(10.5f, 14f); l(7f, 14f); l(7f, 17f); close() }, fill = true)
            path({ m(22f, 12f); l(17f, 7f); l(17f, 10f); l(13.5f, 10f); l(13.5f, 14f); l(17f, 14f); l(17f, 17f); close() }, fill = true)
        }
        Symbol.DOOR -> {
            // Car from above, nose up, driver's door swung open.
            drawRoundRect(color, p(8f, 2.5f), Size(8f * u, 19f * u), CornerRadius(3f * u), style = stroke)
            line(8.5f, 8f, 15.5f, 8f); line(8.5f, 16.5f, 15.5f, 16.5f)
            line(8f, 9.5f, 3.5f, 13.5f)
        }
        Symbol.SKID -> {
            // Car from behind with two wavy tracks.
            path({ m(5f, 11f); l(7f, 5.5f); l(17f, 5.5f); l(19f, 11f); l(19f, 14f); l(5f, 14f); close() })
            line(6f, 14f, 6f, 15.5f); line(18f, 14f, 18f, 15.5f)
            path({ m(7.5f, 22.5f); q(5.5f, 20.5f, 8f, 18.5f); q(10f, 17f, 8.5f, 16.5f) })
            path({ m(16.5f, 22.5f); q(14.5f, 20.5f, 17f, 18.5f); q(19f, 17f, 17.5f, 16.5f) })
        }
        Symbol.LED -> {
            drawCircle(color.copy(alpha = 0.25f), 6f * u, p(12f, 12f))
            drawCircle(color, 3.5f * u, p(12f, 12f))
        }
        Symbol.SEAT_BELT -> {
            drawCircle(color, 2.3f * u, p(13f, 4f))
            path({ m(12.5f, 7.5f); l(11f, 15f); l(16.5f, 15f); l(18f, 21f) })
            line(16f, 7.5f, 8.5f, 15.5f)
            line(12f, 10f, 9f, 13f)
        }
    }
}
