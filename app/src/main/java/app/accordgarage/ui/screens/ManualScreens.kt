@file:OptIn(ExperimentalLayoutApi::class)

package app.accordgarage.ui.screens

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
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
import app.accordgarage.data.ManualBookmark
import app.accordgarage.data.ManualDoc
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
import app.accordgarage.ui.openLink
import app.accordgarage.ui.theme.LocalGarage
import app.accordgarage.ui.theme.Type
import kotlinx.coroutines.launch

private val quickQuestions = listOf("Tire pressure", "Fuses", "Warning lights", "Jump start", "Set the clock", "Maintenance Minder")

@Composable
fun ManualScreen(nav: NavHostController, initialQuery: String) {
    val c = LocalContainer.current
    val g = LocalGarage.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val docs by c.db.manual().docs().collectAsState(initial = emptyList())
    val bookmarks by c.db.manual().bookmarks().collectAsState(initial = emptyList())
    var query by remember { mutableStateOf(initialQuery) }
    var results by remember { mutableStateOf<List<PageHit>?>(null) }
    var importing by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }

    fun runSearch(q: String) {
        query = q
        if (q.isBlank()) { results = null; return }
        scope.launch { results = c.manual.search(q) }
    }

    LaunchedEffect(initialQuery) { if (initialQuery.isNotBlank()) runSearch(initialQuery) }

    val pickPdf = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> pendingUri = uri }
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
                Note("Add Honda's official 2008 Accord Coupe owner's manual once and it's searchable offline from then on. Download the PDF from Honda's owner site, then import it here.")
                VSpace(12)
                ButtonRow {
                    SecondaryButton("Get it from Honda", { context.openLink(Vehicle.HONDA_OWNERS_SITE) }, Modifier.weight(1f))
                    PrimaryButton("Import PDF", { pickPdf.launch(arrayOf("application/pdf")) }, Modifier.weight(1f))
                }
            }

            importing?.let { (done, total) ->
                VSpace(16)
                Text("Indexing page $done of $total…", style = Type.mono, color = g.text2)
                VSpace(8)
                LinearProgressIndicator(
                    progress = { if (total == 0) 0f else done.toFloat() / total },
                    modifier = Modifier.fillMaxWidth(),
                    color = g.accent,
                    trackColor = g.surface2,
                )
            }
            importError?.let { VSpace(12); Note(it, g.accent) }

            val r = results
            if (r != null) {
                VSpace(16)
                SectionLabel("Results", trailing = "${r.size}")
                if (r.isEmpty()) Text("Nothing found. Try fewer or different words.", style = Type.body, color = g.muted, modifier = Modifier.padding(vertical = 12.dp))
                val terms = SearchQuery.terms(query)
                r.forEach { hit ->
                    val doc = docs.firstOrNull { it.id == hit.docId }
                    SearchResult(hit, doc, terms) { nav.navigate("page/${hit.docId}/${hit.pageNumber}?q=${Uri.encode(query)}") }
                }
            } else if (docs.isNotEmpty()) {
                VSpace(16)
                SectionLabel("Quick look-ups")
                VSpace(8)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    quickQuestions.forEach { q -> SelectPill(q, false, { runSearch(q) }) }
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
                docs.forEach { d -> ManualDocRow(d) { scope.launch { c.manual.delete(d) } } }
                VSpace(8)
                SecondaryButton("Add another manual (e.g. the head unit's)", { pickPdf.launch(arrayOf("application/pdf")) }, Modifier.fillMaxWidth())
            }
            VSpace(32)
        }
    }

    pendingUri?.let { uri ->
        var title by remember(uri) { mutableStateOf(if (docs.isEmpty()) "2008 Accord Coupe owner's manual" else "") }
        AlertDialog(
            onDismissRequest = { pendingUri = null },
            title = { Text("Name this manual") },
            text = { Field("Title", title, { title = it }) },
            confirmButton = {
                TextButton(enabled = title.isNotBlank(), onClick = {
                    pendingUri = null
                    importError = null
                    importing = 0 to 0
                    scope.launch {
                        runCatching { c.manual.import(uri, title.trim()) { d, t -> importing = d to t } }
                            .onFailure { importError = "Couldn't read that PDF: ${it.message ?: "unknown error"}" }
                        importing = null
                    }
                }) { Text("Import") }
            },
            dismissButton = { TextButton(onClick = { pendingUri = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ManualDocRow(doc: ManualDoc, onDelete: () -> Unit) {
    val g = LocalGarage.current
    var confirm by remember { mutableStateOf(false) }
    RowItem(doc.title, "${doc.pageCount} PAGES", onClick = { confirm = true }, trailing = "Remove", trailingColor = g.muted, showChevron = false)
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Remove ${doc.title}?") },
            text = { Text("The PDF and its search index are removed from the app. You can import it again later.") },
            confirmButton = { TextButton(onClick = { confirm = false; onDelete() }) { Text("Remove", color = g.accent) } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SearchResult(hit: PageHit, doc: ManualDoc?, terms: List<String>, onClick: () -> Unit) {
    val g = LocalGarage.current
    val snippet = remember(hit, terms) { highlight(snippetOf(hit.text, terms), terms, g.accent) }
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Panel(Modifier.fillMaxWidth(), onClick = onClick) {
            Column {
                Text("${doc?.title?.uppercase() ?: "MANUAL"} · PAGE ${hit.pageNumber}", style = Type.monoSmall, color = g.muted)
                VSpace(4)
                Text(snippet, style = Type.bodySmall, color = g.text2)
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

@Composable
fun ManualPageScreen(nav: NavHostController, docId: Long, startPage: Int, query: String) {
    val c = LocalContainer.current
    val g = LocalGarage.current
    val scope = rememberCoroutineScope()
    var doc by remember { mutableStateOf<ManualDoc?>(null) }
    var page by remember { mutableStateOf(startPage) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val bookmarks by c.db.manual().bookmarks().collectAsState(initial = emptyList())
    val marked = bookmarks.any { it.docId == docId && it.pageNumber == page }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(docId) { doc = c.db.manual().doc(docId) }

    Column(Modifier.fillMaxSize()) {
        GarageTopBar(doc?.title ?: "Manual", "PAGE $page${doc?.let { " OF ${it.pageCount}" } ?: ""}", onBack = { nav.popBackStack() }) {
            IconButton(onClick = {
                scope.launch {
                    if (marked) c.db.manual().removeBookmark(docId, page)
                    else c.db.manual().addBookmark(ManualBookmark(docId, page, query.ifBlank { "" }))
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
                    scale = 1f; offset = Offset.Zero
                    bitmap = c.manual.render(d, page, (widthPx * 2).coerceAtMost(2400))
                }
            }
            val b = bitmap
            if (b != null) {
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
                    Image(
                        b.asImageBitmap(),
                        contentDescription = "Manual page $page",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y),
                    )
                }
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Loading page…", color = g.muted) }
            }
        }
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton("Previous", { if (page > 1) page-- }, Modifier.weight(1f), enabled = page > 1, tall = true)
            SecondaryButton("Next", { if (page < (doc?.pageCount ?: page)) page++ }, Modifier.weight(1f), enabled = page < (doc?.pageCount ?: 0), tall = true)
        }
    }
}
