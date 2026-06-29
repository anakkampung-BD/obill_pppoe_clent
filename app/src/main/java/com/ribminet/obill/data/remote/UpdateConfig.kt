package com.ribminet.obill.data.remote

import java.net.URLEncoder

/**
 * Konfigurasi auto-deteksi versi rilis terbaru dari GitLab.
 *
 * Repo publik: https://gitrepo.aks-network.co.id/siribere/obill_pppoe_client
 *
 * Pengecekan memakai GitLab Releases API (tanpa token untuk repo publik).
 * APK diunduh dari asset rilis (Package Registry) lalu dipasang otomatis.
 *
 * [ACCESS_TOKEN] hanya diisi bila project kembali dijadikan privat.
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

    /** Daftar rilis (fallback bila permalink/latest redirect). */
    val releasesApi: String
        get() = "$HOST/api/v4/projects/$encodedProject/releases"

    /** Halaman rilis (fallback bila APK tidak ditemukan pada aset). */
    val releasesPage: String
        get() = "$HOST/$PROJECT_PATH/-/releases"
}
