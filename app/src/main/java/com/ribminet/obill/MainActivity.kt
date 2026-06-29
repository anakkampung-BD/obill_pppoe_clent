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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ribminet.obill.ui.components.UpdateDialog
import com.ribminet.obill.ui.navigation.AppNavGraph
import com.ribminet.obill.ui.theme.RibmiNetTheme
import com.ribminet.obill.ui.theme.ScreenBackground

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { App() }
    }
}

@Composable
fun App() {
    RibmiNetTheme {
        val vm: AppViewModel = viewModel()
        val context = LocalContext.current

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
    }
}
