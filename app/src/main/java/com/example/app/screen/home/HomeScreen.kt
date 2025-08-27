package com.example.app.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.app.screen.home.components.HomeTabContent
import com.example.app.screen.home.components.PlaceholderTab
import com.example.app.screen.rutas.AlternateRoutesScreen
import com.example.app.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    authViewModel: AuthViewModel,
    navController: NavController
) {
    val userState = authViewModel.user
    val isLoggedIn = authViewModel.isLoggedIn
    val isDarkTheme = isSystemInDarkTheme()
    val accessToken = authViewModel.accessToken ?: ""

    // Estados de animación
    var isVisible by remember { mutableStateOf(false) }
    var showContent by remember { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableStateOf(0) }

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

    // Colores según el tema
    val backgroundGradient = if (isDarkTheme) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1A1A2E),
                Color(0xFF16213E)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF8F9FA),
                Color(0xFFE3F2FD)
            )
        )
    }

    val cardColors = if (isDarkTheme) {
        CardDefaults.cardColors(containerColor = Color(0xFF2D2D44))
    } else {
        CardDefaults.cardColors(containerColor = Color.White)
    }

    val primaryColor = if (isDarkTheme) Color(0xFF64B5F6) else Color(0xFF1976D2)
    val textColor = if (isDarkTheme) Color.White else Color.Black
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

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = if (isDarkTheme) Color(0xFF2D2D44) else Color.White,
                contentColor = primaryColor
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = "Inicio",
                            tint = if (selectedTab == 0) primaryColor else Color.Gray
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
                            tint = if (selectedTab == 1) primaryColor else Color.Gray
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
                            tint = if (selectedTab == 2) primaryColor else Color.Gray
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
                            tint = if (selectedTab == 3) primaryColor else Color.Gray
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
                            tint = if (selectedTab == 4) primaryColor else Color.Gray
                        )
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
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
                            tint = primaryColor,
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
                            color = primaryColor
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
                            primaryColor = primaryColor,
                            textColor = textColor,
                            cardColors = cardColors,
                            showContent = showContent,
                            accentColor = accentColor,
                            isDarkTheme = isDarkTheme,
                            onTabSelected = { selectedTab = it }
                        )
                        1 -> AlternateRoutesScreen(
                            navController = navController,
                            token = accessToken,
                            isDarkTheme = isDarkTheme,
                            primaryColor = primaryColor,
                            textColor = textColor,
                        )
                        2 -> PlaceholderTab("Recordatorios", "Próximamente")
                        3 -> PlaceholderTab("Grupos Colaborativos", "Próximamente")
                        4 -> PlaceholderTab("Configuración", "Próximamente")
                    }
                }

            }
        }
    }
}