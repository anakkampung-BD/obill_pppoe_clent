package com.ribminet.obill.data.local

import android.content.Context

/** Cache QRIS tagihan PPPoE — poll sering tidak mengembalikan ulang `qris_image_url`. */
class BillPaymentPrefs(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun save(
        orderNo: String,
        qrisImageUrl: String?,
        qrisString: String?,
        payAmount: Long?,
        amountBase: Long?,
        expireMinutes: Int?,
        expiresAtMs: Long? = null,
        merchantName: String? = null,
    ) {
        if (orderNo.isBlank()) return
        prefs.edit()
            .putString(KEY_ORDER_NO, orderNo)
            .putString(KEY_QRIS_URL, qrisImageUrl)
            .putString(KEY_QRIS_STRING, qrisString)
            .putLong(KEY_PAY_AMOUNT, payAmount ?: -1L)
            .putLong(KEY_AMOUNT_BASE, amountBase ?: -1L)
            .putInt(KEY_EXPIRE_MIN, expireMinutes ?: -1)
            .putLong(KEY_EXPIRES_AT, expiresAtMs ?: -1L)
            .putString(KEY_MERCHANT, merchantName)
            .apply()
    }

    fun cachedOrderNo(): String? = prefs.getString(KEY_ORDER_NO, null)?.takeIf { it.isNotBlank() }

    fun qrisImageUrl(forOrderNo: String): String? {
        if (cachedOrderNo() != forOrderNo) return null
        return prefs.getString(KEY_QRIS_URL, null)?.takeIf { it.isNotBlank() }
    }

    fun qrisString(forOrderNo: String): String? {
        if (cachedOrderNo() != forOrderNo) return null
        return prefs.getString(KEY_QRIS_STRING, null)?.takeIf { it.isNotBlank() }
    }

    fun payAmount(forOrderNo: String): Long? {
        if (cachedOrderNo() != forOrderNo) return null
        return prefs.getLong(KEY_PAY_AMOUNT, -1L).takeIf { it >= 0L }
    }

    fun amountBase(forOrderNo: String): Long? {
        if (cachedOrderNo() != forOrderNo) return null
        return prefs.getLong(KEY_AMOUNT_BASE, -1L).takeIf { it >= 0L }
    }

    fun expireMinutes(forOrderNo: String): Int? {
        if (cachedOrderNo() != forOrderNo) return null
        return prefs.getInt(KEY_EXPIRE_MIN, -1).takeIf { it > 0 }
    }

    fun expiresAtMs(forOrderNo: String): Long? {
        if (cachedOrderNo() != forOrderNo) return null
        return prefs.getLong(KEY_EXPIRES_AT, -1L).takeIf { it > 0L }
    }

    fun merchantName(forOrderNo: String): String? {
        if (cachedOrderNo() != forOrderNo) return null
        return prefs.getString(KEY_MERCHANT, null)?.takeIf { it.isNotBlank() }
    }

    fun clear(orderNo: String? = null) {
        if (orderNo != null && cachedOrderNo() != null && cachedOrderNo() != orderNo) return
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS = "obill_bill_payment"
        private const val KEY_ORDER_NO = "order_no"
        private const val KEY_QRIS_URL = "qris_image_url"
        private const val KEY_QRIS_STRING = "qris_string"
        private const val KEY_PAY_AMOUNT = "pay_amount"
        private const val KEY_AMOUNT_BASE = "amount_base"
        private const val KEY_EXPIRE_MIN = "expire_minutes"
        private const val KEY_EXPIRES_AT = "expires_at_ms"
        private const val KEY_MERCHANT = "merchant_name"
    }
}
