package com.ribminet.obill.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ribminet.obill.data.Complaint
import com.ribminet.obill.data.ComplaintStatus
import com.ribminet.obill.ui.components.IconButtonRound
import com.ribminet.obill.ui.components.clickableNoRipple
import com.ribminet.obill.ui.guide.GuideTarget
import com.ribminet.obill.ui.guide.guideTarget
import com.ribminet.obill.ui.theme.AppThemeState
import com.ribminet.obill.ui.theme.BrandBlue
import com.ribminet.obill.ui.theme.CardWhite
import com.ribminet.obill.ui.theme.DangerRed
import com.ribminet.obill.ui.theme.DangerSurface
import com.ribminet.obill.ui.theme.Divider
import com.ribminet.obill.ui.theme.HeroGreenBottom
import com.ribminet.obill.ui.theme.HeroGreenTop
import com.ribminet.obill.ui.theme.OnAccent
import com.ribminet.obill.ui.theme.InfoBlueSurface
import com.ribminet.obill.ui.theme.ScreenBackground
import com.ribminet.obill.ui.theme.SuccessGreen
import com.ribminet.obill.ui.theme.SuccessSurface
import com.ribminet.obill.ui.theme.TextPrimary
import com.ribminet.obill.ui.theme.TextSecondary
import com.ribminet.obill.ui.theme.WarningOrange
import com.ribminet.obill.ui.theme.WarningSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    complaints: List<Complaint>,
    onCreate: () -> Unit,
    onDelete: (Complaint) -> Unit,
    alert: com.ribminet.obill.AppAlert?,
    onDismissAlert: () -> Unit,
    bottomBar: @Composable () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf<Complaint?>(null) }
    var statusFilter by remember { mutableStateOf<ComplaintStatus?>(null) }
    var showFilter by remember { mutableStateOf(false) }
    val sheet = rememberModalBottomSheetState()
    val filterSheet = rememberModalBottomSheetState()

    val filtered = remember(complaints, query, statusFilter) {
        complaints.filter { c ->
            val matchStatus = statusFilter == null || c.status == statusFilter
            val q = query.trim().lowercase()
            val matchQuery = q.isEmpty() ||
                c.category.lowercase().contains(q) ||
                c.problem.lowercase().contains(q) ||
                c.description.lowercase().contains(q) ||
                c.id.lowercase().contains(q)
            matchStatus && matchQuery
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(HeroGreenTop, HeroGreenBottom)))
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Riwayat Pengaduan", color = OnAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                IconButtonRound(if (AppThemeState.dark) Icons.Filled.DarkMode else Icons.Filled.LightMode, tint = OnAccent) { AppThemeState.dark = !AppThemeState.dark }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 14.sp, color = TextPrimary),
                    decorationBox = { inner ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(CardWhite)
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Box(Modifier.weight(1f)) {
                                if (query.isEmpty()) {
                                    Text("Cari keluhan...", color = TextSecondary, fontSize = 14.sp)
                                }
                                inner()
                            }
                        }
                    }
                )
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(OnAccent.copy(alpha = if (statusFilter != null) 0.30f else 0.18f))
                        .border(
                            width = if (statusFilter != null) 1.5.dp else 0.dp,
                            color = if (statusFilter != null) OnAccent else Color.Transparent,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickableNoRipple { showFilter = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Tune, contentDescription = "Filter", tint = OnAccent)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Box(modifier = Modifier.weight(1f)) {
            if (complaints.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Assignment,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Belum Ada Pengaduan", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Pengaduan yang Anda kirim ke admin akan muncul di sini.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (filtered.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Tidak Ditemukan", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Coba kata kunci lain atau ubah filter status.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, bottom = 90.dp)
                ) {
                    items(filtered, key = { it.id }) { c ->
                        ComplaintCard(c) { detail = c }
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
            FloatingActionButton(
                onClick = onCreate,
                containerColor = BrandBlue,
                contentColor = OnAccent,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .guideTarget(GuideTarget.REPORT_FAB),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Buat Laporan")
            }
        }

        bottomBar()
    }

    detail?.let { c ->
        ModalBottomSheet(onDismissRequest = { detail = null }, sheetState = sheet) {
            Column(Modifier.padding(20.dp)) {
                Text("Detail Pengaduan", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                DetailKV("Kategori", c.category)
                DetailKV("Permasalahan", c.problem)
                Row(modifier = Modifier.padding(vertical = 6.dp)) {
                    Text("Status", color = TextSecondary, modifier = Modifier.width(120.dp))
                    Text(": ", color = TextSecondary)
                    val (bg, fg) = statusColors(c.status)
                    com.ribminet.obill.ui.components.StatusBadge(c.status.name, bg, fg)
                }
                DetailKV("Tanggal Lapor", c.dateTime)
                Spacer(Modifier.height(12.dp))
                Text("Deskripsi Laporan:", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ScreenBackground)
                        .padding(14.dp)
                ) {
                    Text(c.description, color = TextSecondary, fontSize = 14.sp)
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(DangerSurface)
                            .clickableNoRipple {
                                onDelete(c)
                                detail = null
                            }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Delete, contentDescription = null, tint = DangerRed, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Hapus", color = DangerRed, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }
                    Box(Modifier.weight(1f)) {
                        com.ribminet.obill.ui.components.PrimaryButton(text = "Tutup", onClick = { detail = null })
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showFilter) {
        ModalBottomSheet(onDismissRequest = { showFilter = false }, sheetState = filterSheet) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text("Filter Status", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                FilterChipRow("Semua", selected = statusFilter == null) {
                    statusFilter = null
                    showFilter = false
                }
                Spacer(Modifier.height(10.dp))
                ComplaintStatus.entries.forEach { status ->
                    FilterChipRow(statusLabel(status), selected = statusFilter == status) {
                        statusFilter = status
                        showFilter = false
                    }
                    Spacer(Modifier.height(10.dp))
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    com.ribminet.obill.ui.components.SweetAlertDialog(alert = alert, onConfirm = onDismissAlert)
}

@Composable
private fun DetailKV(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(label, color = TextSecondary, modifier = Modifier.width(120.dp))
        Text(": ", color = TextSecondary)
        Text(value, color = TextPrimary, fontWeight = FontWeight.SemiBold)
    }
}

private fun statusColors(status: ComplaintStatus): Pair<Color, Color> = when (status) {
    ComplaintStatus.TERKIRIM -> SuccessSurface to SuccessGreen
    ComplaintStatus.SELESAI -> SuccessSurface to SuccessGreen
    ComplaintStatus.DIPROSES -> InfoBlueSurface to BrandBlue
    ComplaintStatus.MENUNGGU -> WarningSurface to WarningOrange
}

@Composable
private fun ComplaintCard(c: Complaint, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardWhite)
            .clickableNoRipple(onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(WarningSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null, tint = WarningOrange)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(c.category, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
            Text(c.description, color = TextSecondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(c.dateTime, color = TextSecondary, fontSize = 12.sp)
        }
        Spacer(Modifier.width(8.dp))
        val (bg, fg) = statusColors(c.status)
        com.ribminet.obill.ui.components.StatusBadge(c.status.name, bg, fg)
    }
}

@Composable
private fun FilterChipRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) BrandBlue.copy(alpha = 0.12f) else ScreenBackground)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) BrandBlue else Divider,
                shape = RoundedCornerShape(14.dp)
            )
            .clickableNoRipple(onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = if (selected) BrandBlue else TextPrimary, fontSize = 14.sp)
        if (selected) {
            com.ribminet.obill.ui.components.StatusBadge("Aktif", BrandBlue.copy(alpha = 0.15f), BrandBlue)
        }
    }
}

private fun statusLabel(status: ComplaintStatus): String = when (status) {
    ComplaintStatus.TERKIRIM -> "Terkirim"
    ComplaintStatus.SELESAI -> "Selesai"
    ComplaintStatus.DIPROSES -> "Diproses"
    ComplaintStatus.MENUNGGU -> "Menunggu"
}
