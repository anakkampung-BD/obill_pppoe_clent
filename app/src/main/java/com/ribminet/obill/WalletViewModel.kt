package com.ribminet.obill

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ribminet.obill.data.local.WalletTopUpPrefs
import com.ribminet.obill.data.remote.ApiResult
import com.ribminet.obill.data.remote.WalletPaymentDto
import com.ribminet.obill.data.remote.WalletTopupDto
import com.ribminet.obill.data.remote.isUnpaid
import com.ribminet.obill.data.remote.toPaymentDto
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

enum class WalletTopUpStep { AMOUNT, PAYMENT, SUCCESS }

class WalletViewModel : ViewModel() {
    private val repo = ObillApp.instance.repository
    private val paymentPrefs = WalletTopUpPrefs(ObillApp.instance)

    var step by mutableStateOf(WalletTopUpStep.AMOUNT)
        private set
    var balance by mutableStateOf(0L)
        private set
    var balanceFormatted by mutableStateOf<String?>(null)
        private set
    var minTopup by mutableStateOf(1_000L)
        private set
    var autoRenewNote by mutableStateOf<String?>(null)
        private set
    var qrisEnabled by mutableStateOf(true)
        private set

    var amountText by mutableStateOf("")
    var selectedPreset by mutableStateOf<Long?>(null)

    var submitting by mutableStateOf(false)
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
    var info by mutableStateOf<String?>(null)

    var topupCode by mutableStateOf<String?>(null)
        private set
    var payment by mutableStateOf<WalletPaymentDto?>(null)
        private set
    var lastTopup by mutableStateOf<WalletTopupDto?>(null)
        private set
    var paymentExpiresAtMs by mutableStateOf<Long?>(null)
        private set
    var isWaitingPayment by mutableStateOf(false)
        private set
    var isPaid by mutableStateOf(false)
        private set

    /** Dipanggil UI agar AppViewModel refresh saldo beranda. */
    var onBalanceUpdated: ((Long) -> Unit)? = null

    private var pollJob: Job? = null

    val presets: List<Long> = listOf(10_000L, 25_000L, 50_000L, 100_000L, 200_000L, 500_000L)

    fun resolvedAmount(): Long? {
        selectedPreset?.let { return it }
        val raw = amountText.filter { it.isDigit() }
        return raw.toLongOrNull()?.takeIf { it > 0L }
    }

    fun selectPreset(amount: Long) {
        selectedPreset = amount
        amountText = amount.toString()
        error = null
    }

    fun onAmountTextChange(text: String) {
        amountText = text.filter { it.isDigit() }.take(9)
        selectedPreset = amountText.toLongOrNull()?.takeIf { it in presets }
        error = null
    }

    fun loadAndResume() {
        viewModelScope.launch {
            loading = true
            error = null
            when (val r = repo.wallet()) {
                is ApiResult.Ok -> {
                    if (r.data.success) {
                        applyWallet(r.data.wallet?.balance, r.data.wallet?.balanceFormatted)
                        minTopup = r.data.wallet?.minTopup?.takeIf { it > 0 } ?: 1_000L
                        autoRenewNote = r.data.wallet?.autoRenewNote
                        qrisEnabled = r.data.qrisEnabled != false
                        val pending = r.data.pendingTopups.orEmpty().firstOrNull { it.isUnpaid() }
                        if (pending != null) {
                            resumePending(pending)
                        } else {
                            paymentPrefs.cachedTopupCode()?.let { code ->
                                checkStatusOnce(code, resumeIfUnpaid = true)
                            }
                        }
                    } else {
                        error = r.data.message ?: r.data.error ?: "Gagal memuat saldo"
                    }
                }
                is ApiResult.Err -> error = r.message
            }
            loading = false
        }
    }

    private fun applyWallet(bal: Long?, formatted: String?) {
        if (bal != null) {
            balance = bal
            balanceFormatted = formatted
            onBalanceUpdated?.invoke(bal)
        }
    }

    private fun resumePending(topup: WalletTopupDto) {
        val code = topup.topupCode ?: return
        val cachedUrl = paymentPrefs.qrisImageUrl(code)
        val pay = topup.toPaymentDto().copy(
            qrisImageUrl = topup.qrisImageUrl ?: cachedUrl,
            qrisString = topup.qrisString ?: paymentPrefs.qrisString(code),
            payAmount = topup.amountPay ?: topup.payAmount ?: paymentPrefs.payAmount(code),
            amountBase = topup.amountBase ?: paymentPrefs.amountBase(code),
            merchantName = topup.merchantName ?: paymentPrefs.merchantName(code),
            expireMinutes = topup.expireMinutes ?: paymentPrefs.expireMinutes(code),
        )
        topupCode = code
        payment = pay
        lastTopup = topup
        isWaitingPayment = true
        isPaid = false
        paymentExpiresAtMs = resolveDeadline(pay.expiresAt, pay.expireMinutes, code)
        paymentPrefs.save(
            topupCode = code,
            qrisImageUrl = pay.qrisImageUrl,
            qrisString = pay.qrisString,
            payAmount = pay.payAmount,
            amountBase = pay.amountBase,
            expireMinutes = pay.expireMinutes,
            expiresAtMs = paymentExpiresAtMs,
            merchantName = pay.merchantName,
        )
        step = WalletTopUpStep.PAYMENT
        startPoll(code)
    }

