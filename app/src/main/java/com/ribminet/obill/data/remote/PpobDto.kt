package com.ribminet.obill.data.remote

import com.google.gson.annotations.SerializedName

// ---- Katalog ----
data class PpobCatalogResp(
    val success: Boolean = false,
    val cmd: String? = null,
    val products: List<PpobProductDto> = emptyList(),
    @SerializedName("product_count") val productCount: Int? = null,
    @SerializedName("synced_at") val syncedAt: String? = null,
    @SerializedName("sync_date") val syncDate: String? = null,
    @SerializedName("cached_at") val cachedAt: String? = null,
    val stale: Boolean? = null,
    @SerializedName("sync_status") val syncStatus: PpobSyncStatusDetailDto? = null,
    val message: String? = null,
    val code: String? = null,
)

data class PpobProductDto(
    @SerializedName("buyer_sku_code") val buyerSkuCode: String? = null,
    @SerializedName("product_name") val productName: String? = null,
    val category: String? = null,
    val brand: String? = null,
    val type: String? = null,
    @SerializedName("product_kind") val productKind: String? = null,
    @SerializedName("product_category") val productCategory: String? = null,
    @SerializedName("product_brand") val productBrand: String? = null,
    @SerializedName("sell_price") val sellPrice: Long? = null,
    @SerializedName("has_sell_price") val hasSellPrice: Boolean? = null,
    @SerializedName("is_active") val isActive: Boolean? = null,
    @SerializedName("needs_name_check") val needsNameCheck: Boolean? = null,
    @SerializedName("name_check_type") val nameCheckType: String? = null,
    @SerializedName("name_check_fee") val nameCheckFee: Long? = null,
    val desc: String? = null,
)

data class PpobSyncStatusResp(
    val success: Boolean = false,
    @SerializedName("sync_status") val syncStatus: PpobSyncStatusDetailDto? = null,
    val message: String? = null,
    val code: String? = null,
)

data class PpobSyncStatusDetailDto(
    @SerializedName("interval_seconds") val intervalSeconds: Int? = null,
    @SerializedName("interval_minutes") val intervalMinutes: Int? = null,
    @SerializedName("last_full_sync_at") val lastFullSyncAt: String? = null,
    @SerializedName("next_sync_at") val nextSyncAt: String? = null,
    val today: String? = null,
    val prepaid: PpobSyncSliceDto? = null,
    val pasca: PpobSyncSliceDto? = null,
)

data class PpobSyncSliceDto(
    @SerializedName("synced_at") val syncedAt: String? = null,
    @SerializedName("sync_date") val syncDate: String? = null,
    @SerializedName("product_count") val productCount: Int? = null,
    val stale: Boolean? = null,
    @SerializedName("last_error") val lastError: String? = null,
)

// ---- Request transaksi ----
data class PpobTopupReq(
    @SerializedName("buyer_sku_code") val buyerSkuCode: String,
    @SerializedName("customer_no") val customerNo: String,
    @SerializedName("product_name") val productName: String? = null,
)

data class PpobInquiryReq(
    @SerializedName("buyer_sku_code") val buyerSkuCode: String,
    @SerializedName("customer_no") val customerNo: String,
    @SerializedName("product_name") val productName: String? = null,
)

data class PpobPayPascaReq(
    @SerializedName("ref_id") val refId: String,
    @SerializedName("buyer_sku_code") val buyerSkuCode: String,
    @SerializedName("customer_no") val customerNo: String,
)

data class PpobQuoteReq(
    @SerializedName("buyer_sku_code") val buyerSkuCode: String,
    @SerializedName("customer_no") val customerNo: String,
)

data class PpobCheckoutReq(
    @SerializedName("buyer_sku_code") val buyerSkuCode: String,
    @SerializedName("customer_no") val customerNo: String,
    @SerializedName("product_name") val productName: String? = null,
)

// ---- Respons transaksi ----
data class PpobTransactionResp(
    val success: Boolean = false,
    val message: String? = null,
    val code: String? = null,
    val transaction: PpobTransactionDto? = null,
    @SerializedName("ref_id") val refId: String? = null,
    val inquiry: PpobInquiryResultDto? = null,
    val payment: PpobPaymentDto? = null,
    val product: PpobProductDto? = null,
    val pricing: PpobPricingDto? = null,
    val notifications: PpobNotificationsDto? = null,
    val error: String? = null,
)

data class PpobQuoteResp(
    val success: Boolean = false,
    val message: String? = null,
    val code: String? = null,
    val product: PpobProductDto? = null,
    val pricing: PpobPricingDto? = null,
    val error: String? = null,
)

data class PpobCheckoutResp(
    val success: Boolean = false,
    val message: String? = null,
    val code: String? = null,
    @SerializedName("ref_id") val refId: String? = null,
    val payment: PpobPaymentDto? = null,
    val transaction: PpobTransactionDto? = null,
    val error: String? = null,
)

data class PpobPaymentStatusResp(
    val success: Boolean = false,
    val message: String? = null,
    val code: String? = null,
    @SerializedName("ref_id") val refId: String? = null,
    val status: String? = null,
    @SerializedName("payment_status") val paymentStatus: String? = null,
    @SerializedName("is_waiting_payment") val isWaitingPayment: Boolean? = null,
    @SerializedName("is_paid") val isPaid: Boolean? = null,
    @SerializedName("is_success") val isSuccess: Boolean? = null,
    val payment: PpobPaymentDto? = null,
    val transaction: PpobTransactionDto? = null,
    val error: String? = null,
)

