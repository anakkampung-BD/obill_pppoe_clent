package com.ribminet.obill.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ribminet.obill.data.Bill
import com.ribminet.obill.data.PaymentChannel
import com.ribminet.obill.data.PaymentStatus
import com.ribminet.obill.ui.components.IconButtonRound
import com.ribminet.obill.ui.components.KeyValueRow
import com.ribminet.obill.ui.components.ShimmerBox
import com.ribminet.obill.ui.components.clickableNoRipple
import com.ribminet.obill.ui.theme.AppThemeState
import com.ribminet.obill.ui.theme.BrandBlue
import com.ribminet.obill.ui.theme.CardWhite
import com.ribminet.obill.ui.theme.HeroGreenBottom
import com.ribminet.obill.ui.theme.HeroGreenTop
import com.ribminet.obill.ui.theme.OnAccent
import com.ribminet.obill.ui.theme.DangerRed
import com.ribminet.obill.ui.theme.DangerSurface
import com.ribminet.obill.ui.theme.Divider
import com.ribminet.obill.ui.theme.SuccessGreen
import com.ribminet.obill.ui.theme.SuccessSurface
import com.ribminet.obill.ui.theme.TextPrimary
import com.ribminet.obill.ui.theme.TextSecondary
import com.ribminet.obill.ui.theme.WarningOrange
import com.ribminet.obill.ui.theme.WarningSurface
import com.ribminet.obill.util.rupiah

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentHistoryScreen(
    payments: List<Bill>,
    loading: Boolean,
    onDelete: (Bill) -> Unit,
    bottomBar: @Composable () -> Unit,
) {
    val tabs = listOf("Semua", "Menunggu", "Batal", "Selesai")
    var selectedTab by remember { mutableStateOf(0) }
    var detail by remember { mutableStateOf<Bill?>(null) }
    val sheet = rememberModalBottomSheetState()

    val filtered = when (selectedTab) {
        1 -> payments.filter { it.status == PaymentStatus.PENDING }
        2 -> payments.filter { it.status == PaymentStatus.CANCELLED }
        3 -> payments.filter { it.status == PaymentStatus.PAID }
        else -> payments
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(HeroGreenTop, HeroGreenBottom)))
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Riwayat Pembayaran", color = OnAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                IconButtonRound(Icons.Filled.Search, tint = OnAccent) {}
                IconButtonRound(if (AppThemeState.dark) Icons.Filled.DarkMode else Icons.Filled.LightMode, tint = OnAccent) { AppThemeState.dark = !AppThemeState.dark }
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                tabs.forEachIndexed { i, t ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickableNoRipple { selectedTab = i },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            t,
                            color = if (i == selectedTab) OnAccent else OnAccent.copy(alpha = 0.7f),
                            fontWeight = if (i == selectedTab) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                        Box(
                            modifier = Modifier
                                .height(3.dp)
                                .width(40.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (i == selectedTab) OnAccent else Color.Transparent)
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
        ) {
            if (loading && payments.isEmpty()) {
                items(4) {
                    PaymentCardLoading()
                    Spacer(Modifier.height(12.dp))
                }
            } else if (filtered.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Belum ada riwayat pembayaran", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            }
            items(filtered, key = { it.id }) { bill ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = {
                        if (it == SwipeToDismissBoxValue.EndToStart) { onDelete(bill); true } else false
                    }
                )
                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = false,
                    backgroundContent = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp))
                                .background(DangerRed)
                                .padding(end = 24.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.Delete, contentDescription = null, tint = OnAccent)
                                Text("Hapus", color = OnAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                ) {
                    PaymentCard(bill) { detail = bill }
                }
                Spacer(Modifier.height(12.dp))
            }
        }

        bottomBar()
    }

    detail?.let { bill ->
        ModalBottomSheet(onDismissRequest = { detail = null }, sheetState = sheet) {
            Column(Modifier.padding(20.dp)) {
                Text("Detail Pembayaran", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier
                    .fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                KeyValueRow("Kode Bayar", bill.payCode)
                KeyValueRow("Item Tagihan", "Tagihan Internet")
                KeyValueRow("Detail Bulan", bill.periodLabel.removePrefix("Tagihan Bulan "))
                KeyValueRow("Waktu Bayar", bill.dateTime)
                KeyValueRow("Metode Pembayaran", bill.method)
                if (bill.channel == PaymentChannel.MANUAL) {
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Divider))
                    Spacer(Modifier.height(8.dp))
                    KeyValueRow("Bank Tujuan", bill.targetBank)
                    KeyValueRow("No. Rekening", bill.targetAccount)
                    KeyValueRow("Atas Nama", bill.targetOwner)
                }
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(Divider))
                Spacer(Modifier.height(8.dp))
                KeyValueRow("Total Nominal", rupiah(bill.amount), valueColor = BrandBlue)
                Spacer(Modifier.height(16.dp))
                val (label, color) = when (bill.status) {
                    PaymentStatus.PAID -> "Transaksi Berhasil" to SuccessGreen
                    PaymentStatus.PENDING -> "Menunggu Pembayaran" to WarningOrange
                    PaymentStatus.CANCELLED -> "Transaksi Dibatalkan" to TextSecondary
                    PaymentStatus.UNPAID -> "Belum Dibayar" to DangerRed
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Divider)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, color = color, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun PaymentCard(bill: Bill, onClick: () -> Unit) {
    val (cardBg, amountColor) = when (bill.status) {
        PaymentStatus.PAID -> SuccessSurface to SuccessGreen
        PaymentStatus.PENDING -> WarningSurface to WarningOrange
        PaymentStatus.CANCELLED -> DangerSurface to DangerRed
        PaymentStatus.UNPAID -> CardWhite to DangerRed
    }
    val ribbonColor = if (bill.channel == PaymentChannel.INSTANT) BrandBlue else WarningOrange
    val ribbonText = if (bill.channel == PaymentChannel.INSTANT) "INSTANT" else "MANUAL"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .clickableNoRipple(onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(CardWhite),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (bill.channel == PaymentChannel.INSTANT) Icons.Filled.QrCode2 else Icons.Filled.ReceiptLong,
                    contentDescription = null,
                    tint = ribbonColor
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(bill.periodLabel, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(bill.dateTime, color = TextSecondary, fontSize = 12.sp)
                }
            }
            Text(rupiah(bill.amount), color = amountColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        CornerRibbon(text = ribbonText, color = ribbonColor)
    }
}

@Composable
private fun PaymentCardLoading() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardWhite)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ShimmerBox(Modifier.size(44.dp), shape = RoundedCornerShape(22.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                ShimmerBox(Modifier.fillMaxWidth(0.6f).height(15.dp))
                Spacer(Modifier.height(8.dp))
                ShimmerBox(Modifier.fillMaxWidth(0.4f).height(12.dp))
            }
            ShimmerBox(Modifier.width(70.dp).height(15.dp))
        }
    }
}

@Composable
private fun CornerRibbon(text: String, color: Color) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(topStart = 16.dp))
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-26).dp, y = (4).dp)
                .rotate(-45f)
                .background(color)
                .width(90.dp)
                .padding(vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text, color = OnAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}
