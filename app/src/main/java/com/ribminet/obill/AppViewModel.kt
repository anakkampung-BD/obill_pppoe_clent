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
import com.ribminet.obill.data.local.AnnouncementPrefs
import com.ribminet.obill.data.local.BillPaymentPrefs
import com.ribminet.obill.data.local.ComplaintStore
import com.ribminet.obill.data.remote.ApiResult
import com.ribminet.obill.data.remote.AnnouncementDto
import com.ribminet.obill.data.remote.ActivationDto
import com.ribminet.obill.data.remote.BillDto
import com.ribminet.obill.data.remote.DeviceClientDto
import com.ribminet.obill.data.remote.DeviceDto
import com.ribminet.obill.data.remote.NotificationDto
import com.ribminet.obill.data.remote.OrderDto
import com.ribminet.obill.data.remote.PayChannelDto
import com.ribminet.obill.data.remote.WalletPaymentDto
import com.ribminet.obill.data.remote.activationFromFlat
import com.ribminet.obill.data.remote.activationResolved
import com.ribminet.obill.data.remote.dueDisplay
import com.ribminet.obill.data.remote.formatDateId
import com.ribminet.obill.data.remote.disconnectDisplay
import com.ribminet.obill.data.remote.shouldShowDisconnectHint
import com.ribminet.obill.data.remote.enrichedFromPayResp
import com.ribminet.obill.data.remote.isQrisDinamisPayment
import com.ribminet.obill.data.remote.mergeActivationInfo
import com.ribminet.obill.data.remote.resolvePaymentBill
import com.ribminet.obill.data.remote.resolvedBill
import com.ribminet.obill.data.remote.sortedForDisplay
import com.ribminet.obill.data.remote.withMergedPayment
import com.ribminet.obill.data.remote.PendingChangeDto
import com.ribminet.obill.data.remote.ReleaseInfo
import com.ribminet.obill.data.remote.UpdateChecker
import com.ribminet.obill.data.remote.UpdateConfig
import com.ribminet.obill.data.remote.VersionUtil
import com.ribminet.obill.data.remote.RxPointDto
import com.ribminet.obill.data.remote.RxSummaryDto
import com.ribminet.obill.data.remote.UnifiedHistoryItem
import com.ribminet.obill.data.remote.toBill
import com.ribminet.obill.data.remote.toInternetPackage
import com.ribminet.obill.data.remote.toPaymentMethodOption
import com.ribminet.obill.data.remote.toUnifiedHistoryItem
import com.ribminet.obill.data.remote.toUserProfile
import com.ribminet.obill.push.NotificationHelper
import com.ribminet.obill.push.NotificationSyncScheduler
import com.ribminet.obill.push.OneSignalManager
import com.ribminet.obill.push.ShownNotificationStore
import com.ribminet.obill.ui.guide.GuideSession
import com.ribminet.obill.ui.guide.UserGuides
import com.ribminet.obill.ui.navigation.Routes
import com.ribminet.obill.util.ApkUpdater
import com.ribminet.obill.util.ImageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
    /** Sisa detik sebelum "Kirim Ulang" aktif. 0 = boleh kirim. */
    var otpResendRemaining by mutableStateOf(0)
        private set
    var otpLength by mutableStateOf(6)
    var authLoading by mutableStateOf(false)
        private set
    private var otpResendJob: Job? = null

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
     * Unduh APK rilis terbaru lalu picu installer in-app.
     * Tidak membuka browser; bila gagal tampilkan error agar user bisa coba lagi.
     */
    fun downloadAndInstallUpdate(onFallback: (String) -> Unit = {}) {
        val info = updateInfo ?: return
        if (updateDownloading) return
        if (!info.hasApkDownload) {
            alert = AppAlert(
                AlertType.ERROR,
                "Pembaruan Belum Siap",
                "File APK rilis belum tersedia. Coba lagi nanti."
            )
            return
        }
        updateDownloading = true
        updateProgress = 0f
        viewModelScope.launch {
            val file = ApkUpdater.download(ObillApp.instance, info) { p ->
                updateProgress = if (p < 0f) updateProgress else p
            }
            updateDownloading = false
            if (file != null) {
                // Jangan clear updateInfo — update wajib sampai versi baru terpasang.
                ApkUpdater.installApk(ObillApp.instance, file)
            } else {
                alert = AppAlert(
                    AlertType.ERROR,
                    "Unduhan Gagal",
                    "Tidak dapat mengunduh pembaruan. Periksa koneksi internet lalu coba lagi."
                )
                // Jangan buka browser GitHub — user minta update in-app saja.
                onFallback("")
            }
        }
    }

    /** Tidak dipakai: update bersifat wajib dan tidak dapat ditunda. */
    fun dismissUpdate() { /* no-op */ }

    // ---- Pengumuman admin (popup full + kartu beranda) ----
    private val announcementPrefs = AnnouncementPrefs(ObillApp.instance)
    private var announcementsFetchedAtMs = 0L
    private val announcementsCacheMs = 10 * 60 * 1000L
    /** Modal pengumuman (slider) sedang tampil. */
    var announcementModalVisible by mutableStateOf(false)
        private set
    /** ID item awal saat dibuka dari kartu beranda (opsional). */
    var announcementModalStartId by mutableStateOf<String?>(null)
        private set
    /** Daftar pengumuman untuk kartu/slider beranda & modal. */
    val announcements = mutableStateListOf<AnnouncementDto>()
    var announcementsLoading by mutableStateOf(false)
        private set
    /** Lock UI tombol Sudah Baca saat POST announcement_read. */
    var announcementMarkingRead by mutableStateOf(false)
        private set

    /** Buka modal dari kartu beranda — abaikan snooze; tampilkan semua item. */
    fun openAnnouncementDetail(start: AnnouncementDto? = null) {
        if (announcements.isEmpty()) return
        announcementModalStartId = start?.resolvedId()
            ?: announcements.firstOrNull { !it.markedRead() }?.resolvedId()
            ?: announcements.first().resolvedId()
        announcementModalVisible = true
    }

    /** Set daftar pengumuman; buka modal jika ada yang belum dibaca dan belum di-snooze. */
    fun setAnnouncements(items: List<AnnouncementDto>) {
        val localRead = announcementPrefs.readIds()
        val sorted = items.sortedForDisplay().map { item ->
            val id = item.resolvedId()
            if (item.markedRead() || localRead.contains(id)) {
                item.copy(isRead = true)
            } else {
                item
            }
        }
        announcements.clear()
        announcements.addAll(sorted)
        if (sorted.isEmpty()) {
            announcementModalVisible = false
            announcementModalStartId = null
            return
        }
        val hasUnread = sorted.any { !it.markedRead() }
        // Auto-popup hanya untuk unread + hormati snooze. Buka dari beranda abaikan snooze.
        if (!hasUnread) return
        if (!announcementModalVisible && !announcementPrefs.isModalSnoozed()) {
            announcementModalStartId = sorted.first { !it.markedRead() }.resolvedId()
            announcementModalVisible = true
        }
    }

    /** Close: tutup popup tanpa snooze (bisa muncul lagi nanti). */
    fun dismissAnnouncement() {
        announcementModalVisible = false
        announcementModalStartId = null
    }

    /**
     * Sudah Baca: POST view-counter ke server (idempotent), update lokal.
     * Tutup + snooze 24 jam hanya jika semua item sudah dibaca.
     */
    fun markAnnouncementRead(item: AnnouncementDto) {
        if (announcementMarkingRead) return
        if (item.markedRead()) {
            finishAnnouncementReadIfDone()
            return
        }
        val announcementId = item.id
        if (announcementId == null) {
            applyAnnouncementReadLocal(item, readCount = item.readCount)
            finishAnnouncementReadIfDone()
            return
        }
        announcementMarkingRead = true
        viewModelScope.launch {
            val readerKey = repo.tokenStore.customerId.takeIf { it > 0 }?.toString()
            val readerName = profile?.fullName?.takeIf { it.isNotBlank() }
                ?: repo.tokenStore.phone?.takeIf { it.isNotBlank() }
            when (val r = repo.announcementRead(announcementId, readerKey, readerName)) {
                is ApiResult.Ok -> {
                    val data = r.data
                    if (data.success || data.isRead == true || data.alreadyRead == true) {
                        applyAnnouncementReadLocal(
                            item,
                            readCount = data.readCount ?: item.readCount,
                        )
                        finishAnnouncementReadIfDone()
                    } else {
                        alert = AppAlert(
                            AlertType.ERROR,
                            "Gagal",
                            data.message ?: "Tidak dapat menandai pengumuman sudah dibaca.",
                        )
                    }
                }
                is ApiResult.Err -> {
                    alert = AppAlert(
                        AlertType.ERROR,
                        "Gagal",
                        r.message,
                    )
                }
            }
            announcementMarkingRead = false
        }
    }

    private fun applyAnnouncementReadLocal(item: AnnouncementDto, readCount: Int?) {
        val key = item.resolvedId()
        announcementPrefs.markRead(key)
        val idx = announcements.indexOfFirst { it.resolvedId() == key }
        if (idx >= 0) {
            announcements[idx] = announcements[idx].copy(
                isRead = true,
                readCount = readCount ?: announcements[idx].readCount,
            )
        }
    }

    /** Tutup modal + snooze jika tidak ada pengumuman belum dibaca; else fokus item berikutnya. */
    private fun finishAnnouncementReadIfDone() {
        val nextUnread = announcements.firstOrNull { !it.markedRead() }
        if (nextUnread == null) {
            announcementPrefs.snoozeModal()
            dismissAnnouncement()
        } else {
            announcementModalStartId = nextUnread.resolvedId()
        }
    }

    /**
     * Ambil pengumuman PPPoE aktif dari server.
     * Cache singkat ~10 menit; [force] mengabaikan cache (mis. pull-to-refresh).
     */
    fun refreshAnnouncements(force: Boolean = false) {
        if (!loggedIn) return
        val now = System.currentTimeMillis()
        if (!force &&
            announcements.isNotEmpty() &&
            now - announcementsFetchedAtMs < announcementsCacheMs
        ) {
            return
        }
        if (announcementsLoading) return
        announcementsLoading = true
        viewModelScope.launch {
            when (val r = repo.announcements()) {
                is ApiResult.Ok -> {
                    if (r.data.success) {
                        announcementsFetchedAtMs = System.currentTimeMillis()
                        setAnnouncements(r.data.items())
                    }
                }
                is ApiResult.Err -> Unit
            }
            announcementsLoading = false
        }
    }

    // ---- User guide (FAQ onboarding) ----
    var guideSession by mutableStateOf<GuideSession?>(null)
        private set
    /** Pesanan demo untuk highlight layar instruksi saat panduan (bukan pesanan nyata). */
    var guideDemoOrder by mutableStateOf<OrderDto?>(null)
        private set

    fun startUserGuide(guideId: String) {
        val guide = UserGuides.byId(guideId) ?: return
        dismissAnnouncement()
        guideSession = GuideSession(guide, 0)
        syncGuideDemoOrder()
        // Siapkan data layar yang akan dilalui panduan.
        if (loggedIn) {
            loadPaymentMethods()
            loadOrders()
            refreshBilling()
        }
    }

    fun nextGuideStep() {
        val current = guideSession ?: return
        if (current.isLast) {
            endUserGuide()
        } else {
            guideSession = current.copy(stepIndex = current.stepIndex + 1)
            syncGuideDemoOrder()
        }
    }

    fun skipUserGuide() {
        endUserGuide()
    }

    private fun endUserGuide() {
        guideSession = null
        guideDemoOrder = null
    }

    private fun syncGuideDemoOrder() {
        val target = guideSession?.step?.target
        guideDemoOrder = target?.let { UserGuides.demoOrderFor(it) }
    }

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
    /** Batas waktu bayar QRIS tagihan (epoch ms). */
    var billPaymentExpiresAtMs by mutableStateOf<Long?>(null)
        private set
    private val billPaymentPrefs = BillPaymentPrefs(ObillApp.instance)
    private var billPayPollJob: Job? = null

    // ---- Riwayat pesanan (PPPoE + PPOB) ----
    val orders = mutableStateListOf<OrderDto>()
    val unifiedOrders = mutableStateListOf<UnifiedHistoryItem>()
    var ordersLoading by mutableStateOf(false)
        private set
    var ordersError by mutableStateOf<String?>(null)
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
                refId = data["ref_id"],
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
        pendingPushRoute = routeForNotificationType(
            data["type"],
            data["order_id"]?.toIntOrNull(),
            data["ref_id"],
        )
        refreshUnreadCount()
    }

    private fun offerInAppNotificationPopup(
        title: String,
        body: String,
        type: String?,
        orderId: Int?,
        refId: String? = null,
        notificationId: Int?,
    ) {
        if (notificationPopup != null) return
        if (notificationId != null && shownNotificationStore.isShown(notificationId)) return
        notificationPopup = alertForNotification(title, body, type)
        notificationPopupRoute = routeForNotificationType(type, orderId, refId)
        pendingPopupNotificationId = notificationId
    }

    private fun showNotificationPopup(title: String, body: String, type: String?, orderId: Int?) {
        offerInAppNotificationPopup(title, body, type, orderId, notificationId = null)
    }

    private fun alertForNotification(title: String, body: String, type: String?): AppAlert {
        val alertType = when (type) {
            "payment_verified", "ppob_success" -> AlertType.SUCCESS
            "payment_rejected", "expiry_reminder_24h" -> AlertType.ERROR
            "billing_reminder" -> AlertType.WARNING
            else -> AlertType.INFO
        }
        return AppAlert(alertType, title, body, confirmText = "Lihat")
    }

    private fun alertForNotification(dto: NotificationDto): AppAlert =
        alertForNotification(dto.title ?: "Notifikasi", dto.body ?: "", dto.type)

    private fun notificationRefId(dto: NotificationDto): String? =
        dto.data?.get("ref_id")?.toString()?.takeIf { it.isNotBlank() }

    private fun routeForNotificationType(type: String?, orderId: Int?, refId: String? = null): String = when (type) {
        "billing_reminder", "expiry_reminder_24h" -> Routes.OUTSTANDING
        "payment_verified" -> Routes.HISTORY
        "ppob_success" -> refId?.let { Routes.ppobDetail(it) } ?: Routes.PPOB
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
            notificationPopupRoute = routeForNotificationType(first.type, first.orderId, notificationRefId(first))
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
        val bill = billDto ?: return
        val np = bill.nextPayment ?: return
        val dueLabel = bill.dueDisplay().takeIf { it != "-" }
        val disconnectLabel = bill.disconnectDisplay().takeIf { it != "-" }
        when {
            np.isOverdue == true -> {
                billingReminderShown = true
                val disconnectHint = disconnectLabel?.let {
                    " Layanan akan diputus pada $it jika belum bayar."
                }.orEmpty()
                billingReminder = AppAlert(
                    AlertType.ERROR,
                    "Tagihan Jatuh Tempo",
                    "Masa aktif layanan Anda telah berakhir" +
                        (dueLabel?.let { " ($it)" } ?: "") +
                        ".$disconnectHint Segera lakukan pembayaran agar layanan tetap aktif.",
                )
            }
            np.daysUntilDue == 1 || bill.shouldShowDisconnectHint() -> {
                billingReminderShown = true
                val disconnectHint = disconnectLabel?.let {
                    " Layanan diputus jika belum bayar: $it."
                }.orEmpty()
                billingReminder = AppAlert(
                    AlertType.WARNING,
                    "Masa Aktif Hampir Habis",
                    "Masa aktif layanan Anda hampir berakhir" +
                        (dueLabel?.let { " ($it)" } ?: "") +
                        ".$disconnectHint Lakukan pembayaran sekarang untuk menghindari isolir.",
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
                        startOtpResendCooldown()
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
                is ApiResult.Err -> {
                    // Server sering kirim WA dulu, baru balas HTTP. Timeout/502/rate-limit ≠ gagal kirim OTP.
                    val gateway = r.code == "GATEWAY" || r.httpCode in listOf(502, 503, 504)
                    when {
                        r.code == "TIMEOUT" || r.code == "NETWORK" || gateway -> {
                            otpStep = OtpStep.OTP
                            startOtpResendCooldown()
                            alert = AppAlert(
                                AlertType.WARNING,
                                when {
                                    r.code == "TIMEOUT" -> "Koneksi Lambat"
                                    gateway -> "Server Sibuk"
                                    else -> "Koneksi Terputus"
                                },
                                "Jika kode OTP sudah masuk WhatsApp, masukkan di sini. Jika belum, ketuk Kirim Ulang.",
                            )
                        }
                        r.code == "OTP_RATE_LIMITED" -> {
                            val wait = (r.retryAfterSeconds ?: otpResendSeconds).coerceAtLeast(1)
                            otpResendSeconds = wait
                            startOtpResendCooldown()
                            otpStep = OtpStep.OTP
                            alert = AppAlert(
                                AlertType.WARNING,
                                "Tunggu Sebentar",
                                "OTP baru belum bisa dikirim. Cek WhatsApp — kode sebelumnya mungkin sudah terkirim. " +
                                    "Jika belum ada, tunggu $wait detik lalu ketuk Kirim Ulang.",
                            )
                        }
                        else -> alert = errToAlert(r)
                    }
                }
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
            "TIMEOUT" -> "Koneksi Lambat"
            "GATEWAY" -> "Server Sibuk"
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
        otpResendJob?.cancel()
        otpResendRemaining = 0
    }

    /** Lanjut ke form OTP bila kode sudah diterima di WhatsApp (mis. setelah error 502). */
    fun continueToOtpEntry() {
        if (phone.trim().length < 8) {
            alert = AppAlert(AlertType.WARNING, "Nomor Tidak Valid", "Masukkan nomor WhatsApp terlebih dahulu.")
            return
        }
        alert = null
        otpStep = OtpStep.OTP
    }

    private fun startOtpResendCooldown(seconds: Int = otpResendSeconds) {
        otpResendJob?.cancel()
        val total = seconds.coerceAtLeast(0)
        otpResendRemaining = total
        if (total <= 0) return
        otpResendJob = viewModelScope.launch {
            while (otpResendRemaining > 0) {
                kotlinx.coroutines.delay(1_000)
                otpResendRemaining -= 1
            }
        }
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
            announcementModalVisible = false
            announcementModalStartId = null
            announcementMarkingRead = false
            announcements.clear()
            announcementsFetchedAtMs = 0L
            announcementPrefs.clear()
            guideSession = null
            guideDemoOrder = null
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
            unifiedOrders.clear()
            ordersError = null
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
        refreshAnnouncements()
        refreshWallet()
        repo.tokenStore.customerId.takeIf { it > 0 }?.let { if (!pushLinked) linkPushForCustomer(it) }
    }

    fun updateWalletBalance(balance: Long) {
        profile = profile?.copy(walletBalance = balance)
    }

    /** Refresh saldo wallet dari GET wallet (lebih akurat dari field me). */
    fun refreshWallet() {
        if (!loggedIn) return
        viewModelScope.launch {
            when (val r = repo.wallet()) {
                is ApiResult.Ok -> {
                    if (r.data.success) {
                        val bal = r.data.wallet?.balance
                        if (bal != null) updateWalletBalance(bal)
                    }
                }
                is ApiResult.Err -> Unit
            }
        }
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
            val payMethod = methodId.ifBlank { "qris_dinamis" }
            if (orderFlow == OrderFlow.UPGRADE) {
                val target = selectedUpgradeProfileId
                if (target == null) { orderError = "Paket tujuan belum dipilih."; orderSubmitting = false; return@launch }
                when (val r = repo.upgradeRequest(target, payMethod, note)) {
                    is ApiResult.Ok -> {
                        if (r.data.success) {
                            val order = r.data.order?.let { o ->
                                o.copy(payment = o.payment ?: r.data.payment)
                            }
                            handleCreated(order, r.data.message, onSuccess)
                        } else {
                            orderError = r.data.message ?: "Gagal membuat pesanan."
                            alert = AppAlert(AlertType.ERROR, "Gagal Membuat Pesanan", orderError ?: "")
                        }
                    }
                    is ApiResult.Err -> handleOrderError(r, onSuccess)
                }
            } else {
                when (val r = repo.billPay(payMethod, note)) {
                    is ApiResult.Ok -> {
                        if (r.data.success) {
                            val enriched = r.data.order?.enrichedFromPayResp(r.data)
                            handleCreated(enriched, r.data.message, onSuccess)
                        } else {
                            orderError = r.data.message ?: "Gagal membuat pesanan."
                            when (r.data.code) {
                                "QRIS_UNAVAILABLE" -> alert = AppAlert(
                                    AlertType.ERROR,
                                    "QRIS Tidak Tersedia",
                                    r.data.message ?: "QRIS dinamis belum dikonfigurasi. Hubungi admin.",
                                )
                                "OPEN_ORDER_EXISTS" -> resumeOpenBillOrder(onSuccess)
                                else -> alert = AppAlert(AlertType.ERROR, "Gagal Membuat Pesanan", orderError ?: "")
                            }
                        }
                    }
                    is ApiResult.Err -> handleOrderError(r, onSuccess)
                }
            }
            orderSubmitting = false
        }
    }

    private fun handleOrderError(e: ApiResult.Err, onOpenExisting: () -> Unit = {}) {
        orderError = e.message
        when (e.code) {
            "OPEN_ORDER_EXISTS" -> {
                alert = AppAlert(
                    AlertType.WARNING,
                    "Masih Ada Pesanan",
                    "Anda masih memiliki pesanan yang belum selesai. Membuka pesanan tersebut…",
                )
                resumeOpenBillOrder(onOpenExisting)
            }
            "QRIS_UNAVAILABLE" -> alert = AppAlert(
                AlertType.ERROR,
                "QRIS Tidak Tersedia",
                e.message,
            )
            else -> alert = AppAlert(AlertType.ERROR, "Gagal Membuat Pesanan", e.message)
        }
    }

    private fun resumeOpenBillOrder(onReady: () -> Unit) {
        viewModelScope.launch {
            when (val r = repo.bill()) {
                is ApiResult.Ok -> {
                    val open = r.data.openOrder
                    billOpenOrder = open
                    if (open != null) {
                        prepareBillQrisOrder(open)
                        onReady()
                    }
                }
                is ApiResult.Err -> Unit
            }
        }
    }

    private fun handleCreated(order: OrderDto?, message: String?, onSuccess: () -> Unit) {
        if (order != null) {
            prepareBillQrisOrder(order)
            if (order.status == "confirmed") {
                loadMe(); loadPayments(); loadBill()
                alert = AppAlert(
                    AlertType.SUCCESS,
                    "Pembayaran Berhasil",
                    message ?: "Tagihan lunas. Layanan Anda telah diaktifkan.",
                )
            }
            onSuccess()
        } else {
            orderError = message ?: "Gagal membuat pesanan."
        }
    }

    /** Siapkan order + cache QRIS + mulai poll bila masih pending. */
    fun prepareBillQrisOrder(order: OrderDto) {
        var prepared = order
        val orderNo = order.orderNo
        if (!orderNo.isNullOrBlank()) {
            val cachedUrl = billPaymentPrefs.qrisImageUrl(orderNo)
            val cachedPay = order.payment ?: WalletPaymentDto(
                qrisImageUrl = cachedUrl,
                qrisString = billPaymentPrefs.qrisString(orderNo),
                payAmount = billPaymentPrefs.payAmount(orderNo),
                amountBase = billPaymentPrefs.amountBase(orderNo),
                expireMinutes = billPaymentPrefs.expireMinutes(orderNo),
                merchantName = billPaymentPrefs.merchantName(orderNo),
                payChannel = "qris_dinamis",
                paymentStatus = "unpaid",
            )
            prepared = order.withMergedPayment(
                cachedPay.copy(
                    qrisImageUrl = order.payment?.qrisImageUrl ?: cachedUrl,
                    qrisString = order.payment?.qrisString ?: billPaymentPrefs.qrisString(orderNo),
                ),
            )
            val pay = prepared.payment
            val deadline = resolveBillPayDeadline(pay?.expiresAt ?: prepared.expiresAt, pay?.expireMinutes, orderNo)
            billPaymentExpiresAtMs = deadline
            if (pay != null && prepared.status == "pending") {
                billPaymentPrefs.save(
                    orderNo = orderNo,
                    qrisImageUrl = pay.qrisImageUrl,
                    qrisString = pay.qrisString,
                    payAmount = pay.payAmount,
                    amountBase = pay.amountBase,
                    expireMinutes = pay.expireMinutes,
                    expiresAtMs = deadline,
                    merchantName = pay.merchantName,
                )
            }
        } else {
            billPaymentExpiresAtMs = resolveBillPayDeadline(
                order.payment?.expiresAt ?: order.expiresAt,
                order.payment?.expireMinutes,
                "",
            )
        }
        currentOrder = prepared
        if (prepared.status == "pending" && prepared.isQrisDinamisPayment()) {
            startBillPayPoll()
        } else {
            stopBillPayPoll()
        }
    }

    fun resumeBillQrisPayment() {
        val order = currentOrder ?: billOpenOrder ?: return
        prepareBillQrisOrder(order)
    }

    private fun startBillPayPoll() {
        billPayPollJob?.cancel()
        val orderNo = currentOrder?.orderNo
        val orderId = currentOrder?.id
        if (orderNo.isNullOrBlank() && orderId == null) return
        billPayPollJob = viewModelScope.launch {
            while (isActive) {
                delay(4_000)
                when (val r = repo.billPayStatus(orderNo = orderNo, orderId = orderId)) {
                    is ApiResult.Ok -> {
                        if (!r.data.success) continue
                        val prev = currentOrder
                        var next = r.data.order ?: prev
                        if (next != null) {
                            next = next.withMergedPayment(r.data.payment ?: next.payment ?: prev?.payment)
                            // pertahankan QRIS dari cache bila poll kosong
                            val no = next.orderNo
                            if (!no.isNullOrBlank()) {
                                next = next.withMergedPayment(
                                    next.payment?.copy(
                                        qrisImageUrl = next.payment?.qrisImageUrl
                                            ?: billPaymentPrefs.qrisImageUrl(no),
                                        qrisString = next.payment?.qrisString
                                            ?: billPaymentPrefs.qrisString(no),
                                    ),
                                )
                            }
                            currentOrder = next
                        }
                        val status = r.data.paymentStatus?.lowercase()
                        val orderStatus = currentOrder?.status?.lowercase()
                        when {
                            r.data.paid == true || status == "paid" || orderStatus == "confirmed" -> {
                                billPaymentPrefs.clear(orderNo)
                                stopBillPayPoll()
                                currentOrder = currentOrder?.copy(status = "confirmed", statusLabel = "Pembayaran Berhasil")
                                loadMe(); loadPayments(); loadBill()
                                return@launch
                            }
                            status == "cancelled" || orderStatus == "cancelled" ||
                                status == "rejected" || orderStatus == "rejected" -> {
                                billPaymentPrefs.clear(orderNo)
                                stopBillPayPoll()
                                return@launch
                            }
                        }
                    }
                    is ApiResult.Err -> Unit
                }
            }
        }
    }

    private fun stopBillPayPoll() {
        billPayPollJob?.cancel()
        billPayPollJob = null
    }

    private fun resolveBillPayDeadline(expiresAt: String?, expireMinutes: Int?, orderNo: String): Long {
        if (orderNo.isNotBlank()) {
            billPaymentPrefs.expiresAtMs(orderNo)?.takeIf { it > System.currentTimeMillis() }?.let { return it }
        }
        parseBillDateTimeMs(expiresAt)?.let { return it }
        val mins = expireMinutes?.takeIf { it > 0 } ?: 30
        return System.currentTimeMillis() + mins * 60_000L
    }

    private fun parseBillDateTimeMs(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        val patterns = listOf(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
        )
        for (p in patterns) {
            try {
                val fmt = java.text.SimpleDateFormat(p, java.util.Locale.US)
                fmt.isLenient = false
                return fmt.parse(raw)?.time
            } catch (_: Exception) {
            }
        }
        return null
    }

    fun confirmOrder(referenceNo: String?, onSuccess: () -> Unit) {
        // QRIS dinamis: tidak memakai order_confirm
        if (currentOrder?.isQrisDinamisPayment() == true) {
            onSuccess()
            return
        }
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
                    stopBillPayPoll()
                    billPaymentPrefs.clear(currentOrder?.orderNo)
                    currentOrder = null
                    billOpenOrder = null
                    billPaymentExpiresAtMs = null
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
        val order = currentOrder ?: return
        if (order.isQrisDinamisPayment()) {
            // status QRIS dinamis diurus poll bill_pay_status
            viewModelScope.launch {
                orderStatusRefreshing = true
                val r = repo.billPayStatus(order.orderNo, order.id)
                if (r is ApiResult.Ok && r.data.success) {
                    var next = r.data.order ?: currentOrder
                    if (next != null) {
                        next = next.withMergedPayment(r.data.payment ?: next.payment)
                        currentOrder = next
                    }
                    if (r.data.paid == true || currentOrder?.status == "confirmed") {
                        billPaymentPrefs.clear(order.orderNo)
                        stopBillPayPoll()
                        loadMe(); loadPayments(); loadBill()
                        onConfirmed()
                    }
                }
                orderStatusRefreshing = false
            }
            return
        }
        val id = order.id ?: return
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
        ordersError = null
        viewModelScope.launch {
            coroutineScope {
                val pppoeDeferred = async { repo.orders() }
                val ppobDeferred = async { repo.ppobTransactions(limit = 50) }

                val merged = mutableListOf<UnifiedHistoryItem>()
                var hadError: String? = null

                when (val r = pppoeDeferred.await()) {
                    is ApiResult.Ok -> {
                        orders.clear()
                        orders.addAll(r.data.orders)
                        merged.addAll(r.data.orders.map { it.toUnifiedHistoryItem() })
                        if (!r.data.success && r.data.orders.isEmpty()) {
                            hadError = "Gagal memuat pesanan PPPoE"
                        }
                    }
                    is ApiResult.Err -> hadError = r.message
                }

                when (val r = ppobDeferred.await()) {
                    is ApiResult.Ok -> {
                        merged.addAll(r.data.transactions.map { it.toUnifiedHistoryItem() })
                        if (!r.data.success && r.data.transactions.isEmpty() &&
                            hadError == null && !r.data.message.isNullOrBlank()
                        ) {
                            hadError = r.data.message
                        }
                    }
                    is ApiResult.Err -> {
                        if (hadError == null) hadError = r.message
                    }
                }

                unifiedOrders.clear()
                unifiedOrders.addAll(
                    merged.sortedByDescending { it.createdAt.orEmpty() },
                )
                // Tampilkan error hanya jika kedua sumber gagal / list kosong + ada error
                ordersError = if (unifiedOrders.isEmpty()) hadError else null
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
