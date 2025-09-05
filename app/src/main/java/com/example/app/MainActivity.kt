package com.example.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.app.repository.RutasRepository
import com.example.app.screen.auth.LoginScreen
import com.example.app.screen.home.HomeScreen
import com.example.app.screen.mapa.RutaMapa
import com.example.app.screen.rutas.AlternateRoutesScreen
import com.example.app.screen.rutas.components.MapScreen
import com.example.app.ui.theme.AppTheme
import com.example.app.viewmodel.AuthViewModel
import com.example.app.viewmodel.MapViewModelFactory
import com.example.app.viewmodel.UbicacionesViewModel
import com.example.app.viewmodel.UbicacionesViewModelFactory
import com.example.app.viewmodel.MapViewModel

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

                        // 🔹 Rutas por ID en vez de lat/lon
                        composable(
                            "rutas_screen/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val id = backStackEntry.arguments?.getInt("id") ?: 0
                            val token = authViewModel.accessToken ?: ""

                            val mapViewModel: MapViewModel = viewModel(
                                factory = MapViewModelFactory(RutasRepository())
                            )

                            val viewModel: UbicacionesViewModel = viewModel(
                                factory = UbicacionesViewModelFactory(token)
                            )

                            LaunchedEffect(id) {
                                viewModel.cargarUbicacionPorId(id)
                                // Setear el token en el MapViewModel
                                mapViewModel.setToken(token)
                            }

                            val ubicacion = viewModel.ubicacionSeleccionada
                            val selectedLocationId = ubicacion?.id ?: 0

                            RutaMapa(
                                defaultLat = 0.0,
                                defaultLon = 0.0,
                                ubicaciones = if (ubicacion != null) listOf(ubicacion) else emptyList(),
                                viewModel = mapViewModel,
                                token = token,  // Pasar token
                                selectedLocationId = selectedLocationId
                            )
                        }
                    }
                }
            }
        }
    }
}