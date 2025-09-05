package com.example.app.screen.mapa

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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import calcularDistanciaSobreRuta
import com.example.app.models.UbicacionUsuarioResponse
import com.example.app.screen.rutas.components.RutasBottomButtons
import com.example.app.viewmodel.decodePolyline
import com.example.app.viewmodels.MapViewModel
import kotlinx.coroutines.delay
import org.osmdroid.util.GeoPoint
import kotlin.collections.isNotEmpty

@Composable
fun RutaMapa(
    modifier: Modifier = Modifier,
    defaultLat: Double = 0.0,
    defaultLon: Double = 0.0,
    ubicaciones: List<UbicacionUsuarioResponse> = emptyList(),
    viewModel: MapViewModel = viewModel(),
    token: String,
    selectedLocationId: Int
) {
    var tiempoRecorrido by remember { mutableStateOf(0L) }
    var tiempoInicioRuta by remember { mutableStateOf(0L) } // NUEVO: tiempo cuando inició la ruta

    // Estados existentes
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

    // Observar la ruta del ViewModel
    val currentRoute by viewModel.route

    // Estados para mostrar información de la ruta
    var showRouteInfo by remember { mutableStateOf(false) }
    var routeDistance by remember { mutableStateOf("") }
    var routeDuration by remember { mutableStateOf("") }

    // NUEVOS ESTADOS para manejar la ruta original
    var rutaOriginal by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var distanciaOriginal by remember { mutableStateOf(0.0) }
    var duracionOriginal by remember { mutableStateOf(0.0) }
    var rutaActiva by remember { mutableStateOf(false) }

    // Estados para mensajes
    var destinoAlcanzado by remember { mutableStateOf(false) }
    var mensajeDestinoMostrado by remember { mutableStateOf(false) }
    var showTransportMessage by remember { mutableStateOf(false) }
    var transportMessage by remember { mutableStateOf("") }


    val mostrarOpcionesFinalizar by viewModel.mostrarOpcionesFinalizar
    val rutaIdActiva by viewModel.rutaIdActiva


    rutaActiva = true
    tiempoInicioRuta = System.currentTimeMillis() / 1000


    // Actualizar selectedLocation cuando cambien las ubicaciones
    LaunchedEffect(ubicaciones) {
        if (ubicaciones.isNotEmpty()) {
            selectedLocation = ubicaciones.first()
            mensajeDestinoMostrado = false
            destinoAlcanzado = false
        }
    }

    // GUARDAR ruta original cuando se calcula nueva ruta
    LaunchedEffect(currentRoute) {
        currentRoute?.routes?.firstOrNull()?.let { route ->
            rutaOriginal = route.geometry.decodePolyline()
            distanciaOriginal = route.summary.distance * 1000.0 // convertir a metros
            duracionOriginal = route.summary.duration // en segundos
            rutaActiva = true
            tiempoInicioRuta = System.currentTimeMillis() / 1000 // tiempo actual en segundos

            // Calcular valores iniciales
            val distanciaRestanteKm = calcularDistanciaSobreRuta(userLat.value, userLon.value, rutaOriginal) / 1000.0
            val durationRemaining = duracionOriginal * (distanciaRestanteKm / (distanciaOriginal / 1000.0))
            val durationMin = (durationRemaining / 60.0).toInt()

            routeDistance = String.format("%.2f km", distanciaRestanteKm)
            routeDuration = "${durationMin} min"
            showRouteInfo = true

            mensajeDestinoMostrado = false
            destinoAlcanzado = false
        }
    }

    // ACTUALIZAR distancia y tiempo continuamente cuando hay ruta activa
    LaunchedEffect(userLat.value, userLon.value, rutaActiva) {
        if (rutaActiva && rutaOriginal.isNotEmpty()) {
            // Calcular distancia restante usando GPS actual
            val distanciaRestanteMetros = calcularDistanciaSobreRuta(userLat.value, userLon.value, rutaOriginal)
            val distanciaRestanteKm = distanciaRestanteMetros / 1000.0

            // Calcular tiempo transcurrido desde que inició la ruta
            val tiempoActual = System.currentTimeMillis() / 1000
            val tiempoTranscurrido = tiempoActual - tiempoInicioRuta

            // Calcular duración restante basada en la proporción de distancia
            val proporcionDistanciaRestante = if (distanciaOriginal > 0) {
                distanciaRestanteMetros / distanciaOriginal
            } else {
                0.0
            }

            // Tiempo estimado original para la distancia restante
            val tiempoEstimadoRestante = duracionOriginal * proporcionDistanciaRestante

            // Calcular duración usando proporción simple como Google Maps
            val duracionFinal = if (tiempoTranscurrido > 30 && distanciaOriginal > distanciaRestanteMetros) {
                // Calcular progreso real
                val distanciaRecorrida = distanciaOriginal - distanciaRestanteMetros
                val progresoReal = if (distanciaOriginal > 0) distanciaRecorrida / distanciaOriginal else 0.0

                if (progresoReal > 0.01) { // Solo si hay progreso significativo (1%)
                    // Calcular tiempo restante basado en velocidad real observada
                    val velocidadObservada = distanciaRecorrida / tiempoTranscurrido
                    if (velocidadObservada > 0) {
                        distanciaRestanteMetros / velocidadObservada
                    } else {
                        tiempoEstimadoRestante
                    }
                } else {
                    // Si no hay progreso significativo, usar estimación original
                    tiempoEstimadoRestante
                }
            } else {
                // Usar estimación proporcional simple
                tiempoEstimadoRestante
            }

            val durationMin = (duracionFinal / 60.0).toInt()

            // Actualizar valores mostrados de forma simple
            routeDistance = String.format("%.2f km", distanciaRestanteKm)
            routeDuration = when {
                distanciaRestanteMetros < 50 -> "Llegando..."
                durationMin > 0 -> "${durationMin} min"
                distanciaRestanteKm > 0 -> {
                    // Usar proporción simple de la estimación original
                    val tiempoProporcionado = (duracionOriginal * proporcionDistanciaRestante / 60.0).toInt()
                    "${maxOf(tiempoProporcionado, 1)} min"
                }
                else -> "Llegando..."
            }

            // Detección de llegada MÁS ESTRICTA - usar distancia al destino real
            selectedLocation?.let { destino ->
                val distanciaAlDestino = calcularDistancia(
                    userLat.value, userLon.value,
                    destino.latitud, destino.longitud
                )

                // Solo considerar llegada si está a menos de 30 metros del destino real
                if (distanciaAlDestino < 30 && !mensajeDestinoMostrado) {
                    rutaActiva = false
                    destinoAlcanzado = true
                    mensajeDestinoMostrado = true
                }
            }
        }
    }

    // Contador de tiempo para mostrar tiempo transcurrido
    LaunchedEffect(locationObtained) {
        if (locationObtained) {
            while (true) {
                delay(1000L)
                tiempoRecorrido++
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

            if (distancia < 50 && !mensajeDestinoMostrado) {
                destinoAlcanzado = true
                mensajeDestinoMostrado = true
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
                        rutaActiva = false // RESETEAR ruta activa

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

                // Mostrar información de la ruta - ACTUALIZADA DINÁMICAMENTE
                if (showRouteInfo && (currentRoute != null || rutaActiva)) {
                    RouteInfoCard(
                        distance = routeDistance,
                        duration = routeDuration,
                        transportMode = selectedTransportMode,
                        onDismiss = {
                            showRouteInfo = false
                            viewModel.clearRoute()
                            rutaActiva = false // RESETEAR ruta activa
                        },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(16.dp)
                    )
                }

                // Mostrar información de la ruta - ACTUALIZADA DINÁMICAMENTE
                if (showRouteInfo && (currentRoute != null || rutaActiva)) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(top = 16.dp, start = 16.dp, end = 16.dp) // 👈 espacio superior
                    ) {
                        // Card con km/min
                        RouteInfoCard(
                            distance = routeDistance,
                            duration = routeDuration,
                            transportMode = selectedTransportMode,
                            onDismiss = {
                                showRouteInfo = false
                                viewModel.clearRoute()
                                rutaActiva = false // RESETEAR ruta activa
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Botones Finalizar/Cancelar debajo 👇
                        if (mostrarOpcionesFinalizar) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp), // espacio entre la card y los botones
                                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
                            ) {
                                Button(
                                    onClick = {
                                        rutaIdActiva?.let { id ->
                                            viewModel.finalizarRutaBackend(id)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "Finalizar", tint = Color.White)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Finalizar")
                                }

                                Button(
                                    onClick = {
                                        rutaIdActiva?.let { id ->
                                            viewModel.cancelarRutaBackend(id)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Cancelar", tint = Color.White)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Cancelar")
                                }
                            }
                        }
                    }
                }

                // BOTONES INFERIORES
                RutasBottomButtons(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    selectedTransportMode = selectedTransportMode,
                    showDestinationReached = destinoAlcanzado,
                    destinationMessage = "Has llegado a tu destino",
                    onDismissDestination = { destinoAlcanzado = false },
                    showTransportMessage = showTransportMessage,
                    transportMessage = transportMessage,
                    onDismissTransport = { showTransportMessage = false },
                    onAgregarClick = { },
                    onRutasClick = {
                        selectedLocation?.let { destination ->
                            if (userLat.value != 0.0 && userLon.value != 0.0) {
                                val startPoint = Pair(userLat.value, userLon.value)
                                val endPoint = Pair(destination.latitud, destination.longitud)

                                // ✅ AGREGAR los parámetros para guardar automáticamente
                                viewModel.fetchRouteWithML(
                                    start = startPoint,
                                    end = endPoint,
                                    token = token,
                                    ubicacionId = selectedLocationId,
                                    transporteTexto = selectedTransportMode
                                )

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
                            mapCenterLat = loc.latitud
                            mapCenterLon = loc.longitud
                            recenterTrigger++
                        }
                    },
                    viewModel = viewModel,                 // 🔹 PASAR ViewModel
                    token = token,                         // 🔹 PASAR token
                    selectedLocationId = selectedLocationId // 🔹 PASAR ID
                )
            }
            else -> {
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
        "foot-walking" -> "Caminar"
        "driving-car" -> "Carro"
        "cycling-regular" -> "Bicicleta"
        else -> "Caminar"
    }
}