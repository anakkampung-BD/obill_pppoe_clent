package com.ribminet.obill.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ribminet.obill.data.remote.HistoryOrderSource
import com.ribminet.obill.data.remote.UnifiedHistoryItem
import com.ribminet.obill.data.remote.formatDateTimeId
import com.ribminet.obill.ui.components.AppCard
import com.ribminet.obill.ui.components.AppTopBar
import com.ribminet.obill.ui.components.StatusBadge
import com.ribminet.obill.ui.components.clickableNoRipple
import com.ribminet.obill.ui.guide.GuideTarget
import com.ribminet.obill.ui.guide.guideTarget
import com.ribminet.obill.ui.theme.BrandBlue
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
fun OrdersScreen(
    items: List<UnifiedHistoryItem>,
    loading: Boolean,
    error: String? = null,
    onBack: () -> Unit,
    onOpen: (UnifiedHistoryItem) -> Unit,
    guideMode: Boolean = false,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "Riwayat Pesanan", onBack = onBack)
        when {
            loading && items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandBlue)
            }
            items.isEmpty() -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .guideTarget(GuideTarget.ORDERS_LIST),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.ReceiptLong, contentDescription = null, tint = TextSecondary, modifier = Modifier.height(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (guideMode) "Daftar Pesanan" else "Belum Ada Pesanan",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 14.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        when {
                            guideMode ->
                                "Pesanan pembayaran PPPoE dan transaksi PPOB akan tampil di sini beserta statusnya. Ketuk item untuk membuka detail."
                            !error.isNullOrBlank() -> error
                            else ->
                                "Pesanan pembayaran PPPoE dan transaksi PPOB akan tampil di sini."
                        },
                        color = if (!error.isNullOrBlank() && !guideMode) DangerRed else TextSecondary,
                        fontSize = 12.sp,
                    )
                }
            }
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .guideTarget(GuideTarget.ORDERS_LIST),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items, key = { it.key }) { item ->
                    HistoryOrderRow(item, onClick = { onOpen(item) })
                }
            }
        }
    }
}

@Composable
private fun HistoryOrderRow(item: UnifiedHistoryItem, onClick: () -> Unit) {
    Box(modifier = Modifier.clickableNoRipple(onClick)) {
        AppCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        item.title,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        maxLines = 1,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    SourceChip(item.source)
                }
                StatusChip(item.source, item.status, item.statusLabel)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(item.subtitle, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Text(
                    item.amount?.let { rupiah(it) } ?: "-",
                    fontWeight = FontWeight.Bold,
                    color = BrandBlue,
                    fontSize = 13.sp,
                )
            }
            if (!item.createdAt.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(formatDateTimeId(item.createdAt), color = TextSecondary, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun SourceChip(source: HistoryOrderSource) {
    val label = when (source) {
        HistoryOrderSource.PPPOE -> "PPPoE"
        HistoryOrderSource.PPOB -> "PPOB"
    }
    Text(
        label,
        color = BrandBlue,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(com.ribminet.obill.ui.theme.BrandBlueSurface)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun StatusChip(source: HistoryOrderSource, status: String?, label: String?) {
    val normalized = status?.lowercase().orEmpty()
    val (bg, fg) = when (source) {
        HistoryOrderSource.PPPOE -> when (status) {
            "confirmed", "paid" -> SuccessSurface to SuccessGreen
            "awaiting_confirmation" -> WarningSurface to WarningOrange
            "rejected", "cancelled" -> DangerSurface to DangerRed
            else -> WarningSurface to WarningOrange
        }
        HistoryOrderSource.PPOB -> when {
            label.equals("Berhasil", true) || normalized in setOf("sukses", "paid", "success") ->
                SuccessSurface to SuccessGreen
            label?.contains("Batal", true) == true ||
                normalized.contains("cancel") || normalized.contains("gagal") ||
                normalized.contains("expired") ->
                DangerSurface to DangerRed
            else -> WarningSurface to WarningOrange
        }
    }
    val text = label ?: when (source) {
        HistoryOrderSource.PPPOE -> when (status) {
            "confirmed", "paid" -> "Berhasil"
            "awaiting_confirmation" -> "Menunggu Konfirmasi"
            "rejected" -> "Ditolak"
            "cancelled" -> "Dibatalkan"
            else -> "Menunggu Pembayaran"
        }
        HistoryOrderSource.PPOB -> status ?: "Diproses"
    }
    StatusBadge(text, bg, fg)
}
