package com.ribminet.obill.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.ribminet.obill.data.remote.ReleaseInfo
import com.ribminet.obill.data.remote.UpdateChecker
import com.ribminet.obill.data.remote.isApkUrl
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
     * Unduh APK dari [info] ke storage privat aplikasi.
     * Urutan: browser_download_url (tanpa auth) → Assets API (dengan auth bila ada).
     * [onProgress] dipanggil dengan nilai 0f..1f (atau -1f bila ukuran tak diketahui).
     */
    suspend fun download(
        context: Context,
        info: ReleaseInfo,
        onProgress: (Float) -> Unit,
    ): File? = withContext(Dispatchers.IO) {
        val attempts = buildList {
            if (isApkUrl(info.downloadUrl)) {
                add(DownloadTarget(info.downloadUrl, useApiAuth = false, octetStream = false))
            }
            if (info.apiAssetUrl.isNotBlank()) {
                add(DownloadTarget(info.apiAssetUrl, useApiAuth = true, octetStream = true))
            }
        }
        if (attempts.isEmpty()) return@withContext null

        val dir = File(context.getExternalFilesDir(null), "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val outFile = File(dir, "obill-update.apk")

        for (target in attempts) {
            val ok = runCatching {
                downloadTo(outFile, target, onProgress)
            }.getOrDefault(false)
            if (ok && looksLikeApk(outFile)) return@withContext outFile
            outFile.delete()
        }
        null
    }

    /** Kompatibilitas: unduh dari satu URL (browser download, tanpa auth). */
    suspend fun download(
        context: Context,
        url: String,
        onProgress: (Float) -> Unit,
    ): File? = download(
        context,
        ReleaseInfo(
            versionName = "",
            notes = "",
            downloadUrl = url,
            pageUrl = "",
        ),
        onProgress,
    )

    private data class DownloadTarget(
        val url: String,
        val useApiAuth: Boolean,
        val octetStream: Boolean,
    )

    private fun downloadTo(
        outFile: File,
        target: DownloadTarget,
        onProgress: (Float) -> Unit,
    ): Boolean {
        val builder = Request.Builder()
            .url(target.url)
            .header("User-Agent", "Obill-Android-Updater")
        if (target.octetStream) {
            builder.header("Accept", "application/octet-stream")
        }
        // Jangan kirim Authorization ke github.com/releases/download — sering gagal/redirect rusak.
        // Auth hanya untuk api.github.com (repo privat).
        if (target.useApiAuth) {
            UpdateChecker.applyAuth(builder)
        }
        client.newCall(builder.build()).execute().use { resp ->
            if (!resp.isSuccessful) return false
            val body = resp.body ?: return false
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
        return outFile.exists() && outFile.length() > 1024
    }

    /** APK/ZIP magic: 'PK' di awal file. */
    private fun looksLikeApk(file: File): Boolean {
        if (!file.exists() || file.length() < 4) return false
        return runCatching {
            file.inputStream().use { input ->
                val b0 = input.read()
                val b1 = input.read()
                b0 == 'P'.code && b1 == 'K'.code
            }
        }.getOrDefault(false)
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
