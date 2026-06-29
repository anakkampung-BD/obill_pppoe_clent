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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ribminet.obill.data.PaymentMethodOption
import com.ribminet.obill.ui.components.AppTopBar
import com.ribminet.obill.ui.components.ConfirmDialog
import com.ribminet.obill.ui.components.ConfirmRequest
import com.ribminet.obill.ui.components.PrimaryButton
import com.ribminet.obill.ui.components.SectionLabel
import com.ribminet.obill.ui.components.clickableNoRipple
import com.ribminet.obill.ui.theme.BrandBlue
import com.ribminet.obill.ui.theme.BrandBlueSurface
import com.ribminet.obill.ui.theme.CardWhite
import com.ribminet.obill.ui.theme.Divider
import com.ribminet.obill.ui.theme.TextPrimary
import com.ribminet.obill.ui.theme.TextSecondary

@Composable
fun PaymentMethodScreen(
    amount: Long,
    packageLabel: String,
    methods: List<PaymentMethodOption>,
    loading: Boolean,
    submitting: Boolean,
    error: String?,
    alert: com.ribminet.obill.AppAlert?,
    onDismissAlert: () -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onPay: (PaymentMethodOption) -> Unit,
) {
    var selected by remember { mutableStateOf<PaymentMethodOption?>(null) }
    var confirm by remember { mutableStateOf<ConfirmRequest?>(null) }
    val grouped = methods.groupBy { it.group }
    val expanded = remember { mutableStateMapOf<String, Boolean>().apply { grouped.keys.forEach { put(it, true) } } }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(
            title = "Pembayaran",
            onBack = onBack,
            trailingIcon = Icons.Filled.Close,
            onTrailingClick = onClose,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BrandBlueSurface)
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total Tagihan", color = TextSecondary, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        com.ribminet.obill.util.rupiah(amount),
                        color = BrandBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(packageLabel, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("Pilih Metode Pembayaran", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))

            if (loading && methods.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator(color = BrandBlue, modifier = Modifier.size(26.dp))
                }
            }
            if (!loading && methods.isEmpty()) {
                Text("Belum ada metode pembayaran yang tersedia. Hubungi admin.", color = TextSecondary, fontSize = 13.sp)
            }
            if (!error.isNullOrBlank()) {
                Text(error, color = com.ribminet.obill.ui.theme.DangerRed, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
            }

            grouped.forEach { (group, items) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickableNoRipple { expanded[group] = !(expanded[group] ?: true) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SectionLabel(group, color = BrandBlue)
                    Icon(
                        if (expanded[group] == true) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        tint = BrandBlue
                    )
                }
                if (expanded[group] == true) {
                    items.forEach { method ->
                        MethodRow(
                            method = method,
                            selected = selected?.id == method.id,
                            onClick = { selected = method }
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
            Spacer(Modifier.height(8.dp))
        }

        Box(
            modifier = Modifier
                .background(ScreenBg())
                .padding(16.dp)
        ) {
            PrimaryButton(
                text = if (submitting) "Memproses..." else "Lanjutkan Pembayaran",
                enabled = selected != null && !submitting,
                onClick = {
                    selected?.let { m ->
                        confirm = ConfirmRequest(
                            title = "Buat Pesanan Pembayaran?",
                            message = "Anda akan membuat pesanan sebesar ${com.ribminet.obill.util.rupiah(amount)} dengan metode ${m.name}. Lanjutkan?",
                            confirmText = "Ya, Buat",
                            onConfirm = { onPay(m) },
                        )
                    }
                }
            )
        }
    }

    ConfirmDialog(request = confirm, onDismiss = { confirm = null })
    com.ribminet.obill.ui.components.SweetAlertDialog(alert = alert, onConfirm = onDismissAlert)
}

@Composable
private fun ScreenBg(): Color = com.ribminet.obill.ui.theme.ScreenBackground

@Composable
private fun MethodRow(method: PaymentMethodOption, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardWhite)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) BrandBlue else Divider,
                shape = RoundedCornerShape(14.dp)
            )
            .clickableNoRipple(onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        com.ribminet.obill.ui.components.PaymentBrandIcon(
            iconKey = method.iconKey,
            fallbackText = method.short,
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(method.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
            if (method.subtitle.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(method.subtitle, color = TextSecondary, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.width(10.dp))
        RadioDot(selected)
    }
}

@Composable
fun RadioDot(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .border(2.dp, if (selected) BrandBlue else Divider, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(BrandBlue)
            )
        }
    }
}
