package com.example.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.example.app.ui.theme.AppTheme
import com.example.app.viewmodel.AuthViewModel
import androidx.core.content.ContextCompat
import com.example.app.services.LocationReminderService
import com.example.app.utils.NotificationHelper
import com.example.app.websocket.testWebSocketPing

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NotificationHelper.createNotificationChannel(this)
        testWebSocketPing()

        val authViewModel: AuthViewModel = ViewModelProvider(
            this,
            AuthViewModel.AuthViewModelFactory(this)
        )[AuthViewModel::class.java]

        // 🔹 SOLO iniciar el servicio SI los permisos están concedidos
        if (hasLocationPermissions()) {
            LocationReminderService.start(this)
        } else {
            Log.w("MainActivity", "⚠️ Permisos de ubicación no concedidos, servicio no iniciado")
        }

        // Configurar barras del sistema para toda la app
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.navigationBarColor = Color.Black.toArgb()
        window.statusBarColor = Color.Black.toArgb()
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(authViewModel = authViewModel)
                }
            }
        }
    }

    private fun hasLocationPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }
}