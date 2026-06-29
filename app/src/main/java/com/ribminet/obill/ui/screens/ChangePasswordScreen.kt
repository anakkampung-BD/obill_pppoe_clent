package com.ribminet.obill.ui.screens

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VerifiedUser
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
import com.ribminet.obill.ui.components.AppTextField
import com.ribminet.obill.ui.components.AppTopBar
import com.ribminet.obill.ui.components.PrimaryButton
import com.ribminet.obill.ui.theme.BrandBlue
import com.ribminet.obill.ui.theme.CardWhite
import com.ribminet.obill.ui.theme.Divider
import com.ribminet.obill.ui.theme.OnAccent
import com.ribminet.obill.ui.theme.ScreenBackground
import com.ribminet.obill.ui.theme.SuccessGreen
import com.ribminet.obill.ui.theme.TextPrimary
import com.ribminet.obill.ui.theme.TextSecondary

@Composable
fun ChangePasswordScreen(onBack: () -> Unit, onSave: () -> Unit) {
    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    val hasMinLen = new.length >= 8
    val hasCase = new.any { it.isUpperCase() } && new.any { it.isLowerCase() }
    val hasDigit = new.any { it.isDigit() }
    val hasSymbol = new.any { !it.isLetterOrDigit() }
    val allValid = hasMinLen && hasCase && hasDigit && hasSymbol && new == confirm && confirm.isNotEmpty()

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "Keamanan Kata Sandi", onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            AppTextField(current, { current = it }, "Kata Sandi Sekarang", leadingIcon = Icons.Filled.Lock, isPassword = true)
            Spacer(Modifier.height(14.dp))
            AppTextField(new, { new = it }, "Kata Sandi Baru", leadingIcon = Icons.Filled.Lock, isPassword = true)
            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardWhite)
                    .padding(16.dp)
            ) {
                Text("Kriteria Keamanan Kata Sandi Baru:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                Spacer(Modifier.height(10.dp))
                Criteria("Minimal berukuran 8 karakter", hasMinLen)
                Criteria("Kombinasi Huruf Besar [A-Z] & Kecil [a-z]", hasCase)
                Criteria("Memiliki minimal 1 Angka [0-9]", hasDigit)
                Criteria("Memiliki minimal 1 Simbol Khusus (@,#,$,%...)", hasSymbol)
            }

            Spacer(Modifier.height(16.dp))
            AppTextField(confirm, { confirm = it }, "Konfirmasi Kata Sandi Baru", leadingIcon = Icons.Filled.VerifiedUser, isPassword = true)
        }
        Column(modifier = Modifier
            .background(ScreenBackground)
            .padding(16.dp)) {
            PrimaryButton(text = "Simpan Kata Sandi Baru", enabled = allValid, onClick = onSave)
        }
    }
}

@Composable
private fun Criteria(text: String, met: Boolean) {
    Row(
        modifier = Modifier.padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(if (met) SuccessGreen else Divider),
            contentAlignment = Alignment.Center
        ) {
            if (met) Icon(Icons.Filled.Check, contentDescription = null, tint = OnAccent, modifier = Modifier.size(12.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(text, color = if (met) TextPrimary else TextSecondary, fontSize = 13.sp)
    }
}
