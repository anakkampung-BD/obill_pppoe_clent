package com.ribminet.obill

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ribminet.obill.data.local.PpobPaymentPrefs
import com.ribminet.obill.data.remote.ApiResult
import com.ribminet.obill.AlertType
import com.ribminet.obill.AppAlert
import com.ribminet.obill.data.remote.PpobInquiryResultDto
import com.ribminet.obill.data.remote.PpobPaymentDto
import com.ribminet.obill.data.remote.PpobPricingDto
import com.ribminet.obill.data.remote.PpobProductDto
import com.ribminet.obill.data.remote.PpobTransactionDto
import com.ribminet.obill.data.remote.availableOnly
import com.ribminet.obill.data.remote.customerNoHint
import com.ribminet.obill.data.remote.customerNoLabel
import com.ribminet.obill.data.remote.isActiveUnpaid
import com.ribminet.obill.data.remote.isFailedStatus
import com.ribminet.obill.data.remote.looksLikeInvalidNumber
import com.ribminet.obill.data.remote.requiresNumberCheck
import com.ribminet.obill.data.remote.resolvedCategory
import com.ribminet.obill.data.remote.sortedGroupedByBrand
import com.ribminet.obill.data.remote.toPaymentDto
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class PpobTab { PREPAID, POSTPAID }

/**
 * Wizard PPOB + QRIS:
 * Jenis → Kategori → Produk → Input nomor → Bayar sekarang (QRIS) → Sukses
 * Cek nomor (DANA/PLN) dikerjakan backend setelah bayar; hasil tampil di halaman sukses.
 */
enum class PpobStep {
    TYPE,
    CATEGORY,
    PRODUCT,
    INPUT,
    PAYMENT,
    SUCCESS,
}

class PpobViewModel : ViewModel() {

    private val repo = ObillApp.instance.repository
    private val paymentPrefs = PpobPaymentPrefs(ObillApp.instance)
    private var paymentPollJob: Job? = null
    private var detailPollJob: Job? = null
    private var unpaidCheckJob: Job? = null

    var tab by mutableStateOf(PpobTab.PREPAID)
    var step by mutableStateOf(PpobStep.TYPE)
        private set

    var loading by mutableStateOf(false)
        private set
    var submitting by mutableStateOf(false)
        private set
    var checkingUnpaid by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var info by mutableStateOf<String?>(null)
        private set
    var alert by mutableStateOf<AppAlert?>(null)
        private set

    fun dismissAlert() {
        alert = null
    }

    private val allProducts = mutableStateListOf<PpobProductDto>()
    val categories = mutableStateListOf<String>()
    val categoryProducts = mutableStateListOf<PpobProductDto>()
    val history = mutableStateListOf<PpobTransactionDto>()

    var selectedCategory by mutableStateOf<String?>(null)
        private set
    var selectedProduct by mutableStateOf<PpobProductDto?>(null)
        private set
    var customerNo by mutableStateOf("")

    var quotePricing by mutableStateOf<PpobPricingDto?>(null)
        private set
    var quoteProduct by mutableStateOf<PpobProductDto?>(null)
        private set
    var pendingInquiry by mutableStateOf<PpobInquiryResultDto?>(null)
        private set

    var paymentRefId by mutableStateOf<String?>(null)
        private set
    var payment by mutableStateOf<PpobPaymentDto?>(null)
        private set
    var paymentStatusLabel by mutableStateOf<String?>(null)
        private set
    var isWaitingPayment by mutableStateOf(true)
        private set
    var isPaid by mutableStateOf(false)
        private set
    var isPaymentSuccess by mutableStateOf(false)
        private set

    var lastTransaction by mutableStateOf<PpobTransactionDto?>(null)
        private set

    var detailTransaction by mutableStateOf<PpobTransactionDto?>(null)
        private set
    var detailLoading by mutableStateOf(false)
        private set
    var detailPolling by mutableStateOf(false)
        private set

    val customerNoFieldLabel: String
        get() = selectedProduct?.customerNoLabel(prepaid = tab == PpobTab.PREPAID)
            ?: if (tab == PpobTab.PREPAID) "Nomor tujuan" else "ID pelanggan / nomor tagihan"

