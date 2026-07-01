package com.ribminet.obill.data.remote

import com.google.gson.annotations.SerializedName

// ---- Umum ----
data class BaseResp(
    val success: Boolean = false,
    val message: String? = null,
    val code: String? = null,
)

data class ErrorResp(
    val success: Boolean = false,
    val message: String? = null,
    val code: String? = null,
    @SerializedName("retry_after_seconds") val retryAfterSeconds: Int? = null,
    @SerializedName("remaining_attempts") val remainingAttempts: Int? = null,
)

// ---- Auth ----
data class RequestOtpReq(val phone: String)

data class RequestOtpResp(
    val success: Boolean = false,
    val message: String? = null,
    val code: String? = null,
    val registered: Boolean? = null,
    @SerializedName("otp_ttl_seconds") val otpTtlSeconds: Int? = null,
    @SerializedName("resend_after_seconds") val resendAfterSeconds: Int? = null,
)

data class VerifyOtpReq(val phone: String, val otp: String)

data class VerifyOtpResp(
    val success: Boolean = false,
    val message: String? = null,
    val code: String? = null,
    val token: String? = null,
    @SerializedName("token_type") val tokenType: String? = null,
    @SerializedName("expires_at") val expiresAt: String? = null,
    val customer: CustomerDto? = null,
)

// ---- Customer / langganan aktif ----
data class MeResp(
    val success: Boolean = false,
    val customer: CustomerDto? = null,
    val activation: ActivationDto? = null,
)

data class ActivationDto(
    @SerializedName("is_first_activation") val isFirstActivation: Boolean? = null,
    @SerializedName("requires_installation_fee") val requiresInstallationFee: Boolean? = null,
    @SerializedName("installation_fee_amount") val installationFeeAmount: Long? = null,
    @SerializedName("installation_fee_label") val installationFeeLabel: String? = null,
    @SerializedName("payment_count") val paymentCount: Int? = null,
    val reason: String? = null,
)

data class BillAmountsDto(
    @SerializedName("subscription_amount") val subscriptionAmount: Long? = null,
    @SerializedName("installation_fee_amount") val installationFeeAmount: Long? = null,
    @SerializedName("installation_fee_label") val installationFeeLabel: String? = null,
    @SerializedName("late_penalty_amount") val latePenaltyAmount: Long? = null,
    @SerializedName("total_amount") val totalAmount: Long? = null,
)

data class CustomerDto(
    val id: Int? = null,
    @SerializedName("customer_code") val customerCode: String? = null,
    @SerializedName("customer_name") val customerName: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    @SerializedName("username_pppoe") val usernamePppoe: String? = null,
    @SerializedName("photo_profile") val photoProfile: String? = null,
    @SerializedName("photo_profile_url") val photoProfileUrl: String? = null,
    @SerializedName("profile_id") val profileId: Int? = null,
    @SerializedName("profile_name") val profileName: String? = null,
    @SerializedName("rate_limit") val rateLimit: String? = null,
    val price: Long? = null,
    @SerializedName("started_at") val startedAt: String? = null,
    @SerializedName("expired_at") val expiredAt: String? = null,
    @SerializedName("status_langganan") val statusLangganan: String? = null,
    @SerializedName("activation_state") val activationState: String? = null,
    @SerializedName("time_remaining") val timeRemaining: TimeRemainingDto? = null,
    @SerializedName("time_remaining_display") val timeRemainingDisplay: String? = null,
    @SerializedName("next_payment") val nextPayment: NextPaymentDto? = null,
    val activation: ActivationDto? = null,
    @SerializedName("is_first_activation") val isFirstActivationFlat: Boolean? = null,
    @SerializedName("requires_installation_fee") val requiresInstallationFeeFlat: Boolean? = null,
    @SerializedName("installation_fee_amount") val installationFeeAmountFlat: Long? = null,
    @SerializedName("installation_fee_label") val installationFeeLabelFlat: String? = null,
)

