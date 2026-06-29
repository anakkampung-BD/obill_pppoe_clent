package com.ribminet.obill.ui.theme

import android.app.Activity
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
private fun appColorScheme() = if (AppThemeState.dark) {
    darkColorScheme(
        primary = BrandBlue,
        onPrimary = OnAccent,
        primaryContainer = BrandBlueSurface,
        onPrimaryContainer = BrandBlueLight,
        secondary = BrandBlue,
        background = ScreenBackground,
        onBackground = TextPrimary,
        surface = CardWhite,
        onSurface = TextPrimary,
        surfaceVariant = ScreenBackground,
        onSurfaceVariant = TextSecondary,
        error = DangerRed,
        outline = Divider,
    )
} else {
    lightColorScheme(
        primary = BrandBlue,
        onPrimary = OnAccent,
        primaryContainer = BrandBlueSurface,
        onPrimaryContainer = BrandBlueDark,
        secondary = BrandBlue,
        background = ScreenBackground,
        onBackground = TextPrimary,
        surface = CardWhite,
        onSurface = TextPrimary,
        surfaceVariant = ScreenBackground,
        onSurfaceVariant = TextSecondary,
        error = DangerRed,
        outline = Divider,
    )
}

@Composable
fun RibmiNetTheme(content: @Composable () -> Unit) {
    val colorScheme = appColorScheme()
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = HeroGreenBottom.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = {
            CompositionLocalProvider(
                LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = Poppins),
                LocalContentColor provides TextPrimary,
            ) {
                content()
            }
        }
    )
}
