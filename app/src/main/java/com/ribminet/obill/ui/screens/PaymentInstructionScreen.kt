package com.ribminet.obill.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ribminet.obill.data.remote.OrderDto
import com.ribminet.obill.data.remote.WalletPaymentDto
import com.ribminet.obill.data.remote.creditApplied
import com.ribminet.obill.data.remote.creditNote
import com.ribminet.obill.data.remote.installationFeeAmount
import com.ribminet.obill.data.remote.installationFeeLabel
import com.ribminet.obill.data.remote.isQrisDinamisPayment
import com.ribminet.obill.data.remote.latePenaltyAmount
import com.ribminet.obill.data.remote.latePenaltyLabel
import com.ribminet.obill.data.remote.orderTypeDisplay
import com.ribminet.obill.data.remote.payableTotal
import com.ribminet.obill.data.remote.periodLabel
import com.ribminet.obill.data.remote.resolvedAmounts
import com.ribminet.obill.data.remote.subscriptionAmount
import com.ribminet.obill.data.remote.subscriptionGross
import com.ribminet.obill.ui.components.AppCard
import com.ribminet.obill.ui.components.AppTopBar
import com.ribminet.obill.ui.components.BillAmountBreakdown
import com.ribminet.obill.ui.components.LatePenaltyNotice
import com.ribminet.obill.ui.components.ConfirmDialog
import com.ribminet.obill.ui.components.ConfirmRequest
import com.ribminet.obill.ui.components.PrimaryButton
import com.ribminet.obill.ui.components.QrisStandardCard
import com.ribminet.obill.ui.components.SecondaryButton
import com.ribminet.obill.ui.components.AppTextField
import com.ribminet.obill.ui.components.clickableNoRipple
import com.ribminet.obill.ui.guide.GuideTarget
import com.ribminet.obill.ui.guide.guideTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.ribminet.obill.ui.theme.BrandBlue
import com.ribminet.obill.ui.theme.BrandBlueSurface
import com.ribminet.obill.ui.theme.DangerRed
import com.ribminet.obill.ui.theme.DangerSurface
import com.ribminet.obill.ui.theme.Divider
import com.ribminet.obill.ui.theme.OnAccent
import com.ribminet.obill.ui.theme.SuccessGreen
import com.ribminet.obill.ui.theme.SuccessSurface
import com.ribminet.obill.ui.theme.TextPrimary
import com.ribminet.obill.ui.theme.TextSecondary
import com.ribminet.obill.ui.theme.WarningOrange
import com.ribminet.obill.ui.theme.WarningSurface
import com.ribminet.obill.util.rupiah

