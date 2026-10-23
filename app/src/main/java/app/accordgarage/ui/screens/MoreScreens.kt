@file:OptIn(ExperimentalLayoutApi::class)

package app.accordgarage.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import app.accordgarage.BuildConfigInfo
import app.accordgarage.content.Roadside
import app.accordgarage.content.SpecDef
import app.accordgarage.content.Specs
import app.accordgarage.data.Attachment
import app.accordgarage.data.DocumentItem
import app.accordgarage.data.Recall
import app.accordgarage.data.RecallStatus
import app.accordgarage.data.SpecValue
import app.accordgarage.data.Vehicle
import app.accordgarage.ui.LocalContainer
import app.accordgarage.ui.components.ButtonRow
import app.accordgarage.ui.components.DateField
import app.accordgarage.ui.components.EmptyState
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
import app.accordgarage.ui.components.Tag
import app.accordgarage.ui.components.VSpace
import app.accordgarage.ui.manualSearch
import app.accordgarage.ui.openLink
import app.accordgarage.ui.theme.LocalGarage
import app.accordgarage.ui.theme.Type
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun MoreScreen(nav: NavHostController, lightTheme: Boolean, onToggleTheme: (Boolean) -> Unit) {
    val g = LocalGarage.current
    Column(Modifier.fillMaxSize()) {
        GarageTopBar("More")
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            RowItem("Maintenance schedule", onClick = { nav.navigate("maintenance") }, icon = Icons.Outlined.ListAlt)
            RowItem("Maintenance Minder", onClick = { nav.navigate("minder") }, icon = Icons.Outlined.Speed)
            RowItem("Costs", onClick = { nav.navigate("costs") }, icon = Icons.Outlined.Payments)
            RowItem("Wear trends", onClick = { nav.navigate("trends") }, icon = Icons.Outlined.Insights)
            RowItem("Warning lights", onClick = { nav.navigate("lights") }, icon = Icons.Outlined.ErrorOutline)
            RowItem("Recalls", onClick = { nav.navigate("recalls") }, icon = Icons.Outlined.WarningAmber)
            RowItem("Quick specs", onClick = { nav.navigate("specs") }, icon = Icons.Outlined.Tune)
            RowItem("Documents", onClick = { nav.navigate("documents") }, icon = Icons.Outlined.Description)
            RowItem("Roadside help", onClick = { nav.navigate("roadside") }, icon = Icons.Outlined.Build)
            RowItem("Backup and restore", onClick = { nav.navigate("backup") }, icon = Icons.Outlined.Backup)
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.WbSunny, contentDescription = null, tint = g.text2)
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Text("Sunlight mode", style = Type.rowTitle, color = g.text)
                    Text("HIGH-CONTRAST LIGHT THEME", style = Type.monoSmall, color = g.muted)
                }
                Switch(
                    checked = lightTheme,
                    onCheckedChange = onToggleTheme,
                    colors = SwitchDefaults.colors(checkedTrackColor = g.accentFill, uncheckedTrackColor = g.surface2, uncheckedBorderColor = g.line2),
                )
            }
            RowItem("About this app", onClick = { nav.navigate("about") }, icon = Icons.Outlined.Info)
            VSpace(32)
        }
    }
}

// ---------------- Recalls ----------------

