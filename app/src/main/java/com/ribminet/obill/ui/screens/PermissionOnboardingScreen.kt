package com.ribminet.obill.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Icon
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
import com.ribminet.obill.ui.components.PrimaryButton
import com.ribminet.obill.ui.components.clickableNoRipple
import com.ribminet.obill.ui.theme.BrandBlue
import com.ribminet.obill.ui.theme.BrandBlueSurface
import com.ribminet.obill.ui.theme.CardWhite
import com.ribminet.obill.ui.theme.Divider
import com.ribminet.obill.ui.theme.ScreenBackground
import com.ribminet.obill.ui.theme.SuccessGreen
import com.ribminet.obill.ui.theme.TextPrimary
import com.ribminet.obill.ui.theme.TextSecondary

private data class PermItem(
    val icon: ImageVector,
    val title: String,
    val desc: String,
)

@Composable
fun PermissionOnboardingScreen(onDone: () -> Unit) {
    val context = LocalContext.current

    var galleryGranted by remember { mutableStateOf(hasGalleryPermission(context)) }
    var notifGranted by remember { mutableStateOf(hasNotificationPermission(context)) }
    var smsGranted by remember { mutableStateOf(hasPermission(context, Manifest.permission.RECEIVE_SMS)) }
    var installGranted by remember { mutableStateOf(canInstallPackages(context)) }

    val multiLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        galleryGranted = hasGalleryPermission(context)
        notifGranted = hasNotificationPermission(context)
        smsGranted = result[Manifest.permission.RECEIVE_SMS] ?: smsGranted
        // Setelah izin runtime selesai diminta, arahkan ke pengaturan install bila belum aktif.
        if (!canInstallPackages(context)) openInstallSettings(context)
        onDone()
    }

    val items = listOf(
        PermItem(Icons.Filled.PhotoLibrary, "Akses Galeri", "Untuk melampirkan foto bukti pengaduan dan foto profil."),
        PermItem(Icons.Filled.Notifications, "Notifikasi & Dering", "Agar Anda menerima pemberitahuan tagihan dan status layanan."),
        PermItem(Icons.Filled.Sms, "Deteksi OTP Otomatis", "Mempercepat login dengan mengisi kode OTP secara otomatis."),
        PermItem(Icons.Filled.Download, "Install Pembaruan", "Mengizinkan pemasangan pembaruan aplikasi terbaru."),
    )
    val granted = listOf(galleryGranted, notifGranted, smsGranted, installGranted)

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier.size(72.dp).clip(CircleShape).background(BrandBlueSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text("Izin Aplikasi", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
            Spacer(Modifier.height(6.dp))
            Text(
                "Agar Obill berjalan optimal, mohon berikan izin berikut. Anda tetap dapat melanjutkan dan mengaturnya nanti.",
                color = TextSecondary, fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(20.dp))

            items.forEachIndexed { i, item ->
                PermRow(item, granted[i])
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(BrandBlueSurface)
                    .clickableNoRipple {
                        if (!installGranted) openInstallSettings(context)
                        else installGranted = canInstallPackages(context)
                    }
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (installGranted) "Izin Install: Aktif" else "Atur Izin Install Pembaruan",
                    color = if (installGranted) SuccessGreen else BrandBlue,
                    fontWeight = FontWeight.Bold, fontSize = 13.sp
                )
            }
        }

        Box(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Column {
                PrimaryButton(
                    text = "Berikan Izin & Lanjutkan",
                    onClick = {
                        installGranted = canInstallPackages(context)
                        multiLauncher.launch(runtimePermissions())
                    }
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Lewati untuk sekarang",
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickableNoRipple(onDone)
                        .padding(vertical = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun PermRow(item: PermItem, granted: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardWhite)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(BrandBlueSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(item.icon, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
            Text(item.desc, color = TextSecondary, fontSize = 11.sp)
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = if (granted) SuccessGreen else Divider,
            modifier = Modifier.size(22.dp)
        )
    }
}

private fun runtimePermissions(): Array<String> {
    val perms = mutableListOf(Manifest.permission.RECEIVE_SMS)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        perms.add(Manifest.permission.POST_NOTIFICATIONS)
        perms.add(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        perms.add(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    return perms.toTypedArray()
}

private fun hasPermission(context: Context, permission: String): Boolean =
    androidx.core.content.ContextCompat.checkSelfPermission(context, permission) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED

private fun hasGalleryPermission(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        hasPermission(context, Manifest.permission.READ_MEDIA_IMAGES)
    else hasPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)

private fun hasNotificationPermission(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)
    else true

private fun canInstallPackages(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        context.packageManager.canRequestPackageInstalls()
    else true

private fun openInstallSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        runCatching {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                )
            )
        }
    }
}
