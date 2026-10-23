@file:OptIn(ExperimentalLayoutApi::class)

package app.accordgarage.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import app.accordgarage.data.Vehicle
import app.accordgarage.data.WearReading
import app.accordgarage.domain.CarSystem
import app.accordgarage.domain.Wear
import app.accordgarage.ui.LocalContainer
import app.accordgarage.ui.components.BarChart
import app.accordgarage.ui.components.ChartSeries
import app.accordgarage.ui.components.DateField
import app.accordgarage.ui.components.EmptyState
import app.accordgarage.ui.components.Field
import app.accordgarage.ui.components.Fmt
import app.accordgarage.ui.components.GarageTopBar
import app.accordgarage.ui.components.LineChart
import app.accordgarage.ui.components.Note
import app.accordgarage.ui.components.Panel
import app.accordgarage.ui.components.RowItem
import app.accordgarage.ui.components.SecondaryButton
import app.accordgarage.ui.components.SectionLabel
import app.accordgarage.ui.components.SelectPill
import app.accordgarage.ui.components.ShareBar
import app.accordgarage.ui.components.Tile
import app.accordgarage.ui.components.VSpace
import app.accordgarage.ui.components.toNumberOrNull
import app.accordgarage.ui.theme.LocalGarage
import app.accordgarage.ui.theme.Type
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CostsScreen(nav: NavHostController) {
    val c = LocalContainer.current
    val g = LocalGarage.current
    val services by c.db.services().all().collectAsState(initial = emptyList())
    val fuel by c.db.fuel().all().collectAsState(initial = emptyList())
    val snap by c.snapshot.collectAsState(initial = null)

    Column(Modifier.fillMaxSize()) {
        GarageTopBar("Costs", "Service, repairs and fuel", onBack = { nav.popBackStack() })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            if (services.isEmpty() && fuel.isEmpty()) {
                EmptyState("Nothing logged yet. Costs appear here as you log services and fill-ups.")
                return@Column
            }
            fun fuelCost(day: Long? = null) = fuel.filter { day == null || it.dateEpochDay == day }.sumOf { f -> (f.pricePerGallon ?: 0.0) * f.gallons }
            val serviceTotal = services.sumOf { it.totalCost }
            val fuelTotal = fuelCost()
            val firstOdo = (services.map { it.odometer } + fuel.map { it.odometer }).minOrNull() ?: Vehicle.STARTING_ODOMETER
            val miles = (snap?.odometer ?: firstOdo) - minOf(firstOdo, Vehicle.STARTING_ODOMETER)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Tile("Service", Fmt.money(serviceTotal), "ALL TIME", Modifier.weight(1f))
                Tile("Fuel", Fmt.money(fuelTotal), "ALL TIME", Modifier.weight(1f))
                Tile(
                    "Per mile",
                    if (miles > 0) Fmt.money((serviceTotal + fuelTotal) / miles) else "—",
                    if (miles > 0) "OVER ${Fmt.miles(miles)} MI" else "LOG MORE MILES",
                    Modifier.weight(1f),
                )
            }

            // Last 12 months, service and fuel together.
            val now = YearMonth.now()
            val months = (11 downTo 0).map { now.minusMonths(it.toLong()) }
            fun monthOf(day: Long) = YearMonth.from(LocalDate.ofEpochDay(day))
            val perMonth = months.map { m ->
                val s = services.filter { monthOf(it.dateEpochDay) == m }.sumOf { it.totalCost }
                val f = fuel.filter { monthOf(it.dateEpochDay) == m }.sumOf { (it.pricePerGallon ?: 0.0) * it.gallons }
                m to s + f
            }
            val fmt = DateTimeFormatter.ofPattern("MMM", Locale.US)
            VSpace(20)
            SectionLabel("Per month", trailing = "${Fmt.money(perMonth.last().second)} THIS MONTH")
            VSpace(8)
            BarChart(perMonth.map { (m, v) -> m.format(fmt).take(1) to v })
            VSpace(4)
            Text("Last 12 months, service and fuel. This month is the light bar.", style = Type.bodySmall, color = g.muted)

            VSpace(20)
            SectionLabel("Per year")
            val years = (services.map { LocalDate.ofEpochDay(it.dateEpochDay).year } + fuel.map { LocalDate.ofEpochDay(it.dateEpochDay).year }).distinct().sortedDescending()
            years.forEach { y ->
                val s = services.filter { LocalDate.ofEpochDay(it.dateEpochDay).year == y }.sumOf { it.totalCost }
                val f = fuel.filter { LocalDate.ofEpochDay(it.dateEpochDay).year == y }.sumOf { (it.pricePerGallon ?: 0.0) * it.gallons }
                RowItem("$y", "SERVICE ${Fmt.money(s)} · FUEL ${Fmt.money(f)}", trailing = Fmt.money(s + f))
            }

            // A visit that covers several systems splits its cost evenly between them.
            val bySystem = mutableMapOf<CarSystem, Double>()
            services.forEach { r ->
                val systems = r.systems().ifEmpty { setOf(CarSystem.OTHER) }
                systems.forEach { s -> bySystem[s] = (bySystem[s] ?: 0.0) + r.totalCost / systems.size }
            }
            val systemRows = bySystem.filter { it.value > 0 }.toList().sortedByDescending { it.second }
            if (systemRows.isNotEmpty()) {
                VSpace(20)
                SectionLabel("By system")
                val top = systemRows.first().second
                systemRows.forEach { (s, v) -> ShareBar(s.label, Fmt.money(v), (v / top).toFloat()) }
            }

            val diy = services.filter { it.isDiy }
            val withEstimate = diy.filter { it.shopEstimate != null }
            VSpace(20)
            SectionLabel("DIY savings")
            VSpace(8)
            Panel(Modifier.fillMaxWidth()) {
                Column {
                    val saved = withEstimate.sumOf { (it.shopEstimate ?: 0.0) - it.totalCost }
                    Text(if (withEstimate.isEmpty()) "—" else Fmt.money(saved), style = Type.readout.copy(fontSize = Type.tileValue.fontSize * 1.3f), color = g.text)
                    Text(
                        "${diy.size} DIY JOB${if (diy.size == 1) "" else "S"} · ${Fmt.money(diy.sumOf { it.totalCost })} SPENT",
                        style = Type.monoSmall, color = g.muted,
                    )
                    VSpace(6)
                    Text(
                        if (withEstimate.isEmpty()) "Add what a shop would have charged on a DIY service and the savings show here."
                        else "Shop estimates minus what you spent, on the ${withEstimate.size} DIY job${if (withEstimate.size == 1) "" else "s"} with an estimate.",
                        style = Type.bodySmall, color = g.text2,
                    )
                }
            }
            VSpace(32)
        }
    }
}

