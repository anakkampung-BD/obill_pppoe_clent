package com.ribminet.obill.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.ribminet.obill.data.remote.UpdateChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/** Mengunduh APK rilis terbaru lalu memicu installer sistem. */
object ApkUpdater {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    /**
     * Unduh APK dari [url] ke storage privat aplikasi.
     * [onProgress] dipanggil dengan nilai 0f..1f (atau -1f bila ukuran tak diketahui).
     * Mengembalikan file APK bila sukses, atau null bila gagal.
     */
    suspend fun download(
        context: Context,
        url: String,
        onProgress: (Float) -> Unit,
    ): File? = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.getExternalFilesDir(null), "updates").apply { mkdirs() }
            // Bersihkan unduhan lama agar tidak menumpuk.
            dir.listFiles()?.forEach { it.delete() }
            val outFile = File(dir, "obill-update.apk")

            val builder = Request.Builder().url(url)
            // GitHub: Bearer token (bila repo privat). Unduhan asset publik tidak perlu token.
            UpdateChecker.applyAuth(builder)
            client.newCall(builder.build()).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val body = resp.body ?: return@withContext null
                val total = body.contentLength()
                body.byteStream().use { input ->
                    outFile.outputStream().use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var downloaded = 0L
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            downloaded += read
                            if (total > 0) {
                                onProgress((downloaded.toFloat() / total).coerceIn(0f, 1f))
                            } else {
                                onProgress(-1f)
                            }
                        }
                    }
                }
            }
            outFile
        } catch (e: Exception) {
            null
        }
    }

    /** Luncurkan installer sistem untuk file APK yang sudah diunduh. */
    fun installApk(context: Context, file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
