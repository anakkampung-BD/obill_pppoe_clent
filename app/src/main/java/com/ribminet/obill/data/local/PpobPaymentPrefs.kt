package com.ribminet.obill.data.local

import android.content.Context

/**
 * Cache QRIS untuk resume order unpaid.
 * `payment_status` server sering tidak mengembalikan ulang `qris_image_url`.
 */
class PpobPaymentPrefs(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun save(
        refId: String,
        qrisImageUrl: String?,
        qrisString: String?,
        payAmount: Long?,
        expireMinutes: Int?,
        expiresAtMs: Long? = null,
    ) {
        if (refId.isBlank()) return
        prefs.edit()
            .putString(KEY_REF, refId)
            .putString(KEY_QRIS_URL, qrisImageUrl)
            .putString(KEY_QRIS_STRING, qrisString)
            .putLong(KEY_PAY_AMOUNT, payAmount ?: -1L)
            .putInt(KEY_EXPIRE_MIN, expireMinutes ?: -1)
            .putLong(KEY_EXPIRES_AT, expiresAtMs ?: -1L)
            .apply()
    }

    fun cachedRefId(): String? = prefs.getString(KEY_REF, null)?.takeIf { it.isNotBlank() }

    fun qrisImageUrl(forRefId: String): String? {
        if (cachedRefId() != forRefId) return null
        return prefs.getString(KEY_QRIS_URL, null)?.takeIf { it.isNotBlank() }
    }

    fun qrisString(forRefId: String): String? {
        if (cachedRefId() != forRefId) return null
        return prefs.getString(KEY_QRIS_STRING, null)?.takeIf { it.isNotBlank() }
    }

    fun expireMinutes(forRefId: String): Int? {
        if (cachedRefId() != forRefId) return null
        val v = prefs.getInt(KEY_EXPIRE_MIN, -1)
        return v.takeIf { it > 0 }
    }

    fun expiresAtMs(forRefId: String): Long? {
        if (cachedRefId() != forRefId) return null
        val v = prefs.getLong(KEY_EXPIRES_AT, -1L)
        return v.takeIf { it > 0L }
    }

    fun clear(refId: String? = null) {
        if (refId != null && cachedRefId() != null && cachedRefId() != refId) return
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS = "obill_ppob_payment"
        private const val KEY_REF = "ref_id"
        private const val KEY_QRIS_URL = "qris_image_url"
        private const val KEY_QRIS_STRING = "qris_string"
        private const val KEY_PAY_AMOUNT = "pay_amount"
        private const val KEY_EXPIRE_MIN = "expire_minutes"
        private const val KEY_EXPIRES_AT = "expires_at_ms"
    }
}
