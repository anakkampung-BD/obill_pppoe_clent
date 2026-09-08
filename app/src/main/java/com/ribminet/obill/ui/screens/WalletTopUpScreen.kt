package com.ribminet.obill.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ribminet.obill.WalletTopUpStep
import com.ribminet.obill.WalletViewModel
import com.ribminet.obill.data.remote.WalletPaymentDto
import com.ribminet.obill.ui.components.AppCard
import com.ribminet.obill.ui.components.AppTopBar
import com.ribminet.obill.ui.components.PrimaryButton
import com.ribminet.obill.ui.components.QrisStandardCard
import com.ribminet.obill.ui.components.SecondaryButton
import com.ribminet.obill.ui.components.clickableNoRipple
import com.ribminet.obill.ui.theme.BrandBlue
import com.ribminet.obill.ui.theme.BrandBlueSurface
import com.ribminet.obill.ui.theme.CardWhite
import com.ribminet.obill.ui.theme.DangerRed
import com.ribminet.obill.ui.theme.DangerSurface
import com.ribminet.obill.ui.theme.Divider
import com.ribminet.obill.ui.theme.IconChipBlue
import com.ribminet.obill.ui.theme.SuccessGreen
import com.ribminet.obill.ui.theme.SuccessSurface
import com.ribminet.obill.ui.theme.TextPrimary
import com.ribminet.obill.ui.theme.TextSecondary
import com.ribminet.obill.ui.theme.WarningOrange
import com.ribminet.obill.ui.theme.WarningSurface
import com.ribminet.obill.util.rupiah
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun WalletTopUpScreen(
    vm: WalletViewModel,
    onBack: () -> Unit,
    onBalanceUpdated: (Long) -> Unit = {},
) {
    LaunchedEffect(Unit) {
        vm.onBalanceUpdated = onBalanceUpdated
        vm.loadAndResume()
    }
    DisposableEffect(Unit) {
        onDispose { vm.onBalanceUpdated = null }
    }

    Column(modifier = Modifier.fillMaxSize().background(CardWhite)) {
        AppTopBar(
            title = when (vm.step) {
                WalletTopUpStep.AMOUNT -> "Top Up Saldo"
                WalletTopUpStep.PAYMENT -> "Bayar QRIS"
                WalletTopUpStep.SUCCESS -> "Top Up Berhasil"
            },
            onBack = {
                when (vm.step) {
                    WalletTopUpStep.PAYMENT -> {
                        // tetap di payment; back = keluar screen (poll tetap jalan via prefs resume)
                        onBack()
                    }
                    WalletTopUpStep.SUCCESS -> {
                        vm.done()
                        onBack()
                    }
                    else -> onBack()
                }
            },
        )

        when {
            vm.loading && vm.step == WalletTopUpStep.AMOUNT && vm.balance == 0L && vm.payment == null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BrandBlue)
                }
            }
            vm.step == WalletTopUpStep.AMOUNT -> WalletAmountStep(vm)
            vm.step == WalletTopUpStep.PAYMENT -> WalletPaymentStep(vm)
            vm.step == WalletTopUpStep.SUCCESS -> WalletSuccessStep(vm, onDone = {
                vm.done()
                onBack()
            })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WalletAmountStep(vm: WalletViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AppCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(IconChipBlue),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.AccountBalanceWallet, null, tint = BrandBlue, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Saldo saat ini", fontSize = 12.sp, color = TextSecondary)
                    Text(
                        vm.balanceFormatted?.takeIf { it.isNotBlank() } ?: rupiah(vm.balance),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = BrandBlue,
                    )
                }
            }
        }

        Text("Pilih nominal", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            vm.presets.forEach { amount ->
                val selected = vm.selectedPreset == amount
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) BrandBlue else BrandBlueSurface)
                        .border(
                            1.dp,
                            if (selected) BrandBlue else Divider,
                            RoundedCornerShape(12.dp),
                        )
                        .clickableNoRipple { vm.selectPreset(amount) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Text(
                        rupiah(amount),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) CardWhite else BrandBlue,
                    )
                }
            }
        }

        Text("Atau masukkan nominal lain", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Divider, RoundedCornerShape(12.dp))
                .background(CardWhite)
                .padding(horizontal = 14.dp, vertical = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Rp ", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                BasicTextField(
                    value = vm.amountText,
                    onValueChange = vm::onAmountTextChange,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                    ),
                    cursorBrush = SolidColor(BrandBlue),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (vm.amountText.isEmpty()) {
                            Text("Minimal ${rupiah(vm.minTopup)}", color = TextSecondary, fontSize = 15.sp)
                        }
                        inner()
                    },
                )
            }
        }

        vm.info?.let { Banner(it, BrandBlueSurface, BrandBlue) }
        vm.error?.let { Banner(it, DangerSurface, DangerRed) }

        PrimaryButton(
            text = if (vm.submitting) "Memproses…" else "Bayar",
            enabled = !vm.submitting && vm.qrisEnabled,
            onClick = { vm.submitTopup() },
        )
        if (!vm.qrisEnabled) {
            Text("QRIS top-up sedang tidak tersedia.", fontSize = 12.sp, color = DangerRed)
        }
    }
}

