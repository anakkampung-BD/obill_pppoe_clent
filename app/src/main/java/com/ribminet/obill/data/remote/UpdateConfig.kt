package com.ribminet.obill.data.remote

/**
 * Konfigurasi auto-deteksi versi rilis terbaru dari GitHub Releases.
 *
 * Repo: https://github.com/anakkampung-BD/obill_pppoe_clent
 *
 * Pengecekan memakai GitHub Releases API (tanpa token untuk repo publik).
 * APK diunduh dari asset rilis lalu dipasang otomatis.
 *
 * [ACCESS_TOKEN] hanya diisi bila repo privat (classic PAT dengan scope `repo` /
 * fine-grained: Contents read).
 *
 * Catatan migrasi: rilis transisi (mis. 3.0.5) boleh tetap diunggah ke repo GitLab
 * lama agar klien 3.0.4 masih bisa mengunduh; binary rilis itu sudah memakai URL
 * GitHub ini untuk pengecekan update selanjutnya.
 */
object UpdateConfig {
    const val HOST = "https://github.com"
    const val API_HOST = "https://api.github.com"
    /** owner/repo — harus sama dengan path di github.com */
    const val PROJECT_PATH = "anakkampung-BD/obill_pppoe_clent"
    const val ACCESS_TOKEN = "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"

    val isConfigured: Boolean get() = PROJECT_PATH.isNotBlank()

    /** Endpoint rilis terbaru. */
    val latestReleaseApi: String
        get() = "$API_HOST/repos/$PROJECT_PATH/releases/latest"

    /** Daftar rilis (fallback). */
    val releasesApi: String
        get() = "$API_HOST/repos/$PROJECT_PATH/releases"

    /** Halaman rilis (fallback bila APK tidak ditemukan pada aset). */
    val releasesPage: String
        get() = "$HOST/$PROJECT_PATH/releases"
}
