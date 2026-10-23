package app.accordgarage.domain

import app.accordgarage.data.OilTopUp
import app.accordgarage.data.ServiceRecord
import app.accordgarage.data.WearReading

/**
 * What the app measures over time. [limit] is the replace-at value used for the
 * projection: a common rule of thumb, shown as such, not a figure from the manual.
 */
data class WearKind(
    val key: String,
    val label: String,
    val unit: String,
    val jobs: List<String>,
    val limit: Double,
    val group: String,
)

object Wear {
    val kinds: List<WearKind> = listOf(
        WearKind("pad_front", "Front pads", "mm", listOf("front_brakes"), 3.0, "Brake pads"),
        WearKind("pad_rear", "Rear pads", "mm", listOf("rear_brakes"), 3.0, "Brake pads"),
        WearKind("tread_fl", "Tread, front left", "/32 in", listOf("tire_rotation", "tires"), 2.0, "Tread depth"),
        WearKind("tread_fr", "Tread, front right", "/32 in", listOf("tire_rotation", "tires"), 2.0, "Tread depth"),
        WearKind("tread_rl", "Tread, rear left", "/32 in", listOf("tire_rotation", "tires"), 2.0, "Tread depth"),
        WearKind("tread_rr", "Tread, rear right", "/32 in", listOf("tire_rotation", "tires"), 2.0, "Tread depth"),
    )

    fun kind(key: String): WearKind? = kinds.firstOrNull { it.key == key }

    /**
     * Straight-line fit through (odometer, value). Returns the odometer where the line
     * reaches [limit], or null with fewer than two readings or no downward trend.
     */
    fun projectedOdometer(readings: List<WearReading>, limit: Double): Int? {
        val pts = readings.sortedBy { it.odometer }
        if (pts.size < 2 || pts.last().odometer == pts.first().odometer) return null
        val n = pts.size.toDouble()
        val mx = pts.sumOf { it.odometer.toDouble() } / n
        val my = pts.sumOf { it.value } / n
        val sxx = pts.sumOf { (it.odometer - mx) * (it.odometer - mx) }
        val sxy = pts.sumOf { (it.odometer - mx) * (it.value - my) }
        if (sxx == 0.0) return null
        val slope = sxy / sxx
        if (slope >= 0) return null
        val at = mx + (limit - my) / slope
        return at.toInt().takeIf { it > pts.last().odometer }
    }

    /**
     * Miles per quart for each top-up: miles driven since the previous top-up (or the
     * last oil change before it) divided by the quarts added. Pairs of (odometer, mi/qt).
     */
    fun oilUse(topUps: List<OilTopUp>, services: List<ServiceRecord>): List<Pair<Int, Double>> {
        val changes = services.filter { "oil_change" in it.jobs }.map { it.odometer }
        var previous: Int? = null
        return topUps.sortedBy { it.odometer }.mapNotNull { t ->
            val lastChange = changes.filter { it <= t.odometer }.maxOrNull()
            val start = listOfNotNull(previous, lastChange).maxOrNull()
            previous = t.odometer
            if (start == null || t.quarts <= 0 || t.odometer <= start) null
            else t.odometer to (t.odometer - start) / t.quarts
        }
    }
}
