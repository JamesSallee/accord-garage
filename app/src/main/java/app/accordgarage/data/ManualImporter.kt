package app.accordgarage.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.LruCache
import com.google.mlkit.vision.text.TextRecognizer
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** A rectangle on a page, as fractions of the page's width and height. */
data class PageBox(val left: Float, val top: Float, val right: Float, val bottom: Float)

/** Ranked search results, the words to highlight, and the query after typo fixes. */
data class ManualSearch(val hits: List<PageHit>, val terms: List<String>, val correctedQuery: String?)

/**
 * Copies owner's manuals into the app and indexes every page for search. PDFs keep
 * their own text; scanned pages and photographed manuals are read with on-device OCR.
 */
class ManualRepository(private val context: Context, private val db: AppDatabase) {

    private val dir: File get() = File(context.filesDir, "manuals").apply { mkdirs() }

    // PdfRenderer isn't thread-safe, so one stays open behind a lock for page views and thumbnails.
    private val renderLock = Mutex()
    private var openDocId: Long? = null
    private var openPfd: ParcelFileDescriptor? = null
    private var openRenderer: PdfRenderer? = null
    private val thumbs = LruCache<String, Bitmap>(48)

    @Volatile private var vocab: Map<String, Int>? = null

    fun file(doc: ManualDoc): File = File(dir, doc.fileName)

    private fun newFile(): File = File(dir, "manual_${System.currentTimeMillis()}.pdf")

    /**
     * Imports one PDF, or several stitched into one manual (for a manual Honda splits
     * into chapter files). Files are joined in file-name order.
     */
    suspend fun importPdfs(uris: List<Uri>, title: String, onProgress: (Int, Int) -> Unit): Long =
        withContext(Dispatchers.IO) {
            PDFBoxResourceLoader.init(context.applicationContext)
            val target = newFile()
            try {
                if (uris.size == 1) {
                    copy(uris.first(), target)
                } else {
                    val ordered = uris.sortedBy { displayName(it).lowercase() }
                    val streams = ordered.map { uri ->
                        requireNotNull(context.contentResolver.openInputStream(uri)) { "Couldn't open ${displayName(uri)}" }
                    }
                    try {
                        val merger = PDFMergerUtility()
                        streams.forEach { merger.addSource(it) }
                        merger.destinationFileName = target.path
                        merger.mergeDocuments(MemoryUsageSetting.setupTempFileOnly())
                    } finally {
                        streams.forEach { runCatching { it.close() } }
                    }
                }
                index(target, title, emptyMap(), onProgress)
            } catch (e: Exception) {
                target.delete()
                throw e
            }
        }

    /**
     * Turns photos of a printed manual (one page per photo, in the order picked) into a
     * PDF in the app and reads each page with on-device OCR.
     */
    suspend fun importPhotos(uris: List<Uri>, title: String, onProgress: (Int, Int) -> Unit): Long =
        withContext(Dispatchers.IO) {
            PDFBoxResourceLoader.init(context.applicationContext)
            val target = newFile()
            val texts = mutableMapOf<Int, String>()
            val pdf = PdfDocument()
            val recognizer = Ocr.client()
            try {
                uris.forEachIndexed { i, uri ->
                    val bmp = decodeScaled(uri, PHOTO_MAX_PX)
                    val page = pdf.startPage(PdfDocument.PageInfo.Builder(bmp.width, bmp.height, i + 1).create())
                    page.canvas.drawColor(Color.WHITE)
                    page.canvas.drawBitmap(bmp, null, Rect(0, 0, bmp.width, bmp.height), Paint(Paint.FILTER_BITMAP_FLAG))
                    pdf.finishPage(page)
                    texts[i + 1] = Ocr.recognize(recognizer, bmp)?.text.orEmpty()
                    bmp.recycle()
                    onProgress(i + 1, uris.size)
                }
                target.outputStream().use { pdf.writeTo(it) }
                pdf.close()
                index(target, title, texts, onProgress)
            } catch (e: Exception) {
                runCatching { pdf.close() }
                target.delete()
                throw e
            } finally {
                recognizer.close()
            }
        }

