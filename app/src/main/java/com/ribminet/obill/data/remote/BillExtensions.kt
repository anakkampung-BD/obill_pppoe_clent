package com.ribminet.obill.data.remote

/**
 * Resolusi nominal tagihan — **jangan** pakai [BillDto.amount] sebagai total bayar.
 * Prioritas: `amounts.total_amount` (root respons / nested) → flat fields → jumlah komponen.
 */

fun CustomerDto.activationFromFlat(): ActivationDto? {
    activation?.let { return it }
    if (isFirstActivationFlat == null && installationFeeAmountFlat == null && requiresInstallationFeeFlat == null) {
        return null
    }
    return ActivationDto(
        isFirstActivation = isFirstActivationFlat,
        requiresInstallationFee = requiresInstallationFeeFlat,
        installationFeeAmount = installationFeeAmountFlat,
        installationFeeLabel = installationFeeLabelFlat,
    )
}

fun BillDto.amountsFromFlat(): BillAmountsDto? {
    amounts?.let { return it }
    if (totalAmountFlat == null && subscriptionAmountFlat == null && installationFeeAmountFlat == null) {
        return null
    }
    return BillAmountsDto(
        subscriptionAmount = subscriptionAmountFlat,
        installationFeeAmount = installationFeeAmountFlat,
        installationFeeLabel = installationFeeLabelFlat,
        latePenaltyAmount = latePenaltyAmountFlat,
        totalAmount = totalAmountFlat,
    )
}

fun BillDto.activationFromFlat(): ActivationDto? {
    activation?.let { return it }
    if (isFirstActivationFlat == null && installationFeeAmountFlat == null && requiresInstallationFeeFlat == null) {
        return null
    }
    return ActivationDto(
        isFirstActivation = isFirstActivationFlat,
        requiresInstallationFee = requiresInstallationFeeFlat,
        installationFeeAmount = installationFeeAmountFlat,
        installationFeeLabel = installationFeeLabelFlat,
    )
}

fun ActivationInfoResp.amountsFromFlat(): BillAmountsDto? {
    amounts?.let { return it }
    if (totalAmountFlat == null && subscriptionAmountFlat == null && installationFeeAmountFlat == null) {
        return null
    }
    return BillAmountsDto(
        subscriptionAmount = subscriptionAmountFlat,
        installationFeeAmount = installationFeeAmountFlat,
        installationFeeLabel = installationFeeLabelFlat,
        latePenaltyAmount = latePenaltyAmountFlat,
        totalAmount = totalAmountFlat,
    )
}

fun ActivationInfoResp.activationResolved(): ActivationDto? =
    activation ?: run {
        if (isFirstActivationFlat == null && installationFeeAmountFlat == null && requiresInstallationFeeFlat == null) {
            null
        } else {
            ActivationDto(
                isFirstActivation = isFirstActivationFlat,
                requiresInstallationFee = requiresInstallationFeeFlat,
                installationFeeAmount = installationFeeAmountFlat,
                installationFeeLabel = installationFeeLabelFlat,
            )
        }
    }

fun BillResp.resolvedBill(customerActivation: ActivationDto? = null): BillDto? {
    val raw = bill ?: return null
    return resolvePaymentBill(
        bill = raw,
        respActivation = activation ?: customerActivation,
        respAmounts = amounts,
        customerActivation = customerActivation,
    )
}

fun resolvePaymentBill(
    bill: BillDto?,
    respActivation: ActivationDto? = null,
    respAmounts: BillAmountsDto? = null,
    customerActivation: ActivationDto? = null,
): BillDto? {
    if (bill == null) return null
    val mergedAmounts = firstAmounts(respAmounts, bill.amountsFromFlat())
    val mergedActivation = firstActivation(respActivation, bill.activationFromFlat(), customerActivation)
    return bill.copy(amounts = mergedAmounts, activation = mergedActivation)
}

