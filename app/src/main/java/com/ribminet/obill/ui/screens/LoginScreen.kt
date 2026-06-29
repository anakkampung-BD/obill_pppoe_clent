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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.ribminet.obill.AppViewModel
import com.ribminet.obill.OtpStep
import com.ribminet.obill.ui.components.AppTextField
import com.ribminet.obill.ui.components.PrimaryButton
import com.ribminet.obill.ui.components.SweetAlertDialog
import com.ribminet.obill.ui.components.clickableNoRipple
import com.ribminet.obill.ui.theme.BrandBlue
import com.ribminet.obill.ui.theme.CardWhite
import com.ribminet.obill.ui.theme.DangerRed
import com.ribminet.obill.ui.theme.Divider
import com.ribminet.obill.ui.theme.HeroGreenBottom
import com.ribminet.obill.ui.theme.OnAccent
import com.ribminet.obill.ui.theme.TextPrimary
import com.ribminet.obill.ui.theme.TextSecondary

private val LoginGradient = Brush.verticalGradient(
    0.0f to Color(0xFFE9FBF2),
    0.45f to Color(0xFFBDEFD6),
    1.0f to Color(0xFF8FE0BC),
)

@Composable
fun LoginScreen(vm: AppViewModel, onLoggedIn: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LoginGradient)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 72.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .shadow(12.dp, CircleShape)
                    .clip(CircleShape)
                    .background(OnAccent),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Wifi, contentDescription = null, tint = HeroGreenBottom, modifier = Modifier.size(50.dp))
            }
            Spacer(Modifier.height(18.dp))
            Text("Obill", color = Color(0xFF14693F), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(4.dp))
            Text("Kelola layanan internet Anda", color = Color(0xFF2E8A5E), fontSize = 13.sp)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(18.dp, RoundedCornerShape(24.dp), clip = false)
                .clip(RoundedCornerShape(24.dp))
                .background(CardWhite)
                .border(1.dp, OnAccent.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            if (vm.otpStep == OtpStep.PHONE) {
                PhoneStep(vm)
            } else {
                OtpStep(vm, onLoggedIn)
            }
        }

        Spacer(Modifier.height(24.dp))
        val appVersion = com.ribminet.obill.ObillApp.instance.appVersionName()
        val year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        Text(
            "Obill • v.$appVersion • Powered By AKS",
            color = Color(0xFF2E8A5E),
            fontSize = 10.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "© $year • All Rights Reserved",
            color = Color(0xFF2E8A5E),
            fontSize = 10.sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )
    }

    SweetAlertDialog(alert = vm.alert) { vm.dismissAlert() }
}

@Composable
private fun PhoneStep(vm: AppViewModel) {
    Column {
        Text("Masuk Akun", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.height(20.dp))

        AppTextField(
            value = vm.phone,
            onValueChange = { vm.phone = it },
            placeholder = "08xxxxxxxxxx",
            label = "Nomor WhatsApp",
            leadingIcon = Icons.Filled.ChatBubble,
            keyboardType = KeyboardType.Phone,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Pastikan WhatsApp aktif untuk dapat menerima kode OTP.",
            color = DangerRed,
            fontSize = 10.sp,
        )
        Spacer(Modifier.height(20.dp))
        PrimaryButton(
            text = if (vm.authLoading) "Mengirim..." else "Kirim Kode OTP",
            enabled = !vm.authLoading,
            onClick = { vm.requestOtp() }
        )
        if (vm.authLoading) {
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandBlue, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun OtpStep(vm: AppViewModel, onLoggedIn: () -> Unit) {
    val otpLength = vm.otpLength
    var otp by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { otp = "" }
    Column {
        Text("Verifikasi OTP", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.height(4.dp))
        Text("Masukkan $otpLength digit kode yang dikirim ke WhatsApp ${vm.phone}.", color = TextSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(24.dp))

        OtpInput(
            value = otp,
            length = otpLength,
            onValueChange = { otp = it },
            onFilled = { if (!vm.authLoading) vm.verifyOtp(otp, onLoggedIn) },
        )
        Spacer(Modifier.height(20.dp))
        PrimaryButton(
            text = if (vm.authLoading) "Memverifikasi..." else "Verifikasi & Masuk",
            enabled = !vm.authLoading && otp.length == otpLength,
            onClick = { vm.verifyOtp(otp, onLoggedIn) }
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Ganti Nomor",
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier.clickableNoRipple { vm.resetAuth() }
            )
            Text(
                "Kirim Ulang OTP",
                color = BrandBlue,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier.clickableNoRipple { vm.requestOtp() }
            )
        }
    }
}

@Composable
private fun OtpInput(
    value: String,
    length: Int,
    onValueChange: (String) -> Unit,
    onFilled: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    BasicTextField(
        value = value,
        onValueChange = {
            val filtered = it.filter { c -> c.isDigit() }.take(length)
            onValueChange(filtered)
            if (filtered.length == length) onFilled()
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        cursorBrush = SolidColor(BrandBlue),
        modifier = Modifier.focusRequester(focusRequester),
        decorationBox = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(length) { index ->
                    val char = value.getOrNull(index)?.toString() ?: ""
                    val isActive = index == value.length
                    val hasValue = char.isNotEmpty()
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CardWhite)
                            .border(
                                width = if (isActive || hasValue) 1.5.dp else 1.dp,
                                color = if (isActive || hasValue) BrandBlue else Divider,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary,
                        )
                    }
                }
            }
        }
    )
}
