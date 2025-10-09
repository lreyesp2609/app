package com.example.app.screen.recordatorios

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.app.screen.components.AppButton
import kotlinx.coroutines.delay

@Composable
fun RemindersScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    var showContent by remember { mutableStateOf(false) }

    // Animación de entrada
    LaunchedEffect(Unit) {
        delay(200)
        showContent = true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(animationSpec = tween(600)) +
                        slideInVertically(initialOffsetY = { -it })
            ) {

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Recordatorios",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Recordatorios",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Botón principal
                    AppButton(
                        text = "Agregar recordatorio",
                        icon = Icons.Default.AddLocationAlt,
                        onClick = { navController.navigate("reminder_map") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Contenido informativo
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(animationSpec = tween(800, delayMillis = 400)) +
                        slideInVertically(initialOffsetY = { it / 2 })
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Icono grande
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessAlarm,
                            contentDescription = "Alarma",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(56.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Card con características
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Text(
                                text = "Características",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            ReminderFeatureItem(
                                icon = Icons.Default.LocationOn,
                                text = "Recordatorios basados en ubicación"
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            ReminderFeatureItem(
                                icon = Icons.Default.Schedule,
                                text = "Alertas personalizadas por horario"
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            ReminderFeatureItem(
                                icon = Icons.Default.Repeat,
                                text = "Recordatorios recurrentes"
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            ReminderFeatureItem(
                                icon = Icons.Default.Group,
                                text = "Notificaciones grupales"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderFeatureItem(
    icon: ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}