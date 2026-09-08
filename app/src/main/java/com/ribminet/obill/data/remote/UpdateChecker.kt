package com.ribminet.obill.data.remote

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** Info rilis terbaru hasil pengecekan ke GitHub Releases. */
data class ReleaseInfo(
    val versionName: String,
    val notes: String,
    /** URL unduhan langsung (…/releases/download/…/xxx.apk). */
    val downloadUrl: String,
    /**
     * URL Assets API GitHub (…/releases/assets/{id}) untuk unduhan repo privat
     * dengan header Accept: application/octet-stream.
     */
    val apiAssetUrl: String = "",
    val pageUrl: String,
) {
    /** True bila ada URL yang bisa dipakai unduh APK in-app. */
    val hasApkDownload: Boolean
        get() = isApkUrl(downloadUrl) || apiAssetUrl.isNotBlank()
}

private data class GithubAssetDto(
    val name: String? = null,
    val url: String? = null,
    @SerializedName("browser_download_url") val browserDownloadUrl: String? = null,
    @SerializedName("content_type") val contentType: String? = null,
)

private data class GithubReleaseDto(
    @SerializedName("tag_name") val tagName: String? = null,
    val name: String? = null,
    val body: String? = null,
    @SerializedName("html_url") val htmlUrl: String? = null,
    val assets: List<GithubAssetDto> = emptyList(),
)

object UpdateChecker {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
    private val gson = Gson()

    /** Ambil rilis terbaru. Mengembalikan null bila belum dikonfigurasi atau gagal. */
    suspend fun fetchLatest(): ReleaseInfo? = withContext(Dispatchers.IO) {
        if (!UpdateConfig.isConfigured) return@withContext null
        try {
            fetchFromUrl(UpdateConfig.latestReleaseApi)
                ?: fetchLatestFromList()
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchFromUrl(url: String): ReleaseInfo? {
        val dto = getReleaseDto(url) ?: return null
        return dto.toReleaseInfo()
    }

    private fun fetchLatestFromList(): ReleaseInfo? {
        val body = getBody(UpdateConfig.releasesApi) ?: return null
        val list = gson.fromJson(body, Array<GithubReleaseDto>::class.java) ?: return null
        return list.firstOrNull()?.toReleaseInfo()
    }

    private fun getReleaseDto(url: String): GithubReleaseDto? {
        val body = getBody(url) ?: return null
        return gson.fromJson(body, GithubReleaseDto::class.java)
    }

    private fun getBody(url: String): String? {
        val builder = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "Obill-Android-Updater")
            .header("X-GitHub-Api-Version", "2022-11-28")
        applyAuth(builder)
        client.newCall(builder.build()).execute().use { resp ->
            if (!resp.isSuccessful) return null
            return resp.body?.string()
        }
    }

    /** Auth hanya untuk api.github.com bila token diisi (repo privat). */
    fun applyAuth(builder: Request.Builder) {
        if (UpdateConfig.ACCESS_TOKEN.isNotBlank()) {
            builder.header("Authorization", "Bearer ${UpdateConfig.ACCESS_TOKEN}")
        }
    }

    private fun GithubReleaseDto.toReleaseInfo(): ReleaseInfo? {
        val tag = tagName ?: return null
        val version = tag.removePrefix("v").trim()
        if (version.isBlank()) return null
        val apkAsset = assets.firstOrNull { asset ->
            asset.name?.endsWith(".apk", ignoreCase = true) == true ||
                asset.contentType?.contains("android.package", ignoreCase = true) == true ||
                isApkUrl(asset.browserDownloadUrl)
        }
        val browserUrl = apkAsset?.browserDownloadUrl.orEmpty()
        val apiUrl = apkAsset?.url.orEmpty()
        val page = htmlUrl?.takeIf { it.isNotBlank() } ?: UpdateConfig.releasesPage
        // Jangan jatuhkan ke halaman HTML sebagai "downloadUrl" — itu memicu buka browser.
        val download = when {
            isApkUrl(browserUrl) -> browserUrl
            else -> ""
        }
        if (download.isBlank() && apiUrl.isBlank()) return null
        return ReleaseInfo(
            versionName = version,
            notes = body?.trim().orEmpty(),
            downloadUrl = download,
            apiAssetUrl = apiUrl,
            pageUrl = page,
        )
    }
}

internal fun isApkUrl(url: String?): Boolean {
    if (url.isNullOrBlank()) return false
    val path = url.substringBefore('?').substringBefore('#')
    return path.endsWith(".apk", ignoreCase = true)
}

/** Perbandingan versi semantik sederhana (mis. "3.0.1" > "3.0.0"). */
object VersionUtil {
    fun isNewer(latest: String, current: String): Boolean {
        val a = parse(latest)
        val b = parse(current)
        val n = maxOf(a.size, b.size)
        for (i in 0 until n) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    private fun parse(v: String): List<Int> =
        v.trim().removePrefix("v")
            .split(".", "-", "+", "_")
            .mapNotNull { part -> part.takeWhile { it.isDigit() }.toIntOrNull() }
}
