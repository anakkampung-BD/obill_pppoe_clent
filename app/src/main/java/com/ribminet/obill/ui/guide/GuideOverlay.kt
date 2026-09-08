package com.ribminet.obill.ui.guide

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ribminet.obill.ui.components.PrimaryButton
import com.ribminet.obill.ui.components.clickableNoRipple
import com.ribminet.obill.ui.theme.BrandBlue
import com.ribminet.obill.ui.theme.CardWhite
import com.ribminet.obill.ui.theme.TextPrimary
import com.ribminet.obill.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun UserGuideOverlay(
    session: GuideSession,
    targetBounds: Rect?,
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    val step = session.step
    val density = LocalDensity.current
    val padPx = with(density) { 8.dp.toPx() }
    val hole = targetBounds?.inflate(padPx)
    var cardHeightPx by remember(session.stepIndex) { mutableFloatStateOf(0f) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenH = constraints.maxHeight.toFloat()
        val screenW = constraints.maxWidth.toFloat()
        val margin = with(density) { 16.dp.toPx() }
        val gap = with(density) { 14.dp.toPx() }
        val fallbackCardH = with(density) { 168.dp.toPx() }
        val cardH = if (cardHeightPx > 0f) cardHeightPx else fallbackCardH
        val cardMaxWidth = with(density) { minOf(screenW * 0.92f, 400.dp.toPx()) }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val overlay = Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(Rect(0f, 0f, size.width, size.height))
                if (hole != null && hole.width > 0f && hole.height > 0f) {
                    addRoundRect(RoundRect(hole, CornerRadius(24f, 24f)))
                }
            }
            drawPath(overlay, Color.Black.copy(alpha = 0.72f))
            if (hole != null) {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.35f),
                    topLeft = hole.topLeft,
                    size = hole.size,
                    cornerRadius = CornerRadius(24f, 24f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f),
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickableNoRipple { /* block */ },
        )

        val cardTop = computeTooltipTop(
            hole = hole,
            screenH = screenH,
            cardH = cardH,
            margin = margin,
            gap = gap,
        )
        val cardLeft = ((screenW - cardMaxWidth) / 2f).coerceAtLeast(with(density) { 12.dp.toPx() })

        Column(
            modifier = Modifier
                .offset { IntOffset(cardLeft.roundToInt(), cardTop.roundToInt()) }
                .widthIn(max = with(density) { cardMaxWidth.toDp() })
                .onSizeChanged { cardHeightPx = it.height.toFloat() }
                .clip(RoundedCornerShape(16.dp))
                .background(CardWhite)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Text(
                "Panduan ${session.progressLabel}",
                color = BrandBlue,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(step.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(6.dp))
            Text(step.message, color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Lewati",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickableNoRipple(onSkip)
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                )
                Spacer(Modifier.weight(1f))
                PrimaryButton(
                    text = if (session.isLast) "Selesai" else "Lanjut",
                    onClick = onNext,
                    fillMaxWidth = false,
                    modifier = Modifier.width(96.dp),
                )
            }
        }
    }
}

/**
 * Tempatkan kartu di ruang kosong terbesar di atas/bawah target,
 * agar tidak menutupi area yang sedang dijelaskan.
 */
private fun computeTooltipTop(
    hole: Rect?,
    screenH: Float,
    cardH: Float,
    margin: Float,
    gap: Float,
): Float {
    if (hole == null) {
        return ((screenH - cardH) / 2f).coerceAtLeast(margin)
    }

    val spaceAbove = (hole.top - margin).coerceAtLeast(0f)
    val spaceBelow = (screenH - hole.bottom - margin).coerceAtLeast(0f)
    val need = cardH + gap

    // Target besar (form penuh): parkir kartu di tepi dengan ruang lebih besar.
    val largeTarget = hole.height > screenH * 0.48f

    val placeBelow = when {
        largeTarget -> spaceBelow >= spaceAbove
        spaceBelow >= need && spaceBelow >= spaceAbove -> true
        spaceAbove >= need -> false
        spaceBelow >= spaceAbove -> true
        else -> false
    }

    return if (placeBelow) {
        val top = hole.bottom + gap
        // Pastikan tidak overlap; jika kurang ruang, tempel ke bawah layar.
        when {
            top + cardH <= screenH - margin -> top
            spaceAbove >= need -> (hole.top - gap - cardH).coerceAtLeast(margin)
            else -> (screenH - cardH - margin).coerceAtLeast(margin)
        }
    } else {
        val top = hole.top - gap - cardH
        when {
            top >= margin -> top
            spaceBelow >= need -> hole.bottom + gap
            else -> margin
        }
    }
}
