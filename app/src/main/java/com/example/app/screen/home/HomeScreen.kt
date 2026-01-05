package com.example.app.screen.home

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessAlarm
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.app.screen.config.SettingsScreen
import com.example.app.screen.home.components.HomeTabContent
import com.example.app.screen.home.components.PlaceholderTab
import com.example.app.screen.recordatorios.RemindersScreen
import com.example.app.screen.rutas.AlternateRoutesScreen
import com.example.app.ui.theme.getBackgroundGradient
import com.example.app.utils.NotificationHelper
import com.example.app.viewmodel.AuthViewModel
import kotlinx.coroutines.delay
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import com.example.app.screen.grupos.CollaborativeGroupsScreen
import com.example.app.screen.mapa.GetCurrentLocation
import com.example.app.screen.mapa.GpsEnableButton
import com.example.app.services.LocationReminderService
import com.example.app.utils.LocationManager
import com.example.app.viewmodel.NotificationViewModel
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    authViewModel: AuthViewModel,
    navController: NavController,
    initialTab: Int = 0,
    skipPermissions: Boolean = false,
    notificationViewModel: NotificationViewModel
) {
    val context = LocalContext.current
    val userState = authViewModel.user
    val isLoggedIn = authViewModel.isLoggedIn
    val accessToken = authViewModel.accessToken ?: ""
    val locationManager = remember { LocationManager.getInstance() }

    var isVisible by remember { mutableStateOf(false) }
    var showContent by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(
        initialPage = initialTab,
        pageCount = { 5 }
    )
    val scope = rememberCoroutineScope()

    var showPermissionDialog by remember { mutableStateOf(false) }
    var permissionType by remember { mutableStateOf("") }

    var retryLocationCounter by remember { mutableIntStateOf(0) }
    var showGpsButton by remember { mutableStateOf(false) }
    var locationServiceStarted by remember { mutableStateOf(false) }

    var notificationPermissionChecked by remember { mutableStateOf(false) }
    var locationPermissionChecked by remember { mutableStateOf(false) }
    var shouldRequestLocation by remember { mutableStateOf(false) }

    var permissionsReady by remember { mutableStateOf(false) }
    var batteryOptimizationRequested by remember { mutableStateOf(false) }

    // 🔥 NUEVO: Guardar el estado ANTES de abrir el diálogo
    var wasIgnoringBatteryOptimization by remember { mutableStateOf(false) }


    var locationReady by remember { mutableStateOf(false) }

    // 🔥 MODIFICADO: Launcher con verificación de estado
    val batteryOptimizationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // Verificar el estado DESPUÉS de regresar del diálogo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val packageName = context.packageName

            val isNowIgnoring = powerManager.isIgnoringBatteryOptimizations(packageName)

            // 🔥 COMPARAR: ¿Cambió el estado?
            if (!wasIgnoringBatteryOptimization && isNowIgnoring) {
                // ✅ El usuario ACEPTÓ (cambió de false a true)
                Log.d("HomeScreen", "✅ Usuario ACEPTÓ la exclusión de batería")
                Toast.makeText(
                    context,
                    "¡Perfecto! RecuerdaGo funcionará mejor en segundo plano",
                    Toast.LENGTH_SHORT
                ).show()
            } else if (!isNowIgnoring) {
                // ❌ El usuario RECHAZÓ o canceló (sigue en false)
                Log.d("HomeScreen", "⚠️ Usuario RECHAZÓ la exclusión de batería")
                Toast.makeText(
                    context,
                    "Recomendamos activar esta opción para que los recordatorios funcionen mejor",
                    Toast.LENGTH_LONG
                ).show()
            }
            // Si ya estaba ignorando optimización, no mostrar nada
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        notificationPermissionChecked = true

        if (isGranted) {
            Log.d("HomeScreen", "✅ Permiso de notificaciones concedido")
            Toast.makeText(context, "Notificaciones activadas", Toast.LENGTH_SHORT).show()

            scope.launch {
                delay(500)
                shouldRequestLocation = true
            }
        } else {
            Log.w("HomeScreen", "⚠️ Permiso de notificaciones denegado")
            permissionType = "notification"
            showPermissionDialog = true

            scope.launch {
                delay(1000)
                shouldRequestLocation = true
            }
        }
    }

    LaunchedEffect("permissions_delay") {
        if (skipPermissions) {
            Log.d("HomeScreen", "⏭️ Saltando permisos - usuario recién registrado")
            return@LaunchedEffect
        }

        delay(2000)
        permissionsReady = true
    }

    LaunchedEffect(permissionsReady) {
        if (!permissionsReady || skipPermissions) {
            return@LaunchedEffect
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS

            when {
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d("HomeScreen", "✅ Permiso de notificaciones ya concedido")
                    notificationPermissionChecked = true
                    delay(500)
                    shouldRequestLocation = true
                }
                else -> {
                    Log.d("HomeScreen", "🔔 Solicitando permiso de notificaciones")
                    notificationPermissionLauncher.launch(permission)
                }
            }
        } else {
            notificationPermissionChecked = true
            delay(500)
            shouldRequestLocation = true
        }

        NotificationHelper.createNotificationChannel(context)
    }

    // 🔥 MODIFICADO: Guardar estado ANTES y abrir diálogo
    LaunchedEffect(locationServiceStarted) {
        if (locationServiceStarted && !batteryOptimizationRequested) {
            batteryOptimizationRequested = true
            delay(2000)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                val packageName = context.packageName

                // 🔥 GUARDAR ESTADO ANTES
                wasIgnoringBatteryOptimization = powerManager.isIgnoringBatteryOptimizations(packageName)

                if (!wasIgnoringBatteryOptimization) {
                    Log.d("HomeScreen", "⚠️ Solicitando exclusión de batería")

                    try {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:$packageName")
                        }

                        batteryOptimizationLauncher.launch(intent)

                    } catch (e: Exception) {
                        Log.e("HomeScreen", "❌ Error solicitando exclusión: ${e.message}")
                        Toast.makeText(
                            context,
                            "No se pudo solicitar exclusión de batería",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Log.d("HomeScreen", "✅ Ya excluida de optimización de batería")
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        val cachedLocation = locationManager.getLastKnownLocation()
        if (cachedLocation != null) {
            locationReady = true
            Log.d("HomeScreen", "✅ Ubicación en caché disponible al iniciar")
        }
    }

    // 📍 Componente invisible que maneja la ubicación
    if (shouldRequestLocation && !showGpsButton) {
        GetCurrentLocation(
            hasPermission = false,
            retryCounter = retryLocationCounter,
            onLocationResult = { lat, lon ->
                Log.d("HomeScreen", "📍 Ubicación obtenida: $lat, $lon")

                locationManager.updateLocation(lat, lon)
                locationReady = true

                if (!locationServiceStarted) {
                    LocationReminderService.start(context)
                    locationServiceStarted = true
                    Log.d("HomeScreen", "✅ Servicio de ubicación iniciado")
                }
            },
            onError = { error ->
                locationManager.setError(error)
                Log.e("HomeScreen", "❌ Error de ubicación: $error")

                if (error.contains("Permiso de ubicación denegado")) {
                    locationPermissionChecked = true
                    permissionType = "location"
                    showPermissionDialog = true
                }
            },
            onGpsDisabled = {
                Log.w("HomeScreen", "⚠️ GPS deshabilitado")
                showGpsButton = true
            },
            onPermissionGranted = {
                Log.d("HomeScreen", "✅ Permisos de ubicación concedidos")
                locationPermissionChecked = true
            }
        )
    }

    // 🔥 AGREGAR ESTO AQUÍ:
    LaunchedEffect(locationPermissionChecked, locationReady) {
        if (locationPermissionChecked && locationReady) {
            Log.d("HomeScreen", "🔄 Permisos de ubicación confirmados, verificando tracking pendiente...")
            authViewModel.reiniciarTrackingSiPendiente()
        }
    }

    val logoScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ), label = ""
    )

    val logoRotation by animateFloatAsState(
        targetValue = if (isVisible) 0f else 360f,
        animationSpec = tween(1000), label = ""
    )

    val accentColor = Color(0xFFFF6B6B)

    LaunchedEffect(userState, isLoggedIn) {
        if (!isLoggedIn || (userState != null && !userState.activo)) {
            authViewModel.logout(context, shouldRemoveFCMToken = false) {
                navController.navigate("login") {
                    popUpTo("home") { inclusive = true }
                }
            }
        }
    }

    LaunchedEffect("animations") {
        delay(300)
        isVisible = true
        delay(800)
        showContent = true
    }

    val isLoading = authViewModel.isLoading
    val errorMessage = authViewModel.errorMessage

    // 🔔 Diálogo de permisos
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            icon = {
                Icon(
                    if (permissionType == "notification") Icons.Default.Notifications else Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = if (permissionType == "notification")
                        "Notificaciones desactivadas"
                    else
                        "Ubicación desactivada",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    if (permissionType == "notification") {
                        "Para recibir recordatorios, necesitas activar las notificaciones en la configuración de tu dispositivo.\n\n" +
                                "Ve a: Ajustes → Apps → RecuerdaGo → Notificaciones"
                    } else {
                        "Para usar recordatorios basados en ubicación, necesitas activar los permisos de ubicación.\n\n" +
                                "Ve a: Ajustes → Apps → RecuerdaGo → Permisos → Ubicación"
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionDialog = false
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                ) {
                    Text("Ir a ajustes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Ahora no")
                }
            }
        )
    }

    // 🔥 CAMBIO PRINCIPAL: Box que contiene TODO (contenido + overlay GPS)
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    NavigationBarItem(
                        selected = pagerState.currentPage == 0,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(0)
                            }
                        },
                        icon = {
                            Icon(
                                Icons.Default.Home,
                                contentDescription = "Inicio",
                                tint = if (pagerState.currentPage == 0)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    NavigationBarItem(
                        selected = pagerState.currentPage == 1,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(1)
                            }
                        },
                        icon = {
                            Icon(
                                Icons.Default.Map,
                                contentDescription = "Rutas alternas",
                                tint = if (pagerState.currentPage == 1)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    NavigationBarItem(
                        selected = pagerState.currentPage == 2,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(2)
                            }
                        },
                        icon = {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = "Recordatorios",
                                tint = if (pagerState.currentPage == 2)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    NavigationBarItem(
                        selected = pagerState.currentPage == 3,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(3)
                            }
                        },
                        icon = {
                            Icon(
                                Icons.Default.Group,
                                contentDescription = "Grupos colaborativos",
                                tint = if (pagerState.currentPage == 3)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    NavigationBarItem(
                        selected = pagerState.currentPage == 4,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(4)
                            }
                        },
                        icon = {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Configuración",
                                tint = if (pagerState.currentPage == 4)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(getBackgroundGradient())
                    .padding(paddingValues)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header con logo
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .scale(logoScale)
                                .rotate(logoRotation),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Ubicación",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(50.dp)
                            )
                            Icon(
                                imageVector = Icons.Default.AccessAlarm,
                                contentDescription = "Alarma",
                                tint = accentColor,
                                modifier = Modifier
                                    .size(20.dp)
                                    .offset(x = 15.dp, y = (-15).dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        AnimatedVisibility(
                            visible = isVisible,
                            enter = slideInVertically(
                                initialOffsetY = { -it }
                            ) + fadeIn(
                                animationSpec = tween(800, delayMillis = 400)
                            )
                        ) {
                            Text(
                                text = "RecuerdaGo",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        when (page) {
                            0 -> HomeTabContent(
                                userState = userState,
                                isLoading = isLoading,
                                errorMessage = errorMessage,
                                authViewModel = authViewModel,
                                showContent = showContent,
                                accentColor = accentColor,
                                onTabSelected = { tabIndex ->
                                    scope.launch {
                                        pagerState.animateScrollToPage(tabIndex)
                                    }
                                }
                            )
                            1 -> AlternateRoutesScreen(
                                navController = navController,
                                token = accessToken,
                                notificationViewModel = notificationViewModel
                            )
                            2 -> RemindersScreen(
                                navController = navController,
                                token = accessToken
                            )
                            3 -> CollaborativeGroupsScreen(
                                navController = navController,
                                token = accessToken,
                                notificationViewModel = notificationViewModel
                            )
                            4 -> SettingsScreen(
                                userState = userState,
                                onLogout = {
                                    authViewModel.logout(context) {
                                        navController.navigate("login") {
                                            popUpTo("home") { inclusive = true }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        if (showGpsButton) {
            GpsEnableButton(
                onEnableGps = {
                    showGpsButton = false
                    retryLocationCounter++
                }
            )
        }
    }
}