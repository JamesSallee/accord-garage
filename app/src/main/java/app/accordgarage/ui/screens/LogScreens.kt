@file:OptIn(ExperimentalLayoutApi::class)

package app.accordgarage.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PhotoLibrary
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
import app.accordgarage.data.Attachment
import app.accordgarage.data.FuelEntry
import app.accordgarage.data.Ocr
import app.accordgarage.data.OilTopUp
import app.accordgarage.data.ReceiptParser
import app.accordgarage.data.ServiceRecord
import app.accordgarage.domain.Catalog
import app.accordgarage.domain.DueEngine
import app.accordgarage.domain.Minder
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

@Composable
fun LogScreen(nav: NavHostController, initialTab: Int) {
    val c = LocalContainer.current
    val g = LocalGarage.current
    var tab by remember { mutableStateOf(initialTab.coerceIn(0, 2)) }
    val services by c.db.services().all().collectAsState(initial = emptyList())
    val fuel by c.db.fuel().all().collectAsState(initial = emptyList())
    val oil by c.db.oil().all().collectAsState(initial = emptyList())
    val snap by c.snapshot.collectAsState(initial = null)

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
                    val thisYear = LocalDate.now().year
                    val yearTotal = services.filter { LocalDate.ofEpochDay(it.dateEpochDay).year == thisYear }.sumOf { it.totalCost }
                    val allTotal = services.sumOf { it.totalCost }
                    SectionLabel("Services", trailing = "${Fmt.money(yearTotal)} THIS YEAR · ${Fmt.money(allTotal)} ALL")
                    if (services.isEmpty()) EmptyState("No services logged yet. Tap + to add the first one.")
                    services.forEach { r ->
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
                    Note("This engine is known for oil consumption. Check the dipstick at every fill-up and log any oil you add; the app tracks miles per quart since the last oil change.")
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

@Composable
fun ServiceEditScreen(nav: NavHostController, id: Long, presetJobs: String) {
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
    var parts by remember { mutableStateOf("") }
    var labor by remember { mutableStateOf("") }
    var minder by remember { mutableStateOf("") }
    var partsText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    val photos = remember { mutableStateListOf<Attachment>() }
    var ocrMessage by remember { mutableStateOf<String?>(null) }
    var pendingPhoto by remember { mutableStateOf<String?>(null) }

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
                parts = r.partsCost?.let { Fmt.num(it, 2) } ?: ""
                labor = r.laborCost?.let { Fmt.num(it, 2) } ?: ""
                minder = r.minderCode
                partsText = r.parts
                notes = r.notes
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
        if (guess.total != null && parts.isBlank() && labor.isBlank()) { parts = Fmt.num(guess.total, 2); filled += "total" }
        if (guess.shop != null && shop.isBlank() && text.isNotBlank()) { shop = guess.shop; diy = false; filled += "shop" }
        if (guess.date != null && existing == null) { date = guess.date.toEpochDay(); filled += "date" }
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
            SectionLabel("What was done")
            VSpace(8)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Catalog.jobs.forEach { job ->
                    SelectPill(job.name, job.id in jobs, {
                        if (job.id in jobs) jobs.remove(job.id) else jobs.add(job.id)
                    })
                }
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
                Field("Parts $", parts, { parts = it }, Modifier.weight(1f), number = true, mono = true)
                Field("Labor $", labor, { labor = it }, Modifier.weight(1f), number = true, mono = true)
            }
            VSpace(12)
            Field("Minder code (optional)", minder, { minder = it.uppercase() }, mono = true, placeholder = "e.g. B13")
            VSpace(12)
            Field("Parts used", partsText, { partsText = it }, singleLine = false, placeholder = "Brand, part number, quantity, price — one per line")
            VSpace(12)
            Field("Notes", notes, { notes = it }, singleLine = false)

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
                            partsCost = parts.toNumberOrNull(),
                            laborCost = labor.toNumberOrNull(),
                            minderCode = minder.trim(),
                            parts = partsText.trim(),
                            notes = notes.trim(),
                        )
                        val result = c.db.services().upsert(record)
                        val savedId = if (record.id == 0L) result else record.id
                        if (record.id == 0L) c.db.attachments().reassign("service", -1L, savedId)
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
                        c.db.services().delete(r)
                        nav.popBackStack()
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text("Delete this service", color = g.accent) }
            }
            VSpace(32)
        }
    }
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
                    nav.popBackStack()
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
