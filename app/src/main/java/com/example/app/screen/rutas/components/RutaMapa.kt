package com.example.app.screen.rutas.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.app.models.UbicacionUsuarioResponse
import com.example.app.viewmodel.decodePolyline
import com.example.app.viewmodel.MapViewModel
import kotlin.collections.isNotEmpty
import com.example.app.screen.mapa.GetCurrentLocation
import com.example.app.screen.mapa.GpsEnableButton
import com.example.app.screen.mapa.LocationTracker
import com.example.app.screen.mapa.MapControlButton
import com.example.app.screen.mapa.RouteInfoCard
import com.example.app.screen.mapa.SimpleMapOSM
import com.example.app.screen.mapa.calcularDistancia
import kotlin.math.roundToInt

@Composable
fun RutaMapa(
    modifier: Modifier = Modifier,
    defaultLat: Double = 0.0,
    defaultLon: Double = 0.0,
    ubicaciones: List<UbicacionUsuarioResponse> = emptyList(),
    viewModel: MapViewModel = viewModel(),
    token: String,
    selectedLocationId: Int,
    navController: NavController,

    ) {
    // Estados de ubicación y mapa
    val userLat = remember { mutableStateOf(defaultLat) }
    val userLon = remember { mutableStateOf(defaultLon) }
    var mapCenterLat by remember { mutableStateOf(defaultLat) }
    var mapCenterLon by remember { mutableStateOf(defaultLon) }
    var recenterTrigger by remember { mutableStateOf(0) }
    var locationObtained by remember { mutableStateOf(false) }
    var showGpsButton by remember { mutableStateOf(false) }
    var selectedLocation by remember { mutableStateOf<UbicacionUsuarioResponse?>(ubicaciones.firstOrNull()) }
    var selectedTransportMode by remember { mutableStateOf("foot-walking") }

    // Estados para controles de zoom
    var zoomInTrigger by remember { mutableStateOf(0) }
    var zoomOutTrigger by remember { mutableStateOf(0) }

    // Estados para la ruta
    val currentRoute by viewModel.route
    var showRouteInfo by remember { mutableStateOf(false) }
    var routeDistance by remember { mutableStateOf("") }
    var routeDuration by remember { mutableStateOf("") }
    var rutaEstado by remember { mutableStateOf(RutaEstado()) }

    // Estados para mensajes y notificaciones
    var destinoAlcanzado by remember { mutableStateOf(false) }
    var mensajeDestinoMostrado by remember { mutableStateOf(false) }
    var showTransportMessage by remember { mutableStateOf(false) }
    var transportMessage by remember { mutableStateOf("") }

    // NUEVO: Estado para la alerta de desobediencia
    var showSecurityAlert by remember { mutableStateOf(false) }
    var securityMessage by remember { mutableStateOf("") }

    // Estados del ViewModel
    val mostrarOpcionesFinalizar by viewModel.mostrarOpcionesFinalizar
    val rutaIdActiva by viewModel.rutaIdActiva
    val mostrarAlertaDesobediencia by viewModel.mostrarAlertaDesobediencia
    val mensajeAlertaDesobediencia by viewModel.mensajeAlertaDesobediencia

    val alternativeRoutes by viewModel.alternativeRoutes
    val showRouteSelector by viewModel.showRouteSelector

    // NUEVO: Observar la alerta de desobediencia y convertirla en notificación
    LaunchedEffect(mostrarAlertaDesobediencia, mensajeAlertaDesobediencia) {
        if (mostrarAlertaDesobediencia && mensajeAlertaDesobediencia != null) {
            securityMessage = mensajeAlertaDesobediencia ?: ""
            showSecurityAlert = true
            viewModel.cerrarAlertaDesobediencia() // Cerrar la alerta del ViewModel inmediatamente
        }
    }

    // Actualizar selectedLocation cuando cambien las ubicaciones
    LaunchedEffect(ubicaciones) {
        if (ubicaciones.isNotEmpty()) {
            selectedLocation = ubicaciones.first()
            mensajeDestinoMostrado = false
            destinoAlcanzado = false
        }
    }

    LaunchedEffect(currentRoute) {
        currentRoute?.routes?.firstOrNull()?.let { route ->
            val geometria = route.geometry.decodePolyline()
            val distanciaMetros = route.summary.distance * 1000.0
            val duracionSegundos = route.summary.duration

            rutaEstado = inicializarRutaEstado(
                rutaOriginal = geometria,
                distanciaMetros = distanciaMetros,
                duracionSegundos = duracionSegundos
            )

            showRouteInfo = true
            mensajeDestinoMostrado = false
            destinoAlcanzado = false
        }
    }

    LaunchedEffect(userLat.value, userLon.value) {
        if (rutaEstado.activa && userLat.value != 0.0 && userLon.value != 0.0) {
            rutaEstado = actualizarHistorialPosiciones(
                userLat.value,
                userLon.value,
                rutaEstado
            )
        }
    }

    CalculadorETADinamico(
        userLat = userLat.value,
        userLon = userLon.value,
        rutaEstado = rutaEstado,
        transportMode = selectedTransportMode,
        onETACalculado = { distancia, duracion ->
            routeDistance = distancia
            routeDuration = duracion
        }
    )

    // Detectar llegada al destino
    LaunchedEffect(userLat.value, userLon.value, rutaEstado.activa) {
        if (rutaEstado.activa && !mensajeDestinoMostrado) {
            selectedLocation?.let { destino ->
                val distanciaAlDestino = calcularDistancia(
                    userLat.value, userLon.value,
                    destino.latitud, destino.longitud
                )

                Log.d("RutaMapa", "Distancia al destino: ${distanciaAlDestino}m")

                if (distanciaAlDestino < 25) {
                    Log.d("RutaMapa", "🎯 LLEGADA DETECTADA!")

                    rutaEstado = rutaEstado.copy(activa = false)
                    destinoAlcanzado = true
                    mensajeDestinoMostrado = true

                    rutaIdActiva?.let { id ->
                        Log.d("RutaMapa", "📤 Enviando finalizarRutaBackend con ID: $id")
                        viewModel.finalizarRutaBackend(id)
                    }

                    showRouteInfo = false
                    viewModel.clearRoute()
                    viewModel.ocultarOpcionesFinalizar()
                }
            }
        }
    }

    LocationTracker { lat, lon ->
        userLat.value = lat
        userLon.value = lon

        if (rutaIdActiva != null) {
            viewModel.agregarPuntoGPSReal(lat, lon)
        }

        if (!locationObtained) {
            mapCenterLat = lat
            mapCenterLon = lon
            locationObtained = true
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
                    // ➕ Zoom In
                    MapControlButton(
                        icon = Icons.Default.Add,
                        onClick = { zoomInTrigger++ }
                    )

                    // ➖ Zoom Out
                    MapControlButton(
                        icon = Icons.Default.Remove,
                        onClick = { zoomOutTrigger++ }
                    )

                    // 🎯 Centrar usuario
                    MapControlButton(
                        icon = Icons.Default.MyLocation,
                        onClick = {
                            mapCenterLat = userLat.value
                            mapCenterLon = userLon.value
                            recenterTrigger++
                        }
                    )
                }


                // Información de la ruta
                if (showRouteInfo && (currentRoute != null || rutaEstado.activa)) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                    ) {
                        RouteInfoCard(
                            distance = routeDistance,
                            duration = routeDuration,
                            transportMode = selectedTransportMode,
                            onDismiss = {
                                showRouteInfo = false
                                viewModel.clearRoute()
                                rutaEstado = rutaEstado.copy(activa = false)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Botones Finalizar/Cancelar
                        if (mostrarOpcionesFinalizar) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
                            ) {
                                Button(
                                    onClick = {
                                        rutaIdActiva?.let { id ->
                                            viewModel.finalizarRutaBackend(id)
                                        }
                                        showRouteInfo = false
                                        viewModel.clearRoute()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "Finalizar", tint = Color.White)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Finalizar", color = Color.White)
                                }

                                Button(
                                    onClick = {
                                        rutaIdActiva?.let { id ->
                                            viewModel.cancelarRutaBackend(id)
                                        }
                                        showRouteInfo = false
                                        viewModel.clearRoute()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Cancelar", tint = Color.White)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Cancelar", color = Color.White)
                                }
                            }
                        }
                    }
                }

                if (showRouteSelector && alternativeRoutes.isNotEmpty()) {
                    RouteAlternativesDialog(
                        alternatives = alternativeRoutes,
                        transportMode = selectedTransportMode,
                        onSelectRoute = { alternative ->
                            viewModel.selectRouteAlternative(
                                alternative = alternative,
                                token = token,
                                ubicacionId = selectedLocationId,
                                transporteTexto = selectedTransportMode
                            )

                            // Actualizar UI
                            showRouteInfo = true
                            routeDistance = "${(alternative.distance / 1000).roundToInt()} km"
                            routeDuration = "${(alternative.duration / 60).roundToInt()} min"

                            transportMessage = "Ruta ${alternative.displayName} seleccionada"
                            showTransportMessage = true
                        },
                        onDismiss = { viewModel.hideRouteSelector() }
                    )
                }

                RutasBottomButtons(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    selectedTransportMode = selectedTransportMode,
                    showDestinationReached = destinoAlcanzado,
                    destinationMessage = "Has llegado a tu destino",
                    onDismissDestination = { destinoAlcanzado = false },
                    showTransportMessage = showTransportMessage,
                    transportMessage = transportMessage,
                    onDismissTransport = { showTransportMessage = false },
                    navController = navController,
                    showSecurityAlert = showSecurityAlert,
                    securityMessage = securityMessage,
                    onDismissSecurityAlert = { showSecurityAlert = false },
                    onAgregarClick = { },
                    onRutasClick = {
                        selectedLocation?.let { destination ->
                            if (userLat.value != 0.0 && userLon.value != 0.0) {
                                val startPoint = Pair(userLat.value, userLon.value)
                                val endPoint = Pair(destination.latitud, destination.longitud)

                                viewModel.fetchAllRouteAlternatives(
                                    start = startPoint,
                                    end = endPoint,
                                    token = token,
                                    ubicacionId = selectedLocationId,
                                    transporteTexto = selectedTransportMode
                                )

                                transportMessage = "Calculando rutas alternativas..."
                                showTransportMessage = true

                                transportMessage = "Calculando ruta en ${getModeDisplayName(selectedTransportMode)}"
                                showTransportMessage = true
                            } else {
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
                            recenterTrigger++
                        }
                    },
                    viewModel = viewModel,
                    token = token,
                    selectedLocationId = selectedLocationId
                )
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
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
            isSelected = selectedMode == "foot-walking",
            onClick = { onModeSelected("foot-walking") },
            contentDescription = "Caminar"
        )

        TransportButton(
            icon = Icons.Default.DirectionsCar,
            isSelected = selectedMode == "driving-car",
            onClick = { onModeSelected("driving-car") },
            contentDescription = "Carro"
        )

        TransportButton(
            icon = Icons.Default.DirectionsBike,
            isSelected = selectedMode == "cycling-regular",
            onClick = { onModeSelected("cycling-regular") },
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
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
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
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.size(24.dp)
        )
    }
}

fun getModeDisplayName(mode: String): String {
    return when (mode) {
        "foot-walking" -> "Caminar"
        "driving-car" -> "Carro"
        "cycling-regular" -> "Bicicleta"
        else -> "Caminar"
    }
}