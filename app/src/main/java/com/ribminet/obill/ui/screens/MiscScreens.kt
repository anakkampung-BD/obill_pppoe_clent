package com.ribminet.obill.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
import com.ribminet.obill.data.remote.NotificationDto
import com.ribminet.obill.data.remote.formatDateId
import com.ribminet.obill.ui.components.AppTopBar
import com.ribminet.obill.ui.components.PullToRefresh
import com.ribminet.obill.ui.components.SecondaryButton
import com.ribminet.obill.ui.components.clickableNoRipple
import com.ribminet.obill.ui.theme.BrandBlue
import com.ribminet.obill.ui.theme.CardWhite
import com.ribminet.obill.ui.theme.IconChipBlue
import com.ribminet.obill.ui.theme.InfoBlueSurface
import com.ribminet.obill.ui.theme.OnAccent
import com.ribminet.obill.ui.theme.SuccessGreen
import com.ribminet.obill.ui.theme.TextPrimary
import com.ribminet.obill.ui.theme.TextSecondary

private const val CS_PHONE = "082178277876"
private const val CS_WA_INTL = "6282178277876"

@Composable
fun HelpScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val faq = listOf(
        "Bagaimana cara masuk ke aplikasi?" to "Masukkan nomor WhatsApp yang terdaftar, lalu masukkan kode OTP yang dikirim ke WhatsApp Anda. Jika nomor belum terdaftar, hubungi admin.",
        "Bagaimana cara membayar atau memperpanjang tagihan?" to "Buka menu Tagihan, tekan \"Bayar Tagihan\", pilih metode pembayaran (Tunai, Transfer Bank, atau QRIS), lalu buat pesanan. Setelah membayar, tekan \"Saya Sudah Bayar\" untuk konfirmasi.",
        "Apa saja metode pembayaran yang tersedia?" to "Sesuai pengaturan admin: Tunai (bayar di kantor), Transfer Bank (ke rekening yang tertera), dan QRIS (scan kode QR). Pilihan yang muncul mengikuti yang diaktifkan admin.",
        "Setelah konfirmasi bayar, apa langkah berikutnya?" to "Status pesanan menjadi \"Menunggu Konfirmasi\". Admin akan memverifikasi pembayaran Anda, lalu layanan diperpanjang otomatis dan status berubah menjadi \"Pembayaran Berhasil\".",
        "Bagaimana cara upgrade paket?" to "Buka menu Ganti Paket, pilih paket tujuan, pilih metode pembayaran, lalu selesaikan pembayaran seperti tagihan biasa.",
        "Bagaimana memeriksa status pesanan saya?" to "Buka Profil > Riwayat Pesanan untuk melihat semua pesanan beserta statusnya. Tarik layar ke bawah untuk menyegarkan.",
        "Internet saya lambat atau mati, apa solusinya?" to "Cek status perangkat di Beranda. Coba Refresh atau Reboot perangkat di menu Pengaturan WiFi. Jika masih bermasalah, kirim pengaduan lewat menu Laporan.",
        "Bagaimana mengubah nama (SSID) & password WiFi?" to "Buka menu Pengaturan WiFi, ubah nama SSID dan/atau password, lalu simpan. Perubahan dikirim ke perangkat Anda.",
        "Bagaimana cara mengirim pengaduan?" to "Buka menu Laporan, tekan tombol tambah, isi kategori dan deskripsi keluhan, lampirkan foto bila perlu, lalu kirim. Pengaduan diteruskan ke admin via WhatsApp.",
        "Bagaimana mengubah biodata saya?" to "Buka Profil > Edit Biodata. Anda dapat mengubah nama, email, alamat, dan foto profil. Nomor WhatsApp dan data paket hanya dapat diubah oleh admin.",
    )
    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "FAQ & Kontak", onBack = onBack)
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
            ) {
                SecondaryButton(text = "WhatsApp CS", icon = Icons.Filled.Chat, color = SuccessGreen, modifier = Modifier.weight(1f)) {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$CS_WA_INTL"))
                        )
                    }
                }
                SecondaryButton(text = "Telepon", icon = Icons.Filled.Call, modifier = Modifier.weight(1f)) {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$CS_PHONE"))
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("Pertanyaan Umum (FAQ)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(12.dp))
            faq.forEach { (q, a) -> FaqItem(q, a); Spacer(Modifier.height(10.dp)) }
        }
    }
}

@Composable
private fun FaqItem(question: String, answer: String) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardWhite)
            .clickableNoRipple { expanded = !expanded }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(question, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.weight(1f), fontSize = 14.sp)
            Icon(Icons.Filled.ExpandMore, contentDescription = null, tint = TextSecondary)
        }
        if (expanded) {
            Spacer(Modifier.height(8.dp))
            Text(answer, color = TextSecondary, fontSize = 13.sp)
        }
    }
}

private fun notificationIcon(type: String?): ImageVector = when (type) {
    "payment_verified" -> Icons.Filled.Payments
    "payment_rejected" -> Icons.Filled.Cancel
    "expiry_reminder_24h" -> Icons.Filled.Schedule
    "billing_reminder" -> Icons.Filled.Info
    else -> Icons.Filled.Notifications
}

@Composable
fun NotificationsScreen(
    notifications: List<NotificationDto>,
    loading: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onMarkRead: (Int) -> Unit,
    onMarkAllRead: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "Notifikasi", onBack = onBack)
        if (notifications.any { it.isRead != true }) {
            Text(
                "Tandai semua dibaca",
                color = BrandBlue,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickableNoRipple(onMarkAllRead)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        PullToRefresh(refreshing = loading, onRefresh = onRefresh) {
            when {
                loading && notifications.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BrandBlue)
                    }
                }
                notifications.isEmpty() -> {
                    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("Belum ada notifikasi.", color = TextSecondary, fontSize = 13.sp)
                    }
                }
                else -> LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
                    items(notifications, key = { it.id ?: it.hashCode() }) { n ->
                        val unread = n.isRead != true
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (unread) InfoBlueSurface else CardWhite)
                                .clickableNoRipple { n.id?.let(onMarkRead) }
                                .padding(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(IconChipBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    notificationIcon(n.type),
                                    contentDescription = null,
                                    tint = BrandBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(n.title ?: "-", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                                Text(n.body ?: "", color = TextSecondary, fontSize = 13.sp)
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    n.createdAt?.let { formatDateId(it) } ?: "",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun TwoFactorScreen(onBack: () -> Unit) {
    var enabled by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "Verifikasi 2 Langkah", onBack = onBack)
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(InfoBlueSurface)
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Tambahkan lapisan keamanan ekstra dengan kode OTP via WhatsApp setiap kali Anda masuk.",
                        color = TextPrimary, fontSize = 13.sp
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardWhite)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Aktifkan Verifikasi 2 Langkah", fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(if (enabled) "Status: Aktif" else "Status: Nonaktif", color = if (enabled) SuccessGreen else TextSecondary, fontSize = 13.sp)
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = { enabled = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = OnAccent, checkedTrackColor = BrandBlue)
                )
            }
        }
    }
}

@Composable
fun SimpleDocScreen(title: String, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = title, onBack = onBack)
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            repeat(5) { i ->
                Text("${i + 1}. Ketentuan Layanan", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Dengan menggunakan layanan Ribmi Net, pelanggan menyetujui seluruh ketentuan yang berlaku terkait penggunaan layanan internet, penagihan, serta kebijakan data dan privasi yang ditetapkan oleh penyedia layanan.",
                    color = TextSecondary, fontSize = 13.sp
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
