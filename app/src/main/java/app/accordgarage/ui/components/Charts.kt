package app.accordgarage.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import app.accordgarage.ui.theme.LocalGarage
import app.accordgarage.ui.theme.Type

data class ChartSeries(val label: String, val color: Color, val points: List<Pair<Double, Double>>)

/**
 * Thin lines against mileage, with an optional dashed limit line. Labels are set in
 * the mono face like every other reading.
 */
@Composable
fun LineChart(
    series: List<ChartSeries>,
    modifier: Modifier = Modifier,
    limit: Double? = null,
    xLabel: (Double) -> String = { Fmt.miles(it.toInt()) },
    yLabel: (Double) -> String = { Fmt.num(it) },
) {
    val g = LocalGarage.current
    val measurer = rememberTextMeasurer()
    val all = series.flatMap { it.points }
    if (all.isEmpty()) return
    val xMin = all.minOf { it.first }
    val xMax = all.maxOf { it.first }.let { if (it == xMin) xMin + 1 else it }
    val ys = all.map { it.second } + listOfNotNull(limit)
    val yMin = (ys.min() * 0.9).coerceAtLeast(0.0)
    val yMax = (ys.max() * 1.05).let { if (it <= yMin) yMin + 1 else it }
    val labelStyle = Type.monoSmall.copy(color = g.muted)

    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(160.dp)) {
            val left = 44.dp.toPx()
            val w = size.width - left
            val h = size.height
            fun x(v: Double) = left + ((v - xMin) / (xMax - xMin) * w).toFloat()
            fun y(v: Double) = (h - (v - yMin) / (yMax - yMin) * h).toFloat()

            listOf(yMin, yMax).forEach { v ->
                drawLine(g.line, Offset(left, y(v)), Offset(size.width, y(v)), strokeWidth = 1f)
                val text = measurer.measure(yLabel(v), labelStyle)
                drawText(text, topLeft = Offset(0f, (y(v) - text.size.height / 2f).coerceIn(0f, h - text.size.height)))
            }
            limit?.let { v ->
                drawLine(
                    g.amber, Offset(left, y(v)), Offset(size.width, y(v)), strokeWidth = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
                )
            }
            series.forEach { s ->
                val pts = s.points.sortedBy { it.first }
                if (pts.size > 1) {
                    val path = Path()
                    pts.forEachIndexed { i, p -> if (i == 0) path.moveTo(x(p.first), y(p.second)) else path.lineTo(x(p.first), y(p.second)) }
                    drawPath(path, s.color, style = Stroke(width = 2.dp.toPx()))
                }
                pts.forEach { p -> drawCircle(s.color, radius = 3.dp.toPx(), center = Offset(x(p.first), y(p.second))) }
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 44.dp, top = 4.dp)) {
            Text(xLabel(xMin), style = Type.monoSmall, color = g.muted, modifier = Modifier.weight(1f))
            Text(xLabel(xMax), style = Type.monoSmall, color = g.muted)
        }
        if (series.size > 1) {
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                series.forEach { s ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Dot(s.color, 6.dp)
                        Spacer(Modifier.width(4.dp))
                        Text(s.label.uppercase(), style = Type.monoSmall, color = g.text2)
                    }
                }
            }
        }
    }
}

/** Vertical bars, one per label, for things like cost per month. */
@Composable
fun BarChart(values: List<Pair<String, Double>>, modifier: Modifier = Modifier, highlightLast: Boolean = true) {
    val g = LocalGarage.current
    val max = values.maxOfOrNull { it.second }?.takeIf { it > 0 } ?: 1.0
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(120.dp)) {
            val slot = size.width / values.size.coerceAtLeast(1)
            val barW = slot * 0.56f
            values.forEachIndexed { i, (_, v) ->
                val bh = (v / max * size.height).toFloat()
                val color = if (highlightLast && i == values.lastIndex) g.text else g.muted
                drawRoundRect(
                    color,
                    topLeft = Offset(i * slot + (slot - barW) / 2, size.height - bh),
                    size = Size(barW, bh),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                )
            }
            drawLine(g.line, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1f)
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            values.forEach { (label, _) ->
                Text(label, style = Type.monoSmall, color = g.muted, modifier = Modifier.weight(1f), maxLines = 1)
            }
        }
    }
}

/** A labelled thin bar: label, value, and a bar showing its share of [max]. */
@Composable
fun ShareBar(label: String, value: String, fraction: Float) {
    val g = LocalGarage.current
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(label, style = Type.rowTitle, color = g.text, modifier = Modifier.weight(1f))
            Text(value, style = Type.mono, color = g.text2)
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(4.dp)) {
            Canvas(Modifier.fillMaxWidth().height(4.dp)) {
                drawRoundRect(g.surface2, size = size, cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()))
                drawRoundRect(g.text2, size = Size(size.width * fraction.coerceIn(0f, 1f), size.height), cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()))
            }
        }
    }
}
