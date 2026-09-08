package com.ribminet.obill.data.remote

import com.google.gson.annotations.SerializedName

// ---- Wallet / saldo pelanggan ----

data class WalletResp(
    val success: Boolean = false,
    val wallet: WalletInfoDto? = null,
    @SerializedName("qris_enabled") val qrisEnabled: Boolean? = null,
    @SerializedName("pending_topups") val pendingTopups: List<WalletTopupDto>? = null,
    @SerializedName("pending_count") val pendingCount: Int? = null,
    val message: String? = null,
    val error: String? = null,
    val code: String? = null,
)

data class WalletInfoDto(
    val balance: Long? = null,
    @SerializedName("balance_formatted") val balanceFormatted: String? = null,
    val currency: String? = null,
    @SerializedName("auto_renew") val autoRenew: Boolean? = null,
    @SerializedName("auto_renew_note") val autoRenewNote: String? = null,
    @SerializedName("min_topup") val minTopup: Long? = null,
)

data class WalletTopupReq(
    val amount: Long,
)

data class WalletTopupResp(
    val success: Boolean = false,
    val message: String? = null,
    @SerializedName("topup_code") val topupCode: String? = null,
    val payment: WalletPaymentDto? = null,
    val topup: WalletTopupDto? = null,
    val error: String? = null,
    val code: String? = null,
)

data class WalletPaymentDto(
    @SerializedName("pay_channel") val payChannel: String? = null,
    @SerializedName("payment_status") val paymentStatus: String? = null,
    @SerializedName("amount_base") val amountBase: Long? = null,
    @SerializedName("unique_code") val uniqueCode: Int? = null,
    @SerializedName("pay_amount") val payAmount: Long? = null,
    @SerializedName("pay_amount_formatted") val payAmountFormatted: String? = null,
    @SerializedName("qris_string") val qrisString: String? = null,
    @SerializedName("qris_image_url") val qrisImageUrl: String? = null,
    @SerializedName("expires_at") val expiresAt: String? = null,
    @SerializedName("expire_minutes") val expireMinutes: Int? = null,
    @SerializedName("merchant_name") val merchantName: String? = null,
    val nmid: String? = null,
    @SerializedName("terminal_id") val terminalId: String? = null,
)

data class WalletTopupDto(
    val id: Int? = null,
    @SerializedName("topup_code") val topupCode: String? = null,
    @SerializedName("payment_status") val paymentStatus: String? = null,
    @SerializedName("amount_base") val amountBase: Long? = null,
    @SerializedName("amount_pay") val amountPay: Long? = null,
    @SerializedName("unique_code") val uniqueCode: Int? = null,
    @SerializedName("pay_amount") val payAmount: Long? = null,
    @SerializedName("qris_image_url") val qrisImageUrl: String? = null,
    @SerializedName("qris_string") val qrisString: String? = null,
    @SerializedName("expires_at") val expiresAt: String? = null,
    @SerializedName("expire_minutes") val expireMinutes: Int? = null,
    @SerializedName("merchant_name") val merchantName: String? = null,
    @SerializedName("paid_at") val paidAt: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
)

data class WalletTopupStatusResp(
    val success: Boolean = false,
    @SerializedName("payment_status") val paymentStatus: String? = null,
    val paid: Boolean? = null,
    val balance: Long? = null,
    val topup: WalletTopupDto? = null,
    val payment: WalletPaymentDto? = null,
    val message: String? = null,
    val error: String? = null,
    val code: String? = null,
)

data class WalletTopupCancelReq(
    @SerializedName("topup_code") val topupCode: String,
)

data class WalletTopupCancelResp(
    val success: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val code: String? = null,
)

data class WalletLedgerResp(
    val success: Boolean = false,
    val balance: Long? = null,
    val count: Int? = null,
    val ledger: List<WalletLedgerEntryDto>? = null,
    val message: String? = null,
)

data class WalletLedgerEntryDto(
    val id: Int? = null,
    @SerializedName("entry_type") val entryType: String? = null,
    val amount: Long? = null,
    @SerializedName("balance_after") val balanceAfter: Long? = null,
    val source: String? = null,
    val reference: String? = null,
    val note: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
)

data class WalletTopupsListResp(
    val success: Boolean = false,
    val count: Int? = null,
    val topups: List<WalletTopupDto>? = null,
    val message: String? = null,
)

fun WalletTopupDto.toPaymentDto(): WalletPaymentDto = WalletPaymentDto(
    paymentStatus = paymentStatus,
    amountBase = amountBase,
    uniqueCode = uniqueCode,
    payAmount = amountPay ?: payAmount,
    qrisString = qrisString,
    qrisImageUrl = qrisImageUrl,
    expiresAt = expiresAt,
    expireMinutes = expireMinutes,
    merchantName = merchantName,
)

fun WalletTopupDto.isUnpaid(): Boolean =
    paymentStatus.equals("unpaid", ignoreCase = true)
