package com.ribminet.obill.data.remote

import android.content.Context

class TokenStore(context: Context) {

    private val prefs = context.getSharedPreferences("obill_session", Context.MODE_PRIVATE)

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit().apply {
            if (value == null) remove(KEY_TOKEN) else putString(KEY_TOKEN, value)
        }.apply()

    var expiresAt: String?
        get() = prefs.getString(KEY_EXPIRES, null)
        set(value) = prefs.edit().putString(KEY_EXPIRES, value).apply()

    var phone: String?
        get() = prefs.getString(KEY_PHONE, null)
        set(value) = prefs.edit().putString(KEY_PHONE, value).apply()

    var customerId: Int
        get() = prefs.getInt(KEY_CUSTOMER_ID, 0)
        set(value) = prefs.edit().putInt(KEY_CUSTOMER_ID, value).apply()

    var notificationSinceId: Int
        get() = prefs.getInt(KEY_NOTIF_SINCE, 0)
        set(value) = prefs.edit().putInt(KEY_NOTIF_SINCE, value).apply()

    val isLoggedIn: Boolean get() = !token.isNullOrBlank()

    fun clear() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_EXPIRES)
            .remove(KEY_CUSTOMER_ID)
            .remove(KEY_NOTIF_SINCE)
            .apply()
    }

    private companion object {
        const val KEY_TOKEN = "token"
        const val KEY_EXPIRES = "expires_at"
        const val KEY_PHONE = "phone"
        const val KEY_CUSTOMER_ID = "customer_id"
        const val KEY_NOTIF_SINCE = "notif_since_id"
    }
}
