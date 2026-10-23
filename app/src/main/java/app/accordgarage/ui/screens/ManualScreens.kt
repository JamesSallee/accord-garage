@file:OptIn(ExperimentalLayoutApi::class)

package app.accordgarage.ui.screens

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import app.accordgarage.content.Tutorial
import app.accordgarage.content.Tutorials
import app.accordgarage.data.ManualBookmark
import app.accordgarage.data.ManualDoc
import app.accordgarage.data.ManualNote
import app.accordgarage.data.ManualSearch
import app.accordgarage.data.PageBox
import app.accordgarage.data.PageHit
import app.accordgarage.data.SearchQuery
import app.accordgarage.data.Vehicle
import app.accordgarage.ui.LocalContainer
import app.accordgarage.ui.components.ButtonRow
import app.accordgarage.ui.components.Field
import app.accordgarage.ui.components.GarageTopBar
import app.accordgarage.ui.components.Note
import app.accordgarage.ui.components.Panel
import app.accordgarage.ui.components.PrimaryButton
import app.accordgarage.ui.components.RowItem
import app.accordgarage.ui.components.SecondaryButton
import app.accordgarage.ui.components.SectionLabel
import app.accordgarage.ui.components.SelectPill
import app.accordgarage.ui.components.VSpace
import app.accordgarage.ui.components.toNumberOrNull
import app.accordgarage.ui.openLink
import app.accordgarage.ui.theme.LocalGarage
import app.accordgarage.ui.theme.Type
import kotlinx.coroutines.launch

private val quickQuestions = listOf("Tire pressure", "Fuses", "Warning lights", "Jump start", "Set the clock", "Maintenance Minder")

/** Files waiting for a title before import. */
private data class PendingImport(val uris: List<Uri>, val photos: Boolean)

