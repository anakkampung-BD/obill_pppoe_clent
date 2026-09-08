package com.ribminet.obill.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

object AppThemeState {
    var dark by mutableStateOf(false)
}

private fun pick(light: Color, dark: Color): Color = if (AppThemeState.dark) dark else light

// Brand & accent colors (sama untuk light & dark)
val BrandBlue = Color(0xFF16ABE3)
val BrandBlueDark = Color(0xFF0E96CC)
val BrandBlueLight = Color(0xFF5BC6F0)

val SuccessGreen = Color(0xFF1FA75A)
val WarningOrange = Color(0xFFF6A623)
val DangerRed = Color(0xFFE5443B)

// Teks/ikon putih di atas warna aksen (selalu putih di kedua mode)
val OnAccent = Color(0xFFFFFFFF)

// Hero: green gradient lembut (~20% ketajaman) namun tetap terasa hijau
val HeroGreenTop = Color(0xFF4FC68C)
val HeroGreenBottom = Color(0xFF1FA75A)

// Warna semantik theme-aware
val ScreenBackground: Color get() = pick(Color(0xFFF2F7FB), Color(0xFF0F1216))
val CardWhite: Color get() = pick(Color(0xFFFFFFFF), Color(0xFF1B1F25))

val TextPrimary: Color get() = pick(Color(0xFF24303A), Color(0xFFECEFF2))
val TextSecondary: Color get() = pick(Color(0xFF8A94A0), Color(0xFF99A3AD))
val TextMuted: Color get() = pick(Color(0xFFAAB2BB), Color(0xFF6B7480))

val Divider: Color get() = pick(Color(0xFFEDF0F3), Color(0xFF2A2F36))

val BrandBlueSurface: Color get() = pick(Color(0xFFE7F6FC), Color(0xFF13313D))
val IconChipBlue: Color get() = pick(Color(0xFFE7F6FC), Color(0xFF13313D))
val SuccessSurface: Color get() = pick(Color(0xFFE9F8EF), Color(0xFF12301F))
val WarningSurface: Color get() = pick(Color(0xFFFFF3E0), Color(0xFF3A2E15))
val DangerSurface: Color get() = pick(Color(0xFFFDECEA), Color(0xFF3A1F1D))
val InfoBlueSurface: Color get() = pick(Color(0xFFE6F4FB), Color(0xFF13303C))