    val customerNoFieldHint: String
        get() = selectedProduct?.customerNoHint(prepaid = tab == PpobTab.PREPAID)
            ?: "08xx / ID meter / ID pelanggan"

    /** Durasi countdown pembayaran di UI (tetap 15 menit). */
    val paymentCountdownMinutes: Int = 15

    /** Epoch ms batas waktu bayar; dipakai countdown `00:15:00`. */
    var paymentExpiresAtMs by mutableStateOf<Long?>(null)
        private set

    private fun startPaymentCountdown(createdAt: String? = null, refId: String? = null) {
        val durationMs = paymentCountdownMinutes * 60_000L
        val cached = refId?.let { paymentPrefs.expiresAtMs(it) }
        val fromCreated = parsePpobDateTimeMs(createdAt)?.plus(durationMs)
        val deadline = when {
            cached != null && cached > System.currentTimeMillis() -> cached
            fromCreated != null -> fromCreated
            else -> System.currentTimeMillis() + durationMs
        }
        paymentExpiresAtMs = deadline
        val ref = refId ?: paymentRefId
        if (!ref.isNullOrBlank()) {
            val cur = payment
            paymentPrefs.save(
                refId = ref,
                qrisImageUrl = cur?.qrisImageUrl ?: paymentPrefs.qrisImageUrl(ref),
                qrisString = cur?.qrisString ?: paymentPrefs.qrisString(ref),
                payAmount = cur?.payAmount,
                expireMinutes = paymentCountdownMinutes,
                expiresAtMs = deadline,
            )
        }
    }

    private fun parsePpobDateTimeMs(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        val patterns = listOf(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
        )
        for (p in patterns) {
            try {
                val fmt = java.text.SimpleDateFormat(p, java.util.Locale.US)
                fmt.isLenient = false
                return fmt.parse(raw.trim())?.time
            } catch (_: Exception) {
                // coba pola berikutnya
            }
        }
        return null
    }

    /**
     * Saat masuk layar PPOB: jika ada order unpaid aktif, langsung ke halaman pembayaran.
     */
    fun checkActiveUnpaidAndResume() {
        if (step == PpobStep.PAYMENT || step == PpobStep.SUCCESS) return
        unpaidCheckJob?.cancel()
        unpaidCheckJob = viewModelScope.launch {
            checkingUnpaid = true
            error = null
            val unpaid = findActiveUnpaidTransaction()
            if (unpaid != null) {
                resumeUnpaidPayment(unpaid, notify = true)
            }
            checkingUnpaid = false
        }
    }

    private suspend fun findActiveUnpaidTransaction(): PpobTransactionDto? {
        return when (val r = repo.ppobTransactions(limit = 30)) {
            is ApiResult.Ok -> {
                if (!r.data.success) null
                else r.data.transactions.firstOrNull { it.isActiveUnpaid() }
            }
            is ApiResult.Err -> null
        }
    }

    private fun resumeUnpaidPayment(tx: PpobTransactionDto, notify: Boolean) {
        val ref = tx.refId ?: return
        val cachedUrl = paymentPrefs.qrisImageUrl(ref)
        val cachedString = paymentPrefs.qrisString(ref)
        paymentRefId = ref
        lastTransaction = tx
        payment = tx.toPaymentDto(
            cachedQrisImageUrl = cachedUrl,
            cachedQrisString = cachedString,
            expireMinutes = paymentCountdownMinutes,
        )
        paymentStatusLabel = tx.paymentStatus ?: tx.status ?: "unpaid"
        isWaitingPayment = tx.isWaitingPayment != false
        isPaid = tx.isPaid == true
        isPaymentSuccess = tx.isSuccess == true
        customerNo = tx.customerNo.orEmpty()
        if (notify) {
            info = "Anda masih punya transaksi belum dibayar. Selesaikan pembayaran dulu."
        }
        startPaymentCountdown(createdAt = tx.createdAt, refId = ref)
        step = PpobStep.PAYMENT
        startPaymentPoll(ref)
        refreshPaymentStatusNow()
    }

