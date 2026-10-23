@file:OptIn(ExperimentalLayoutApi::class)

package app.accordgarage.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.room.withTransaction
import app.accordgarage.content.Tutorials
import app.accordgarage.data.Attachment
import app.accordgarage.data.FuelEntry
import app.accordgarage.data.Ocr
import app.accordgarage.data.OilTopUp
import app.accordgarage.data.PartUsed
import app.accordgarage.data.ReceiptLine
import app.accordgarage.data.ReceiptParser
import app.accordgarage.data.ServiceRecord
import app.accordgarage.data.WearReading
import app.accordgarage.domain.CarSystem
import app.accordgarage.domain.Catalog
import app.accordgarage.domain.DueEngine
import app.accordgarage.domain.Minder
import app.accordgarage.domain.Wear
import app.accordgarage.ui.LocalContainer
import app.accordgarage.ui.components.ButtonRow
import app.accordgarage.ui.components.DateField
import app.accordgarage.ui.components.EmptyState
import app.accordgarage.ui.components.Field
import app.accordgarage.ui.components.Fmt
import app.accordgarage.ui.components.GarageTopBar
import app.accordgarage.ui.components.Note
import app.accordgarage.ui.components.PrimaryButton
import app.accordgarage.ui.components.RowItem
import app.accordgarage.ui.components.SecondaryButton
import app.accordgarage.ui.components.SectionLabel
import app.accordgarage.ui.components.SelectPill
import app.accordgarage.ui.components.VSpace
import app.accordgarage.ui.components.toNumberOrNull
import app.accordgarage.ui.theme.LocalGarage
import app.accordgarage.ui.theme.Type
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** The systems a record touches, from its catalog jobs. */
fun ServiceRecord.systems(): Set<CarSystem> = jobs.mapNotNull { Catalog.job(it)?.system }.toSet()

