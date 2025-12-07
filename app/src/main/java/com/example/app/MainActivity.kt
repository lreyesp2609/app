package com.example.app

import android.os.Bundle
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.app.ui.theme.AppTheme
import com.example.app.viewmodel.AuthViewModel
import com.example.app.viewmodel.MapViewModel
import com.example.app.utils.NotificationHelper
import com.example.app.viewmodel.MapViewModelFactory
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
                    // 🔥 CREAR MapViewModel AQUÍ (dentro de setContent)
                    val mapViewModel: MapViewModel = viewModel(
                        factory = MapViewModelFactory()
                    )

                    AppNavigation(
                        authViewModel = authViewModel,
                        mapViewModel = mapViewModel
                    )
                }
            }
        }
    }
}