    fun selectTab(newTab: PpobTab) {
        if (checkingUnpaid || submitting) return
        viewModelScope.launch {
            val unpaid = findActiveUnpaidTransaction()
            if (unpaid != null) {
                resumeUnpaidPayment(unpaid, notify = true)
                return@launch
            }
            if (tab == newTab && step != PpobStep.TYPE) return@launch
            tab = newTab
            clearDownstreamFrom(PpobStep.TYPE)
            step = PpobStep.CATEGORY
            refreshCatalog()
        }
    }

    fun refreshCatalog() {
        viewModelScope.launch {
            loading = true
            error = null
            val cmd = if (tab == PpobTab.PREPAID) "prepaid" else "pasca"
            when (val r = repo.ppobCatalog(cmd = cmd)) {
                is ApiResult.Ok -> {
                    if (!r.data.success) {
                        error = r.data.message ?: mapPpobCode(r.data.code)
                    } else {
                        allProducts.clear()
                        allProducts.addAll(r.data.products.availableOnly())
                        rebuildCategories()
                        if (allProducts.isEmpty()) {
                            error = "Belum ada produk aktif dengan harga jual."
                        }
                    }
                }
                is ApiResult.Err -> error = mapPpobError(r)
            }
            loading = false
        }
    }

    private fun rebuildCategories() {
        val cats = allProducts.map { it.resolvedCategory() }.distinct().sorted()
        categories.clear()
        categories.addAll(cats)
    }

    fun selectCategory(category: String) {
        selectedCategory = category
        categoryProducts.clear()
        categoryProducts.addAll(
            allProducts
                .filter { it.resolvedCategory().equals(category, ignoreCase = true) }
                .sortedGroupedByBrand(),
        )
        selectedProduct = null
        customerNo = ""
        clearQuoteAndPayment()
        error = null
        step = PpobStep.PRODUCT
    }

    fun selectProduct(product: PpobProductDto) {
        selectedProduct = product
        customerNo = ""
        clearQuoteAndPayment()
        error = null
        info = null
        step = PpobStep.INPUT
    }

    /** Langsung checkout QRIS; cek nomor dikerjakan backend setelah bayar. */
    fun onInputContinue() {
        val no = customerNo.trim()
        if (no.isBlank()) {
            error = "$customerNoFieldLabel wajib diisi"
            return
        }
        error = null
        submitCheckout()
    }

    fun submitCheckout() {
        val product = selectedProduct ?: return
        val sku = product.buyerSkuCode ?: return
        val no = customerNo.trim()
        if (no.isBlank()) {
            error = "$customerNoFieldLabel wajib diisi"
            return
        }
        viewModelScope.launch {
            submitting = true
            error = null
            info = null
            val unpaid = findActiveUnpaidTransaction()
            if (unpaid != null) {
                submitting = false
                resumeUnpaidPayment(unpaid, notify = true)
                return@launch
            }
            when (val r = repo.ppobCheckout(sku, no, product.productName)) {
                is ApiResult.Ok -> {
                    if (r.data.success && r.data.payment != null) {
                        val ref = r.data.refId ?: r.data.transaction?.refId
                        if (ref.isNullOrBlank()) {
                            error = "Checkout berhasil tetapi ref_id kosong."
                        } else {
                            val pay = r.data.payment.copy(
                                expireMinutes = paymentCountdownMinutes,
                            )
                            val deadline = System.currentTimeMillis() + paymentCountdownMinutes * 60_000L
                            paymentPrefs.save(
                                refId = ref,
                                qrisImageUrl = pay.qrisImageUrl,
                                qrisString = pay.qrisString,
                                payAmount = pay.payAmount,
                                expireMinutes = paymentCountdownMinutes,
                                expiresAtMs = deadline,
                            )
                            paymentRefId = ref
                            payment = pay
                            lastTransaction = r.data.transaction
                            paymentStatusLabel = pay.paymentStatus ?: "unpaid"
                            isWaitingPayment = true
                            isPaid = false
                            isPaymentSuccess = false
                            info = null
                            paymentExpiresAtMs = deadline
                            step = PpobStep.PAYMENT
                            startPaymentPoll(ref)
                        }
                    } else {
                        error = r.data.message ?: r.data.error ?: mapPpobCode(r.data.code)
                    }
                }
                is ApiResult.Err -> error = mapPpobError(r)
            }
            submitting = false
        }
    }

