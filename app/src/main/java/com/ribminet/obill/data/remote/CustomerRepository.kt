package com.ribminet.obill.data.remote

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.IOException

class CustomerRepository(
    private val api: CustomerApi,
    val tokenStore: TokenStore,
) {
    private val gson = Gson()

    val isLoggedIn: Boolean get() = tokenStore.isLoggedIn

    suspend fun requestOtp(phone: String): ApiResult<RequestOtpResp> =
        safe { api.requestOtp(RequestOtpReq(phone)) }

    suspend fun verifyOtp(phone: String, otp: String): ApiResult<VerifyOtpResp> {
        val result = safe { api.verifyOtp(VerifyOtpReq(phone, otp)) }
        if (result is ApiResult.Ok && !result.data.token.isNullOrBlank()) {
            tokenStore.token = result.data.token
            tokenStore.expiresAt = result.data.expiresAt
            tokenStore.phone = phone
            result.data.customer?.id?.let { tokenStore.customerId = it }
        }
        return result
    }

    suspend fun logout(subscriptionId: String? = null): ApiResult<BaseResp> {
        val result = safe { api.logout(LogoutReq(subscriptionId)) }
        tokenStore.clear()
        return result
    }

    suspend fun oneSignalConfig(): ApiResult<OneSignalConfigResp> = safe { api.oneSignalConfig() }

    suspend fun registerOneSignal(
        subscriptionId: String,
        deviceName: String? = null,
    ): ApiResult<RegisterOneSignalResp> = safe {
        api.registerOneSignal(
            RegisterOneSignalReq(
                subscriptionId = subscriptionId,
                platform = "android",
                deviceName = deviceName,
            )
        )
    }

    suspend fun unregisterOneSignal(subscriptionId: String): ApiResult<BaseResp> =
        safe { api.unregisterOneSignal(UnregisterOneSignalReq(subscriptionId)) }

    suspend fun notifications(
        sinceId: Int? = null,
        unreadOnly: Boolean = false,
        limit: Int = 50,
    ): ApiResult<NotificationsResp> = safe {
        api.notifications(
            sinceId = sinceId,
            unreadOnly = if (unreadOnly) 1 else 0,
            limit = limit,
        )
    }

    suspend fun notificationsPoll(sinceId: Int, timeout: Int = 25): ApiResult<NotificationsPollResp> =
        safe { api.notificationsPoll(sinceId, timeout) }

    suspend fun unreadCount(): ApiResult<UnreadCountResp> = safe { api.unreadCount() }

    suspend fun notificationsRead(id: Int? = null, ids: List<Int>? = null, all: Boolean = false): ApiResult<BaseResp> =
        safe { api.notificationsRead(NotificationsReadReq(id = id, ids = ids, all = if (all) true else null)) }

    suspend fun announcements(): ApiResult<AnnouncementListResp> = safe { api.announcements() }

    suspend fun announcementRead(
        announcementId: Int,
        readerKey: String? = null,
        readerName: String? = null,
    ): ApiResult<AnnouncementReadResp> = safe {
        api.announcementRead(
            AnnouncementReadReq(
                announcementId = announcementId,
                readerKey = readerKey?.takeIf { it.isNotBlank() },
                readerName = readerName?.takeIf { it.isNotBlank() },
            ),
        )
    }

    suspend fun me(): ApiResult<MeResp> = safe { api.me() }

    /**
     * Ubah biodata (partial update). Jika [photoBytes] ada, gunakan multipart agar
     * foto langsung diunggah. Field yang null tidak dikirim (tidak diubah server).
     */
    suspend fun profileUpdate(
        name: String?,
        email: String?,
        address: String?,
        photoBytes: ByteArray? = null,
        photoMime: String? = null,
        photoName: String? = null,
        removePhoto: Boolean = false,
    ): ApiResult<ProfileUpdateResp> {
        if (photoBytes == null) {
            return safe {
                api.profileUpdate(
                    ProfileUpdateReq(
                        customerName = name,
                        email = email,
                        address = address,
                        removePhoto = if (removePhoto) 1 else null,
                    )
                )
            }
        }
        val mime = (photoMime ?: "image/jpeg").toMediaTypeOrNull()
        val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
        name?.let { builder.addFormDataPart("customer_name", it) }
        email?.let { builder.addFormDataPart("email", it) }
        address?.let { builder.addFormDataPart("address", it) }
        builder.addFormDataPart(
            "photo",
            photoName ?: "profile.jpg",
            photoBytes.toRequestBody(mime, 0, photoBytes.size),
        )
        return safe { api.profileUpdateMultipart(builder.build()) }
    }

    suspend fun payments(limit: Int = 100): ApiResult<PaymentsResp> = safe { api.payments(limit) }

    suspend fun device(): ApiResult<DeviceResp> = safe { api.device() }

    suspend fun deviceClients(): ApiResult<DeviceClientsResp> = safe { api.deviceClients() }

    suspend fun deviceRxHistory(limit: Int = 100, days: Int? = null): ApiResult<RxHistoryResp> =
        safe { api.deviceRxHistory(limit, days) }

    suspend fun deviceReboot(): ApiResult<DeviceActionResp> = safe { api.deviceReboot() }

    suspend fun deviceRefresh(): ApiResult<DeviceActionResp> = safe { api.deviceRefresh() }

    suspend fun deviceWifi(ssid: String?, password: String?): ApiResult<DeviceActionResp> =
        safe { api.deviceWifi(WifiUpdateReq(ssid?.ifBlank { null }, password?.ifBlank { null })) }

    suspend fun complaint(
        message: String,
        category: String?,
        subject: String?,
        photoBytes: ByteArray? = null,
        photoMime: String? = null,
        photoName: String? = null,
    ): ApiResult<ComplaintResp> {
        val cat = category?.ifBlank { null }
        val subj = subject?.ifBlank { null }
        if (photoBytes == null) {
            return safe { api.complaint(ComplaintReq(message, cat, subj)) }
        }
        val mime = (photoMime ?: "image/jpeg").toMediaTypeOrNull()
        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("message", message)
            .apply {
                cat?.let { addFormDataPart("category", it) }
                subj?.let { addFormDataPart("subject", it) }
                addFormDataPart(
                    "photo",
                    photoName ?: "photo.jpg",
                    photoBytes.toRequestBody(mime, 0, photoBytes.size),
                )
            }
            .build()
        return safe { api.complaintMultipart(multipart) }
    }

    suspend fun bill(): ApiResult<BillResp> = safe { api.bill() }

    suspend fun activationInfo(): ApiResult<ActivationInfoResp> = safe { api.activationInfo() }

    suspend fun latePenalty(paymentDate: String? = null, amount: Long? = null): ApiResult<LatePenaltyResp> =
        safe { api.latePenalty(paymentDate, amount) }

    suspend fun billPay(method: String? = "qris_dinamis", note: String? = null): ApiResult<BillPayResp> =
        safe { api.billPay(BillPayReq(method, note)) }

    suspend fun billPayStatus(orderNo: String? = null, orderId: Int? = null): ApiResult<BillPayStatusResp> =
        safe { api.billPayStatus(orderNo, orderId) }

    suspend fun upgradeOptions(): ApiResult<UpgradeOptionsResp> = safe { api.upgradeOptions() }

    suspend fun packageOptions(): ApiResult<UpgradeOptionsResp> = safe { api.packageOptions() }

    suspend fun packageChangeRequest(toProfileId: Int): ApiResult<PackageChangeResp> =
        safe { api.packageChangeRequest(PackageChangeReq(toProfileId)) }

    suspend fun packageChange(): ApiResult<PackageChangeResp> = safe { api.packageChange() }

    suspend fun packageChangeCancel(): ApiResult<PackageChangeResp> =
        safe { api.packageChangeCancel(emptyMap()) }

    suspend fun paymentMethods(): ApiResult<PaymentMethodsResp> = safe { api.paymentMethods() }

    suspend fun upgradeRequest(toProfileId: Int, method: String, note: String? = null): ApiResult<OrderResp> =
        safe { api.upgradeRequest(UpgradeReq(toProfileId, method, note)) }

    suspend fun orders(): ApiResult<OrdersResp> = safe { api.orders() }

    suspend fun order(id: Int): ApiResult<OrderResp> = safe { api.order(id) }

    suspend fun orderConfirm(
        orderId: Int,
        referenceNo: String? = null,
        proofUrl: String? = null,
        note: String? = null,
    ): ApiResult<OrderResp> = safe { api.orderConfirm(OrderConfirmReq(orderId, referenceNo, proofUrl, note)) }

    suspend fun orderCancel(orderId: Int): ApiResult<OrderResp> =
        safe { api.orderCancel(OrderCancelReq(orderId)) }

    // ---- Wallet ----
    suspend fun wallet(): ApiResult<WalletResp> = safe { api.wallet() }

    suspend fun walletLedger(limit: Int = 50): ApiResult<WalletLedgerResp> =
        safe { api.walletLedger(limit) }

    suspend fun walletTopups(status: String? = null, limit: Int = 50): ApiResult<WalletTopupsListResp> =
        safe { api.walletTopups(status, limit) }

    suspend fun walletTopup(amount: Long): ApiResult<WalletTopupResp> =
        safe { api.walletTopup(WalletTopupReq(amount)) }

    suspend fun walletTopupStatus(topupCode: String): ApiResult<WalletTopupStatusResp> =
        safe { api.walletTopupStatus(topupCode) }

    suspend fun walletTopupCancel(topupCode: String): ApiResult<WalletTopupCancelResp> =
        safe { api.walletTopupCancel(WalletTopupCancelReq(topupCode)) }

    // ---- PPOB ----
    suspend fun ppobCatalog(
        cmd: String = "prepaid",
        category: String? = null,
        brand: String? = null,
        type: String? = null,
        code: String? = null,
    ): ApiResult<PpobCatalogResp> = safe { api.ppobCatalog(cmd, category, brand, type, code) }

    suspend fun ppobSyncStatus(): ApiResult<PpobSyncStatusResp> = safe { api.ppobSyncStatus() }

    suspend fun ppobTopup(
        buyerSkuCode: String,
        customerNo: String,
        productName: String? = null,
    ): ApiResult<PpobTransactionResp> = safe {
        api.ppobTopup(PpobTopupReq(buyerSkuCode, customerNo, productName))
    }

    suspend fun ppobInquiry(
        buyerSkuCode: String,
        customerNo: String,
        productName: String? = null,
    ): ApiResult<PpobTransactionResp> = safe {
        api.ppobInquiry(PpobInquiryReq(buyerSkuCode, customerNo, productName))
    }

    suspend fun ppobPayPasca(
        refId: String,
        buyerSkuCode: String,
        customerNo: String,
    ): ApiResult<PpobTransactionResp> = safe {
        api.ppobPayPasca(PpobPayPascaReq(refId, buyerSkuCode, customerNo))
    }

    suspend fun ppobQuote(
        buyerSkuCode: String,
        customerNo: String,
    ): ApiResult<PpobQuoteResp> = safe {
        api.ppobQuote(PpobQuoteReq(buyerSkuCode, customerNo))
    }

    suspend fun ppobCheckout(
        buyerSkuCode: String,
        customerNo: String,
        productName: String? = null,
    ): ApiResult<PpobCheckoutResp> = safe {
        api.ppobCheckout(PpobCheckoutReq(buyerSkuCode, customerNo, productName))
    }

    suspend fun ppobPaymentStatus(refId: String): ApiResult<PpobPaymentStatusResp> =
        safe { api.ppobPaymentStatus(refId) }

    suspend fun ppobCancel(refId: String): ApiResult<PpobCancelResp> =
        safe { api.ppobCancel(PpobCancelReq(refId)) }

    suspend fun ppobTransaction(refId: String): ApiResult<PpobTransactionResp> =
        safe { api.ppobTransaction(refId) }

    suspend fun ppobTransactions(
        limit: Int = 50,
        offset: Int = 0,
        status: String? = null,
    ): ApiResult<PpobTransactionsResp> = safe { api.ppobTransactions(limit, offset, status) }

    suspend fun legalIndex(): ApiResult<LegalIndexResp> = safe { api.legalIndex() }

    suspend fun legalPrivacy(): ApiResult<LegalDocumentResp> = safe { api.legalPrivacy() }

    suspend fun legalTerms(): ApiResult<LegalDocumentResp> = safe { api.legalTerms() }

    private suspend fun <T> safe(block: suspend () -> T): ApiResult<T> = withContext(Dispatchers.IO) {
        try {
            ApiResult.Ok(block())
        } catch (e: HttpException) {
            val raw = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
            val err = try { if (!raw.isNullOrBlank()) gson.fromJson(raw, ErrorResp::class.java) else null } catch (_: Exception) { null }
            if (e.code() == 401) tokenStore.clear()
            ApiResult.Err(
                message = err?.message ?: "Terjadi kesalahan (${e.code()}).",
                code = err?.code ?: if (e.code() == 401) "UNAUTHORIZED" else null,
                httpCode = e.code(),
                retryAfterSeconds = err?.retryAfterSeconds,
                remainingAttempts = err?.remainingAttempts,
            )
        } catch (e: IOException) {
            ApiResult.Err(message = "Tidak dapat terhubung ke server. Periksa koneksi internet Anda.", code = "NETWORK")
        } catch (e: Exception) {
            ApiResult.Err(message = e.message ?: "Terjadi kesalahan tak terduga.", code = "UNKNOWN")
        }
    }
}