@Composable
fun RecallsScreen(nav: NavHostController) {
    val c = LocalContainer.current
    val g = LocalGarage.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var recalls by remember { mutableStateOf(c.recalls.cached()) }
    var checked by remember { mutableStateOf(c.recalls.lastCheckedEpochDay()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val statuses by c.db.settings().recallStatuses().collectAsState(initial = emptyList())

    fun refresh() {
        busy = true; error = null
        scope.launch {
            c.recalls.refresh(LocalDate.now().toEpochDay()).onFailure { error = "Couldn't reach NHTSA. Check your connection and try again." }
            recalls = c.recalls.cached(); checked = c.recalls.lastCheckedEpochDay(); busy = false
        }
    }
    LaunchedEffect(Unit) { if (checked == null) refresh() }

    Column(Modifier.fillMaxSize()) {
        GarageTopBar("Recalls", checked?.let { "Checked ${Fmt.date(it)}" } ?: "Not yet checked", onBack = { nav.popBackStack() })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Note("NHTSA lists recalls for every 2008 Accord, so some may not apply to this exact car. For a definite answer, check the VIN on nhtsa.gov, then mark each recall done once a dealer has fixed it.")
            VSpace(12)
            ButtonRow {
                SecondaryButton(if (busy) "Checking…" else "Check again", { refresh() }, Modifier.weight(1f), enabled = !busy)
                PrimaryButton("Check my VIN", { context.openLink(Vehicle.NHTSA_VIN_LOOKUP) }, Modifier.weight(1f))
            }
            error?.let { VSpace(8); Note(it, g.accent) }
            VSpace(16)
            if (recalls.isEmpty() && checked != null) EmptyState("NHTSA lists no recalls for the 2008 Accord.")
            val statusMap = statuses.associateBy { it.campaign }
            recalls.forEach { r -> RecallCard(r, statusMap[r.campaign]) { done ->
                scope.launch { c.db.settings().upsertRecall(RecallStatus(r.campaign, if (done) LocalDate.now().toEpochDay() else null)) }
            } }
            VSpace(32)
        }
    }
}

@Composable
private fun RecallCard(r: Recall, status: RecallStatus?, onDone: (Boolean) -> Unit) {
    val g = LocalGarage.current
    var expanded by remember { mutableStateOf(false) }
    val done = status?.completedEpochDay != null
    Panel(Modifier.fillMaxWidth().padding(vertical = 4.dp), onClick = { expanded = !expanded }) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(r.campaign, style = Type.mono, color = g.muted, modifier = Modifier.weight(1f))
                if (done) Tag("Done ${Fmt.date(status!!.completedEpochDay!!)}", g.green) else Tag("Open", g.amber)
            }
            VSpace(4)
            Text(r.component.lowercase().replaceFirstChar { it.uppercase() }, style = Type.rowTitle, color = g.text)
            VSpace(4)
            Text(r.summary, style = Type.bodySmall, color = g.text2, maxLines = if (expanded) Int.MAX_VALUE else 3)
            if (expanded) {
                if (r.consequence.isNotBlank()) { VSpace(8); SectionLabel("Risk"); Text(r.consequence, style = Type.bodySmall, color = g.text2) }
                if (r.remedy.isNotBlank()) { VSpace(8); SectionLabel("Remedy"); Text(r.remedy, style = Type.bodySmall, color = g.text2) }
            }
            VSpace(8)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = done, onCheckedChange = onDone, colors = CheckboxDefaults.colors(checkedColor = g.green, uncheckedColor = g.line2))
                Text("Fixed on my car", style = Type.body, color = g.text2)
            }
        }
    }
}

// ---------------- Quick specs ----------------

