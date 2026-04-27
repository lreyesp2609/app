package com.rutai.app.screen.rutas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rutai.app.models.UbicacionUsuarioResponse
import com.rutai.app.ui.theme.AppColors
import com.rutai.app.ui.theme.SecurityColors
import com.rutai.app.viewmodel.NotificationViewModel
import com.rutai.app.viewmodel.UbicacionesViewModel
import com.rutai.app.viewmodel.UbicacionesViewModelFactory
import kotlinx.coroutines.delay
import com.rutai.app.R
@Composable
fun AlternateRoutesScreen(
    token: String,
    modifier: Modifier = Modifier,
    navController: NavController,
    notificationViewModel: NotificationViewModel
) {
    val context = LocalContext.current
    val viewModel: UbicacionesViewModel = viewModel(
        factory = UbicacionesViewModelFactory(context, token)
    )
    val isDarkTheme = isSystemInDarkTheme()

    var showContent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(200)
        showContent = true
        viewModel.cargarUbicaciones()
    }

    val ubicaciones = viewModel.ubicaciones
    val isLoading = viewModel.isLoading

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 0.dp) // ✨ Eliminado padding vertical
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header animado
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
                            imageVector = Icons.Default.Route,
                            contentDescription = stringResource(R.string.cd_routes),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.routes_title),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Botones de acción principales
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Botón agregar destino
                        Button(
                            onClick = { navController.navigate("mapa") },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                Icons.Default.AddLocationAlt,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.new_destination),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        // Botón zonas peligrosas
                        OutlinedButton(
                            onClick = { navController.navigate("zonas_peligrosas") },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                2.dp,
                                SecurityColors.getDangerColor(isDarkTheme)
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = SecurityColors.getDangerColor(isDarkTheme)
                            )
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.zones),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Contenido principal
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(animationSpec = tween(800, delayMillis = 300)) +
                        scaleIn(initialScale = 0.9f)
            ) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = stringResource(R.string.loading_destinations),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    ubicaciones.isEmpty() -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Ícono animado
                            var iconScale by remember { mutableStateOf(0f) }
                            LaunchedEffect(Unit) {
                                iconScale = 1f
                            }

                            Box(
                                modifier = Modifier
                                    .size(140.dp)
                                    .scale(
                                        animateFloatAsState(
                                            targetValue = iconScale,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow
                                            ), label = ""
                                        ).value
                                    )
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.secondaryContainer,
                                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                                            )
                                        ),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Map,
                                    contentDescription = stringResource(R.string.cd_no_reminders),
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(72.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = stringResource(R.string.no_destinations_title),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = stringResource(R.string.no_destinations_description),
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            // Card informativo
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    // Header informativo
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 16.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(R.string.empty_state_help_title),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                        )
                                    }

                                    // Características
                                    FeatureRow(
                                        icon = Icons.Default.Place,
                                        title = stringResource(R.string.feature_save_destinations_title),
                                        description = stringResource(R.string.feature_save_destinations_desc)
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    FeatureRow(
                                        icon = Icons.Default.Route,
                                        title = stringResource(R.string.feature_route_options_title),
                                        description = stringResource(R.string.feature_route_options_desc)
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    FeatureRow(
                                        icon = Icons.Default.Analytics,
                                        title = stringResource(R.string.feature_stats_title),
                                        description = stringResource(R.string.feature_stats_desc)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }

                    else -> {
                        // Lista de ubicaciones
                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.destinations_count, ubicaciones.size),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(ubicaciones.size) { index ->
                                    UbicacionCard(
                                        ubicacion = ubicaciones[index],
                                        onGenerarRuta = {
                                            navController.navigate("rutas_screen/${ubicaciones[index].id}")
                                        },
                                        onEstadisticas = {
                                            navController.navigate("estadisticas/${ubicaciones[index].id}")
                                        },
                                        onEliminar = {
                                            viewModel.eliminarUbicacion(
                                                ubicaciones[index].id,
                                                notificationViewModel
                                            )
                                        }
                                    )
                                }

                                item {
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Componente simple para las características (igual que en Reminders)
@Composable
private fun FeatureRow(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            modifier = Modifier
                .size(20.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun UbicacionCard(
    ubicacion: UbicacionUsuarioResponse,
    onGenerarRuta: () -> Unit,
    onEstadisticas: () -> Unit,
    onEliminar: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .scale(if (isPressed) 0.98f else 1f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isPressed) 2.dp else 6.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header de la ubicación
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ubicacion.nombre,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = ubicacion.direccion_completa,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 18.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Botón PRINCIPAL - Generar ruta
            Button(
                onClick = {
                    isPressed = true
                    onGenerarRuta()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Directions,
                    contentDescription = stringResource(R.string.generate_route),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.generate_route),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Botones secundarios
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Botón Estadísticas
                OutlinedButton(
                    onClick = onEstadisticas,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    ),
                    contentPadding = PaddingValues(vertical = 10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = stringResource(R.string.data),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.data),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // 🆕 Botón Eliminar (reemplaza Editar)
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        AppColors.Danger.copy(alpha = 0.4f)
                    ),
                    contentPadding = PaddingValues(vertical = 10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = AppColors.Danger
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.delete),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    // Diálogo de confirmación para eliminar
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = AppColors.Danger,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    stringResource(R.string.delete_destination_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    stringResource(R.string.delete_destination_confirmation, ubicacion.nombre),
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onEliminar()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Danger
                    )
                ) {
                    Text(stringResource(R.string.delete), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}