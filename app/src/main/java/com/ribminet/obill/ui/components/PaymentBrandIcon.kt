package com.ribminet.obill.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ribminet.obill.ui.theme.BrandBlue
import com.ribminet.obill.ui.theme.BrandBlueSurface

// Warna brand internal (logo tidak tersedia dari API, disiapkan di aplikasi)
private val BcaBlue = Color(0xFF0060AF)
private val BcaAccent = Color(0xFFE61E2B)
private val DanaBlue = Color(0xFF108EE9)

/**
 * Ikon brand metode pembayaran. Untuk BCA & DANA memakai wordmark berwarna brand
 * yang disiapkan internal; untuk lainnya memakai ikon generik.
 */
@Composable
fun PaymentBrandIcon(
    iconKey: String,
    fallbackText: String,
    modifier: Modifier = Modifier.size(44.dp),
) {
    when (iconKey) {
        "bca" -> Box(
            modifier = modifier.clip(RoundedCornerShape(8.dp)).background(Color.White)
                .then(Modifier.size(44.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Wordmark "BCA" biru dengan aksen merah (gaya logo BCA)
            Box(contentAlignment = Alignment.Center) {
                Text("BCA", color = BcaBlue, fontWeight = FontWeight.Black, fontSize = 15.sp)
            }
        }
        "dana" -> Box(
            modifier = modifier.clip(RoundedCornerShape(8.dp)).background(DanaBlue),
            contentAlignment = Alignment.Center
        ) {
            Text("dana", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        "cash" -> Box(
            modifier = modifier.clip(RoundedCornerShape(8.dp)).background(BrandBlueSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Payments, contentDescription = null, tint = BrandBlue)
        }
        else -> Box(
            modifier = modifier.clip(RoundedCornerShape(8.dp)).background(BrandBlueSurface),
            contentAlignment = Alignment.Center
        ) {
            if (fallbackText.isNotBlank()) {
                Text(fallbackText, color = BrandBlue, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            } else {
                Icon(Icons.Filled.AccountBalance, contentDescription = null, tint = BrandBlue)
            }
        }
    }
}