@Composable
fun SpecsScreen(nav: NavHostController) {
    val c = LocalContainer.current
    val g = LocalGarage.current
    val scope = rememberCoroutineScope()
    val stored by c.db.settings().specs().collectAsState(initial = emptyList())
    val map = stored.associateBy { it.key }
    var editing by remember { mutableStateOf<SpecDef?>(null) }

    Column(Modifier.fillMaxSize()) {
        GarageTopBar("Quick specs", "Tap a row to fill in or confirm it", onBack = { nav.popBackStack() })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Note("Values with a ✓ were checked against the 2008 Accord Coupe owner's manual. Values marked CHECK or ADD still need confirming against the manual or the label named.")
            Specs.groups.forEach { (group, defs) ->
                VSpace(16)
                SectionLabel(group)
                defs.forEach { d ->
                    val sv = map[d.key]
                    val value = sv?.value ?: d.value
                    val confirmed = d.fromVin || (sv?.confirmed ?: d.fromManual)
                    val shown = when {
                        value.isBlank() -> "—"
                        d.sensitive -> "••••"
                        else -> value
                    }
                    Column(Modifier.fillMaxWidth()) {
                        RowItem(
                            d.label,
                            if (value.isBlank()) (d.source.ifBlank { "FROM THE MANUAL" }).uppercase() else shown,
                            onClick = if (d.fromVin) null else ({ editing = d }),
                            trailing = when {
                                d.fromVin -> "VIN"
                                confirmed -> "✓"
                                value.isNotBlank() -> "CHECK"
                                else -> "ADD"
                            },
                            trailingColor = when {
                                confirmed -> g.green
                                value.isNotBlank() -> g.amber
                                else -> g.muted
                            },
                        )
                    }
                }
            }
            VSpace(32)
        }
    }

    editing?.let { d ->
        val sv = map[d.key]
        var value by remember(d) { mutableStateOf(sv?.value ?: d.value) }
        var confirmed by remember(d) { mutableStateOf(sv?.confirmed ?: d.fromManual) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(d.label) },
            text = {
                Column {
                    Field("Value", value, { value = it })
                    if (d.source.isNotBlank()) { VSpace(6); Text("Source: ${d.source}", style = Type.bodySmall, color = g.muted) }
                    VSpace(6)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = confirmed, onCheckedChange = { confirmed = it }, colors = CheckboxDefaults.colors(checkedColor = g.green))
                        Text("Confirmed against the manual or label", style = Type.bodySmall, color = g.text2)
                    }
                    d.manualQuery?.let { q ->
                        TextButton(onClick = { editing = null; nav.manualSearch(q) }) { Text("Search the manual for “$q”") }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { c.db.settings().upsertSpec(SpecValue(d.key, value.trim(), confirmed)) }
                    editing = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } },
        )
    }
}

// ---------------- Documents ----------------

private val documentKinds = listOf("Registration", "Insurance", "Inspection", "Title", "Warranty", "Receipt", "Other")

@Composable
fun DocumentsScreen(nav: NavHostController) {
    val c = LocalContainer.current
    val g = LocalGarage.current
    val docs by c.db.documents().all().collectAsState(initial = emptyList())
    val today = LocalDate.now().toEpochDay()
    Column(Modifier.fillMaxSize()) {
        GarageTopBar("Documents", "Registration, insurance, inspection and more", onBack = { nav.popBackStack() })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            if (docs.isEmpty()) EmptyState("Add your registration, insurance card and inspection date. The app reminds you 30 days before anything expires.")
            docs.forEach { d ->
                val exp = d.expiryEpochDay
                val days = exp?.let { it - today }
                RowItem(
                    d.title.ifBlank { d.kind },
                    listOfNotNull(d.kind.uppercase(), exp?.let { "EXPIRES ${Fmt.date(it).uppercase()}" }).joinToString(" · "),
                    onClick = { nav.navigate("document/${d.id}") },
                    dot = when {
                        days == null -> null
                        days < 0 -> g.accent
                        days <= 30 -> g.amber
                        else -> g.green
                    },
                )
            }
            VSpace(16)
            PrimaryButton("Add a document", { nav.navigate("document/0") }, Modifier.fillMaxWidth())
            VSpace(32)
        }
    }
}

