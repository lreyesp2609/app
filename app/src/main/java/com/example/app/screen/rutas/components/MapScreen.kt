package com.example.app.screen.rutas.components

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.app.models.RouteAlternative
import com.example.app.models.UbicacionUsuarioCreate
import com.example.app.models.ZonaGuardada
import com.example.app.models.ZonaPeligrosaCreate
import com.example.app.screen.mapa.GetCurrentLocation
import com.example.app.screen.mapa.GpsEnableButton
import com.example.app.screen.mapa.OpenStreetMap
import com.example.app.network.NominatimClient
import com.example.app.network.RetrofitClient
import com.example.app.screen.components.AppBackButton
import com.example.app.screen.components.AppButton
import com.example.app.screen.components.AppTextField
import com.example.app.screen.mapa.MapControlButton
import com.example.app.utils.DialogoCrearZonaPeligrosa
import com.example.app.utils.LocationManager
import com.example.app.utils.SessionManager
import com.example.app.viewmodel.MapViewModel
import com.example.app.viewmodel.MapViewModelFactory
import com.example.app.viewmodel.NotificationViewModel
import com.example.app.viewmodel.UbicacionesViewModel
import com.example.app.viewmodel.UbicacionesViewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun MapScreen(
    navController: NavController,
    defaultLat: Double = 0.0,
    defaultLon: Double = 0.0,
    onConfirmClick: () -> Unit = {},
    mapViewModel: MapViewModel = viewModel(factory = MapViewModelFactory()),
    notificationViewModel: NotificationViewModel
) {
    val context = LocalContext.current
    val locationManager = remember { LocationManager.getInstance() }
    val sessionManager = remember { SessionManager.getInstance(context) }
    val token = sessionManager.getAccessToken() ?: return

    var currentLat by remember { mutableStateOf(0.0) }
    var currentLon by remember { mutableStateOf(0.0) }
    var locationObtained by remember { mutableStateOf(false) }
    var showGpsButton by remember { mutableStateOf(false) }

    var currentAddress by remember { mutableStateOf("") }
    var selectedAddress by remember { mutableStateOf("") }

    var recenterTrigger by remember { mutableStateOf(0) }
    var job by remember { mutableStateOf<Job?>(null) }

    var mapCenterLat by remember { mutableStateOf(0.0) }
    var mapCenterLon by remember { mutableStateOf(0.0) }
    var locationName by rememberSaveable { mutableStateOf("") }

    var zoomInTrigger by remember { mutableStateOf(0) }
    var zoomOutTrigger by remember { mutableStateOf(0) }

    var showLocationCards by remember { mutableStateOf(true) }

    // Estados para zonas peligrosas
    var mostrarDialogoZona by remember { mutableStateOf(false) }
    var coordenadasZonaSeleccionada by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var radioPreview by remember { mutableStateOf(200) }
    var zonasCreadas by remember { mutableStateOf<List<ZonaGuardada>>(emptyList()) }

    val ubicacionesViewModel: UbicacionesViewModel = viewModel(
        factory = UbicacionesViewModelFactory(token)
    )
    val scope = rememberCoroutineScope()

    // Estados del MapViewModel para seguridad
    val alternativeRoutes by mapViewModel.alternativeRoutes
    val showRouteSelector by mapViewModel.showRouteSelector
    val mostrarAdvertenciaSeguridad by mapViewModel.mostrarAdvertenciaSeguridad
    val validacionSeguridad by mapViewModel.validacionSeguridad

    // 🆕 NUEVOS ESTADOS PARA REGENERACIÓN
    val isRegeneratingRoutes by mapViewModel.isRegeneratingRoutes
    val rutasGeneradasEvitandoZonas by mapViewModel.rutasGeneradasEvitandoZonas

    var mostrarZonasPeligrosas by remember { mutableStateOf(true) }
    var cargandoZonas by remember { mutableStateOf(false) }

    var showMapHelp by remember { mutableStateOf(false) }
    var showGestureHint by remember { mutableStateOf(false) }


    LaunchedEffect(currentLat, currentLon) {
        if (currentLat != 0.0 && currentLon != 0.0 && mapCenterLat == 0.0 && mapCenterLon == 0.0) {
            Log.d("MapScreen", "🎯 Inicializando mapCenter con ubicación actual: $currentLat, $currentLon")
            mapCenterLat = currentLat
            mapCenterLon = currentLon
        }
    }

    // Verificar si debe mostrar el tutorial
    LaunchedEffect(Unit) {
        val sharedPrefs = context.getSharedPreferences("map_prefs", Context.MODE_PRIVATE)
        showMapHelp = !sharedPrefs.getBoolean("map_help_seen", false)
    }

    // Mostrar hint de gesto si no ha marcado zonas
    LaunchedEffect(locationObtained, zonasCreadas.size) {
        if (locationObtained && zonasCreadas.isEmpty()) {
            delay(3000) // Esperar 3 segundos
            showGestureHint = true
        } else {
            showGestureHint = false
        }
    }

    // Función para marcar el tutorial como visto
    fun dismissMapHelp() {
        val sharedPrefs = context.getSharedPreferences("map_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("map_help_seen", true).apply()
        showMapHelp = false
    }

    // Cargar desde caché
    LaunchedEffect(Unit) {
        val cachedLocation = locationManager.getLastKnownLocation()
        if (cachedLocation != null) {
            val ageSeconds = (System.currentTimeMillis() - cachedLocation.timestamp) / 1000
            Log.d("MapScreen", "⚡ Usando ubicación en caché (${ageSeconds}s de antigüedad)")

            currentLat = cachedLocation.latitude
            currentLon = cachedLocation.longitude
            currentAddress = cachedLocation.address
            selectedAddress = cachedLocation.address
            mapCenterLat = cachedLocation.latitude
            mapCenterLon = cachedLocation.longitude
            locationObtained = true
        } else {
            Log.d("MapScreen", "⏳ No hay ubicación en caché, obteniendo nueva...")
        }
    }

    LaunchedEffect(token) {
        if (token.isNotEmpty()) {
            cargandoZonas = true
            scope.launch {
                try {
                    val response = RetrofitClient.rutasApiService.obtenerMisZonasPeligrosas("Bearer $token")

                    // 🔥 CORRECCIÓN: Usar los nombres correctos de las propiedades
                    zonasCreadas = response.mapNotNull { zona ->
                        // Validar que tenga las coordenadas necesarias
                        val coordenadas = zona.poligono?.firstOrNull()
                        if (coordenadas != null) {
                            ZonaGuardada(
                                lat = coordenadas.lat,
                                lon = coordenadas.lon,
                                radio = zona.radioMetros ?: 200,
                                nombre = zona.nombre,
                                nivel = zona.nivelPeligro
                            )
                        } else {
                            Log.w("MapScreen", "Zona '${zona.nombre}' sin coordenadas, ignorando")
                            null
                        }
                    }

                    Log.d("MapScreen", "✅ ${zonasCreadas.size} zonas cargadas desde el backend")

                    if (zonasCreadas.isNotEmpty()) {
                        notificationViewModel.showSuccess("${zonasCreadas.size} zonas peligrosas cargadas")
                    }
                } catch (e: Exception) {
                    Log.e("MapScreen", "Error cargando zonas: ${e.message}", e)
                    notificationViewModel.showError("No se pudieron cargar las zonas peligrosas")
                } finally {
                    cargandoZonas = false
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            locationObtained -> {
                Box(modifier = Modifier.fillMaxSize()) {

                    OpenStreetMap(
                        latitude = currentLat,
                        longitude = currentLon,
                        showUserLocation = true,
                        recenterTrigger = recenterTrigger,
                        zoomInTrigger = zoomInTrigger,
                        zoomOutTrigger = zoomOutTrigger,
                        modifier = Modifier.fillMaxSize(),
                        centerLat = mapCenterLat,
                        centerLon = mapCenterLon,
                        onLocationSelected = { lat, lon ->
                            mapCenterLat = lat
                            mapCenterLon = lon
                            job?.cancel()
                            job = scope.launch {
                                delay(500)
                                try {
                                    val response = NominatimClient.apiService.reverseGeocode(
                                        lat = lat,
                                        lon = lon
                                    )
                                    selectedAddress = response.display_name ?: ""
                                } catch (e: Exception) {
                                    selectedAddress = "Error obteniendo dirección"
                                }
                            }
                        },
                        onLocationLongPress = { lat, lon ->
                            coordenadasZonaSeleccionada = Pair(lat, lon)
                            mostrarDialogoZona = true
                            radioPreview = 200
                        },
                        zonaPreviewLat = if (mostrarDialogoZona) coordenadasZonaSeleccionada?.first else null,
                        zonaPreviewLon = if (mostrarDialogoZona) coordenadasZonaSeleccionada?.second else null,
                        zonaPreviewRadio = if (mostrarDialogoZona) radioPreview else null,
                        zonasGuardadas = if (mostrarZonasPeligrosas) zonasCreadas else emptyList()
                    )


                    AppBackButton(
                        navController = navController,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .statusBarsPadding()
                            .padding(16.dp),
                        backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    )

                    IconButton(
                        onClick = { showLocationCards = !showLocationCards },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(16.dp)
                            .size(48.dp)
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                                RoundedCornerShape(12.dp)
                            )
                    ) {
                        Icon(
                            imageVector = if (showLocationCards) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showLocationCards) "Ocultar info" else "Mostrar info",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp)
                            .offset(y = 40.dp), // 🆕 Baja todos los botones 40dp
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
                                // ✅ Usar currentLat/currentLon en lugar de userLat/userLon
                                mapCenterLat = currentLat
                                mapCenterLon = currentLon
                                recenterTrigger++

                                Log.d("MapScreen", "🎯 Botón Mi Ubicación presionado: $currentLat, $currentLon")
                            }
                        )

                        if (zonasCreadas.isNotEmpty()) {
                            MapControlButton(
                                icon = if (mostrarZonasPeligrosas) {
                                    Icons.Default.Visibility
                                } else {
                                    Icons.Default.VisibilityOff
                                },
                                onClick = {
                                    mostrarZonasPeligrosas = !mostrarZonasPeligrosas
                                    notificationViewModel.showInfo(
                                        if (mostrarZonasPeligrosas) "Zonas peligrosas visibles"
                                        else "Zonas peligrosas ocultas"
                                    )
                                },
                                badge = zonasCreadas.size.toString()
                            )
                        }
                    }


                    // En tu MapScreen, reemplaza la sección de AnimatedVisibility con esto:

                    AnimatedVisibility(
                        visible = showLocationCards,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .statusBarsPadding()
                                .padding(
                                    top = 80.dp,
                                    start = 16.dp,
                                    end = 16.dp
                                ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (showMapHelp) {
                                MapHelpBannerIntegrated(
                                    onDismiss = { dismissMapHelp() }
                                )
                            }

                            // 🆕 REEMPLAZAR CompactLocationCard con SearchLocationCard
                            SearchLocationCard(
                                currentAddress = currentAddress,
                                userLat = currentLat,  // 🔥 Pasar ubicación del usuario
                                userLon = currentLon,  // 🔥 Pasar ubicación del usuario
                                onSearchResult = { lat, lon, address ->
                                    // ✅ ACTUALIZAR las coordenadas de referencia
                                    mapCenterLat = lat
                                    mapCenterLon = lon
                                    selectedAddress = address

                                    // ✅ IMPORTANTE: Actualizar userLat y userLon para que el botón
                                    // "Mi ubicación" siga funcionando correctamente
                                    // (No sobrescribir, solo si quieres que la búsqueda sea el nuevo centro)

                                    // ✅ FORZAR recentrado del mapa (esto mueve el mapa físicamente)
                                    recenterTrigger++

                                    // Mostrar notificación
                                    notificationViewModel.showSuccess("📍 Ubicación encontrada")
                                }
                            )

                            // Mantener la card de ubicación seleccionada
                            if (selectedAddress.isNotEmpty() && selectedAddress != currentAddress) {
                                CompactLocationCard(
                                    title = "Ubicación seleccionada",
                                    location = selectedAddress,
                                    icon = Icons.Default.LocationOn,
                                    iconColor = Color(0xFFEF4444)
                                )
                            }
                        }
                    }

                    BottomConfirmPanel(
                        selectedLocation = selectedAddress,
                        modifier = Modifier.align(Alignment.BottomCenter),
                        locationName = locationName,
                        onLocationNameChange = { locationName = it },
                        onConfirmClick = {
                            if (locationName.isNotBlank() && selectedAddress.isNotBlank()) {
                                val nuevaUbicacion = UbicacionUsuarioCreate(
                                    nombre = locationName,
                                    latitud = mapCenterLat,
                                    longitud = mapCenterLon,
                                    direccion_completa = selectedAddress
                                )
                                ubicacionesViewModel.crearUbicacion(nuevaUbicacion) { success, error ->
                                    if (success) {
                                        notificationViewModel.showSuccess("Destino guardado correctamente")
                                        navController.popBackStack()
                                    } else {
                                        notificationViewModel.showError(error ?: "Error guardando el destino")
                                    }
                                }
                            }
                        }
                    )
                }
            }

            else -> {
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
                        "Obteniendo ubicación...",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Esto puede tardar unos segundos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }

                GetCurrentLocation(
                    onLocationResult = { lat, lon ->
                        currentLat = lat
                        currentLon = lon
                        locationObtained = true

                        scope.launch {
                            try {
                                val response = NominatimClient.apiService.reverseGeocode(
                                    lat = lat,
                                    lon = lon
                                )
                                val address = response.display_name ?: ""
                                currentAddress = address
                                selectedAddress = address
                                mapCenterLat = lat
                                mapCenterLon = lon
                                locationManager.updateLocation(lat, lon, address)

                                Log.d("MapScreen", "✅ Nueva ubicación obtenida y guardada en caché")
                            } catch (e: Exception) {
                                notificationViewModel.showError("Error creando zona peligrosa: ${e.message}")
                            }
                        }
                    },
                    onError = { error ->
                        Log.e("MapScreen", "Error de ubicación: $error")
                    },
                    onGpsDisabled = { showGpsButton = true }
                )
            }
        }

        // DIÁLOGO PARA CREAR ZONA PELIGROSA
        if (showRouteSelector) {
            RouteSelectorDialog(
                alternativeRoutes = alternativeRoutes,
                validacionSeguridad = validacionSeguridad,
                isRegenerating = isRegeneratingRoutes,
                rutasGeneradasEvitandoZonas = rutasGeneradasEvitandoZonas,
                onRouteSelected = { route ->
                    // Obtener ubicacionId y transporte del contexto
                    val ubicacionId = 1 // Cambiar según tu lógica
                    val transporteTexto = "walking" // Cambiar según modo seleccionado

                    mapViewModel.selectRouteAlternative(
                        route,
                        token,
                        ubicacionId,
                        transporteTexto
                    )
                },
                onRegenerarEvitandoZonas = {
                    // Regenerar rutas evitando zonas peligrosas
                    val ubicacionId = 1 // Cambiar según tu lógica
                    val transporteTexto = "walking"

                    mapViewModel.regenerarRutasEvitandoZonasPeligrosas(
                        start = Pair(currentLat, currentLon),
                        end = Pair(mapCenterLat, mapCenterLon),
                        token = token,
                        ubicacionId = ubicacionId,
                        transporteTexto = transporteTexto
                    )
                },
                onDismiss = {
                    mapViewModel.hideRouteSelector()
                    // Reset estado de regeneración al cerrar
                    mapViewModel.resetRegeneracionZonas()
                }
            )
        }

        // 🔥 DIÁLOGO PARA CREAR ZONA PELIGROSA
        if (mostrarDialogoZona && coordenadasZonaSeleccionada != null) {
            DialogoCrearZonaPeligrosa(
                coordenadas = coordenadasZonaSeleccionada!!,
                onConfirmar = { nombre, radio, nivel, tipo, notas ->
                    scope.launch {
                        try {
                            val request = ZonaPeligrosaCreate(
                                nombre = nombre,
                                lat = coordenadasZonaSeleccionada!!.first,
                                lon = coordenadasZonaSeleccionada!!.second,
                                radioMetros = radio,
                                nivelPeligro = nivel,
                                tipo = tipo,
                                notas = notas
                            )

                            val response = RetrofitClient.rutasApiService.marcarZonaPeligrosa(
                                token = "Bearer $token",
                                zona = request
                            )

                            notificationViewModel.showSuccess(
                                "Zona marcada correctamente: ${response.nombre}"
                            )

                            Log.d("MapScreen", "Zona creada: ID=${response.id}, Radio=${radio}m")

                            // 🔥 AGREGAR A LA LISTA LOCAL
                            zonasCreadas = zonasCreadas + ZonaGuardada(
                                lat = coordenadasZonaSeleccionada!!.first,
                                lon = coordenadasZonaSeleccionada!!.second,
                                radio = radio,
                                nombre = nombre,
                                nivel = nivel
                            )

                            mostrarDialogoZona = false
                            radioPreview = 200

                        } catch (e: Exception) {
                            notificationViewModel.showError(
                                "Error al crear la zona: ${e.message}"
                            )
                            Log.e("MapScreen", "Error creando zona", e)
                        }
                    }
                },
                onCancelar = {
                    mostrarDialogoZona = false
                    radioPreview = 200
                },
                onRadioChanged = { nuevoRadio ->
                    radioPreview = nuevoRadio
                }
            )
        }

        // SELECTOR DE RUTAS (resto del código igual)
        if (showRouteSelector) {
            AlertDialog(
                onDismissRequest = { mapViewModel.hideRouteSelector() },
                title = {
                    Text(
                        "Selecciona tu ruta",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        validacionSeguridad?.advertenciaGeneral?.let { advertencia ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFFFEBEE)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color.Red,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        advertencia,
                                        fontSize = 13.sp,
                                        color = Color(0xFFD32F2F)
                                    )
                                }
                            }
                        }

                        alternativeRoutes.forEach { route ->
                            RutaCard(
                                route = route,
                                onClick = {
                                    val ubicacionId = 1
                                    val transporteTexto = "walking"

                                    mapViewModel.selectRouteAlternative(
                                        route,
                                        token,
                                        ubicacionId,
                                        transporteTexto
                                    )
                                }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { mapViewModel.hideRouteSelector() }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        if (showGpsButton) {
            GpsEnableButton(onEnableGps = { showGpsButton = false })
        }
    }
}

