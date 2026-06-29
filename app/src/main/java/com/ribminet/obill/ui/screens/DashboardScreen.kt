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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ribminet.obill.data.remote.BillDto
import com.ribminet.obill.data.remote.DeviceDto
import com.ribminet.obill.data.remote.OrderDto
import com.ribminet.obill.data.remote.formatDateId
import com.ribminet.obill.ui.components.AppCard
import com.ribminet.obill.ui.components.IconChip
import com.ribminet.obill.ui.components.IconButtonRound
import com.ribminet.obill.ui.components.ShimmerBox
import com.ribminet.obill.ui.components.StatusBadge
import com.ribminet.obill.ui.components.clickableNoRipple
import com.ribminet.obill.ui.theme.AppThemeState
import com.ribminet.obill.ui.theme.BrandBlue
import com.ribminet.obill.ui.theme.BrandBlueDark
import com.ribminet.obill.ui.theme.DangerRed
import com.ribminet.obill.ui.theme.DangerSurface
import com.ribminet.obill.ui.theme.CardWhite
import com.ribminet.obill.ui.theme.HeroGreenBottom
import com.ribminet.obill.ui.theme.HeroGreenTop
import com.ribminet.obill.ui.theme.IconChipBlue
import com.ribminet.obill.ui.theme.OnAccent
import com.ribminet.obill.ui.theme.ScreenBackground
import com.ribminet.obill.ui.theme.SuccessGreen
import com.ribminet.obill.ui.theme.SuccessSurface
import com.ribminet.obill.ui.theme.TextPrimary
import com.ribminet.obill.ui.theme.TextSecondary
import com.ribminet.obill.ui.theme.WarningOrange
import com.ribminet.obill.ui.theme.WarningSurface
import com.ribminet.obill.util.initialsOf
import com.ribminet.obill.util.rupiah