data class TimeRemainingDto(
    val days: Int? = null,
    val hours: Int? = null,
    val minutes: Int? = null,
    val seconds: Int? = null,
    @SerializedName("total_seconds_remaining") val totalSecondsRemaining: Long? = null,
)

data class NextPaymentDto(
    @SerializedName("due_date") val dueDate: String? = null,
    @SerializedName("due_datetime") val dueDatetime: String? = null,
    @SerializedName("days_until_due") val daysUntilDue: Int? = null,
    @SerializedName("is_overdue") val isOverdue: Boolean? = null,
)

// ---- Ubah biodata ----
data class ProfileUpdateReq(
    @SerializedName("customer_name") val customerName: String? = null,
    val email: String? = null,
    val address: String? = null,
    @SerializedName("photo_base64") val photoBase64: String? = null,
    @SerializedName("remove_photo") val removePhoto: Int? = null,
)

data class ProfileUpdateResp(
    val success: Boolean = false,
    val message: String? = null,
    val code: String? = null,
    val customer: CustomerDto? = null,
)

// ---- Riwayat pembayaran ----
data class PaymentsResp(
    val success: Boolean = false,
    @SerializedName("next_payment") val nextPayment: NextPaymentDto? = null,
    val count: Int? = null,
    val payments: List<PaymentDto> = emptyList(),
)

data class PaymentDto(
    val id: Int? = null,
    @SerializedName("invoice_no") val invoiceNo: String? = null,
    @SerializedName("periode_label") val periodeLabel: String? = null,
    val amount: Long? = null,
    @SerializedName("payment_method") val paymentMethod: String? = null,
    @SerializedName("reference_no") val referenceNo: String? = null,
    @SerializedName("paid_at") val paidAt: String? = null,
    @SerializedName("next_expired_at") val nextExpiredAt: String? = null,
    val catatan: String? = null,
)

// ---- Tagihan / perpanjang ----
data class BillResp(
    val success: Boolean = false,
    val bill: BillDto? = null,
    @SerializedName("open_order") val openOrder: OrderDto? = null,
    val activation: ActivationDto? = null,
    val amounts: BillAmountsDto? = null,
)

data class BillDto(
    @SerializedName("profile_id") val profileId: Int? = null,
    @SerializedName("profile_name") val profileName: String? = null,
    @SerializedName("current_profile_id") val currentProfileId: Int? = null,
    @SerializedName("current_profile_name") val currentProfileName: String? = null,
    val amount: Long? = null,
    @SerializedName("status_langganan") val statusLangganan: String? = null,
    @SerializedName("pending_change") val pendingChange: PendingChangeDto? = null,
    @SerializedName("next_payment") val nextPayment: NextPaymentDto? = null,
    @SerializedName("preview_renewal") val previewRenewal: PreviewRenewalDto? = null,
    val activation: ActivationDto? = null,
    val amounts: BillAmountsDto? = null,
    @SerializedName("total_amount") val totalAmountFlat: Long? = null,
    @SerializedName("subscription_amount") val subscriptionAmountFlat: Long? = null,
    @SerializedName("installation_fee_amount") val installationFeeAmountFlat: Long? = null,
    @SerializedName("installation_fee_label") val installationFeeLabelFlat: String? = null,
    @SerializedName("late_penalty_amount") val latePenaltyAmountFlat: Long? = null,
    @SerializedName("is_first_activation") val isFirstActivationFlat: Boolean? = null,
    @SerializedName("requires_installation_fee") val requiresInstallationFeeFlat: Boolean? = null,
)

data class ActivationInfoResp(
    val success: Boolean = false,
    val message: String? = null,
    val activation: ActivationDto? = null,
    val amounts: BillAmountsDto? = null,
    @SerializedName("total_amount") val totalAmountFlat: Long? = null,
    @SerializedName("subscription_amount") val subscriptionAmountFlat: Long? = null,
    @SerializedName("installation_fee_amount") val installationFeeAmountFlat: Long? = null,
    @SerializedName("installation_fee_label") val installationFeeLabelFlat: String? = null,
    @SerializedName("late_penalty_amount") val latePenaltyAmountFlat: Long? = null,
    @SerializedName("is_first_activation") val isFirstActivationFlat: Boolean? = null,
    @SerializedName("requires_installation_fee") val requiresInstallationFeeFlat: Boolean? = null,
)

