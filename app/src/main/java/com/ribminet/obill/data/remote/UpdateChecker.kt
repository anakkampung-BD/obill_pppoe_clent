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
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    /** Ambil rilis terbaru. Mengembalikan null bila belum dikonfigurasi atau gagal. */
    suspend fun fetchLatest(): ReleaseInfo? = withContext(Dispatchers.IO) {
        if (!UpdateConfig.isConfigured) return@withContext null
        try {
            val builder = Request.Builder()
                .url(UpdateConfig.latestReleaseApi)
                .header("Accept", "application/json")
            if (UpdateConfig.ACCESS_TOKEN.isNotBlank()) {
                builder.header("PRIVATE-TOKEN", UpdateConfig.ACCESS_TOKEN)
            }
            client.newCall(builder.build()).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val dto = gson.fromJson(resp.body?.string(), GitlabReleaseDto::class.java)
                    ?: return@withContext null
                val tag = dto.tagName ?: return@withContext null
                val version = tag.removePrefix("v").trim()
                if (version.isBlank()) return@withContext null

                val apkLink = dto.assets?.links?.firstOrNull { link ->
                    val n = link.name?.endsWith(".apk", ignoreCase = true) == true
                    val u = link.directAssetUrl?.endsWith(".apk", ignoreCase = true) == true ||
                        link.url?.endsWith(".apk", ignoreCase = true) == true
                    n || u
                }
                val apkUrl = apkLink?.directAssetUrl ?: apkLink?.url
                val page = dto.links?.self ?: UpdateConfig.releasesPage

                ReleaseInfo(
                    versionName = version,
                    notes = dto.description?.trim().orEmpty(),
                    downloadUrl = apkUrl ?: page,
                    pageUrl = page,
                )
            }
        } catch (e: Exception) {
            null
        }
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
