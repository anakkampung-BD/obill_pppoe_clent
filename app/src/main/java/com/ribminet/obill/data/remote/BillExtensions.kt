package com.ribminet.obill.data.remote

/**
 * Resolusi nominal tagihan EOM — **jangan** hardcode 30 hari / pakai [BillDto.amount] sebagai harga paket.
 * Total bayar: `amounts.total_amount` (atau `billing_breakdown.amount_payable` / `bill.amount`).
 * Harga langganan penuh: `subscription_gross` / `billing_breakdown.subscription_full_price`.
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
    val resolved = resolvePaymentBill(
        bill = raw,
        respActivation = activation ?: customerActivation,
        respAmounts = amounts,
        respLatePenalty = latePenalty ?: raw.latePenalty,
        customerActivation = customerActivation,
        respBreakdown = billingBreakdown,
        respPreview = previewRenewal,
        respNextPayment = nextPayment,
    )
    return resolved
}

fun resolvePaymentBill(
    bill: BillDto?,
    respActivation: ActivationDto? = null,
    respAmounts: BillAmountsDto? = null,
    respLatePenalty: LatePenaltyDto? = null,
    customerActivation: ActivationDto? = null,
    respBreakdown: BillingBreakdownDto? = null,
    respPreview: PreviewRenewalDto? = null,
    respNextPayment: NextPaymentDto? = null,
): BillDto? {
    if (bill == null) return null
    val mergedAmounts = mergeAmounts(respAmounts, bill.amountsFromFlat(), bill.billingBreakdown)
    val mergedActivation = firstActivation(respActivation, bill.activationFromFlat(), customerActivation)
    val mergedPenalty = respLatePenalty ?: bill.latePenalty
    val withPenaltyAmounts = if (mergedPenalty?.totalPenalty != null && mergedAmounts != null) {
        mergedAmounts.copy(
            latePenaltyAmount = mergedAmounts.latePenaltyAmount ?: mergedPenalty.totalPenalty,
        )
    } else mergedAmounts
    return bill.copy(
        amounts = withPenaltyAmounts,
        activation = mergedActivation,
        latePenalty = mergedPenalty,
        billingBreakdown = respBreakdown ?: bill.billingBreakdown,
        previewRenewal = respPreview ?: bill.previewRenewal,
        nextPayment = preferNextPayment(respNextPayment, bill.nextPayment),
    )
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

private fun firstActivation(vararg candidates: ActivationDto?): ActivationDto? =
    candidates.firstOrNull { it != null }

private fun preferNextPayment(a: NextPaymentDto?, b: NextPaymentDto?): NextPaymentDto? {
    if (a == null) return b
    if (b == null) return a
    return a.copy(
        disconnectAt = a.disconnectAt ?: b.disconnectAt,
        disconnectGraceDays = a.disconnectGraceDays ?: b.disconnectGraceDays,
        dueDatetime = a.dueDatetime ?: b.dueDatetime,
        dueDate = a.dueDate ?: b.dueDate,
        daysUntilDue = a.daysUntilDue ?: b.daysUntilDue,
        isOverdue = a.isOverdue ?: b.isOverdue,
    )
}

private fun mergeAmounts(
    root: BillAmountsDto?,
    nested: BillAmountsDto?,
    breakdown: BillingBreakdownDto?,
): BillAmountsDto? {
    if (root == null && nested == null && breakdown == null) return null
    return BillAmountsDto(
        subscriptionAmount = root?.subscriptionAmount
            ?: nested?.subscriptionAmount
            ?: breakdown?.subscriptionAmount,
        subscriptionGross = root?.subscriptionGross
            ?: nested?.subscriptionGross
            ?: breakdown?.subscriptionFullPrice,
        creditApplied = root?.creditApplied
            ?: nested?.creditApplied
            ?: breakdown?.creditApplied,
        billingCreditBalance = root?.billingCreditBalance
            ?: nested?.billingCreditBalance
            ?: breakdown?.billingCreditBalance,
        installationFeeAmount = root?.installationFeeAmount ?: nested?.installationFeeAmount,
        installationFeeLabel = root?.installationFeeLabel ?: nested?.installationFeeLabel,
        latePenaltyAmount = root?.latePenaltyAmount ?: nested?.latePenaltyAmount,
        totalAmount = root?.totalAmount
            ?: nested?.totalAmount
            ?: breakdown?.amountPayable,
    )
}

/** Total yang harus dibayar. */
fun BillDto.payableTotal(): Long {
    amounts?.totalAmount?.takeIf { it >= 0 }?.let { return it }
    totalAmountFlat?.takeIf { it >= 0 }?.let { return it }
    billingBreakdown?.amountPayable?.takeIf { it >= 0 }?.let { return it }
    amount?.takeIf { it >= 0 }?.let { return it }
    val sum = subscriptionAmount() + installationFeeAmount() + latePenaltyAmount() - creditApplied()
    if (sum > 0) return sum
    return 0L
}

