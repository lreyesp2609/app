package com.example.app.screen.home

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
import android.provider.Settings

@Composable
fun HomeScreen(
    authViewModel: AuthViewModel,
    navController: NavController,
    initialTab: Int = 0
) {
    val context = LocalContext.current
    val userState = authViewModel.user
    val isLoggedIn = authViewModel.isLoggedIn
    val accessToken = authViewModel.accessToken ?: ""

    // Estados de animación
    var isVisible by remember { mutableStateOf(false) }
    var showContent by remember { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableStateOf(initialTab) }

    // 🔔 Estado para el permiso de notificaciones
    var showPermissionDialog by remember { mutableStateOf(false) }

    // 🔔 Launcher para solicitar permiso de notificaciones (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("HomeScreen", "✅ Permiso de notificaciones concedido")
            Toast.makeText(context, "Notificaciones activadas", Toast.LENGTH_SHORT).show()
        } else {
            Log.w("HomeScreen", "⚠️ Permiso de notificaciones denegado")
            showPermissionDialog = true
        }
    }

    // 🔔 Solicitar permiso al iniciar
    LaunchedEffect(Unit) {
        delay(1000) // Esperar un poco después de que cargue la pantalla

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS

            when {
                ContextCompat.checkSelfPermission(
                    context,
                    permission
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d("HomeScreen", "✅ Permiso de notificaciones ya concedido")
                }
                else -> {
                    Log.d("HomeScreen", "🔔 Solicitando permiso de notificaciones")
                    notificationPermissionLauncher.launch(permission)
                }
            }
        }

        // Crear el canal de notificaciones
        NotificationHelper.createNotificationChannel(context)
    }

    // Animaciones
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
            authViewModel.logout()
            navController.navigate("login") {
                popUpTo("home") { inclusive = true }
            }
        }
    }

    // Animación de entrada
    LaunchedEffect(Unit) {
        delay(300)
        isVisible = true
        delay(800)
        showContent = true
    }

    val isLoading = authViewModel.isLoading
    val errorMessage = authViewModel.errorMessage

    val userId = try {
        val idField = userState!!::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.get(userState).toString()
    } catch (e: Exception) {
        "N/A"
    }

    // 🔔 Diálogo para cuando el usuario deniega el permiso
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            icon = {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Notificaciones desactivadas",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Para recibir recordatorios, necesitas activar las notificaciones en la configuración de tu dispositivo.\n\n" +
                            "Ve a: Ajustes → Apps → RecuerdaGo → Notificaciones"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionDialog = false
                        // Abrir configuración de la app
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

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = "Inicio",
                            tint = if (selectedTab == 0)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            Icons.Default.Map,
                            contentDescription = "Rutas alternas",
                            tint = if (selectedTab == 1)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "Recordatorios",
                            tint = if (selectedTab == 2)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = {
                        Icon(
                            Icons.Default.Group,
                            contentDescription = "Grupos colaborativos",
                            tint = if (selectedTab == 3)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Configuración",
                            tint = if (selectedTab == 4)
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

                // Contenido de las pestañas
                Box(modifier = Modifier.fillMaxSize()) {
                    when (selectedTab) {
                        0 -> HomeTabContent(
                            userState = userState,
                            isLoading = isLoading,
                            errorMessage = errorMessage,
                            authViewModel = authViewModel,
                            showContent = showContent,
                            accentColor = accentColor,
                            onTabSelected = { selectedTab = it }
                        )
                        1 -> AlternateRoutesScreen(
                            navController = navController,
                            token = accessToken
                        )
                        2 -> RemindersScreen(
                            navController = navController,
                            token = accessToken
                        )
                        3 -> PlaceholderTab("Grupos Colaborativos", "Próximamente")
                        4 -> SettingsScreen(
                            userState = userState,
                            onLogout = {
                                authViewModel.logout()
                                navController.navigate("login") {
                                    popUpTo("home") { inclusive = true }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}