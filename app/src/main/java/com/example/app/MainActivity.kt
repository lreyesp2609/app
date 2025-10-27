package com.example.app

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.example.app.screen.config.SettingsScreen
import com.example.app.screen.home.HomeScreen
import com.example.app.screen.home.components.PlaceholderScreen
import com.example.app.screen.mapa.RutaMapa
import com.example.app.screen.rutas.AlternateRoutesScreen
import com.example.app.screen.rutas.components.EstadisticasScreen
import com.example.app.screen.rutas.components.MapScreen
import com.example.app.ui.theme.AppTheme
import com.example.app.viewmodel.AuthViewModel
import com.example.app.viewmodel.MapViewModelFactory
import com.example.app.viewmodel.UbicacionesViewModel
import com.example.app.viewmodel.UbicacionesViewModelFactory
import com.example.app.viewmodel.MapViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.app.screen.auth.RegisterScreen
import com.example.app.screen.grupos.CollaborativeGroupsScreen
import com.example.app.screen.grupos.components.ChatGrupoScreen
import com.example.app.screen.grupos.components.CreateGroupScreen
import com.example.app.screen.recordatorios.components.AddReminderScreen
import com.example.app.screen.recordatorios.components.ReminderMapScreen
import com.example.app.services.LocationReminderService
import com.example.app.services.LocationService
import com.example.app.services.LocationTrackingService
import com.example.app.utils.NotificationHelper
import com.example.app.websocket.NotificationWebSocketManager
import com.example.app.websocket.WebSocketLocationManager
import com.example.app.websocket.WebSocketManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NotificationHelper.createNotificationChannel(this)

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

