package com.ribminet.obill

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ribminet.obill.data.Bill
import com.ribminet.obill.data.Complaint
import com.ribminet.obill.data.DummyData
import com.ribminet.obill.data.InternetPackage
import com.ribminet.obill.data.PaymentMethodOption
import com.ribminet.obill.data.UserProfile
import com.ribminet.obill.data.local.ComplaintStore
import com.ribminet.obill.data.remote.ApiResult
import com.ribminet.obill.data.remote.ActivationDto
import com.ribminet.obill.data.remote.BillDto
import com.ribminet.obill.data.remote.DeviceClientDto
import com.ribminet.obill.data.remote.DeviceDto
import com.ribminet.obill.data.remote.NotificationDto
import com.ribminet.obill.data.remote.OrderDto
import com.ribminet.obill.data.remote.PayChannelDto
import com.ribminet.obill.data.remote.activationFromFlat
import com.ribminet.obill.data.remote.activationResolved
import com.ribminet.obill.data.remote.formatDateId
import com.ribminet.obill.data.remote.mergeActivationInfo
import com.ribminet.obill.data.remote.resolvePaymentBill
import com.ribminet.obill.data.remote.resolvedBill
import com.ribminet.obill.data.remote.PendingChangeDto
import com.ribminet.obill.data.remote.ReleaseInfo
import com.ribminet.obill.data.remote.UpdateChecker
import com.ribminet.obill.data.remote.UpdateConfig
import com.ribminet.obill.data.remote.VersionUtil
import com.ribminet.obill.data.remote.RxPointDto
import com.ribminet.obill.data.remote.RxSummaryDto
import com.ribminet.obill.data.remote.toBill
import com.ribminet.obill.data.remote.toInternetPackage
import com.ribminet.obill.data.remote.toPaymentMethodOption
import com.ribminet.obill.data.remote.toUserProfile
import com.ribminet.obill.push.NotificationHelper
import com.ribminet.obill.push.NotificationSyncScheduler
import com.ribminet.obill.push.OneSignalManager
import com.ribminet.obill.push.ShownNotificationStore
import com.ribminet.obill.ui.navigation.Routes
import com.ribminet.obill.util.ApkUpdater
import com.ribminet.obill.util.ImageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class OtpStep { PHONE, OTP }
enum class OrderFlow { BILL, UPGRADE }

class AppViewModel : ViewModel() {

    private val repo = ObillApp.instance.repository

    // ---- Auth ----
    var loggedIn by mutableStateOf(repo.isLoggedIn)
        private set

    var otpStep by mutableStateOf(OtpStep.PHONE)
    var phone by mutableStateOf(repo.tokenStore.phone ?: "")
    var otpTtlSeconds by mutableStateOf(300)
    var otpResendSeconds by mutableStateOf(60)
    var otpLength by mutableStateOf(6)
    var authLoading by mutableStateOf(false)
        private set

    // Notifikasi gaya SweetAlert (login & lainnya)
    var alert by mutableStateOf<AppAlert?>(null)
    private var alertOnDismiss: (() -> Unit)? = null

    fun dismissAlert() {
        val cb = alertOnDismiss
        alertOnDismiss = null
        alert = null
        cb?.invoke()
    }

    // ---- Auto-deteksi versi rilis terbaru ----
    var updateInfo by mutableStateOf<ReleaseInfo?>(null)
        private set
    var updateDownloading by mutableStateOf(false)
        private set
    var updateProgress by mutableStateOf(0f)
        private set
    private var updateChecked = false

    /** Cek rilis terbaru sekali per sesi. Mengisi [updateInfo] bila ada versi lebih baru. */
    fun checkForUpdate() {
        if (updateChecked || !UpdateConfig.isConfigured) return
        updateChecked = true
        viewModelScope.launch {
            val latest = UpdateChecker.fetchLatest() ?: return@launch
            val current = ObillApp.instance.appVersionName()
            if (VersionUtil.isNewer(latest.versionName, current)) {
                updateInfo = latest
            }
        }
    }

    /**
     * Unduh APK rilis terbaru lalu picu installer.
     * Bila APK tidak tersedia/gagal, [onFallback] dipanggil dengan URL halaman rilis.
     */
    fun downloadAndInstallUpdate(onFallback: (String) -> Unit) {
        val info = updateInfo ?: return
        if (updateDownloading) return
        val url = info.downloadUrl
        if (url.isBlank() || !url.endsWith(".apk", ignoreCase = true)) {
            onFallback(info.pageUrl.ifBlank { url })
            return
        }
        updateDownloading = true
        updateProgress = 0f
        viewModelScope.launch {
            val file = ApkUpdater.download(ObillApp.instance, url) { p ->
                updateProgress = if (p < 0f) updateProgress else p
            }
            updateDownloading = false
            if (file != null) {
                updateInfo = null
                ApkUpdater.installApk(ObillApp.instance, file)
            } else {
                alert = AppAlert(
                    AlertType.ERROR,
                    "Unduhan Gagal",
                    "Tidak dapat mengunduh pembaruan. Anda akan diarahkan ke halaman rilis."
                )
                onFallback(info.pageUrl.ifBlank { url })
            }
        }
    }

    fun dismissUpdate() { updateInfo = null }

    // ---- Profil & langganan ----
    var profile by mutableStateOf<UserProfile?>(null)
        private set
    var meLoading by mutableStateOf(false)
        private set
    var meError by mutableStateOf<String?>(null)

    // ---- Riwayat pembayaran ----
    val payments = mutableStateListOf<Bill>()
    var paymentsLoading by mutableStateOf(false)
        private set
    var nextDueDate by mutableStateOf("-")
        private set

