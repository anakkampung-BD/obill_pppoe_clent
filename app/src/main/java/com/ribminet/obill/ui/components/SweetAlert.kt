package com.ribminet.obill.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ribminet.obill.AppAlert
import com.ribminet.obill.AlertType
import com.ribminet.obill.ui.theme.BrandBlue
import com.ribminet.obill.ui.theme.CardWhite
import com.ribminet.obill.ui.theme.DangerRed
import com.ribminet.obill.ui.theme.OnAccent
import com.ribminet.obill.ui.theme.SuccessGreen
import com.ribminet.obill.ui.theme.TextPrimary
import com.ribminet.obill.ui.theme.TextSecondary
import com.ribminet.obill.ui.theme.WarningOrange

@Composable
fun SweetAlertDialog(alert: AppAlert?, onConfirm: () -> Unit) {
    SweetAlertDialog(alert, onConfirm, onDismiss = onConfirm)
}

@Composable
fun SweetAlertDialog(
    alert: AppAlert?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (alert == null) return

    val color = when (alert.type) {
        AlertType.SUCCESS -> SuccessGreen
        AlertType.ERROR -> DangerRed
        AlertType.WARNING -> WarningOrange
        AlertType.INFO -> BrandBlue
    }
    val icon: ImageVector = when (alert.type) {
        AlertType.SUCCESS -> Icons.Filled.Check
        AlertType.ERROR -> Icons.Filled.Close
        AlertType.WARNING -> Icons.Filled.PriorityHigh
        AlertType.INFO -> Icons.Filled.Info
    }

    val scale = remember { Animatable(0.7f) }
    LaunchedEffect(alert) { scale.snapTo(0.7f); scale.animateTo(1f) }

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
                    .background(color.copy(alpha = 0.12f))
                    .border(3.dp, color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(18.dp))
            Text(
                alert.title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                alert.message,
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(22.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(color)
                    .clickableNoRipple(onConfirm)
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(alert.confirmText, color = OnAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}