    fun submitTopup() {
        if (submitting) return
        if (!qrisEnabled) {
            error = "QRIS top-up sedang tidak tersedia."
            return
        }
        val amount = resolvedAmount()
        if (amount == null) {
            error = "Masukkan nominal top-up"
            return
        }
        if (amount < minTopup) {
            error = "Minimal top-up ${formatRp(minTopup)}"
            return
        }
        viewModelScope.launch {
            submitting = true
            error = null
            info = null
            // Cek pending dulu
            when (val w = repo.wallet()) {
                is ApiResult.Ok -> {
                    val pending = w.data.pendingTopups.orEmpty().firstOrNull { it.isUnpaid() }
                    if (pending != null) {
                        submitting = false
                        info = "Ada top-up yang belum dibayar. Melanjutkan pembayaran…"
                        resumePending(pending)
                        return@launch
                    }
                }
                is ApiResult.Err -> Unit
            }
            when (val r = repo.walletTopup(amount)) {
                is ApiResult.Ok -> {
                    if (r.data.success && r.data.payment != null) {
                        val code = r.data.topupCode ?: r.data.topup?.topupCode
                        if (code.isNullOrBlank()) {
                            error = "Top-up dibuat tetapi topup_code kosong."
                        } else {
                            val pay = r.data.payment
                            val deadline = resolveDeadline(pay.expiresAt, pay.expireMinutes, code)
                            paymentPrefs.save(
                                topupCode = code,
                                qrisImageUrl = pay.qrisImageUrl,
                                qrisString = pay.qrisString,
                                payAmount = pay.payAmount,
                                amountBase = pay.amountBase,
                                expireMinutes = pay.expireMinutes,
                                expiresAtMs = deadline,
                                merchantName = pay.merchantName,
                            )
                            topupCode = code
                            payment = pay
                            lastTopup = r.data.topup
                            isWaitingPayment = true
                            isPaid = false
                            paymentExpiresAtMs = deadline
                            info = r.data.message
                            step = WalletTopUpStep.PAYMENT
                            startPoll(code)
                        }
                    } else {
                        error = mapWalletError(r.data.message, r.data.error, r.data.code)
                    }
                }
                is ApiResult.Err -> error = mapWalletError(r.message, null, r.code)
            }
            submitting = false
        }
    }

