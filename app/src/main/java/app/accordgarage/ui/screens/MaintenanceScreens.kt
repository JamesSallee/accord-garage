package app.accordgarage.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import app.accordgarage.data.IntervalOverride
import app.accordgarage.data.JobSnooze
import app.accordgarage.data.ServiceRecord
import app.accordgarage.data.MinderReading
import app.accordgarage.domain.Catalog
import app.accordgarage.domain.CarSystem
import app.accordgarage.domain.DueState
import app.accordgarage.domain.Minder
import app.accordgarage.ui.LocalContainer
import app.accordgarage.ui.components.ButtonRow
import app.accordgarage.ui.components.DateField
import app.accordgarage.ui.components.Field
import app.accordgarage.ui.components.Fmt
import app.accordgarage.ui.components.GarageTopBar
import app.accordgarage.ui.components.Note
import app.accordgarage.ui.components.Panel
import app.accordgarage.ui.components.PrimaryButton
import app.accordgarage.ui.components.RowItem
import app.accordgarage.ui.components.SecondaryButton
import app.accordgarage.ui.components.SectionLabel
import app.accordgarage.ui.components.SelectPill
import app.accordgarage.ui.components.VSpace
import app.accordgarage.ui.components.stateColor
import app.accordgarage.ui.components.toNumberOrNull
import app.accordgarage.ui.manualSearch
import app.accordgarage.ui.theme.LocalGarage
import app.accordgarage.ui.theme.Type
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun MinderScreen(nav: NavHostController) {
    val c = LocalContainer.current
    val g = LocalGarage.current
    val scope = rememberCoroutineScope()
    val readings by c.db.minder().all().collectAsState(initial = emptyList())
    val snap by c.snapshot.collectAsState(initial = null)

    var main by remember { mutableStateOf<String?>(null) }
    val subs = remember { mutableStateListOf<Int>() }
    var oilLife by remember { mutableStateOf("") }
    val code = Minder.normalize(main, subs.toSet())
    val items = Minder.parse(code)

    Column(Modifier.fillMaxSize()) {
        GarageTopBar("Maintenance Minder", "Decode what the dash shows", onBack = { nav.popBackStack() })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Note("Enter the oil life and the code on the dash (a letter, then any numbers). The app lists the work and adds it to Due now until you log it.")
            VSpace(16)
            Field("Oil life %", oilLife, { oilLife = it.filter { ch -> ch.isDigit() }.take(3) }, number = true, mono = true)
            VSpace(16)
            SectionLabel("Main item")
            VSpace(8)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("A", "B").forEach { m -> SelectPill(m, main == m, { main = if (main == m) null else m }, Modifier.weight(1f)) }
            }
            VSpace(12)
            SectionLabel("Sub-items")
            VSpace(8)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..5).forEach { n ->
                    SelectPill("$n", n in subs, { if (n in subs) subs.remove(n) else subs.add(n) }, Modifier.weight(1f))
                }
            }

            if (items.isNotEmpty()) {
                VSpace(20)
                SectionLabel("Code $code means")
                VSpace(6)
                items.forEach { item ->
                    Panel(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column {
                            Text("${item.code} · ${item.title}", style = Type.rowTitle, color = g.text)
                            VSpace(4)
                            item.details.forEach { Text("— $it", style = Type.bodySmall, color = g.text2) }
                        }
                    }
                }
                VSpace(8)
                Text("Confirm against the Maintenance Minder pages of your owner's manual.", style = Type.bodySmall, color = g.muted)
            }

            VSpace(20)
            val life = oilLife.toIntOrNull()
            ButtonRow {
                SecondaryButton("Save reading", enabled = life != null || code.isNotBlank(), onClick = {
                    scope.launch {
                        val odo = c.snapshotNow().odometer
                        c.db.minder().insert(MinderReading(dateEpochDay = LocalDate.now().toEpochDay(), odometer = odo, oilLifePercent = life, code = code))
                        nav.popBackStack()
                    }
                }, modifier = Modifier.weight(1f))
                PrimaryButton("Log this service", enabled = items.isNotEmpty(), onClick = {
                    val jobs = Minder.jobsFor(code).joinToString(",")
                    nav.navigate("service/0?jobs=${Uri.encode(jobs)}")
                }, modifier = Modifier.weight(1f))
            }
            VSpace(12)
            RowItem("How to reset the Minder", "SEARCH THE OWNER'S MANUAL", onClick = { nav.manualSearch("Maintenance Minder reset") })

            if (readings.isNotEmpty()) {
                VSpace(20)
                SectionLabel("Past readings")
                readings.take(10).forEach { r ->
                    RowItem(
                        listOfNotNull(r.oilLifePercent?.let { "$it% oil life" }, r.code.ifBlank { null }).joinToString(" · ").ifBlank { "Reading" },
                        "${Fmt.date(r.dateEpochDay).uppercase()} · ${Fmt.miles(r.odometer)} MI",
                    )
                }
            }
            snap?.latestMinder?.let { VSpace(8) }
            VSpace(32)
        }
    }
}