data class PpobCancelReq(
    @SerializedName("ref_id") val refId: String,
)

data class PpobCancelResp(
    val success: Boolean = false,
    val message: String? = null,
    val code: String? = null,
    @SerializedName("ref_id") val refId: String? = null,
    val transaction: PpobTransactionDto? = null,
    val error: String? = null,
)

data class PpobPricingDto(
    @SerializedName("sell_price") val sellPrice: Long? = null,
    @SerializedName("name_check_fee") val nameCheckFee: Long? = null,
    @SerializedName("name_check_label") val nameCheckLabel: String? = null,
    @SerializedName("name_check_sku") val nameCheckSku: String? = null,
    @SerializedName("base_before_unique") val baseBeforeUnique: Long? = null,
    @SerializedName("unique_code_note") val uniqueCodeNote: String? = null,
)

data class PpobPaymentDto(
    @SerializedName("pay_channel") val payChannel: String? = null,
    @SerializedName("payment_status") val paymentStatus: String? = null,
    @SerializedName("pay_amount") val payAmount: Long? = null,
    val breakdown: PpobPaymentBreakdownDto? = null,
    @SerializedName("qris_string") val qrisString: String? = null,
    @SerializedName("qris_image_url") val qrisImageUrl: String? = null,
    @SerializedName("expire_minutes") val expireMinutes: Int? = null,
    @SerializedName("merchant_name") val merchantName: String? = null,
    @SerializedName("nmid") val nmid: String? = null,
    @SerializedName("terminal_id") val terminalId: String? = null,
    val note: String? = null,
)

data class PpobPaymentBreakdownDto(
    @SerializedName("sell_price") val sellPrice: Long? = null,
    @SerializedName("name_check_fee") val nameCheckFee: Long? = null,
    @SerializedName("name_check_label") val nameCheckLabel: String? = null,
    @SerializedName("base_before_unique") val baseBeforeUnique: Long? = null,
    @SerializedName("unique_code") val uniqueCode: Int? = null,
    @SerializedName("unique_code_str") val uniqueCodeStr: String? = null,
    @SerializedName("pay_amount") val payAmount: Long? = null,
    @SerializedName("breakdown_line") val breakdownLine: String? = null,
)

data class PpobTransactionsResp(
    val success: Boolean = false,
    val transactions: List<PpobTransactionDto> = emptyList(),
    val count: Int? = null,
    val limit: Int? = null,
    val offset: Int? = null,
    val message: String? = null,
    val code: String? = null,
)

data class PpobTransactionDto(
    @SerializedName("ref_id") val refId: String? = null,
    @SerializedName("trx_type") val trxType: String? = null,
    @SerializedName("buyer_sku_code") val buyerSkuCode: String? = null,
    @SerializedName("product_name") val productName: String? = null,
    @SerializedName("product_kind") val productKind: String? = null,
    @SerializedName("product_category") val productCategory: String? = null,
    @SerializedName("product_brand") val productBrand: String? = null,
    @SerializedName("customer_no") val customerNo: String? = null,
    @SerializedName("customer_name") val customerName: String? = null,
    val status: String? = null,
    @SerializedName("is_success") val isSuccess: Boolean? = null,
    @SerializedName("payment_status") val paymentStatus: String? = null,
    @SerializedName("is_paid") val isPaid: Boolean? = null,
    @SerializedName("is_waiting_payment") val isWaitingPayment: Boolean? = null,
    @SerializedName("is_cancelled") val isCancelled: Boolean? = null,
    @SerializedName("is_expired") val isExpired: Boolean? = null,
    @SerializedName("pay_channel") val payChannel: String? = null,
    @SerializedName("pay_amount") val payAmount: Long? = null,
    @SerializedName("display_amount") val displayAmount: Long? = null,
    @SerializedName("payment_breakdown") val paymentBreakdown: PpobPaymentBreakdownDto? = null,
    @SerializedName("base_sell_price") val baseSellPrice: Long? = null,
    @SerializedName("name_check_fee") val nameCheckFee: Long? = null,
    @SerializedName("payment_unique_code") val paymentUniqueCode: Int? = null,
    val rc: String? = null,
    val message: String? = null,
    val amount: Long? = null,
    @SerializedName("sell_price") val sellPrice: Long? = null,
    val sn: String? = null,
    val token: String? = null,
    @SerializedName("wa_notified") val waNotified: Boolean? = null,
    @SerializedName("push_notified") val pushNotified: Boolean? = null,
    val source: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
)

data class PpobInquiryResultDto(
    @SerializedName("customer_name") val customerName: String? = null,
    val price: Long? = null,
    val desc: String? = null,
    val period: String? = null,
)

data class PpobNotificationsDto(
    val whatsapp: PpobChannelNotifyDto? = null,
    val push: PpobChannelNotifyDto? = null,
)

data class PpobChannelNotifyDto(
    val sent: Boolean? = null,
    val message: String? = null,
    @SerializedName("notif_id") val notifId: Int? = null,
)
