package app.accordgarage.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Encrypted backup: a zip of the database, photos and (optionally) manuals,
 * encrypted with AES-256-GCM under a key derived from the owner's passphrase.
 * File layout: "AGBK1" + 16-byte salt + 12-byte IV + ciphertext.
 */
class BackupService(private val context: Context, private val db: () -> AppDatabase) {

    suspend fun export(target: Uri, passphrase: CharArray, includeManuals: Boolean) = withContext(Dispatchers.IO) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, key(passphrase, salt), GCMParameterSpec(128, iv))
        }
        val dbFile = context.getDatabasePath(AppDatabase.FILE_NAME)
        val out: OutputStream = requireNotNull(context.contentResolver.openOutputStream(target)) { "Couldn't write there" }
        out.use { raw ->
            raw.write(MAGIC)
            raw.write(salt)
            raw.write(iv)
            ZipOutputStream(CipherOutputStream(raw, cipher)).use { zip ->
                addFile(zip, "db/${AppDatabase.FILE_NAME}", dbFile)
                File(context.filesDir, "attachments").listFiles()?.forEach { addFile(zip, "attachments/${it.name}", it) }
                if (includeManuals) {
                    File(context.filesDir, "manuals").listFiles()?.forEach { addFile(zip, "manuals/${it.name}", it) }
                }
            }
        }
    }

    /** Restores a backup over the current data, then restarts the app. */
    suspend fun restore(source: Uri, passphrase: CharArray) = withContext(Dispatchers.IO) {
        val staging = File(context.cacheDir, "restore").apply { deleteRecursively(); mkdirs() }
        val input: InputStream = requireNotNull(context.contentResolver.openInputStream(source)) { "Couldn't open that file" }
        input.use { raw ->
            val data = DataInputStream(raw)
            val magic = ByteArray(MAGIC.size).also { data.readFully(it) }
            require(magic.contentEquals(MAGIC)) { "That isn't an Accord Garage backup" }
            val salt = ByteArray(16).also { data.readFully(it) }
            val iv = ByteArray(12).also { data.readFully(it) }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.DECRYPT_MODE, key(passphrase, salt), GCMParameterSpec(128, iv))
            }
            try {
                ZipInputStream(CipherInputStream(data, cipher)).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        val name = entry.name
                        require(!name.contains("..")) { "Bad entry in backup" }
                        val f = File(staging, name)
                        f.parentFile?.mkdirs()
                        f.outputStream().use { zip.copyTo(it) }
                    }
                }
            } catch (e: Exception) {
                throw IllegalStateException("Wrong passphrase, or the file is damaged", e)
            }
        }
        val stagedDb = File(staging, "db/${AppDatabase.FILE_NAME}")
        require(stagedDb.exists() && stagedDb.length() > 0) { "Wrong passphrase, or the file is damaged" }

        db().close()
        val dbFile = context.getDatabasePath(AppDatabase.FILE_NAME)
        listOf("", "-journal", "-wal", "-shm").forEach { File(dbFile.path + it).delete() }
        stagedDb.copyTo(dbFile, overwrite = true)

        for (folder in listOf("attachments", "manuals")) {
            val staged = File(staging, folder)
            val live = File(context.filesDir, folder)
            if (staged.exists()) {
                live.deleteRecursively()
                staged.copyRecursively(live, overwrite = true)
            }
        }
        staging.deleteRecursively()
    }

    fun restartApp() {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        if (intent != null) context.startActivity(intent)
        Runtime.getRuntime().exit(0)
    }

    private fun addFile(zip: ZipOutputStream, name: String, file: File) {
        if (!file.exists() || !file.isFile) return
        zip.putNextEntry(ZipEntry(name))
        file.inputStream().use { it.copyTo(zip) }
        zip.closeEntry()
    }

    private fun key(passphrase: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(passphrase, salt, 150_000, 256)
        val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        spec.clearPassword()
        return SecretKeySpec(bytes, "AES")
    }

    companion object {
        private val MAGIC = "AGBK1".toByteArray()
    }
}
