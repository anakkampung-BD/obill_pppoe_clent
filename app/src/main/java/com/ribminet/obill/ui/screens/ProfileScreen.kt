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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ribminet.obill.ui.components.IconButtonRound
import com.ribminet.obill.ui.components.SectionLabel
import com.ribminet.obill.ui.components.ShimmerBox
import com.ribminet.obill.ui.components.clickableNoRipple
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
import com.ribminet.obill.ui.theme.SuccessGreen
import com.ribminet.obill.ui.theme.TextPrimary
import com.ribminet.obill.ui.theme.TextSecondary
import com.ribminet.obill.ui.theme.WarningOrange
import com.ribminet.obill.ui.theme.WarningSurface

@Composable
fun ProfileScreen(
    user: com.ribminet.obill.data.UserProfile?,
    onEditBiodata: () -> Unit,
    onNotifications: () -> Unit,
    onOrders: () -> Unit,
    onHelp: () -> Unit,
    onDoc: (String) -> Unit,
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    .background(Brush.verticalGradient(listOf(HeroGreenTop, HeroGreenBottom)))
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(bottom = 36.dp)
            ) {
                IconButtonRound(
                    if (AppThemeState.dark) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                    tint = OnAccent,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) { AppThemeState.dark = !AppThemeState.dark }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(OnAccent),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!loading) {
                                val photo = user?.photoUrl?.takeIf { it.isNotBlank() }
                                if (photo != null) {
                                    coil.compose.AsyncImage(
                                        model = photo,
                                        contentDescription = null,
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier.matchParentSize().clip(CircleShape)
                                    )
                                } else {
                                    Text(com.ribminet.obill.util.initialsOf(user?.fullName), color = HeroGreenBottom, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(BrandBlueDark),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = OnAccent, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    if (loading) {
                        Box(
                            modifier = Modifier
                                .width(160.dp)
                                .height(16.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(OnAccent.copy(alpha = 0.35f))
                        )
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .width(110.dp)
                                .height(13.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(OnAccent.copy(alpha = 0.25f))
                        )
                    } else {
                        Text(user?.fullName ?: "", color = OnAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("ID Pelanggan: ${user?.customerId}", color = OnAccent.copy(alpha = 0.85f), fontSize = 13.sp)
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                SectionLabel("Pengaturan")
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardWhite)
                ) {
                    SettingRow(Icons.Filled.Person, "Edit Biodata", onClick = onEditBiodata)
                    Sep()
                    SettingRow(Icons.Filled.Notifications, "Notifikasi Aplikasi", onClick = onNotifications)
                    Sep()
                    SettingRow(Icons.Filled.ReceiptLong, "Riwayat Pesanan", onClick = onOrders)
                    Sep()
                    SettingRow(Icons.Filled.SupportAgent, "FAQ & Kontak", onClick = onHelp)
                    Sep()
                    SettingRow(Icons.Filled.Description, "Syarat & Ketentuan", onClick = { onDoc("Syarat & Ketentuan") })
                    Sep()
                    SettingRow(Icons.Filled.PrivacyTip, "Kebijakan Privasi", onClick = { onDoc("Kebijakan Privasi") })
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
                    Text("Obill v.3.0.0", color = TextSecondary, fontSize = 12.sp)
                    Text("© 2026 Obill Powered by AKS. All Rights Reserved.", color = TextSecondary, fontSize = 11.sp)
                }
                Spacer(Modifier.height(16.dp))
            }
        }
        bottomBar()
    }
}

private fun Modifier.offsetUp(): Modifier = this

@Composable
private fun Sep() {
    Box(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .height(1.dp)
        .background(Divider))
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    label: String,
    danger: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit = {},
) {
    val tint = if (danger) DangerRed else BrandBlue
    val labelColor = if (danger) DangerRed else TextPrimary
    Row(
        modifier = Modifier
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