/**
 * Langganan setelah kredit (sebelum denda/instalasi).
 * Jangan fallback ke [BillDto.amount] — field itu sekarang = total bayar.
 */
fun BillDto.subscriptionAmount(): Long =
    amounts?.subscriptionAmount?.takeIf { it >= 0 }
        ?: subscriptionAmountFlat?.takeIf { it >= 0 }
        ?: billingBreakdown?.subscriptionAmount?.takeIf { it >= 0 }
        ?: 0L

/** Langganan sebelum kredit (prorata/full). */
fun BillDto.subscriptionGross(): Long =
    amounts?.subscriptionGross?.takeIf { it > 0 }
        ?: billingBreakdown?.subscriptionFullPrice?.takeIf { it > 0 }
        ?: subscriptionAmount()

fun BillDto.creditApplied(): Long =
    amounts?.creditApplied?.takeIf { it > 0 }
        ?: billingBreakdown?.creditApplied?.takeIf { it > 0 }
        ?: 0L

fun BillDto.creditNote(): String? =
    billingBreakdown?.billingCreditNote?.takeIf { it.isNotBlank() }

fun BillDto.periodLabel(): String? =
    billingBreakdown?.label?.takeIf { it.isNotBlank() }
        ?: billingBreakdown?.periodLabel?.takeIf { it.isNotBlank() }

fun BillDto.isProrate(): Boolean =
    billingBreakdown?.billingMode == "prorate_to_eom"

fun BillDto.installationFeeAmount(): Long =
    amounts?.installationFeeAmount?.takeIf { it > 0 }
        ?: installationFeeAmountFlat?.takeIf { it > 0 }
        ?: activation?.installationFeeAmount?.takeIf { it > 0 }
        ?: 0L

fun BillDto.latePenaltyAmount(): Long =
    amounts?.latePenaltyAmount?.takeIf { it > 0 }
        ?: latePenaltyAmountFlat?.takeIf { it > 0 }
        ?: latePenalty?.totalPenalty?.takeIf { it > 0 }
        ?: 0L

fun LatePenaltyDto?.labelForUi(): String? =
    this?.formulaLabel?.takeIf { it.isNotBlank() }
        ?: this?.description?.takeIf { it.isNotBlank() }

fun BillDto.latePenaltyLabel(): String =
    latePenalty.labelForUi() ?: "Denda keterlambatan"

fun BillDto.hasLatePenalty(): Boolean =
    latePenalty?.applies == true || latePenaltyAmount() > 0L

fun BillDto.latePenaltyDescription(): String? =
    latePenalty?.description?.takeIf { it.isNotBlank() }

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

fun BillDto.activeUntilDisplay(): String =
    formatDateTimeId(previewRenewal?.newExpiredAt ?: billingBreakdown?.newExpiredAt)

fun BillDto.dueDisplay(): String =
    formatDateTimeId(nextPayment?.dueDatetime) 
        .takeIf { it != "-" }
        ?: formatDateId(nextPayment?.dueDate)

fun BillDto.disconnectDisplay(): String =
    formatDateTimeId(nextPayment?.disconnectAt)

fun BillDto.shouldShowDisconnectHint(): Boolean {
    val np = nextPayment ?: return false
    if (np.disconnectAt.isNullOrBlank()) return false
    if (np.isOverdue == true) return true
    val grace = np.disconnectGraceDays ?: billingBreakdown?.disconnectGraceDays ?: 1
    val days = np.daysUntilDue ?: return false
    return days <= grace
}

fun OrderDto.payableTotal(): Long {
    amounts?.totalAmount?.takeIf { it >= 0 }?.let { return it }
    amountBreakdown?.totalAmount?.takeIf { it >= 0 }?.let { return it }
    billingBreakdown?.amountPayable?.takeIf { it >= 0 }?.let { return it }
    val sum = subscriptionAmount() + installationFeeAmount() + latePenaltyAmount() - creditApplied()
    if (sum > 0) return sum
    return amount?.takeIf { it >= 0 } ?: 0L
}

