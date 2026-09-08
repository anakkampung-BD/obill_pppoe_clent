package com.ribminet.obill.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AccountBalanceWallet
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
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ribminet.obill.data.remote.BillDto
import com.ribminet.obill.data.remote.DeviceDto
import com.ribminet.obill.data.remote.OrderDto
import com.ribminet.obill.data.remote.AnnouncementDto
import com.ribminet.obill.data.remote.creditApplied
import com.ribminet.obill.data.remote.disconnectDisplay
import com.ribminet.obill.data.remote.dueDisplay
import com.ribminet.obill.data.remote.formatDateId
import com.ribminet.obill.data.remote.installationFeeAmount
import com.ribminet.obill.data.remote.installationFeeLabel
import com.ribminet.obill.data.remote.isFirstActivation
import com.ribminet.obill.data.remote.latePenaltyAmount
import com.ribminet.obill.data.remote.latePenaltyLabel
import com.ribminet.obill.data.remote.payableTotal
import com.ribminet.obill.data.remote.shouldShowDisconnectHint
import com.ribminet.obill.data.remote.subscriptionAmount
import com.ribminet.obill.data.remote.subscriptionGross
import com.ribminet.obill.ui.components.HomeAnnouncementSection
import com.ribminet.obill.ui.components.IconButtonRound
import com.ribminet.obill.ui.components.ShimmerBox
import com.ribminet.obill.ui.components.StatusBadge
import com.ribminet.obill.ui.components.clickableNoRipple
import com.ribminet.obill.ui.guide.GuideTarget
import com.ribminet.obill.ui.guide.guideTarget
import com.ribminet.obill.ui.theme.AppThemeState
import com.ribminet.obill.ui.theme.BrandBlue
import com.ribminet.obill.ui.theme.BrandBlueDark
import com.ribminet.obill.ui.theme.CardWhite
import com.ribminet.obill.ui.theme.DangerRed
import com.ribminet.obill.ui.theme.DangerSurface
import com.ribminet.obill.ui.theme.HeroGreenBottom
import com.ribminet.obill.ui.theme.HeroGreenTop
import com.ribminet.obill.ui.theme.IconChipBlue
import com.ribminet.obill.ui.theme.OnAccent
import com.ribminet.obill.ui.theme.SuccessGreen
import com.ribminet.obill.ui.theme.SuccessSurface
import com.ribminet.obill.ui.theme.TextPrimary
import com.ribminet.obill.ui.theme.TextSecondary
import com.ribminet.obill.ui.theme.WarningOrange
import com.ribminet.obill.ui.theme.WarningSurface
import com.ribminet.obill.util.initialsOf
import com.ribminet.obill.util.rupiah

