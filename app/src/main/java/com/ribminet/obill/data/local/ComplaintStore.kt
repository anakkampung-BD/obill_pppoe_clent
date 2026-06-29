package com.ribminet.obill.data.local

import android.content.Context
import com.google.gson.Gson
import com.ribminet.obill.data.Complaint

/**
 * Penyimpanan riwayat pengaduan di perangkat (SharedPreferences).
 * Server tidak menyimpan pengaduan, jadi riwayat dipertahankan lokal,
 * dipisahkan per nomor akun agar tidak tercampur antar pengguna.
 */
class ComplaintStore(context: Context) {

    private val prefs = context.getSharedPreferences("obill_complaints", Context.MODE_PRIVATE)
    private val gson = Gson()

    private fun keyFor(phone: String?): String = "complaints_" + (phone?.takeIf { it.isNotBlank() } ?: "anon")

    fun load(phone: String?): List<Complaint> {
        val json = prefs.getString(keyFor(phone), null) ?: return emptyList()
        return try {
            gson.fromJson(json, Array<Complaint>::class.java)?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(phone: String?, complaints: List<Complaint>) {
        prefs.edit().putString(keyFor(phone), gson.toJson(complaints)).apply()
    }
}
