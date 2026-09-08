package com.ribminet.obill.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.ribminet.obill.PpobStep
import com.ribminet.obill.PpobTab
import com.ribminet.obill.PpobViewModel
import com.ribminet.obill.data.remote.PpobPaymentDto
import com.ribminet.obill.data.remote.PpobProductDto
import com.ribminet.obill.data.remote.PpobTransactionDto
import com.ribminet.obill.data.remote.displayName
import com.ribminet.obill.data.remote.displayPrice
import com.ribminet.obill.data.remote.displayToken
import com.ribminet.obill.data.remote.isPending
import com.ribminet.obill.data.remote.isPln
import com.ribminet.obill.data.remote.resolvedBrand
import com.ribminet.obill.data.remote.resolvedPaymentBreakdown
import com.ribminet.obill.data.remote.resolvedBreakdown
import com.ribminet.obill.data.remote.resolvedPayAmount
import com.ribminet.obill.data.remote.statusLabel
import com.ribminet.obill.ui.components.AppCard
import com.ribminet.obill.ui.components.AppTextField
import com.ribminet.obill.ui.components.AppTopBar
import com.ribminet.obill.ui.components.ConfirmDialog
import com.ribminet.obill.ui.components.ConfirmRequest
import com.ribminet.obill.ui.components.PpobAmountBreakdown
import com.ribminet.obill.ui.components.PrimaryButton
import com.ribminet.obill.ui.components.QrisStandardCard
import com.ribminet.obill.ui.components.SecondaryButton
import com.ribminet.obill.ui.components.SweetAlertDialog
import com.ribminet.obill.ui.components.clickableNoRipple
import com.ribminet.obill.ui.theme.BrandBlue
import com.ribminet.obill.ui.theme.BrandBlueSurface
import com.ribminet.obill.ui.theme.DangerRed
import com.ribminet.obill.ui.theme.DangerSurface
import com.ribminet.obill.ui.theme.SuccessGreen
import com.ribminet.obill.ui.theme.SuccessSurface
import com.ribminet.obill.ui.theme.TextPrimary
import com.ribminet.obill.ui.theme.TextSecondary
import com.ribminet.obill.ui.theme.WarningOrange
import com.ribminet.obill.ui.theme.WarningSurface
import com.ribminet.obill.util.rupiah

@Composable
fun PpobScreen(
    vm: PpobViewModel,
    onBack: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenDetail: (String) -> Unit,
) {
    LaunchedEffect(Unit) { vm.checkActiveUnpaidAndResume() }
    var confirmCancel by remember { mutableStateOf<ConfirmRequest?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(
                title = when (vm.step) {
                    PpobStep.TYPE -> "PPOB"
                    PpobStep.CATEGORY -> if (vm.tab == PpobTab.PREPAID) "Prabayar" else "Pascabayar"
                    PpobStep.PRODUCT -> vm.selectedCategory ?: "Produk"
                    PpobStep.INPUT -> "Nomor tujuan"
                    PpobStep.PAYMENT -> "Bayar QRIS"
                    PpobStep.SUCCESS -> "Pembayaran Berhasil"
                },
                onBack = {
                    if (vm.step == PpobStep.TYPE) onBack() else vm.back()
                },
                trailingIcon = if (vm.step == PpobStep.TYPE) Icons.Filled.History else null,
                onTrailingClick = if (vm.step == PpobStep.TYPE) onOpenHistory else null,
            )

            if (vm.checkingUnpaid && vm.step == PpobStep.TYPE) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BrandBlue)
                }
            } else {
                when (vm.step) {
                    PpobStep.TYPE -> PpobTypeStep(vm)
                    PpobStep.CATEGORY -> PpobCategoryStep(vm)
                    PpobStep.PRODUCT -> PpobProductStep(vm)
                    PpobStep.INPUT -> PpobInputStep(vm)
                    PpobStep.PAYMENT -> PpobPaymentStep(
                        vm = vm,
                        onRequestCancel = {
                            confirmCancel = ConfirmRequest(
                                title = "Batalkan Transaksi?",
                                message = "QRIS transaksi ini akan dibatalkan. Anda dapat membuat transaksi baru setelahnya.",
                                confirmText = "Ya, Batalkan",
                                cancelText = "Tidak",
                                icon = Icons.Filled.HelpOutline,
                                accent = DangerRed,
                                onConfirm = { vm.cancelActivePayment() },
                            )
                        },
                    )
                    PpobStep.SUCCESS -> PpobSuccessStep(vm, onOpenDetail)
                }
            }
        }

        ConfirmDialog(request = confirmCancel, onDismiss = { confirmCancel = null })
        SweetAlertDialog(alert = vm.alert, onConfirm = { vm.dismissAlert() })
    }
}

