package com.ribminet.obill.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ribminet.obill.ui.theme.AppThemeState
import com.ribminet.obill.ui.theme.ScreenBackground

private val BlobTeal = Color(0xFFBFE8D8)
private val BlobBlue = Color(0xFFC8E6F5)

/** Latar soft + blob (pola beranda) untuk seluruh aplikasi. */
@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground),
    ) {
        if (!AppThemeState.dark) {
            SoftBlob(size = 240.dp, x = (-80).dp, y = 80.dp, color = BlobTeal, alpha = 0.45f)
            SoftBlob(size = 200.dp, x = 220.dp, y = 240.dp, color = BlobBlue, alpha = 0.40f)
            SoftBlob(size = 180.dp, x = (-40).dp, y = 480.dp, color = BlobBlue, alpha = 0.30f)
            SoftBlob(size = 220.dp, x = 200.dp, y = 640.dp, color = BlobTeal, alpha = 0.28f)
            SoftBlob(size = 160.dp, x = 40.dp, y = 820.dp, color = BlobBlue, alpha = 0.22f)
        }
        content()
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
