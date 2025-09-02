package com.example.app.screen.mapa

import MapViewModel
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.app.models.UbicacionUsuarioResponse
import com.example.app.screen.rutas.components.RutasBottomButtons

@Composable
fun RutaMapa(
    modifier: Modifier = Modifier,
    defaultLat: Double = 0.0,
    defaultLon: Double = 0.0,
    ubicaciones: List<UbicacionUsuarioResponse> = emptyList(),
    viewModel: MapViewModel = viewModel()
) {
    val context = LocalContext.current

    // Estados existentes
    val userLat = remember { mutableStateOf(defaultLat) }
    val userLon = remember { mutableStateOf(defaultLon) }
    var mapCenterLat by remember { mutableStateOf(defaultLat) }
    var mapCenterLon by remember { mutableStateOf(defaultLon) }
    var recenterTrigger by remember { mutableStateOf(0) }
    var locationObtained by remember { mutableStateOf(false) }
    var showGpsButton by remember { mutableStateOf(false) }
    var selectedLocation by remember { mutableStateOf<UbicacionUsuarioResponse?>(ubicaciones.firstOrNull()) }
    var selectedTransportMode by remember { mutableStateOf("walking") }

    // Estados para controles de zoom
    var zoomInTrigger by remember { mutableStateOf(0) }
    var zoomOutTrigger by remember { mutableStateOf(0) }

    // Observar la ruta del ViewModel
    val currentRoute by viewModel.route

    // Estados para mostrar información de la ruta
    var showRouteInfo by remember { mutableStateOf(false) }
    var routeDistance by remember { mutableStateOf("") }
    var routeDuration by remember { mutableStateOf("") }

    // ESTADOS MODIFICADOS para evitar que reaparezca el mensaje
    var destinoAlcanzado by remember { mutableStateOf(false) }
    var mensajeDestinoMostrado by remember { mutableStateOf(false) } // NUEVO: rastrea si ya se mostró
    var showTransportMessage by remember { mutableStateOf(false) }
    var transportMessage by remember { mutableStateOf("") }

    // Actualizar selectedLocation cuando cambien las ubicaciones
    LaunchedEffect(ubicaciones) {
        if (ubicaciones.isNotEmpty()) {
            selectedLocation = ubicaciones.first()
            // RESETEAR cuando cambia la ubicación destino
            mensajeDestinoMostrado = false
            destinoAlcanzado = false
        }
    }

    // RESETEAR cuando se calcula nueva ruta
    LaunchedEffect(currentRoute) {
        currentRoute?.let { response ->
            response.routes.firstOrNull()?.let { route ->
                val distanceKm = (route.summary.distance / 1000.0)
                val durationMin = (route.summary.duration / 60.0).toInt()
                routeDistance = String.format("%.1f km", distanceKm)
                routeDuration = "${durationMin} min"
                showRouteInfo = true

                // RESETEAR el estado del mensaje cuando se calcula nueva ruta
                mensajeDestinoMostrado = false
                destinoAlcanzado = false
            }
        }
    }

    LocationTracker { lat, lon ->
        userLat.value = lat
        userLon.value = lon

        if (!locationObtained) {
            mapCenterLat = lat
            mapCenterLon = lon
            locationObtained = true
        }

        selectedLocation?.let { destino ->
            val distancia = calcularDistancia(lat, lon, destino.latitud, destino.longitud)

            // LÓGICA MODIFICADA: Solo mostrar si no se ha mostrado antes
            if (distancia < 50 && !mensajeDestinoMostrado) {
                destinoAlcanzado = true
                mensajeDestinoMostrado = true // Marcar que ya se mostró
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            showGpsButton -> {
                GpsEnableButton(
                    onEnableGps = { showGpsButton = false },
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            locationObtained -> {
                // Mapa con la ruta
                SimpleMapOSM(
                    userLat = userLat.value,
                    userLon = userLon.value,
                    recenterTrigger = recenterTrigger,
                    ubicaciones = ubicaciones,
                    mapCenterLat = mapCenterLat,
                    mapCenterLon = mapCenterLon,
                    transportMode = selectedTransportMode,
                    routeGeometry = currentRoute?.routes?.firstOrNull()?.geometry,
                    zoomInTrigger = zoomInTrigger,
                    zoomOutTrigger = zoomOutTrigger,
                    modifier = Modifier.fillMaxSize()
                )

                // Botones de transporte (lado izquierdo)
                TransportModeButtons(
                    selectedMode = selectedTransportMode,
                    onModeSelected = { mode ->
                        selectedTransportMode = mode
                        viewModel.setMode(mode)
                        viewModel.clearRoute()
                        showRouteInfo = false

                        // USAR NOTIFICACIÓN INTEGRADA en lugar de Toast
                        transportMessage = "Modo de transporte: ${getModeDisplayName(mode)}"
                        showTransportMessage = true
                    },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 16.dp)
                )

                // Controles del lado derecho (centrar, zoom)
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Botón Zoom In (+)
                    FloatingActionButton(
                        onClick = { zoomInTrigger++ },
                        modifier = Modifier.size(48.dp),
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Aumentar zoom",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    // Botón Zoom Out (-)
                    FloatingActionButton(
                        onClick = { zoomOutTrigger++ },
                        modifier = Modifier.size(48.dp),
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Disminuir zoom",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    // Botón para centrar usuario
                    FloatingActionButton(
                        onClick = {
                            mapCenterLat = userLat.value
                            mapCenterLon = userLon.value
                            recenterTrigger++
                        },
                        modifier = Modifier.size(48.dp),
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "Centrar usuario",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                // Mostrar información de la ruta
                if (showRouteInfo && currentRoute != null) {
                    RouteInfoCard(
                        distance = routeDistance,
                        duration = routeDuration,
                        transportMode = selectedTransportMode,
                        onDismiss = {
                            showRouteInfo = false
                            viewModel.clearRoute()
                        },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(16.dp)
                    )
                }

                // BOTONES INFERIORES CON NOTIFICACIONES INTEGRADAS
                RutasBottomButtons(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    selectedTransportMode = selectedTransportMode,
                    showDestinationReached = destinoAlcanzado,
                    destinationMessage = "Has llegado a tu destino",
                    onDismissDestination = {
                        destinoAlcanzado = false
                        // NO reseteamos mensajeDestinoMostrado para que no vuelva a aparecer
                    },
                    showTransportMessage = showTransportMessage,
                    transportMessage = transportMessage,
                    onDismissTransport = { showTransportMessage = false },
                    onAgregarClick = {
                        // Ya no usamos Toast aquí, puedes agregar lógica personalizada
                        // o crear un nuevo estado para mostrar mensaje de "Agregar ubicación"
                    },
                    onRutasClick = {
                        selectedLocation?.let { destination ->
                            if (userLat.value != 0.0 && userLon.value != 0.0) {
                                val startPoint = Pair(userLat.value, userLon.value)
                                val endPoint = Pair(destination.latitud, destination.longitud)
                                viewModel.fetchRoute(startPoint, endPoint)

                                // USAR NOTIFICACIÓN INTEGRADA
                                transportMessage = "Calculando ruta en ${getModeDisplayName(selectedTransportMode)}"
                                showTransportMessage = true
                            } else {
                                // Puedes crear otro estado para este mensaje si quieres
                                transportMessage = "Ubicación del usuario no disponible"
                                showTransportMessage = true
                            }
                        } ?: run {
                            transportMessage = "Selecciona un destino primero"
                            showTransportMessage = true
                        }
                    },
                    onUbicacionClick = {
                        selectedLocation?.let { loc ->
                            mapCenterLat = loc.latitud
                            mapCenterLon = loc.longitud
                            recenterTrigger++
                        }
                    }
                )
            }
            else -> {
                // Estado de carga
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }

                GetCurrentLocation(
                    onLocationResult = { lat, lon ->
                        userLat.value = lat
                        userLon.value = lon
                        locationObtained = true
                    },
                    onError = { showGpsButton = true },
                    onGpsDisabled = { showGpsButton = true }
                )
            }
        }
    }
}

@Composable
fun TransportModeButtons(
    selectedMode: String,
    onModeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TransportButton(
            icon = Icons.Default.DirectionsWalk,
            isSelected = selectedMode == "walking",
            onClick = { onModeSelected("walking") },
            contentDescription = "Caminar"
        )

        TransportButton(
            icon = Icons.Default.DirectionsCar,
            isSelected = selectedMode == "driving",
            onClick = { onModeSelected("driving") },
            contentDescription = "Carro"
        )

        TransportButton(
            icon = Icons.Default.DirectionsBike,
            isSelected = selectedMode == "cycling",
            onClick = { onModeSelected("cycling") },
            contentDescription = "Bicicleta"
        )
    }
}

@Composable
fun TransportButton(
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    contentDescription: String
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .background(
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.Black.copy(alpha = 0.7f)
                },
                shape = CircleShape
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                Color.White
            },
            modifier = Modifier.size(24.dp)
        )
    }
}

fun getModeDisplayName(mode: String): String {
    return when (mode) {
        "walking" -> "Caminar"
        "driving" -> "Carro"
        "cycling" -> "Bicicleta"
        else -> "Caminar"
    }
}