    private suspend fun index(target: File, title: String, known: Map<Int, String>, onProgress: (Int, Int) -> Unit): Long {
        PDDocument.load(target).use { pdf ->
            val count = pdf.numberOfPages
            val docId = db.manual().insertDoc(
                ManualDoc(title = title, fileName = target.name, pageCount = count, importedEpochDay = LocalDate.now().toEpochDay())
            )
            try {
                db.manual().insertSections(outline(pdf, docId))
                val stripper = PDFTextStripper()
                var pfd: ParcelFileDescriptor? = null
                var renderer: PdfRenderer? = null
                var recognizer: TextRecognizer? = null
                try {
                    for (page in 1..count) {
                        var text = known[page] ?: run {
                            stripper.startPage = page
                            stripper.endPage = page
                            runCatching { stripper.getText(pdf) }.getOrDefault("")
                        }
                        text = clean(text)
                        if (text.length < MIN_PAGE_TEXT && page !in known) {
                            // No text layer: a scanned page. Read it with OCR.
                            val r = renderer ?: run {
                                val fd = ParcelFileDescriptor.open(target, ParcelFileDescriptor.MODE_READ_ONLY)
                                pfd = fd
                                PdfRenderer(fd).also { renderer = it }
                            }
                            val ocrClient = recognizer ?: Ocr.client().also { recognizer = it }
                            val bmp = renderPage(r, page, OCR_WIDTH_PX)
                            val ocr = Ocr.recognize(ocrClient, bmp)?.text.orEmpty()
                            bmp.recycle()
                            if (ocr.length > text.length) text = clean(ocr)
                        }
                        if (text.isNotEmpty()) db.manual().insertPage(docId, page, text)
                        onProgress(page, count)
                    }
                } finally {
                    runCatching { renderer?.close() }
                    runCatching { pfd?.close() }
                    recognizer?.close()
                }
            } catch (e: Exception) {
                db.manual().deletePages(docId)
                db.manual().deleteSections(docId)
                db.manual().deleteDoc(docId)
                throw e
            }
            vocab = null
            return docId
        }
    }

    /** Chapters from the PDF's bookmarks, two levels deep. */
    private fun outline(pdf: PDDocument, docId: Long): List<ManualSection> {
        val root = runCatching { pdf.documentCatalog.documentOutline }.getOrNull() ?: return emptyList()
        val out = mutableListOf<ManualSection>()
        fun walk(items: Iterable<PDOutlineItem>, level: Int) {
            for (item in items) {
                val index = runCatching { item.findDestinationPage(pdf)?.let { pdf.pages.indexOf(it) } }.getOrNull() ?: -1
                val title = item.title?.replace(Regex("\\s+"), " ")?.trim().orEmpty()
                if (index >= 0 && title.isNotEmpty()) {
                    out += ManualSection(manualId = docId, title = title, pageNumber = index + 1, level = level)
                }
                if (level < 1) walk(item.children(), level + 1)
            }
        }
        runCatching { walk(root.children(), 0) }
        return out
    }

    /** Reads chapters for a manual imported before chapters were kept. */
    suspend fun ensureSections(doc: ManualDoc) = withContext(Dispatchers.IO) {
        if (db.manual().sectionsNow(doc.id).isNotEmpty()) return@withContext
        runCatching {
            PDFBoxResourceLoader.init(context.applicationContext)
            PDDocument.load(file(doc)).use { pdf -> db.manual().insertSections(outline(pdf, doc.id)) }
        }
    }

    suspend fun delete(doc: ManualDoc) = withContext(Dispatchers.IO) {
        renderLock.withLock { if (openDocId == doc.id) closeRenderer() }
        db.manual().deletePages(doc.id)
        db.manual().deleteSections(doc.id)
        db.manual().removeBookmarksFor(doc.id)
        db.manual().deleteNotesFor(doc.id)
        db.manual().deleteDoc(doc.id)
        file(doc).delete()
        vocab = null
    }

    /** Renders one page (1-based) to a bitmap [widthPx] wide. */
    suspend fun render(doc: ManualDoc, page: Int, widthPx: Int): Bitmap? = withContext(Dispatchers.IO) {
        renderLock.withLock {
            runCatching { renderPage(rendererFor(doc), page, widthPx) }.getOrNull()
        }
    }

    /** A small cached render for search results. */
    suspend fun thumbnail(doc: ManualDoc, page: Int): Bitmap? {
        val key = "${doc.id}:$page"
        thumbs.get(key)?.let { return it }
        return render(doc, page, THUMB_WIDTH_PX)?.also { thumbs.put(key, it) }
    }

