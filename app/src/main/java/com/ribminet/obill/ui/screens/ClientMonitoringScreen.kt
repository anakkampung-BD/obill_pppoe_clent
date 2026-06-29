package com.ribminet.obill.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ribminet.obill.AppViewModel
import com.ribminet.obill.data.remote.DeviceClientDto
import com.ribminet.obill.ui.components.AppTopBar
import com.ribminet.obill.ui.components.ShimmerBox
import com.ribminet.obill.ui.theme.BrandBlue
import com.ribminet.obill.ui.theme.BrandBlueSurface
import com.ribminet.obill.ui.theme.CardWhite
import com.ribminet.obill.ui.theme.SuccessGreen
import com.ribminet.obill.ui.theme.TextPrimary
import com.ribminet.obill.ui.theme.TextSecondary
import com.ribminet.obill.ui.theme.WarningOrange

@Composable
fun ClientMonitoringScreen(vm: AppViewModel, onBack: () -> Unit) {
    // Hanya tampilkan perangkat pada segmen IP 192.168.1.0/24
    val clients = vm.lanClients
    val loading = vm.deviceClientsLoading && clients.isEmpty()
    val wifiCount = clients.count { it.connection == "wifi" }
    val lanCount = clients.count { it.connection == "lan" }
    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "Monitoring Perangkat", onBack = onBack)
        LazyColumn(contentPadding = PaddingValues(16.dp)) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryBox("Total", "${clients.size}", BrandBlue, Modifier.weight(1f))
                    SummaryBox("WiFi", "$wifiCount", SuccessGreen, Modifier.weight(1f))
                    SummaryBox("LAN", "$lanCount", WarningOrange, Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))
                Text("Perangkat Terhubung", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(10.dp))
            }
            if (loading) {
                items(3) {
                    ClientRowLoading()
                    Spacer(Modifier.height(10.dp))
                }
            } else if (clients.isEmpty()) {
                item {
                    Text(
                        "Tidak ada perangkat terhubung yang dilaporkan oleh ONT.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                    )
                }
            } else {
                items(clients, key = { it.macAddress ?: it.hashCode().toString() }) { c ->
                    ClientRow(c)
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun SummaryBox(label: String, value: String, color: Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CardWhite)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(label, color = TextSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun ClientRow(c: DeviceClientDto) {
    val isWifi = c.connection == "wifi"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardWhite)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BrandBlueSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(if (isWifi) Icons.Filled.Devices else Icons.Filled.Computer, contentDescription = null, tint = BrandBlue)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                c.hostname?.takeIf { it.isNotBlank() } ?: "Perangkat Tidak Dikenal",
                fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary
            )
            Text(c.ipAddress ?: "-", color = TextSecondary, fontSize = 12.sp)
            Text(c.macAddress ?: "-", color = TextSecondary, fontSize = 11.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            val tag = if (isWifi) "WiFi" else if (c.connection == "lan") "LAN" else "—"
            val tagColor = if (isWifi) SuccessGreen else WarningOrange
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isWifi) Icon(Icons.Filled.Wifi, contentDescription = null, tint = tagColor, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(tag, color = tagColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ClientRowLoading() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardWhite)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShimmerBox(Modifier.size(44.dp), shape = RoundedCornerShape(12.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            ShimmerBox(Modifier.fillMaxWidth(0.6f).height(14.dp))
            Spacer(Modifier.height(6.dp))
            ShimmerBox(Modifier.fillMaxWidth(0.4f).height(12.dp))
        }
    }
}