    private fun startPaymentPoll(refId: String) {
        paymentPollJob?.cancel()
        paymentPollJob = viewModelScope.launch {
            while (isActive) {
                delay(4_000)
                when (val r = repo.ppobPaymentStatus(refId)) {
                    is ApiResult.Ok -> {
                        if (!r.data.success) continue
                        applyPaymentStatus(
                            r.data.isWaitingPayment,
                            r.data.isPaid,
                            r.data.isSuccess,
                            r.data.status,
                            r.data.payment,
                            r.data.transaction,
                        )
                        when {
                            r.data.isSuccess == true -> {
                                paymentPrefs.clear(refId)
                                step = PpobStep.SUCCESS
                                return@launch
                            }
                            r.data.isFailedStatus() -> {
                                cancelInvalidOrFailedTransaction(
                                    r.data.message
                                        ?: r.data.transaction?.message
                                        ?: "Transaksi gagal",
                                    invalidNumber = r.data.looksLikeInvalidNumber() ||
                                        (selectedProduct?.requiresNumberCheck() == true),
                                )
                                return@launch
                            }
                        }
                    }
                    is ApiResult.Err -> Unit
                }
            }
        }
    }

    private fun applyPaymentStatus(
        waiting: Boolean?,
        paid: Boolean?,
        success: Boolean?,
        status: String?,
        pay: PpobPaymentDto?,
        tx: PpobTransactionDto?,
    ) {
        if (waiting != null) isWaitingPayment = waiting
        if (paid != null) isPaid = paid
        if (success != null) isPaymentSuccess = success
        if (status != null) paymentStatusLabel = status
        if (pay != null) {
            val merged = mergePayment(pay)
            payment = merged
            paymentRefId?.let { ref ->
                if (!merged.qrisImageUrl.isNullOrBlank() || !merged.qrisString.isNullOrBlank()) {
                    paymentPrefs.save(
                        refId = ref,
                        qrisImageUrl = merged.qrisImageUrl,
                        qrisString = merged.qrisString,
                        payAmount = merged.payAmount,
                        expireMinutes = paymentCountdownMinutes,
                        expiresAtMs = paymentExpiresAtMs ?: paymentPrefs.expiresAtMs(ref),
                    )
                }
            }
        } else if (tx != null) {
            val current = payment
            payment = tx.toPaymentDto(
                cachedQrisImageUrl = current?.qrisImageUrl ?: paymentPrefs.qrisImageUrl(tx.refId.orEmpty()),
                cachedQrisString = current?.qrisString ?: paymentPrefs.qrisString(tx.refId.orEmpty()),
                expireMinutes = current?.expireMinutes ?: paymentPrefs.expireMinutes(tx.refId.orEmpty()),
            ).let { built ->
                if (built.payAmount == null && current?.payAmount != null) {
                    built.copy(payAmount = current.payAmount)
                } else built
            }
        }
        if (tx != null) lastTransaction = tx
        // Fallback jika flag tidak lengkap
        when {
            success == true -> {
                isWaitingPayment = false
                isPaid = true
                isPaymentSuccess = true
            }
            paid == true -> {
                isWaitingPayment = false
                isPaid = true
            }
            status.equals("WaitingPayment", true) -> isWaitingPayment = true
            status.equals("Pending", true) -> {
                isWaitingPayment = false
                isPaid = true
            }
            status.equals("Sukses", true) -> {
                isWaitingPayment = false
                isPaid = true
                isPaymentSuccess = true
            }
        }
    }