    private fun rendererFor(doc: ManualDoc): PdfRenderer {
        val current = openRenderer
        if (openDocId == doc.id && current != null) return current
        closeRenderer()
        val pfd = ParcelFileDescriptor.open(file(doc), ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        openDocId = doc.id
        openPfd = pfd
        openRenderer = renderer
        return renderer
    }

    private fun closeRenderer() {
        runCatching { openRenderer?.close() }
        runCatching { openPfd?.close() }
        openRenderer = null
        openPfd = null
        openDocId = null
    }

    private fun renderPage(renderer: PdfRenderer, page: Int, widthPx: Int): Bitmap {
        val index = (page - 1).coerceIn(0, renderer.pageCount - 1)
        renderer.openPage(index).use { p ->
            val scale = widthPx.toFloat() / p.width
            val bmp = Bitmap.createBitmap(widthPx, (p.height * scale).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
            bmp.eraseColor(Color.WHITE)
            p.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            return bmp
        }
    }

    suspend fun pageText(docId: Long, page: Int): String = withContext(Dispatchers.IO) {
        db.manual().pageText(docId, page).orEmpty()
    }

    /**
     * Where the search words sit on a page, for highlighting. Uses the PDF's text
     * positions, or OCR word boxes on a scanned page.
     */
    suspend fun matchBoxes(doc: ManualDoc, page: Int, terms: List<String>): List<PageBox> = withContext(Dispatchers.IO) {
        if (terms.isEmpty()) return@withContext emptyList()
        val fromText = runCatching {
            PDFBoxResourceLoader.init(context.applicationContext)
            PDDocument.load(file(doc)).use { pdf ->
                val pdPage = pdf.getPage(page - 1)
                val crop = pdPage.cropBox
                val turned = pdPage.rotation % 180 != 0
                val w = if (turned) crop.height else crop.width
                val h = if (turned) crop.width else crop.height
                val finder = BoxFinder(terms, w, h)
                finder.startPage = page
                finder.endPage = page
                val text = finder.getText(pdf)
                if (clean(text).length < MIN_PAGE_TEXT) null else finder.boxes.toList()
            }
        }.getOrNull()
        fromText ?: ocrBoxes(doc, page, terms)
    }

    private suspend fun ocrBoxes(doc: ManualDoc, page: Int, terms: List<String>): List<PageBox> {
        val bmp = render(doc, page, OCR_WIDTH_PX) ?: return emptyList()
        val recognizer = Ocr.client()
        try {
            val text = Ocr.recognize(recognizer, bmp) ?: return emptyList()
            val w = bmp.width.toFloat()
            val h = bmp.height.toFloat()
            return text.textBlocks.flatMap { it.lines }.flatMap { it.elements }.mapNotNull { el ->
                val word = el.text.lowercase()
                val box = el.boundingBox ?: return@mapNotNull null
                if (terms.none { word.startsWith(it) }) null
                else PageBox(box.left / w, box.top / h, box.right / w, box.bottom / h)
            }
        } finally {
            recognizer.close()
            bmp.recycle()
        }
    }

    suspend fun search(raw: String): ManualSearch = withContext(Dispatchers.IO) {
        val words = SearchQuery.terms(raw)
        if (words.isEmpty()) return@withContext ManualSearch(emptyList(), emptyList(), null)
        val vocabulary = vocabulary()
        val fixed = words.map { w ->
            if (vocabulary.isEmpty() || SearchQuery.known(w, vocabulary)) w else SearchQuery.correct(w, vocabulary) ?: w
        }
        val corrected = if (fixed != words) fixed.joinToString(" ") else null

        fun run(query: String?): List<PageHit> =
            if (query == null) emptyList() else runCatching { db.manual().search(query) }.getOrDefault(emptyList())

        val hits = run(SearchQuery.build(fixed)).ifEmpty { run(SearchQuery.buildAny(fixed)) }
        val sections = hits.map { it.docId }.distinct().associateWith { db.manual().sectionsNow(it) }
        val ranked = Ranker.rank(hits, fixed, sections).take(MAX_RESULTS)
        ManualSearch(ranked, SearchQuery.expand(fixed), corrected)
    }

    private suspend fun vocabulary(): Map<String, Int> {
        vocab?.let { return it }
        val counts = HashMap<String, Int>()
        db.manual().allText().forEach { text ->
            SearchQuery.tokens(text).forEach { counts[it] = (counts[it] ?: 0) + 1 }
        }
        return counts.also { vocab = it }
    }

    private fun copy(uri: Uri, target: File) {
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Couldn't open that file" }
            target.outputStream().use { input.copyTo(it) }
        }
    }

    private fun displayName(uri: Uri): String =
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull() ?: uri.lastPathSegment.orEmpty()

    private fun decodeScaled(uri: Uri, maxPx: Int): Bitmap {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val size = info.size
            val scale = min(1f, maxPx.toFloat() / max(size.width, size.height))
            decoder.setTargetSize((size.width * scale).roundToInt().coerceAtLeast(1), (size.height * scale).roundToInt().coerceAtLeast(1))
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    }

    private fun clean(text: String) = text.replace(Regex("\\s+"), " ").trim()

    companion object {
        private const val MIN_PAGE_TEXT = 25
        private const val OCR_WIDTH_PX = 1600
        private const val PHOTO_MAX_PX = 2000
        private const val THUMB_WIDTH_PX = 180
        private const val MAX_RESULTS = 30
    }
}

/** Collects the boxes of words that start with a search term. */
private class BoxFinder(private val terms: List<String>, private val pageWidth: Float, private val pageHeight: Float) : PDFTextStripper() {
    val boxes = mutableListOf<PageBox>()

