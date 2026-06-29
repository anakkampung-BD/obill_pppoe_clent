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
import com.ribminet.obill.data.remote.OrderDto
import com.ribminet.obill.data.remote.formatDateTimeId
import com.ribminet.obill.ui.components.AppCard
import com.ribminet.obill.ui.components.AppTopBar
import com.ribminet.obill.ui.components.StatusBadge
import com.ribminet.obill.ui.components.clickableNoRipple
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
    orders: List<OrderDto>,
    loading: Boolean,
    onBack: () -> Unit,
    onOpen: (OrderDto) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "Riwayat Pesanan", onBack = onBack)
        when {
            loading && orders.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandBlue)
            }
            orders.isEmpty() -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.ReceiptLong, contentDescription = null, tint = TextSecondary, modifier = Modifier.height(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Belum Ada Pesanan", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Pesanan pembayaran dan upgrade paket akan tampil di sini.", color = TextSecondary, fontSize = 12.sp)
                }
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(orders) { order ->
                    OrderRow(order, onClick = { onOpen(order) })
                }
            }
        }
    }
}

@Composable
private fun OrderRow(order: OrderDto, onClick: () -> Unit) {
    Box(modifier = Modifier.clickableNoRipple(onClick)) {
        AppCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(order.orderNo ?: "-", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                StatusChip(order.status, order.statusLabel)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (order.orderType == "upgrade") "Upgrade Paket" else "Perpanjang Langganan", color = TextSecondary, fontSize = 12.sp)
                Text(rupiah(order.amount ?: 0L), fontWeight = FontWeight.Bold, color = BrandBlue, fontSize = 13.sp)
            }
            if (!order.createdAt.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(formatDateTimeId(order.createdAt), color = TextSecondary, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun StatusChip(status: String?, label: String?) {
    val (bg, fg) = when (status) {
        "confirmed", "paid" -> SuccessSurface to SuccessGreen
        "awaiting_confirmation" -> WarningSurface to WarningOrange
        "rejected", "cancelled" -> DangerSurface to DangerRed
        else -> WarningSurface to WarningOrange
    }
    val text = label ?: when (status) {
        "confirmed", "paid" -> "Berhasil"
        "awaiting_confirmation" -> "Menunggu Konfirmasi"
        "rejected" -> "Ditolak"
        "cancelled" -> "Dibatalkan"
        else -> "Menunggu Pembayaran"
    }
    StatusBadge(text, bg, fg)
}