    // ---- Tagihan berjalan ----
    var billDto by mutableStateOf<BillDto?>(null)
        private set
    var customerActivation by mutableStateOf<com.ribminet.obill.data.remote.ActivationDto?>(null)
        private set
    var billOpenOrder by mutableStateOf<OrderDto?>(null)
        private set
    var billLoading by mutableStateOf(false)
        private set
    var billError by mutableStateOf<String?>(null)

    // ---- Upgrade paket ----
    val packages = mutableStateListOf<InternetPackage>()
    var upgradeLoading by mutableStateOf(false)
        private set
    var upgradeError by mutableStateOf<String?>(null)
    var pendingChange by mutableStateOf<PendingChangeDto?>(null)
        private set
    var packagesSource by mutableStateOf<String?>(null)
        private set
    var packageChangeSubmitting by mutableStateOf(false)
        private set

    // ---- Metode pembayaran ----
    private val channels = mutableStateListOf<PayChannelDto>()
    val methods = mutableStateListOf<PaymentMethodOption>()
    var methodsLoading by mutableStateOf(false)
        private set

    // ---- Flow pesanan ----
    var orderFlow by mutableStateOf(OrderFlow.BILL)
    var selectedUpgradeProfileId by mutableStateOf<Int?>(null)
    var selectedUpgradeName by mutableStateOf("")
    var currentOrder by mutableStateOf<OrderDto?>(null)
    var orderSubmitting by mutableStateOf(false)
        private set
    var orderError by mutableStateOf<String?>(null)
    var orderStatusRefreshing by mutableStateOf(false)
        private set

    // ---- Riwayat pesanan ----
    val orders = mutableStateListOf<OrderDto>()
    var ordersLoading by mutableStateOf(false)
        private set

    // ---- Perangkat (Genie ACS) ----
    var device by mutableStateOf<DeviceDto?>(null)
        private set
    var deviceFound by mutableStateOf(true)
        private set
    var deviceLoading by mutableStateOf(false)
        private set
    var deviceError by mutableStateOf<String?>(null)

    val deviceClients = mutableStateListOf<DeviceClientDto>()
    var deviceClientsLoading by mutableStateOf(false)
        private set
    var deviceWifiCount by mutableStateOf(0)
        private set
    var deviceLanCount by mutableStateOf(0)
        private set

    val rxHistory = mutableStateListOf<RxPointDto>()
    var rxSummary by mutableStateOf<RxSummaryDto?>(null)
        private set
    var rxHistoryLoading by mutableStateOf(false)
        private set

    var deviceActionRunning by mutableStateOf(false)
        private set

    // Perangkat terhubung yang hanya berada di segmen IP 192.168.1.0/24
    val lanClients: List<DeviceClientDto>
        get() = deviceClients.filter { it.ipAddress?.trim()?.startsWith("192.168.1.") == true }

    // ---- Pengaduan (riwayat disimpan lokal di perangkat) ----
    private val complaintStore = ComplaintStore(ObillApp.instance)
    val complaints = mutableStateListOf<Complaint>()

    private fun loadLocalComplaints() {
        complaints.clear()
        complaints.addAll(complaintStore.load(repo.tokenStore.phone))
    }

    private fun persistComplaints() {
        complaintStore.save(repo.tokenStore.phone, complaints.toList())
    }

    var docTitle by mutableStateOf("Syarat & Ketentuan")

    // Banner "Status Tagihan Kamu" hanya tampil sekali per sesi (sampai ditutup pengguna).
    var billingBannerVisible by mutableStateOf(true)
        private set
    fun dismissBillingBanner() { billingBannerVisible = false }

    // Notifikasi internal masa aktif: tampil sekali per sesi saat sisa 1 hari / jatuh tempo.
    var billingReminder by mutableStateOf<AppAlert?>(null)
        private set
    private var billingReminderShown = false
    fun dismissBillingReminder() { billingReminder = null }

    // ---- Notifikasi (feed + OneSignal) ----
    val notifications = mutableStateListOf<NotificationDto>()
    var notificationsLoading by mutableStateOf(false)
        private set
    var unreadCount by mutableStateOf(0)
        private set
    var pendingPushRoute by mutableStateOf<String?>(null)
        private set
    /** Popup in-app (SweetAlert) untuk notifikasi baru saat app dibuka. */
    var notificationPopup by mutableStateOf<AppAlert?>(null)
        private set
    private var notificationPopupRoute: String? = null
    private var pendingPopupNotificationId: Int? = null
    private val shownNotificationStore = ShownNotificationStore(ObillApp.instance)
    private var notificationSinceId = repo.tokenStore.notificationSinceId
    private var notificationPollJob: Job? = null
    private var lastRegisteredSubscriptionId: String? = null
    private var pushLinked = false

    fun consumePushRoute(): String? {
        val r = pendingPushRoute
        pendingPushRoute = null
        return r
    }

    fun dismissNotificationPopup() {
        notificationPopup = null
        notificationPopupRoute = null
        pendingPopupNotificationId = null
    }

    /** Tap "Lihat" pada popup notifikasi → navigasi ke layar terkait. */
    fun confirmNotificationPopup() {
        val route = notificationPopupRoute
        val notifId = pendingPopupNotificationId
        notificationPopup = null
        notificationPopupRoute = null
        pendingPopupNotificationId = null
        notifId?.let { id ->
            shownNotificationStore.markShown(id)
            markNotificationRead(id)
        }
        route?.let { pendingPushRoute = it }
    }

    /** Navigasi dari tap notifikasi sistem (cold start / background). */
    fun handleNotificationIntent(type: String?, orderId: Int?) {
        if (type.isNullOrBlank()) return
        pendingPushRoute = routeForNotificationType(type, orderId)
        refreshUnreadCount()
    }

