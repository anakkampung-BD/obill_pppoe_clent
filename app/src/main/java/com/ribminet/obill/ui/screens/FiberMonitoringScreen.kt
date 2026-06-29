package com.ribminet.obill.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ribminet.obill.AppViewModel
import com.ribminet.obill.data.remote.RxPointDto
import com.ribminet.obill.ui.components.AppTopBar
import com.ribminet.obill.ui.components.ShimmerBox
import com.ribminet.obill.ui.components.StatusBadge
import com.ribminet.obill.ui.theme.BrandBlue
import com.ribminet.obill.ui.theme.CardWhite
import com.ribminet.obill.ui.theme.DangerRed
import com.ribminet.obill.ui.theme.DangerSurface
import com.ribminet.obill.ui.theme.Divider
import com.ribminet.obill.ui.theme.SuccessGreen
import com.ribminet.obill.ui.theme.SuccessSurface
import com.ribminet.obill.ui.theme.TextPrimary
import com.ribminet.obill.ui.theme.TextSecondary
import com.ribminet.obill.ui.theme.WarningOrange
import com.ribminet.obill.ui.theme.WarningSurface

@Composable
fun FiberMonitoringScreen(vm: AppViewModel, onBack: () -> Unit) {
    val device = vm.device
    val loading = vm.deviceLoading && device == null
    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "Performansi Fiber Optik", onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            when {
                loading -> GaugeLoading()
                device == null || !vm.deviceFound -> {
                    DeviceEmpty(vm.deviceError ?: "Perangkat belum terbaca di server ACS.")
                }
                else -> {
                    val dbm = device.rxPowerDbm
                    val status = device.rxPowerStatus
                    val color = rxColor(status)
                    val surface = rxSurface(status)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(CardWhite)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Redaman Sinyal Optik (RX Power)", color = TextSecondary, fontSize = 13.sp)
                        Spacer(Modifier.height(16.dp))
                        Box(contentAlignment = Alignment.Center) {
                            Gauge(dbm ?: 0.0, color)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(dbm?.toString() ?: "-", color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("dBm", color = TextSecondary, fontSize = 14.sp)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        StatusBadge(rxStatusLabel(status), surface, color)
                    }

                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetricBox(
                            "RX Power",
                            dbm?.let { "$it dBm" } ?: "-",
                            Icons.Filled.ArrowDownward, color, Modifier.weight(1f)
                        )
                        MetricBox(
                            "Suhu ONT",
                            device.temperatureC?.let { "$it °C" } ?: "-",
                            Icons.Filled.DeviceThermostat, BrandBlue, Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetricBox(
                            "Uptime",
                            device.uptime ?: "-",
                            Icons.Filled.Schedule, BrandBlue, Modifier.weight(1f)
                        )
                        MetricBox(
                            "Status",
                            if (device.online == true) "Online" else "Offline",
                            Icons.Filled.Wifi,
                            if (device.online == true) SuccessGreen else DangerRed,
                            Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(20.dp))
                    Text("Riwayat Redaman", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(12.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CardWhite)
                            .padding(16.dp)
                    ) {
                        if (vm.rxHistoryLoading && vm.rxHistory.isEmpty()) {
                            ShimmerBox(Modifier.fillMaxWidth().height(120.dp), shape = RoundedCornerShape(12.dp))
                        } else if (vm.rxHistory.isEmpty()) {
                            Text("Belum ada data riwayat redaman.", color = TextSecondary, fontSize = 12.sp)
                        } else {
                            vm.rxSummary?.let { s ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    SummaryItem("Min", s.minDbm)
                                    SummaryItem("Rata-rata", s.avgDbm)
                                    SummaryItem("Max", s.maxDbm)
                                }
                                Spacer(Modifier.height(14.dp))
                            }
                            HistoryChart(vm.rxHistory)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(surface)
                            .padding(14.dp)
                    ) {
                        Text(
                            rxAdvice(status),
                            color = color,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

private fun rxColor(status: String?): Color = when (status) {
    "baik" -> SuccessGreen
    "cukup" -> WarningOrange
    "lemah" -> DangerRed
    else -> SuccessGreen
}

private fun rxSurface(status: String?): Color = when (status) {
    "baik" -> SuccessSurface
    "cukup" -> WarningSurface
    "lemah" -> DangerSurface
    else -> SuccessSurface
}

private fun rxStatusLabel(status: String?): String = when (status) {
    "baik" -> "Sinyal Baik"
    "cukup" -> "Sinyal Cukup"
    "lemah" -> "Sinyal Lemah"
    else -> "Tidak Diketahui"
}

private fun rxAdvice(status: String?): String = when (status) {
    "baik" -> "Standar redaman ideal ≥ -25 dBm. Sinyal Anda berada pada kondisi optimal."
    "cukup" -> "Redaman berada di rentang -25 s/d -28 dBm. Masih dapat digunakan, namun pantau secara berkala."
    "lemah" -> "Redaman di bawah -28 dBm. Sinyal lemah, segera hubungi admin untuk pengecekan jaringan."
    else -> "Status redaman belum tersedia."
}

@Composable
private fun SummaryItem(label: String, value: Double?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value?.let { "$it" } ?: "-", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(label, color = TextSecondary, fontSize = 11.sp)
    }
}

@Composable
private fun DeviceEmpty(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardWhite)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.Wifi, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(12.dp))
        Text("Perangkat Tidak Tersedia", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
        Spacer(Modifier.height(6.dp))
        Text(message, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun GaugeLoading() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardWhite)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ShimmerBox(Modifier.fillMaxWidth(0.6f).height(14.dp))
        Spacer(Modifier.height(16.dp))
        ShimmerBox(Modifier.size(160.dp), shape = androidx.compose.foundation.shape.CircleShape)
        Spacer(Modifier.height(12.dp))
        ShimmerBox(Modifier.fillMaxWidth(0.4f).height(24.dp), shape = RoundedCornerShape(12.dp))
    }
}

@Composable
private fun Gauge(dbm: Double, color: Color) {
    Canvas(modifier = Modifier.size(180.dp)) {
        val stroke = 22f
        drawArc(
            color = Divider,
            startAngle = 135f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = Offset(stroke, stroke),
            size = androidx.compose.ui.geometry.Size(size.width - stroke * 2, size.height - stroke * 2),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )
        val ratio = ((dbm + 30) / 30).coerceIn(0.0, 1.0)
        drawArc(
            color = color,
            startAngle = 135f,
            sweepAngle = (270f * ratio).toFloat(),
            useCenter = false,
            topLeft = Offset(stroke, stroke),
            size = androidx.compose.ui.geometry.Size(size.width - stroke * 2, size.height - stroke * 2),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )
    }
}

@Composable
private fun MetricBox(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CardWhite)
            .padding(16.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(8.dp))
        Text(label, color = TextSecondary, fontSize = 12.sp)
        Text(value, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
private fun HistoryChart(history: List<RxPointDto>) {
    // history urut terbaru dulu; balik agar kiri = lama, kanan = terbaru
    val points = history.reversed().mapNotNull { it.rxDbm }
    if (points.isEmpty()) {
        Text("Belum ada data riwayat redaman.", color = TextSecondary, fontSize = 12.sp)
        return
    }
    val maxV = (points.maxOrNull() ?: -16.0) + 1
    val minV = (points.minOrNull() ?: -22.0) - 1
    Canvas(modifier = Modifier
        .fillMaxWidth()
        .height(120.dp)) {
        val n = points.size
        val barW = size.width / (n * 2f)
        points.forEachIndexed { i, v ->
            val ratio = ((v - minV) / (maxV - minV)).coerceIn(0.0, 1.0).toFloat()
            val barH = size.height * ratio
            val x = barW + i * barW * 2
            drawRoundRect(
                color = SuccessGreen,
                topLeft = Offset(x, size.height - barH),
                size = androidx.compose.ui.geometry.Size(barW, barH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    Text("${points.size} sampel terakhir", color = TextSecondary, fontSize = 11.sp)
}
