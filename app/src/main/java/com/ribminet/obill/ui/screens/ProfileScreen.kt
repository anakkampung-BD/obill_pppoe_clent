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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.SupportAgent
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ribminet.obill.ui.components.IconButtonRound
import com.ribminet.obill.ui.components.SectionLabel
import com.ribminet.obill.ui.components.clickableNoRipple
import com.ribminet.obill.ui.guide.GuideTarget
import com.ribminet.obill.ui.guide.guideTarget
import com.ribminet.obill.ui.theme.AppThemeState
import com.ribminet.obill.ui.theme.BrandBlue
import com.ribminet.obill.ui.theme.BrandBlueDark
import com.ribminet.obill.ui.theme.CardWhite
import com.ribminet.obill.ui.theme.DangerRed
import com.ribminet.obill.ui.theme.DangerSurface
import com.ribminet.obill.ui.theme.Divider
import com.ribminet.obill.ui.theme.HeroGreenBottom
import com.ribminet.obill.ui.theme.HeroGreenTop
import com.ribminet.obill.ui.theme.IconChipBlue
import com.ribminet.obill.ui.theme.OnAccent
import com.ribminet.obill.ui.theme.TextPrimary
import com.ribminet.obill.ui.theme.TextSecondary
import com.ribminet.obill.util.initialsOf

@Composable
fun ProfileScreen(
    user: com.ribminet.obill.data.UserProfile?,
    onEditBiodata: () -> Unit,
    onNotifications: () -> Unit,
    onOrders: () -> Unit,
    onHelp: () -> Unit,
    onDoc: (LegalDocType) -> Unit,
    onLogout: () -> Unit,
    bottomBar: @Composable () -> Unit,
) {
    val loading = user == null

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            ProfileHeader(
                user = user,
                loading = loading,
                onEditBiodata = onEditBiodata,
            )

            Column(modifier = Modifier.padding(16.dp)) {
                SectionLabel("Pengaturan")
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardWhite)
                ) {
                    SettingRow(
                        Icons.Filled.Person,
                        "Edit Biodata",
                        modifier = Modifier.guideTarget(GuideTarget.PROFILE_EDIT_BIODATA),
                        onClick = onEditBiodata,
                    )
                    Sep()
                    SettingRow(Icons.Filled.Notifications, "Notifikasi Aplikasi", onClick = onNotifications)
                    Sep()
                    SettingRow(
                        Icons.Filled.ReceiptLong,
                        "Riwayat Pesanan",
                        modifier = Modifier.guideTarget(GuideTarget.PROFILE_ORDERS),
                        onClick = onOrders,
                    )
                    Sep()
                    SettingRow(Icons.Filled.SupportAgent, "FAQ & Kontak", onClick = onHelp)
                    Sep()
                    SettingRow(Icons.Filled.Description, "Syarat & Ketentuan", onClick = { onDoc(LegalDocType.TERMS) })
                    Sep()
                    SettingRow(Icons.Filled.PrivacyTip, "Kebijakan Privasi", onClick = { onDoc(LegalDocType.PRIVACY) })
                }

                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(DangerSurface)
                        .clickableNoRipple(onLogout)
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = DangerRed, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Keluar dari Akun", color = DangerRed, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(20.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val appVersion = com.ribminet.obill.ObillApp.instance.appVersionName()
                    val year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                    Text("Obill • v.$appVersion • Powered By AKS", color = TextSecondary, fontSize = 10.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("© $year • All Rights Reserved", color = TextSecondary, fontSize = 10.sp)
                }
                Spacer(Modifier.height(16.dp))
            }
        }
        bottomBar()
    }
}

@Composable
private fun ProfileHeader(
    user: com.ribminet.obill.data.UserProfile?,
    loading: Boolean,
    onEditBiodata: () -> Unit,
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
            SoftBlob(size = 180.dp, x = 240.dp, y = (-30).dp, color = Color.White, alpha = 0.14f)
            SoftBlob(size = 140.dp, x = (-50).dp, y = 80.dp, color = Color.White, alpha = 0.10f)
        }

        IconButtonRound(
            if (AppThemeState.dark) Icons.Filled.DarkMode else Icons.Filled.LightMode,
            tint = OnAccent,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) { AppThemeState.dark = !AppThemeState.dark }

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.clickableNoRipple(onEditBiodata),
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(OnAccent.copy(alpha = 0.95f))
                        .border(2.dp, OnAccent.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!loading) {
                        if (photoUrl != null) {
                            coil.compose.AsyncImage(
                                model = photoUrl,
                                contentDescription = null,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.matchParentSize().clip(CircleShape),
                            )
                        } else {
                            Text(
                                initialsOf(user?.fullName),
                                color = HeroGreenBottom,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(BrandBlueDark),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = OnAccent, modifier = Modifier.size(12.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (loading) {
                    Box(
                        modifier = Modifier
                            .width(140.dp)
                            .height(16.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(OnAccent.copy(alpha = 0.35f)),
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .width(110.dp)
                            .height(13.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(OnAccent.copy(alpha = 0.25f)),
                    )
                } else {
                    Text(
                        user?.fullName ?: "",
                        color = OnAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "ID Pelanggan: ${user?.customerId}",
                        color = OnAccent.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun SoftBlob(size: Dp, x: Dp, y: Dp, color: Color, alpha: Float) {
    Box(
        modifier = Modifier
            .offset(x = x, y = y)
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha)),
    )
}

@Composable
private fun Sep() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(Divider),
    )
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    label: String,
    danger: Boolean = false,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit = {},
) {
    val tint = if (danger) DangerRed else BrandBlue
    val labelColor = if (danger) DangerRed else TextPrimary
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickableNoRipple(onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (danger) DangerSurface else IconChipBlue),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(label, color = labelColor, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        if (trailing != null) trailing() else Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = TextSecondary
        )
    }
}