fun mergeActivationInfo(bill: BillDto?, info: ActivationInfoResp?): BillDto? {
    if (bill == null && info == null) return null
    val base = bill ?: BillDto()
    return resolvePaymentBill(
        bill = base,
        respActivation = info?.activationResolved(),
        respAmounts = info?.amountsFromFlat(),
    )
}

private fun firstAmounts(vararg candidates: BillAmountsDto?): BillAmountsDto? =
    candidates.firstOrNull { it != null }

private fun firstActivation(vararg candidates: ActivationDto?): ActivationDto? =
    candidates.firstOrNull { it != null }

fun BillDto.payableTotal(): Long {
    amounts?.totalAmount?.takeIf { it > 0 }?.let { return it }
    totalAmountFlat?.takeIf { it > 0 }?.let { return it }
    val sum = subscriptionAmount() + installationFeeAmount() + latePenaltyAmount()
    if (sum > 0) return sum
    return 0L
}

/** Harga langganan periode — [BillDto.amount] hanya dipakai sebagai fallback subscription, bukan total. */
fun BillDto.subscriptionAmount(): Long =
    amounts?.subscriptionAmount?.takeIf { it > 0 }
        ?: subscriptionAmountFlat?.takeIf { it > 0 }
        ?: amount?.takeIf { it > 0 }
        ?: 0L

fun BillDto.installationFeeAmount(): Long =
    amounts?.installationFeeAmount?.takeIf { it > 0 }
        ?: installationFeeAmountFlat?.takeIf { it > 0 }
        ?: activation?.installationFeeAmount?.takeIf { it > 0 }
        ?: 0L

fun BillDto.latePenaltyAmount(): Long =
    amounts?.latePenaltyAmount?.takeIf { it > 0 }
        ?: latePenaltyAmountFlat?.takeIf { it > 0 }
        ?: 0L

fun BillDto.installationFeeLabel(): String =
    amounts?.installationFeeLabel?.takeIf { it.isNotBlank() }
        ?: installationFeeLabelFlat?.takeIf { it.isNotBlank() }
        ?: activation?.installationFeeLabel?.takeIf { it.isNotBlank() }
        ?: "Biaya instalasi"

fun BillDto.isFirstActivation(): Boolean =
    isFirstActivationFlat == true || activation?.isFirstActivation == true

fun BillDto.requiresInstallationFee(): Boolean =
    requiresInstallationFeeFlat == true
        || activation?.requiresInstallationFee == true
        || installationFeeAmount() > 0L

fun OrderDto.payableTotal(): Long {
    amounts?.totalAmount?.takeIf { it > 0 }?.let { return it }
    val sum = subscriptionAmount() + installationFeeAmount() + latePenaltyAmount()
    if (sum > 0) return sum
    return amount?.takeIf { it > 0 } ?: 0L
}

fun OrderDto.subscriptionAmount(): Long =
    amounts?.subscriptionAmount?.takeIf { it > 0 } ?: amount?.takeIf { it > 0 } ?: 0L

fun OrderDto.installationFeeAmount(): Long =
    amounts?.installationFeeAmount?.takeIf { it > 0 }
        ?: activation?.installationFeeAmount?.takeIf { it > 0 }
        ?: 0L

fun OrderDto.latePenaltyAmount(): Long =
    amounts?.latePenaltyAmount?.takeIf { it > 0 } ?: 0L

fun OrderDto.installationFeeLabel(): String =
    amounts?.installationFeeLabel?.takeIf { it.isNotBlank() }
        ?: activation?.installationFeeLabel?.takeIf { it.isNotBlank() }
        ?: "Biaya instalasi"

fun OrderDto.orderTypeDisplay(): String =
    orderTypeLabel ?: when (orderType) {
        "activation" -> "Aktivasi Pertama"
        "upgrade" -> "Upgrade Paket"
        else -> "Perpanjang Langganan"
    }
