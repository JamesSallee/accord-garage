package app.accordgarage.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

/** Copies an owner's-manual PDF into the app and indexes every page for search. */
class ManualRepository(private val context: Context, private val db: AppDatabase) {

    private val dir: File get() = File(context.filesDir, "manuals").apply { mkdirs() }

    fun file(doc: ManualDoc): File = File(dir, doc.fileName)

    suspend fun import(uri: Uri, title: String, onProgress: (Int, Int) -> Unit): Long =
        withContext(Dispatchers.IO) {
            PDFBoxResourceLoader.init(context.applicationContext)
            val fileName = "manual_${System.currentTimeMillis()}.pdf"
            val target = File(dir, fileName)
            context.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "Couldn't open that file" }
                target.outputStream().use { input.copyTo(it) }
            }
            try {
                PDDocument.load(target).use { pdf ->
                    val count = pdf.numberOfPages
                    val docId = db.manual().insertDoc(
                        ManualDoc(
                            title = title,
                            fileName = fileName,
                            pageCount = count,
                            importedEpochDay = LocalDate.now().toEpochDay(),
                        )
                    )
                    val stripper = PDFTextStripper()
                    for (page in 1..count) {
                        stripper.startPage = page
                        stripper.endPage = page
                        val text = runCatching { stripper.getText(pdf) }.getOrDefault("")
                            .replace(Regex("\\s+"), " ").trim()
                        if (text.isNotEmpty()) db.manual().insertPage(docId, page, text)
                        onProgress(page, count)
                    }
                    docId
                }
            } catch (e: Exception) {
                target.delete()
                throw e
            }
        }

    suspend fun delete(doc: ManualDoc) = withContext(Dispatchers.IO) {
        db.manual().deletePages(doc.id)
        db.manual().removeBookmarksFor(doc.id)
        db.manual().deleteDoc(doc.id)
        file(doc).delete()
    }

    /** Renders one page (1-based) to a bitmap [widthPx] wide. */
    suspend fun render(doc: ManualDoc, page: Int, widthPx: Int): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            ParcelFileDescriptor.open(file(doc), ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val index = (page - 1).coerceIn(0, renderer.pageCount - 1)
                    renderer.openPage(index).use { p ->
                        val scale = widthPx.toFloat() / p.width
                        val bmp = Bitmap.createBitmap(widthPx, (p.height * scale).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(Color.WHITE)
                        p.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bmp
                    }
                }
            }
        }.getOrNull()
    }

    suspend fun search(raw: String): List<PageHit> = withContext(Dispatchers.IO) {
        val query = SearchQuery.build(raw) ?: return@withContext emptyList()
        val strict = runCatching { db.manual().search(query) }.getOrDefault(emptyList())
        if (strict.isNotEmpty()) return@withContext strict
        val loose = SearchQuery.buildAny(raw) ?: return@withContext emptyList()
        runCatching { db.manual().search(loose) }.getOrDefault(emptyList())
    }
}

/** Turns a plain question into an FTS query, with car-word synonyms. */
object SearchQuery {
    private val stopWords = setOf(
        "how", "do", "does", "i", "the", "a", "an", "to", "my", "is", "what", "where", "when", "of",
        "in", "on", "for", "and", "or", "can", "you", "it", "are", "should", "with", "set", "change",
    )

    private val synonyms: Map<String, List<String>> = mapOf(
        "gas" to listOf("fuel", "gasoline"),
        "fuel" to listOf("gasoline"),
        "check" to listOf("malfunction"),
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
        "battery" to listOf("battery"),
    )

    fun terms(raw: String): List<String> =
        raw.lowercase()
            .replace(Regex("[^a-z0-9 ]"), " ")
            .split(' ')
            .filter { it.length > 1 && it !in stopWords }
            .distinct()

    /** Every meaningful word must match (or one of its synonyms), each as a prefix. */
    fun build(raw: String): String? {
        val words = terms(raw)
        if (words.isEmpty()) return null
        return words.joinToString(" ") { w ->
            val options = (listOf(w) + (synonyms[w] ?: emptyList()))
                .flatMap { it.split(' ') }
                .distinct()
                .map { "$it*" }
            if (options.size == 1) options.first() else options.joinToString(" OR ")
        }
    }

    /** Fallback: any of the words. */
    fun buildAny(raw: String): String? {
        val words = terms(raw)
        if (words.isEmpty()) return null
        return words.flatMap { listOf(it) + (synonyms[it] ?: emptyList()) }
            .flatMap { it.split(' ') }
            .distinct()
            .joinToString(" OR ") { "$it*" }
    }
}
