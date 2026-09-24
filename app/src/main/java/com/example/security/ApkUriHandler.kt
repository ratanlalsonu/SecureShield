package com.example.security

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest

object ApkUriHandler {

    data class ResolvedApk(
        val file: File,
        val originalFileName: String,
        val fileSize: Long,
        val sha256: String,
        val sourceApp: String
    )

    fun resolveAndCopyApk(
        context: Context,
        uri: Uri,
        sourcePackageHint: String? = null
    ): Result<ResolvedApk> {
        return try {
            val contentResolver = context.contentResolver
            var fileName = "downloaded_app.apk"
            var fileSize = 0L

            // 1. Query metadata if content URI
            if (uri.scheme == "content") {
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIndex != -1) {
                            val name = cursor.getString(nameIndex)
                            if (!name.isNullOrBlank()) {
                                // sanitize filename to prevent path traversal
                                fileName = File(name).name
                            }
                        }
                        if (sizeIndex != -1) {
                            fileSize = cursor.getLong(sizeIndex)
                        }
                    }
                }
            } else if (uri.scheme == "file") {
                val path = uri.path
                if (path != null) {
                    val f = File(path)
                    fileName = f.name
                    fileSize = f.length()
                }
            }

            // 2. Resolve source app name
            val sourceApp = resolveSourceApp(context, uri, sourcePackageHint)

            // 3. Create safe temporary file in internal cache
            val cacheDir = File(context.cacheDir, "apk_analysis").apply { mkdirs() }
            val tempFile = File(cacheDir, "temp_target.apk")
            if (tempFile.exists()) tempFile.delete()

            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(32 * 1024)
            var bytesCopied = 0L
            val maxSizeBytes = 500L * 1024L * 1024L // 500 MB safety limit

            val inputStream: InputStream? = contentResolver.openInputStream(uri)
                ?: return Result.failure(IllegalStateException("Unable to open input stream for URI: $uri"))

            inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        bytesCopied += read
                        if (bytesCopied > maxSizeBytes) {
                            tempFile.delete()
                            return Result.failure(SecurityException("APK file size exceeds maximum safety limit (500MB)"))
                        }
                        digest.update(buffer, 0, read)
                        output.write(buffer, 0, read)
                    }
                }
            }

            if (fileSize <= 0) {
                fileSize = tempFile.length()
            }

            // 4. Calculate SHA-256
            val sha256 = digest.digest().joinToString("") { "%02x".format(it) }

            // 5. Basic ZIP signature check (PK header 0x50, 0x4B, 0x03, 0x04)
            if (tempFile.length() < 4) {
                tempFile.delete()
                return Result.failure(IllegalArgumentException("File is too small to be a valid Android APK archive."))
            }

            val header = ByteArray(4)
            tempFile.inputStream().use { it.read(header) }
            val isZip = header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() &&
                    (header[2] == 0x03.toByte() || header[2] == 0x05.toByte() || header[2] == 0x07.toByte())

            if (!isZip) {
                tempFile.delete()
                return Result.failure(IllegalArgumentException("The provided file does not have a valid ZIP/APK signature."))
            }

            Result.success(
                ResolvedApk(
                    file = tempFile,
                    originalFileName = fileName,
                    fileSize = fileSize,
                    sha256 = sha256,
                    sourceApp = sourceApp
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun resolveSourceApp(context: Context, uri: Uri, sourceHint: String?): String {
        if (!sourceHint.isNullOrBlank()) {
            return try {
                val pm = context.packageManager
                val appInfo = pm.getApplicationInfo(sourceHint, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                sourceHint
            }
        }

        // Check content provider authority hints
        val authority = uri.authority?.lowercase() ?: ""
        return when {
            authority.contains("telegram") || authority.contains("org.telegram") -> "Telegram"
            authority.contains("whatsapp") -> "WhatsApp"
            authority.contains("chrome") -> "Google Chrome"
            authority.contains("download") -> "Downloads"
            authority.contains("filemanager") || authority.contains("files") -> "File Manager"
            authority.contains("browser") -> "Web Browser"
            else -> "Source unavailable"
        }
    }
}
