package com.example.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.app.screen.auth.LoginScreen
import com.example.app.screen.home.HomeScreen
import com.example.app.screen.rutas.AlternateRoutesScreen
import com.example.app.screen.rutas.components.MapScreen
import com.example.app.ui.theme.AppTheme
import com.example.app.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val authViewModel: AuthViewModel = ViewModelProvider(
            this,
            AuthViewModel.AuthViewModelFactory(this)
        )[AuthViewModel::class.java]

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
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "login") {
                        composable("login") {
                            LoginScreen(navController = navController, authViewModel = authViewModel)
                        }
                        composable("home") {
                            HomeScreen(authViewModel = authViewModel, navController = navController)
                        }
                        composable("rutas") {
                            val token = authViewModel.accessToken ?: ""
                            val isDarkTheme = isSystemInDarkTheme()
                            val primaryColor = if (isDarkTheme) Color(0xFF64B5F6) else Color(0xFF1976D2)
                            val textColor = if (isDarkTheme) Color.White else Color.Black

                            AlternateRoutesScreen(
                                navController = navController,
                                token = token,
                                isDarkTheme = isDarkTheme,
                                primaryColor = primaryColor,
                                textColor = textColor
                            )
                        }
                        composable("mapa") {
                            MapScreen(navController = navController)
                        }
                    }
                }
            }
        }
    }
}