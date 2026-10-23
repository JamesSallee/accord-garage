package app.accordgarage.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import kotlin.coroutines.resume

/** Photos and receipts live in files/attachments. */
class AttachmentFiles(private val context: Context) {
    val dir: File get() = File(context.filesDir, "attachments").apply { mkdirs() }

    fun file(name: String) = File(dir, name)

    /** A new empty file plus a content Uri the camera app can write into. */
    fun newPhoto(): Pair<String, Uri> {
        val name = "photo_${System.currentTimeMillis()}.jpg"
        val f = file(name)
        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", f)
        return name to uri
    }

    suspend fun copyIn(uri: Uri): String = withContext(Dispatchers.IO) {
        val name = "photo_${System.currentTimeMillis()}.jpg"
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Couldn't open that image" }
            file(name).outputStream().use { input.copyTo(it) }
        }
        name
    }

    fun delete(name: String) {
        file(name).delete()
    }
}

/** On-device text recognition for receipts and scanned manual pages (ML Kit, no network). */
object Ocr {
    fun client(): TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /** Reads a bitmap with an open [client]; null if recognition fails. */
    suspend fun recognize(client: TextRecognizer, bitmap: Bitmap): Text? =
        suspendCancellableCoroutine { cont ->
            client.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resume(null) }
        }

    suspend fun read(context: Context, file: File): String {
        val image = withContext(Dispatchers.IO) { InputImage.fromFilePath(context, Uri.fromFile(file)) }
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            suspendCancellableCoroutine { cont ->
                recognizer.process(image)
                    .addOnSuccessListener { cont.resume(it.text) }
                    .addOnFailureListener { cont.resume("") }
            }
        } finally {
            recognizer.close()
        }
    }
}

data class ReceiptGuess(val shop: String?, val total: Double?, val date: LocalDate?)

data class ReceiptLine(val name: String, val price: Double)

object ReceiptParser {
    private val money = Regex("""\$?\s?(\d{1,5}(?:,\d{3})*\.\d{2})""")
    private val usDate = Regex("""\b(\d{1,2})[/-](\d{1,2})[/-](\d{2,4})\b""")
    private val isoDate = Regex("""\b(20\d{2})-(\d{1,2})-(\d{1,2})\b""")

    fun parse(text: String): ReceiptGuess {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val shop = lines.firstOrNull { line -> line.any { it.isLetter() } && line.length in 3..40 }

        fun amounts(line: String) = money.findAll(line).mapNotNull { it.groupValues[1].replace(",", "").toDoubleOrNull() }
        val totalLine = lines.lastOrNull { it.contains("total", ignoreCase = true) && !it.contains("subtotal", ignoreCase = true) }
        val total = totalLine?.let { amounts(it).maxOrNull() } ?: lines.flatMap { amounts(it).toList() }.maxOrNull()

        val date = isoDate.find(text)?.let {
            runCatching { LocalDate.of(it.groupValues[1].toInt(), it.groupValues[2].toInt(), it.groupValues[3].toInt()) }.getOrNull()
        } ?: usDate.find(text)?.let {
            val y = it.groupValues[3].toInt().let { y -> if (y < 100) 2000 + y else y }
            runCatching { LocalDate.of(y, it.groupValues[1].toInt(), it.groupValues[2].toInt()) }.getOrNull()
        }
        return ReceiptGuess(shop, total, date)
    }

    private val notItems = Regex("(?i)total|subtotal|tax|change|cash|visa|mastercard|amex|debit|credit|balance|due|tender|discount|labor|labour|tip")

    /** Lines that look like "Oil filter 15220-R40-A01   8.99": a name, then a price. */
    fun lineItems(text: String): List<ReceiptLine> =
        text.lines().map { it.trim() }.mapNotNull { line ->
            if (notItems.containsMatchIn(line)) return@mapNotNull null
            val m = money.findAll(line).lastOrNull() ?: return@mapNotNull null
            val price = m.groupValues[1].replace(",", "").toDoubleOrNull() ?: return@mapNotNull null
            val name = line.removeRange(m.range).trim().trimEnd('$', '-', ':').trim()
            if (name.count { it.isLetter() } < 3) null else ReceiptLine(name.take(60), price)
        }.take(20)
}
