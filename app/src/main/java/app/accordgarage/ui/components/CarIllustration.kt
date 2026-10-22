package app.accordgarage.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import app.accordgarage.domain.Zone
import app.accordgarage.ui.theme.LocalGarage
import app.accordgarage.ui.theme.Mono

private const val VB_W = 310f
private const val VB_H = 96f

/**
 * Side view of a red two-door coupe, drawn in a 310 x 96 box. A callout appears only
 * on a zone that has something to act on; [callouts] maps zone -> label text.
 * (A photo of the owner's own car replaces this in a later version.)
 */
@Composable
fun CarIllustration(callouts: Map<Zone, String>, modifier: Modifier = Modifier) {
    val g = LocalGarage.current
    val measurer = rememberTextMeasurer()
    val description = if (callouts.isEmpty()) "Red coupe. Nothing needs attention."
    else "Red coupe. Needs attention: " + callouts.values.joinToString(", ")

    Canvas(
        modifier
            .fillMaxWidth()
            .aspectRatio(VB_W / VB_H)
            .semantics { contentDescription = description },
    ) {
        val s = size.width / VB_W
        val ground = if (g.isDark) Color(0xFF1E242C) else Color(0xFFD5D9DE)
        val tire = if (g.isDark) Color(0xFF15191E) else Color(0xFF2A3038)
        val rim = Color(0xFF9AA1AA)
        val glass = Color(0xFF0E1217)

        withTransform({ scale(s, s, pivot = Offset.Zero) }) {
            drawLine(ground, Offset(6f, 84f), Offset(304f, 84f), strokeWidth = 1f)
            drawOval(Color.Black.copy(alpha = if (g.isDark) 0.55f else 0.18f), Offset(18f, 80.8f), Size(276f, 6.4f))

            val body = Path().apply {
                moveTo(18f, 72f); lineTo(16f, 61f)
                quadraticBezierTo(17f, 54f, 32f, 52f); lineTo(110f, 46f)
                quadraticBezierTo(142f, 26f, 170f, 22f); lineTo(214f, 21f)
                quadraticBezierTo(240f, 22f, 266f, 42f); lineTo(292f, 46f)
                quadraticBezierTo(299f, 48f, 299f, 57f); lineTo(297f, 71f); lineTo(266f, 72f)
                arcTo(Rect(214f, 46f, 266f, 98f), 0f, -180f, false)
                lineTo(92f, 72f)
                arcTo(Rect(40f, 46f, 92f, 98f), 0f, -180f, false)
                close()
            }
            drawPath(body, g.paint)
            drawPath(body, g.paint, style = Stroke(width = 1f, join = StrokeJoin.Round))

            // shading and details
            drawRect(Color.Black.copy(alpha = 0.28f), Offset(92f, 69.5f), Size(122f, 2.5f))
            drawLine(Color.White.copy(alpha = 0.14f), Offset(116f, 48.6f), Offset(262f, 44.6f), strokeWidth = 1f)
            drawLine(Color.White.copy(alpha = 0.28f), Offset(30f, 53f), Offset(110f, 47.5f), strokeWidth = 1f, cap = StrokeCap.Round)

            val window = Path().apply {
                moveTo(122f, 46f); quadraticBezierTo(148f, 30f, 172f, 27f); lineTo(210f, 26f)
                quadraticBezierTo(230f, 27f, 250f, 42f); close()
            }
            drawPath(window, glass)
            drawLine(g.paint, Offset(226f, 26.5f), Offset(232f, 43f), strokeWidth = 2.2f)
            drawLine(Color.Black.copy(alpha = 0.35f), Offset(130f, 48f), Offset(128f, 70f), strokeWidth = 1f)
            drawLine(Color.Black.copy(alpha = 0.35f), Offset(234f, 45f), Offset(230f, 70f), strokeWidth = 1f)

            val mirror = Path().apply { moveTo(120f, 47.5f); lineTo(126.5f, 44.5f); lineTo(131f, 46f); lineTo(128.5f, 49.2f); close() }
            drawPath(mirror, g.paint)
            drawPath(mirror, Color.Black.copy(alpha = 0.45f), style = Stroke(width = 0.8f, join = StrokeJoin.Round))
            drawRect(Color.Black.copy(alpha = 0.45f), Offset(196f, 51f), Size(9f, 1.7f))

            drawLine(Color(0xFFE8EAED), Offset(17f, 57f), Offset(33f, 55f), strokeWidth = 1.6f, cap = StrokeCap.Round)
            drawLine(Color(0xFFFF8A8D), Offset(292f, 49f), Offset(298f, 53f), strokeWidth = 1.8f, cap = StrokeCap.Round)

            wheel(66f, 76f, tire, rim)
            wheel(240f, 76f, tire, rim)

            val lead = g.muted
            callouts[Zone.POWERTRAIN]?.let {
                marker(62f, 48f, g.amber, g.bg)
                drawLine(lead, Offset(62f, 41f), Offset(62f, 14f), strokeWidth = 0.8f)
                drawLine(lead, Offset(62f, 14f), Offset(78f, 14f), strokeWidth = 0.8f)
            }
            callouts[Zone.FRONT_WHEELS]?.let {
                marker(66f, 76f, g.amber, g.bg)
                drawLine(lead, Offset(66f, 83f), Offset(66f, 88f), strokeWidth = 0.8f)
            }
            callouts[Zone.REAR_BRAKES]?.let {
                marker(240f, 76f, g.amber, g.bg)
                drawLine(lead, Offset(240f, 83f), Offset(240f, 88f), strokeWidth = 0.8f)
            }
        }

        // Labels are drawn unscaled so the text stays crisp.
        val style = TextStyle(fontFamily = Mono, fontSize = (8.4f * s).toSp(), color = g.text2)
        fun label(text: String, x: Float, baseline: Float, alignEnd: Boolean) {
            val layout = measurer.measure(text, style)
            val left = if (alignEnd) x * s - layout.size.width else x * s
            drawText(layout, topLeft = Offset(left, baseline * s - layout.firstBaseline))
        }
        callouts[Zone.POWERTRAIN]?.let { label(it, 82f, 16.5f, false) }
        callouts[Zone.FRONT_WHEELS]?.let { label(it, 8f, 95f, false) }
        callouts[Zone.REAR_BRAKES]?.let { label(it, 300f, 95f, true) }
    }
}

private fun DrawScope.wheel(cx: Float, cy: Float, tire: Color, rim: Color) {
    val c = Offset(cx, cy)
    drawCircle(tire, 17f, c)
    drawCircle(Color(0xFF5C636C), 17f, c, style = Stroke(1.15f))
    drawCircle(rim, 9f, c, style = Stroke(1f))
    for (i in 0 until 5) {
        val a = Math.toRadians(-90.0 + i * 72.0)
        val cos = kotlin.math.cos(a).toFloat()
        val sin = kotlin.math.sin(a).toFloat()
        drawLine(rim, Offset(cx + 3f * cos, cy + 3f * sin), Offset(cx + 8.5f * cos, cy + 8.5f * sin), strokeWidth = 1.2f, cap = StrokeCap.Round)
    }
}

private fun DrawScope.marker(cx: Float, cy: Float, color: Color, ring: Color) {
    drawCircle(color.copy(alpha = 0.25f), 7f, Offset(cx, cy))
    drawCircle(ring, 3.6f, Offset(cx, cy))
    drawCircle(color, 2.8f, Offset(cx, cy))
}