@Composable
fun ManualScreen(nav: NavHostController, initialQuery: String) {
    val c = LocalContainer.current
    val g = LocalGarage.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val docs by c.db.manual().docs().collectAsState(initial = emptyList())
    val bookmarks by c.db.manual().bookmarks().collectAsState(initial = emptyList())
    val notes by c.db.manual().notes().collectAsState(initial = emptyList())
    var query by remember { mutableStateOf(initialQuery) }
    var results by remember { mutableStateOf<ManualSearch?>(null) }
    var importing by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var importLabel by remember { mutableStateOf("Indexing page") }
    var importError by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<PendingImport?>(null) }

    fun runSearch(q: String) {
        query = q
        if (q.isBlank()) { results = null; return }
        scope.launch { results = c.manual.search(q) }
    }

    LaunchedEffect(initialQuery) { if (initialQuery.isNotBlank()) runSearch(initialQuery) }

    val pickPdfs = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) pending = PendingImport(uris, photos = false)
    }
    val pickPhotos = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        if (uris.isNotEmpty()) pending = PendingImport(uris, photos = true)
    }
    fun launchPdfs() = pickPdfs.launch(arrayOf("application/pdf"))
    fun launchPhotos() = pickPhotos.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

    val voice = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { runSearch(it) }
        }
    }

    Column(Modifier.fillMaxSize()) {
        GarageTopBar("Manual", if (docs.isEmpty()) "Official Honda owner's manual" else docs.joinToString(" · ") { it.title })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Ask or search: “how do I set the clock”", color = g.muted) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = g.muted) },
                trailingIcon = {
                    IconButton(onClick = {
                        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            .putExtra(RecognizerIntent.EXTRA_PROMPT, "What do you want to find?")
                        runCatching { voice.launch(i) }
                    }) { Icon(Icons.Outlined.Mic, contentDescription = "Voice search", tint = g.text2) }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { runSearch(query) }),
                shape = RoundedCornerShape(14.dp),
                textStyle = Type.body.copy(color = g.text),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = g.accent, unfocusedBorderColor = g.line2, cursorColor = g.accent),
            )

            if (docs.isEmpty() && importing == null) {
                VSpace(16)
                Note("Add Honda's official 2008 Accord Coupe owner's manual once and it's searchable offline from then on. Download the PDF from Honda's owner site, then import it here. If Honda splits it into chapter files, pick them all and they're joined in order.")
                VSpace(12)
                ButtonRow {
                    SecondaryButton("Get it from Honda", { context.openLink(Vehicle.HONDA_OWNERS_SITE) }, Modifier.weight(1f))
                    PrimaryButton("Import PDF", { launchPdfs() }, Modifier.weight(1f))
                }
                VSpace(8)
                SecondaryButton("Add a printed manual from photos", { launchPhotos() }, Modifier.fillMaxWidth())
            }

            importing?.let { (done, total) ->
                VSpace(16)
                Text(if (total == 0) "Preparing…" else "$importLabel $done of $total…", style = Type.mono, color = g.text2)
                VSpace(8)
                LinearProgressIndicator(
                    progress = { if (total == 0) 0f else done.toFloat() / total },
                    modifier = Modifier.fillMaxWidth(),
                    color = g.accent,
                    trackColor = g.surface2,
                )
                VSpace(6)
                Text("Scanned pages are read with on-device text recognition, which takes a little longer.", style = Type.bodySmall, color = g.muted)
            }
            importError?.let { VSpace(12); Note(it, g.accent) }

            val r = results
            if (r != null) {
                VSpace(16)
                SectionLabel("Results", trailing = "${r.hits.size}")
                r.correctedQuery?.let {
                    VSpace(6)
                    Text("Showing results for “$it”", style = Type.bodySmall, color = g.text2)
                }
                if (r.hits.isEmpty()) Text("Nothing found. Try fewer or different words.", style = Type.body, color = g.muted, modifier = Modifier.padding(vertical = 12.dp))
                val pageQuery = r.correctedQuery ?: query
                r.hits.forEach { hit ->
                    val doc = docs.firstOrNull { it.id == hit.docId }
                    SearchResult(hit, doc, r.terms) { nav.navigate("page/${hit.docId}/${hit.pageNumber}?q=${Uri.encode(pageQuery)}") }
                }
            } else if (docs.isNotEmpty()) {
                VSpace(16)
                SectionLabel("Quick look-ups")
                VSpace(8)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    quickQuestions.forEach { q -> SelectPill(q, false, { runSearch(q) }) }
                }
                if (notes.isNotEmpty()) {
                    VSpace(16)
                    SectionLabel("Your notes")
                    notes.forEach { n ->
                        val doc = docs.firstOrNull { it.id == n.docId }
                        RowItem(n.text.lineSequence().first().take(60), "${doc?.title?.uppercase() ?: ""} · P. ${n.pageNumber}", onClick = { nav.navigate("page/${n.docId}/${n.pageNumber}") })
                    }
                }
                if (bookmarks.isNotEmpty()) {
                    VSpace(16)
                    SectionLabel("Bookmarks")
                    bookmarks.forEach { b ->
                        val doc = docs.firstOrNull { it.id == b.docId }
                        RowItem(b.label.ifBlank { "Page ${b.pageNumber}" }, "${doc?.title?.uppercase() ?: ""} · P. ${b.pageNumber}", onClick = { nav.navigate("page/${b.docId}/${b.pageNumber}") })
                    }
                }
                VSpace(16)
                SectionLabel("Manuals")
                docs.forEach { d -> RowItem(d.title, "${d.pageCount} PAGES · CHAPTERS", onClick = { nav.navigate("chapters/${d.id}") }) }
                VSpace(8)
                ButtonRow {
                    SecondaryButton("Add a PDF", { launchPdfs() }, Modifier.weight(1f))
                    SecondaryButton("Add from photos", { launchPhotos() }, Modifier.weight(1f))
                }
            }
            VSpace(32)
        }
    }

    pending?.let { p ->
        var title by remember(p) {
            mutableStateOf(
                when {
                    p.photos -> "AINAVITO head unit manual"
                    docs.isEmpty() -> "2008 Accord Coupe owner's manual"
                    else -> ""
                }
            )
        }
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("Name this manual") },
            text = {
                Column {
                    Field("Title", title, { title = it })
                    VSpace(8)
                    Text(
                        if (p.photos) "${p.uris.size} photo${if (p.uris.size == 1) "" else "s"}, one page each, in the order you picked them."
                        else if (p.uris.size > 1) "${p.uris.size} files, joined in file-name order."
                        else "One PDF.",
                        style = Type.bodySmall, color = g.muted,
                    )
                }
            },
            confirmButton = {
                TextButton(enabled = title.isNotBlank(), onClick = {
                    pending = null
                    importError = null
                    importing = 0 to 0
                    scope.launch {
                        runCatching {
                            if (p.photos) {
                                importLabel = "Reading photo"
                                c.manual.importPhotos(p.uris, title.trim()) { d, t ->
                                    importing = d to t
                                    if (d == t) importLabel = "Indexing page"
                                }
                            } else {
                                importLabel = "Indexing page"
                                c.manual.importPdfs(p.uris, title.trim()) { d, t -> importing = d to t }
                            }
                        }.onFailure { importError = "Couldn't import that: ${it.message ?: "unknown error"}" }
                        importing = null
                    }
                }) { Text("Import") }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SearchResult(hit: PageHit, doc: ManualDoc?, terms: List<String>, onClick: () -> Unit) {
    val g = LocalGarage.current
    val c = LocalContainer.current
    val snippet = remember(hit, terms) { highlight(snippetOf(hit.text, terms), terms, g.accent) }
    var thumb by remember(hit.docId, hit.pageNumber) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(doc, hit.pageNumber) { if (doc != null) thumb = c.manual.thumbnail(doc, hit.pageNumber) }
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Panel(Modifier.fillMaxWidth(), onClick = onClick) {
            Row {
                Box(
                    Modifier.width(56.dp).aspectRatio(0.72f).clip(RoundedCornerShape(4.dp)).background(g.surface2),
                    contentAlignment = Alignment.Center,
                ) {
                    thumb?.let { Image(it.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("${doc?.title?.uppercase() ?: "MANUAL"} · PAGE ${hit.pageNumber}", style = Type.monoSmall, color = g.muted)
                    VSpace(4)
                    Text(snippet, style = Type.bodySmall, color = g.text2)
                }
            }
        }
    }
}

private fun snippetOf(text: String, terms: List<String>): String {
    val lower = text.lowercase()
    val at = terms.map { lower.indexOf(it) }.filter { it >= 0 }.minOrNull() ?: 0
    val start = (at - 80).coerceAtLeast(0)
    val end = (at + 200).coerceAtMost(text.length)
    return (if (start > 0) "…" else "") + text.substring(start, end).trim() + (if (end < text.length) "…" else "")
}

private fun highlight(text: String, terms: List<String>, color: androidx.compose.ui.graphics.Color): AnnotatedString =
    buildAnnotatedString {
        append(text)
        val lower = text.lowercase()
        terms.forEach { t ->
            var i = lower.indexOf(t)
            while (i >= 0 && t.isNotEmpty()) {
                addStyle(SpanStyle(color = color, fontWeight = FontWeight.SemiBold), i, i + t.length)
                i = lower.indexOf(t, i + t.length)
            }
        }
    }

/** Guides whose manual look-ups this page answers. */
private fun relatedGuides(pageText: String): List<Tutorial> {
    if (pageText.isBlank()) return emptyList()
    val words = SearchQuery.tokens(pageText).toSet()
    return Tutorials.all.filter { t ->
        t.steps.mapNotNull { it.manualQuery }.any { q ->
            val needed = SearchQuery.terms(q)
            needed.isNotEmpty() && needed.all { n -> words.any { it.startsWith(n) } }
        }
    }.take(4)
}

@Composable
fun ManualPageScreen(nav: NavHostController, docId: Long, startPage: Int, query: String) {
    val c = LocalContainer.current
    val g = LocalGarage.current
    val scope = rememberCoroutineScope()
    var doc by remember { mutableStateOf<ManualDoc?>(null) }
    var page by remember { mutableStateOf(startPage) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var boxes by remember { mutableStateOf<List<PageBox>>(emptyList()) }
    var related by remember { mutableStateOf<List<Tutorial>>(emptyList()) }
    var editNote by remember { mutableStateOf(false) }
    val bookmarks by c.db.manual().bookmarks().collectAsState(initial = emptyList())
    val notes by c.db.manual().notes().collectAsState(initial = emptyList())
    val sections by c.db.manual().sections(docId).collectAsState(initial = emptyList())
    val marked = bookmarks.any { it.docId == docId && it.pageNumber == page }
    val note = notes.firstOrNull { it.docId == docId && it.pageNumber == page }
    val chapter = sections.lastOrNull { it.pageNumber <= page && it.level == 0 }
    val terms = remember(query) { SearchQuery.expand(SearchQuery.terms(query)) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(docId) { doc = c.db.manual().doc(docId) }
    LaunchedEffect(docId, page) { related = relatedGuides(c.manual.pageText(docId, page)) }

    Column(Modifier.fillMaxSize()) {
        GarageTopBar(
            doc?.title ?: "Manual",
            listOfNotNull("PAGE $page${doc?.let { " OF ${it.pageCount}" } ?: ""}", chapter?.title?.uppercase()).joinToString(" · "),
            onBack = { nav.popBackStack() },
        ) {
            IconButton(onClick = { editNote = true }) {
                Icon(Icons.Outlined.EditNote, contentDescription = if (note == null) "Add a note" else "Edit note", tint = if (note != null) g.accent else g.text2)
            }
            IconButton(onClick = {
                scope.launch {
                    if (marked) c.db.manual().removeBookmark(docId, page)
                    else c.db.manual().addBookmark(ManualBookmark(docId, page, query.ifBlank { chapter?.title ?: "" }))
                }
            }) {
                Icon(if (marked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder, contentDescription = if (marked) "Remove bookmark" else "Bookmark this page", tint = if (marked) g.accent else g.text2)
            }
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().clipToBounds().background(g.surface2)) {
            val widthPx = with(LocalDensity.current) { maxWidth.toPx().toInt() }
            val d = doc
            LaunchedEffect(d, page, widthPx) {
                if (d != null) {
                    bitmap = null
                    boxes = emptyList()
                    scale = 1f; offset = Offset.Zero
                    bitmap = c.manual.render(d, page, (widthPx * 2).coerceAtMost(2400))
                    if (terms.isNotEmpty()) boxes = c.manual.matchBoxes(d, page, terms)
                }
            }
            val b = bitmap
            if (b != null) {
                val mark = g.accent.copy(alpha = 0.28f)
                Box(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                offset = if (scale == 1f) Offset.Zero else offset + pan
                            }
                        },
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y),
                    ) {
                        Image(
                            b.asImageBitmap(),
                            contentDescription = "Manual page $page",
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier.fillMaxWidth().aspectRatio(b.width.toFloat() / b.height),
                        )
                        if (boxes.isNotEmpty()) {
                            Canvas(Modifier.matchParentSize()) {
                                val pad = 2.dp.toPx()
                                boxes.forEach { box ->
                                    drawRoundRect(
                                        color = mark,
                                        topLeft = Offset(box.left * size.width - pad, box.top * size.height - pad),
                                        size = Size((box.right - box.left) * size.width + pad * 2, (box.bottom - box.top) * size.height + pad * 2),
                                        cornerRadius = CornerRadius(pad, pad),
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Loading page…", color = g.muted) }
            }
        }
        if (note != null || related.isNotEmpty()) {
            Column(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp)) {
                note?.let { n ->
                    Panel(Modifier.fillMaxWidth(), onClick = { editNote = true }) {
                        Column {
                            Text("YOUR NOTE", style = Type.label, color = g.muted)
                            Text(n.text, style = Type.bodySmall, color = g.text2, maxLines = 3)
                        }
                    }
                }
                if (related.isNotEmpty()) {
                    if (note != null) VSpace(8)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("GUIDES", style = Type.label, color = g.muted)
                        related.forEach { t -> SelectPill(t.title, false, { nav.navigate("tutorial/${t.id}") }) }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton("Previous", { if (page > 1) page-- }, Modifier.weight(1f), enabled = page > 1, tall = true)
            SecondaryButton("Next", { if (page < (doc?.pageCount ?: page)) page++ }, Modifier.weight(1f), enabled = page < (doc?.pageCount ?: 0), tall = true)
        }
    }

    if (editNote) {
        var text by remember(page) { mutableStateOf(note?.text ?: "") }
        AlertDialog(
            onDismissRequest = { editNote = false },
            title = { Text("Note on page $page") },
            text = { Field("Note", text, { text = it }, singleLine = false, placeholder = "e.g. Fuse 14 is the one I tapped for the dash cam") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        if (text.isBlank()) c.db.manual().deleteNote(docId, page)
                        else c.db.manual().upsertNote(ManualNote(docId, page, text.trim()))
                    }
                    editNote = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = {
                    if (note != null) scope.launch { c.db.manual().deleteNote(docId, page) }
                    editNote = false
                }) { Text(if (note != null) "Delete" else "Cancel") }
            },
        )
    }
}

@Composable
fun ManualChaptersScreen(nav: NavHostController, docId: Long) {
    val c = LocalContainer.current
    val g = LocalGarage.current
    val scope = rememberCoroutineScope()
    var doc by remember { mutableStateOf<ManualDoc?>(null) }
    var checked by remember { mutableStateOf(false) }
    var goTo by remember { mutableStateOf("") }
    var confirmRemove by remember { mutableStateOf(false) }
    val sections by c.db.manual().sections(docId).collectAsState(initial = emptyList())

    LaunchedEffect(docId) {
        val d = c.db.manual().doc(docId)
        doc = d
        if (d != null) c.manual.ensureSections(d)
        checked = true
    }

    Column(Modifier.fillMaxSize()) {
        GarageTopBar("Chapters", doc?.title, onBack = { nav.popBackStack() })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            val d = doc
            Row(verticalAlignment = Alignment.CenterVertically) {
                Field("Go to page", goTo, { goTo = it.filter { ch -> ch.isDigit() }.take(4) }, Modifier.weight(1f), number = true, mono = true)
                Spacer(Modifier.width(10.dp))
                val p = goTo.toNumberOrNull()?.toInt()
                PrimaryButton("Open", { if (p != null) nav.navigate("page/$docId/$p") }, enabled = p != null && d != null && p in 1..d.pageCount)
            }
            VSpace(16)
            if (checked && sections.isEmpty()) {
                Note("This file has no chapter bookmarks. Use the contents pages at the front, or search.")
            }
            sections.forEach { s ->
                Box(Modifier.padding(start = (s.level * 16).dp)) {
                    RowItem(s.title, "P. ${s.pageNumber}", onClick = { nav.navigate("page/$docId/${s.pageNumber}") })
                }
            }
            VSpace(24)
            if (d != null) {
                TextButton(onClick = { confirmRemove = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Remove this manual", color = g.accent)
                }
            }
            VSpace(24)
        }
    }

    val d = doc
    if (confirmRemove && d != null) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text("Remove ${d.title}?") },
            text = { Text("The file, its search index, bookmarks and notes are removed from the app. You can import it again later.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmRemove = false
                    scope.launch {
                        c.manual.delete(d)
                        nav.popBackStack()
                    }
                }) { Text("Remove", color = g.accent) }
            },
            dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text("Cancel") } },
        )
    }
}