    private fun mergePayment(incoming: PpobPaymentDto): PpobPaymentDto {
        val cur = payment
        return incoming.copy(
            qrisImageUrl = incoming.qrisImageUrl?.takeIf { it.isNotBlank() }
                ?: cur?.qrisImageUrl,
            qrisString = incoming.qrisString?.takeIf { it.isNotBlank() }
                ?: cur?.qrisString,
            payAmount = incoming.payAmount ?: cur?.payAmount,
            expireMinutes = incoming.expireMinutes ?: cur?.expireMinutes,
            breakdown = incoming.breakdown ?: cur?.breakdown,
        )
    }

    fun refreshPaymentStatusNow() {
        val ref = paymentRefId ?: return
        viewModelScope.launch {
            when (val r = repo.ppobPaymentStatus(ref)) {
                is ApiResult.Ok -> {
                    if (r.data.success) {
                        applyPaymentStatus(
                            r.data.isWaitingPayment,
                            r.data.isPaid,
                            r.data.isSuccess,
                            r.data.status,
                            r.data.payment,
                            r.data.transaction,
                        )
                        if (r.data.isSuccess == true) {
                            paymentPollJob?.cancel()
                            paymentPrefs.clear(ref)
                            step = PpobStep.SUCCESS
                        } else if (r.data.isFailedStatus()) {
                            cancelInvalidOrFailedTransaction(
                                r.data.message ?: "Transaksi gagal",
                                invalidNumber = r.data.looksLikeInvalidNumber() ||
                                    (selectedProduct?.requiresNumberCheck() == true),
                            )
                        }
                    }
                }
                is ApiResult.Err -> error = mapPpobError(r)
            }
        }
    }

    /**
     * Batalkan order QRIS unpaid via `POST /api/customer/ppob/cancel`.
     * Idempotent untuk sudah cancelled/expired; 409 jika sudah lunas.
     */
    fun cancelActivePayment() {
        val ref = paymentRefId ?: return
        if (submitting) return
        viewModelScope.launch {
            submitting = true
            error = null
            paymentPollJob?.cancel()
            when (val r = repo.ppobCancel(ref)) {
                is ApiResult.Ok -> {
                    // 200: dibatalkan, atau sudah cancelled/expired (idempotent)
                    if (r.data.success ||
                        r.data.code in setOf("ALREADY_CANCELLED", "ALREADY_EXPIRED") ||
                        r.data.transaction?.isCancelled == true
                    ) {
                        paymentPrefs.clear(ref)
                        clearDownstreamFrom(PpobStep.TYPE)
                        tab = PpobTab.PREPAID
                        step = PpobStep.TYPE
                        allProducts.clear()
                        categories.clear()
                        categoryProducts.clear()
                        info = null
                        alert = AppAlert(
                            type = AlertType.SUCCESS,
                            title = "Pembatalan Berhasil",
                            message = r.data.message ?: "Transaksi PPOB telah dibatalkan.",
                        )
                    } else {
                        error = r.data.message ?: r.data.error ?: mapPpobCode(r.data.code)
                        alert = AppAlert(
                            type = AlertType.ERROR,
                            title = "Gagal Membatalkan",
                            message = error ?: "Transaksi tidak dapat dibatalkan.",
                        )
                        startPaymentPoll(ref)
                    }
                }
                is ApiResult.Err -> {
                    when (r.code) {
                        // Sudah lunas — jangan batalkan; lanjut poll / ke sukses
                        "ALREADY_PAID" -> {
                            error = mapPpobError(r)
                            alert = AppAlert(
                                type = AlertType.WARNING,
                                title = "Tidak Dapat Dibatalkan",
                                message = mapPpobError(r),
                            )
                            refreshPaymentStatusNow()
                            startPaymentPoll(ref)
                        }
                        else -> {
                            error = mapPpobError(r)
                            alert = AppAlert(
                                type = AlertType.ERROR,
                                title = "Gagal Membatalkan",
                                message = mapPpobError(r),
                            )
                            // Unpaid masih aktif → lanjut tampilkan QR
                            if (r.code != "NOT_FOUND") startPaymentPoll(ref)
                            else {
                                paymentPrefs.clear(ref)
                                clearDownstreamFrom(PpobStep.TYPE)
                                step = PpobStep.TYPE
                            }
                        }
                    }
                }
            }
            submitting = false
        }
    }

