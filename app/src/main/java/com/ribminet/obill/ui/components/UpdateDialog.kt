package com.ribminet.obill.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ribminet.obill.data.remote.ReleaseInfo
import com.ribminet.obill.ui.theme.BrandBlue
import com.ribminet.obill.ui.theme.CardWhite
import com.ribminet.obill.ui.theme.OnAccent
import com.ribminet.obill.ui.theme.TextPrimary
import com.ribminet.obill.ui.theme.TextSecondary

@Composable
fun UpdateDialog(
    info: ReleaseInfo?,
    downloading: Boolean,
    progress: Float,
    onUpdate: (ReleaseInfo) -> Unit,
    onDismiss: () -> Unit,
) {
    if (info == null) return

    val scale = remember { Animatable(0.7f) }
    LaunchedEffect(info) { scale.snapTo(0.7f); scale.animateTo(1f) }

    Dialog(onDismissRequest = { if (!downloading) onDismiss() }) {
        Column(
            modifier = Modifier
                .scale(scale.value)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CardWhite)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(BrandBlue.copy(alpha = 0.12f))
                    .border(3.dp, BrandBlue, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.SystemUpdate, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(38.dp))
            }
            Spacer(Modifier.height(18.dp))
            Text(
                "Pembaruan Tersedia",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Versi ${info.versionName} sudah tersedia. Perbarui aplikasi untuk mendapatkan fitur dan perbaikan terbaru.",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            if (info.notes.isNotBlank()) {
                Spacer(Modifier.height(14.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 160.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BrandBlue.copy(alpha = 0.06f))
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp)
                ) {
                    Text(
                        info.notes,
                        fontSize = 12.sp,
                        color = TextSecondary,
                    )
                }
            }
            Spacer(Modifier.height(22.dp))
            if (downloading) {
                val pct = (progress * 100).toInt()
                Text(
                    "Mengunduh pembaruan... $pct%",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                if (progress > 0f) {
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp)),
                        color = BrandBlue,
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp)),
                        color = BrandBlue,
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(BrandBlue)
                        .clickableNoRipple { onUpdate(info) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Perbarui Sekarang", color = OnAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Nanti Saja",
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clickableNoRipple(onDismiss)
                        .padding(vertical = 6.dp)
                )
            }
        }
    }
}