data class PendingChangeDto(
    val pending: Boolean? = null,
    @SerializedName("from_profile_id") val fromProfileId: Int? = null,
    @SerializedName("from_profile_name") val fromProfileName: String? = null,
    @SerializedName("to_profile_id") val toProfileId: Int? = null,
    @SerializedName("to_profile_name") val toProfileName: String? = null,
    @SerializedName("new_price") val newPrice: Long? = null,
    @SerializedName("is_upgrade") val isUpgrade: Boolean? = null,
    @SerializedName("requested_at") val requestedAt: String? = null,
    @SerializedName("effective_note") val effectiveNote: String? = null,
)

data class PreviewRenewalDto(
    @SerializedName("paid_at") val paidAt: String? = null,
    @SerializedName("anchor_date") val anchorDate: String? = null,
    @SerializedName("from_due_date") val fromDueDate: Boolean? = null,
    @SerializedName("new_started_at") val newStartedAt: String? = null,
    @SerializedName("new_expired_at") val newExpiredAt: String? = null,
    @SerializedName("extension_days") val extensionDays: Int? = null,
)

data class BillPayReq(
    @SerializedName("payment_method") val paymentMethod: String,
    @SerializedName("customer_note") val customerNote: String? = null,
)

data class BillPayResp(
    val success: Boolean = false,
    val message: String? = null,
    val code: String? = null,
    val order: OrderDto? = null,
    @SerializedName("preview_renewal") val previewRenewal: PreviewRenewalDto? = null,
    val activation: ActivationDto? = null,
    val amounts: BillAmountsDto? = null,
)

// ---- Denda keterlambatan ----
data class LatePenaltyResp(
    val success: Boolean = false,
    val activation: ActivationDto? = null,
    val amounts: BillAmountsDto? = null,
    val message: String? = null,
)

// ---- Upgrade paket ----
data class UpgradeOptionsResp(
    val success: Boolean = false,
    @SerializedName("current_profile_id") val currentProfileId: Int? = null,
    @SerializedName("current_profile_name") val currentProfileName: String? = null,
    @SerializedName("current_price") val currentPrice: Long? = null,
    @SerializedName("packages_source") val packagesSource: String? = null,
    @SerializedName("pending_change") val pendingChange: PendingChangeDto? = null,
    val packages: List<PackageDto> = emptyList(),
)

data class PackageChangeReq(
    @SerializedName("to_profile_id") val toProfileId: Int,
)

data class PackageChangeResp(
    val success: Boolean = false,
    val message: String? = null,
    val code: String? = null,
    @SerializedName("pending_change") val pendingChange: PendingChangeDto? = null,
    @SerializedName("admin_notified") val adminNotified: Boolean? = null,
    @SerializedName("customer_notified") val customerNotified: Boolean? = null,
    @SerializedName("next_payment") val nextPayment: NextPaymentDto? = null,
)

data class PackageDto(
    @SerializedName("profile_id") val profileId: Int? = null,
    @SerializedName("profile_name") val profileName: String? = null,
    @SerializedName("rate_limit") val rateLimit: String? = null,
    val price: Long? = null,
    val keterangan: String? = null,
    @SerializedName("is_current") val isCurrent: Boolean? = null,
    @SerializedName("is_upgrade") val isUpgrade: Boolean? = null,
)

data class UpgradeReq(
    @SerializedName("to_profile_id") val toProfileId: Int,
    @SerializedName("payment_method") val paymentMethod: String,
    @SerializedName("customer_note") val customerNote: String? = null,
)

// ---- Metode pembayaran ----
data class PaymentMethodsResp(
    val success: Boolean = false,
    @SerializedName("payment_methods") val paymentMethods: List<PayChannelDto> = emptyList(),
)