    /** Batalkan order QRIS bila nomor DANA/PLN tidak valid atau Digiflazz gagal. */
    private fun cancelInvalidOrFailedTransaction(message: String, invalidNumber: Boolean) {
        val ref = paymentRefId
        paymentPollJob?.cancel()
        viewModelScope.launch {
            if (!ref.isNullOrBlank()) {
                repo.ppobCancel(ref)
                paymentPrefs.clear(ref)
            }
            payment = null
            paymentRefId = null
            isWaitingPayment = false
            isPaid = false
            isPaymentSuccess = false
            lastTransaction = null
            error = if (invalidNumber) {
                "Nomor tidak valid. Transaksi dibatalkan. Periksa nomor lalu coba lagi."
            } else {
                message
            }
            step = if (selectedProduct != null) PpobStep.INPUT else PpobStep.TYPE
        }
    }

    fun back() {
        when (step) {
            PpobStep.TYPE -> Unit
            PpobStep.CATEGORY -> {
                clearDownstreamFrom(PpobStep.TYPE)
                step = PpobStep.TYPE
            }
            PpobStep.PRODUCT -> {
                selectedProduct = null
                categoryProducts.clear()
                step = PpobStep.CATEGORY
            }
            PpobStep.INPUT -> {
                customerNo = ""
                clearQuoteAndPayment()
                step = PpobStep.PRODUCT
            }
            // Jangan hapus unpaid; kembali ke TYPE akan diarahkan lagi ke pembayaran.
            PpobStep.PAYMENT -> {
                clearDownstreamFrom(PpobStep.TYPE)
                step = PpobStep.TYPE
                checkActiveUnpaidAndResume()
            }
            PpobStep.SUCCESS -> startNewTransaction()
        }
    }

    fun startNewTransaction() {
        paymentPollJob?.cancel()
        unpaidCheckJob?.cancel()
        clearDownstreamFrom(PpobStep.TYPE)
        tab = PpobTab.PREPAID
        step = PpobStep.TYPE
        allProducts.clear()
        categories.clear()
        categoryProducts.clear()
        checkActiveUnpaidAndResume()
    }

    private fun clearDownstreamFrom(from: PpobStep) {
        if (from.ordinal <= PpobStep.TYPE.ordinal) {
            selectedCategory = null
        }
        if (from.ordinal <= PpobStep.CATEGORY.ordinal) {
            selectedProduct = null
            categoryProducts.clear()
        }
        if (from.ordinal <= PpobStep.PRODUCT.ordinal) {
            customerNo = ""
        }
        clearQuoteAndPayment()
        error = null
        info = null
    }

    private fun clearQuoteAndPayment() {
        paymentPollJob?.cancel()
        quotePricing = null
        quoteProduct = null
        pendingInquiry = null
        paymentRefId = null
        payment = null
        paymentStatusLabel = null
        paymentExpiresAtMs = null
        isWaitingPayment = true
        isPaid = false
        isPaymentSuccess = false
        lastTransaction = null
    }

    fun loadHistory() {
        viewModelScope.launch {
            loading = true
            error = null
            when (val r = repo.ppobTransactions(limit = 50)) {
                is ApiResult.Ok -> {
                    if (r.data.success) {
                        history.clear()
                        history.addAll(r.data.transactions)
                    } else {
                        error = r.data.message ?: "Gagal memuat riwayat"
                    }
                }
                is ApiResult.Err -> error = mapPpobError(r)
            }
            loading = false
        }
    }