@Composable
fun TrendsScreen(nav: NavHostController) {
    val c = LocalContainer.current
    val g = LocalGarage.current
    val scope = rememberCoroutineScope()
    val readings by c.db.wear().all().collectAsState(initial = emptyList())
    val oil by c.db.oil().all().collectAsState(initial = emptyList())
    val services by c.db.services().all().collectAsState(initial = emptyList())
    val snap by c.snapshot.collectAsState(initial = null)
    var adding by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<WearReading?>(null) }
    var threshold by remember { mutableStateOf(c.oilAlertMilesPerQuart.takeIf { it > 0 }?.toString() ?: "") }
    val seriesColors = listOf(g.text, g.muted, g.text2, g.line2)

    Column(Modifier.fillMaxSize()) {
        GarageTopBar("Wear trends", "Measured over miles", onBack = { nav.popBackStack() })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Wear.kinds.groupBy { it.group }.forEach { (group, kinds) ->
                SectionLabel(group, trailing = kinds.first().unit.uppercase())
                VSpace(8)
                val series = kinds.mapIndexedNotNull { i, k ->
                    val pts = readings.filter { it.kind == k.key }.map { it.odometer.toDouble() to it.value }
                    if (pts.isEmpty()) null else ChartSeries(k.label.substringAfter(", "), seriesColors[i % seriesColors.size], pts)
                }
                if (series.isEmpty()) {
                    Text("No measurements yet. Add them when you log brake or tire work, or below.", style = Type.bodySmall, color = g.muted)
                } else {
                    LineChart(series, limit = kinds.first().limit)
                    VSpace(6)
                    kinds.forEach { k ->
                        val mine = readings.filter { it.kind == k.key }
                        val latest = mine.maxByOrNull { it.odometer } ?: return@forEach
                        val at = Wear.projectedOdometer(mine, k.limit)
                        val line = buildString {
                            append("${k.label}: ${Fmt.num(latest.value)} ${k.unit} at ${Fmt.miles(latest.odometer)} mi")
                            if (at != null) append(". At this rate, ${Fmt.num(k.limit, 0)} ${k.unit} around ${Fmt.miles(at)} mi")
                        }
                        Text(line, style = Type.bodySmall, color = if (latest.value <= k.limit) g.accent else g.text2)
                    }
                    VSpace(4)
                    Text("Dashed line: ${Fmt.num(kinds.first().limit, 0)} ${kinds.first().unit}, a common replace-at point. Check the manual for this car's limit.", style = Type.bodySmall, color = g.muted)
                }
                VSpace(20)
            }

            SectionLabel("Oil use", trailing = snap?.milesPerQuart?.let { "${Fmt.miles(it.toInt())} MI / QT NOW" })
            VSpace(8)
            val use = Wear.oilUse(oil, services)
            if (use.isEmpty()) {
                Text("Log oil top-ups and the miles per quart between them shows here.", style = Type.bodySmall, color = g.muted)
            } else {
                val limit = c.oilAlertMilesPerQuart.takeIf { it > 0 }?.toDouble()
                LineChart(listOf(ChartSeries("Miles per quart", g.text, use.map { it.first.toDouble() to it.second })), limit = limit, yLabel = { Fmt.miles(it.toInt()) })
                if (use.size >= 3) {
                    val recent = use.last().second
                    val before = use.dropLast(1).map { it.second }.average()
                    if (recent < before * 0.8) {
                        VSpace(6)
                        Note("Getting worse: the last top-up came after ${Fmt.miles(recent.toInt())} mi per quart, against ${Fmt.miles(before.toInt())} before.", g.amber)
                    }
                }
            }
            VSpace(12)
            Row {
                Field("Warn me below … mi per quart", threshold, { threshold = it.filter { ch -> ch.isDigit() }.take(5) }, Modifier.weight(1f), number = true, mono = true)
            }
            TextButton(onClick = { c.oilAlertMilesPerQuart = threshold.toIntOrNull() ?: 0 }) {
                Text(if (threshold.isBlank()) "Turn the warning off" else "Save warning level", color = g.accent)
            }

            VSpace(16)
            SecondaryButton("Add a measurement", { adding = true }, Modifier.fillMaxWidth())
            if (readings.isNotEmpty()) {
                VSpace(16)
                SectionLabel("All measurements", trailing = "${readings.size}")
                readings.sortedByDescending { it.odometer }.forEach { r ->
                    val k = Wear.kind(r.kind)
                    RowItem(
                        k?.label ?: r.kind,
                        "${Fmt.date(r.dateEpochDay).uppercase()} · ${Fmt.miles(r.odometer)} MI",
                        trailing = "${Fmt.num(r.value)} ${k?.unit ?: ""}",
                        onClick = { deleting = r },
                        showChevron = false,
                    )
                }
            }
            VSpace(32)
        }
    }

    if (adding) {
        var kind by remember { mutableStateOf(Wear.kinds.first().key) }
        var value by remember { mutableStateOf("") }
        var odo by remember { mutableStateOf(snap?.odometer?.toString() ?: "") }
        var date by remember { mutableStateOf(LocalDate.now().toEpochDay()) }
        AlertDialog(
            onDismissRequest = { adding = false },
            title = { Text("Add a measurement") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Wear.kinds.forEach { k -> SelectPill(k.label, kind == k.key, { kind = k.key }) }
                    }
                    VSpace(10)
                    Field("Value (${Wear.kind(kind)?.unit ?: ""})", value, { value = it }, number = true, mono = true)
                    VSpace(8)
                    Field("Odometer", odo, { odo = it }, number = true, mono = true)
                    VSpace(8)
                    DateField("Date", date, { date = it })
                }
            },
            confirmButton = {
                val v = value.toNumberOrNull()
                val o = odo.toNumberOrNull()?.toInt()
                TextButton(enabled = v != null && o != null, onClick = {
                    if (v != null && o != null) scope.launch { c.db.wear().insert(WearReading(dateEpochDay = date, odometer = o, kind = kind, value = v)) }
                    adding = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { adding = false }) { Text("Cancel") } },
        )
    }

    deleting?.let { r ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete this measurement?") },
            text = { Text("${Wear.kind(r.kind)?.label ?: r.kind}, ${Fmt.num(r.value)} at ${Fmt.miles(r.odometer)} mi.") },
            confirmButton = {
                TextButton(onClick = { scope.launch { c.db.wear().delete(r) }; deleting = null }) { Text("Delete", color = g.accent) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
}
