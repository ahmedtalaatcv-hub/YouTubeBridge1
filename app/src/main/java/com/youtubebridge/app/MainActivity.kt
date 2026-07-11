package com.youtubebridge.app

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.youtubebridge.app.service.ServerForegroundService
import com.youtubebridge.app.ui.MainScreen
import com.youtubebridge.app.ui.MainViewModel
import com.youtubebridge.app.ui.theme.YouTubeBridgeTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var bound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as ServerForegroundService.LocalBinder
            viewModel.onServiceConnected(binder.getService())
            bound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            viewModel.onServiceDisconnected()
            bound = false
        }
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestRuntimePermissionsIfNeeded()
        requestIgnoreBatteryOptimizations()

        // Auto-start the server as soon as the app opens (foreground service keeps
        // it alive even if the user leaves the app afterwards).
        ServerForegroundService.start(this)

        setContent {
            val state by viewModel.uiState.collectAsState()

            YouTubeBridgeTheme(darkTheme = state.darkMode) {
                MainScreen(
                    state = state,
                    onStart = { viewModel.startServer() },
                    onStop = { viewModel.stopServer() },
                    onRestart = { viewModel.restartServer() },
                    onPortChange = { newPort ->
                        viewModel.setPort(newPort)
                        viewModel.restartServer()
                    },
                    onAutoStartChange = { viewModel.setAutoStart(it) },
                    onDarkModeChange = { viewModel.setDarkMode(it) },
                    onClearLogs = { viewModel.clearLogs() },
                    onUpdateExtractor = {
                        // Extraction runs via a bundled library (NewPipeExtractor), not an
                        // external process, so "updating" it means shipping a new APK build
                        // with a newer library version. We simply surface that fact to the user.
                    }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Intent(this, ServerForegroundService::class.java).also {
            bindService(it, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        if (bound) {
            unbindService(connection)
            bound = false
        }
    }

    private fun requestRuntimePermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun requestIgnoreBatteryOptimizations() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } catch (e: Exception) {
                // Some OEMs restrict this intent; ignore silently, the user can grant it manually.
            }
        }
    }
}