@Composable
fun LogScreen(nav: NavHostController, initialTab: Int) {
    val c = LocalContainer.current
    val g = LocalGarage.current
    var tab by remember { mutableStateOf(initialTab.coerceIn(0, 2)) }
    val services by c.db.services().all().collectAsState(initial = emptyList())
    val parts by c.db.parts().all().collectAsState(initial = emptyList())
    val fuel by c.db.fuel().all().collectAsState(initial = emptyList())
    val oil by c.db.oil().all().collectAsState(initial = emptyList())
    val snap by c.snapshot.collectAsState(initial = null)
    var search by remember { mutableStateOf("") }
    var system by remember { mutableStateOf<CarSystem?>(null) }

    Column(Modifier.fillMaxSize()) {
        GarageTopBar("Log", "Everything done to the car") {
            IconButton(onClick = {
                when (tab) {
                    0 -> nav.navigate("service/0")
                    1 -> nav.navigate("fuel/0")
                    else -> nav.navigate("oil/0")
                }
            }) { Icon(Icons.Outlined.Add, contentDescription = "Add entry", tint = g.accent) }
        }
        Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Services", "Fuel", "Oil top-ups").forEachIndexed { i, label ->
                SelectPill(label, tab == i, { tab = i })
            }
        }
        VSpace(12)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            when (tab) {
                0 -> {
                    RowItem("Costs", "PER MONTH · PER MILE · BY SYSTEM · DIY SAVINGS", onClick = { nav.navigate("costs") }, icon = Icons.Outlined.Payments)
                    RowItem("Wear trends", "BRAKE PADS · TREAD · OIL USE", onClick = { nav.navigate("trends") }, icon = Icons.Outlined.Insights)
                    VSpace(12)
                    Field("Search notes, parts and shops", search, { search = it })
                    val present = CarSystem.entries.filter { s -> services.any { s in it.systems() } }
                    if (present.size > 1) {
                        VSpace(8)
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SelectPill("All", system == null, { system = null })
                            present.forEach { s -> SelectPill(s.label, system == s, { system = if (system == s) null else s }) }
                        }
                    }
                    VSpace(12)
                    val partsByRecord = parts.groupBy { it.recordId }
                    val q = search.trim().lowercase()
                    val shown = services.filter { r ->
                        (system == null || system in r.systems()) && (q.isEmpty() || matches(r, partsByRecord[r.id].orEmpty(), q))
                    }
                    val thisYear = LocalDate.now().year
                    val yearTotal = shown.filter { LocalDate.ofEpochDay(it.dateEpochDay).year == thisYear }.sumOf { it.totalCost }
                    val allTotal = shown.sumOf { it.totalCost }
                    SectionLabel(
                        if (shown.size == services.size) "Services" else "${shown.size} of ${services.size} services",
                        trailing = "${Fmt.money(yearTotal)} THIS YEAR · ${Fmt.money(allTotal)} ALL",
                    )
                    if (services.isEmpty()) EmptyState("No services logged yet. Tap + to add the first one.")
                    else if (shown.isEmpty()) EmptyState("Nothing matches.")
                    shown.forEach { r ->
                        val title = r.title.ifBlank { r.jobs.joinToString(", ") { Catalog.name(it) } }
                        RowItem(
                            title.ifBlank { "Service" },
                            "${Fmt.date(r.dateEpochDay).uppercase()} · ${Fmt.miles(r.odometer)} MI · ${if (r.isDiy) "DIY" else r.shop.uppercase()}",
                            onClick = { nav.navigate("service/${r.id}") },
                            trailing = if (r.totalCost > 0) Fmt.money(r.totalCost) else null,
                        )
                    }
                    VSpace(12)
                    SecondaryButton("Log a service", { nav.navigate("service/0") }, Modifier.fillMaxWidth())
                }
                1 -> {
                    val mpg = DueEngine.mpgByEntry(fuel)
                    val avg = if (mpg.isEmpty()) null else mpg.values.average()
                    SectionLabel("Fuel", trailing = avg?.let { "AVG ${Fmt.num(it)} MPG" })
                    if (fuel.isEmpty()) EmptyState("No fill-ups yet. Log full tanks to track MPG; a sudden drop can point to a dragging brake or a sensor problem.")
                    fuel.forEach { f ->
                        val m = mpg[f.id]
                        val drop = m != null && avg != null && m < avg * 0.85
                        RowItem(
                            "${Fmt.num(f.gallons, 2)} gal" + (f.pricePerGallon?.let { " · ${Fmt.money(it)}/gal" } ?: ""),
                            "${Fmt.date(f.dateEpochDay).uppercase()} · ${Fmt.miles(f.odometer)} MI${if (!f.fullTank) " · PARTIAL" else ""}",
                            onClick = { nav.navigate("fuel/${f.id}") },
                            trailing = m?.let { "${Fmt.num(it)} mpg" },
                            trailingColor = if (drop) g.amber else null,
                        )
                    }
                    VSpace(12)
                    SecondaryButton("Log a fill-up", { nav.navigate("fuel/0") }, Modifier.fillMaxWidth())
                }
                else -> {
                    SectionLabel("Oil top-ups", trailing = snap?.milesPerQuart?.let { "${Fmt.miles(it.toInt())} MI / QT" })
                    Note("This engine is known for oil consumption. Check the dipstick at every fill-up and log any oil you add; the app tracks miles per quart since the last oil change and warns you below your limit (set it in Wear trends).")
                    VSpace(8)
                    if (oil.isEmpty()) EmptyState("No top-ups logged.")
                    oil.forEach { o ->
                        RowItem(
                            "${Fmt.num(o.quarts, 2)} qt",
                            "${Fmt.date(o.dateEpochDay).uppercase()} · ${Fmt.miles(o.odometer)} MI",
                            onClick = { nav.navigate("oil/${o.id}") },
                        )
                    }
                    VSpace(12)
                    SecondaryButton("Log a top-up", { nav.navigate("oil/0") }, Modifier.fillMaxWidth())
                }
            }
            VSpace(24)
        }
    }
}

