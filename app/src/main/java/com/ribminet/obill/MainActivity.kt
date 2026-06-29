package com.ribminet.obill

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ribminet.obill.data.local.OnboardingPrefs
import com.ribminet.obill.ui.components.SweetAlertDialog
import com.ribminet.obill.ui.components.UpdateDialog
import com.ribminet.obill.ui.navigation.AppNavGraph
import com.ribminet.obill.ui.screens.PermissionOnboardingScreen
import com.ribminet.obill.ui.screens.SplashScreen
import com.ribminet.obill.ui.theme.RibmiNetTheme
import com.ribminet.obill.ui.theme.ScreenBackground
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { App() }
    }
}

private enum class AppStage { SPLASH, PERMISSIONS, READY }

@Composable
fun App() {
    RibmiNetTheme {
        val vm: AppViewModel = viewModel()
        val context = LocalContext.current
        val onboarding = remember { OnboardingPrefs(context) }

        var stage by remember { mutableStateOf(AppStage.SPLASH) }

        LaunchedEffect(Unit) {
            delay(1800)
            stage = if (onboarding.permissionsRequested) AppStage.READY else AppStage.PERMISSIONS
        }

        when (stage) {
            AppStage.SPLASH -> {
                Surface(modifier = Modifier.fillMaxSize(), color = ScreenBackground) {
                    SplashScreen()
                }
                return@RibmiNetTheme
            }
            AppStage.PERMISSIONS -> {
                Surface(modifier = Modifier.fillMaxSize(), color = ScreenBackground) {
                    PermissionOnboardingScreen(onDone = {
                        onboarding.permissionsRequested = true
                        stage = AppStage.READY
                    })
                }
                return@RibmiNetTheme
            }
            AppStage.READY -> Unit
        }

        LaunchedEffect(Unit) { vm.checkForUpdate() }

        Surface(modifier = Modifier.fillMaxSize(), color = ScreenBackground) {
            AppNavGraph(vm)
        }

        UpdateDialog(
            info = vm.updateInfo,
            downloading = vm.updateDownloading,
            progress = vm.updateProgress,
            onUpdate = {
                vm.downloadAndInstallUpdate { fallbackUrl ->
                    if (fallbackUrl.isNotBlank()) {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)))
                        }
                    }
                }
            },
            onDismiss = { vm.dismissUpdate() },
        )

        // Notifikasi internal masa aktif (sekali per sesi). Tunda bila dialog update aktif.
        if (vm.updateInfo == null) {
            SweetAlertDialog(alert = vm.billingReminder) { vm.dismissBillingReminder() }
        }
    }
}