    /** Pasang callback OneSignal (init sudah di [ObillApp.onCreate]). */
    fun setupOneSignal() {
        setupOneSignalCallbacks()
    }

    private fun setupOneSignalCallbacks() {
        OneSignalManager.setOnSubscriptionChanged { subId ->
            if (loggedIn) registerPushSubscription(subId)
        }
        OneSignalManager.setOnNotificationOpened { data -> handlePushOpened(data) }
        OneSignalManager.setOnPushReceived { title, body, data ->
            offerInAppNotificationPopup(
                title = title,
                body = body,
                type = data["type"],
                orderId = data["order_id"]?.toIntOrNull(),
                notificationId = data["notification_id"]?.toIntOrNull()
                    ?: data["id"]?.toIntOrNull(),
            )
            refreshUnreadCount()
        }
    }

    /** Hubungkan pelanggan ke OneSignal + daftar subscription ke server. */
    fun linkPushForCustomer(customerId: Int) {
        if (customerId <= 0) return
        repo.tokenStore.customerId = customerId
        OneSignalManager.login(customerId)
        pushLinked = true
        OneSignalManager.currentSubscriptionId()?.let { registerPushSubscription(it) }
        refreshUnreadCount()
        startNotificationPoll()
        NotificationSyncScheduler.schedule(ObillApp.instance)
    }

    private fun registerPushSubscription(subscriptionId: String) {
        if (!loggedIn || subscriptionId.isBlank()) return
        if (subscriptionId == lastRegisteredSubscriptionId) return
        viewModelScope.launch {
            when (val r = repo.registerOneSignal(subscriptionId, OneSignalManager.deviceName())) {
                is ApiResult.Ok -> if (r.data.success) lastRegisteredSubscriptionId = subscriptionId
                is ApiResult.Err -> Unit
            }
        }
    }

    private fun handlePushOpened(data: Map<String, String>) {
        pendingPushRoute = routeForNotificationType(data["type"], data["order_id"]?.toIntOrNull())
        refreshUnreadCount()
    }

    private fun offerInAppNotificationPopup(
        title: String,
        body: String,
        type: String?,
        orderId: Int?,
        notificationId: Int?,
    ) {
        if (notificationPopup != null) return
        if (notificationId != null && shownNotificationStore.isShown(notificationId)) return
        notificationPopup = alertForNotification(title, body, type)
        notificationPopupRoute = routeForNotificationType(type, orderId)
        pendingPopupNotificationId = notificationId
    }

    private fun showNotificationPopup(title: String, body: String, type: String?, orderId: Int?) {
        offerInAppNotificationPopup(title, body, type, orderId, notificationId = null)
    }

    private fun alertForNotification(title: String, body: String, type: String?): AppAlert {
        val alertType = when (type) {
            "payment_verified" -> AlertType.SUCCESS
            "payment_rejected", "expiry_reminder_24h" -> AlertType.ERROR
            "billing_reminder" -> AlertType.WARNING
            else -> AlertType.INFO
        }
        return AppAlert(alertType, title, body, confirmText = "Lihat")
    }

    private fun alertForNotification(dto: NotificationDto): AppAlert =
        alertForNotification(dto.title ?: "Notifikasi", dto.body ?: "", dto.type)

    private fun routeForNotificationType(type: String?, orderId: Int?): String = when (type) {
        "billing_reminder", "expiry_reminder_24h" -> Routes.OUTSTANDING
        "payment_verified" -> Routes.HISTORY
        "payment_rejected" -> {
            orderId?.let { id ->
                viewModelScope.launch {
                    when (val r = repo.order(id)) {
                        is ApiResult.Ok -> r.data.order?.let { currentOrder = it }
                        is ApiResult.Err -> Unit
                    }
                }
            }
            if (orderId != null) Routes.PAYMENT_INSTRUCTION else Routes.ORDERS
        }
        else -> Routes.NOTIFICATIONS
    }

    fun startNotificationPoll() {
        if (!loggedIn) return
        notificationPollJob?.cancel()
        notificationPollJob = viewModelScope.launch {
            while (isActive && loggedIn) {
                when (val r = repo.notificationsPoll(notificationSinceId, timeout = 25)) {
                    is ApiResult.Ok -> {
                        val d = r.data
                        d.latestId?.let { latest ->
                            if (latest > notificationSinceId) {
                                notificationSinceId = latest
                                repo.tokenStore.notificationSinceId = latest
                            }
                        }
                        d.unreadCount?.let { unreadCount = it }
                        if (d.hasNew == true && d.notifications.isNotEmpty()) {
                            processNewNotifications(d.notifications, showInAppPopup = true)
                        }
                    }
                    is ApiResult.Err -> delay(5_000)
                }
            }
        }
    }

    fun stopNotificationPoll() {
        notificationPollJob?.cancel()
        notificationPollJob = null
    }

    private fun processNewNotifications(incoming: List<NotificationDto>, showInAppPopup: Boolean) {
        val existing = notifications.mapNotNull { it.id }.toSet()
        val newOnes = incoming
            .filter { it.id != null && it.id !in existing }
            .sortedByDescending { it.id }
        if (newOnes.isEmpty()) return

        newOnes.forEach { dto ->
            notifications.add(0, dto)
            dto.id?.let { id ->
                if (!shownNotificationStore.isShown(id)) {
                    NotificationHelper.showFromDto(ObillApp.instance, dto, shownNotificationStore)
                }
            }
        }

        if (showInAppPopup) {
            val first = newOnes.first()
            val id = first.id
            if (notificationPopup != null) return
            if (id != null && shownNotificationStore.isShown(id)) return
            notificationPopup = alertForNotification(first)
            notificationPopupRoute = routeForNotificationType(first.type, first.orderId)
            pendingPopupNotificationId = id
        }
    }