@Composable
fun PaymentInstructionScreen(
    order: OrderDto?,
    submitting: Boolean,
    statusRefreshing: Boolean,
    error: String?,
    alert: com.ribminet.obill.AppAlert?,
    onDismissAlert: () -> Unit,
    onConfirm: (String?) -> Unit,
    onCancel: () -> Unit,
    onRefreshStatus: () -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit,
    billPaymentExpiresAtMs: Long? = null,
) {
    if (order == null) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(title = "Pembayaran QRIS", onBack = onBack)
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Pesanan tidak tersedia.", color = TextSecondary, fontSize = 13.sp)
            }
        }
        com.ribminet.obill.ui.components.SweetAlertDialog(alert = alert, onConfirm = onDismissAlert)
        return
    }

    var refNo by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf<ConfirmRequest?>(null) }
    val status = order.status ?: "pending"
    val instr = order.paymentInstruction
    val qrisDinamis = order.isQrisDinamisPayment()
    val pay = order.payment

    // Polling status (QRIS dinamis diurus ViewModel; awaiting_confirmation legacy tetap di sini)
    if (status == "awaiting_confirmation" && (order.id ?: 0) > 0 && !qrisDinamis) {
        LaunchedEffect(order.id) {
            while (true) {
                delay(8000)
                onRefreshStatus()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(
            title = if (qrisDinamis) "Bayar QRIS" else "Instruksi Pembayaran",
            onBack = onBack,
        )

        if (qrisDinamis && (status == "pending" || status == "confirmed" || status == "paid")) {
            // Layout sederhana seperti PPOB / Top Up
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .guideTarget(GuideTarget.PAYMENT_INSTRUCTION_DETAIL),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (status) {
                    "confirmed", "paid" -> {
                        Icon(Icons.Filled.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(64.dp))
                        Text("Pembayaran Berhasil", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = SuccessGreen)
                        Text(
                            "Layanan Anda telah diaktifkan.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                        )
                    }
                    else -> {
                        BillPaymentCountdown(billPaymentExpiresAtMs)
                        QrisStandardCard(
                            qrisImageUrl = pay?.qrisImageUrl,
                            payAmount = pay?.payAmount,
                            merchantName = pay?.merchantName?.takeIf { it.isNotBlank() } ?: "Onesky Internet",
                            nmid = pay?.nmid?.takeIf { it.isNotBlank() } ?: "ID1024325805181",
                            terminalId = pay?.terminalId?.takeIf { it.isNotBlank() } ?: "A01",
                        )
                        StatusPillSimple("Menunggu pembayaran QRIS", WarningSurface, WarningOrange)
                        BillQrisDownloadButton(pay?.qrisImageUrl, order.orderNo)
                        if (!error.isNullOrBlank()) {
                            Text(error, color = DangerRed, fontSize = 12.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
            Box(modifier = Modifier.padding(16.dp)) {
                when (status) {
                    "pending" -> SecondaryButton(
                        text = if (submitting) "Membatalkan…" else "Batalkan Transaksi",
                        color = DangerRed,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (submitting) return@SecondaryButton
                            confirm = ConfirmRequest(
                                title = "Batalkan Pesanan?",
                                message = "Pesanan ini akan dibatalkan dan Anda perlu membuat pesanan baru jika ingin membayar.",
                                confirmText = "Ya, Batalkan",
                                cancelText = "Tidak",
                                accent = DangerRed,
                                onConfirm = onCancel,
                            )
                        },
                    )
                    else -> PrimaryButton(text = "Kembali ke Beranda", onClick = onHome)
                }
            }
        } else {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            StatusBanner(
                status,
                order.statusLabel,
                order.adminNote,
                qrisDinamis = qrisDinamis,
                modifier = Modifier.guideTarget(
                    if (status == "awaiting_confirmation") GuideTarget.PAYMENT_AWAITING_STATUS
                    else GuideTarget.PAYMENT_INSTRUCTION_DETAIL,
                ),
            )
            Spacer(modifier = Modifier.height(16.dp))

            Column(modifier = Modifier.guideTarget(GuideTarget.PAYMENT_INSTRUCTION_DETAIL)) {
            order.latePenalty?.let { penalty ->
                LatePenaltyNotice(penalty = penalty)
                Spacer(modifier = Modifier.height(16.dp))
            }

            AppCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(order.orderNo ?: "-", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                    Text(order.orderTypeDisplay(), color = TextSecondary, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
                val amounts = order.resolvedAmounts()
                if (amounts != null) {
                    BillAmountBreakdown(
                        subscriptionLabel = "Biaya berlangganan — ${order.toProfileName ?: "-"}",
                        subscriptionAmount = order.subscriptionAmount(),
                        subscriptionGross = order.subscriptionGross(),
                        creditApplied = order.creditApplied(),
                        creditNote = order.creditNote(),
                        periodLabel = order.periodLabel(),
                        installationLabel = order.installationFeeLabel(),
                        installationAmount = order.installationFeeAmount(),
                        latePenaltyAmount = order.latePenaltyAmount(),
                        latePenaltyLabel = order.latePenaltyLabel(),
                        totalAmount = order.payableTotal(),
                    )
                } else {
                    InfoLine("Nominal", rupiah(order.payableTotal()), BrandBlue)
                }
                InfoLine("Paket", order.toProfileName ?: "-")
                InfoLine("Metode", instr?.label ?: order.paymentMethod ?: "-")
            }

            if (instr != null && status == "pending") {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Instruksi Pembayaran", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                Spacer(modifier = Modifier.height(10.dp))
                when (instr.type) {
                    "bank_transfer" -> instr.accounts?.forEach { acc ->
                        BankCard(acc.bank ?: "-", acc.accountNumber ?: "-", acc.accountName ?: "-")
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    "qris", "qr" -> QrCard(instr.label ?: "QRIS", instr.qrImageUrl, instr.merchantName, instr.note)
                    "cash" -> CashCard(instr.label ?: "Cash", instr.note)
                    else -> if (!instr.note.isNullOrBlank()) CashCard(instr.label ?: "Pembayaran", instr.note)
                }
            }

            if (status == "pending") {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Konfirmasi Pembayaran", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                AppTextField(
                    value = refNo,
                    onValueChange = { refNo = it },
                    placeholder = "No. referensi transfer (opsional)",
                    label = "No. Referensi",
                )
            }

            if (!error.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(error, color = DangerRed, fontSize = 12.sp)
            }
            }
        }

        Box(modifier = Modifier.padding(16.dp)) {
            Column {
                when (status) {
                    "pending" -> {
                        PrimaryButton(
                            text = if (submitting) "Memproses..." else "Saya Sudah Bayar",
                            enabled = !submitting,
                            modifier = Modifier.guideTarget(GuideTarget.PAYMENT_CONFIRM_BTN),
                            onClick = {
                                confirm = ConfirmRequest(
                                    title = "Konfirmasi Pembayaran?",
                                    message = "Pastikan Anda sudah melakukan pembayaran. Konfirmasi ini akan dikirim ke admin untuk diverifikasi.",
                                    confirmText = "Ya, Sudah Bayar",
                                    onConfirm = { onConfirm(refNo.ifBlank { null }) },
                                )
                            }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        SecondaryButton(
                            text = if (submitting) "Membatalkan…" else "Batalkan Pesanan",
                            color = DangerRed,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                if (submitting) return@SecondaryButton
                                confirm = ConfirmRequest(
                                    title = "Batalkan Pesanan?",
                                    message = "Pesanan ini akan dibatalkan dan Anda perlu membuat pesanan baru jika ingin membayar.",
                                    confirmText = "Ya, Batalkan",
                                    cancelText = "Tidak",
                                    accent = DangerRed,
                                    onConfirm = onCancel,
                                )
                            }
                        )
                    }
                    "awaiting_confirmation" -> {
                        PrimaryButton(
                            text = if (statusRefreshing) "Memeriksa..." else "Cek Status Pembayaran",
                            enabled = !statusRefreshing,
                            modifier = Modifier.guideTarget(GuideTarget.PAYMENT_CHECK_STATUS),
                            onClick = onRefreshStatus
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        SecondaryButton(
                            text = "Batalkan Pesanan",
                            color = DangerRed,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                confirm = ConfirmRequest(
                                    title = "Batalkan Pesanan?",
                                    message = "Pesanan ini akan dibatalkan walau Anda sudah menekan konfirmasi bayar.",
                                    confirmText = "Ya, Batalkan",
                                    cancelText = "Tidak",
                                    accent = DangerRed,
                                    onConfirm = onCancel,
                                )
                            }
                        )
                    }
                    else -> PrimaryButton(text = "Kembali ke Beranda", onClick = onHome)
                }
            }
        }
        } // end legacy layout
    }

    ConfirmDialog(request = confirm, onDismiss = { confirm = null })
    com.ribminet.obill.ui.components.SweetAlertDialog(alert = alert, onConfirm = onDismissAlert)
}

@Composable
private fun StatusPillSimple(
    text: String,
    bg: Color,
    fg: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.QrCode2, null, tint = fg, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, color = fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun BillQrisDownloadButton(qrisImageUrl: String?, orderNo: String?) {
    if (qrisImageUrl.isNullOrBlank()) return
    val context = LocalContext.current
    var downloading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    SecondaryButton(
        text = if (downloading) "Mengunduh…" else "Download QRIS",
        onClick = {
            if (downloading) return@SecondaryButton
            downloading = true
            scope.launch {
                val ok = saveBillQrisToGallery(context, qrisImageUrl, orderNo)
                downloading = false
                Toast.makeText(
                    context,
                    if (ok) "QRIS disimpan ke Galeri" else "Gagal mengunduh QRIS",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun BillPaymentCountdown(expiresAtMs: Long?) {
    var remainingMs by remember(expiresAtMs) {
        mutableLongStateOf(
            ((expiresAtMs ?: 0L) - System.currentTimeMillis()).coerceAtLeast(0L),
        )
    }
    LaunchedEffect(expiresAtMs) {
        while (true) {
            val left = ((expiresAtMs ?: 0L) - System.currentTimeMillis()).coerceAtLeast(0L)
            remainingMs = left
            if (left <= 0L || expiresAtMs == null) break
            delay(1_000)
        }
    }
    val totalSec = remainingMs / 1_000L
    val hours = totalSec / 3_600L
    val minutes = (totalSec % 3_600L) / 60L
    val seconds = totalSec % 60L
    val text = String.format("%02d:%02d:%02d", hours, minutes, seconds)
    val color = if (remainingMs <= 60_000L) DangerRed else WarningOrange
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("Selesaikan pembayaran dalam", fontSize = 11.sp, color = TextSecondary)
        Text(
            text,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = color,
            letterSpacing = 1.sp,
        )
    }
}

private suspend fun saveBillQrisToGallery(
    context: android.content.Context,
    imageUrl: String,
    orderNo: String?,
): Boolean = withContext(Dispatchers.IO) {
    try {
        val connection = java.net.URL(imageUrl).openConnection()
        connection.connectTimeout = 15_000
        connection.readTimeout = 20_000
        connection.getInputStream().use { input ->
            val bytes = input.readBytes()
            val fileName = "qris_bill_${orderNo ?: System.currentTimeMillis()}.png"
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Obill")
                    put(android.provider.MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return@withContext false
            resolver.openOutputStream(uri)?.use { out -> out.write(bytes) }
                ?: return@withContext false
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                values.clear()
                values.put(android.provider.MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            true
        }
    } catch (_: Exception) {
        false
    }
}

@Composable
private fun StatusBanner(
    status: String,
    statusLabel: String?,
    adminNote: String?,
    qrisDinamis: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val (bg, fg, icon, title, desc) = when (status) {
        "awaiting_confirmation" -> Quint(
            WarningSurface, WarningOrange, Icons.Filled.HourglassTop,
            statusLabel ?: "Menunggu Konfirmasi",
            "Pembayaran Anda sedang diverifikasi oleh admin. Halaman akan diperbarui otomatis.",
        )
        "confirmed", "paid" -> Quint(
            SuccessSurface, SuccessGreen, Icons.Filled.CheckCircle,
            statusLabel ?: "Pembayaran Berhasil",
            "Pembayaran terverifikasi. Layanan Anda telah diaktifkan.",
        )
        "rejected" -> Quint(
            DangerSurface, DangerRed, Icons.Filled.CheckCircle,
            statusLabel ?: "Pembayaran Ditolak",
            adminNote ?: "Pembayaran ditolak admin.",
        )
        "cancelled" -> Quint(
            DangerSurface, DangerRed, Icons.Filled.CheckCircle,
            statusLabel ?: "Dibatalkan",
            "Pesanan ini telah dibatalkan.",
        )
        else -> Quint(
            WarningSurface, WarningOrange, Icons.Filled.HourglassTop,
            statusLabel ?: "Menunggu Pembayaran",
            if (qrisDinamis) {
                "Scan QRIS di bawah. Pembayaran dikonfirmasi otomatis setelah transfer berhasil."
            } else {
                "Lakukan pembayaran sesuai instruksi di bawah."
            },
        )
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(fg),
                contentAlignment = Alignment.Center
            ) { Icon(icon, contentDescription = null, tint = OnAccent, modifier = Modifier.size(24.dp)) }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, color = fg, fontSize = 13.sp)
                Text(desc, color = TextSecondary, fontSize = 12.sp)
            }
        }
    }
}

private data class Quint(
    val bg: Color,
    val fg: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val desc: String,
)

@Composable
private fun BankCard(bank: String, number: String, owner: String) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BrandBlueSurface)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val iconKey = when {
            bank.contains("BCA", ignoreCase = true) -> "bca"
            bank.contains("DANA", ignoreCase = true) -> "dana"
            else -> "bank"
        }
        com.ribminet.obill.ui.components.PaymentBrandIcon(
            iconKey = iconKey,
            fallbackText = "BANK",
            modifier = Modifier.size(40.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(bank, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
            Text(number, color = BrandBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("a.n. $owner", color = TextSecondary, fontSize = 12.sp)
        }
        val cleanNumber = number.filter { it.isDigit() }
        if (cleanNumber.isNotBlank()) {
            Spacer(modifier = Modifier.width(8.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(BrandBlue)
                    .clickableNoRipple {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Nomor Rekening", cleanNumber))
                        Toast.makeText(context, "Nomor rekening disalin", Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = "Salin", tint = OnAccent, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Salin", color = OnAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun CashCard(label: String, note: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BrandBlueSurface)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.PointOfSale, contentDescription = null, tint = BrandBlue)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
            Text(note?.takeIf { it.isNotBlank() } ?: "Silakan bayar tunai di kantor lalu tekan konfirmasi.", color = TextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun QrCard(label: String, url: String?, merchant: String?, note: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BrandBlueSurface)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.QrCode2, contentDescription = null, tint = BrandBlue)
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = "QR",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(220.dp).clip(RoundedCornerShape(8.dp))
            )
        } else {
            Box(modifier = Modifier.size(220.dp).clip(RoundedCornerShape(8.dp)).background(Divider), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.QrCode2, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(80.dp))
            }
        }
        if (!merchant.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(merchant, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
        }
        if (!note.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(note, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp))
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String, valueColor: Color = TextPrimary) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondary, fontSize = 13.sp)
        Text(value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}
