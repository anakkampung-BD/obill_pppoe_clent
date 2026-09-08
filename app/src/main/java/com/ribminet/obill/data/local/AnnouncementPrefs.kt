package com.ribminet.obill.data.local

import android.content.Context

/**
 * Preferensi pengumuman lokal.
 * - Tombol "Sudah Baca" menahan auto-popup modal selama 24 jam.
 * - ID yang sudah ditandai baca disimpan agar UI konsisten antar sesi
 *   (meski list API belum mengembalikan is_read).
 */
class AnnouncementPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("obill_announcements", Context.MODE_PRIVATE)

    fun isModalSnoozed(nowMs: Long = System.currentTimeMillis()): Boolean =
        nowMs < prefs.getLong(KEY_MODAL_SNOOZE_UNTIL, 0L)

    /** Tahan auto-tampil modal hingga [durationMs] dari sekarang (default 24 jam). */
    fun snoozeModal(durationMs: Long = MODAL_SNOOZE_MS, nowMs: Long = System.currentTimeMillis()) {
        prefs.edit().putLong(KEY_MODAL_SNOOZE_UNTIL, nowMs + durationMs).apply()
    }

    fun isRead(id: String): Boolean = readIds().contains(id)

    fun markRead(id: String) {
        if (id.isBlank()) return
        val next = readIds().toMutableSet().apply { add(id) }
        prefs.edit().putStringSet(KEY_READ, next).apply()
    }

    fun readIds(): Set<String> =
        prefs.getStringSet(KEY_READ, emptySet())?.toSet().orEmpty()

    fun clear() {
        prefs.edit()
            .remove(KEY_MODAL_SNOOZE_UNTIL)
            .remove(KEY_READ)
            .apply()
    }

    companion object {
        private const val KEY_MODAL_SNOOZE_UNTIL = "modal_snooze_until_ms"
        private const val KEY_READ = "read_ids"
        const val MODAL_SNOOZE_MS: Long = 24L * 60L * 60L * 1000L
    }
}
