package com.ribminet.obill.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ribminet.obill.ui.theme.BrandBlue

/**
 * Klik dengan efek ripple Material.
 * Nama [clickableNoRipple] dipertahankan agar pemanggilan lama tidak berubah.
 */
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = clickableRipple(onClick = onClick)

fun Modifier.clickableRipple(
    bounded: Boolean = true,
    radius: Dp = Dp.Unspecified,
    color: Color = BrandBlue.copy(alpha = 0.18f),
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    clickable(
        interactionSource = interaction,
        indication = rememberRipple(bounded = bounded, radius = radius, color = color),
        enabled = enabled,
        onClick = onClick,
    )
}