@Composable
fun MaintenanceScreen(nav: NavHostController) {
    val c = LocalContainer.current
    val snap by c.snapshot.collectAsState(initial = null)
    var showAll by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        GarageTopBar("Maintenance schedule", "Every job, by system", onBack = { nav.popBackStack() })
        val s = snap ?: return@Column
        // Jobs with no interval and no history are as-needed repairs; keep them out of the way.
        val asNeeded = s.statuses.filter { it.state == DueState.NO_INTERVAL && it.lastDone == null }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            CarSystem.entries.forEach { system ->
                val rows = s.statuses.filter { it.job.system == system && (showAll || it !in asNeeded) }
                if (rows.isNotEmpty()) {
                    VSpace(12)
                    SectionLabel(system.label)
                    rows.forEach { st -> StatusRow(st) { nav.navigate("job/${st.job.id}") } }
                }
            }
            VSpace(12)
            SecondaryButton(
                if (showAll) "Hide as-needed jobs" else "Show ${asNeeded.size} as-needed jobs",
                { showAll = !showAll },
                Modifier.fillMaxWidth(),
            )
            VSpace(32)
        }
    }
}

@Composable
fun JobScreen(nav: NavHostController, jobId: String) {
    val c = LocalContainer.current
    val g = LocalGarage.current
    val scope = rememberCoroutineScope()
    val job = Catalog.job(jobId) ?: return
    val snap by c.snapshot.collectAsState(initial = null)
    val services by c.db.services().all().collectAsState(initial = emptyList())
    var editInterval by remember { mutableStateOf(false) }
    var snoozing by remember { mutableStateOf(false) }
    var elsewhere by remember { mutableStateOf(false) }
    val status = snap?.statuses?.firstOrNull { it.job.id == jobId }
    val history = services.filter { jobId in it.jobs }

    Column(Modifier.fillMaxSize()) {
        GarageTopBar(job.system.label, onBack = { nav.popBackStack() })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Text(job.name, style = Type.title, color = g.text)
            if (status != null) {
                VSpace(6)
                Text(status.state.label, style = Type.mono, color = stateColor(status.state))
                VSpace(16)
                Panel(Modifier.fillMaxWidth()) {
                    Column {
                        SectionLabel("Interval")
                        VSpace(4)
                        val miles = status.intervalMiles?.let { "${Fmt.miles(it)} mi" }
                        val months = status.intervalMonths?.let { "$it months" }
                        Text(listOfNotNull(miles, months).joinToString(" or ").ifBlank { "As needed" }, style = Type.mono.copy(fontSize = Type.tileValue.fontSize * 0.7f), color = g.text)
                        Text(
                            if (status.isCustomInterval) "Your setting" else "Default starting point; check it against the owner's manual and adjust",
                            style = Type.bodySmall, color = g.muted,
                        )
                        if (status.lastDone != null) {
                            VSpace(10)
                            SectionLabel("Last done")
                            Text("${Fmt.date(status.lastDone.dateEpochDay)} at ${Fmt.miles(status.lastDone.odometer)} mi", style = Type.mono, color = g.text2)
                        }
                        if (status.dueOdometer != null || status.dueDate != null) {
                            VSpace(10)
                            SectionLabel("Next due")
                            val parts = listOfNotNull(
                                status.dueOdometer?.let { "${Fmt.miles(it)} mi" },
                                status.dueDate?.let { Fmt.date(it) },
                            )
                            Text(parts.joinToString(" or "), style = Type.mono, color = g.text2)
                            status.projectedDate?.let {
                                Text("At your pace, around ${Fmt.date(it)}", style = Type.bodySmall, color = g.muted)
                            }
                        }
                    }
                }
            }
            VSpace(16)
            ButtonRow {
                PrimaryButton("Log it", { nav.navigate("service/0?jobs=${job.id}") }, Modifier.weight(1f))
                SecondaryButton("Interval", { editInterval = true }, Modifier.weight(1f))
            }
            VSpace(8)
            ButtonRow {
                SecondaryButton("Done elsewhere", { elsewhere = true }, Modifier.weight(1f))
                val snoozedUntil = status?.snoozedUntil
                when {
                    snoozedUntil != null -> SecondaryButton("Un-snooze", {
                        scope.launch { c.db.settings().clearSnooze(job.id) }
                    }, Modifier.weight(1f))
                    status?.isAlerting == true -> SecondaryButton("Snooze", { snoozing = true }, Modifier.weight(1f))
                }
            }
            status?.snoozedUntil?.let {
                VSpace(6)
                Text("Snoozed until ${Fmt.date(it)}. It's off Due now and won't notify until then.", style = Type.bodySmall, color = g.muted)
            }
            job.tutorialId?.let { t ->
                VSpace(8)
                SecondaryButton("Open the guide", { nav.navigate("tutorial/$t") }, Modifier.fillMaxWidth())
            }
            VSpace(20)
            SectionLabel("History", trailing = "${history.size}")
            if (history.isEmpty()) Text("Not logged yet.", style = Type.body, color = g.muted, modifier = Modifier.padding(vertical = 12.dp))
            history.forEach { r ->
                RowItem(
                    Fmt.date(r.dateEpochDay),
                    "${Fmt.miles(r.odometer)} MI · ${if (r.isDiy) "DIY" else r.shop.uppercase()}",
                    onClick = { nav.navigate("service/${r.id}") },
                    trailing = if (r.totalCost > 0) Fmt.money(r.totalCost) else null,
                )
            }
            VSpace(32)
        }
    }

    if (editInterval && status != null) {
        var miles by remember { mutableStateOf(status.intervalMiles?.toString() ?: "") }
        var months by remember { mutableStateOf(status.intervalMonths?.toString() ?: "") }
        AlertDialog(
            onDismissRequest = { editInterval = false },
            title = { Text("${job.name} interval") },
            text = {
                Column {
                    Field("Every … miles", miles, { miles = it }, number = true, mono = true)
                    VSpace(8)
                    Field("or every … months", months, { months = it }, number = true, mono = true)
                    VSpace(8)
                    Text("Leave both empty for as-needed. Whichever comes first applies.", style = Type.bodySmall, color = g.muted)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        c.db.settings().upsertOverride(
                            IntervalOverride(job.id, miles.toNumberOrNull()?.toInt(), months.toNumberOrNull()?.toInt()),
                        )
                    }
                    editInterval = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = {
                    scope.launch { c.db.settings().clearOverride(job.id) }
                    editInterval = false
                }) { Text("Use default") }
            },
        )
    }

    if (snoozing) {
        AlertDialog(
            onDismissRequest = { snoozing = false },
            title = { Text("Snooze ${job.name}") },
            text = {
                Column {
                    listOf("1 week" to 7L, "2 weeks" to 14L, "1 month" to 30L, "3 months" to 91L).forEach { (label, days) ->
                        RowItem(label, "UNTIL ${Fmt.date(LocalDate.now().plusDays(days)).uppercase()}", onClick = {
                            scope.launch { c.db.settings().upsertSnooze(JobSnooze(job.id, LocalDate.now().plusDays(days).toEpochDay())) }
                            snoozing = false
                        }, showChevron = false)
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { snoozing = false }) { Text("Cancel") } },
        )
    }

    if (elsewhere) {
        var date by remember { mutableStateOf(LocalDate.now().toEpochDay()) }
        var odo by remember { mutableStateOf(snap?.odometer?.toString() ?: "") }
        var where by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { elsewhere = false },
            title = { Text("${job.name}: done elsewhere") },
            text = {
                Column {
                    Text("Records it as done so the reminder resets. Add costs and receipts later from the Log if you have them.", style = Type.bodySmall, color = g.muted)
                    VSpace(10)
                    DateField("Date", date, { date = it })
                    VSpace(8)
                    Field("Odometer", odo, { odo = it }, number = true, mono = true)
                    VSpace(8)
                    Field("Where (optional)", where, { where = it }, placeholder = "e.g. dealer, quick lube")
                }
            },
            confirmButton = {
                val o = odo.toNumberOrNull()?.toInt()
                TextButton(enabled = o != null, onClick = {
                    if (o != null) scope.launch {
                        c.db.services().upsert(
                            ServiceRecord(
                                jobIds = job.id,
                                dateEpochDay = date,
                                odometer = o,
                                shop = where.trim().ifBlank { "Elsewhere" },
                                notes = "Marked done elsewhere.",
                            )
                        )
                        c.db.settings().clearSnooze(job.id)
                    }
                    elsewhere = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { elsewhere = false }) { Text("Cancel") } },
        )
    }
}