@Composable
private fun WalletPaymentStep(vm: WalletViewModel) {
    val pay = vm.payment
    val context = LocalContext.current
    var downloading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (vm.isWaitingPayment) {
            WalletCountdownTimer(expiresAtMs = vm.paymentExpiresAtMs)
        }

        WalletQrisPanel(pay)

        when {
            vm.isPaid -> {
                StatusLine("Pembayaran diterima. Memperbarui saldo…", WarningSurface, WarningOrange, Icons.Filled.HourglassTop)
            }
            vm.isWaitingPayment -> {
                StatusLine("Menunggu pembayaran QRIS", WarningSurface, WarningOrange, Icons.Filled.QrCode2)
            }
        }

        pay?.amountBase?.let { base ->
            AppCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Dikredit ke saldo", fontSize = 12.sp, color = TextSecondary)
                    Text(rupiah(base), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
                }
                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Kode unik", fontSize = 12.sp, color = TextSecondary)
                    Text("${pay.uniqueCode ?: "-"}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
                }
                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total bayar", fontSize = 12.sp, color = TextSecondary)
                    Text(
                        pay.payAmountFormatted?.takeIf { it.isNotBlank() } ?: rupiah(pay.payAmount ?: 0L),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = BrandBlue,
                    )
                }
            }
        }

        if (!pay?.qrisImageUrl.isNullOrBlank()) {
            SecondaryButton(
                text = if (downloading) "Mengunduh…" else "Download QRIS",
                onClick = {
                    val url = pay?.qrisImageUrl ?: return@SecondaryButton
                    if (downloading) return@SecondaryButton
                    downloading = true
                    scope.launch {
                        val ok = saveWalletQrisToGallery(context, url, vm.topupCode)
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

        vm.topupCode?.let {
            Text("Kode: $it", fontSize = 11.sp, color = TextSecondary)
        }

        if (vm.isWaitingPayment) {
            Text(
                "Scan QRIS di aplikasi e-wallet/bank Anda. Nominal harus sama persis. Status diperbarui otomatis.",
                fontSize = 11.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            )
        }

        vm.error?.let { Banner(it, DangerSurface, DangerRed) }

        SecondaryButton(
            text = if (vm.submitting) "Membatalkan…" else "Batalkan Top Up",
            onClick = {
                if (!vm.submitting && vm.isWaitingPayment) vm.cancelTopup()
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun WalletSuccessStep(vm: WalletViewModel, onDone: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Filled.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(64.dp))
        Text("Top Up Berhasil", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = SuccessGreen)
        Text(
            "Saldo Anda telah ditambahkan.",
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )

        AppCard {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Saldo sekarang", fontSize = 13.sp, color = TextSecondary)
                Text(
                    vm.balanceFormatted?.takeIf { it.isNotBlank() } ?: rupiah(vm.balance),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = BrandBlue,
                )
            }
            vm.lastTopup?.amountBase?.let { base ->
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Nominal top-up", fontSize = 12.sp, color = TextSecondary)
                    Text(rupiah(base), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
                }
            }
            vm.topupCode?.let { code ->
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Kode", fontSize = 12.sp, color = TextSecondary)
                    Text(code, fontSize = 11.sp, color = TextPrimary)
                }
            }
        }

        PrimaryButton(text = "Selesai", onClick = onDone)
    }
}

@Composable
private fun WalletQrisPanel(pay: WalletPaymentDto?) {
    QrisStandardCard(
        qrisImageUrl = pay?.qrisImageUrl,
        payAmount = pay?.payAmount,
        merchantName = pay?.merchantName?.takeIf { it.isNotBlank() } ?: "Onesky Internet",
        nmid = pay?.nmid?.takeIf { it.isNotBlank() } ?: "ID1024325805181",
        terminalId = pay?.terminalId?.takeIf { it.isNotBlank() } ?: "A01",
    )
}

@Composable
private fun WalletCountdownTimer(expiresAtMs: Long?) {
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
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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

@Composable
private fun StatusLine(
    text: String,
    bg: androidx.compose.ui.graphics.Color,
    fg: androidx.compose.ui.graphics.Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(8.dp))
        Text(text, color = fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Banner(text: String, bg: androidx.compose.ui.graphics.Color, fg: androidx.compose.ui.graphics.Color) {
    Text(
        text,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .padding(12.dp),
        color = fg,
        fontSize = 12.sp,
    )
}

private suspend fun saveWalletQrisToGallery(
    context: android.content.Context,
    imageUrl: String,
    topupCode: String?,
): Boolean = withContext(Dispatchers.IO) {
    try {
        val connection = java.net.URL(imageUrl).openConnection()
        connection.connectTimeout = 15_000
        connection.readTimeout = 20_000
        connection.getInputStream().use { input ->
            val bytes = input.readBytes()
            val fileName = "qris_wallet_${topupCode ?: System.currentTimeMillis()}.png"
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