data class PayChannelDto(
    val method: String? = null,
    val label: String? = null,
    val type: String? = null,
    val accounts: List<BankAccountDto>? = null,
    @SerializedName("merchant_name") val merchantName: String? = null,
    val phone: String? = null,
    @SerializedName("qr_image_url") val qrImageUrl: String? = null,
    val note: String? = null,
)

data class BankAccountDto(
    val bank: String? = null,
    @SerializedName("account_number") val accountNumber: String? = null,
    @SerializedName("account_name") val accountName: String? = null,
)

// ---- Pesanan (upgrade + renewal) ----
data class OrdersResp(
    val success: Boolean = false,
    val count: Int? = null,
    val orders: List<OrderDto> = emptyList(),
)

data class OrderResp(
    val success: Boolean = false,
    val message: String? = null,
    val code: String? = null,
    val order: OrderDto? = null,
    @SerializedName("preview_renewal") val previewRenewal: PreviewRenewalDto? = null,
)

data class OrderDto(
    val id: Int? = null,
    @SerializedName("order_no") val orderNo: String? = null,
    @SerializedName("order_type") val orderType: String? = null,
    @SerializedName("order_type_label") val orderTypeLabel: String? = null,
    @SerializedName("from_profile_id") val fromProfileId: Int? = null,
    @SerializedName("from_profile_name") val fromProfileName: String? = null,
    @SerializedName("to_profile_id") val toProfileId: Int? = null,
    @SerializedName("to_profile_name") val toProfileName: String? = null,
    val amount: Long? = null,
    @SerializedName("payment_method") val paymentMethod: String? = null,
    @SerializedName("payment_instruction") val paymentInstruction: PayChannelDto? = null,
    val status: String? = null,
    @SerializedName("status_label") val statusLabel: String? = null,
    @SerializedName("reference_no") val referenceNo: String? = null,
    @SerializedName("proof_url") val proofUrl: String? = null,
    @SerializedName("customer_note") val customerNote: String? = null,
    @SerializedName("admin_note") val adminNote: String? = null,
    @SerializedName("confirmed_at") val confirmedAt: String? = null,
    @SerializedName("paid_at") val paidAt: String? = null,
    @SerializedName("processed_at") val processedAt: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    val activation: ActivationDto? = null,
    val amounts: BillAmountsDto? = null,
)

data class OrderConfirmReq(
    @SerializedName("order_id") val orderId: Int,
    @SerializedName("reference_no") val referenceNo: String? = null,
    @SerializedName("proof_url") val proofUrl: String? = null,
    @SerializedName("customer_note") val customerNote: String? = null,
)

data class OrderCancelReq(
    @SerializedName("order_id") val orderId: Int,
)

// ---- Perangkat (Genie ACS) ----
data class DeviceResp(
    val success: Boolean = false,
    val found: Boolean = false,
    val device: DeviceDto? = null,
    val code: String? = null,
    val message: String? = null,
)

data class DeviceDto(
    @SerializedName("serial_number") val serialNumber: String? = null,
    val manufacturer: String? = null,
    val model: String? = null,
    val firmware: String? = null,
    @SerializedName("ip_address") val ipAddress: String? = null,
    val ssid: String? = null,
    @SerializedName("pon_mode") val ponMode: String? = null,
    @SerializedName("rx_power_dbm") val rxPowerDbm: Double? = null,
    @SerializedName("rx_power_status") val rxPowerStatus: String? = null,
    @SerializedName("temperature_c") val temperatureC: Double? = null,
    val uptime: String? = null,
    @SerializedName("connected_devices") val connectedDevices: Int? = null,
    val online: Boolean? = null,
    @SerializedName("online_source") val onlineSource: String? = null,
    @SerializedName("last_inform") val lastInform: String? = null,
)

data class DeviceClientsResp(
    val success: Boolean = false,
    val found: Boolean = false,
    val count: Int? = null,
    @SerializedName("wifi_count") val wifiCount: Int? = null,
    @SerializedName("lan_count") val lanCount: Int? = null,
    val clients: List<DeviceClientDto> = emptyList(),
)