    override fun writeString(text: String?, textPositions: MutableList<TextPosition>?) {
        super.writeString(text, textPositions)
        if (text == null || textPositions == null || text.length != textPositions.size) return
        val lower = text.lowercase()
        for (t in terms) {
            if (t.isEmpty()) continue
            var i = lower.indexOf(t)
            while (i >= 0) {
                if (i == 0 || !lower[i - 1].isLetterOrDigit()) {
                    var end = i + t.length
                    while (end < lower.length && lower[end].isLetterOrDigit()) end++
                    val span = textPositions.subList(i, end)
                    val left = span.minOf { it.xDirAdj }
                    val right = span.maxOf { it.xDirAdj + it.widthDirAdj }
                    val top = span.minOf { it.yDirAdj - it.heightDir }
                    val bottom = span.maxOf { it.yDirAdj }
                    boxes += PageBox(left / pageWidth, top / pageHeight, right / pageWidth, bottom / pageHeight)
                }
                i = lower.indexOf(t, i + t.length)
            }
        }
    }
}

/** Orders hits so the page that answers the question comes first. */
object Ranker {
    fun rank(hits: List<PageHit>, words: List<String>, sections: Map<Long, List<ManualSection>>): List<PageHit> {
        if (hits.size <= 1) return hits
        val groups = words.map { w -> SearchQuery.expand(listOf(w)) }
        val tokenized = hits.map { SearchQuery.tokens(it.text) }
        val n = hits.size.toDouble()
        val idf = groups.map { options ->
            val df = tokenized.count { tokens -> tokens.any { t -> options.any { t.startsWith(it) } } }
            ln(1 + n / max(df, 1))
        }
        val phrase = words.joinToString(" ")
        return hits.indices.map { i ->
            val hit = hits[i]
            val tokens = tokenized[i]
            var score = 0.0
            groups.forEachIndexed { g, options ->
                val tf = tokens.count { t -> options.any { t.startsWith(it) } }
                if (tf > 0) score += (1 + ln(tf.toDouble())) * idf[g]
            }
            if (words.size > 1 && hit.text.lowercase().contains(phrase)) score += 3.0
            // A chapter or section that starts here and is named for the question.
            val heading = sections[hit.docId].orEmpty().filter { it.pageNumber == hit.pageNumber }
            if (heading.any { s -> groups.any { options -> options.any { s.title.lowercase().contains(it) } } }) score += 2.5
            // Index and contents pages list every topic; push them down.
            if (tokens.isNotEmpty() && tokens.count { t -> t.all { it.isDigit() } }.toDouble() / tokens.size > 0.2) score *= 0.4
            hit to score
        }.sortedWith(compareByDescending<Pair<PageHit, Double>> { it.second }.thenBy { it.first.pageNumber }).map { it.first }
    }
}

/** Turns a plain question into an FTS query, with car-word synonyms and typo fixes. */
object SearchQuery {
    private val stopWords = setOf(
        "how", "do", "does", "i", "the", "a", "an", "to", "my", "is", "what", "where", "when", "of",
        "in", "on", "for", "and", "or", "can", "you", "it", "are", "should", "with", "set", "change",
        "check", "find", "get", "use", "why", "which", "there",
    )

