package com.ribminet.obill.data.remote

/**
 * Konfigurasi koneksi ke API web pelanggan PPPoE.
 *
 * Ganti [BASE_URL] sesuai domain server produksi.
 * [APP_KEY] hanya diisi bila server mengaktifkan `customer_app_api_key`
 * (dikirim lewat header `X-Customer-App-Key`). Biarkan kosong jika tidak dipakai.
 */
object ApiConfig {
    const val BASE_URL = "https://sln.onesky.id/"
    const val APP_KEY = ""
}
