package com.ribminet.obill.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ribminet.obill.ObillApp
import com.ribminet.obill.ui.theme.HeroGreenBottom
import com.ribminet.obill.ui.theme.OnAccent

private val SplashGradient = Brush.verticalGradient(
    0.0f to Color(0xFFE9FBF2),
    0.5f to Color(0xFFBDEFD6),
    1.0f to Color(0xFF8FE0BC),
)

@Composable
fun SplashScreen() {
    val version = ObillApp.instance.appVersionName()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SplashGradient),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .shadow(14.dp, CircleShape)
                    .clip(CircleShape)
                    .background(OnAccent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Wifi,
                    contentDescription = null,
                    tint = HeroGreenBottom,
                    modifier = Modifier.size(58.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Obill v.$version",
                color = Color(0xFF14693F),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
        }
    }
}
