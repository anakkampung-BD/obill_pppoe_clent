package com.ribminet.obill.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ribminet.obill.data.DummyData
import com.ribminet.obill.data.PaymentChannel
import com.ribminet.obill.ui.components.AppTopBar
import com.ribminet.obill.ui.components.PrimaryButton
import com.ribminet.obill.ui.components.SectionLabel
import com.ribminet.obill.ui.components.clickableNoRipple
import com.ribminet.obill.ui.theme.BrandBlue
import com.ribminet.obill.ui.theme.BrandBlueSurface
import com.ribminet.obill.ui.theme.CardWhite
import com.ribminet.obill.ui.theme.Divider
import com.ribminet.obill.ui.theme.InfoBlueSurface
import com.ribminet.obill.ui.theme.ScreenBackground
import com.ribminet.obill.ui.theme.TextPrimary
import com.ribminet.obill.ui.theme.TextSecondary
import com.ribminet.obill.ui.theme.WarningSurface
import com.ribminet.obill.ui.theme.WarningOrange
import com.ribminet.obill.util.rupiah

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentConfirmScreen(
    period: String,
    amount: Long,
    packageLabel: String,
    onBack: () -> Unit,
    onContinueInstant: () -> Unit,
    onSubmitManual: () -> Unit,
) {
    var channel by remember { mutableStateOf(PaymentChannel.INSTANT) }
    var bankIndex by remember { mutableIntStateOf(0) }
    var senderName by remember { mutableStateOf(DummyData.user.fullName) }
    var showMethodSheet by remember { mutableStateOf(false) }
    var showBankSheet by remember { mutableStateOf(false) }
    val methodSheet = rememberModalBottomSheetState()
    val bankSheet = rememberModalBottomSheetState()

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "Konfirmasi Pembayaran", onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            SectionLabel("Rincian Tagihan")
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardWhite)
                    .padding(16.dp)
            ) {
                Column {
                    DetailLine("Paket", packageLabel)
                    HLine()
                    DetailLine("Periode", period)
                    HLine()
                    DetailLine("Total Tagihan", rupiah(amount), valueColor = BrandBlue)
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel("Metode Pembayaran")
            Spacer(Modifier.height(10.dp))
            SelectorCard(
                icon = if (channel == PaymentChannel.INSTANT) Icons.Filled.Bolt else Icons.Filled.ReceiptLong,
                topLabel = "Pilihan Metode",
                value = if (channel == PaymentChannel.INSTANT) "Pembayaran Instan & Otomatis" else "Transfer Manual / Kirim Bukti",
                onClick = { showMethodSheet = true }
            )

            if (channel == PaymentChannel.MANUAL) {
                val bank = DummyData.bankAccounts[bankIndex]
                Spacer(Modifier.height(20.dp))
                SectionLabel("Rekening Tujuan")
                Spacer(Modifier.height(10.dp))
                SelectorCard(
                    icon = Icons.Filled.AccountBalance,
                    topLabel = "a.n ${bank.owner}",
                    value = "${bank.bank} - ${bank.number}",
                    iconBg = WarningSurface,
                    iconTint = WarningOrange,
                    reverse = true,
                    onClick = { showBankSheet = true }
                )

                Spacer(Modifier.height(20.dp))
                SectionLabel("Data Pengirim")
                Spacer(Modifier.height(10.dp))
                com.ribminet.obill.ui.components.AppTextField(
                    value = senderName,
                    onValueChange = { senderName = it },
                    placeholder = "Nama Pengirim",
                    label = "Nama Pengirim",
                    leadingIcon = Icons.Filled.Person,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ReadonlyField("Tanggal Transfer", "28-6-2026", Icons.Filled.CalendarMonth, Modifier.weight(1f))
                    ReadonlyField("Jam Transfer", "13:29", Icons.Filled.Schedule, Modifier.weight(1f))
                }
                Spacer(Modifier.height(20.dp))
                SectionLabel("Bukti Transfer")
                Spacer(Modifier.height(10.dp))
                UploadBox()
            } else {
                Spacer(Modifier.height(20.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(InfoBlueSurface)
                        .padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(CardWhite),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.QrCode2, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(34.dp))
                        }
                        Spacer(Modifier.height(14.dp))
                        Text("Pembayaran Instan", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Anda akan diarahkan ke halaman QRIS, Virtual Account, atau Retail. Pembayaran akan terverifikasi secara otomatis oleh sistem.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        Box(modifier = Modifier
            .padding(16.dp)) {
            PrimaryButton(
                text = if (channel == PaymentChannel.INSTANT) "Lanjutkan Pembayaran" else "Kirim Konfirmasi Manual",
                onClick = { if (channel == PaymentChannel.INSTANT) onContinueInstant() else onSubmitManual() }
            )
        }
    }

    if (showMethodSheet) {
        ModalBottomSheet(onDismissRequest = { showMethodSheet = false }, sheetState = methodSheet) {
            Column(Modifier.padding(20.dp)) {
                Text("Pilih Metode Pembayaran", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(16.dp))
                ChannelOption(
                    icon = Icons.Filled.ReceiptLong,
                    title = "Transfer Manual / Kirim Bukti",
                    subtitle = "Verifikasi manual oleh admin",
                    selected = channel == PaymentChannel.MANUAL,
                ) { channel = PaymentChannel.MANUAL; showMethodSheet = false }
                Spacer(Modifier.height(12.dp))
                ChannelOption(
                    icon = Icons.Filled.Bolt,
                    title = "Pembayaran Instan & Otomatis",
                    subtitle = "Verifikasi detik itu juga (QRIS, VA, Retail)",
                    selected = channel == PaymentChannel.INSTANT,
                ) { channel = PaymentChannel.INSTANT; showMethodSheet = false }
                Spacer(Modifier.height(20.dp))
            }
        }
    }

    if (showBankSheet) {
        ModalBottomSheet(onDismissRequest = { showBankSheet = false }, sheetState = bankSheet) {
            Column(Modifier.padding(20.dp)) {
                Text("Pilih Rekening Bank Tujuan", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(16.dp))
                DummyData.bankAccounts.forEachIndexed { i, acc ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickableNoRipple { bankIndex = i; showBankSheet = false }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (i == bankIndex) BrandBlueSurface else ScreenBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.AccountBalance, contentDescription = null, tint = if (i == bankIndex) BrandBlue else TextSecondary)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${acc.bank} - ${acc.number}", fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("a.n ${acc.owner}", color = TextSecondary, fontSize = 13.sp)
                        }
                        com.ribminet.obill.ui.screens.RadioDot(i == bankIndex)
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String, valueColor: Color = TextPrimary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondary, fontSize = 14.sp)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = valueColor)
    }
}

@Composable
private fun HLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Divider)
    )
}

