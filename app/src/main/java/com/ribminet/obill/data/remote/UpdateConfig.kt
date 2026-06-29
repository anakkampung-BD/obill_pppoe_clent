package com.ribminet.obill.data.remote

import java.net.URLEncoder

/**
 * Konfigurasi auto-deteksi versi rilis terbaru dari GitLab.
 *
 * Repo: https://gitrepo.aks-network.co.id/siribere/obill_pppoe_client
 *
 * Pengecekan memakai GitLab Releases API:
 *   GET {HOST}/api/v4/projects/{PROJECT_PATH (url-encoded)}/releases/permalink/latest
 *
 * Aplikasi membandingkan `tag_name` rilis terbaru (mis. "v3.0.0" / "3.0.0")
 * dengan versi terpasang. Bila lebih baru, APK pada aset rilis diunduh lalu
 * dipasang otomatis.
 *
 * Catatan: bila project bersifat privat, GitLab API perlu token baca.
 * Isi [ACCESS_TOKEN] dengan Personal/Project Access Token (scope: read_api).
 * Untuk project publik, biarkan kosong.
 */
object UpdateConfig {
    const val HOST = "https://gitrepo.aks-network.co.id"
    const val PROJECT_PATH = "siribere/obill_pppoe_client"
    const val ACCESS_TOKEN = ""

    val isConfigured: Boolean get() = PROJECT_PATH.isNotBlank()

    private val encodedProject: String
        get() = URLEncoder.encode(PROJECT_PATH, "UTF-8")

    /** Endpoint rilis terbaru (permalink/latest). */
    val latestReleaseApi: String
        get() = "$HOST/api/v4/projects/$encodedProject/releases/permalink/latest"

    /** Halaman rilis (fallback bila APK tidak ditemukan pada aset). */
    val releasesPage: String
        get() = "$HOST/$PROJECT_PATH/-/releases"
}