@Composable
fun DocumentEditScreen(nav: NavHostController, id: Long) {
    val c = LocalContainer.current
    val g = LocalGarage.current
    val scope = rememberCoroutineScope()
    var existing by remember { mutableStateOf<DocumentItem?>(null) }
    var kind by remember { mutableStateOf(documentKinds.first()) }
    var title by remember { mutableStateOf("") }
    var hasExpiry by remember { mutableStateOf(false) }
    var expiry by remember { mutableStateOf(LocalDate.now().plusYears(1).toEpochDay()) }
    var notes by remember { mutableStateOf("") }
    val photos = remember { mutableStateListOf<Attachment>() }
    var pendingPhoto by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(id) {
        if (id != 0L) {
            c.db.documents().get(id)?.let { d ->
                existing = d; kind = d.kind; title = d.title; notes = d.notes
                hasExpiry = d.expiryEpochDay != null
                d.expiryEpochDay?.let { expiry = it }
            }
            photos.clear(); photos.addAll(c.db.attachments().forOwnerNow("document", id))
        } else {
            c.db.attachments().forOwnerNow("document", -1L).forEach { c.attachments.delete(it.fileName); c.db.attachments().delete(it) }
        }
        Unit
    }

    suspend fun addPhoto(name: String) {
        val a = Attachment(ownerType = "document", ownerId = existing?.id ?: -1L, fileName = name)
        photos.add(a.copy(id = c.db.attachments().insert(a)))
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
        GarageTopBar(if (id == 0L) "New document" else "Edit document", onBack = { nav.popBackStack() })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            SectionLabel("Kind")
            VSpace(8)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                documentKinds.forEach { k -> SelectPill(k, kind == k, { kind = k }) }
            }
            VSpace(12)
            Field("Title", title, { title = it }, placeholder = "e.g. NY registration")
            VSpace(12)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = hasExpiry, onCheckedChange = { hasExpiry = it }, colors = CheckboxDefaults.colors(checkedColor = g.accent))
                Text("Has an expiry date", style = Type.body, color = g.text2)
            }
            if (hasExpiry) DateField("Expires", expiry, { expiry = it })
            VSpace(12)
            Field("Notes", notes, { notes = it }, singleLine = false, placeholder = "Policy number, plate, agent's phone…")
            VSpace(16)
            SectionLabel("Photos")
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
            if (photos.isNotEmpty()) {
                VSpace(8)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    photos.forEach { a ->
                        PhotoThumb(c.attachments.file(a.fileName).path) {
                            scope.launch { c.db.attachments().delete(a); c.attachments.delete(a.fileName); photos.remove(a) }
                        }
                    }
                }
            }
            VSpace(20)
            PrimaryButton("Save", {
                scope.launch {
                    val item = DocumentItem(existing?.id ?: 0L, kind, title.trim().ifBlank { kind }, if (hasExpiry) expiry else null, notes.trim())
                    val result = c.db.documents().upsert(item)
                    if (item.id == 0L) c.db.attachments().reassign("document", -1L, result)
                    nav.popBackStack()
                }
            }, Modifier.fillMaxWidth(), tall = true)
            existing?.let { d ->
                VSpace(8)
                TextButton(onClick = {
                    scope.launch {
                        c.db.attachments().forOwnerNow("document", d.id).forEach { c.attachments.delete(it.fileName); c.db.attachments().delete(it) }
                        c.db.documents().delete(d)
                        nav.popBackStack()
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text("Delete this document", color = g.accent) }
            }
            VSpace(32)
        }
    }
}

// ---------------- Roadside ----------------

@SuppressLint("MissingPermission")
private fun lastLocation(context: Context): android.location.Location? {
    val lm = context.getSystemService(LocationManager::class.java) ?: return null
    val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
    return providers.mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }.maxByOrNull { it.time }
}

