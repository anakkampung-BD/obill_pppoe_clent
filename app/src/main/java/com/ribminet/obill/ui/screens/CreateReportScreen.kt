package com.ribminet.obill.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ribminet.obill.AppViewModel
import com.ribminet.obill.data.DummyData
import com.ribminet.obill.ui.components.AppTextField
import com.ribminet.obill.ui.components.AppTopBar
import com.ribminet.obill.ui.components.Dropdown
import com.ribminet.obill.ui.components.PrimaryButton
import com.ribminet.obill.ui.components.SweetAlertDialog
import com.ribminet.obill.ui.theme.BrandBlue
import com.ribminet.obill.ui.theme.DangerRed
import com.ribminet.obill.ui.theme.InfoBlueSurface
import com.ribminet.obill.ui.theme.OnAccent
import com.ribminet.obill.ui.theme.ScreenBackground
import com.ribminet.obill.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReportScreen(
    vm: AppViewModel,
    onBack: () -> Unit,
    onSubmit: (category: String, problem: String, description: String) -> Unit,
    onComplaintSent: () -> Unit,
) {
    var category by remember { mutableStateOf("") }
    var problem by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var sentPending by remember { mutableStateOf(false) }
    val problems = DummyData.complaintProblems[category] ?: emptyList()

    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) photoUri = uri }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "Buat Laporan", onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Sampaikan Kendala Anda", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                "Laporan Anda akan langsung ditinjau oleh tim teknisi kami. Kami berkomitmen untuk memberikan solusi tercepat bagi kenyamanan koneksi internet Anda.",
                color = TextSecondary,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(20.dp))

            Dropdown(
                label = "Kategori",
                placeholder = "Pilih Kategori",
                value = category,
                options = DummyData.complaintCategories,
                onSelect = { category = it; problem = "" }
            )
            Spacer(Modifier.height(14.dp))
            Dropdown(
                label = "Permasalahan",
                placeholder = "Pilih Permasalahan",
                value = problem,
                options = problems,
                enabled = category.isNotEmpty(),
                onSelect = { problem = it }
            )
            Spacer(Modifier.height(14.dp))
            AppTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = "Deskripsi Kendala (wajib diisi)",
                singleLine = false,
                minLines = 4
            )
            Spacer(Modifier.height(14.dp))
            val openGallery = {
                pickImage.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
            if (photoUri != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(InfoBlueSurface)
                        .clickable { openGallery() }
                ) {
                    AsyncImage(
                        model = photoUri,
                        contentDescription = "Bukti pengaduan",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(DangerRed)
                            .clickable { photoUri = null },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Hapus foto", tint = OnAccent, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text("Ketuk gambar untuk mengganti foto", color = TextSecondary, fontSize = 12.sp)
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(InfoBlueSurface)
                        .border(1.dp, BrandBlue.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                        .clickable { openGallery() },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(30.dp))
                        Spacer(Modifier.height(6.dp))
                        Text("Upload Bukti (opsional)", color = TextSecondary, fontSize = 13.sp)
                        Text("JPEG/PNG/WEBP, maks 5 MB", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }
        Box(modifier = Modifier
            .background(ScreenBackground)
            .padding(16.dp)) {
            PrimaryButton(
                text = if (vm.complaintSubmitting) "Mengirim..." else "Kirim Laporan",
                enabled = !vm.complaintSubmitting && category.isNotEmpty() && description.isNotBlank(),
                onClick = {
                    vm.submitComplaint(category = category, subject = problem, message = description, photoUri = photoUri) {
                        onSubmit(category, problem, description)
                        category = ""; problem = ""; description = ""; photoUri = null
                        sentPending = true
                    }
                }
            )
        }
    }

    SweetAlertDialog(alert = vm.alert) {
        vm.dismissAlert()
        if (sentPending) {
            sentPending = false
            onComplaintSent()
        }
    }
}
