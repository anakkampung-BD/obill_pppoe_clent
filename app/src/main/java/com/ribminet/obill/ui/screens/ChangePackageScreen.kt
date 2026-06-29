package com.ribminet.obill.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ribminet.obill.data.InternetPackage
import com.ribminet.obill.data.remote.PendingChangeDto
import com.ribminet.obill.ui.components.AppTopBar
import com.ribminet.obill.ui.components.ConfirmDialog
import com.ribminet.obill.ui.components.ConfirmRequest
import com.ribminet.obill.ui.components.StatusBadge
import com.ribminet.obill.ui.components.SweetAlertDialog
import com.ribminet.obill.ui.components.clickableNoRipple
import com.ribminet.obill.ui.theme.BrandBlue
import com.ribminet.obill.ui.theme.BrandBlueSurface
import com.ribminet.obill.ui.theme.CardWhite
import com.ribminet.obill.ui.theme.DangerRed
import com.ribminet.obill.ui.theme.Divider
import com.ribminet.obill.ui.theme.InfoBlueSurface
import com.ribminet.obill.ui.theme.OnAccent
import com.ribminet.obill.ui.theme.SuccessGreen
import com.ribminet.obill.ui.theme.SuccessSurface
import com.ribminet.obill.ui.theme.TextPrimary
import com.ribminet.obill.ui.theme.TextSecondary
import com.ribminet.obill.ui.theme.WarningOrange
import com.ribminet.obill.ui.theme.WarningSurface
import com.ribminet.obill.util.rupiah

@Composable
fun ChangePackageScreen(
    packages: List<InternetPackage>,
    loading: Boolean,
    error: String?,
    pendingChange: PendingChangeDto?,
    packagesSource: String?,
    submitting: Boolean,
    alert: com.ribminet.obill.AppAlert?,
    onDismissAlert: () -> Unit,
    onBack: () -> Unit,
    onRequestChange: (InternetPackage) -> Unit,
    onCancelPending: () -> Unit,
) {
    var confirm by remember { mutableStateOf<ConfirmRequest?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "Ganti Paket Langganan", onBack = onBack)
        when {
            loading && packages.isEmpty() -> LoadingState()
            error != null && packages.isEmpty() -> ErrorState(error)
            else -> LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
                if (pendingChange != null) {
                    item {
                        PendingChangeCard(pendingChange, submitting) {
                            confirm = ConfirmRequest(
                                title = "Batalkan Permohonan?",
                                message = "Permohonan ubah paket ke ${pendingChange.toProfileName ?: "-"} akan dibatalkan. Tagihan periode berikutnya kembali ke paket lama.",
                                confirmText = "Ya, Batalkan",
                                cancelText = "Tidak",
                                accent = DangerRed,
                                onConfirm = onCancelPending,
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                    }
                }
                packagesSourceNote(packagesSource)?.let { note ->
                    item {
                        InfoNote(note, warn = packagesSource in setOf("current_only", "all"))
                        Spacer(Modifier.height(14.dp))
                    }
                }
                val selectable = packages.filter { pkg ->
                    !pkg.current && pendingChange?.toProfileId?.toString() != pkg.id
                }
                if (!loading && packages.isNotEmpty() && selectable.isEmpty() && pendingChange == null) {
                    item {
                        InfoNote(
                            "Belum ada paket lain yang tersedia untuk diubah. Coba refresh atau hubungi CS.",
                            warn = true,
                        )
                        Spacer(Modifier.height(14.dp))
                    }
                }
                items(packages, key = { it.id }) { pkg ->
                    val isPendingTarget = pendingChange?.toProfileId?.toString() == pkg.id
                    PackageCard(pkg, isPendingTarget) {
                        confirm = ConfirmRequest(
                            title = "Ajukan Ubah Paket?",
                            message = "Anda akan mengajukan perubahan ke paket ${pkg.name} (${rupiah(pkg.price)}/bln). Paket baru berjalan pada periode berikutnya setelah pembayaran tagihan diverifikasi.",
                            confirmText = "Ya, Ajukan",
                            onConfirm = { onRequestChange(pkg) },
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                }
            }
        }
    }

    ConfirmDialog(request = confirm, onDismiss = { confirm = null })
    SweetAlertDialog(alert = alert, onConfirm = onDismissAlert)
}

@Composable
private fun PendingChangeCard(pending: PendingChangeDto, submitting: Boolean, onCancel: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(WarningSurface)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Schedule, contentDescription = null, tint = WarningOrange)
            Spacer(Modifier.width(10.dp))
            Text("Perubahan Paket Terjadwal", fontWeight = FontWeight.Bold, color = WarningOrange, fontSize = 14.sp)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "${pending.fromProfileName ?: "-"}  →  ${pending.toProfileName ?: "-"}",
            fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp
        )
        Text("Harga baru: ${rupiah(pending.newPrice ?: 0L)} /bln", color = TextSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            pending.effectiveNote ?: "Paket baru berjalan pada periode berikutnya setelah pembayaran tagihan diverifikasi.",
            color = TextSecondary, fontSize = 12.sp
        )
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(CardWhite)
                .clickableNoRipple { if (!submitting) onCancel() }
                .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            Text(if (submitting) "Memproses..." else "Batalkan Permohonan", color = DangerRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

/** Pesan info sesuai sumber daftar paket dari API `package_options`. */
private fun packagesSourceNote(source: String?): String? = when (source) {
    "current_only" -> "Belum ada data paket lain yang tersedia. Hubungi CS jika ingin mengubah paket."
    "all" -> "Perangkat router belum terdaftar pada akun Anda. Hubungi CS untuk bantuan mengubah paket."
    else -> null
}

@Composable
private fun InfoNote(text: String, warn: Boolean = false) {
    val bg = if (warn) WarningSurface else InfoBlueSurface
    val tint = if (warn) WarningOrange else BrandBlue
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Info, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, color = TextSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        androidx.compose.material3.CircularProgressIndicator(color = BrandBlue)
    }
}

@Composable
private fun ErrorState(message: String) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(message, color = TextSecondary, fontSize = 13.sp)
    }
}

@Composable
private fun PackageCard(pkg: InternetPackage, isPendingTarget: Boolean, onSelect: (InternetPackage) -> Unit) {
    val highlight = pkg.current || isPendingTarget
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardWhite)
            .border(
                width = if (highlight) 2.dp else 1.dp,
                color = if (isPendingTarget) WarningOrange else if (pkg.current) BrandBlue else Divider,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BrandBlueSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Speed, contentDescription = null, tint = BrandBlue)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(pkg.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                Text(pkg.speed, color = TextSecondary, fontSize = 13.sp)
            }
            when {
                isPendingTarget -> StatusBadge("Terjadwal", WarningSurface, WarningOrange)
                pkg.current -> StatusBadge("Aktif", SuccessSurface, SuccessGreen)
                pkg.popular -> StatusBadge("Upgrade", WarningSurface, WarningOrange)
            }
        }
        Spacer(Modifier.height(12.dp))
        pkg.features.forEach { f ->
            Row(modifier = Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(f, color = TextSecondary, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(rupiah(pkg.price), color = BrandBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(" /bln", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(bottom = 2.dp))
            }
            if (!pkg.current && !isPendingTarget) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(BrandBlue)
                        .clickableNoRipple { onSelect(pkg) }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text("Ajukan", color = OnAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
