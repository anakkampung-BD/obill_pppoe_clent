package com.ribminet.obill.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ribminet.obill.ui.theme.BrandBlue
import com.ribminet.obill.ui.theme.CardWhite
import com.ribminet.obill.ui.theme.Divider
import com.ribminet.obill.ui.theme.OnAccent
import com.ribminet.obill.ui.theme.TextPrimary
import com.ribminet.obill.ui.theme.TextSecondary

/** Permintaan konfirmasi yang ditampilkan sebelum sebuah aksi dijalankan. */
data class ConfirmRequest(
    val title: String,
    val message: String,
    val confirmText: String = "Ya, Lanjutkan",
    val cancelText: String = "Batal",
    val icon: ImageVector = Icons.Filled.HelpOutline,
    val accent: androidx.compose.ui.graphics.Color = BrandBlue,
    val onConfirm: () -> Unit,
)

@Composable
fun ConfirmDialog(request: ConfirmRequest?, onDismiss: () -> Unit) {
    if (request == null) return

    val scale = remember { Animatable(0.7f) }
    LaunchedEffect(request) { scale.snapTo(0.7f); scale.animateTo(1f) }

    Dialog(onDismissRequest = onDismiss) {
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
                    .background(request.accent.copy(alpha = 0.12f))
                    .border(3.dp, request.accent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(request.icon, contentDescription = null, tint = request.accent, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(18.dp))
            Text(
                request.title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                request.message,
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(22.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Divider, RoundedCornerShape(12.dp))
                        .clickableNoRipple(onDismiss)
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(request.cancelText, color = TextSecondary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(request.accent)
                        .clickableNoRipple {
                            onDismiss()
                            request.onConfirm()
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(request.confirmText, color = OnAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
