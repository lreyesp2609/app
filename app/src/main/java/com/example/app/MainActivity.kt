package com.example.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
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
import android.provider.Settings
import android.net.Uri

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NotificationHelper.createNotificationChannel(this)
        testWebSocketPing()

        val authViewModel: AuthViewModel = ViewModelProvider(
            this,
            AuthViewModel.AuthViewModelFactory(this)
        )[AuthViewModel::class.java]

        // 🔥 SOLICITAR EXCLUSIÓN DE OPTIMIZACIÓN DE BATERÍA
        // Esto debe hacerse ANTES de iniciar el servicio
        // requestBatteryOptimizationExemption()

        // 🔹 SOLO iniciar el servicio SI los permisos están concedidos
        // if (hasLocationPermissions()) {
           // LocationReminderService.start(this)
            // Log.d("MainActivity", "✅ Servicio de ubicación iniciado")
        // } else {
           // Log.w("MainActivity", "⚠️ Permisos de ubicación no concedidos, servicio no iniciado")
        // }

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
}