package app.accordgarage.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import app.accordgarage.data.OdometerReading
import app.accordgarage.data.Vehicle
import app.accordgarage.domain.DueState
import app.accordgarage.domain.JobStatus
import app.accordgarage.domain.Zone
import app.accordgarage.ui.LocalContainer
import app.accordgarage.ui.components.CarIllustration
import app.accordgarage.ui.components.Field
import app.accordgarage.ui.components.Fmt
import app.accordgarage.ui.components.RowItem
import app.accordgarage.ui.components.SecondaryButton
import app.accordgarage.ui.components.SectionLabel
import app.accordgarage.ui.components.Tile
import app.accordgarage.ui.components.VSpace
import app.accordgarage.ui.components.stateColor
import app.accordgarage.ui.components.toNumberOrNull
import app.accordgarage.ui.theme.LocalGarage
import app.accordgarage.ui.theme.Type
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun HomeScreen(nav: NavHostController) {
    val c = LocalContainer.current
    val g = LocalGarage.current
    val scope = rememberCoroutineScope()
    val snap by c.snapshot.collectAsState(initial = null)
    val recallStatuses by c.db.settings().recallStatuses().collectAsState(initial = emptyList())
    var recalls by remember { mutableStateOf(c.recalls.cached()) }
    var recallChecked by remember { mutableStateOf(c.recalls.lastCheckedEpochDay()) }
    var checking by remember { mutableStateOf(false) }
    var editOdo by remember { mutableStateOf(false) }

    fun checkRecalls() {
        if (checking) return
        checking = true
        scope.launch {
            c.recalls.refresh(LocalDate.now().toEpochDay())
            recalls = c.recalls.cached()
            recallChecked = c.recalls.lastCheckedEpochDay()
            checking = false
        }
    }

    LaunchedEffect(Unit) {
        val last = c.recalls.lastCheckedEpochDay()
        if (last == null || LocalDate.now().toEpochDay() - last >= 7) checkRecalls()
    }

    val s = snap
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("ACCORD GARAGE", style = Type.label, color = g.muted, modifier = Modifier.weight(1f))
            IconButton(onClick = { nav.navigate("roadside") }) {
                Icon(Icons.Outlined.WarningAmber, contentDescription = "Roadside help", tint = g.text2)
            }
            IconButton(onClick = { nav.navigate("manual") }) {
                Icon(Icons.Outlined.Search, contentDescription = "Search the manual", tint = g.text2)
            }
        }

        Column(Modifier.padding(horizontal = 20.dp)) {
            Text(Vehicle.headline, style = Type.mono, color = g.text2)
            Text(Vehicle.subline, style = Type.mono, color = g.muted)
        }

        val callouts: Map<Zone, String> = if (s == null) emptyMap() else
            listOf(Zone.POWERTRAIN, Zone.FRONT_WHEELS, Zone.REAR_BRAKES).mapNotNull { zone ->
                val items = s.zoneItems(zone)
                when {
                    items.isEmpty() -> null
                    items.size == 1 -> zone to "${zone.label} · ${items.first().actionWord}"
                    else -> zone to "${zone.label} · ${items.size} DUE"
                }
            }.toMap()
        CarIllustration(
            callouts,
            Modifier.padding(horizontal = 14.dp, vertical = 6.dp).clickable { nav.navigate("maintenance") },
        )

        // Odometer
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 14.dp), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text("ODOMETER", style = Type.label, color = g.muted)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(s?.let { Fmt.miles(it.odometer) } ?: "—", style = Type.readout, color = g.text)
                    Spacer(Modifier.width(6.dp))
                    Text("mi", style = Type.mono.copy(fontSize = Type.mono.fontSize * 1.15f), color = g.muted, modifier = Modifier.padding(bottom = 6.dp))
                }
            }
            SecondaryButton("Update", onClick = { editOdo = true })
        }

        // Tiles
        if (s != null) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val m = s.latestMinder
                Tile(
                    "Oil life",
                    m?.oilLifePercent?.let { "$it" } ?: "—",
                    if (m == null) "Enter Minder code" else if (m.code.isNotBlank()) "Minder ${m.code}" else "As of ${Fmt.date(m.dateEpochDay)}",
                    Modifier.weight(1f),
                    unit = if (m?.oilLifePercent != null) "%" else null,
                    valueColor = m?.oilLifePercent?.let { if (it <= 15) g.amber else null },
                    onClick = { nav.navigate("minder") },
                )
                Tile(
                    "Mi / quart",
                    s.milesPerQuart?.let { Fmt.miles(it.toInt()) } ?: "—",
                    if (s.milesPerQuart == null) "Start top-up log" else "Since oil change",
                    Modifier.weight(1f),
                    onClick = { nav.navigate("log?tab=2") },
                )
                val total = s.catchUp.size
                if (s.catchUpDone < total) {
                    Tile(
                        "Catch-up", "${s.catchUpDone}/$total", "Items logged", Modifier.weight(1f),
                        dot = g.amber, onClick = { nav.navigate("maintenance") },
                    )
                } else {
                    val next = s.statuses.filter { it.state == DueState.OK && it.milesLeft != null }.minByOrNull { it.milesLeft!! }
                    Tile(
                        "Next due",
                        next?.milesLeft?.let { Fmt.miles(it) } ?: "—",
                        next?.job?.name ?: "Nothing scheduled",
                        Modifier.weight(1f),
                        unit = if (next != null) "mi" else null,
                        onClick = { nav.navigate("maintenance") },
                    )
                }
            }
        }

        // Recalls banner
        val completed = recallStatuses.filter { it.completedEpochDay != null }.map { it.campaign }.toSet()
        val open = recalls.count { it.campaign !in completed }
        if (recallChecked == null || open > 0) {
            val shape = RoundedCornerShape(14.dp)
            Row(
                Modifier
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp)
                    .fillMaxWidth()
                    .clip(shape)
                    .background(g.amber.copy(alpha = 0.07f))
                    .border(1.dp, g.amber.copy(alpha = 0.4f), shape)
                    .clickable { nav.navigate("recalls") }
                    .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = g.amber, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (recallChecked == null) "Recalls" else "$open recall${if (open == 1) "" else "s"} to review", style = Type.rowTitle, color = g.text)
                    Text(
                        if (recallChecked == null) (if (checking) "Checking NHTSA…" else "Not yet checked") else "Mark each one done once it's fixed",
                        style = Type.bodySmall, color = g.text2,
                    )
                }
                if (recallChecked == null) {
                    TextButton(onClick = { checkRecalls() }, enabled = !checking) { Text("Check", color = g.amber) }
                } else {
                    TextButton(onClick = { nav.navigate("recalls") }) { Text("Review", color = g.amber) }
                }
            }
        }

        // Work list
        if (s != null) {
            VSpace(16)
            Column(Modifier.padding(horizontal = 20.dp)) {
                val catchUpOpen = s.catchUpDone < s.catchUp.size
                if (catchUpOpen) {
                    SectionLabel("100,000-mile catch-up", trailing = "${s.catchUpDone} / ${s.catchUp.size}")
                    s.catchUp.forEach { st -> StatusRow(st) { nav.navigate("job/${st.job.id}") } }
                    val others = s.actionable.filter { !it.job.catchUp }
                    if (others.isNotEmpty()) {
                        VSpace(16)
                        SectionLabel("Due now")
                        others.forEach { st -> StatusRow(st) { nav.navigate("job/${st.job.id}") } }
                    }
                } else {
                    val due = s.actionable
                    SectionLabel("Due now", trailing = if (due.isEmpty()) null else "${due.size}")
                    if (due.isEmpty()) {
                        Text("Nothing due right now.", style = Type.body, color = g.text2, modifier = Modifier.padding(vertical = 12.dp))
                    } else {
                        due.forEach { st -> StatusRow(st) { nav.navigate("job/${st.job.id}") } }
                    }
                }
                VSpace(8)
                RowItem("Full maintenance schedule", onClick = { nav.navigate("maintenance") })
                VSpace(24)
            }
        }
    }

    if (editOdo && s != null) {
        var text by remember { mutableStateOf(s.odometer.toString()) }
        AlertDialog(
            onDismissRequest = { editOdo = false },
            title = { Text("Update odometer") },
            text = {
                Column {
                    Field("Odometer (mi)", text, { text = it }, number = true, mono = true)
                    val v = text.toNumberOrNull()?.toInt()
                    if (v != null && v < s.odometer) {
                        VSpace(8)
                        Text("That's lower than the last reading (${Fmt.miles(s.odometer)}).", style = Type.bodySmall, color = g.amber)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val v = text.toNumberOrNull()?.toInt()
                    if (v != null && v > 0) {
                        scope.launch { c.db.odometer().insert(OdometerReading(dateEpochDay = LocalDate.now().toEpochDay(), odometer = v)) }
                    }
                    editOdo = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editOdo = false }) { Text("Cancel") } },
        )
    }
}

@Composable
fun StatusRow(st: JobStatus, onClick: () -> Unit) {
    val sub = when {
        st.state == DueState.CATCH_UP -> "${st.job.action.uppercase()} · NOT LOGGED"
        st.state == DueState.MINDER -> "MINDER ${st.job.action.uppercase()}"
        st.lastDone != null && st.milesLeft != null && st.milesLeft < 0 -> "${Fmt.miles(-st.milesLeft)} MI OVERDUE"
        st.milesLeft != null -> "IN ${Fmt.miles(st.milesLeft)} MI"
        st.dueDate != null -> "DUE ${Fmt.date(st.dueDate).uppercase()}"
        st.lastDone != null -> "LOGGED ${Fmt.date(st.lastDone.dateEpochDay).uppercase()}"
        else -> st.state.label
    }
    RowItem(st.job.name, sub, onClick = onClick, dot = stateColor(st.state))
}