private fun matches(r: ServiceRecord, parts: List<PartUsed>, q: String): Boolean {
    val haystack = buildString {
        append(r.title).append(' ').append(r.notes).append(' ').append(r.shop).append(' ').append(r.parts).append(' ')
        append(r.minderCode).append(' ')
        r.jobs.forEach { append(Catalog.name(it)).append(' ') }
        parts.forEach { append(it.name).append(' ').append(it.brand).append(' ').append(it.partNumber).append(' ') }
    }.lowercase()
    return q.split(' ').filter { it.isNotBlank() }.all { haystack.contains(it) }
}

@Composable
fun ServiceEditScreen(nav: NavHostController, id: Long, presetJobs: String, presetTutorial: String) {
    val c = LocalContainer.current
    val g = LocalGarage.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var loaded by remember { mutableStateOf(id == 0L) }
    var existing by remember { mutableStateOf<ServiceRecord?>(null) }
    var date by remember { mutableStateOf(LocalDate.now().toEpochDay()) }
    var odometer by remember { mutableStateOf("") }
    val jobs = remember { mutableStateListOf<String>() }
    var title by remember { mutableStateOf("") }
    var shop by remember { mutableStateOf("") }
    var diy by remember { mutableStateOf(true) }
    var partsCost by remember { mutableStateOf("") }
    var labor by remember { mutableStateOf("") }
    var shopEstimate by remember { mutableStateOf("") }
    var minder by remember { mutableStateOf("") }
    var legacyParts by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var tutorialId by remember { mutableStateOf(presetTutorial.ifBlank { null }) }
    val parts = remember { mutableStateListOf<PartUsed>() }
    val wear = remember { mutableStateListOf<String>().apply { repeat(Wear.kinds.size) { add("") } } }
    val photos = remember { mutableStateListOf<Attachment>() }
    var ocrMessage by remember { mutableStateOf<String?>(null) }
    var receiptLines by remember { mutableStateOf<List<ReceiptLine>>(emptyList()) }
    var pendingPhoto by remember { mutableStateOf<String?>(null) }
    var pickJobs by remember { mutableStateOf(false) }
    var editPart by remember { mutableStateOf<Int?>(null) } // index, or -1 for a new part

    LaunchedEffect(id) {
        if (id != 0L) {
            c.db.services().get(id)?.let { r ->
                existing = r
                date = r.dateEpochDay
                odometer = r.odometer.toString()
                jobs.clear(); jobs.addAll(r.jobs)
                title = r.title
                shop = r.shop
                diy = r.isDiy
                partsCost = r.partsCost?.let { Fmt.num(it, 2) } ?: ""
                labor = r.laborCost?.let { Fmt.num(it, 2) } ?: ""
                shopEstimate = r.shopEstimate?.let { Fmt.num(it, 2) } ?: ""
                minder = r.minderCode
                legacyParts = r.parts
                notes = r.notes
                tutorialId = r.tutorialId
            }
            parts.clear(); parts.addAll(c.db.parts().forRecord(id))
            c.db.wear().forService(id).forEach { w ->
                val i = Wear.kinds.indexOfFirst { it.key == w.kind }
                if (i >= 0) wear[i] = Fmt.num(w.value, 1)
            }
            photos.clear(); photos.addAll(c.db.attachments().forOwnerNow("service", id))
            loaded = true
        } else {
            c.db.attachments().forOwnerNow("service", -1L).forEach {
                c.attachments.delete(it.fileName); c.db.attachments().delete(it)
            }
            val snap = c.snapshotNow()
            odometer = snap.odometer.toString()
            presetJobs.split(',').filter { it.isNotBlank() }.forEach { if (it !in jobs) jobs.add(it) }
            snap.latestMinder?.let { m ->
                if (m.code.isNotBlank() && jobs.any { it in Minder.jobsFor(m.code) }) minder = m.code
            }
            tutorialId?.let { Tutorials.get(it) }?.parts?.forEach { name ->
                parts.add(PartUsed(recordId = 0, name = name))
            }
        }
        Unit
    }

    suspend fun addPhoto(name: String) {
        val text = runCatching { Ocr.read(context, c.attachments.file(name)) }.getOrDefault("")
        // Temporary owner id -1 until the record is saved.
        val ownerId = existing?.id ?: -1L
        val a = Attachment(ownerType = "service", ownerId = ownerId, fileName = name, ocrText = text)
        val newId = c.db.attachments().insert(a)
        photos.add(a.copy(id = newId))
        val guess = ReceiptParser.parse(text)
        val filled = mutableListOf<String>()
        if (guess.total != null && partsCost.isBlank() && labor.isBlank()) { partsCost = Fmt.num(guess.total, 2); filled += "total" }
        if (guess.shop != null && shop.isBlank() && text.isNotBlank()) { shop = guess.shop; diy = false; filled += "shop" }
        if (guess.date != null && existing == null) { date = guess.date.toEpochDay(); filled += "date" }
        receiptLines = ReceiptParser.lineItems(text)
        ocrMessage = if (filled.isEmpty()) "Photo added." else "Read the receipt: filled ${filled.joinToString(", ")}. Check them."
    }

    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val name = pendingPhoto
        pendingPhoto = null
        if (ok && name != null) scope.launch { addPhoto(name) } else if (name != null) c.attachments.delete(name)
    }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) scope.launch { addPhoto(c.attachments.copyIn(uri)) }
    }

    Column(Modifier.fillMaxSize()) {
        GarageTopBar(if (id == 0L) "New service" else "Edit service", onBack = { nav.popBackStack() })
        if (!loaded) return@Column
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            SectionLabel("What was done", trailing = if (jobs.isEmpty()) null else "${jobs.size}")
            VSpace(8)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (Catalog.common + jobs.filter { it !in Catalog.common }).forEach { jobId ->
                    SelectPill(Catalog.name(jobId), jobId in jobs, {
                        if (jobId in jobs) jobs.remove(jobId) else jobs.add(jobId)
                    })
                }
                SelectPill("More jobs…", false, { pickJobs = true })
            }
            VSpace(12)
            Field("Title (optional)", title, { title = it }, placeholder = "e.g. Minder B13 service")
            VSpace(12)
            ButtonRow {
                DateField("Date", date, { date = it }, Modifier.weight(1f))
                Field("Odometer", odometer, { odometer = it }, Modifier.weight(1f), number = true, mono = true)
            }
            VSpace(16)
            SectionLabel("Who did it")
            VSpace(8)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectPill("Me (DIY)", diy, { diy = true })
                SelectPill("A shop", !diy, { diy = false })
            }
            if (!diy) {
                VSpace(8)
                Field("Shop", shop, { shop = it })
            }
            VSpace(16)
            SectionLabel("Cost")
            VSpace(8)
            ButtonRow {
                Field("Parts $", partsCost, { partsCost = it }, Modifier.weight(1f), number = true, mono = true)
                Field("Labor $", labor, { labor = it }, Modifier.weight(1f), number = true, mono = true)
            }
            val partsSum = parts.mapNotNull { it.total }.sum()
            if (partsSum > 0 && partsCost.toNumberOrNull() != partsSum) {
                TextButton(onClick = { partsCost = Fmt.num(partsSum, 2) }) { Text("Use parts total ${Fmt.money(partsSum)}", color = g.accent) }
            }
            if (diy) {
                VSpace(8)
                Field("A shop would charge $ (optional)", shopEstimate, { shopEstimate = it }, number = true, mono = true, placeholder = "For the DIY savings figure")
            }
            VSpace(12)
            Field("Minder code (optional)", minder, { minder = it.uppercase() }, mono = true, placeholder = "e.g. B13")

            VSpace(16)
            SectionLabel("Parts and fluids", trailing = if (parts.isEmpty()) null else "${parts.size}")
            parts.forEachIndexed { i, p ->
                RowItem(
                    p.name.ifBlank { "Part" },
                    listOfNotNull(
                        p.brand.ifBlank { null },
                        p.partNumber.ifBlank { null },
                        "${Fmt.num(p.quantity, if (p.quantity % 1.0 == 0.0) 0 else 2)}${if (p.unit.isNotBlank()) " ${p.unit}" else ""}",
                    ).joinToString(" · ").uppercase(),
                    onClick = { editPart = i },
                    trailing = p.total?.let { Fmt.money(it) },
                )
            }
            VSpace(8)
            SecondaryButton("Add a part or fluid", { editPart = -1 }, Modifier.fillMaxWidth())
            if (legacyParts.isNotBlank()) {
                VSpace(12)
                Field("Earlier parts notes", legacyParts, { legacyParts = it }, singleLine = false)
            }

            val wearShown = Wear.kinds.indices.filter { i -> Wear.kinds[i].jobs.any { it in jobs } || wear[i].isNotBlank() }
            if (wearShown.isNotEmpty()) {
                VSpace(16)
                SectionLabel("Measurements (optional)")
                VSpace(8)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), maxItemsInEachRow = 2) {
                    wearShown.forEach { i ->
                        val k = Wear.kinds[i]
                        Field("${k.label} (${k.unit})", wear[i], { wear[i] = it }, Modifier.weight(1f), number = true, mono = true)
                    }
                }
            }

            VSpace(12)
            Field("Notes", notes, { notes = it }, singleLine = false)
            tutorialId?.let { Tutorials.get(it) }?.let { t ->
                VSpace(8)
                RowItem("Guide used", t.title.uppercase(), onClick = { nav.navigate("tutorial/${t.id}") })
            }

            VSpace(16)
            SectionLabel("Photos and receipts")
            VSpace(8)
            ButtonRow {
                SecondaryButton("Camera", {
                    val (name, uri) = c.attachments.newPhoto()
                    pendingPhoto = name
                    takePhoto.launch(uri)
                }, Modifier.weight(1f))
                SecondaryButton("Gallery", {
                    pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }, Modifier.weight(1f))
            }
            ocrMessage?.let { VSpace(8); Note(it) }
            if (receiptLines.isNotEmpty()) {
                TextButton(onClick = {
                    receiptLines.forEach { parts.add(PartUsed(recordId = 0, name = it.name, price = it.price)) }
                    receiptLines = emptyList()
                }) { Text("Add ${receiptLines.size} receipt line${if (receiptLines.size == 1) "" else "s"} as parts", color = g.accent) }
            }
            if (photos.isNotEmpty()) {
                VSpace(8)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    photos.forEach { a ->
                        PhotoThumb(c.attachments.file(a.fileName).path) {
                            scope.launch {
                                c.db.attachments().delete(a)
                                c.attachments.delete(a.fileName)
                                photos.remove(a)
                            }
                        }
                    }
                }
            }

            VSpace(20)
            val odo = odometer.toNumberOrNull()?.toInt()
            PrimaryButton(
                "Save",
                enabled = odo != null && (jobs.isNotEmpty() || title.isNotBlank()),
                onClick = {
                    scope.launch {
                        val record = ServiceRecord(
                            id = existing?.id ?: 0L,
                            jobIds = jobs.joinToString(","),
                            title = title.trim(),
                            dateEpochDay = date,
                            odometer = odo ?: 0,
                            shop = if (diy) "" else shop.trim().ifBlank { "Shop" },
                            partsCost = partsCost.toNumberOrNull() ?: partsSum.takeIf { it > 0 },
                            laborCost = labor.toNumberOrNull(),
                            minderCode = minder.trim(),
                            parts = legacyParts.trim(),
                            notes = notes.trim(),
                            shopEstimate = if (diy) shopEstimate.toNumberOrNull() else null,
                            tutorialId = tutorialId,
                        )
                        c.db.withTransaction {
                            val result = c.db.services().upsert(record)
                            val savedId = if (record.id == 0L) result else record.id
                            if (record.id == 0L) c.db.attachments().reassign("service", -1L, savedId)
                            c.db.parts().deleteFor(savedId)
                            c.db.parts().insertAll(parts.filter { it.name.isNotBlank() }.map { it.copy(id = 0, recordId = savedId) })
                            jobs.forEach { c.db.settings().clearSnooze(it) }
                            c.db.wear().deleteForService(savedId)
                            c.db.wear().insertAll(
                                Wear.kinds.indices.mapNotNull { i ->
                                    wear[i].toNumberOrNull()?.let { v ->
                                        WearReading(dateEpochDay = date, odometer = odo ?: 0, kind = Wear.kinds[i].key, value = v, serviceId = savedId)
                                    }
                                }
                            )
                        }
                        nav.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                tall = true,
            )
            if (existing != null) {
                VSpace(8)
                TextButton(onClick = {
                    scope.launch {
                        val r = existing ?: return@launch
                        c.db.attachments().forOwnerNow("service", r.id).forEach {
                            c.attachments.delete(it.fileName); c.db.attachments().delete(it)
                        }
                        c.db.parts().deleteFor(r.id)
                        c.db.wear().deleteForService(r.id)
                        c.db.services().delete(r)
                        nav.popBackStack()
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text("Delete this service", color = g.accent) }
            }
            VSpace(32)
        }
    }

    if (pickJobs) {
        JobPickerDialog(jobs.toSet(), onDone = { picked ->
            jobs.clear(); jobs.addAll(picked)
            pickJobs = false
        }, onDismiss = { pickJobs = false })
    }

    editPart?.let { index ->
        val start = if (index >= 0) parts.getOrNull(index) else null
        PartDialog(start, onSave = { p ->
            if (index >= 0 && index < parts.size) parts[index] = p else parts.add(p)
            editPart = null
        }, onDelete = if (start != null) ({ parts.removeAt(index); editPart = null }) else null, onDismiss = { editPart = null })
    }
}

/** Every catalog job, grouped by system, with a filter box. */
@Composable
private fun JobPickerDialog(initial: Set<String>, onDone: (List<String>) -> Unit, onDismiss: () -> Unit) {
    val g = LocalGarage.current
    val picked = remember { mutableStateListOf<String>().apply { addAll(initial) } }
    var filter by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("What was done") },
        text = {
            Column {
                Field("Filter", filter, { filter = it })
                VSpace(8)
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    val q = filter.trim().lowercase()
                    CarSystem.entries.forEach { system ->
                        val jobs = Catalog.jobs.filter { it.system == system && (q.isEmpty() || it.name.lowercase().contains(q)) }
                        if (jobs.isNotEmpty()) {
                            VSpace(10)
                            SectionLabel(system.label)
                            VSpace(6)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                jobs.forEach { job ->
                                    SelectPill(job.name, job.id in picked, {
                                        if (job.id in picked) picked.remove(job.id) else picked.add(job.id)
                                    })
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onDone(picked.toList()) }) { Text("Done", color = g.accent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun PartDialog(start: PartUsed?, onSave: (PartUsed) -> Unit, onDelete: (() -> Unit)?, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(start?.name ?: "") }
    var brand by remember { mutableStateOf(start?.brand ?: "") }
    var number by remember { mutableStateOf(start?.partNumber ?: "") }
    var qty by remember { mutableStateOf(start?.quantity?.let { Fmt.num(it, if (it % 1.0 == 0.0) 0 else 2) } ?: "1") }
    var unit by remember { mutableStateOf(start?.unit ?: "") }
    var price by remember { mutableStateOf(start?.price?.let { Fmt.num(it, 2) } ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (start == null) "Add a part or fluid" else "Edit part") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Field("What it is", name, { name = it }, placeholder = "e.g. Oil filter, or 5W-20 oil")
                VSpace(8)
                Field("Brand", brand, { brand = it })
                VSpace(8)
                Field("Part number", number, { number = it }, mono = true)
                VSpace(8)
                ButtonRow {
                    Field("Quantity", qty, { qty = it }, Modifier.weight(1f), number = true, mono = true)
                    Field("Unit", unit, { unit = it }, Modifier.weight(1f), placeholder = "qt, ea")
                }
                VSpace(8)
                Field("Price each $", price, { price = it }, number = true, mono = true)
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = {
                onSave(
                    PartUsed(
                        id = start?.id ?: 0,
                        recordId = start?.recordId ?: 0,
                        name = name.trim(),
                        brand = brand.trim(),
                        partNumber = number.trim(),
                        quantity = qty.toNumberOrNull() ?: 1.0,
                        unit = unit.trim(),
                        price = price.toNumberOrNull(),
                    )
                )
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = { onDelete?.invoke() ?: onDismiss() }) { Text(if (onDelete != null) "Remove" else "Cancel") }
        },
    )
}

@Composable
fun PhotoThumb(path: String, onRemove: (() -> Unit)? = null) {
    val g = LocalGarage.current
    var bmp by remember(path) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(path) {
        bmp = withContext(Dispatchers.IO) {
            runCatching {
                val opts = BitmapFactory.Options().apply { inSampleSize = 8 }
                BitmapFactory.decodeFile(path, opts)
            }.getOrNull()
        }
    }
    Box(Modifier.size(96.dp).clip(RoundedCornerShape(10.dp))) {
        bmp?.let { Image(it.asImageBitmap(), contentDescription = "Photo", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
            ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("…", color = g.muted) }
        if (onRemove != null) {
            IconButton(onClick = onRemove, modifier = Modifier.align(Alignment.TopEnd).size(36.dp)) {
                Icon(Icons.Outlined.Close, contentDescription = "Remove photo", tint = g.text)
            }
        }
    }
}

@Composable
fun FuelEditScreen(nav: NavHostController, id: Long) {
    val c = LocalContainer.current
    val g = LocalGarage.current
    val scope = rememberCoroutineScope()
    var existing by remember { mutableStateOf<FuelEntry?>(null) }
    var date by remember { mutableStateOf(LocalDate.now().toEpochDay()) }
    var odometer by remember { mutableStateOf("") }
    var gallons by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var full by remember { mutableStateOf(true) }
    var station by remember { mutableStateOf("") }
    var askOil by remember { mutableStateOf(false) }

    LaunchedEffect(id) {
        if (id != 0L) {
            c.db.fuel().get(id)?.let {
                existing = it; date = it.dateEpochDay; odometer = it.odometer.toString(); gallons = Fmt.num(it.gallons, 3)
                price = it.pricePerGallon?.let { p -> Fmt.num(p, 3) } ?: ""; full = it.fullTank; station = it.station
            }
        } else {
            odometer = c.snapshotNow().odometer.toString()
        }
        Unit
    }

    Column(Modifier.fillMaxSize()) {
        GarageTopBar(if (id == 0L) "New fill-up" else "Edit fill-up", onBack = { nav.popBackStack() })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            ButtonRow {
                DateField("Date", date, { date = it }, Modifier.weight(1f))
                Field("Odometer", odometer, { odometer = it }, Modifier.weight(1f), number = true, mono = true)
            }
            VSpace(12)
            ButtonRow {
                Field("Gallons", gallons, { gallons = it }, Modifier.weight(1f), number = true, mono = true)
                Field("Price / gal", price, { price = it }, Modifier.weight(1f), number = true, mono = true)
            }
            VSpace(12)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectPill("Full tank", full, { full = true })
                SelectPill("Partial", !full, { full = false })
            }
            VSpace(12)
            Field("Station (optional)", station, { station = it })
            VSpace(20)
            val odo = odometer.toNumberOrNull()?.toInt()
            val gal = gallons.toNumberOrNull()
            PrimaryButton("Save", enabled = odo != null && gal != null && gal > 0, onClick = {
                scope.launch {
                    c.db.fuel().upsert(FuelEntry(existing?.id ?: 0L, date, odo ?: 0, gal ?: 0.0, price.toNumberOrNull(), full, station.trim()))
                    // Known oil burner: every new fill-up is a reminder to check the dipstick.
                    if (existing == null) askOil = true else nav.popBackStack()
                }
            }, modifier = Modifier.fillMaxWidth(), tall = true)
            existing?.let { e ->
                VSpace(8)
                TextButton(onClick = { scope.launch { c.db.fuel().delete(e); nav.popBackStack() } }, modifier = Modifier.fillMaxWidth()) {
                    Text("Delete this fill-up", color = g.accent)
                }
            }
        }
    }

    if (askOil) {
        AlertDialog(
            onDismissRequest = { askOil = false; nav.popBackStack() },
            title = { Text("Check the oil") },
            text = { Text("With the engine off for a few minutes, pull the dipstick. This engine can burn oil, so a check at every fill-up catches it early.") },
            confirmButton = {
                TextButton(onClick = {
                    askOil = false
                    nav.popBackStack()
                    nav.navigate("oil/0")
                }) { Text("I added oil", color = g.accent) }
            },
            dismissButton = { TextButton(onClick = { askOil = false; nav.popBackStack() }) { Text("Level is fine") } },
        )
    }
}

@Composable
fun OilEditScreen(nav: NavHostController, id: Long) {
    val c = LocalContainer.current
    val g = LocalGarage.current
    val scope = rememberCoroutineScope()
    var existing by remember { mutableStateOf<OilTopUp?>(null) }
    var date by remember { mutableStateOf(LocalDate.now().toEpochDay()) }
    var odometer by remember { mutableStateOf("") }
    var quarts by remember { mutableStateOf("") }

    LaunchedEffect(id) {
        if (id != 0L) {
            c.db.oil().get(id)?.let {
                existing = it; date = it.dateEpochDay; odometer = it.odometer.toString(); quarts = Fmt.num(it.quarts, 2)
            }
        } else {
            odometer = c.snapshotNow().odometer.toString()
        }
        Unit
    }

    Column(Modifier.fillMaxSize()) {
        GarageTopBar(if (id == 0L) "New oil top-up" else "Edit oil top-up", onBack = { nav.popBackStack() })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            ButtonRow {
                DateField("Date", date, { date = it }, Modifier.weight(1f))
                Field("Odometer", odometer, { odometer = it }, Modifier.weight(1f), number = true, mono = true)
            }
            VSpace(12)
            Field("Quarts added", quarts, { quarts = it }, number = true, mono = true)
            VSpace(8)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("0.25", "0.5", "1").forEach { q -> SelectPill("$q qt", quarts == q, { quarts = q }) }
            }
            VSpace(20)
            val odo = odometer.toNumberOrNull()?.toInt()
            val qt = quarts.toNumberOrNull()
            PrimaryButton("Save", enabled = odo != null && qt != null && qt > 0, onClick = {
                scope.launch {
                    c.db.oil().upsert(OilTopUp(existing?.id ?: 0L, date, odo ?: 0, qt ?: 0.0))
                    nav.popBackStack()
                }
            }, modifier = Modifier.fillMaxWidth(), tall = true)
            existing?.let { e ->
                VSpace(8)
                TextButton(onClick = { scope.launch { c.db.oil().delete(e); nav.popBackStack() } }, modifier = Modifier.fillMaxWidth()) {
                    Text("Delete this top-up", color = g.accent)
                }
            }
        }
    }
}
