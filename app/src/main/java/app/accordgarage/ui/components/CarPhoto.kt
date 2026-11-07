package app.accordgarage.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import app.accordgarage.R
import app.accordgarage.domain.Zone
import app.accordgarage.ui.theme.LocalGarage
import app.accordgarage.ui.theme.Mono

private const val VB_W = 310f
private const val VB_H = 126f

// Where the photo sits in the 310 x 126 box; the margins above and below hold the labels.
private const val IMG_X = 20f
private const val IMG_Y = 16f
private const val IMG_W = 270f

/**
 * Points on the photo (fractions of its width and height) that each callout marks:
 * engine bay on the hood, and the hub of each wheel. Measured on car_photo.webp, so
 * they have to be re-measured if the photo is replaced.
 */
private val anchors = mapOf(
    Zone.POWERTRAIN to Offset(0.198f, 0.31f),
    Zone.FRONT_WHEELS to Offset(0.188f, 0.801f),
    Zone.REAR_BRAKES to Offset(0.847f, 0.792f),
)

/**
 * Side photo of James's own Accord, background removed, drawn in a 310 x 126 box.
 * A callout appears only on a zone that has something to act on; [callouts] maps
 * zone -> label text.
 */
@Composable
fun CarPhoto(callouts: Map<Zone, String>, modifier: Modifier = Modifier) {
    val g = LocalGarage.current
    val photo = ImageBitmap.imageResource(R.drawable.car_photo)
    val measurer = rememberTextMeasurer()
    val description = if (callouts.isEmpty()) "Your red Accord coupe. Nothing needs attention."
    else "Your red Accord coupe. Needs attention: " + callouts.values.joinToString(", ")

    Canvas(
        modifier
            .fillMaxWidth()
            .aspectRatio(VB_W / VB_H)
            .semantics { contentDescription = description },
    ) {
        val s = size.width / VB_W
        val imgH = IMG_W * photo.height / photo.width
        val groundY = IMG_Y + imgH
        fun at(zone: Zone): Offset {
            val p = anchors.getValue(zone)
            return Offset(IMG_X + p.x * IMG_W, IMG_Y + p.y * imgH)
        }

        withTransform({ scale(s, s, pivot = Offset.Zero) }) {
            val ground = if (g.isDark) Color(0xFF1E242C) else Color(0xFFD5D9DE)
            drawLine(ground, Offset(6f, groundY), Offset(304f, groundY), strokeWidth = 1f)
            drawOval(
                Color.Black.copy(alpha = if (g.isDark) 0.6f else 0.2f),
                Offset(IMG_X + 6f, groundY - 4f), Size(IMG_W - 10f, 7f),
            )
        }

        // The photo is drawn in device pixels so it stays sharp.
        drawImage(
            photo,
            dstOffset = IntOffset((IMG_X * s).toInt(), (IMG_Y * s).toInt()),
            dstSize = IntSize((IMG_W * s).toInt(), (imgH * s).toInt()),
            filterQuality = FilterQuality.High,
        )

        withTransform({ scale(s, s, pivot = Offset.Zero) }) {
            val lead = g.muted
            callouts[Zone.POWERTRAIN]?.let {
                val p = at(Zone.POWERTRAIN)
                marker(p.x, p.y, g.amber, g.bg)
                drawLine(lead, Offset(p.x, p.y - 7f), Offset(p.x, 10f), strokeWidth = 0.8f)
                drawLine(lead, Offset(p.x, 10f), Offset(p.x + 16f, 10f), strokeWidth = 0.8f)
            }
            callouts[Zone.FRONT_WHEELS]?.let {
                val p = at(Zone.FRONT_WHEELS)
                marker(p.x, p.y, g.amber, g.bg)
                drawLine(lead, Offset(p.x, p.y + 7f), Offset(p.x, groundY + 5f), strokeWidth = 0.8f)
            }
            callouts[Zone.REAR_BRAKES]?.let {
                val p = at(Zone.REAR_BRAKES)
                marker(p.x, p.y, g.amber, g.bg)
                drawLine(lead, Offset(p.x, p.y + 7f), Offset(p.x, groundY + 5f), strokeWidth = 0.8f)
            }
        }

        // Labels are drawn unscaled so the text stays crisp.
        val style = TextStyle(fontFamily = Mono, fontSize = (8.4f * s).toSp(), color = g.text2)
        fun label(text: String, x: Float, baseline: Float, alignEnd: Boolean) {
            val layout = measurer.measure(text, style)
            val left = if (alignEnd) x * s - layout.size.width else x * s
            drawText(layout, topLeft = Offset(left, baseline * s - layout.firstBaseline))
        }
        callouts[Zone.POWERTRAIN]?.let { label(it, at(Zone.POWERTRAIN).x + 20f, 12.5f, false) }
        callouts[Zone.FRONT_WHEELS]?.let { label(it, 8f, groundY + 13f, false) }
        callouts[Zone.REAR_BRAKES]?.let { label(it, 302f, groundY + 13f, true) }
    }
}

private fun DrawScope.marker(cx: Float, cy: Float, color: Color, ring: Color) {
    drawCircle(color.copy(alpha = 0.25f), 7f, Offset(cx, cy))
    drawCircle(ring, 3.6f, Offset(cx, cy))
    drawCircle(color, 2.8f, Offset(cx, cy))
}
