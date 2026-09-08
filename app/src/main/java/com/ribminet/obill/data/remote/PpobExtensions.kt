package com.ribminet.obill.data.remote

import com.ribminet.obill.util.rupiah

fun PpobProductDto.displayName(): String =
    productName?.takeIf { it.isNotBlank() } ?: buyerSkuCode ?: "-"

fun PpobProductDto.displayPrice(): String =
    sellPrice?.let { rupiah(it) } ?: "—"

fun PpobProductDto.isAvailable(): Boolean =
    isActive != false && hasSellPrice == true && (sellPrice ?: 0L) > 0L

fun PpobProductDto.resolvedCategory(): String =
    productCategory?.takeIf { it.isNotBlank() }
        ?: category?.takeIf { it.isNotBlank() }
        ?: "Lainnya"

fun PpobProductDto.resolvedBrand(): String =
    productBrand?.takeIf { it.isNotBlank() }
        ?: brand?.takeIf { it.isNotBlank() }
        ?: ""

fun PpobProductDto.resolvedKind(): String =
    productKind?.takeIf { it.isNotBlank() }?.lowercase()
        ?: category?.takeIf { it.isNotBlank() }?.lowercase()
        ?: ""

/** DANA / PLN / produk lain yang butuh cek nomor sebelum checkout. */
fun PpobProductDto.requiresNumberCheck(): Boolean =
    needsNameCheck == true ||
        resolvedKind() == "pln" ||
        nameCheckType.equals("dana", ignoreCase = true) ||
        nameCheckType.equals("pln", ignoreCase = true) ||
        brand.equals("DANA", ignoreCase = true)

fun PpobProductDto.customerNoLabel(prepaid: Boolean): String = when {
    resolvedKind() == "pln" || nameCheckType.equals("pln", true) -> "ID meter / nomor pelanggan PLN"
    resolvedKind() in setOf("pulsa", "data") -> "Nomor HP"
    resolvedKind() == "emoney" || nameCheckType.equals("dana", true) -> "Nomor e-wallet / DANA"
    resolvedKind() in setOf("game", "games") -> "ID pemain / tujuan"
    !prepaid -> "ID pelanggan / nomor tagihan"
    else -> "Nomor tujuan"
}

fun PpobProductDto.customerNoHint(prepaid: Boolean): String = when {
    resolvedKind() == "pln" || nameCheckType.equals("pln", true) -> "Contoh: 01428800700"
    resolvedKind() in setOf("pulsa", "data") -> "08xxxxxxxxxx"
    resolvedKind() == "emoney" || nameCheckType.equals("dana", true) -> "08xxxxxxxxxx"
    !prepaid -> "Nomor ID pelanggan sesuai tagihan"
    else -> "Nomor tujuan transaksi"
}

fun PpobTransactionDto.displayToken(): String? =
    token?.takeIf { it.isNotBlank() } ?: sn?.takeIf { it.isNotBlank() }

fun PpobTransactionDto.isPln(): Boolean =
    productKind.equals("pln", ignoreCase = true)

fun PpobTransactionDto.isPending(): Boolean {
    if (isSuccess == true) return false
    val s = status?.trim().orEmpty()
    if (s.equals("Sukses", ignoreCase = true)) return false
    if (s.equals("Gagal", ignoreCase = true)) return false
    if (s.equals("Inquiry", ignoreCase = true)) return false
    return true
}

/** Order QRIS yang masih menunggu bayar (blok transaksi baru). */
fun PpobTransactionDto.isActiveUnpaid(): Boolean {
    if (isCancelled == true || isExpired == true) return false
    if (isPaid == true || isSuccess == true) return false
    if (paymentStatus.equals("cancelled", true) || paymentStatus.equals("expired", true)) return false
    if (paymentStatus.equals("paid", true)) return false
    if (status?.contains("Dibatalkan", true) == true) return false
    if (status?.contains("Gagal", true) == true) return false
    if (isWaitingPayment == true) return true
    if (paymentStatus.equals("unpaid", true)) return true
    if (status.equals("WaitingPayment", true)) return true
    return false
}

fun PpobTransactionDto.resolvedPayAmount(): Long? =
    payAmount ?: displayAmount ?: amount