    private fun mergeNotifications(incoming: List<NotificationDto>) {
        processNewNotifications(incoming, showInAppPopup = true)
    }

    fun loadNotifications() {
        notificationsLoading = true
        viewModelScope.launch {
            when (val r = repo.notifications(limit = 50)) {
                is ApiResult.Ok -> {
                    notifications.clear()
                    notifications.addAll(r.data.notifications)
                    r.data.unreadCount?.let { unreadCount = it }
                    r.data.latestId?.let { latest ->
                        if (latest > notificationSinceId) {
                            notificationSinceId = latest
                            repo.tokenStore.notificationSinceId = latest
                        }
                    }
                }
                is ApiResult.Err -> Unit
            }
            notificationsLoading = false
        }
    }

    fun refreshUnreadCount() {
        if (!loggedIn) return
        viewModelScope.launch {
            when (val r = repo.unreadCount()) {
                is ApiResult.Ok -> {
                    r.data.unreadCount?.let { unreadCount = it }
                    r.data.latestId?.let { latest ->
                        if (latest > notificationSinceId) {
                            notificationSinceId = latest
                            repo.tokenStore.notificationSinceId = latest
                        }
                    }
                }
                is ApiResult.Err -> Unit
            }
        }
    }

    fun markNotificationRead(id: Int) {
        viewModelScope.launch {
            when (repo.notificationsRead(id = id)) {
                is ApiResult.Ok -> {
                    val idx = notifications.indexOfFirst { it.id == id }
                    if (idx >= 0) {
                        val n = notifications[idx]
                        notifications[idx] = n.copy(isRead = true, readAt = n.readAt ?: "now")
                    }
                    refreshUnreadCount()
                }
                is ApiResult.Err -> Unit
            }
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            when (repo.notificationsRead(all = true)) {
                is ApiResult.Ok -> {
                    for (i in notifications.indices) {
                        notifications[i] = notifications[i].copy(isRead = true)
                    }
                    unreadCount = 0
                }
                is ApiResult.Err -> Unit
            }
        }
    }

    private fun clearNotificationState() {
        stopNotificationPoll()
        NotificationSyncScheduler.cancel(ObillApp.instance)
        shownNotificationStore.clear()
        notifications.clear()
        unreadCount = 0
        notificationSinceId = 0
        lastRegisteredSubscriptionId = null
        pushLinked = false
        pendingPushRoute = null
        notificationPopup = null
        notificationPopupRoute = null
        pendingPopupNotificationId = null
    }

    private fun evaluateBillingReminder() {
        if (billingReminderShown) return
        val np = billDto?.nextPayment ?: return
        val due = billDto?.nextPayment?.dueDate
        when {
            np.isOverdue == true -> {
                billingReminderShown = true
                billingReminder = AppAlert(
                    AlertType.ERROR,
                    "Tagihan Jatuh Tempo",
                    "Masa aktif layanan Anda telah berakhir" +
                        (due?.let { " (jatuh tempo ${formatDateId(it)})" } ?: "") +
                        ". Segera lakukan pembayaran agar layanan tetap aktif.",
                )
            }
            np.daysUntilDue == 1 -> {
                billingReminderShown = true
                billingReminder = AppAlert(
                    AlertType.WARNING,
                    "Masa Aktif Hampir Habis",
                    "Masa aktif layanan Anda tersisa 1 hari lagi" +
                        (due?.let { " (jatuh tempo ${formatDateId(it)})" } ?: "") +
                        ". Lakukan pembayaran sekarang untuk menghindari isolir.",
                )
            }
        }
    }

    init {
        loadLocalComplaints()
        setupOneSignal()
        if (loggedIn) {
            loadInitial()
            val cid = repo.tokenStore.customerId
            if (cid > 0) linkPushForCustomer(cid)
        }
    }

    // ---------------- Auth ----------------
    fun requestOtp() {
        val p = phone.trim()
        if (p.length < 8) {
            alert = AppAlert(AlertType.WARNING, "Nomor Tidak Valid", "Masukkan nomor WhatsApp yang benar (contoh: 08xxxxxxxxxx).")
            return
        }
        alert = null
        authLoading = true
        viewModelScope.launch {
            when (val r = repo.requestOtp(p)) {
                is ApiResult.Ok -> {
                    if (r.data.success && r.data.registered != false) {
                        otpTtlSeconds = r.data.otpTtlSeconds ?: 300
                        otpResendSeconds = r.data.resendAfterSeconds ?: 60
                        otpStep = OtpStep.OTP
                    } else {
                        // Nomor tidak terdaftar: tetap di halaman login, jangan lanjut ke OTP
                        alert = AppAlert(
                            AlertType.ERROR,
                            "Nomor Tidak Terdaftar",
                            r.data.message ?: "Nomor WhatsApp tidak terdaftar sebagai pelanggan. Silakan hubungi admin."
                        )
                    }
                }
                is ApiResult.Err -> alert = errToAlert(r)
            }
            authLoading = false
        }
    }

    fun verifyOtp(otp: String, onSuccess: () -> Unit) {
        alert = null
        authLoading = true
        viewModelScope.launch {
            when (val r = repo.verifyOtp(phone.trim(), otp.trim())) {
                is ApiResult.Ok -> {
                    if (r.data.success && !r.data.token.isNullOrBlank()) {
                        r.data.customer?.let {
                            profile = it.toUserProfile()
                            it.id?.let { id -> linkPushForCustomer(id) }
                        }
                        loggedIn = true
                        loadInitial()
                        onSuccess()
                    } else {
                        alert = AppAlert(AlertType.ERROR, "Verifikasi Gagal", r.data.message ?: "Kode OTP salah.")
                    }
                }
                is ApiResult.Err -> alert = errToAlert(r)
            }
            authLoading = false
        }
    }

