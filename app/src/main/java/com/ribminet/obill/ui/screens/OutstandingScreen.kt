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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.ribminet.obill.data.remote.BillDto
import com.ribminet.obill.data.remote.OrderDto
import com.ribminet.obill.data.remote.formatDateId
import com.ribminet.obill.data.remote.installationFeeAmount
import com.ribminet.obill.data.remote.installationFeeLabel
import com.ribminet.obill.data.remote.isFirstActivation
import com.ribminet.obill.data.remote.latePenaltyAmount
import com.ribminet.obill.data.remote.payableTotal
import com.ribminet.obill.data.remote.subscriptionAmount
import com.ribminet.obill.ui.components.AppCard
import com.ribminet.obill.ui.components.AppTopBar
import com.ribminet.obill.ui.components.BillAmountBreakdown
import com.ribminet.obill.ui.components.PrimaryButton
import com.ribminet.obill.ui.components.SecondaryButton
import com.ribminet.obill.ui.components.StatusBadge
import com.ribminet.obill.ui.theme.BrandBlue
import com.ribminet.obill.ui.theme.BrandBlueSurface
import com.ribminet.obill.ui.theme.DangerRed
import com.ribminet.obill.ui.theme.SuccessGreen
import com.ribminet.obill.ui.theme.SuccessSurface
import com.ribminet.obill.ui.theme.TextPrimary
import com.ribminet.obill.ui.theme.TextSecondary
import com.ribminet.obill.ui.theme.WarningOrange
import com.ribminet.obill.ui.theme.WarningSurface
import com.ribminet.obill.util.rupiah

@Composable
fun OutstandingScreen(
    loading: Boolean,
    error: String?,
    bill: BillDto?,
    openOrder: OrderDto?,
    onBack: () -> Unit,
    onPay: () -> Unit,
    onViewOrder: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "Tagihan Berjalan", onBack = onBack)
        when {
            loading && bill == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandBlue)
            }
            error != null && bill == null -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(error, color = TextSecondary, fontSize = 13.sp)
            }
            bill == null -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("Tidak ada tagihan saat ini.", color = TextSecondary, fontSize = 13.sp)
            }
            else -> Content(bill, openOrder, onPay, onViewOrder)
        }
    }
}

@Composable
private fun Content(bill: BillDto, openOrder: OrderDto?, onPay: () -> Unit, onViewOrder: () -> Unit) {
    val overdue = bill.nextPayment?.isOverdue == true
    val firstActivation = bill.isFirstActivation()
    val subscriptionLabel = "Biaya berlangganan — ${bill.profileName ?: "Paket"}"
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (openOrder != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(WarningSurface)
                    .padding(14.dp)
            ) {
                Column {
                    Text("Pembayaran Sedang Diproses", fontWeight = FontWeight.Bold, color = WarningOrange, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Anda memiliki pesanan ${openOrder.orderNo ?: "-"} yang belum selesai.", color = TextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(10.dp))
                    SecondaryButton(text = "Lihat Pesanan", onClick = onViewOrder)
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        bill.pendingChange?.takeIf { it.pending == true }?.let { pc ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(WarningSurface)
                    .padding(14.dp)
            ) {
                Column {
                    Text("Perubahan Paket Terjadwal", fontWeight = FontWeight.Bold, color = WarningOrange, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${pc.fromProfileName ?: "-"} → ${pc.toProfileName ?: "-"}. Nominal tagihan sudah menyesuaikan harga paket baru. Paket aktif setelah pembayaran diverifikasi.",
                        color = TextSecondary, fontSize = 12.sp
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        if (firstActivation) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(BrandBlueSurface)
                    .padding(14.dp)
            ) {
                Column {
                    Text("Aktivasi Pertama", fontWeight = FontWeight.Bold, color = BrandBlue, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Pembayaran pertama mencakup biaya berlangganan dan biaya instalasi (sekali bayar).",
                        color = TextSecondary, fontSize = 12.sp
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        AppCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(SuccessSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.ReceiptLong, contentDescription = null, tint = SuccessGreen)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(bill.profileName ?: "Paket", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                    Text(
                        when {
                            firstActivation -> "Aktivasi pertama"
                            bill.pendingChange?.pending == true -> "Paket baru (periode berikutnya)"
                            else -> "Perpanjang langganan"
                        },
                        color = TextSecondary, fontSize = 12.sp
                    )
                }
                if (overdue) StatusBadge("Jatuh Tempo", WarningSurface, DangerRed)
            }
            Spacer(Modifier.height(16.dp))
            BillAmountBreakdown(
                subscriptionLabel = subscriptionLabel,
                subscriptionAmount = bill.subscriptionAmount(),
                installationLabel = bill.installationFeeLabel(),
                installationAmount = bill.installationFeeAmount(),
                latePenaltyAmount = bill.latePenaltyAmount(),
                totalAmount = bill.payableTotal(),
            )
            Spacer(Modifier.height(8.dp))
            InfoRow("Jatuh Tempo", formatDateId(bill.nextPayment?.dueDate))
            bill.previewRenewal?.let {
                InfoRow("Aktif Sampai (Setelah Bayar)", formatDateId(it.newExpiredAt))
                InfoRow("Tambahan Masa Aktif", "${it.extensionDays ?: 30} hari")
            }
        }

        Spacer(Modifier.height(20.dp))
        if (openOrder == null) {
            PrimaryButton(
                text = if (firstActivation) "Bayar Aktivasi" else "Bayar Tagihan",
                onClick = onPay,
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color = TextPrimary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondary, fontSize = 13.sp)
        Text(value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}
