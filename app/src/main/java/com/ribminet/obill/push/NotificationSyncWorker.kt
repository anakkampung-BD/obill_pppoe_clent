package com.ribminet.obill.push

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ribminet.obill.ObillApp
import com.ribminet.obill.data.remote.ApiResult

/**
 * Sync notifikasi feed di background (meski app tidak dibuka).
 * Menampilkan popup sistem bila ada notifikasi baru dari server.
 */
class NotificationSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repo = runCatching { ObillApp.instance.repository }.getOrNull() ?: return Result.success()
        if (!repo.isLoggedIn) return Result.success()

        val store = ShownNotificationStore(applicationContext)
        val sinceId = repo.tokenStore.notificationSinceId
        var synced = false

        when (val poll = repo.notificationsPoll(sinceId, timeout = 0)) {
            is ApiResult.Ok -> {
                val d = poll.data
                d.latestId?.let { latest ->
                    if (latest > repo.tokenStore.notificationSinceId) {
                        repo.tokenStore.notificationSinceId = latest
                    }
                }
                if (d.hasNew == true && d.notifications.isNotEmpty()) {
                    d.notifications.forEach { NotificationHelper.showFromDto(applicationContext, it, store) }
                    synced = true
                }
            }
            is ApiResult.Err -> Unit
        }

        if (!synced) {
            when (val unread = repo.unreadCount()) {
                is ApiResult.Ok -> {
                    if ((unread.data.unreadCount ?: 0) > 0) {
                        when (val list = repo.notifications(sinceId = sinceId, limit = 10)) {
                            is ApiResult.Ok -> {
                                list.data.notifications.forEach {
                                    NotificationHelper.showFromDto(applicationContext, it, store)
                                }
                                list.data.latestId?.let { latest ->
                                    if (latest > repo.tokenStore.notificationSinceId) {
                                        repo.tokenStore.notificationSinceId = latest
                                    }
                                }
                            }
                            is ApiResult.Err -> Unit
                        }
                    }
                }
                is ApiResult.Err -> Unit
            }
        }

        return Result.success()
    }
}