fun PpobTransactionDto.toPaymentDto(
    cachedQrisImageUrl: String? = null,
    cachedQrisString: String? = null,
    expireMinutes: Int? = null,
): PpobPaymentDto = PpobPaymentDto(
    payChannel = payChannel,
    paymentStatus = paymentStatus,
    payAmount = resolvedPayAmount(),
    breakdown = resolvedPaymentBreakdown(),
    qrisString = cachedQrisString,
    qrisImageUrl = cachedQrisImageUrl,
    expireMinutes = expireMinutes,
)

/** Breakdown lengkap: harga jual + fee cek nama + kode unik (+ total pay_amount). */
fun PpobTransactionDto.resolvedPaymentBreakdown(): PpobPaymentBreakdownDto? {
    val existing = paymentBreakdown
    val sell = existing?.sellPrice ?: baseSellPrice ?: sellPrice
    val fee = existing?.nameCheckFee ?: nameCheckFee ?: 0L
    val pay = existing?.payAmount ?: resolvedPayAmount()
    val unique = existing?.uniqueCode
        ?: existing?.uniqueCodeStr?.toIntOrNull()
        ?: paymentUniqueCode
        ?: run {
            if (sell != null && pay != null) {
                val u = pay - sell - fee
                if (u in 0L..999L) u.toInt() else null
            } else null
        }
    if (sell == null && fee == 0L && unique == null && pay == null) return existing
    return PpobPaymentBreakdownDto(
        sellPrice = sell,
        nameCheckFee = fee.takeIf { it > 0L } ?: existing?.nameCheckFee,
        nameCheckLabel = existing?.nameCheckLabel?.takeIf { it.isNotBlank() }
            ?: if ((fee) > 0L) "Cek nama" else null,
        baseBeforeUnique = existing?.baseBeforeUnique
            ?: listOfNotNull(sell, fee.takeIf { it > 0L }).sum().takeIf { sell != null },
        uniqueCode = unique,
        uniqueCodeStr = existing?.uniqueCodeStr
            ?: unique?.toString()?.padStart(3, '0'),
        payAmount = pay,
        breakdownLine = existing?.breakdownLine,
    )
}

fun PpobPaymentDto.resolvedBreakdown(): PpobPaymentBreakdownDto? {
    val existing = breakdown ?: return null
    val sell = existing.sellPrice
    val fee = existing.nameCheckFee ?: 0L
    val pay = existing.payAmount ?: payAmount
    val unique = existing.uniqueCode
        ?: existing.uniqueCodeStr?.toIntOrNull()
        ?: run {
            if (sell != null && pay != null) {
                val u = pay - sell - fee
                if (u in 0L..999L) u.toInt() else null
            } else null
        }
    if (unique == null || unique == existing.uniqueCode) {
        return if (existing.payAmount == null && pay != null) existing.copy(payAmount = pay) else existing
    }
    return existing.copy(
        uniqueCode = unique,
        uniqueCodeStr = existing.uniqueCodeStr ?: unique.toString().padStart(3, '0'),
        payAmount = pay,
        baseBeforeUnique = existing.baseBeforeUnique
            ?: listOfNotNull(sell, fee.takeIf { it > 0L }).sum().takeIf { sell != null },
    )
}

fun PpobTransactionDto.statusLabel(): String =
    status ?: if (isSuccess == true) "Sukses" else "Diproses"

fun List<PpobProductDto>.availableOnly(): List<PpobProductDto> =
    filter { it.isAvailable() }

/** Urut brand A–Z, dalam brand urut harga naik. */
fun List<PpobProductDto>.sortedGroupedByBrand(): List<PpobProductDto> =
    sortedWith(
        compareBy<PpobProductDto> { it.resolvedBrand().lowercase().ifBlank { "zzz" } }
            .thenBy { it.sellPrice ?: Long.MAX_VALUE }
            .thenBy { it.productName.orEmpty() },
    )

fun PpobPaymentStatusResp.isFailedStatus(): Boolean =
    status?.contains("Gagal", ignoreCase = true) == true ||
        transaction?.status?.contains("Gagal", ignoreCase = true) == true

fun PpobPaymentStatusResp.looksLikeInvalidNumber(): Boolean {
    val msg = listOfNotNull(message, error, transaction?.message)
        .joinToString(" ")
        .lowercase()
    return listOf(
        "tidak valid",
        "invalid",
        "tidak ditemukan",
        "not found",
        "cek nama",
        "nomor salah",
        "customer not found",
        "akun tidak",
    ).any { it in msg }
}
