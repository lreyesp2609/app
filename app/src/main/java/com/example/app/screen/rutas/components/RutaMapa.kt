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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.app.R
import com.example.app.models.UbicacionUsuarioResponse
import com.example.app.screen.mapa.GetCurrentLocation
import com.example.app.screen.mapa.GpsEnableButton
import com.example.app.screen.mapa.LocationTracker
import com.example.app.screen.mapa.MapControlButton
import com.example.app.screen.mapa.RouteInfoCard
import com.example.app.screen.mapa.SimpleMapOSM
import com.example.app.screen.mapa.calcularDistancia
import com.example.app.utils.LocationManager
import com.example.app.utils.getModeDisplayName
import com.example.app.utils.getNivelPeligroColor
import com.example.app.viewmodel.MapViewModel
import com.example.app.viewmodel.MapViewModelFactory
import com.example.app.viewmodel.decodePolyline
import kotlin.math.roundToInt

@Composable
fun RutaMapa(
    modifier: Modifier = Modifier,
    defaultLat: Double = 0.0,
    defaultLon: Double = 0.0,
    ubicaciones: List<UbicacionUsuarioResponse> = emptyList(),
    viewModel: MapViewModel = viewModel(factory = MapViewModelFactory(LocalContext.current)),
    token: String,
    selectedLocationId: Int,
    navController: NavController,
) {
    val context = LocalContext.current
    val locationManager = remember { LocationManager.getInstance() }

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

    // Estado para la alerta de desobediencia
    var showSecurityAlert by remember { mutableStateOf(false) }
    var securityMessage by remember { mutableStateOf("") }

    // Estados de SEGURIDAD del ViewModel
    val mostrarAdvertenciaSeguridad by viewModel.mostrarAdvertenciaSeguridad
    val validacionSeguridad by viewModel.validacionSeguridad
    val alternativeRoutes by viewModel.alternativeRoutes
    val showRouteSelector by viewModel.showRouteSelector

    // Estados del ViewModel
    val mostrarOpcionesFinalizar by viewModel.mostrarOpcionesFinalizar
    val rutaIdActiva by viewModel.rutaIdActiva
    val mostrarAlertaDesobediencia by viewModel.mostrarAlertaDesobediencia
    val mensajeAlertaDesobediencia by viewModel.mensajeAlertaDesobediencia

    val zonasPeligrosas by viewModel.zonasPeligrosas
    val mostrarZonasPeligrosas by viewModel.mostrarZonasPeligrosas
    val cargandoZonas by viewModel.cargandoZonas

    // 🆕 CARGAR ZONAS AL INICIAR
    LaunchedEffect(token) {
        if (token.isNotEmpty()) {
            viewModel.cargarZonasPeligrosas(token)
        }
    }


    // 🔥 NUEVO: Cargar ubicación desde caché
    LaunchedEffect(Unit) {
        val cachedLocation = locationManager.getLastKnownLocation()
        if (cachedLocation != null) {
            val ageSeconds = (System.currentTimeMillis() - cachedLocation.timestamp) / 1000
            Log.d("RutaMapa", "⚡ Usando ubicación en caché (${ageSeconds}s de antigüedad)")

            userLat.value = cachedLocation.latitude
            userLon.value = cachedLocation.longitude
            mapCenterLat = cachedLocation.latitude
            mapCenterLon = cachedLocation.longitude
            locationObtained = true
        } else {
            Log.d("RutaMapa", "⏳ No hay ubicación en caché, obteniendo nueva...")
        }
    }

    // Observar la alerta de desobediencia y convertirla en notificación
    LaunchedEffect(mostrarAlertaDesobediencia, mensajeAlertaDesobediencia) {
        if (mostrarAlertaDesobediencia && mensajeAlertaDesobediencia != null) {
            securityMessage = mensajeAlertaDesobediencia ?: ""
            showSecurityAlert = true
            viewModel.cerrarAlertaDesobediencia()
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

    // 🔥 MEJORADO: LocationTracker con actualización de caché
    LocationTracker { lat, lon ->
        userLat.value = lat
        userLon.value = lon

        // 🔥 Actualizar caché cuando hay nueva ubicación GPS
        locationManager.updateLocation(lat, lon, "")

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
                    zonasPeligrosas = zonasPeligrosas,
                    mostrarZonasPeligrosas = mostrarZonasPeligrosas,
                    viewModel = viewModel,
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

                        transportMessage = context.getString(R.string.transport_mode_label, getModeDisplayName(mode, context))
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
                    MapControlButton(
                        icon = Icons.Default.Add,
                        onClick = { zoomInTrigger++ }
                    )

                    MapControlButton(
                        icon = Icons.Default.Remove,
                        onClick = { zoomOutTrigger++ }
                    )

                    MapControlButton(
                        icon = Icons.Default.MyLocation,
                        onClick = {
                            mapCenterLat = userLat.value
                            mapCenterLon = userLon.value
                            recenterTrigger++
                        }
                    )

                    // 🆕 BOTÓN TOGGLE ZONAS PELIGROSAS
                    if (zonasPeligrosas.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        MapControlButton(
                            icon = if (mostrarZonasPeligrosas) {
                                Icons.Default.Visibility
                            } else {
                                Icons.Default.VisibilityOff
                            },
                            onClick = { viewModel.toggleMostrarZonas() },
                            badge = zonasPeligrosas.size.toString()
                        )
                    }
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
                                    Icon(Icons.Default.Check, contentDescription = stringResource(R.string.finish), tint = Color.White)
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.finish), color = Color.White)
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
                                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel), tint = Color.White)
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.cancel), color = Color.White)
                                }
                            }
                        }
                    }
                }

                // SELECTOR DE RUTAS CON SEGURIDAD
                if (showRouteSelector && alternativeRoutes.isNotEmpty()) {
                    // 🆕 Obtener estados adicionales del ViewModel
                    val isRegenerating by viewModel.isRegeneratingRoutes
                    val rutasGeneradasEvitandoZonas by viewModel.rutasGeneradasEvitandoZonas

                    RouteAlternativesDialogWithSecurity(
                        alternatives = alternativeRoutes,
                        validacionSeguridad = validacionSeguridad,
                        transportMode = selectedTransportMode,
                        isRegenerating = isRegenerating,
                        rutasGeneradasEvitandoZonas = rutasGeneradasEvitandoZonas,
                        onSelectRoute = { alternative ->
                            viewModel.selectRouteAlternative(
                                alternative = alternative,
                                token = token,
                                ubicacionId = selectedLocationId,
                                transporteTexto = selectedTransportMode
                            )

                            showRouteInfo = true
                            routeDistance = "${(alternative.distance / 1000).roundToInt()} km"
                            routeDuration = "${(alternative.duration / 60).roundToInt()} min"

                            transportMessage = context.getString(R.string.route_selected_message, alternative.displayName)
                            showTransportMessage = true
                        },
                        onRegenerarEvitandoZonas = {
                            selectedLocation?.let { destination ->
                                val startPoint = Pair(userLat.value, userLon.value)
                                val endPoint = Pair(destination.latitud, destination.longitud)

                                viewModel.regenerarRutasEvitandoZonasPeligrosas(
                                    start = startPoint,
                                    end = endPoint,
                                    token = token,
                                    ubicacionId = selectedLocationId,
                                    transporteTexto = selectedTransportMode
                                )
                            }
                        },
                        // 🚀 NUEVO: Agregar callback para guardar zonas públicas
                        onSavePublicZone = { zonaId: Int ->
                            viewModel.adoptarZonaPublica(
                                zonaId = zonaId,
                                token = token,
                                onSuccess = {
                                    viewModel.cargarZonasPeligrosas(token)
                                    viewModel.revalidarRutasActuales(token, selectedLocationId)
                                }
                            )
                        },
                        onDismiss = {
                            viewModel.hideRouteSelector()
                            viewModel.resetRegeneracionZonas()
                        }
                    )
                }

                // ADVERTENCIA DE RUTA INSEGURA
                if (mostrarAdvertenciaSeguridad) {
                    val rutaPendiente = alternativeRoutes.find {
                        it.esSegura == false && (it.nivelRiesgo ?: 0) >= 3
                    }

                    AlertDialog(
                        onDismissRequest = { viewModel.rechazarRutaInsegura() },
                        icon = {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier.size(48.dp)
                            )
                        },
                        title = {
                            Text(
                                stringResource(R.string.security_warning_title),
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.Red,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Column(
                                modifier = Modifier.verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    rutaPendiente?.mensajeSeguridad ?: stringResource(R.string.route_safe_avoiding),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                rutaPendiente?.zonasDetectadas?.let { zonas ->
                                    if (zonas.isNotEmpty()) {
                                        Text(
                                            stringResource(R.string.detected_zones_label),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))

                                        zonas.forEach { zona ->
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = getNivelPeligroColor(zona.nivelPeligro)
                                                )
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Text(
                                                        zona.nombre,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp
                                                    )
                                                    Text(
                                                        stringResource(R.string.risk_level_label, zona.nivelPeligro),
                                                        fontSize = 12.sp
                                                    )
                                                    Text(
                                                        stringResource(R.string.affects_route_percentage, zona.porcentajeRuta.toInt()),
                                                        fontSize = 12.sp,
                                                        fontStyle = FontStyle.Italic
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    stringResource(R.string.continue_route_question),
                                    fontStyle = FontStyle.Italic,
                                    fontSize = 13.sp
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.aceptarRiesgoRutaInsegura(
                                        token,
                                        selectedLocationId,
                                        selectedTransportMode
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Red
                                )
                            ) {
                                Text(stringResource(R.string.accept_risk_continue))
                            }
                        },
                        dismissButton = {
                            OutlinedButton(
                                onClick = { viewModel.rechazarRutaInsegura() }
                            ) {
                                Text(stringResource(R.string.cancel))
                            }
                        }
                    )
                }

                RutasBottomButtons(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    selectedTransportMode = selectedTransportMode,
                    showDestinationReached = destinoAlcanzado,
                    destinationMessage = stringResource(R.string.eta_arriving),
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

                                transportMessage = context.getString(R.string.calculating_alternatives)
                                showTransportMessage = true
                            } else {
                                transportMessage = context.getString(R.string.user_location_unavailable)
                                showTransportMessage = true
                            }
                        } ?: run {
                            transportMessage = context.getString(R.string.select_destination_first)
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
                // 🔥 Pantalla de carga mientras obtiene ubicación
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        stringResource(R.string.map_getting_location),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.map_getting_location_long),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }

                GetCurrentLocation(
                    onLocationResult = { lat, lon ->
                        userLat.value = lat
                        userLon.value = lon
                        locationObtained = true

                        // 🔥 Guardar en caché la primera ubicación obtenida
                        locationManager.updateLocation(lat, lon, "")
                        Log.d("RutaMapa", "✅ Primera ubicación obtenida y guardada en caché")
                    },
                    onError = {
                        Log.e("RutaMapa", "Error de ubicación")
                        showGpsButton = true
                    },
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