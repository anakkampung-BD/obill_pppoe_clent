package com.ribminet.obill.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ribminet.obill.util.rupiah

private val QrisRed = Color(0xFFE30613)
private val QrisGpnBlue = Color(0xFF0033A0)
private val QrisInk = Color(0xFF111111)
private val QrisMuted = Color(0xFF666666)
private val QrisCardBg = Color(0xFFFFFFFF)
private val QrisPattern = Color(0xFFE8E8E8)

/**
 * Kartu tampilan QRIS dinamis mengikuti layout cetak standar QRIS
 * (logo QRIS/GPN, nominal bayar, merchant, NMID, QR, tagline, cara bayar).
 */
@Composable
fun QrisStandardCard(
    qrisImageUrl: String?,
    payAmount: Long? = null,
    merchantName: String = "D'BESTIE CAFE",
    nmid: String = "ID1024325805181",
    terminalId: String = "A01",
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(12.dp))
            .background(QrisCardBg),
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val step = 18.dp.toPx()
            var y = 0f
            while (y < h) {
                var x = 0f
                while (x < w) {
                    drawCircle(
                        color = QrisPattern.copy(alpha = 0.55f),
                        radius = 1.2.dp.toPx(),
                        center = Offset(x, y),
                    )
                    x += step
                }
                y += step
            }
            val left = Path().apply {
                moveTo(0f, h * 0.42f)
                lineTo(w * 0.18f, h * 0.55f)
                lineTo(0f, h * 0.72f)
                close()
            }
            drawPath(left, QrisRed)
            val right = Path().apply {
                moveTo(w * 0.55f, h)
                lineTo(w, h * 0.58f)
                lineTo(w, h)
                close()
            }
            drawPath(right, QrisRed)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            QrisHeader(payAmount = payAmount)

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                merchantName,
                color = QrisInk,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
            )
            Text(
                "NMID: $nmid",
                color = QrisInk,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
            )
            Text(
                terminalId,
                color = QrisInk,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .size(220.dp)
                    .background(Color.White)
                    .padding(6.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (!qrisImageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = qrisImageUrl,
                        contentDescription = "QRIS",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        Icons.Filled.QrCode2,
                        contentDescription = null,
                        tint = QrisMuted,
                        modifier = Modifier.size(72.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "SATU QRIS UNTUK SEMUA",
                color = QrisInk,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 0.4.sp,
            )
            Text(
                "Cek aplikasi penyelenggara di: www.aspi-qris.id",
                color = QrisMuted,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(14.dp))

            QrisHowToPay()
        }
    }
}

@Composable
private fun QrisHeader(payAmount: Long?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "QRIS",
                color = QrisInk,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                letterSpacing = 1.sp,
            )
            Text(
                "QR Code Standar\nPembayaran Nasional",
                color = QrisInk,
                fontSize = 9.sp,
                lineHeight = 11.sp,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            payAmount?.let {
                Text(
                    rupiah(it),
                    color = QrisRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.End,
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Canvas(modifier = Modifier.size(28.dp)) {
                    val stroke = Stroke(width = 2.dp.toPx())
                    drawCircle(QrisRed, radius = size.minDimension / 2.2f, style = stroke)
                    val path = Path().apply {
                        moveTo(size.width * 0.5f, size.height * 0.22f)
                        lineTo(size.width * 0.78f, size.height * 0.55f)
                        lineTo(size.width * 0.55f, size.height * 0.52f)
                        lineTo(size.width * 0.62f, size.height * 0.78f)
                        lineTo(size.width * 0.38f, size.height * 0.78f)
                        lineTo(size.width * 0.45f, size.height * 0.52f)
                        lineTo(size.width * 0.22f, size.height * 0.55f)
                        close()
                    }
                    drawPath(path, QrisRed)
                }
                Text(
                    "GPN",
                    color = QrisGpnBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                )
            }
        }
    }
}

@Composable
private fun QrisHowToPay() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(QrisRed)
            .padding(horizontal = 10.dp, vertical = 10.dp),
    ) {
        Text(
            "Cara bayar dengan QRIS:",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            HowToStep(Icons.Filled.PhoneAndroid, "Buka Aplikasi\nBerlogo QRIS")
            HowToStep(Icons.Filled.QrCodeScanner, "Scan dan Cek")
            HowToStep(Icons.Filled.Check, "Bayar")
        }
    }
}

@Composable
private fun HowToStep(icon: ImageVector, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(88.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = QrisInk, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label,
            color = Color.White,
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
            lineHeight = 11.sp,
        )
    }
}