@Composable
private fun SelectorCard(
    icon: ImageVector,
    topLabel: String,
    value: String,
    iconBg: Color = BrandBlueSurface,
    iconTint: Color = BrandBlue,
    reverse: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardWhite)
            .clickableNoRipple(onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            if (reverse) {
                Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                Text(topLabel, color = TextSecondary, fontSize = 12.sp)
            } else {
                Text(topLabel, color = TextSecondary, fontSize = 12.sp)
                Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
            }
        }
        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = TextSecondary)
    }
}

@Composable
private fun ReadonlyField(label: String, value: String, icon: ImageVector, modifier: Modifier) {
    Column(modifier = modifier) {
        Text(label, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Divider, RoundedCornerShape(12.dp))
                .background(CardWhite)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(value, color = TextPrimary, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun UploadBox() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(InfoBlueSurface)
            .border(1.dp, BrandBlue.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.CloudUpload, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(34.dp))
            Spacer(Modifier.height(8.dp))
            Text("Upload Bukti Transfer", color = TextSecondary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun ChannelOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) BrandBlue else Divider,
                shape = RoundedCornerShape(14.dp)
            )
            .clickableNoRipple(onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(BrandBlueSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = BrandBlue)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(subtitle, color = TextSecondary, fontSize = 12.sp)
        }
        if (selected) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = BrandBlue)
        }
    }
}
