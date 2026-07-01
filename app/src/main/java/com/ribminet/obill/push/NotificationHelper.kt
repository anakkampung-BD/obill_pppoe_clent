package com.ribminet.obill.push

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.ribminet.obill.MainActivity
import com.ribminet.obill.R
import com.ribminet.obill.data.remote.NotificationDto

/** Menyimpan ID notifikasi yang sudah ditampilkan sebagai popup sistem. */
class ShownNotificationStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isShown(id: Int): Boolean = prefs.getBoolean(key(id), false)

    fun markShown(id: Int) {
        prefs.edit().putBoolean(key(id), true).apply()
    }

    fun clear() = prefs.edit().clear().apply()

    private fun key(id: Int) = "shown_$id"

    companion object {
        private const val PREFS = "obill_shown_notifs"
    }
}

/** Menampilkan notifikasi sistem (heads-up) — dipakai saat app background / sync worker. */
object NotificationHelper {

    const val CHANNEL_ID = "obill_notifications"
    const val EXTRA_NOTIF_TYPE = "notif_type"
    const val EXTRA_ORDER_ID = "order_id"
    const val EXTRA_NOTIF_ID = "notif_id"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = context.getSystemService(NotificationManager::class.java) ?: return
        if (mgr.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = android.app.NotificationChannel(
            CHANNEL_ID,
            "Notifikasi Obill",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Pengingat tagihan, pembayaran, dan status layanan"
            enableVibration(true)
        }
        mgr.createNotificationChannel(channel)
    }

    fun showFromDto(context: Context, dto: NotificationDto, store: ShownNotificationStore) {
        val id = dto.id ?: return
        if (store.isShown(id)) return
        show(
            context = context,
            notificationId = id,
            title = dto.title ?: "Obill",
            body = dto.body ?: "",
            type = dto.type,
            orderId = dto.orderId,
        )
        store.markShown(id)
    }

    fun show(
        context: Context,
        notificationId: Int,
        title: String,
        body: String,
        type: String? = null,
        orderId: Int? = null,
    ) {
        ensureChannel(context.applicationContext)
        val intent = Intent(context.applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            type?.let { putExtra(EXTRA_NOTIF_TYPE, it) }
            orderId?.let { putExtra(EXTRA_ORDER_ID, it) }
            putExtra(EXTRA_NOTIF_ID, notificationId)
        }
        val pending = PendingIntent.getActivity(
            context.applicationContext,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context.applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        val mgr = context.applicationContext.getSystemService(NotificationManager::class.java)
        mgr?.notify(notificationId, notification)
    }
}
