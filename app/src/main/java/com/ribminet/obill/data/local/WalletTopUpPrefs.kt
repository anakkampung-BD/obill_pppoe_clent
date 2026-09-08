package com.ribminet.obill.data.local

import android.content.Context

/**
 * Cache QRIS top-up wallet — status poll sering tidak mengembalikan ulang `qris_image_url`.
 */
class WalletTopUpPrefs(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun save(
        topupCode: String,
        qrisImageUrl: String?,
        qrisString: String?,
        payAmount: Long?,
        amountBase: Long?,
        expireMinutes: Int?,
        expiresAtMs: Long? = null,
        merchantName: String? = null,
    ) {
        if (topupCode.isBlank()) return
        prefs.edit()
            .putString(KEY_CODE, topupCode)
            .putString(KEY_QRIS_URL, qrisImageUrl)
            .putString(KEY_QRIS_STRING, qrisString)
            .putLong(KEY_PAY_AMOUNT, payAmount ?: -1L)
            .putLong(KEY_AMOUNT_BASE, amountBase ?: -1L)
            .putInt(KEY_EXPIRE_MIN, expireMinutes ?: -1)
            .putLong(KEY_EXPIRES_AT, expiresAtMs ?: -1L)
            .putString(KEY_MERCHANT, merchantName)
            .apply()
    }

    fun cachedTopupCode(): String? = prefs.getString(KEY_CODE, null)?.takeIf { it.isNotBlank() }

    fun qrisImageUrl(forCode: String): String? {
        if (cachedTopupCode() != forCode) return null
        return prefs.getString(KEY_QRIS_URL, null)?.takeIf { it.isNotBlank() }
    }

    fun qrisString(forCode: String): String? {
        if (cachedTopupCode() != forCode) return null
        return prefs.getString(KEY_QRIS_STRING, null)?.takeIf { it.isNotBlank() }
    }

    fun payAmount(forCode: String): Long? {
        if (cachedTopupCode() != forCode) return null
        return prefs.getLong(KEY_PAY_AMOUNT, -1L).takeIf { it >= 0L }
    }

    fun amountBase(forCode: String): Long? {
        if (cachedTopupCode() != forCode) return null
        return prefs.getLong(KEY_AMOUNT_BASE, -1L).takeIf { it >= 0L }
    }

    fun expireMinutes(forCode: String): Int? {
        if (cachedTopupCode() != forCode) return null
        return prefs.getInt(KEY_EXPIRE_MIN, -1).takeIf { it > 0 }
    }

    fun expiresAtMs(forCode: String): Long? {
        if (cachedTopupCode() != forCode) return null
        return prefs.getLong(KEY_EXPIRES_AT, -1L).takeIf { it > 0L }
    }

    fun merchantName(forCode: String): String? {
        if (cachedTopupCode() != forCode) return null
        return prefs.getString(KEY_MERCHANT, null)?.takeIf { it.isNotBlank() }
    }

    fun clear(topupCode: String? = null) {
        if (topupCode != null && cachedTopupCode() != null && cachedTopupCode() != topupCode) return
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS = "obill_wallet_topup"
        private const val KEY_CODE = "topup_code"
        private const val KEY_QRIS_URL = "qris_image_url"
        private const val KEY_QRIS_STRING = "qris_string"
        private const val KEY_PAY_AMOUNT = "pay_amount"
        private const val KEY_AMOUNT_BASE = "amount_base"
        private const val KEY_EXPIRE_MIN = "expire_minutes"
        private const val KEY_EXPIRES_AT = "expires_at_ms"
        private const val KEY_MERCHANT = "merchant_name"
    }
}
