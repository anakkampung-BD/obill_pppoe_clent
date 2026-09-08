package com.ribminet.obill.data.remote

/**
 * Konfigurasi auto-deteksi versi rilis terbaru dari GitHub Releases.
 *
 * Repo: https://github.com/anakkampung-BD/obill_pppoe_clent (publik)
 *
 * Pengecekan memakai GitHub Releases API. APK diunduh dari asset rilis lalu
 * dipasang otomatis (in-app), tanpa membuka halaman browser.
 *
 * [ACCESS_TOKEN] biarkan kosong untuk repo publik.
 * Mengisi token yang sudah dicabut/invalid justru membuat cek update gagal (401).
 * Isi hanya bila repo privat (classic PAT scope `repo` / fine-grained Contents read).
 */
object UpdateConfig {
    const val HOST = "https://github.com"
    const val API_HOST = "https://api.github.com"
    /** owner/repo — harus sama dengan path di github.com */
    const val PROJECT_PATH = "anakkampung-BD/obill_pppoe_clent"
    /** Kosong = repo publik. Jangan commit PAT ke repo publik (GitHub akan revoke). */
    const val ACCESS_TOKEN = ""

    val isConfigured: Boolean get() = PROJECT_PATH.isNotBlank()

    /** Endpoint rilis terbaru. */
    val latestReleaseApi: String
        get() = "$API_HOST/repos/$PROJECT_PATH/releases/latest"

    /** Daftar rilis (fallback). */
    val releasesApi: String
        get() = "$API_HOST/repos/$PROJECT_PATH/releases"

    /** Halaman rilis (hanya untuk teks bantuan, bukan unduhan in-app). */
    val releasesPage: String
        get() = "$HOST/$PROJECT_PATH/releases"
}
