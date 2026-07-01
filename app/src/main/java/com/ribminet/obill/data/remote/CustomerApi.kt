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
    suspend fun latePenalty(): LatePenaltyResp

    @POST("api/customer/langganan/bill_pay")
    suspend fun billPay(@Body body: BillPayReq): BillPayResp

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
}