data class DeviceClientDto(
    val hostname: String? = null,
    @SerializedName("ip_address") val ipAddress: String? = null,
    @SerializedName("mac_address") val macAddress: String? = null,
    @SerializedName("interface_type") val interfaceType: String? = null,
    val connection: String? = null,
    @SerializedName("address_source") val addressSource: String? = null,
    @SerializedName("lease_remaining") val leaseRemaining: Long? = null,
    val active: Boolean? = null,
)

data class RxHistoryResp(
    val success: Boolean = false,
    val count: Int? = null,
    val summary: RxSummaryDto? = null,
    val history: List<RxPointDto> = emptyList(),
)

data class RxSummaryDto(
    val count: Int? = null,
    @SerializedName("min_dbm") val minDbm: Double? = null,
    @SerializedName("max_dbm") val maxDbm: Double? = null,
    @SerializedName("avg_dbm") val avgDbm: Double? = null,
    @SerializedName("latest_dbm") val latestDbm: Double? = null,
)

data class RxPointDto(
    @SerializedName("rx_dbm") val rxDbm: Double? = null,
    @SerializedName("rx_power_status") val rxPowerStatus: String? = null,
    @SerializedName("recorded_at") val recordedAt: String? = null,
)

data class DeviceActionResp(
    val success: Boolean = false,
    val queued: Boolean? = null,
    val message: String? = null,
    val code: String? = null,
)

data class WifiUpdateReq(
    val ssid: String? = null,
    val password: String? = null,
)

// ---- Pengaduan (relay ke WhatsApp admin) ----
data class ComplaintReq(
    val message: String,
    val category: String? = null,
    val subject: String? = null,
)

data class ComplaintResp(
    val success: Boolean = false,
    val message: String? = null,
    val code: String? = null,
    @SerializedName("photo_sent") val photoSent: Boolean? = null,
    @SerializedName("photo_error") val photoError: String? = null,
)

// ---- OneSignal & Notifikasi ----
data class OneSignalConfigResp(
    val success: Boolean = false,
    val configured: Boolean? = null,
    @SerializedName("app_id") val appId: String? = null,
    @SerializedName("external_user_id_hint") val externalUserIdHint: String? = null,
)

data class RegisterOneSignalReq(
    @SerializedName("subscription_id") val subscriptionId: String,
    val platform: String? = "android",
    @SerializedName("device_name") val deviceName: String? = null,
)

data class RegisterOneSignalResp(
    val success: Boolean = false,
    val message: String? = null,
    @SerializedName("device_id") val deviceId: Int? = null,
    @SerializedName("customer_id") val customerId: Int? = null,
)

data class UnregisterOneSignalReq(
    @SerializedName("subscription_id") val subscriptionId: String,
)

data class LogoutReq(
    @SerializedName("subscription_id") val subscriptionId: String? = null,
)

data class NotificationDto(
    val id: Int? = null,
    val type: String? = null,
    val title: String? = null,
    val body: String? = null,
    val data: Map<String, Any?>? = null,
    @SerializedName("order_id") val orderId: Int? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("read_at") val readAt: String? = null,
    @SerializedName("is_read") val isRead: Boolean? = null,
)

data class NotificationsResp(
    val success: Boolean = false,
    val count: Int? = null,
    @SerializedName("unread_count") val unreadCount: Int? = null,
    @SerializedName("latest_id") val latestId: Int? = null,
    val notifications: List<NotificationDto> = emptyList(),
)

data class NotificationsPollResp(
    val success: Boolean = false,
    @SerializedName("has_new") val hasNew: Boolean? = null,
    @SerializedName("latest_id") val latestId: Int? = null,
    @SerializedName("unread_count") val unreadCount: Int? = null,
    val count: Int? = null,
    val notifications: List<NotificationDto> = emptyList(),
)

data class UnreadCountResp(
    val success: Boolean = false,
    @SerializedName("unread_count") val unreadCount: Int? = null,
    @SerializedName("latest_id") val latestId: Int? = null,
)

data class NotificationsReadReq(
    val id: Int? = null,
    val ids: List<Int>? = null,
    val all: Boolean? = null,
)
