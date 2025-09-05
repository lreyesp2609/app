package com.example.app.screen.rutas.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.app.ui.theme.getBackgroundGradient
import com.example.app.viewmodels.MapViewModel
import kotlinx.coroutines.delay

@Composable
fun RutasBottomButtons(
    modifier: Modifier = Modifier,
    onAgregarClick: () -> Unit = {},
    onRutasClick: () -> Unit = {},
    onUbicacionClick: () -> Unit = {},
    selectedTransportMode: String,
    showDestinationReached: Boolean = false,
    destinationMessage: String = "",
    onDismissDestination: () -> Unit = {},
    showTransportMessage: Boolean = false,
    transportMessage: String = "",
    onDismissTransport: () -> Unit = {},
    viewModel: MapViewModel,
    token: String,
    selectedLocationId: Int,
    ) {
    val isDarkTheme = isSystemInDarkTheme()

    Column(modifier = modifier) {
        // Notificación de destino alcanzado
        AnimatedVisibility(
            visible = showDestinationReached,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.9f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = destinationMessage,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    IconButton(
                        onClick = onDismissDestination,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Notificación de cambio de transporte
        AnimatedVisibility(
            visible = showTransportMessage,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when(selectedTransportMode) {
                                "walking" -> Icons.Default.DirectionsWalk
                                "driving" -> Icons.Default.DirectionsCar
                                "cycling" -> Icons.Default.DirectionsBike
                                else -> Icons.Default.DirectionsWalk
                            },
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = transportMessage,
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    IconButton(
                        onClick = onDismissTransport,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        // Botones principales (sin cambios)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(getBackgroundGradient(isDarkTheme))
                .padding(vertical = 12.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botón Agregar ubicación
            FloatingActionButton(
                onClick = onAgregarClick,
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                modifier = Modifier.size(56.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Agregar")
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Agregar",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            // Botón Rutas alternas
            FloatingActionButton(
                onClick = {
                    onRutasClick() // ✅ Solo esta llamada, sin guardarRuta()
                },
                containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f),
                modifier = Modifier.size(56.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.Directions, contentDescription = "Rutas")
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Rutas",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            // Botón centrar GPS
            FloatingActionButton(
                onClick = onUbicacionClick,
                containerColor = if (isDarkTheme) Color(0xFF64B5F6).copy(alpha = 0.9f)
                else Color(0xFF1976D2).copy(alpha = 0.9f),
                modifier = Modifier.size(56.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.LocationOn, contentDescription = "GPS")
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "GPS",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }

    // Auto-dismiss para el mensaje de transporte (más corto)
    LaunchedEffect(showTransportMessage) {
        if (showTransportMessage) {
            delay(2500) // 2.5 segundos
            onDismissTransport()
        }
    }
}