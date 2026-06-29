package com.ribminet.obill.data.remote

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** Info rilis terbaru hasil pengecekan ke GitLab Releases. */
data class ReleaseInfo(
    val versionName: String,
    val notes: String,
    val downloadUrl: String,
    val pageUrl: String,
)

private data class GitlabAssetLinkDto(
    val name: String? = null,
    val url: String? = null,
    @SerializedName("direct_asset_url") val directAssetUrl: String? = null,
    @SerializedName("link_type") val linkType: String? = null,
)

private data class GitlabAssetsDto(
    val links: List<GitlabAssetLinkDto> = emptyList(),
)

private data class GitlabLinksDto(
    val self: String? = null,
)

private data class GitlabReleaseDto(
    @SerializedName("tag_name") val tagName: String? = null,
    val name: String? = null,
    val description: String? = null,
    val assets: GitlabAssetsDto? = null,
    @SerializedName("_links") val links: GitlabLinksDto? = null,
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
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchFromUrl(url: String): ReleaseInfo? {
        val dto = getReleaseDto(url) ?: return null
        return dto.toReleaseInfo()
    }

    private fun fetchLatestFromList(): ReleaseInfo? {
        val body = getBody(UpdateConfig.releasesApi) ?: return null
        val list = gson.fromJson(body, Array<GitlabReleaseDto>::class.java) ?: return null
        return list.firstOrNull()?.toReleaseInfo()
    }

    private fun getReleaseDto(url: String): GitlabReleaseDto? {
        val body = getBody(url) ?: return null
        return gson.fromJson(body, GitlabReleaseDto::class.java)
    }

    private fun getBody(url: String): String? {
        val builder = Request.Builder().url(url).header("Accept", "application/json")
        if (UpdateConfig.ACCESS_TOKEN.isNotBlank()) {
            builder.header("PRIVATE-TOKEN", UpdateConfig.ACCESS_TOKEN)
        }
        client.newCall(builder.build()).execute().use { resp ->
            if (!resp.isSuccessful) return null
            return resp.body?.string()
        }
    }

    private fun GitlabReleaseDto.toReleaseInfo(): ReleaseInfo? {
        val tag = tagName ?: return null
        val version = tag.removePrefix("v").trim()
        if (version.isBlank()) return null
        val apkLink = assets?.links?.firstOrNull { link ->
            link.name?.endsWith(".apk", ignoreCase = true) == true ||
                link.directAssetUrl?.contains(".apk", ignoreCase = true) == true ||
                link.url?.contains(".apk", ignoreCase = true) == true
        }
        val apkUrl = apkLink?.directAssetUrl ?: apkLink?.url
        val page = links?.self ?: UpdateConfig.releasesPage
        return ReleaseInfo(
            versionName = version,
            notes = description?.trim().orEmpty(),
            downloadUrl = apkUrl ?: page,
            pageUrl = page,
        )
    }
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
