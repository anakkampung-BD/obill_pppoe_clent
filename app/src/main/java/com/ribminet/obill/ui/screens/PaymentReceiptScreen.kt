package com.ribminet.obill.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ribminet.obill.ui.components.AppTopBar
import com.ribminet.obill.ui.components.KeyValueRow
import com.ribminet.obill.ui.components.PrimaryButton
import com.ribminet.obill.ui.theme.CardWhite
import com.ribminet.obill.ui.theme.Divider
import com.ribminet.obill.ui.theme.OnAccent
import com.ribminet.obill.ui.theme.ScreenBackground
import com.ribminet.obill.ui.theme.SuccessGreen
import com.ribminet.obill.ui.theme.SuccessSurface
import com.ribminet.obill.ui.theme.TextPrimary
import com.ribminet.obill.ui.theme.TextSecondary
import com.ribminet.obill.util.rupiah

@Composable
fun PaymentReceiptScreen(
    amount: Long,
    code: String,
    period: String,
    dateTime: String,
    method: String,
    gatewayId: String,
    onClose: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "Struk Pembayaran", trailingIcon = Icons.Filled.Close, onTrailingClick = onClose)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardWhite)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(SuccessGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = OnAccent, modifier = Modifier.size(36.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text("Pembayaran Berhasil!", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                Text(rupiah(amount), color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(16.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(Divider))
                Spacer(Modifier.height(12.dp))

                KeyValueRow("Kode Transaksi", code)
                KeyValueRow("Item Tagihan", "Tagihan Internet")
                KeyValueRow("Detail Periode", period)
                KeyValueRow("Waktu Transaksi", dateTime)
                KeyValueRow("Metode Pembayaran", method)
                KeyValueRow("ID Transaksi Gateway", gatewayId)

                Spacer(Modifier.height(12.dp))
                DashedLine()
                Spacer(Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(ScreenBackground)
                        .padding(12.dp)
                ) {
                    Column {
                        Text("MAKLUMAT RESMI / LEGAL NOTICE:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TextSecondary)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Dokumen ini merupakan struk bukti pembayaran elektronik yang sah dan diterbitkan secara otomatis oleh sistem billing kami. Bukti ini memiliki kekuatan hukum yang valid serta setara dengan nota fisik loket resmi. Simpan struk digital ini sebagai acuan referensi administrasi Anda yang sah.",
                            fontSize = 10.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Justify
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SuccessSurface)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("STATUS ACUAN : LUNAS / SETTLEMENT", color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
        Box(modifier = Modifier
            .padding(16.dp)) {
            PrimaryButton(text = "Bagikan Gambar Struk", onClick = onClose)
        }
    }
}

@Composable
private fun DashedLine() {
    androidx.compose.foundation.Canvas(modifier = Modifier
        .fillMaxWidth()
        .height(1.dp)) {
        var x = 0f
        while (x < size.width) {
            drawLine(
                color = Divider,
                start = androidx.compose.ui.geometry.Offset(x, 0f),
                end = androidx.compose.ui.geometry.Offset(x + 8f, 0f),
                strokeWidth = size.height
            )
            x += 14f
        }
    }
}
