package com.ribminet.obill.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ribminet.obill.ui.navigation.Routes
import com.ribminet.obill.ui.guide.GuideTarget
import com.ribminet.obill.ui.guide.guideTarget
import com.ribminet.obill.ui.theme.BrandBlue
import com.ribminet.obill.ui.theme.CardWhite
import com.ribminet.obill.ui.theme.IconChipBlue
import com.ribminet.obill.ui.theme.TextMuted

private data class NavItem(val route: String, val label: String, val icon: ImageVector)

@Composable
fun AppBottomBar(current: String, onNavigate: (String) -> Unit) {
    val items = listOf(
        NavItem(Routes.HOME, "Beranda", Icons.Filled.Home),
        NavItem(Routes.HISTORY, "Riwayat", Icons.AutoMirrored.Filled.ReceiptLong),
        NavItem(Routes.REPORT, "Laporan", Icons.AutoMirrored.Filled.Assignment),
        NavItem(Routes.PROFILE, "Profil", Icons.Filled.Person),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardWhite)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .height(68.dp)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { item ->
            val selected = current == item.route
            val tint by animateColorAsState(
                targetValue = if (selected) BrandBlue else TextMuted,
                animationSpec = tween(180),
                label = "navTint",
            )
            val chipBg by animateColorAsState(
                targetValue = if (selected) IconChipBlue else CardWhite,
                animationSpec = tween(180),
                label = "navChip",
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .then(
                        when (item.route) {
                            Routes.PROFILE -> Modifier.guideTarget(GuideTarget.BOTTOM_PROFILE)
                            Routes.REPORT -> Modifier.guideTarget(GuideTarget.BOTTOM_REPORT)
                            else -> Modifier
                        }
                    )
                    .clickableRipple(bounded = true) { onNavigate(item.route) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(chipBg)
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        item.icon,
                        contentDescription = item.label,
                        tint = tint,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    item.label,
                    color = tint,
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}
