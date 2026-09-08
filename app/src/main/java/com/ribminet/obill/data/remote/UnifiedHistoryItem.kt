package com.ribminet.obill.data.remote

/**
 * Item gabungan untuk layar Riwayat Pesanan:
 * pesanan PPPoE (bayar/upgrade) + transaksi PPOB.
 */
enum class HistoryOrderSource { PPPOE, PPOB }

data class UnifiedHistoryItem(
    val key: String,
    val source: HistoryOrderSource,
    val title: String,
    val subtitle: String,
    val amount: Long?,
    val status: String?,
    val statusLabel: String?,
    val createdAt: String?,
    val order: OrderDto? = null,
    val ppobRefId: String? = null,
)

fun OrderDto.toUnifiedHistoryItem(): UnifiedHistoryItem = UnifiedHistoryItem(
    key = "pppoe-${id ?: orderNo ?: hashCode()}",
    source = HistoryOrderSource.PPPOE,
    title = orderNo ?: "Pesanan #${id ?: "-"}",
    subtitle = orderTypeDisplay(),
    amount = payableTotal(),
    status = status,
    statusLabel = statusLabel,
    createdAt = createdAt,
    order = this,
)

fun PpobTransactionDto.toUnifiedHistoryItem(): UnifiedHistoryItem = UnifiedHistoryItem(
    key = "ppob-${refId ?: hashCode()}",
    source = HistoryOrderSource.PPOB,
    title = productName?.takeIf { it.isNotBlank() } ?: buyerSkuCode ?: (refId ?: "PPOB"),
    subtitle = listOfNotNull(
        "PPOB",
        productBrand?.takeIf { it.isNotBlank() } ?: productCategory?.takeIf { it.isNotBlank() },
        customerNo?.takeIf { it.isNotBlank() }?.let { "Tujuan $it" },
    ).joinToString(" · "),
    amount = resolvedPayAmount() ?: sellPrice,
    status = paymentStatus ?: status,
    statusLabel = ppobHistoryStatusLabel(),
    createdAt = createdAt,
    ppobRefId = refId,
)

fun PpobTransactionDto.ppobHistoryStatusLabel(): String = when {
    isSuccess == true || status.equals("Sukses", true) -> "Berhasil"
    isCancelled == true || status?.contains("Dibatalkan", true) == true -> "Dibatalkan"
    isExpired == true || paymentStatus.equals("expired", true) -> "Kedaluwarsa"
    isWaitingPayment == true || paymentStatus.equals("unpaid", true) ||
        status.equals("WaitingPayment", true) -> "Menunggu Pembayaran"
    isPaid == true || status.equals("Pending", true) -> "Diproses"
    status?.contains("Gagal", true) == true -> "Gagal"
    else -> statusLabel()
}