    private fun errToAlert(e: ApiResult.Err): AppAlert {
        val title = when (e.code) {
            "CUSTOMER_NOT_FOUND" -> "Nomor Tidak Terdaftar"
            "OTP_RATE_LIMITED" -> "Terlalu Sering"
            "WA_SEND_FAILED", "WA_UNAVAILABLE" -> "Gagal Kirim OTP"
            "OTP_MISMATCH" -> "Kode OTP Salah"
            "OTP_INVALID" -> "OTP Kedaluwarsa"
            "OTP_LOCKED" -> "OTP Terkunci"
            "INVALID_PHONE" -> "Nomor Tidak Valid"
            "NETWORK" -> "Koneksi Bermasalah"
            else -> "Gagal"
        }
        var msg = e.message
        e.remainingAttempts?.let { msg += " Sisa percobaan: $it." }
        e.retryAfterSeconds?.let { msg += " Coba lagi dalam $it detik." }
        val type = when (e.code) {
            "CUSTOMER_NOT_FOUND", "OTP_MISMATCH", "OTP_INVALID", "OTP_LOCKED", "WA_SEND_FAILED", "WA_UNAVAILABLE" -> AlertType.ERROR
            else -> AlertType.WARNING
        }
        return AppAlert(type, title, msg)
    }

