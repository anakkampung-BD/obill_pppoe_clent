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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ribminet.obill.AppViewModel
import com.ribminet.obill.ui.components.AppTextField
import com.ribminet.obill.ui.components.AppTopBar
import com.ribminet.obill.ui.components.PrimaryButton
import com.ribminet.obill.ui.components.SectionLabel
import com.ribminet.obill.ui.components.SweetAlertDialog
import com.ribminet.obill.ui.theme.BrandBlue
import com.ribminet.obill.ui.theme.BrandBlueDark
import com.ribminet.obill.ui.theme.CardWhite
import com.ribminet.obill.ui.theme.Divider
import com.ribminet.obill.ui.theme.OnAccent
import com.ribminet.obill.ui.theme.ScreenBackground
import com.ribminet.obill.ui.theme.TextSecondary

@Composable
fun EditBiodataScreen(vm: AppViewModel, onBack: () -> Unit, onSaved: () -> Unit) {
    val user = vm.profile
    var name by remember { mutableStateOf(user?.fullName ?: "") }
    var email by remember { mutableStateOf(user?.email ?: "") }
    var address by remember { mutableStateOf(user?.address ?: "") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var savePending by remember { mutableStateOf(false) }

    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) photoUri = uri }
    val openGallery = {
        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "Edit Biodata", onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(104.dp)
                            .clip(CircleShape)
                            .background(CardWhite)
                            .clickable { openGallery() },
                        contentAlignment = Alignment.Center
                    ) {
                        val model: Any? = photoUri ?: user?.photoUrl?.takeIf { it.isNotBlank() }
                        if (model != null) {
                            AsyncImage(
                                model = model,
                                contentDescription = "Foto profil",
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                com.ribminet.obill.util.initialsOf(name.ifBlank { user?.fullName }),
                                color = BrandBlue, fontWeight = FontWeight.Bold, fontSize = 20.sp
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(BrandBlueDark)
                            .clickable { openGallery() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = OnAccent, modifier = Modifier.size(18.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Ketuk foto untuk mengganti (JPEG/PNG/WEBP, maks 5 MB)",
                color = TextSecondary, fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))
            SectionLabel("Informasi Pribadi")
            Spacer(Modifier.height(12.dp))
            AppTextField(name, { name = it }, "Nama Lengkap", label = "Nama Lengkap", leadingIcon = Icons.Filled.Person)
            Spacer(Modifier.height(14.dp))
            AppTextField(email, { email = it }, "Alamat Email", label = "Email", leadingIcon = Icons.Filled.Email, keyboardType = KeyboardType.Email)
            Spacer(Modifier.height(14.dp))
            AppTextField(address, { address = it }, "Alamat Tempat Tinggal", label = "Alamat", leadingIcon = Icons.Filled.Home, singleLine = false, minLines = 3)

            Spacer(Modifier.height(20.dp))
            SectionLabel("Data Layanan (tidak dapat diubah)")
            Spacer(Modifier.height(12.dp))
            ReadOnlyField(Icons.Filled.PhoneAndroid, "Nomor WhatsApp", user?.whatsapp?.ifBlank { "-" } ?: "-")
            Spacer(Modifier.height(10.dp))
            ReadOnlyField(Icons.Filled.Lock, "Username PPPoE", user?.username?.ifBlank { "-" } ?: "-")
            Spacer(Modifier.height(6.dp))
            Text(
                "Untuk mengubah data layanan, silakan hubungi admin.",
                color = TextSecondary, fontSize = 12.sp
            )
        }
        Column(modifier = Modifier
            .background(ScreenBackground)
            .padding(16.dp)) {
            PrimaryButton(
                text = if (vm.biodataSubmitting) "Menyimpan..." else "Simpan Perubahan",
                enabled = !vm.biodataSubmitting && name.isNotBlank(),
                onClick = {
                    vm.updateBiodata(name = name, email = email, address = address, photoUri = photoUri) {
                        savePending = true
                    }
                }
            )
        }
    }

    SweetAlertDialog(alert = vm.alert) {
        vm.dismissAlert()
        if (savePending) {
            savePending = false
            onSaved()
        }
    }
}

@Composable
private fun ReadOnlyField(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .background(CardWhite)
            .border(1.dp, Divider, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = TextSecondary, fontSize = 12.sp)
            Text(value, color = com.ribminet.obill.ui.theme.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}