    private fun startPoll(code: String) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(4_000)
                when (val r = repo.walletTopupStatus(code)) {
                    is ApiResult.Ok -> {
                        if (!r.data.success) continue
                        mergeFromStatus(r.data.paymentStatus, r.data.paid, r.data.balance, r.data.topup, r.data.payment)
                        val status = r.data.paymentStatus?.lowercase()
                        when {
                            r.data.paid == true || status == "paid" -> {
                                paymentPrefs.clear(code)
                                isPaid = true
                                isWaitingPayment = false
                                step = WalletTopUpStep.SUCCESS
                                return@launch
                            }
                            status == "expired" || status == "cancelled" -> {
                                paymentPrefs.clear(code)
                                isWaitingPayment = false
                                error = if (status == "expired") {
                                    "Pembayaran kedaluwarsa. Silakan buat top-up baru."
                                } else {
                                    "Top-up dibatalkan."
                                }
                                resetToAmount()
                                return@launch
                            }
                        }
                    }
                    is ApiResult.Err -> Unit
                }
            }
        }
    }

    private suspend fun checkStatusOnce(code: String, resumeIfUnpaid: Boolean) {
        when (val r = repo.walletTopupStatus(code)) {
            is ApiResult.Ok -> {
                if (!r.data.success) return
                val status = r.data.paymentStatus?.lowercase()
                when {
                    r.data.paid == true || status == "paid" -> {
                        paymentPrefs.clear(code)
                        applyWallet(r.data.balance, null)
                    }
                    status == "unpaid" && resumeIfUnpaid -> {
                        val topup = r.data.topup ?: WalletTopupDto(
                            topupCode = code,
                            paymentStatus = "unpaid",
                            qrisImageUrl = paymentPrefs.qrisImageUrl(code),
                            qrisString = paymentPrefs.qrisString(code),
                            amountPay = paymentPrefs.payAmount(code),
                            amountBase = paymentPrefs.amountBase(code),
                            expireMinutes = paymentPrefs.expireMinutes(code),
                            merchantName = paymentPrefs.merchantName(code),
                        )
                        resumePending(topup)
                    }
                    status == "expired" || status == "cancelled" -> paymentPrefs.clear(code)
                }
            }
            is ApiResult.Err -> Unit
        }
    }

    private fun mergeFromStatus(
        status: String?,
        paid: Boolean?,
        bal: Long?,
        topup: WalletTopupDto?,
        pay: WalletPaymentDto?,
    ) {
        if (paid == true) isPaid = true
        if (status != null) {
            isWaitingPayment = status.equals("unpaid", true)
        }
        applyWallet(bal, null)
        if (topup != null) lastTopup = topup
        if (pay != null) {
            val code = topupCode
            payment = payment?.copy(
                paymentStatus = pay.paymentStatus ?: payment?.paymentStatus,
                payAmount = pay.payAmount ?: payment?.payAmount,
                amountBase = pay.amountBase ?: payment?.amountBase,
                uniqueCode = pay.uniqueCode ?: payment?.uniqueCode,
                qrisImageUrl = pay.qrisImageUrl
                    ?: payment?.qrisImageUrl
                    ?: code?.let { paymentPrefs.qrisImageUrl(it) },
                qrisString = pay.qrisString
                    ?: payment?.qrisString
                    ?: code?.let { paymentPrefs.qrisString(it) },
                merchantName = pay.merchantName ?: payment?.merchantName,
                expiresAt = pay.expiresAt ?: payment?.expiresAt,
                expireMinutes = pay.expireMinutes ?: payment?.expireMinutes,
            ) ?: pay
        }
    }

    fun cancelTopup() {
        val code = topupCode ?: return
        if (submitting) return
        viewModelScope.launch {
            submitting = true
            error = null
            when (val r = repo.walletTopupCancel(code)) {
                is ApiResult.Ok -> {
                    if (r.data.success || r.data.code.equals("ALREADY_CANCELLED", true)) {
                        pollJob?.cancel()
                        paymentPrefs.clear(code)
                        info = r.data.message ?: "Top-up dibatalkan"
                        resetToAmount()
                    } else if (r.data.code.equals("ALREADY_PAID", true)) {
                        paymentPrefs.clear(code)
                        isPaid = true
                        isWaitingPayment = false
                        step = WalletTopUpStep.SUCCESS
                        loadAndResume()
                    } else {
                        error = r.data.message ?: r.data.error ?: "Gagal membatalkan top-up"
                    }
                }
                is ApiResult.Err -> error = r.message
            }
            submitting = false
        }
    }

    fun resetToAmount() {
        pollJob?.cancel()
        step = WalletTopUpStep.AMOUNT
        topupCode = null
        payment = null
        lastTopup = null
        isWaitingPayment = false
        isPaid = false
        paymentExpiresAtMs = null
        submitting = false
    }

    fun done() {
        resetToAmount()
        amountText = ""
        selectedPreset = null
        info = null
        error = null
        loadAndResume()
    }

    private fun resolveDeadline(expiresAt: String?, expireMinutes: Int?, code: String): Long {
        val cached = paymentPrefs.expiresAtMs(code)
        if (cached != null && cached > System.currentTimeMillis()) return cached
        parseDateTimeMs(expiresAt)?.let { return it }
        val mins = expireMinutes?.takeIf { it > 0 } ?: 15
        return System.currentTimeMillis() + mins * 60_000L
    }

    private fun parseDateTimeMs(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        val patterns = listOf(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
        )
        for (p in patterns) {
            try {
                val fmt = SimpleDateFormat(p, Locale.US)
                fmt.isLenient = false
                return fmt.parse(raw)?.time
            } catch (_: Exception) {
            }
        }
        return null
    }

    private fun mapWalletError(message: String?, error: String?, code: String?): String {
        return when (code?.uppercase()) {
            "AMOUNT_TOO_LOW" -> message ?: "Nominal top-up di bawah minimal."
            "QRIS_UNAVAILABLE" -> message ?: "QRIS sedang tidak tersedia. Coba lagi nanti."
            else -> message ?: error ?: "Terjadi kesalahan. Coba lagi."
        }
    }

    private fun formatRp(amount: Long): String {
        val s = "%,d".format(Locale("id", "ID"), amount).replace(',', '.')
        return "Rp $s"
    }

    override fun onCleared() {
        pollJob?.cancel()
        super.onCleared()
    }
}
