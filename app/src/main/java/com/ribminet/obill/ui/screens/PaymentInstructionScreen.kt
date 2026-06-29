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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ribminet.obill.data.remote.OrderDto
import com.ribminet.obill.ui.components.AppCard
import com.ribminet.obill.ui.components.AppTopBar
import com.ribminet.obill.ui.components.ConfirmDialog
import com.ribminet.obill.ui.components.ConfirmRequest
import com.ribminet.obill.ui.components.PrimaryButton
import com.ribminet.obill.ui.components.SecondaryButton
import com.ribminet.obill.ui.components.AppTextField
import com.ribminet.obill.ui.components.clickableNoRipple
import kotlinx.coroutines.delay
import com.ribminet.obill.ui.theme.BrandBlue
import com.ribminet.obill.ui.theme.BrandBlueSurface
import com.ribminet.obill.ui.theme.DangerRed
import com.ribminet.obill.ui.theme.DangerSurface
import com.ribminet.obill.ui.theme.Divider
import com.ribminet.obill.ui.theme.OnAccent
import com.ribminet.obill.ui.theme.ScreenBackground
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
) {
    if (order == null) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(title = "Instruksi Pembayaran", onBack = onBack)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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

    // Polling otomatis saat menunggu verifikasi admin
    if (status == "awaiting_confirmation") {
        LaunchedEffect(order.id) {
            while (true) {
                delay(8000)
                onRefreshStatus()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "Instruksi Pembayaran", onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            StatusBanner(status, order.statusLabel, order.adminNote)
            Spacer(Modifier.height(16.dp))

            AppCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(order.orderNo ?: "-", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                    Text(if (order.orderType == "upgrade") "Upgrade" else "Perpanjang", color = TextSecondary, fontSize = 12.sp)
                }
                Spacer(Modifier.height(12.dp))
                InfoLine("Nominal", rupiah(order.amount ?: 0L), BrandBlue)
                InfoLine("Paket", order.toProfileName ?: "-")
                InfoLine("Metode", instr?.label ?: order.paymentMethod ?: "-")
            }

            if (instr != null && (status == "pending")) {
                Spacer(Modifier.height(16.dp))
                Text("Instruksi Pembayaran", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                Spacer(Modifier.height(10.dp))
                when (instr.type) {
                    "bank_transfer" -> instr.accounts?.forEach { acc ->
                        BankCard(acc.bank ?: "-", acc.accountNumber ?: "-", acc.accountName ?: "-")
                        Spacer(Modifier.height(10.dp))
                    }
                    "qris", "qr" -> QrCard(instr.label ?: "QRIS", instr.qrImageUrl, instr.merchantName, instr.note)
                    "cash" -> CashCard(instr.label ?: "Cash", instr.note)
                    else -> if (!instr.note.isNullOrBlank()) CashCard(instr.label ?: "Pembayaran", instr.note)
                }
            }

            if (status == "pending") {
                Spacer(Modifier.height(16.dp))
                Text("Konfirmasi Pembayaran", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                AppTextField(
                    value = refNo,
                    onValueChange = { refNo = it },
                    placeholder = "No. referensi transfer (opsional)",
                    label = "No. Referensi",
                )
            }

            if (!error.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(error, color = DangerRed, fontSize = 12.sp)
            }
        }

        Box(modifier = Modifier.background(ScreenBackground).padding(16.dp)) {
            Column {
                when (status) {
                    "pending" -> {
                        PrimaryButton(
                            text = if (submitting) "Memproses..." else "Saya Sudah Bayar",
                            enabled = !submitting,
                            onClick = {
                                confirm = ConfirmRequest(
                                    title = "Konfirmasi Pembayaran?",
                                    message = "Pastikan Anda sudah melakukan pembayaran. Konfirmasi ini akan dikirim ke admin untuk diverifikasi.",
                                    confirmText = "Ya, Sudah Bayar",
                                    onConfirm = { onConfirm(refNo.ifBlank { null }) },
                                )
                            }
                        )
                        Spacer(Modifier.height(10.dp))
                        SecondaryButton(
                            text = "Batalkan Pesanan",
                            color = DangerRed,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
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
                            onClick = onRefreshStatus
                        )
                        Spacer(Modifier.height(10.dp))
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
    }

    ConfirmDialog(request = confirm, onDismiss = { confirm = null })
    com.ribminet.obill.ui.components.SweetAlertDialog(alert = alert, onConfirm = onDismissAlert)
}

@Composable
private fun StatusBanner(status: String, statusLabel: String?, adminNote: String?) {
    val (bg, fg, icon, title, desc) = when (status) {
        "awaiting_confirmation" -> Quint(WarningSurface, WarningOrange, Icons.Filled.HourglassTop, statusLabel ?: "Menunggu Konfirmasi", "Pembayaran Anda sedang diverifikasi oleh admin. Halaman akan diperbarui otomatis.")
        "confirmed", "paid" -> Quint(SuccessSurface, SuccessGreen, Icons.Filled.CheckCircle, statusLabel ?: "Pembayaran Berhasil", "Pembayaran terverifikasi. Layanan Anda telah diaktifkan.")
        "rejected" -> Quint(DangerSurface, DangerRed, Icons.Filled.CheckCircle, statusLabel ?: "Pembayaran Ditolak", adminNote ?: "Pembayaran ditolak admin.")
        "cancelled" -> Quint(DangerSurface, DangerRed, Icons.Filled.CheckCircle, statusLabel ?: "Dibatalkan", "Pesanan ini telah dibatalkan.")
        else -> Quint(WarningSurface, WarningOrange, Icons.Filled.HourglassTop, statusLabel ?: "Menunggu Pembayaran", "Lakukan pembayaran sesuai instruksi di bawah.")
    }
    Box(
        modifier = Modifier
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
            Spacer(Modifier.width(12.dp))
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
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(bank, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
            Text(number, color = BrandBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("a.n. $owner", color = TextSecondary, fontSize = 12.sp)
        }
        val cleanNumber = number.filter { it.isDigit() }
        if (cleanNumber.isNotBlank()) {
            Spacer(Modifier.width(8.dp))
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
                Spacer(Modifier.width(6.dp))
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
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
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
            Spacer(Modifier.width(8.dp))
            Text(label, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
        }
        Spacer(Modifier.height(12.dp))
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = "QR",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(220.dp).clip(RoundedCornerShape(8.dp))
            )
        } else {
            Box(Modifier.size(220.dp).clip(RoundedCornerShape(8.dp)).background(Divider), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.QrCode2, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(80.dp))
            }
        }
        if (!merchant.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(merchant, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
        }
        if (!note.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
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