@Composable
fun RoadsideScreen(nav: NavHostController) {
    val c = LocalContainer.current
    val g = LocalGarage.current
    val context = LocalContext.current
    val specs by c.db.settings().specs().collectAsState(initial = emptyList())
    val phone = specs.firstOrNull { it.key == "roadside_phone" }?.value.orEmpty()
    var open by remember { mutableStateOf<String?>("safety") }

    fun shareLocation() {
        val loc = lastLocation(context)
        if (loc == null) {
            Toast.makeText(context, "No recent location yet. Open a maps app once, then try again.", Toast.LENGTH_LONG).show()
            return
        }
        val text = "My car is here: https://maps.google.com/?q=${loc.latitude},${loc.longitude} (2008 Accord EX-L coupe, red)"
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        context.startActivity(Intent.createChooser(send, "Share location"))
    }
    val askLocation = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) shareLocation()
    }

    Column(Modifier.fillMaxSize()) {
        GarageTopBar("Roadside help", onBack = { nav.popBackStack() })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            ButtonRow {
                PrimaryButton("Call 911", { context.openLink("tel:911") }, Modifier.weight(1f), tall = true)
                SecondaryButton(if (phone.isBlank()) "Set roadside #" else "Call roadside", {
                    if (phone.isBlank()) nav.navigate("specs") else context.openLink("tel:${phone.filter { it.isDigit() || it == '+' }}")
                }, Modifier.weight(1f), tall = true)
            }
            VSpace(10)
            SecondaryButton("Share my location", {
                val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                if (fine || coarse) shareLocation()
                else askLocation.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }, Modifier.fillMaxWidth(), tall = true)
            VSpace(10)
            SecondaryButton("What's that light on the dash?", { nav.navigate("lights") }, Modifier.fillMaxWidth(), tall = true)
            VSpace(16)
            Roadside.guides.forEach { guide ->
                Panel(Modifier.fillMaxWidth().padding(vertical = 4.dp), onClick = { open = if (open == guide.id) null else guide.id }) {
                    Column {
                        Text(guide.title, style = Type.heading, color = g.text)
                        if (open == guide.id) {
                            VSpace(8)
                            guide.steps.forEachIndexed { i, s ->
                                Row(Modifier.padding(vertical = 4.dp)) {
                                    Text("${i + 1}", style = Type.mono, color = g.muted, modifier = Modifier.padding(end = 10.dp))
                                    Text(s, style = Type.body, color = g.text2)
                                }
                            }
                            guide.caution?.let { VSpace(8); Note(it, g.accent) }
                        }
                    }
                }
            }
            VSpace(32)
        }
    }
}

// ---------------- Backup ----------------

