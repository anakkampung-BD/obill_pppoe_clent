package com.ribminet.obill.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ribminet.obill.data.remote.PpobPaymentBreakdownDto
import com.ribminet.obill.ui.theme.BrandBlue
import com.ribminet.obill.ui.theme.TextPrimary
import com.ribminet.obill.ui.theme.TextSecondary
import com.ribminet.obill.util.rupiah

/**
 * Ringkasan biaya PPOB/QRIS: harga jual + fee cek nama + kode unik = total bayar.
 */
@Composable
fun PpobAmountBreakdown(
    breakdown: PpobPaymentBreakdownDto?,
    fallbackPayAmount: Long? = null,
    modifier: Modifier = Modifier,
) {
    val sell = breakdown?.sellPrice
    val fee = breakdown?.nameCheckFee ?: 0L
    val feeLabel = breakdown?.nameCheckLabel?.takeIf { it.isNotBlank() } ?: "Cek nama"
    val unique = breakdown?.uniqueCode
        ?: breakdown?.uniqueCodeStr?.toIntOrNull()
    val uniqueLabel = breakdown?.uniqueCodeStr
        ?: unique?.toString()?.padStart(3, '0')
    val total = breakdown?.payAmount ?: fallbackPayAmount

    // Hitung unik dari total jika server tidak kirim field unique_code
    val resolvedUnique = unique ?: run {
        if (sell != null && total != null) {
            val u = total - sell - fee
            if (u in 0L..999L) u.toInt() else null
        } else null
    }
    val resolvedUniqueLabel = uniqueLabel
        ?: resolvedUnique?.toString()?.padStart(3, '0')

    if (sell == null && fee <= 0L && resolvedUnique == null && total == null) return

    Column(modifier = modifier.fillMaxWidth()) {
        sell?.let {
            BreakdownLine(label = "Harga produk", value = rupiah(it))
        }
        if (fee > 0L) {
            BreakdownLine(label = feeLabel, value = rupiah(fee))
        }
        if (resolvedUnique != null && resolvedUnique > 0) {
            BreakdownLine(
                label = "Kode unik${resolvedUniqueLabel?.let { " ($it)" } ?: ""}",
                value = rupiah(resolvedUnique.toLong()),
            )
        }
        total?.let {
            Spacer(modifier = Modifier.height(6.dp))
            BreakdownLine(
                label = "Total bayar",
                value = rupiah(it),
                valueColor = BrandBlue,
                bold = true,
            )
        }
        breakdown?.breakdownLine?.takeIf { it.isNotBlank() }?.let { line ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(line, color = TextSecondary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun BreakdownLine(
    label: String,
    value: String,
    valueColor: Color = TextPrimary,
    bold: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = TextSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(
            value,
            color = valueColor,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            fontSize = 13.sp,
        )
    }
}
