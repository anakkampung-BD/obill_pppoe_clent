package com.ribminet.obill.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ribminet.obill.ui.navigation.Routes
import com.ribminet.obill.ui.theme.BrandBlue
import com.ribminet.obill.ui.theme.CardWhite
import com.ribminet.obill.ui.theme.TextMuted

private data class NavItem(val route: String, val label: String, val icon: ImageVector)

@Composable
fun AppBottomBar(current: String, onNavigate: (String) -> Unit) {
    val items = listOf(
        NavItem(Routes.HOME, "Beranda", Icons.Filled.Home),
        NavItem(Routes.HISTORY, "Riwayat", Icons.Filled.ReceiptLong),
        NavItem(Routes.REPORT, "Laporan", Icons.AutoMirrored.Filled.Assignment),
        NavItem(Routes.PROFILE, "Profil", Icons.Filled.Person),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardWhite)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .height(64.dp)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            val selected = current == item.route
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickableNoRipple { if (!selected) onNavigate(item.route) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    item.icon,
                    contentDescription = item.label,
                    tint = if (selected) BrandBlue else TextMuted,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    item.label,
                    color = if (selected) BrandBlue else TextMuted,
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