@Composable
fun BackupScreen(nav: NavHostController) {
    val c = LocalContainer.current
    val g = LocalGarage.current
    val scope = rememberCoroutineScope()
    var pass1 by remember { mutableStateOf("") }
    var pass2 by remember { mutableStateOf("") }
    var includeManuals by remember { mutableStateOf(true) }
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var restoreUri by remember { mutableStateOf<Uri?>(null) }

    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) {
            busy = true
            scope.launch {
                status = runCatching { c.backup.export(uri, pass1.toCharArray(), includeManuals) }
                    .fold({ "Backup saved. Keep the passphrase somewhere safe; without it the file can't be opened." }, { "Backup failed: ${it.message}" })
                busy = false
            }
        }
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> restoreUri = uri }

    Column(Modifier.fillMaxSize()) {
        GarageTopBar("Backup and restore", "Encrypted with your passphrase", onBack = { nav.popBackStack() })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Note("A backup holds the whole service history, logs, documents, photos and specs, encrypted with AES-256. Save it to Google Drive, a USB stick or anywhere the file picker reaches.")
            VSpace(16)
            SectionLabel("Make a backup")
            VSpace(8)
            PassField("Passphrase", pass1) { pass1 = it }
            VSpace(8)
            PassField("Repeat passphrase", pass2) { pass2 = it }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = includeManuals, onCheckedChange = { includeManuals = it }, colors = CheckboxDefaults.colors(checkedColor = g.accent))
                Text("Include imported manuals (larger file)", style = Type.body, color = g.text2)
            }
            val ok = pass1.length >= 8 && pass1 == pass2
            if (pass1.isNotEmpty() && !ok) Text(if (pass1.length < 8) "At least 8 characters." else "Passphrases don't match.", style = Type.bodySmall, color = g.amber)
            VSpace(8)
            PrimaryButton(if (busy) "Working…" else "Save backup file", {
                create.launch("accord-garage-${LocalDate.now()}.agbak")
            }, Modifier.fillMaxWidth(), enabled = ok && !busy, tall = true)
            status?.let { VSpace(12); Note(it) }

            VSpace(24)
            SectionLabel("Restore")
            VSpace(8)
            Text("Restoring replaces everything in the app with the backup's contents, then restarts the app.", style = Type.bodySmall, color = g.muted)
            VSpace(8)
            SecondaryButton("Choose a backup file", { open.launch(arrayOf("*/*")) }, Modifier.fillMaxWidth(), enabled = !busy)
            VSpace(32)
        }
    }

    restoreUri?.let { uri ->
        var pass by remember(uri) { mutableStateOf("") }
        var error by remember(uri) { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { if (!busy) restoreUri = null },
            title = { Text("Restore this backup?") },
            text = {
                Column {
                    Text("Everything currently in the app will be replaced.", style = Type.bodySmall, color = g.text2)
                    VSpace(8)
                    PassField("Passphrase", pass) { pass = it }
                    error?.let { VSpace(6); Text(it, style = Type.bodySmall, color = g.accent) }
                }
            },
            confirmButton = {
                TextButton(enabled = pass.isNotEmpty() && !busy, onClick = {
                    busy = true
                    scope.launch {
                        runCatching { c.backup.restore(uri, pass.toCharArray()) }
                            .onSuccess { c.backup.restartApp() }
                            .onFailure { error = it.message ?: "Restore failed"; busy = false }
                    }
                }) { Text(if (busy) "Restoring…" else "Restore", color = g.accent) }
            },
            dismissButton = { TextButton(enabled = !busy, onClick = { restoreUri = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun PassField(label: String, value: String, onChange: (String) -> Unit) {
    val g = LocalGarage.current
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
        textStyle = Type.body.copy(color = g.text),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(focusedBorderColor = g.accent, unfocusedBorderColor = g.line2),
    )
}

// ---------------- About ----------------

@Composable
fun AboutScreen(nav: NavHostController) {
    val g = LocalGarage.current
    val context = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        GarageTopBar("About", onBack = { nav.popBackStack() })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Text("Accord Garage", style = Type.title, color = g.text)
            Text("Version ${BuildConfigInfo.versionName(context)}", style = Type.mono, color = g.muted)
            VSpace(16)
            Panel(Modifier.fillMaxWidth()) {
                Column {
                    SectionLabel("This car")
                    VSpace(6)
                    listOf(
                        "VIN" to Vehicle.VIN,
                        "Model" to "${Vehicle.YEAR} ${Vehicle.MAKE} ${Vehicle.MODEL} ${Vehicle.TRIM} ${Vehicle.BODY}",
                        "Engine" to "${Vehicle.ENGINE}, ${Vehicle.HORSEPOWER}",
                        "Transmission" to Vehicle.TRANSMISSION,
                        "Built" to Vehicle.PLANT,
                    ).forEach { (k, v) ->
                        Row(Modifier.padding(vertical = 3.dp)) {
                            Text(k.uppercase(), style = Type.monoSmall, color = g.muted, modifier = Modifier.weight(0.35f))
                            Text(v, style = Type.mono, color = g.text2, modifier = Modifier.weight(0.65f))
                        }
                    }
                }
            }
            VSpace(16)
            SectionLabel("Sources")
            RowItem("NHTSA vPIC VIN decoder", "VEHICLE DETAILS", onClick = { context.openLink("https://vpic.nhtsa.dot.gov/api/") })
            RowItem("NHTSA recalls API", "RECALL LIST", onClick = { context.openLink("https://api.nhtsa.gov/") })
            RowItem("Honda Owners", "OFFICIAL OWNER'S MANUAL", onClick = { context.openLink(Vehicle.HONDA_OWNERS_SITE) })
            RowItem("Iowa Buckeye install videos", "HEAD UNIT AND CAMERA", onClick = { context.openLink("https://www.youtube.com/watch?v=crPFee1Pvgc") })
            VSpace(16)
            Text("Personal-use app. Your data stays on this phone unless you export a backup. Fonts: Inter and JetBrains Mono, SIL Open Font License.", style = Type.bodySmall, color = g.muted)
            VSpace(32)
        }
    }
}