@Composable
fun DashboardScreen(
    user: com.ribminet.obill.data.UserProfile?,
    bill: BillDto?,
    openOrder: OrderDto?,
    device: DeviceDto?,
    deviceLoading: Boolean,
    connectedCount: Int?,
    connectedLoading: Boolean,
    showBillingStatus: Boolean,
    onDismissBillingStatus: () -> Unit,
    onOpenBilling: () -> Unit,
    onChangePackage: () -> Unit,
    onComplaint: () -> Unit,
    onHelp: () -> Unit,
    onWifi: () -> Unit,
    onClients: () -> Unit,
    onFiber: () -> Unit,
    onNotifications: () -> Unit,
) {
    val profileShimmer = user == null
    val billShimmer = bill == null
    val deviceShimmer = device == null && deviceLoading
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                .background(Brush.verticalGradient(listOf(HeroGreenTop, HeroGreenBottom)))
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(OnAccent),
                    contentAlignment = Alignment.Center
                ) {
                    if (!profileShimmer) {
                        val photo = user?.photoUrl?.takeIf { it.isNotBlank() }
                        if (photo != null) {
                            coil.compose.AsyncImage(
                                model = photo,
                                contentDescription = null,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.matchParentSize().clip(CircleShape)
                            )
                        } else {
                            Text(initialsOf(user?.fullName), color = HeroGreenBottom, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Selamat Datang,", color = OnAccent.copy(alpha = 0.9f), fontSize = 13.sp)
                    if (profileShimmer) {
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(140.dp)
                                .height(16.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(OnAccent.copy(alpha = 0.35f))
                        )
                    } else {
                        Text(user?.fullName ?: "", color = OnAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            "Obill • v.${com.ribminet.obill.ObillApp.instance.appVersionName()}",
                            color = OnAccent.copy(alpha = 0.85f),
                            fontSize = 10.sp,
                        )
                    }
                }
            }
            Row(modifier = Modifier.align(Alignment.TopEnd)) {
                IconButtonRound(if (AppThemeState.dark) Icons.Filled.DarkMode else Icons.Filled.LightMode, tint = OnAccent) { AppThemeState.dark = !AppThemeState.dark }
                IconButtonRound(Icons.Filled.Notifications, tint = OnAccent, onClick = onNotifications)
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            if (profileShimmer) {
                PackageStatusLoading()
            } else {
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconChip(Icons.Filled.Speed, IconChipBlue, BrandBlue)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Status Paket Anda", color = TextSecondary, fontSize = 13.sp)
                            Text("${user?.packageName}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(rupiah(user?.packagePrice ?: 0L), color = BrandBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(" /bln", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 1.dp))
                            }
                        }
                        if (user?.serviceActive == true) StatusBadge("Aktif", SuccessSurface, SuccessGreen)
                        else StatusBadge("Nonaktif", DangerSurface, DangerRed)
                    }
                }
            }

            if (showBillingStatus) {
                Spacer(Modifier.height(12.dp))
                if (billShimmer) {
                    BillingStatusLoading()
                } else {
                    BillingStatusCard(
                        amount = bill?.amount ?: 0L,
                        dueDate = formatDateId(bill?.nextPayment?.dueDate),
                        isOverdue = bill?.nextPayment?.isOverdue == true,
                        hasOpenOrder = openOrder != null,
                        serviceActive = (bill?.statusLangganan == "active") || (user?.serviceActive == true),
                        onClick = onOpenBilling,
                        onClose = onDismissBillingStatus,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Status Jaringan", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                if (!deviceShimmer) {
                    DeviceStatusBadge(online = device?.online == true, available = device != null)
                }
            }
            Spacer(Modifier.height(10.dp))
            val deviceOffline = device != null && device.online != true
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NetworkStat(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.NetworkCheck,
                    label = "RX Power",
                    value = if (deviceOffline) "" else device?.rxPowerDbm?.let { "$it dBm" } ?: "-",
                    valueColor = if (deviceOffline) DangerRed else rxStatusColor(device?.rxPowerStatus),
                    loading = deviceShimmer,
                    centerValue = deviceOffline,
                    valueIcon = if (deviceOffline) Icons.Filled.CloudOff else null,
                    onClick = onFiber,
                )
                NetworkStat(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Wifi,
                    label = "Nama SSID",
                    value = if (deviceOffline) "" else device?.ssid?.takeIf { it.isNotBlank() } ?: "-",
                    valueColor = if (deviceOffline) DangerRed else TextPrimary,
                    loading = deviceShimmer,
                    centerValue = deviceOffline,
                    valueIcon = if (deviceOffline) Icons.Filled.CloudOff else null,
                    onClick = onWifi,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NetworkStat(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Timer,
                    label = "Masa Aktif",
                    value = bill?.nextPayment?.daysUntilDue?.let {
                        if (it >= 0) "$it Day" else "Lewat tempo"
                    } ?: "-",
                    valueColor = TextPrimary,
                    loading = billShimmer,
                    centerValue = true,
                    onClick = onOpenBilling,
                )
                NetworkStat(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Router,
                    label = "User",
                    value = connectedCount?.let { "$it User" } ?: "-",
                    valueColor = TextPrimary,
                    loading = connectedLoading && connectedCount == null,
                    centerValue = true,
                    onClick = onClients,
                )
            }

            Spacer(Modifier.height(20.dp))
            Text("Akses Cepat", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                QuickAction("Tagihan", Icons.Filled.ReceiptLong, IconChipBlue, BrandBlue, Modifier.weight(1f), onOpenBilling)
                QuickAction("Ganti Paket", Icons.Filled.SwapHoriz, SuccessSurface, SuccessGreen, Modifier.weight(1f), onChangePackage)
                QuickAction("Pengaduan", Icons.Filled.SupportAgent, WarningSurface, WarningOrange, Modifier.weight(1f), onComplaint)
                QuickAction("Bantuan", Icons.AutoMirrored.Filled.HelpOutline, IconChipBlue, BrandBlueDark, Modifier.weight(1f), onHelp)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BillingStatusCard(
    amount: Long,
    dueDate: String,
    isOverdue: Boolean,
    hasOpenOrder: Boolean,
    serviceActive: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit,
) {
    val surface: Color
    val accent: Color
    val icon: ImageVector
    val badgeText: String
    when {
        hasOpenOrder -> {
            surface = WarningSurface; accent = WarningOrange
            icon = Icons.Filled.Schedule; badgeText = "Diproses"
        }
        isOverdue -> {
            surface = DangerSurface; accent = DangerRed
            icon = Icons.Filled.ErrorOutline; badgeText = "Belum Lunas"
        }
        !serviceActive -> {
            surface = DangerSurface; accent = DangerRed
            icon = Icons.Filled.ErrorOutline; badgeText = "Nonaktif"
        }
        else -> {
            surface = SuccessSurface; accent = SuccessGreen
            icon = Icons.Filled.CheckCircle; badgeText = "Lunas"
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(surface)
            .clickableNoRipple(onClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(36.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Status Tagihan Kamu", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                Text("Total: ${rupiah(amount)}", color = accent, fontWeight = FontWeight.Bold)
                Text("Jatuh tempo: $dueDate", color = TextSecondary, fontSize = 12.sp)
            }
            StatusBadge(badgeText, accent, OnAccent)
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickableNoRipple(onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Tutup", tint = TextSecondary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

private fun rxStatusColor(status: String?): Color = when (status) {
    "baik" -> SuccessGreen
    "cukup" -> WarningOrange
    "lemah" -> DangerRed
    else -> SuccessGreen
}

@Composable
private fun DeviceStatusBadge(online: Boolean, available: Boolean) {
    val (label, dotColor, textColor, bg) = when {
        !available -> StatusVisual("Tidak Terhubung", TextSecondary, TextSecondary, ScreenBackground)
        online -> StatusVisual("Online", SuccessGreen, SuccessGreen, SuccessSurface)
        else -> StatusVisual("Offline", DangerRed, DangerRed, DangerSurface)
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(Modifier.width(6.dp))
        Text(label, color = textColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

private data class StatusVisual(
    val label: String,
    val dotColor: Color,
    val textColor: Color,
    val bg: Color,
)

@Composable
private fun NetworkStat(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    loading: Boolean = false,
    centerValue: Boolean = false,
    valueIcon: ImageVector? = null,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CardWhite)
            .clickableNoRipple(onClick)
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(22.dp))
                Text(label, color = TextSecondary, fontSize = 12.sp)
            }
            Spacer(Modifier.height(12.dp))
            when {
                loading -> ShimmerBox(Modifier.fillMaxWidth(0.75f).height(16.dp))
                valueIcon != null -> Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (centerValue) Alignment.Center else Alignment.CenterStart
                ) {
                    Icon(valueIcon, contentDescription = value.ifBlank { "Offline" }, tint = valueColor, modifier = Modifier.size(22.dp))
                }
                else -> Text(
                    value,
                    color = valueColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = if (centerValue) TextAlign.Center else TextAlign.Start,
                )
            }
        }
    }
}

@Composable
private fun PackageStatusLoading() {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconChip(Icons.Filled.Speed, IconChipBlue, BrandBlue)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Status Paket Anda", color = TextSecondary, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                ShimmerBox(Modifier.fillMaxWidth(0.8f).height(16.dp))
                Spacer(Modifier.height(6.dp))
                ShimmerBox(Modifier.fillMaxWidth(0.4f).height(14.dp))
            }
            ShimmerBox(Modifier.width(54.dp).height(24.dp), shape = RoundedCornerShape(12.dp))
        }
    }
}

@Composable
private fun BillingStatusLoading() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardWhite)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ShimmerBox(Modifier.size(36.dp), shape = CircleShape)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                ShimmerBox(Modifier.fillMaxWidth(0.6f).height(16.dp))
                Spacer(Modifier.height(6.dp))
                ShimmerBox(Modifier.fillMaxWidth(0.45f).height(14.dp))
                Spacer(Modifier.height(6.dp))
                ShimmerBox(Modifier.fillMaxWidth(0.5f).height(12.dp))
            }
            ShimmerBox(Modifier.width(54.dp).height(24.dp), shape = RoundedCornerShape(12.dp))
        }
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: ImageVector,
    bg: androidx.compose.ui.graphics.Color,
    tint: androidx.compose.ui.graphics.Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier.clickableNoRipple(onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconChip(icon, bg, tint, size = 56, iconSize = 26)
        Spacer(Modifier.height(8.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
    }
}
