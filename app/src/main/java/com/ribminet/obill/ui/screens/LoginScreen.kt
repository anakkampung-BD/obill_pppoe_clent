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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ribminet.obill.AppViewModel
import com.ribminet.obill.OtpStep
import com.ribminet.obill.ui.components.PrimaryButton
import com.ribminet.obill.ui.components.SweetAlertDialog
import com.ribminet.obill.ui.components.clickableNoRipple
import com.ribminet.obill.ui.theme.BrandBlue
import com.ribminet.obill.ui.theme.CardWhite
import com.ribminet.obill.ui.theme.TextPrimary
import com.ribminet.obill.ui.theme.TextSecondary

/** Pola UI mengikuti Nex-T: kartu putih, field abu muda; latar dari AppBackground. */
private val FieldFill = Color(0xFFF4F7FA)
private val TitleNavy = Color(0xFF1F2A37)
private val FooterGray = Color(0xFF9AA6B2)
private val CardRadius = 22.dp

@Composable
fun LoginScreen(vm: AppViewModel, onLoggedIn: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(48.dp))
        BrandHeader()
        Spacer(Modifier.height(28.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(10.dp, RoundedCornerShape(CardRadius), clip = false)
                .clip(RoundedCornerShape(CardRadius))
                .background(CardWhite)
                .padding(horizontal = 22.dp, vertical = 26.dp),
        ) {
            if (vm.otpStep == OtpStep.PHONE) {
                PhoneStep(vm)
            } else {
                OtpStepContent(vm, onLoggedIn)
            }
        }

        Spacer(Modifier.height(40.dp))
        val appVersion = com.ribminet.obill.ObillApp.instance.appVersionName()
        Text(
            "Obill · Powered by AKS · v.$appVersion",
            color = FooterGray,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 28.dp),
        )
    }

    SweetAlertDialog(alert = vm.alert) { vm.dismissAlert() }
}

@Composable
private fun BrandHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(108.dp)
                .shadow(8.dp, CircleShape, clip = false)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(BrandBlue.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Wifi,
                    contentDescription = null,
                    tint = BrandBlue,
                    modifier = Modifier.size(40.dp),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Obill", color = TitleNavy, fontWeight = FontWeight.Bold, fontSize = 28.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            "Kelola layanan internet Anda",
            color = TextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PhoneStep(vm: AppViewModel) {
    Column {
        Text("Selamat Datang", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TitleNavy)
        Spacer(Modifier.height(6.dp))
        Text(
            "Masuk dengan nomor WhatsApp terdaftar",
            color = TextSecondary,
            fontSize = 13.sp,
        )
        Spacer(Modifier.height(22.dp))

        LoginField(
            value = vm.phone,
            onValueChange = { vm.phone = it },
            label = "Nomor WhatsApp",
            placeholder = "08xxxxxxxxxx",
            keyboardType = KeyboardType.Phone,
            leading = {
                Icon(
                    Icons.Filled.ChatBubbleOutline,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp),
                )
            },
        )
        Spacer(Modifier.height(22.dp))
        PrimaryButton(
            text = if (vm.authLoading) "Mengirim..." else "Kirim OTP",
            enabled = !vm.authLoading,
            onClick = { vm.requestOtp() },
        )
        if (vm.authLoading) {
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandBlue, modifier = Modifier.size(22.dp))
            }
        } else {
            Spacer(Modifier.height(16.dp))
            Text(
                "Sudah terima kode OTP? Masukkan di sini",
                color = BrandBlue,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickableNoRipple { vm.continueToOtpEntry() }
                    .padding(vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun OtpStepContent(vm: AppViewModel, onLoggedIn: () -> Unit) {
    val otpLength = vm.otpLength
    var otp by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { otp = "" }

    Column {
        Text("Verifikasi OTP", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TitleNavy)
        Spacer(Modifier.height(6.dp))
        Text(vm.phone, color = TextSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(22.dp))

        OtpInput(
            value = otp,
            length = otpLength,
            onValueChange = { otp = it },
            onFilled = { if (!vm.authLoading) vm.verifyOtp(otp, onLoggedIn) },
        )
        Spacer(Modifier.height(22.dp))
        PrimaryButton(
            text = if (vm.authLoading) "Memverifikasi..." else "Masuk",
            enabled = !vm.authLoading && otp.length == otpLength,
            onClick = { vm.verifyOtp(otp, onLoggedIn) },
        )
        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "Ganti Nomor",
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier.clickableNoRipple { vm.resetAuth() },
            )
            Text(
                "Kirim Ulang",
                color = BrandBlue,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier.clickableNoRipple { vm.requestOtp() },
            )
        }
    }
}

@Composable
private fun LoginField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    keyboardType: KeyboardType,
    leading: @Composable (() -> Unit)? = null,
) {
    Column {
        Text(label, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(FieldFill)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                leading()
                Spacer(Modifier.size(10.dp))
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp),
                cursorBrush = SolidColor(BrandBlue),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(placeholder, color = FooterGray, fontSize = 15.sp)
                    }
                    inner()
                },
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
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                            .background(FieldFill)
                            .border(
                                width = if (isActive || hasValue) 1.5.dp else 0.dp,
                                color = if (isActive || hasValue) BrandBlue else Color.Transparent,
                                shape = RoundedCornerShape(12.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = char,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary,
                        )
                    }
                }
            }
        },
    )
}
