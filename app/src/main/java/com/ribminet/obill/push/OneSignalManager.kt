package com.ribminet.obill.push

import android.content.Context
import android.os.Build
import android.util.Log
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import com.onesignal.notifications.INotificationClickEvent
import com.onesignal.notifications.INotificationClickListener
import com.onesignal.notifications.INotificationLifecycleListener
import com.onesignal.notifications.INotificationWillDisplayEvent
import com.onesignal.user.subscriptions.IPushSubscriptionObserver
import com.onesignal.user.subscriptions.PushSubscriptionChangedState
import com.ribminet.obill.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Wrapper OneSignal SDK: init di Application, permission push, login/logout pelanggan.
 */
object OneSignalManager {

    private const val TAG = "ObillOneSignal"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var initialized = false
    private var onSubscriptionChanged: ((String) -> Unit)? = null
    private var onNotificationOpened: ((Map<String, String>) -> Unit)? = null
    private var onPushReceived: ((String, String, Map<String, String>) -> Unit)? = null

    private fun parseData(data: org.json.JSONObject?): Map<String, String> {
        val map = mutableMapOf<String, String>()
        if (data != null) {
            data.keys().forEach { key ->
                val value = data.opt(key)
                if (value != null) map[key] = value.toString()
            }
        }
        return map
    }

    private val clickListener = object : INotificationClickListener {
        override fun onClick(event: INotificationClickEvent) {
            onNotificationOpened?.invoke(parseData(event.notification.additionalData))
        }
    }

    private val foregroundListener = object : INotificationLifecycleListener {
        override fun onWillDisplay(event: INotificationWillDisplayEvent) {
            val n = event.notification
            val title = n.title ?: "Obill"
            val body = n.body ?: ""
            val data = parseData(n.additionalData)
            Log.d(TAG, "push foreground display title=$title")
            onPushReceived?.invoke(title, body, data)
            // Biarkan OneSignal menampilkan banner sistem (heads-up) di foreground.
        }
    }

    private val subscriptionObserver = object : IPushSubscriptionObserver {
        override fun onPushSubscriptionChange(state: PushSubscriptionChangedState) {
            val current = state.current
            val id = current.id
            val optedIn = current.optedIn
            Log.d(TAG, "subscription changed id=$id optedIn=$optedIn token=${current.token?.take(12)}…")
            if (!id.isNullOrBlank()) onSubscriptionChanged?.invoke(id)
        }
    }

    /**
     * Panggil dari [android.app.Application.onCreate] — sebelum UI.
     * Sesuai dokumentasi OneSignal: initWithContext + App ID.
     */
    fun init(context: Context, appId: String) {
        if (initialized || appId.isBlank()) return
        if (BuildConfig.DEBUG) {
            OneSignal.Debug.logLevel = LogLevel.VERBOSE
        }
        val appContext = context.applicationContext
        OneSignal.initWithContext(appContext, appId)
        OneSignal.Notifications.addClickListener(clickListener)
        OneSignal.Notifications.addForegroundLifecycleListener(foregroundListener)
        OneSignal.User.pushSubscription.addObserver(subscriptionObserver)
        NotificationHelper.ensureChannel(appContext)
        initialized = true
        Log.i(TAG, "init appId=$appId")
        logSubscriptionState("after-init")
        currentSubscriptionId()?.let { onSubscriptionChanged?.invoke(it) }
    }

    /** Minta izin notifikasi sistem + opt-in push OneSignal (wajib agar muncul di dashboard). */
    fun requestPushPermission(fallbackToSettings: Boolean = true) {
        if (!initialized) return
        scope.launch {
            try {
                val accepted = OneSignal.Notifications.requestPermission(fallbackToSettings)
                Log.i(TAG, "requestPermission accepted=$accepted")
                logSubscriptionState("after-permission")
                currentSubscriptionId()?.let { onSubscriptionChanged?.invoke(it) }
            } catch (e: Exception) {
                Log.e(TAG, "requestPermission failed", e)
            }
        }
    }

    fun setOnSubscriptionChanged(listener: ((String) -> Unit)?) {
        onSubscriptionChanged = listener
        currentSubscriptionId()?.let { listener?.invoke(it) }
    }

    fun setOnNotificationOpened(listener: ((Map<String, String>) -> Unit)?) {
        onNotificationOpened = listener
    }

    /** Dipanggil saat push diterima (foreground) — untuk popup in-app. */
    fun setOnPushReceived(listener: ((String, String, Map<String, String>) -> Unit)?) {
        onPushReceived = listener
    }

    fun login(customerId: Int) {
        if (customerId <= 0 || !initialized) return
        OneSignal.login(customerId.toString())
        Log.i(TAG, "login customerId=$customerId")
        logSubscriptionState("after-login")
        currentSubscriptionId()?.let { onSubscriptionChanged?.invoke(it) }
    }

    fun logout() {
        if (!initialized) return
        OneSignal.logout()
        Log.i(TAG, "logout")
    }

    fun currentSubscriptionId(): String? =
        if (!initialized) null
        else OneSignal.User.pushSubscription.id?.takeIf { it.isNotBlank() }

    fun isPushOptedIn(): Boolean =
        initialized && OneSignal.User.pushSubscription.optedIn

    fun deviceName(): String = Build.MODEL.takeIf { it.isNotBlank() } ?: "Android"

    private fun logSubscriptionState(label: String) {
        if (!initialized) return
        val sub = OneSignal.User.pushSubscription
        Log.d(
            TAG,
            "$label subscriptionId=${sub.id} optedIn=${sub.optedIn} token=${sub.token?.take(16)}…"
        )
    }
}
