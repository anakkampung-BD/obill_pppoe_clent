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
import com.ribminet.obill.ui.theme.BrandBlue
import com.ribminet.obill.ui.theme.DangerRed
import com.ribminet.obill.ui.theme.SuccessGreen
import com.ribminet.obill.ui.theme.TextPrimary
import com.ribminet.obill.ui.theme.TextSecondary
import com.ribminet.obill.util.rupiah

@Composable
fun BillAmountBreakdown(
    subscriptionLabel: String,
    subscriptionAmount: Long,
    installationLabel: String?,
    installationAmount: Long,
    latePenaltyAmount: Long,
    latePenaltyLabel: String? = null,
    totalAmount: Long,
    modifier: Modifier = Modifier,
    subscriptionGross: Long = 0L,
    creditApplied: Long = 0L,
    creditNote: String? = null,
    periodLabel: String? = null,
) {
    Column(modifier = modifier) {
        periodLabel?.takeIf { it.isNotBlank() }?.let {
            Text(it, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
        }
        val showGross = subscriptionGross > 0L && creditApplied > 0L && subscriptionGross != subscriptionAmount
        BreakdownRow(
            label = subscriptionLabel,
            value = rupiah(if (showGross) subscriptionGross else subscriptionAmount),
        )
        if (creditApplied > 0L) {
            BreakdownRow(
                label = "Kredit sisa masa",
                value = "− ${rupiah(creditApplied)}",
                valueColor = SuccessGreen,
            )
            creditNote?.takeIf { it.isNotBlank() }?.let { note ->
                Text(
                    note,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        }
        if (installationAmount > 0L) {
            BreakdownRow(
                label = installationLabel ?: "Biaya instalasi",
                value = rupiah(installationAmount),
            )
        }
        if (latePenaltyAmount > 0L) {
            BreakdownRow(
                label = latePenaltyLabel ?: "Denda keterlambatan",
                value = rupiah(latePenaltyAmount),
                valueColor = DangerRed,
            )
        }
        Spacer(Modifier.height(8.dp))
        BreakdownRow(
            label = "Total bayar",
            value = rupiah(totalAmount),
            valueColor = BrandBlue,
            bold = true,
        )
    }
}

@Composable
private fun BreakdownRow(
    label: String,
    value: String,
    valueColor: Color = TextPrimary,
    bold: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
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