    fun resetAuth() {
        otpStep = OtpStep.PHONE
        alert = null
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            val subId = OneSignalManager.currentSubscriptionId()
            if (!subId.isNullOrBlank()) {
                repo.unregisterOneSignal(subId)
            }
            repo.logout(subId)
            OneSignalManager.logout()
            clearNotificationState()
            loggedIn = false
            profile = null
            payments.clear()
            packages.clear()
            channels.clear()
            methods.clear()
            currentOrder = null
            device = null
            deviceClients.clear()
            rxHistory.clear()
            rxSummary = null
            complaints.clear()
            orders.clear()
            currentOrder = null
            packages.clear()
            pendingChange = null
            packagesSource = null
            billingBannerVisible = true
            billDto = null
            customerActivation = null
            billingReminder = null
            billingReminderShown = false
            otpStep = OtpStep.PHONE
            onDone()
        }
    }

    // ---------------- Loaders ----------------
    private fun loadInitial() {
        loadLocalComplaints()
        loadMe()
        loadBill()
        loadPayments()
        loadDevice()
        loadNotifications()
        refreshUnreadCount()
        repo.tokenStore.customerId.takeIf { it > 0 }?.let { if (!pushLinked) linkPushForCustomer(it) }
    }

    fun loadMe() {
        meError = null
        meLoading = true
        viewModelScope.launch {
            when (val r = repo.me()) {
                is ApiResult.Ok -> {
                    customerActivation = r.data.activation
                        ?: r.data.customer?.activationFromFlat()
                    r.data.customer?.let {
                        profile = it.toUserProfile()
                        it.id?.let { id ->
                            repo.tokenStore.customerId = id
                            if (loggedIn && !pushLinked) linkPushForCustomer(id)
                        }
                    }
                    billDto = billDto?.let { resolvePaymentBill(it, customerActivation = customerActivation) }
                }
                is ApiResult.Err -> meError = r.message
            }
            meLoading = false
        }
    }

    fun loadPayments() {
        paymentsLoading = true
        viewModelScope.launch {
            when (val r = repo.payments()) {
                is ApiResult.Ok -> {
                    payments.clear()
                    payments.addAll(r.data.payments.map { it.toBill() })
                    r.data.nextPayment?.dueDate?.let { nextDueDate = it }
                }
                is ApiResult.Err -> { /* biarkan kosong */ }
            }
            paymentsLoading = false
        }
    }

    fun loadBill() = refreshBilling()

    /** Muat tagihan + activation_info (total bayar dari amounts.total_amount, bukan bill.amount). */
    fun refreshBilling() {
        billError = null
        billLoading = true
        viewModelScope.launch {
            val billResult = repo.bill()
            val activationResult = repo.activationInfo()
            when (billResult) {
                is ApiResult.Ok -> {
                    var resolved = billResult.data.resolvedBill(customerActivation)
                    if (activationResult is ApiResult.Ok && activationResult.data.success) {
                        customerActivation = customerActivation ?: activationResult.data.activationResolved()
                        resolved = mergeActivationInfo(resolved, activationResult.data)
                    }
                    billDto = resolved?.let { resolvePaymentBill(it, customerActivation = customerActivation) }
                    billOpenOrder = billResult.data.openOrder
                    evaluateBillingReminder()
                }
                is ApiResult.Err -> {
                    if (activationResult is ApiResult.Ok && activationResult.data.success) {
                        customerActivation = customerActivation ?: activationResult.data.activationResolved()
                        billDto = mergeActivationInfo(billDto, activationResult.data)
                    }
                    billError = billResult.message
                }
            }
            billLoading = false
        }
    }

    fun loadUpgradeOptions() {
        upgradeError = null
        upgradeLoading = true
        viewModelScope.launch {
            when (val r = repo.packageOptions()) {
                is ApiResult.Ok -> {
                    packages.clear()
                    packages.addAll(r.data.packages.map { it.toInternetPackage() })
                    pendingChange = r.data.pendingChange?.takeIf { it.pending == true }
                    packagesSource = r.data.packagesSource
                }
                is ApiResult.Err -> upgradeError = r.message
            }
            upgradeLoading = false
        }
    }

    /** Ajukan ubah/upgrade paket. Berlaku periode berikutnya setelah tagihan diverifikasi. */
    fun requestPackageChange(toProfileId: Int, onSuccess: () -> Unit) {
        if (packageChangeSubmitting) return
        packageChangeSubmitting = true
        viewModelScope.launch {
            when (val r = repo.packageChangeRequest(toProfileId)) {
                is ApiResult.Ok -> {
                    if (r.data.success) {
                        pendingChange = r.data.pendingChange?.takeIf { it.pending == true }
                        loadBill()
                        alert = AppAlert(
                            AlertType.SUCCESS,
                            "Permohonan Diterima",
                            r.data.message
                                ?: "Permohonan perubahan paket diterima. Paket baru akan berjalan pada periode berikutnya setelah pembayaran tagihan diverifikasi."
                        )
                        onSuccess()
                    } else {
                        alert = AppAlert(AlertType.ERROR, "Gagal Mengajukan", r.data.message ?: "Permohonan gagal diproses.")
                    }
                }
                is ApiResult.Err -> alert = AppAlert(AlertType.ERROR, "Gagal Mengajukan", r.message)
            }
            packageChangeSubmitting = false
        }
    }

    fun cancelPackageChange(onSuccess: () -> Unit) {
        if (packageChangeSubmitting) return
        packageChangeSubmitting = true
        viewModelScope.launch {
            when (val r = repo.packageChangeCancel()) {
                is ApiResult.Ok -> {
                    if (r.data.success) {
                        pendingChange = null
                        loadBill()
                        alert = AppAlert(AlertType.SUCCESS, "Permohonan Dibatalkan", r.data.message ?: "Permohonan perubahan paket dibatalkan.")
                        onSuccess()
                    } else {
                        alert = AppAlert(AlertType.ERROR, "Gagal Membatalkan", r.data.message ?: "Tidak ada permohonan aktif.")
                    }
                }
                is ApiResult.Err -> alert = AppAlert(AlertType.ERROR, "Gagal Membatalkan", r.message)
            }
            packageChangeSubmitting = false
        }
    }

    fun loadPaymentMethods() {
        methodsLoading = true
        viewModelScope.launch {
            when (val r = repo.paymentMethods()) {
                is ApiResult.Ok -> {
                    channels.clear(); channels.addAll(r.data.paymentMethods)
                    methods.clear(); methods.addAll(r.data.paymentMethods.map { it.toPaymentMethodOption() })
                }
                is ApiResult.Err -> { /* kosong */ }
            }
            methodsLoading = false
        }
    }

    fun channelFor(methodId: String): PayChannelDto? = channels.firstOrNull { it.method == methodId }

    // ---------------- Perangkat (Genie ACS) ----------------
    fun loadDevice() {
        deviceError = null
        deviceLoading = true
        viewModelScope.launch {
            when (val r = repo.device()) {
                is ApiResult.Ok -> {
                    deviceFound = r.data.found
                    device = r.data.device
                    if (!r.data.found) deviceError = r.data.message
                }
                is ApiResult.Err -> deviceError = r.message
            }
            deviceLoading = false
        }
    }

    fun loadDeviceClients() {
        deviceClientsLoading = true
        viewModelScope.launch {
            when (val r = repo.deviceClients()) {
                is ApiResult.Ok -> {
                    deviceClients.clear()
                    deviceClients.addAll(r.data.clients)
                    deviceWifiCount = r.data.wifiCount ?: r.data.clients.count { it.connection == "wifi" }
                    deviceLanCount = r.data.lanCount ?: r.data.clients.count { it.connection == "lan" }
                }
                is ApiResult.Err -> { /* biarkan kosong */ }
            }
            deviceClientsLoading = false
        }
    }

    fun loadRxHistory(days: Int? = 7) {
        rxHistoryLoading = true
        viewModelScope.launch {
            when (val r = repo.deviceRxHistory(days = days)) {
                is ApiResult.Ok -> {
                    rxHistory.clear()
                    rxHistory.addAll(r.data.history)
                    rxSummary = r.data.summary
                }
                is ApiResult.Err -> { /* biarkan kosong */ }
            }
            rxHistoryLoading = false
        }
    }

    fun rebootDevice() {
        if (deviceActionRunning) return
        deviceActionRunning = true
        viewModelScope.launch {
            when (val r = repo.deviceReboot()) {
                is ApiResult.Ok -> alert = AppAlert(
                    if (r.data.success) AlertType.SUCCESS else AlertType.ERROR,
                    if (r.data.queued == true) "Perintah Diantrikan" else "Reboot Dikirim",
                    r.data.message ?: "Perintah reboot dikirim ke perangkat."
                )
                is ApiResult.Err -> alert = AppAlert(AlertType.ERROR, "Gagal Reboot", r.message)
            }
            deviceActionRunning = false
        }
    }

    fun refreshDevice() {
        if (deviceActionRunning) return
        deviceActionRunning = true
        viewModelScope.launch {
            when (val r = repo.deviceRefresh()) {
                is ApiResult.Ok -> {
                    alert = AppAlert(
                        if (r.data.success) AlertType.SUCCESS else AlertType.ERROR,
                        if (r.data.queued == true) "Perintah Diantrikan" else "Permintaan Dikirim",
                        r.data.message ?: "Perangkat diminta melaporkan data terbaru."
                    )
                    if (r.data.success) loadDevice()
                }
                is ApiResult.Err -> alert = AppAlert(AlertType.ERROR, "Gagal Refresh", r.message)
            }
            deviceActionRunning = false
        }
    }

    fun updateWifi(ssid: String, password: String, onSuccess: () -> Unit) {
        val s = ssid.trim()
        val p = password.trim()
        if (s.isBlank() && p.isBlank()) {
            alert = AppAlert(AlertType.WARNING, "Belum Ada Perubahan", "Isi nama WiFi (SSID) atau password yang ingin diubah.")
            return
        }
        if (s.isNotBlank() && s.length > 32) {
            alert = AppAlert(AlertType.WARNING, "SSID Tidak Valid", "Nama WiFi maksimal 32 karakter.")
            return
        }
        if (p.isNotBlank() && (p.length < 8 || p.length > 63)) {
            alert = AppAlert(AlertType.WARNING, "Password Tidak Valid", "Password WiFi harus 8–63 karakter.")
            return
        }
        if (deviceActionRunning) return
        deviceActionRunning = true
        viewModelScope.launch {
            when (val r = repo.deviceWifi(s.ifBlank { null }, p.ifBlank { null })) {
                is ApiResult.Ok -> {
                    if (r.data.success) {
                        alert = AppAlert(
                            AlertType.SUCCESS,
                            if (r.data.queued == true) "Perubahan Diantrikan" else "Berhasil",
                            r.data.message ?: "SSID & password WiFi berhasil diperbarui."
                        )
                        loadDevice()
                        onSuccess()
                    } else {
                        alert = AppAlert(AlertType.ERROR, "Gagal", r.data.message ?: "Gagal mengubah pengaturan WiFi.")
                    }
                }
                is ApiResult.Err -> alert = AppAlert(AlertType.ERROR, "Gagal", r.message)
            }
            deviceActionRunning = false
        }
    }

    // ---------------- Order flow ----------------
    fun startBillFlow() {
        orderFlow = OrderFlow.BILL
        selectedUpgradeProfileId = null
    }

    fun startUpgradeFlow(profileId: Int, name: String) {
        orderFlow = OrderFlow.UPGRADE
        selectedUpgradeProfileId = profileId
        selectedUpgradeName = name
    }

    fun createOrder(methodId: String, note: String?, onSuccess: () -> Unit) {
        orderError = null
        orderSubmitting = true
        viewModelScope.launch {
            if (orderFlow == OrderFlow.UPGRADE) {
                val target = selectedUpgradeProfileId
                if (target == null) { orderError = "Paket tujuan belum dipilih."; orderSubmitting = false; return@launch }
                when (val r = repo.upgradeRequest(target, methodId, note)) {
                    is ApiResult.Ok -> handleCreated(r.data.order, r.data.message, onSuccess)
                    is ApiResult.Err -> handleOrderError(r)
                }
            } else {
                when (val r = repo.billPay(methodId, note)) {
                    is ApiResult.Ok -> handleCreated(r.data.order, r.data.message, onSuccess)
                    is ApiResult.Err -> handleOrderError(r)
                }
            }
            orderSubmitting = false
        }
    }

    private fun handleOrderError(e: ApiResult.Err) {
        orderError = e.message
        if (e.code == "OPEN_ORDER_EXISTS") {
            alert = AppAlert(
                AlertType.WARNING,
                "Masih Ada Pesanan",
                "Anda masih memiliki pesanan yang belum selesai. Selesaikan atau batalkan dulu pesanan tersebut."
            )
        } else {
            alert = AppAlert(AlertType.ERROR, "Gagal Membuat Pesanan", e.message)
        }
    }

    private fun handleCreated(order: OrderDto?, message: String?, onSuccess: () -> Unit) {
        if (order != null) {
            currentOrder = order
            onSuccess()
        } else {
            orderError = message ?: "Gagal membuat pesanan."
        }
    }

    fun confirmOrder(referenceNo: String?, onSuccess: () -> Unit) {
        val id = currentOrder?.id ?: return
        orderError = null
        orderSubmitting = true
        viewModelScope.launch {
            when (val r = repo.orderConfirm(id, referenceNo)) {
                is ApiResult.Ok -> {
                    currentOrder = r.data.order ?: currentOrder
                    alert = AppAlert(
                        AlertType.SUCCESS,
                        "Konfirmasi Diterima",
                        r.data.message ?: "Konfirmasi pembayaran diterima. Pesanan Anda sedang diverifikasi admin."
                    )
                    onSuccess()
                }
                is ApiResult.Err -> {
                    orderError = r.message
                    alert = AppAlert(AlertType.ERROR, "Gagal Konfirmasi", r.message)
                }
            }
            orderSubmitting = false
        }
    }

    fun cancelOrder(onSuccess: () -> Unit) {
        val id = currentOrder?.id ?: return
        orderSubmitting = true
        viewModelScope.launch {
            when (val r = repo.orderCancel(id)) {
                is ApiResult.Ok -> {
                    currentOrder = null
                    billOpenOrder = null
                    loadBill()
                    alertOnDismiss = onSuccess
                    alert = AppAlert(
                        AlertType.SUCCESS,
                        "Pesanan Dibatalkan",
                        "Pesanan pembayaran berhasil dibatalkan.",
                    )
                }
                is ApiResult.Err -> {
                    orderError = r.message
                    alertOnDismiss = null
                    alert = AppAlert(AlertType.ERROR, "Gagal Membatalkan", r.message)
                }
            }
            orderSubmitting = false
        }
    }

    /** Polling status pesanan berjalan. Bila sudah confirmed, refresh data langganan. */
    fun refreshCurrentOrder(onConfirmed: () -> Unit = {}) {
        val id = currentOrder?.id ?: return
        if (orderStatusRefreshing) return
        orderStatusRefreshing = true
        viewModelScope.launch {
            when (val r = repo.order(id)) {
                is ApiResult.Ok -> {
                    val prev = currentOrder?.status
                    r.data.order?.let { currentOrder = it }
                    if (currentOrder?.status == "confirmed" && prev != "confirmed") {
                        loadMe(); loadPayments(); loadBill()
                        onConfirmed()
                    }
                }
                is ApiResult.Err -> { /* abaikan saat polling */ }
            }
            orderStatusRefreshing = false
        }
    }

    fun loadOrders() {
        ordersLoading = true
        viewModelScope.launch {
            when (val r = repo.orders()) {
                is ApiResult.Ok -> {
                    orders.clear()
                    orders.addAll(r.data.orders)
                }
                is ApiResult.Err -> { /* biarkan kosong */ }
            }
            ordersLoading = false
        }
    }

    fun removePayment(bill: Bill) { payments.remove(bill) }

    fun addComplaint(complaint: Complaint) {
        complaints.add(0, complaint)
        persistComplaints()
    }

    fun removeComplaint(complaint: Complaint) {
        complaints.remove(complaint)
        persistComplaints()
        alert = AppAlert(AlertType.SUCCESS, "Berhasil Dihapus", "Pengaduan telah dihapus dari riwayat.")
    }

    // ---------------- Ubah biodata ----------------
    var biodataSubmitting by mutableStateOf(false)
        private set

    fun updateBiodata(
        name: String,
        email: String,
        address: String,
        photoUri: Uri? = null,
        onSuccess: () -> Unit,
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            alert = AppAlert(AlertType.WARNING, "Nama Kosong", "Nama lengkap tidak boleh kosong.")
            return
        }
        if (biodataSubmitting) return
        biodataSubmitting = true
        viewModelScope.launch {
            val photo = photoUri?.let {
                withContext(Dispatchers.IO) { ImageUtil.compress(ObillApp.instance, it) }
            }
            if (photoUri != null && photo == null) {
                alert = AppAlert(AlertType.ERROR, "Foto Gagal Diproses", "Foto tidak dapat dibaca atau ukurannya terlalu besar (maks 5 MB). Coba pilih foto lain.")
                biodataSubmitting = false
                return@launch
            }
            when (val r = repo.profileUpdate(
                name = trimmedName,
                email = email.trim(),
                address = address.trim(),
                photoBytes = photo?.bytes,
                photoMime = photo?.mime,
                photoName = photo?.name,
            )) {
                is ApiResult.Ok -> {
                    if (r.data.success) {
                        r.data.customer?.let { profile = it.toUserProfile() }
                        alert = AppAlert(AlertType.SUCCESS, "Biodata Diperbarui", r.data.message ?: "Biodata berhasil diperbarui.")
                        onSuccess()
                    } else {
                        alert = AppAlert(AlertType.ERROR, "Gagal Memperbarui", r.data.message ?: "Biodata gagal diperbarui.")
                    }
                }
                is ApiResult.Err -> alert = AppAlert(AlertType.ERROR, "Gagal Memperbarui", r.message)
            }
            biodataSubmitting = false
        }
    }

    // ---------------- Pengaduan (relay ke WhatsApp admin) ----------------
    var complaintSubmitting by mutableStateOf(false)
        private set

    fun submitComplaint(
        category: String,
        subject: String,
        message: String,
        photoUri: Uri? = null,
        onSuccess: () -> Unit,
    ) {
        val msg = message.trim()
        if (msg.isBlank()) {
            alert = AppAlert(AlertType.WARNING, "Pengaduan Kosong", "Mohon isi deskripsi pengaduan terlebih dahulu.")
            return
        }
        if (msg.length > 2000) {
            alert = AppAlert(AlertType.WARNING, "Terlalu Panjang", "Isi pengaduan maksimal 2000 karakter.")
            return
        }
        if (complaintSubmitting) return
        complaintSubmitting = true
        viewModelScope.launch {
            val photo = photoUri?.let {
                withContext(Dispatchers.IO) { ImageUtil.compress(ObillApp.instance, it) }
            }
            if (photoUri != null && photo == null) {
                alert = AppAlert(AlertType.ERROR, "Foto Gagal Diproses", "Foto tidak dapat dibaca atau ukurannya terlalu besar (maks 5 MB). Coba pilih foto lain.")
                complaintSubmitting = false
                return@launch
            }
            when (val r = repo.complaint(msg, category, subject, photo?.bytes, photo?.mime, photo?.name)) {
                is ApiResult.Ok -> {
                    if (r.data.success) {
                        val photoFailed = photo != null && r.data.photoSent == false
                        val photoMsg = r.data.photoError?.takeIf { it.isNotBlank() }
                        alert = AppAlert(
                            if (photoFailed) AlertType.WARNING else AlertType.SUCCESS,
                            "Pengaduan Terkirim",
                            when {
                                photoFailed && photoMsg != null -> "Pengaduan terkirim, namun foto gagal dikirim: $photoMsg"
                                photoFailed -> "Pengaduan terkirim, namun foto gagal dikirim. Admin tetap menerima laporan Anda."
                                else -> r.data.message
                                    ?: "Pengaduan Anda telah dikirim ke admin. Admin akan segera menghubungi Anda."
                            }
                        )
                        onSuccess()
                    } else {
                        alert = AppAlert(AlertType.ERROR, "Gagal Mengirim", r.data.message ?: "Pengaduan gagal dikirim.")
                    }
                }
                is ApiResult.Err -> alert = AppAlert(AlertType.ERROR, "Gagal Mengirim", r.message)
            }
            complaintSubmitting = false
        }
    }
}
