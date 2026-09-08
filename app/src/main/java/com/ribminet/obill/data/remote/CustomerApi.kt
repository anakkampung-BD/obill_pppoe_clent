package com.ribminet.obill.data.remote

import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CustomerApi {

    @POST("api/customer/langganan/request_otp")
    suspend fun requestOtp(@Body body: RequestOtpReq): RequestOtpResp

    @POST("api/customer/langganan/verify_otp")
    suspend fun verifyOtp(@Body body: VerifyOtpReq): VerifyOtpResp

    @POST("api/customer/langganan/logout")
    suspend fun logout(@Body body: LogoutReq = LogoutReq()): BaseResp

    @GET("api/customer/langganan/onesignal_config")
    suspend fun oneSignalConfig(): OneSignalConfigResp

    @POST("api/customer/langganan/register_onesignal")
    suspend fun registerOneSignal(@Body body: RegisterOneSignalReq): RegisterOneSignalResp

    @POST("api/customer/langganan/unregister_onesignal")
    suspend fun unregisterOneSignal(@Body body: UnregisterOneSignalReq): BaseResp

    @GET("api/customer/langganan/notifications")
    suspend fun notifications(
        @Query("since_id") sinceId: Int? = null,
        @Query("unread_only") unreadOnly: Int? = null,
        @Query("limit") limit: Int = 50,
    ): NotificationsResp

    @GET("api/customer/langganan/notifications_poll")
    suspend fun notificationsPoll(
        @Query("since_id") sinceId: Int,
        @Query("timeout") timeout: Int = 25,
    ): NotificationsPollResp

    @GET("api/customer/langganan/unread_count")
    suspend fun unreadCount(): UnreadCountResp

    @POST("api/customer/langganan/notifications_read")
    suspend fun notificationsRead(@Body body: NotificationsReadReq): BaseResp

    @GET("api/customer/langganan/announcements")
    suspend fun announcements(): AnnouncementListResp

    @POST("api/customer/langganan/announcement_read")
    suspend fun announcementRead(@Body body: AnnouncementReadReq): AnnouncementReadResp

    @GET("api/customer/langganan/me")
    suspend fun me(): MeResp

    @POST("api/customer/langganan/profile_update")
    suspend fun profileUpdate(@Body body: ProfileUpdateReq): ProfileUpdateResp

    @POST("api/customer/langganan/profile_update")
    suspend fun profileUpdateMultipart(@Body body: MultipartBody): ProfileUpdateResp

    @GET("api/customer/langganan/payments")
    suspend fun payments(@Query("limit") limit: Int = 100): PaymentsResp

    @GET("api/customer/langganan/device")
    suspend fun device(): DeviceResp

    @GET("api/customer/langganan/device_clients")
    suspend fun deviceClients(): DeviceClientsResp

    @GET("api/customer/langganan/device_rx_history")
    suspend fun deviceRxHistory(
        @Query("limit") limit: Int = 100,
        @Query("days") days: Int? = null,
    ): RxHistoryResp

    @POST("api/customer/langganan/device_reboot")
    suspend fun deviceReboot(): DeviceActionResp

    @POST("api/customer/langganan/device_refresh")
    suspend fun deviceRefresh(): DeviceActionResp

    @POST("api/customer/langganan/device_wifi")
    suspend fun deviceWifi(@Body body: WifiUpdateReq): DeviceActionResp

    @POST("api/customer/langganan/complaint")
    suspend fun complaint(@Body body: ComplaintReq): ComplaintResp

    @POST("api/customer/langganan/complaint")
    suspend fun complaintMultipart(@Body body: MultipartBody): ComplaintResp

    @GET("api/customer/langganan/bill")
    suspend fun bill(): BillResp

    @GET("api/customer/langganan/activation_info")
    suspend fun activationInfo(): ActivationInfoResp

    @GET("api/customer/langganan/late_penalty")
    suspend fun latePenalty(
        @Query("payment_date") paymentDate: String? = null,
        @Query("amount") amount: Long? = null,
    ): LatePenaltyResp

    @POST("api/customer/langganan/bill_pay")
    suspend fun billPay(@Body body: BillPayReq): BillPayResp

    @GET("api/customer/langganan/bill_pay_status")
    suspend fun billPayStatus(
        @Query("order_no") orderNo: String? = null,
        @Query("order_id") orderId: Int? = null,
    ): BillPayStatusResp

    @GET("api/customer/langganan/upgrade_options")
    suspend fun upgradeOptions(): UpgradeOptionsResp

    @GET("api/customer/langganan/package_options")
    suspend fun packageOptions(): UpgradeOptionsResp

    @POST("api/customer/langganan/package_change_request")
    suspend fun packageChangeRequest(@Body body: PackageChangeReq): PackageChangeResp

    @GET("api/customer/langganan/package_change")
    suspend fun packageChange(): PackageChangeResp

    @POST("api/customer/langganan/package_change_cancel")
    suspend fun packageChangeCancel(@Body body: Map<String, String>): PackageChangeResp

    @GET("api/customer/langganan/payment_methods")
    suspend fun paymentMethods(): PaymentMethodsResp

    @POST("api/customer/langganan/upgrade_request")
    suspend fun upgradeRequest(@Body body: UpgradeReq): OrderResp

    @GET("api/customer/langganan/orders")
    suspend fun orders(): OrdersResp

    @GET("api/customer/langganan/order/{id}")
    suspend fun order(@Path("id") id: Int): OrderResp

    @POST("api/customer/langganan/order_confirm")
    suspend fun orderConfirm(@Body body: OrderConfirmReq): OrderResp

    @POST("api/customer/langganan/order_cancel")
    suspend fun orderCancel(@Body body: OrderCancelReq): OrderResp

    // ---- Wallet / saldo ----
    @GET("api/customer/langganan/wallet")
    suspend fun wallet(): WalletResp

    @GET("api/customer/langganan/wallet_ledger")
    suspend fun walletLedger(@Query("limit") limit: Int = 50): WalletLedgerResp

    @GET("api/customer/langganan/wallet_topups")
    suspend fun walletTopups(
        @Query("status") status: String? = null,
        @Query("limit") limit: Int = 50,
    ): WalletTopupsListResp

    @POST("api/customer/langganan/wallet_topup")
    suspend fun walletTopup(@Body body: WalletTopupReq): WalletTopupResp

    @GET("api/customer/langganan/wallet_topup_status")
    suspend fun walletTopupStatus(@Query("topup_code") topupCode: String): WalletTopupStatusResp

    @POST("api/customer/langganan/wallet_topup_cancel")
    suspend fun walletTopupCancel(@Body body: WalletTopupCancelReq): WalletTopupCancelResp

    // ---- PPOB (via server Obill) ----
    @GET("api/customer/ppob/catalog")
    suspend fun ppobCatalog(
        @Query("cmd") cmd: String = "prepaid",
        @Query("category") category: String? = null,
        @Query("brand") brand: String? = null,
        @Query("type") type: String? = null,
        @Query("code") code: String? = null,
    ): PpobCatalogResp

    @GET("api/customer/ppob/sync_status")
    suspend fun ppobSyncStatus(): PpobSyncStatusResp

    @POST("api/customer/ppob/topup")
    suspend fun ppobTopup(@Body body: PpobTopupReq): PpobTransactionResp

    @POST("api/customer/ppob/inquiry")
    suspend fun ppobInquiry(@Body body: PpobInquiryReq): PpobTransactionResp

    @POST("api/customer/ppob/pay_pasca")
    suspend fun ppobPayPasca(@Body body: PpobPayPascaReq): PpobTransactionResp

    @POST("api/customer/ppob/quote")
    suspend fun ppobQuote(@Body body: PpobQuoteReq): PpobQuoteResp

    @POST("api/customer/ppob/checkout")
    suspend fun ppobCheckout(@Body body: PpobCheckoutReq): PpobCheckoutResp

    @GET("api/customer/ppob/payment_status")
    suspend fun ppobPaymentStatus(@Query("ref_id") refId: String): PpobPaymentStatusResp

    @POST("api/customer/ppob/cancel")
    suspend fun ppobCancel(@Body body: PpobCancelReq): PpobCancelResp

    @GET("api/customer/ppob/transaction")
    suspend fun ppobTransaction(@Query("ref_id") refId: String): PpobTransactionResp

    @GET("api/customer/ppob/transactions")
    suspend fun ppobTransactions(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
        @Query("status") status: String? = null,
    ): PpobTransactionsResp

    // ---- Legal (publik) ----
    @GET("api/legal")
    suspend fun legalIndex(): LegalIndexResp

    @GET("api/legal/privacy")
    suspend fun legalPrivacy(): LegalDocumentResp

    @GET("api/legal/terms")
    suspend fun legalTerms(): LegalDocumentResp
}
