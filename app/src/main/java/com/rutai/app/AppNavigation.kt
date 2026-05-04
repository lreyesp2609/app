package com.rutai.app

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.rutai.app.network.AppDatabase
import com.rutai.app.repository.ReminderRepository
import com.rutai.app.screen.SplashScreen
import com.rutai.app.screen.auth.LoginScreen
import com.rutai.app.screen.auth.RegisterScreen
import com.rutai.app.screen.config.SettingsScreen
import com.rutai.app.screen.grupos.CollaborativeGroupsScreen
import com.rutai.app.screen.grupos.components.ChatGrupoScreen
import com.rutai.app.screen.grupos.components.CreateGroupScreen
import com.rutai.app.screen.grupos.components.GrupoDetalleScreen
import com.rutai.app.screen.grupos.components.ParticipantesScreen
import com.rutai.app.screen.home.HomeScreen
import com.rutai.app.screen.recordatorios.components.EditReminderScreen
import com.rutai.app.screen.recordatorios.components.ReminderMapScreen
import com.rutai.app.screen.rutas.AlternateRoutesScreen
import com.rutai.app.screen.rutas.components.EstadisticasScreen
import com.rutai.app.screen.rutas.components.MapScreen
import com.rutai.app.screen.rutas.components.MisZonasPeligrosasScreen
import com.rutai.app.screen.rutas.components.RutaMapa
import com.rutai.app.services.LocationTrackingService
import com.rutai.app.utils.GlobalNotification
import com.rutai.app.viewmodel.AuthViewModel
import com.rutai.app.viewmodel.MapViewModel
import com.rutai.app.viewmodel.MapViewModelFactory
import com.rutai.app.viewmodel.NotificationViewModel
import com.rutai.app.viewmodel.ReminderViewModel
import com.rutai.app.viewmodel.ReminderViewModelFactory
import com.rutai.app.viewmodel.UbicacionesViewModel
import com.rutai.app.viewmodel.UbicacionesViewModelFactory
import com.rutai.app.websocket.NotificationWebSocketManager
import com.rutai.app.websocket.WebSocketLocationManager
import com.rutai.app.websocket.WebSocketManager

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    mapViewModel: MapViewModel,
    navController: NavHostController
    ) {
    val context = LocalContext.current
    val notificationViewModel: NotificationViewModel = viewModel()
    val database = AppDatabase.getDatabase(context)
    val repository = ReminderRepository(database.reminderDao())
    val reminderViewModel: ReminderViewModel = viewModel(
        factory = ReminderViewModelFactory(repository)
    )
    val isLoggedIn = authViewModel.isLoggedIn
    val isLoading = authViewModel.isLoading
    val accessToken = authViewModel.accessToken

    // 🆕 INICIALIZAR NotificationWebSocketManager UNA SOLA VEZ (antes que WebSocketLocationManager)
    LaunchedEffect(Unit) {
        Log.d("AppNavigation", "🏗️ Inicializando NotificationWebSocketManager")
        NotificationWebSocketManager.initialize(context)

        Log.d("AppNavigation", "🏗️ Inicializando WebSocketLocationManager")
        WebSocketLocationManager.initialize(context)
    }

    // Conectar WebSocket de Notificaciones
    val isRestoringSession = authViewModel.isRestoringSession
    LaunchedEffect(isLoggedIn, accessToken, isRestoringSession) {

        if (isRestoringSession) {
            Log.d("AppNavigation", "⏳ Restaurando sesión, no se tocan WebSockets todavía")
            return@LaunchedEffect
        }

        if (isLoggedIn && accessToken != null) {
            val baseUrl = BuildConfig.BASE_URL.removeSuffix("/")

            Log.d("AppNavigation", "🚀 USUARIO LOGUEADO - CONECTANDO WEBSOCKETS")

            // 🆕 Simplemente conectar - el listener ya está registrado
            NotificationWebSocketManager.connect(baseUrl, accessToken)

            Log.d("AppNavigation", "ℹ️ WebSocket de ubicaciones se conectará desde LocationService")
            Log.d("AppNavigation", "✅ WebSockets de notificaciones conectado")
        } else {
            val tokenGuardado = context
                .getSharedPreferences("recuerdago_prefs", Context.MODE_PRIVATE)
                .getString("ACCESS_TOKEN", null)

            if (tokenGuardado == null) {
                Log.d("AppNavigation", "🔒 LOGOUT REAL - CERRANDO WEBSOCKETS")

                NotificationWebSocketManager.close()
                WebSocketManager.close()

                // Solo cerrar ubicaciones si el servicio NO está activo
                if (!LocationTrackingService.isTracking(context)) {
                    Log.d("AppNavigation", "✅ Cerrando WebSocket de ubicaciones")
                    WebSocketLocationManager.close()
                } else {
                    Log.d("AppNavigation", "ℹ️ Manteniendo WebSocket (servicio activo)")
                }
            } else {
                Log.d(
                    "AppNavigation",
                    "⏳ Restart de Activity detectado (token existe), ignorando cierre"
                )
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
                Log.d(
                    "AppNavigation",
                    "ℹ️ Servicio de rastreo activo, manteniendo WebSocket de ubicaciones"
                )
            } else {
                Log.d("AppNavigation", "✅ Cerrando WebSocket de ubicaciones (servicio inactivo)")
                WebSocketLocationManager.close()
            }
        }
    }

    // Determinar la ruta inicial basada en el estado de autenticación
    val startDestination = "splash"

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
                    text = stringResource(R.string.loading_dots),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(navController = navController, startDestination = startDestination) {
                composable("splash") {
                    SplashScreen(
                        onTimeout = {
                            // Después de 2 segundos, decide a dónde ir
                            if (isLoggedIn) {
                                navController.navigate("home") {
                                    popUpTo("splash") { inclusive = true }
                                }
                            } else {
                                navController.navigate("login") {
                                    popUpTo("splash") { inclusive = true }
                                }
                            }
                        }
                    )
                }

                composable("login") {
                    LoginScreen(
                        navController = navController,
                        authViewModel = authViewModel,
                        notificationViewModel = notificationViewModel
                    )
                }
                composable("register") {
                    RegisterScreen(
                        navController = navController,
                        authViewModel = authViewModel,
                        notificationViewModel = notificationViewModel
                    )
                }

                composable(route = "home") { backStackEntry ->
                    HomeScreen(
                        authViewModel = authViewModel,
                        navController = navController,
                        notificationViewModel = notificationViewModel
                    )
                }

                composable("rutas") {
                    val token = authViewModel.accessToken ?: ""
                    AlternateRoutesScreen(
                        navController = navController,
                        token = token,
                        notificationViewModel = notificationViewModel
                    )
                }
                composable("mapa") {
                    MapScreen(
                        navController = navController,
                        notificationViewModel = notificationViewModel
                    )
                }

                composable(
                    "rutas_screen/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.IntType })
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getInt("id") ?: 0
                    val token = authViewModel.accessToken ?: ""

                    val mapViewModel: MapViewModel = viewModel(
                        factory = MapViewModelFactory(LocalContext.current)
                    )

                    val viewModel: UbicacionesViewModel = viewModel(
                        factory = UbicacionesViewModelFactory(LocalContext.current, token)
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
                        selectedLocationId = selectedLocationId,
                        navController = navController
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
                        token = token,
                        notificationViewModel = notificationViewModel
                    )
                }

                composable("settings") {
                    SettingsScreen(
                        userState = authViewModel.user,
                        onLogout = {
                            Log.d("AppNavigation", "🔒 LOGOUT: CERRANDO TODOS LOS WEBSOCKETS")

                            // ✅ Cerrar NUEVO servicio de rastreo
                            if (LocationTrackingService.isTracking(context)) {
                                LocationTrackingService.stopAllTracking(context)
                                Log.d("AppNavigation", "🛑 Todos los servicios de rastreo detenidos")
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
                        },
                        onProfileUpdated = { nuevoNombre, nuevoApellido ->
                            authViewModel.actualizarPerfil(nuevoNombre, nuevoApellido)
                        }
                    )
                }

                composable("reminder_map") {
                    ReminderMapScreen(
                        navController = navController,
                        notificationViewModel = notificationViewModel,
                        onLocationSelected = { lat, lon, address ->
                            navController.navigate(
                                "add_reminder/${Uri.encode(address)}/$lat/$lon"
                            )
                        }
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
                        initialTab = initialTab,
                        notificationViewModel = notificationViewModel
                    )
                }

                composable("create_group") {
                    val token = authViewModel.accessToken ?: ""

                    CreateGroupScreen(
                        token = token,
                        navController = navController,
                        notificationViewModel = notificationViewModel // 🔥 Usar la instancia del scope superior
                    )
                }

                composable(
                    route = "chat_grupo/{grupoId}/{grupoNombre}/{codigoInvitacion}", // 🆕 Agregar
                    arguments = listOf(
                        navArgument("grupoId") { type = NavType.IntType },
                        navArgument("grupoNombre") { type = NavType.StringType },
                        navArgument("codigoInvitacion") { type = NavType.StringType } // 🆕
                    )
                ) { backStackEntry ->
                    val grupoId = backStackEntry.arguments?.getInt("grupoId") ?: 0
                    val grupoNombre = backStackEntry.arguments?.getString("grupoNombre") ?: ""
                    val codigoInvitacion =
                        backStackEntry.arguments?.getString("codigoInvitacion") ?: "" // 🆕

                    ChatGrupoScreen(
                        grupoId = grupoId,
                        grupoNombre = grupoNombre,
                        codigoInvitacion = codigoInvitacion, // 🆕
                        navController = navController
                    )
                }

                // En tu NavHost, agrega esta ruta:
                composable(
                    route = "grupo_detalle/{grupoId}/{grupoNombre}/{codigoInvitacion}", // 🆕 Agregar parámetro
                    arguments = listOf(
                        navArgument("grupoId") { type = NavType.IntType },
                        navArgument("grupoNombre") { type = NavType.StringType },
                        navArgument("codigoInvitacion") { type = NavType.StringType } // 🆕
                    )
                ) { backStackEntry ->
                    val grupoId = backStackEntry.arguments?.getInt("grupoId") ?: 0
                    val grupoNombre = backStackEntry.arguments?.getString("grupoNombre") ?: ""
                    val codigoInvitacion =
                        backStackEntry.arguments?.getString("codigoInvitacion") ?: "" // 🆕

                    GrupoDetalleScreen(
                        grupoId = grupoId,
                        grupoNombre = grupoNombre,
                        codigoInvitacion = codigoInvitacion, // 🆕 Pasar el código
                        navController = navController
                    )
                }

                // En tu NavHost, agrega esta ruta:
                composable(
                    route = "participantes/{grupoId}/{grupoNombre}",
                    arguments = listOf(
                        navArgument("grupoId") { type = NavType.IntType },
                        navArgument("grupoNombre") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val grupoId = backStackEntry.arguments?.getInt("grupoId") ?: 0
                    val grupoNombre = backStackEntry.arguments?.getString("grupoNombre") ?: ""

                    ParticipantesScreen(
                        grupoId = grupoId,
                        grupoNombre = grupoNombre,
                        navController = navController
                    )
                }

                composable("zonas_peligrosas") {
                    MisZonasPeligrosasScreen(
                        navController = navController,
                        notificationViewModel = notificationViewModel,
                        mapViewModel = mapViewModel
                    )
                }

                composable(
                    route = "edit_reminder/{reminderId}",
                    arguments = listOf(
                        navArgument("reminderId") { type = NavType.IntType }
                    )
                ) { backStackEntry ->
                    val reminderId =
                        backStackEntry.arguments?.getInt("reminderId") ?: return@composable

                    EditReminderScreen(
                        reminderId = reminderId,
                        navController = navController,
                        viewModel = reminderViewModel,
                        notificationViewModel = notificationViewModel
                    )
                }
            }
            GlobalNotification(notificationViewModel = notificationViewModel)
        }
    }
}