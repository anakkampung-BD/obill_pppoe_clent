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
        }
        return result
    }

    suspend fun logout(): ApiResult<BaseResp> {
        val result = safe { api.logout() }
        tokenStore.clear()
        return result
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

    suspend fun billPay(method: String, note: String? = null): ApiResult<BillPayResp> =
        safe { api.billPay(BillPayReq(method, note)) }

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
