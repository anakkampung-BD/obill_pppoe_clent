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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.ribminet.obill.AppViewModel
import com.ribminet.obill.ui.components.AppTextField
import com.ribminet.obill.ui.components.AppTopBar
import com.ribminet.obill.ui.components.PrimaryButton
import com.ribminet.obill.ui.components.SectionLabel
import com.ribminet.obill.ui.components.ShimmerBox
import com.ribminet.obill.ui.components.SweetAlertDialog
import com.ribminet.obill.ui.components.clickableNoRipple
import com.ribminet.obill.ui.guide.GuideTarget
import com.ribminet.obill.ui.guide.guideTarget
import com.ribminet.obill.ui.theme.BrandBlue
import com.ribminet.obill.ui.theme.CardWhite
import com.ribminet.obill.ui.theme.DangerRed
import com.ribminet.obill.ui.theme.DangerSurface
import com.ribminet.obill.ui.theme.InfoBlueSurface
import com.ribminet.obill.ui.theme.ScreenBackground
import com.ribminet.obill.ui.theme.SuccessGreen
import com.ribminet.obill.ui.theme.TextPrimary
import com.ribminet.obill.ui.theme.TextSecondary

@Composable
fun WifiSettingsScreen(vm: AppViewModel, onBack: () -> Unit) {
    val device = vm.device
    var ssid by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(device?.ssid) {
        if (ssid.isBlank()) ssid = device?.ssid ?: ""
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "Pengaturan WiFi", onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardWhite)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(InfoBlueSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Wifi, contentDescription = null, tint = SuccessGreen)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    if (vm.deviceLoading && device == null) {
                        ShimmerBox(Modifier.fillMaxWidth(0.6f).height(16.dp))
                        Spacer(Modifier.height(6.dp))
                        ShimmerBox(Modifier.fillMaxWidth(0.4f).height(12.dp))
                    } else {
                        Text(device?.ssid?.takeIf { it.isNotBlank() } ?: "-", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                        Text(
                            "${vm.lanClients.size} perangkat terhubung",
                            color = TextSecondary, fontSize = 12.sp
                        )
                    }
                }
                val online = device?.online == true
                Text(if (online) "Online" else "Offline", color = if (online) SuccessGreen else DangerRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionCard(
                    "Refresh Data", Icons.Filled.Refresh, BrandBlue, Modifier.weight(1f).guideTarget(GuideTarget.WIFI_ACTIONS),
                    enabled = !vm.deviceActionRunning
                ) { vm.refreshDevice() }
                ActionCard(
                    "Reboot Perangkat", Icons.Filled.PowerSettingsNew, DangerRed, Modifier.weight(1f),
                    enabled = !vm.deviceActionRunning
                ) { vm.rebootDevice() }
            }

            Spacer(Modifier.height(20.dp))
            Column(modifier = Modifier.guideTarget(GuideTarget.WIFI_FORM)) {
            SectionLabel("Ubah Nama WiFi (SSID)")
            Spacer(Modifier.height(10.dp))
            AppTextField(ssid, { ssid = it }, "Nama WiFi", leadingIcon = Icons.Filled.Wifi)

            Spacer(Modifier.height(20.dp))
            SectionLabel("Ubah Password WiFi")
            Spacer(Modifier.height(10.dp))
            AppTextField(password, { password = it }, "Min. 8 karakter (kosongkan jika tidak diubah)", leadingIcon = Icons.Filled.Lock, isPassword = true)
            }

            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(InfoBlueSurface)
                    .padding(14.dp)
            ) {
                Text(
                    "Perubahan akan diterapkan ke perangkat (ONT/Router) melalui Genie ACS. Semua perangkat akan terputus sesaat dan perlu terhubung ulang.",
                    color = BrandBlue,
                    fontSize = 12.sp
                )
            }
        }
        Column(modifier = Modifier
            .padding(16.dp)
            .guideTarget(GuideTarget.WIFI_SAVE)) {
            PrimaryButton(
                text = if (vm.deviceActionRunning) "Memproses..." else "Terapkan Perubahan",
                enabled = !vm.deviceActionRunning,
                onClick = { vm.updateWifi(ssid, password) { password = "" } }
            )
        }
    }

    SweetAlertDialog(alert = vm.alert) { vm.dismissAlert() }
}

@Composable
private fun ActionCard(
    label: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CardWhite)
            .let { if (enabled) it.clickableNoRipple(onClick) else it }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = if (enabled) color else TextSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = if (enabled) TextPrimary else TextSecondary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}
