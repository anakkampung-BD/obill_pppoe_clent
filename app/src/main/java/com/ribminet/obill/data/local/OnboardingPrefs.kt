package com.ribminet.obill.data.local

import android.content.Context

/** Menyimpan status apakah onboarding izin sudah pernah ditampilkan. */
class OnboardingPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("obill_onboarding", Context.MODE_PRIVATE)

    var permissionsRequested: Boolean
        get() = prefs.getBoolean(KEY_PERMISSIONS, false)
        set(value) = prefs.edit().putBoolean(KEY_PERMISSIONS, value).apply()

    companion object {
        private const val KEY_PERMISSIONS = "permissions_requested"
    }
}