private val CardRadius = 20.dp

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
    onPpob: () -> Unit = {},
    onWalletTopUp: () -> Unit = {},
    onWifi: () -> Unit,
    onClients: () -> Unit,
    onFiber: () -> Unit,
    onNotifications: () -> Unit,
    unreadNotifications: Int = 0,
    announcements: List<AnnouncementDto> = emptyList(),
    onOpenAnnouncement: (AnnouncementDto) -> Unit = {},
) {
    val profileShimmer = user == null
    val billShimmer = bill == null
    val deviceShimmer = device == null && deviceLoading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        HomeHeader(
            user = user,
            profileShimmer = profileShimmer,
            unreadNotifications = unreadNotifications,
            onNotifications = onNotifications,
        )

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(12.dp))

                if (profileShimmer) {
                    PackageStatusLoading()
                } else {
                    SoftCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(IconChipBlue),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Filled.Speed, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(24.dp))
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(user?.packageName ?: "-", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
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
                            bill = bill,
                            dueDate = bill?.dueDisplay() ?: formatDateId(bill?.nextPayment?.dueDate),
                            isOverdue = bill?.nextPayment?.isOverdue == true,
                            hasOpenOrder = openOrder != null,
                            serviceActive = (bill?.statusLangganan == "active") || (user?.serviceActive == true),
                            onClick = onOpenBilling,
                            onClose = onDismissBillingStatus,
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
                SectionTitle(
                    title = "Jaringan",
                    trailing = {
                        if (!deviceShimmer) {
                            DeviceStatusBadge(online = device?.online == true, available = device != null)
                        }
                    },
                )
                Spacer(Modifier.height(10.dp))
                val deviceOffline = device != null && device.online != true
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NetworkStat(
                        modifier = Modifier.weight(1f).guideTarget(GuideTarget.HOME_NETWORK_RX),
                        icon = Icons.Filled.NetworkCheck,
                        label = "RX Power",
                        value = if (deviceOffline) "" else device?.rxPowerDbm?.let { "$it dBm" } ?: "-",
                        valueColor = if (deviceOffline) DangerRed else rxStatusColor(device?.rxPowerStatus),
                        loading = deviceShimmer,
                        valueIcon = if (deviceOffline) Icons.Filled.CloudOff else null,
                        onClick = onFiber,
                    )
                    NetworkStat(
                        modifier = Modifier.weight(1f).guideTarget(GuideTarget.HOME_NETWORK_SSID),
                        icon = Icons.Filled.Wifi,
                        label = "SSID",
                        value = if (deviceOffline) "" else device?.ssid?.takeIf { it.isNotBlank() } ?: "-",
                        valueColor = if (deviceOffline) DangerRed else TextPrimary,
                        loading = deviceShimmer,
                        valueIcon = if (deviceOffline) Icons.Filled.CloudOff else null,
                        onClick = onWifi,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NetworkStat(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Timer,
                        label = "Masa Aktif",
                        value = bill?.nextPayment?.daysUntilDue?.let {
                            if (it >= 0) "$it Hari" else "Lewat tempo"
                        } ?: "-",
                        valueColor = TextPrimary,
                        loading = billShimmer,
                        onClick = onOpenBilling,
                    )
                    NetworkStat(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Router,
                        label = "User",
                        value = connectedCount?.let { "$it pengguna" } ?: "-",
                        valueColor = TextPrimary,
                        loading = connectedLoading && connectedCount == null,
                        onClick = onClients,
                    )
                }

                Spacer(Modifier.height(22.dp))
                SectionTitle(title = "Akses Cepat")
                Spacer(Modifier.height(12.dp))
                SoftCard(padding = 14.dp) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        QuickAction("Tagihan", Icons.Filled.ReceiptLong, IconChipBlue, BrandBlue, Modifier.weight(1f).guideTarget(GuideTarget.HOME_QUICK_TAGIHAN), onOpenBilling)
                        QuickAction("Ganti Paket", Icons.Filled.SwapHoriz, SuccessSurface, SuccessGreen, Modifier.weight(1f).guideTarget(GuideTarget.HOME_QUICK_GANTI_PAKET), onChangePackage)
                        QuickAction("PPOB", Icons.Filled.SimCard, WarningSurface, WarningOrange, Modifier.weight(1f), onPpob)
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        QuickAction("Pengaduan", Icons.Filled.SupportAgent, WarningSurface, WarningOrange, Modifier.weight(1f).guideTarget(GuideTarget.HOME_QUICK_PENGADUAN), onComplaint)
                        QuickAction("Bantuan", Icons.AutoMirrored.Filled.HelpOutline, IconChipBlue, BrandBlueDark, Modifier.weight(1f), onHelp)
                        QuickAction("Top Up", Icons.Filled.AccountBalanceWallet, SuccessSurface, SuccessGreen, Modifier.weight(1f), onWalletTopUp)
                    }
                }

                if (announcements.isNotEmpty()) {
                    Spacer(Modifier.height(22.dp))
                    HomeAnnouncementSection(
                        announcements = announcements,
                        onOpen = onOpenAnnouncement,
                    )
                }

                Spacer(Modifier.height(28.dp))
            }
        }
}

@Composable
private fun SoftBlob(size: Dp, x: Dp, y: Dp, color: Color, alpha: Float, force: Boolean = false) {
    if (!force && AppThemeState.dark) return
    Box(
        modifier = Modifier
            .offset(x = x, y = y)
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha)),
    )
}