@Composable
fun AppNavigation(authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current

    val isLoggedIn = authViewModel.isLoggedIn
    val isLoading = authViewModel.isLoading
    val accessToken = authViewModel.accessToken

    // Conectar WebSocket de Notificaciones
    LaunchedEffect(isLoggedIn, accessToken) {
        if (isLoggedIn && accessToken != null) {
            val baseUrl = BuildConfig.BASE_URL.removeSuffix("/")
            Log.d("AppNavigation", "🚀 CONECTANDO WEBSOCKET DE NOTIFICACIONES")
            NotificationWebSocketManager.connect(baseUrl, accessToken)
            Log.d("AppNavigation", "ℹ️ Chat y Ubicaciones se conectarán al entrar a un grupo")
        } else {
            Log.d("AppNavigation", "🔒 Usuario no logueado, cerrando WebSockets...")
            NotificationWebSocketManager.close()
            WebSocketManager.close()

            // Solo cerrar ubicaciones si el servicio NO está activo
            if (!LocationTrackingService.isTracking(context)) {
                WebSocketLocationManager.close()
            }
        }
    }

    // 🔥 LIMPIAR WEBSOCKETS al salir (PERO NO el de ubicaciones si el servicio está activo)
    DisposableEffect(Unit) {
        onDispose {
            Log.d("AppNavigation", "🔒 ════════════════════════════════════════")
            Log.d("AppNavigation", "🔒 APP CERRADA, LIMPIANDO WEBSOCKETS")
            Log.d("AppNavigation", "🔒 ════════════════════════════════════════")

            // Cerrar notificaciones y chat
            NotificationWebSocketManager.close()
            WebSocketManager.close()

            // Solo cerrar ubicaciones si el servicio NO está activo
            if (LocationTrackingService.isTracking(context)) {
                Log.d("AppNavigation", "ℹ️ Servicio de rastreo activo, manteniendo WebSocket de ubicaciones")
            } else {
                Log.d("AppNavigation", "✅ Cerrando WebSocket de ubicaciones (servicio inactivo)")
                WebSocketLocationManager.close()
            }
        }
    }

    // Determinar la ruta inicial basada en el estado de autenticación
    val startDestination = if (isLoggedIn) "home" else "login"

    // Mostrar pantalla de carga mientras se restaura la sesión
    if (isLoading && !isLoggedIn && authViewModel.accessToken == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(50.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Cargando...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        return
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(navController = navController, authViewModel = authViewModel)
        }
        composable("register") {
            RegisterScreen(navController = navController, authViewModel = authViewModel)
        }

        composable("home") {
            HomeScreen(authViewModel = authViewModel, navController = navController)
        }
        composable("rutas") {
            val token = authViewModel.accessToken ?: ""
            AlternateRoutesScreen(
                navController = navController,
                token = token
            )
        }
        composable("mapa") {
            MapScreen(navController = navController)
        }

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
                mapViewModel.setToken(token)
            }

            val ubicacion = viewModel.ubicacionSeleccionada
            val selectedLocationId = ubicacion?.id ?: 0

            RutaMapa(
                defaultLat = 0.0,
                defaultLon = 0.0,
                ubicaciones = if (ubicacion != null) listOf(ubicacion) else emptyList(),
                viewModel = mapViewModel,
                token = token,
                selectedLocationId = selectedLocationId
            )
        }

        composable(
            "estadisticas/{id}",
            arguments = listOf(navArgument("id") { type = NavType.IntType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("id") ?: 0
            val token = authViewModel.accessToken ?: ""

            EstadisticasScreen(
                ubicacionId = id,
                token = token,
                navController = navController
            )
        }

        composable("grupos") {
            val token = authViewModel.accessToken ?: ""
            CollaborativeGroupsScreen(
                navController = navController,
                token = token
            )
        }

        composable("settings") {
            SettingsScreen(
                userState = authViewModel.user,
                onLogout = {
                    Log.d("AppNavigation", "🔒 LOGOUT: CERRANDO TODOS LOS WEBSOCKETS")

                    // ✅ Cerrar servicio de rastreo primero
                    if (LocationTrackingService.isTracking(context)) {
                        LocationTrackingService.stopTracking(context)
                        Log.d("AppNavigation", "🛑 Servicio de rastreo detenido")
                    }

                    // Ahora sí cerrar todos los WebSockets
                    NotificationWebSocketManager.close()
                    WebSocketManager.close()
                    WebSocketLocationManager.close()

                    authViewModel.logout(context) {
                        navController.navigate("login") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                }
            )
        }

        composable("reminder_map") {
            ReminderMapScreen(
                navController = navController,
                onLocationSelected = { lat, lon, address ->
                    navController.navigate(
                        "add_reminder/${Uri.encode(address)}/$lat/$lon"
                    )
                }
            )
        }

        composable(
            route = "add_reminder/{selectedAddress}/{latitude}/{longitude}",
            arguments = listOf(
                navArgument("selectedAddress") { type = NavType.StringType },
                navArgument("latitude") { type = NavType.StringType },
                navArgument("longitude") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val selectedAddress = backStackEntry.arguments?.getString("selectedAddress") ?: ""
            val latitude = backStackEntry.arguments?.getString("latitude")?.toDoubleOrNull()
            val longitude = backStackEntry.arguments?.getString("longitude")?.toDoubleOrNull()

            AddReminderScreen(
                navController = navController,
                selectedAddress = selectedAddress,
                latitude = latitude,
                longitude = longitude
            )
        }

        composable(
            route = "home?tab={tab}",
            arguments = listOf(
                navArgument("tab") {
                    type = NavType.IntType
                    defaultValue = 0
                }
            )
        ) { backStackEntry ->
            val initialTab = backStackEntry.arguments?.getInt("tab") ?: 0
            HomeScreen(
                authViewModel = authViewModel,
                navController = navController,
                initialTab = initialTab
            )
        }
        composable("create_group") {
            val token = authViewModel.accessToken ?: ""
            CreateGroupScreen(
                token = token,
                navController = navController
            )
        }
        composable(
            route = "chat_grupo/{grupoId}/{grupoNombre}",
            arguments = listOf(
                navArgument("grupoId") { type = NavType.IntType },
                navArgument("grupoNombre") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val grupoId = backStackEntry.arguments?.getInt("grupoId") ?: 0
            val grupoNombre = backStackEntry.arguments?.getString("grupoNombre") ?: ""

            LaunchedEffect(grupoId) {
                if (!isServiceRunning(context, LocationService::class.java)) {
                    val intent = Intent(context, LocationService::class.java).apply {
                        action = LocationService.ACTION_START
                        putExtra(LocationService.EXTRA_GRUPO_ID, grupoId)
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }

                    Log.d("ChatGrupo", "🚀 Servicio de ubicación iniciado para grupo $grupoId")
                } else {
                    Log.d("ChatGrupo", "ℹ️ Servicio ya está corriendo")
                }
            }

            ChatGrupoScreen(
                grupoId = grupoId,
                grupoNombre = grupoNombre,
                navController = navController,
            )
        }
    }
}

private fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    @Suppress("DEPRECATION")
    return manager.getRunningServices(Integer.MAX_VALUE)
        .any { it.service.className == serviceClass.name }
}