@Composable
private fun PpobTypeStep(vm: PpobViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Pilih jenis transaksi",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextPrimary,
                modifier = Modifier.fillMaxWidth(),
            )
            TypeCard(
                title = "Prabayar",
                subtitle = "Pulsa, paket data, token PLN, DANA, game, dll.",
                icon = Icons.Filled.SimCard,
                onClick = { vm.selectTab(PpobTab.PREPAID) },
            )
            TypeCard(
                title = "Pascabayar",
                subtitle = "Tagihan listrik, PDAM, internet, TV, dll.",
                icon = Icons.Filled.Bolt,
                onClick = { vm.selectTab(PpobTab.POSTPAID) },
            )
        }
    }
}

@Composable
private fun TypeCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickableNoRipple(onClick),
        contentPadding = PaddingValues(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(BrandBlueSurface),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = BrandBlue)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(subtitle, fontSize = 12.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun PpobCategoryStep(vm: PpobViewModel) {
    when {
        vm.loading && vm.categories.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BrandBlue)
        }
        else -> Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Pilih kategori produk", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                vm.error?.let { MessageBanner(it, DangerSurface, DangerRed) }
                vm.categories.forEach { cat ->
                    AppCard(
                        modifier = Modifier.fillMaxWidth().clickableNoRipple { vm.selectCategory(cat) },
                        contentPadding = PaddingValues(14.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(categoryIcon(cat), null, tint = BrandBlue, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(cat, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        }
                    }
                }
                if (!vm.loading && vm.categories.isEmpty() && vm.error == null) {
                    Text("Kategori kosong.", color = TextSecondary)
                }
                SecondaryButton(
                    text = if (vm.loading) "Memuat…" else "Muat ulang katalog",
                    onClick = { vm.refreshCatalog() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun PpobProductStep(vm: PpobViewModel) {
    val products = vm.categoryProducts
    val brandOrder = products.map { it.resolvedBrand().ifBlank { "Lainnya" } }.distinct()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Pilih produk", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
        brandOrder.forEach { brand ->
            item(key = "hdr-$brand") {
                Text(
                    brand,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = BrandBlue,
                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
                )
            }
            val brandProducts = products.filter {
                it.resolvedBrand().ifBlank { "Lainnya" }.equals(brand, ignoreCase = true)
            }
            items(brandProducts, key = { it.buyerSkuCode ?: "${brand}-${it.hashCode()}" }) { product ->
                AppCard(
                    modifier = Modifier.fillMaxWidth().clickableNoRipple { vm.selectProduct(product) },
                    contentPadding = PaddingValues(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(product.displayName(), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            product.type?.takeIf { it.isNotBlank() }?.let {
                                Text(it, fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                        Text(product.displayPrice(), fontWeight = FontWeight.Bold, color = BrandBlue, fontSize = 14.sp)
                    }
                }
            }
        }
        if (products.isEmpty()) {
            item { Text("Tidak ada produk di kategori ini.", color = TextSecondary) }
        }
    }
}

@Composable
private fun PpobInputStep(vm: PpobViewModel) {
    val product = vm.selectedProduct ?: return
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SelectedProductSummary(product)
        AppTextField(
            value = vm.customerNo,
            onValueChange = { vm.customerNo = it.filter { ch -> ch.isDigit() } },
            label = vm.customerNoFieldLabel,
            placeholder = vm.customerNoFieldHint,
            keyboardType = KeyboardType.Number,
        )
        vm.error?.let { MessageBanner(it, DangerSurface, DangerRed) }
        PrimaryButton(
            text = if (vm.submitting) "Memproses…" else "Bayar sekarang",
            enabled = !vm.submitting && vm.customerNo.isNotBlank(),
            onClick = { vm.onInputContinue() },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PpobPaymentStep(vm: PpobViewModel, onRequestCancel: () -> Unit) {
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
            PaymentCountdownTimer(expiresAtMs = vm.paymentExpiresAtMs)
        }

        QrisPanel(pay)

        when {
            vm.isPaid && !vm.isPaymentSuccess -> {
                StatusPill("Pembayaran diterima. Memproses produk…", WarningSurface, WarningOrange, Icons.Filled.HourglassTop)
            }
            vm.isWaitingPayment -> {
                StatusPill("Menunggu pembayaran QRIS", WarningSurface, WarningOrange, Icons.Filled.QrCode2)
            }
            !vm.isPaymentSuccess -> {
                StatusPill(vm.paymentStatusLabel ?: "Memproses…", BrandBlueSurface, BrandBlue, Icons.Filled.HourglassTop)
            }
        }

        if (pay?.qrisImageUrl.isNullOrBlank() && vm.isWaitingPayment) {
            Text(
                "QRIS tidak tersedia di perangkat ini. Batalkan transaksi lalu buat ulang jika perlu.",
                fontSize = 11.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            )
        }

        if (!pay?.qrisImageUrl.isNullOrBlank()) {
            SecondaryButton(
                text = if (downloading) "Mengunduh…" else "Download QRIS",
                onClick = {
                    val url = pay?.qrisImageUrl ?: return@SecondaryButton
                    if (downloading) return@SecondaryButton
                    downloading = true
                    scope.launch {
                        val ok = saveQrisImageToGallery(context, url, vm.paymentRefId)
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

        vm.paymentRefId?.let {
            Text("Ref: $it", fontSize = 11.sp, color = TextSecondary)
        }

        if (vm.isWaitingPayment) {
            Text(
                "Scan QRIS di aplikasi e-wallet/bank Anda. Status diperbarui otomatis.",
                fontSize = 11.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            )
        }

        vm.error?.let { MessageBanner(it, DangerSurface, DangerRed) }

        SecondaryButton(
            text = if (vm.submitting) "Membatalkan…" else "Batalkan Transaksi",
            onClick = {
                if (!vm.submitting && vm.isWaitingPayment) onRequestCancel()
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PpobSuccessStep(vm: PpobViewModel, onOpenDetail: (String) -> Unit) {
    val tx = vm.lastTransaction
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Filled.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(64.dp))
        Text("Pembayaran Berhasil", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = SuccessGreen)
        Text("Transaksi PPOB Anda sudah diproses.", fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center)

        if (tx != null) {
            SuccessReceipt(tx)
        } else {
            vm.paymentRefId?.let { Text("Ref: $it", fontSize = 12.sp, color = TextSecondary) }
        }

        tx?.refId?.let { ref ->
            SecondaryButton(
                text = "Lihat detail",
                onClick = { onOpenDetail(ref) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        PrimaryButton(
            text = "Transaksi baru",
            onClick = { vm.startNewTransaction() },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun PpobHistoryScreen(
    vm: PpobViewModel,
    onBack: () -> Unit,
    onOpenDetail: (String) -> Unit,
) {
    LaunchedEffect(Unit) { vm.loadHistory() }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "Riwayat PPOB", onBack = onBack)
        when {
            vm.loading && vm.history.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandBlue)
            }
            vm.history.isEmpty() -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.SimCard, null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Belum ada transaksi PPOB", fontWeight = FontWeight.Bold)
                }
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(vm.history, key = { it.refId ?: it.hashCode() }) { tx ->
                    HistoryRow(tx) { tx.refId?.let(onOpenDetail) }
                }
            }
        }
    }
}

@Composable
fun PpobTransactionDetailScreen(
    refId: String,
    vm: PpobViewModel,
    onBack: () -> Unit,
) {
    LaunchedEffect(refId) {
        vm.clearDetail()
        vm.loadDetail(refId, autoPoll = true)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "Detail Transaksi", onBack = onBack)
        when {
            vm.detailLoading && vm.detailTransaction == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandBlue)
            }
            vm.detailTransaction == null -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(vm.error ?: "Transaksi tidak ditemukan", color = TextSecondary)
            }
            else -> {
                val tx = vm.detailTransaction!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item { SuccessReceipt(tx) }
                    if (tx.isPending() || vm.detailPolling) {
                        item {
                            MessageBanner("Menunggu status akhir…", WarningSurface, WarningOrange)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedProductSummary(product: PpobProductDto) {
    AppCard {
        Column(modifier = Modifier.padding(4.dp)) {
            Text(product.displayName(), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(
                listOfNotNull(product.brand, product.category).joinToString(" · "),
                fontSize = 12.sp,
                color = TextSecondary,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(product.displayPrice(), fontWeight = FontWeight.Bold, color = BrandBlue, fontSize = 16.sp)
        }
    }
}

@Composable
private fun PaymentCountdownTimer(expiresAtMs: Long?) {
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
private fun QrisPanel(pay: PpobPaymentDto?) {
    val payAmount = pay?.resolvedBreakdown()?.payAmount ?: pay?.payAmount
    QrisStandardCard(
        qrisImageUrl = pay?.qrisImageUrl,
        payAmount = payAmount,
        merchantName = pay?.merchantName?.takeIf { it.isNotBlank() } ?: "D'BESTIE CAFE",
        nmid = pay?.nmid?.takeIf { it.isNotBlank() } ?: "ID1024325805181",
        terminalId = pay?.terminalId?.takeIf { it.isNotBlank() } ?: "A01",
    )
}

private suspend fun saveQrisImageToGallery(
    context: android.content.Context,
    imageUrl: String,
    refId: String?,
): Boolean = withContext(Dispatchers.IO) {
    try {
        val connection = java.net.URL(imageUrl).openConnection()
        connection.connectTimeout = 15_000
        connection.readTimeout = 20_000
        connection.getInputStream().use { input ->
            val bytes = input.readBytes()
            val fileName = "qris_${refId ?: System.currentTimeMillis()}.png"
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
private fun StatusPill(text: String, bg: Color, fg: Color, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, color = fg, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
private fun SuccessReceipt(tx: PpobTransactionDto) {
    val token = tx.displayToken()
    val breakdown = tx.resolvedPaymentBreakdown()
    val paid = breakdown?.payAmount ?: tx.resolvedPayAmount()
    AppCard {
        Column(modifier = Modifier.padding(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            tx.refId?.let { Text("Ref: $it", fontSize = 11.sp, color = TextSecondary) }
            Text(tx.productName ?: "-", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text("Status: ${tx.statusLabel()}", fontSize = 13.sp, color = if (tx.isSuccess == true) SuccessGreen else TextPrimary)
            tx.customerNo?.let { Text("Tujuan: $it", fontSize = 12.sp, color = TextSecondary) }
            tx.customerName?.takeIf { it.isNotBlank() }?.let {
                Text("Nama: $it", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(6.dp))
            PpobAmountBreakdown(
                breakdown = breakdown,
                fallbackPayAmount = paid,
            )
            if (breakdown == null) {
                tx.sellPrice?.let { Text("Harga: ${rupiah(it)}", fontSize = 13.sp) }
                paid?.let { Text("Dibayar: ${rupiah(it)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
            }
            if (!token.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SuccessSurface)
                        .padding(12.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            if (tx.isPln()) "TOKEN LISTRIK" else "SN / Kode",
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen,
                            fontSize = 12.sp,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(token, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center)
                    }
                }
            }
            tx.message?.let { Text(it, fontSize = 12.sp, color = TextSecondary) }
            tx.createdAt?.let { Text(it, fontSize = 11.sp, color = TextSecondary) }
        }
    }
}

@Composable
private fun HistoryRow(tx: PpobTransactionDto, onClick: () -> Unit) {
    val statusColor = when {
        tx.isSuccess == true -> SuccessGreen
        tx.isPending() -> WarningOrange
        else -> TextSecondary
    }
    AppCard(modifier = Modifier.clickableNoRipple(onClick)) {
        Row(modifier = Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(tx.productName ?: tx.buyerSkuCode ?: "-", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(tx.createdAt ?: "", fontSize = 11.sp, color = TextSecondary)
                Text(tx.statusLabel(), fontSize = 12.sp, color = statusColor)
            }
            Text(
                (tx.resolvedPayAmount() ?: tx.sellPrice)?.let { rupiah(it) } ?: "-",
                fontWeight = FontWeight.Bold,
                color = BrandBlue,
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun MessageBanner(message: String, bg: Color, fg: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .padding(12.dp),
    ) {
        Text(message, color = fg, fontSize = 13.sp)
    }
}

private fun categoryIcon(category: String): ImageVector = when {
    category.contains("pln", true) -> Icons.Filled.Bolt
    category.contains("money", true) || category.contains("dana", true) || category.contains("e-money", true) ->
        Icons.Filled.AccountBalanceWallet
    else -> Icons.Filled.SimCard
}