    /** Everyday phrases and the words Honda's manual uses for them. */
    private val phrases: List<Pair<String, String>> = listOf(
        "check engine" to "malfunction indicator",
        "engine light" to "malfunction indicator",
        "gas cap" to "fuel fill cap",
        "gas tank" to "fuel tank",
        "gas door" to "fuel fill door",
        "oil life" to "maintenance minder",
        "key fob" to "remote transmitter",
        "jump start" to "jump starting",
        "cruise" to "cruise control",
    )

    private val synonyms: Map<String, List<String>> = mapOf(
        "gas" to listOf("fuel", "gasoline"),
        "fuel" to listOf("gasoline"),
        "trunk" to listOf("boot"),
        "clicker" to listOf("remote", "transmitter"),
        "remote" to listOf("transmitter"),
        "fob" to listOf("remote", "transmitter"),
        "tire" to listOf("tyre"),
        "tires" to listOf("tire"),
        "ac" to listOf("air", "conditioning", "climate"),
        "heater" to listOf("heating", "climate"),
        "wipers" to listOf("wiper", "washer"),
        "lights" to listOf("light", "lamp", "bulb"),
        "light" to listOf("lamp", "indicator"),
        "bulb" to listOf("lamp"),
        "radio" to listOf("audio"),
        "jump" to listOf("jumpstart", "jumping", "booster"),
        "flat" to listOf("spare", "tire"),
        "oil" to listOf("engine oil"),
        "maintenance" to listOf("minder"),
        "clock" to listOf("time"),
        "coolant" to listOf("antifreeze"),
        "fuses" to listOf("fuse"),
        "fuse" to listOf("fuses"),
    )

    /** Lower-case words of at least two letters or digits. */
    fun tokens(text: String): List<String> =
        text.lowercase().split(Regex("[^a-z0-9]+")).filter { it.length > 1 }

    fun terms(raw: String): List<String> {
        var text = " " + raw.lowercase().replace(Regex("[^a-z0-9 ]"), " ").replace(Regex("\\s+"), " ") + " "
        phrases.forEach { (from, to) -> text = text.replace(" $from ", " $to ") }
        return tokens(text).filter { it !in stopWords }.distinct()
    }

    /** The words plus their synonyms, for matching and highlighting. */
    fun expand(words: List<String>): List<String> =
        words.flatMap { listOf(it) + (synonyms[it] ?: emptyList()) }.flatMap { it.split(' ') }.distinct()

    /** Every meaningful word must match (or one of its synonyms), each as a prefix. */
    fun build(words: List<String>): String? {
        if (words.isEmpty()) return null
        return words.joinToString(" ") { w ->
            val options = expand(listOf(w)).map { "$it*" }
            if (options.size == 1) options.first() else options.joinToString(" OR ")
        }
    }

    /** Fallback: any of the words. */
    fun buildAny(words: List<String>): String? {
        if (words.isEmpty()) return null
        return expand(words).joinToString(" OR ") { "$it*" }
    }

    /** True if the manual has a word starting with [word], or [word] has synonyms. */
    fun known(word: String, vocab: Map<String, Int>): Boolean =
        word in synonyms || word in vocab || vocab.keys.any { it.startsWith(word) }

    /** The manual's closest word to a misspelling, or null if nothing is close. */
    fun correct(word: String, vocab: Map<String, Int>): String? {
        if (word.length < 4 || word.any { it.isDigit() }) return null
        val limit = if (word.length <= 5) 1 else 2
        var best: String? = null
        var bestDistance = limit + 1
        var bestCount = 0
        for ((candidate, count) in vocab) {
            if (abs(candidate.length - word.length) > limit) continue
            val d = distance(word, candidate)
            if (d < bestDistance || (d == bestDistance && count > bestCount)) {
                best = candidate
                bestDistance = d
                bestCount = count
            }
        }
        return best?.takeIf { bestDistance <= limit }
    }

    /** Edit distance counting a swap of two neighbouring letters as one edit. */
    private fun distance(a: String, b: String): Int {
        val d = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) d[i][0] = i
        for (j in 0..b.length) d[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                d[i][j] = minOf(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + cost)
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                    d[i][j] = min(d[i][j], d[i - 2][j - 2] + 1)
                }
            }
        }
        return d[a.length][b.length]
    }
}