fun OrderDto.subscriptionAmount(): Long =
    amounts?.subscriptionAmount?.takeIf { it >= 0 }
        ?: amountBreakdown?.subscriptionAmount?.takeIf { it >= 0 }
        ?: billingBreakdown?.subscriptionAmount?.takeIf { it >= 0 }
        ?: 0L

fun OrderDto.subscriptionGross(): Long =
    amounts?.subscriptionGross?.takeIf { it > 0 }
        ?: amountBreakdown?.subscriptionGross?.takeIf { it > 0 }
        ?: billingBreakdown?.subscriptionFullPrice?.takeIf { it > 0 }
        ?: subscriptionAmount()

fun OrderDto.creditApplied(): Long =
    amounts?.creditApplied?.takeIf { it > 0 }
        ?: amountBreakdown?.creditApplied?.takeIf { it > 0 }
        ?: billingBreakdown?.creditApplied?.takeIf { it > 0 }
        ?: 0L

fun OrderDto.creditNote(): String? =
    billingBreakdown?.billingCreditNote?.takeIf { it.isNotBlank() }

fun OrderDto.periodLabel(): String? =
    billingBreakdown?.label?.takeIf { it.isNotBlank() }
        ?: billingBreakdown?.periodLabel?.takeIf { it.isNotBlank() }

fun OrderDto.installationFeeAmount(): Long =
    amounts?.installationFeeAmount?.takeIf { it > 0 }
        ?: amountBreakdown?.installationFeeAmount?.takeIf { it > 0 }
        ?: activation?.installationFeeAmount?.takeIf { it > 0 }
        ?: 0L

fun OrderDto.latePenaltyAmount(): Long =
    amounts?.latePenaltyAmount?.takeIf { it > 0 }
        ?: amountBreakdown?.latePenaltyAmount?.takeIf { it > 0 }
        ?: latePenalty?.totalPenalty?.takeIf { it > 0 }
        ?: 0L

fun OrderDto.latePenaltyLabel(): String =
    latePenalty.labelForUi() ?: "Denda keterlambatan"

fun OrderDto.resolvedAmounts(): BillAmountsDto? = amounts ?: amountBreakdown

fun OrderDto.enrichedFromPayResp(resp: BillPayResp): OrderDto = copy(
    amounts = amounts ?: resp.amounts ?: amountBreakdown,
    latePenalty = latePenalty ?: resp.latePenalty,
    billingBreakdown = billingBreakdown ?: resp.billingBreakdown,
    payment = payment ?: resp.payment,
    expiresAt = expiresAt ?: resp.payment?.expiresAt ?: resp.order?.expiresAt,
)

/** Order memakai QRIS dinamis (bukan cash/transfer/QRIS statis). */
fun OrderDto.isQrisDinamisPayment(): Boolean {
    val method = paymentMethod?.lowercase().orEmpty()
    val type = paymentInstruction?.type?.lowercase().orEmpty()
    val channel = payment?.payChannel?.lowercase().orEmpty()
    return method == "qris_dinamis" ||
        type == "qris_dinamis" ||
        channel == "qris_dinamis" ||
        (payment != null && (!payment.qrisImageUrl.isNullOrBlank() || payment.payAmount != null))
}

fun OrderDto.withMergedPayment(incoming: WalletPaymentDto?): OrderDto {
    if (incoming == null) return this
    val cur = payment
    return copy(
        payment = WalletPaymentDto(
            payChannel = incoming.payChannel ?: cur?.payChannel,
            paymentStatus = incoming.paymentStatus ?: cur?.paymentStatus,
            amountBase = incoming.amountBase ?: cur?.amountBase,
            uniqueCode = incoming.uniqueCode ?: cur?.uniqueCode,
            payAmount = incoming.payAmount ?: cur?.payAmount,
            payAmountFormatted = incoming.payAmountFormatted ?: cur?.payAmountFormatted,
            qrisString = incoming.qrisString?.takeIf { it.isNotBlank() } ?: cur?.qrisString,
            qrisImageUrl = incoming.qrisImageUrl?.takeIf { it.isNotBlank() } ?: cur?.qrisImageUrl,
            expiresAt = incoming.expiresAt ?: cur?.expiresAt,
            expireMinutes = incoming.expireMinutes ?: cur?.expireMinutes,
            merchantName = incoming.merchantName ?: cur?.merchantName,
            nmid = incoming.nmid ?: cur?.nmid,
            terminalId = incoming.terminalId ?: cur?.terminalId,
        ),
        expiresAt = expiresAt ?: incoming.expiresAt,
    )
}

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