@Composable
fun RutaCard(route: RouteAlternative, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (route.isRecommended) Color(0xFFE8F5E9) else Color.White
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    route.displayName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Badge ML
                    if (route.isRecommended) {
                        Badge(
                            containerColor = Color(0xFF4CAF50)
                        ) {
                            Text("🤖 ML", fontSize = 10.sp, color = Color.White)
                        }
                    }

                    // 🆕 Badge de Seguridad
                    route.esSegura?.let { esSegura ->
                        Badge(
                            containerColor = if (esSegura) Color(0xFF4CAF50) else Color(0xFFF44336)
                        ) {
                            Text(
                                if (esSegura) "SEGURA" else "RIESGO",
                                fontSize = 10.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Distancia y duración
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "📏 ${(route.distance / 1000).roundToInt()} km",
                    fontSize = 14.sp
                )
                Text(
                    "⏱ ${(route.duration / 60).toInt()} min",
                    fontSize = 14.sp
                )
            }

            // 🆕 Mensaje de seguridad si existe
            route.mensajeSeguridad?.let { mensaje ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    mensaje,
                    fontSize = 12.sp,
                    color = Color(0xFFF44336),
                    fontStyle = FontStyle.Italic
                )
            }

            // 🆕 Número de zonas detectadas
            route.zonasDetectadas?.let { zonas ->
                if (zonas.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${zonas.size} zona(s) de riesgo detectada(s)",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun CompactLocationCard(
    title: String,
    location: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(iconColor.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (location.isNotEmpty()) location else "Selecciona una ubicación",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (location.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(iconColor, CircleShape)
                )
            }
        }
    }
}

@Composable
fun BottomConfirmPanel(
    selectedLocation: String,
    locationName: String,
    onLocationNameChange: (String) -> Unit,
    onConfirmClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canConfirm = selectedLocation.isNotEmpty() && locationName.trim().isNotEmpty()

    AnimatedVisibility(
        visible = selectedLocation.isNotEmpty(),
        enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding(),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Nombra este destino",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                AppTextField(
                    value = locationName,
                    onValueChange = { newValue ->
                        if (newValue.length <= 100) onLocationNameChange(newValue)
                    },
                    label = "Nombre del destino",
                    placeholder = "ej. Casa, Trabajo, Gimnasio...",
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = MaterialTheme.colorScheme.primary,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    )
                )

                Text(
                    text = "${locationName.length}/100 caracteres",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                AppButton(
                    text = "Guardar destino",
                    icon = Icons.Default.Check,
                    onClick = onConfirmClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canConfirm,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                )
            }
        }
    }
}