    fun loadDetail(refId: String, autoPoll: Boolean = true) {
        viewModelScope.launch {
            detailLoading = true
            error = null
            when (val r = repo.ppobTransaction(refId)) {
                is ApiResult.Ok -> {
                    if (r.data.success && r.data.transaction != null) {
                        detailTransaction = r.data.transaction
                        if (autoPoll && r.data.transaction.isSuccess != true &&
                            r.data.transaction.status?.contains("Gagal", true) != true &&
                            r.data.transaction.isCancelled != true
                        ) {
                            startDetailPoll(refId)
                        }
                    } else {
                        error = r.data.message ?: r.data.error ?: "Transaksi tidak ditemukan"
                    }
                }
                is ApiResult.Err -> error = mapPpobError(r)
            }
            detailLoading = false
        }
    }

    private fun startDetailPoll(refId: String) {
        detailPollJob?.cancel()
        detailPolling = true
        detailPollJob = viewModelScope.launch {
            try {
                repeat(15) {
                    delay(4_000)
                    when (val r = repo.ppobPaymentStatus(refId)) {
                        is ApiResult.Ok -> {
                            r.data.transaction?.let { detailTransaction = it }
                            if (r.data.isSuccess == true || r.data.isFailedStatus()) return@launch
                        }
                        is ApiResult.Err -> {
                            when (val t = repo.ppobTransaction(refId)) {
                                is ApiResult.Ok -> {
                                    t.data.transaction?.let { detailTransaction = it }
                                    if (t.data.transaction?.isSuccess == true) return@launch
                                }
                                else -> Unit
                            }
                        }
                    }
                }
            } finally {
                detailPolling = false
            }
        }
    }

    fun clearDetail() {
        detailPollJob?.cancel()
        detailPolling = false
        detailTransaction = null
        detailLoading = false
    }

    private fun mapPpobError(err: ApiResult.Err): String = when {
        err.code == "UNAUTHORIZED" -> "Sesi habis. Silakan login ulang."
        err.code == "SELL_PRICE_MISSING" -> "Harga jual produk belum diatur admin."
        err.code == "QRIS_NOT_READY" -> "QRIS belum siap di server. Hubungi admin."
        err.code == "PRODUCT_NOT_FOUND" -> "Produk tidak ditemukan di katalog."
        err.code == "UNIQUE_CODE_EXHAUSTED" -> "Kode unik penuh. Coba beberapa saat lagi."
        err.code == "FIELDS_REQUIRED" -> err.message.takeIf { it.isNotBlank() } ?: "ref_id wajib diisi."
        err.code == "NOT_FOUND" -> err.message.takeIf { it.isNotBlank() } ?: "Transaksi tidak ditemukan."
        err.code == "ALREADY_PAID" -> "Transaksi sudah dibayar dan tidak bisa dibatalkan."
        err.code == "NOT_CANCELLABLE" -> "Transaksi tidak bisa dibatalkan pada status ini."
        err.httpCode == 503 -> "Layanan PPOB belum dikonfigurasi di server."
        err.httpCode == 404 -> err.message
        else -> err.message
    }

    private fun mapPpobCode(code: String?): String = when (code) {
        "CATALOG_FAILED" -> "Katalog belum tersedia."
        "DIGIFLAZZ_NOT_READY" -> "Layanan PPOB belum siap di server."
        "SELL_PRICE_MISSING" -> "Harga jual produk belum diatur admin."
        "QRIS_NOT_READY" -> "QRIS belum siap di server."
        "PRODUCT_NOT_FOUND" -> "Produk tidak ditemukan."
        "UNIQUE_CODE_EXHAUSTED" -> "Kode unik penuh. Coba lagi nanti."
        "FIELDS_REQUIRED" -> "ref_id wajib diisi."
        "NOT_FOUND" -> "Transaksi tidak ditemukan."
        "ALREADY_PAID" -> "Transaksi sudah dibayar."
        "NOT_CANCELLABLE" -> "Transaksi tidak bisa dibatalkan."
        "ALREADY_CANCELLED" -> "Transaksi sudah dibatalkan."
        "ALREADY_EXPIRED" -> "Transaksi sudah kedaluwarsa."
        null, "" -> "Terjadi kesalahan"
        else -> code
    }

    override fun onCleared() {
        paymentPollJob?.cancel()
        detailPollJob?.cancel()
        unpaidCheckJob?.cancel()
        super.onCleared()
    }
}