@Composable
private fun SoftCard(
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(CardRadius), clip = false)
            .clip(RoundedCornerShape(CardRadius))
            .background(CardWhite)
            .padding(padding),
    ) {
        content()
    }
}

@Composable
private fun HomeHeader(
    user: com.ribminet.obill.data.UserProfile?,
    profileShimmer: Boolean,
    unreadNotifications: Int,
    onNotifications: () -> Unit,
) {
    val photoUrl = user?.photoUrl?.takeIf { it.isNotBlank() }
    val greenGradient = Brush.verticalGradient(listOf(HeroGreenTop, HeroGreenBottom))
    val headerHeight = 220.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .windowInsetsPadding(WindowInsets.statusBars)
            .height(headerHeight),
    ) {
        // Background: foto profil penuh, atau gradient hijau sebagai fallback
        if (photoUrl != null) {
            coil.compose.AsyncImage(
                model = photoUrl,
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color(0x33000000),
                            0.45f to Color(0x66000000),
                            1f to Color(0xCC0A3D24),
                        )
                    ),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(greenGradient),
            )
            SoftBlob(size = 180.dp, x = 240.dp, y = (-30).dp, color = Color.White, alpha = 0.14f, force = true)
            SoftBlob(size = 140.dp, x = (-50).dp, y = 80.dp, color = Color.White, alpha = 0.10f, force = true)
        }

        // Aksi di kanan atas
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButtonRound(
                if (AppThemeState.dark) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                tint = OnAccent,
            ) { AppThemeState.dark = !AppThemeState.dark }
            BadgedBox(
                badge = {
                    if (unreadNotifications > 0) {
                        Badge {
                            Text(
                                if (unreadNotifications > 9) "9+" else unreadNotifications.toString(),
                                fontSize = 9.sp,
                            )
                        }
                    }
                },
            ) {
                IconButtonRound(Icons.Filled.Notifications, tint = OnAccent, onClick = onNotifications)
            }
        }

        // Identitas di bawah header
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(OnAccent.copy(alpha = 0.95f))
                    .border(2.dp, OnAccent.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (!profileShimmer) {
                    if (photoUrl != null) {
                        coil.compose.AsyncImage(
                            model = photoUrl,
                            contentDescription = null,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.matchParentSize().clip(CircleShape),
                        )
                    } else {
                        Text(initialsOf(user?.fullName), color = HeroGreenBottom, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Halo,", color = OnAccent.copy(alpha = 0.85f), fontSize = 13.sp)
                if (profileShimmer) {
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(140.dp)
                            .height(16.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(OnAccent.copy(alpha = 0.35f)),
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            user?.fullName ?: "",
                            color = OnAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "Saldo",
                                color = OnAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                            )
                            Text(
                                rupiah(user?.walletBalance ?: 0L),
                                color = OnAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                maxLines = 1,
                            )
                        }
                    }
                    Text(
                        "v.${com.ribminet.obill.ObillApp.instance.appVersionName()}",
                        color = OnAccent.copy(alpha = 0.75f),
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, trailing: @Composable (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
        trailing?.invoke()
    }
}

@Composable
private fun BillingStatusCard(
    bill: BillDto?,
    dueDate: String,
    isOverdue: Boolean,
    hasOpenOrder: Boolean,
    serviceActive: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit,
) {
    val total = bill?.payableTotal() ?: 0L
    val installation = bill?.installationFeeAmount() ?: 0L
    val subscription = bill?.subscriptionAmount() ?: 0L
    val subscriptionGross = bill?.subscriptionGross() ?: 0L
    val credit = bill?.creditApplied() ?: 0L
    val penalty = bill?.latePenaltyAmount() ?: 0L
    val showBreakdown = bill != null && (
        installation > 0L || penalty > 0L || credit > 0L || bill.isFirstActivation()
    )
    val billData = bill
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
            .shadow(6.dp, RoundedCornerShape(CardRadius), clip = false)
            .clip(RoundedCornerShape(CardRadius))
            .background(surface)
            .clickableNoRipple(onClick)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Tagihan", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                if (showBreakdown && billData != null) {
                    if (billData.isFirstActivation()) {
                        Text("Aktivasi pertama", color = TextSecondary, fontSize = 11.sp)
                    }
                    val langgananLabel = if (credit > 0L && subscriptionGross > 0L) {
                        "Langganan: ${rupiah(subscriptionGross)}"
                    } else {
                        "Langganan: ${rupiah(subscription)}"
                    }
                    Text(langgananLabel, color = TextSecondary, fontSize = 12.sp)
                    if (credit > 0L) {
                        Text("Kredit: − ${rupiah(credit)}", color = SuccessGreen, fontSize = 12.sp)
                    }
                    if (installation > 0L) {
                        Text(
                            "${billData.installationFeeLabel()}: ${rupiah(installation)}",
                            color = TextSecondary,
                            fontSize = 12.sp,
                        )
                    }
                    if (penalty > 0L) {
                        Text(
                            billData.latePenaltyLabel() + ": ${rupiah(penalty)}",
                            color = DangerRed,
                            fontSize = 12.sp,
                        )
                    }
                }
                Text(rupiah(total), color = accent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("Aktif sampai: $dueDate", color = TextSecondary, fontSize = 12.sp)
                if (bill?.shouldShowDisconnectHint() == true) {
                    Text(
                        "Putus: ${bill.disconnectDisplay()}",
                        color = DangerRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            StatusBadge(badgeText, accent, OnAccent)
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickableNoRipple(onClose),
                contentAlignment = Alignment.Center,
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
        !available -> StatusVisual("Offline", TextSecondary, TextSecondary, CardWhite)
        online -> StatusVisual("Online", SuccessGreen, SuccessGreen, SuccessSurface)
        else -> StatusVisual("Offline", DangerRed, DangerRed, DangerSurface)
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor),
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
    valueColor: Color,
    onClick: () -> Unit,
    loading: Boolean = false,
    valueIcon: ImageVector? = null,
) {
    Row(
        modifier = modifier
            .shadow(6.dp, RoundedCornerShape(18.dp), clip = false)
            .clip(RoundedCornerShape(18.dp))
            .background(CardWhite)
            .clickableNoRipple(onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(IconChipBlue),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                label,
                color = TextSecondary,
                fontSize = 11.sp,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(2.dp))
            when {
                loading -> ShimmerBox(Modifier.width(64.dp).height(16.dp))
                valueIcon != null -> Icon(
                    valueIcon,
                    contentDescription = value.ifBlank { "Offline" },
                    tint = valueColor,
                    modifier = Modifier.size(20.dp),
                )
                else -> Text(
                    value,
                    color = valueColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun PackageStatusLoading() {
    SoftCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ShimmerBox(Modifier.size(48.dp), shape = RoundedCornerShape(14.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                ShimmerBox(Modifier.fillMaxWidth(0.7f).height(16.dp))
                Spacer(Modifier.height(8.dp))
                ShimmerBox(Modifier.fillMaxWidth(0.4f).height(14.dp))
            }
            ShimmerBox(Modifier.width(54.dp).height(24.dp), shape = RoundedCornerShape(12.dp))
        }
    }
}

@Composable
private fun BillingStatusLoading() {
    SoftCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ShimmerBox(Modifier.size(42.dp), shape = CircleShape)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                ShimmerBox(Modifier.fillMaxWidth(0.5f).height(14.dp))
                Spacer(Modifier.height(6.dp))
                ShimmerBox(Modifier.fillMaxWidth(0.4f).height(16.dp))
                Spacer(Modifier.height(6.dp))
                ShimmerBox(Modifier.fillMaxWidth(0.35f).height(12.dp))
            }
            ShimmerBox(Modifier.width(54.dp).height(24.dp), shape = RoundedCornerShape(12.dp))
        }
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: ImageVector,
    bg: Color,
    tint: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier.clickableNoRipple(onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(bg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextPrimary, textAlign = TextAlign.Center)
    }
}
