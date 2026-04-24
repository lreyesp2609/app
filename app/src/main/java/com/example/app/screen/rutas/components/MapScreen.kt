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
import androidx.compose.foundation.isSystemInDarkTheme
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
import com.example.app.ui.theme.SecurityColors
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
    mapViewModel: MapViewModel = viewModel(factory = MapViewModelFactory(LocalContext.current)),
    notificationViewModel: NotificationViewModel
) {
    val context = LocalContext.current
    val locationManager = remember { LocationManager.getInstance() }
    val sessionManager = remember { SessionManager.getInstance(context) }
    val token = sessionManager.getAccessToken() ?: return
    val isDarkTheme = isSystemInDarkTheme()

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
        factory = UbicacionesViewModelFactory(context, token)
    )
    val scope = rememberCoroutineScope()

    // Estados del MapViewModel para seguridad
    val alternativeRoutes by mapViewModel.alternativeRoutes
    val showRouteSelector by mapViewModel.showRouteSelector
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

    LaunchedEffect(Unit) {
        val sharedPrefs = context.getSharedPreferences("map_prefs", Context.MODE_PRIVATE)
        showMapHelp = !sharedPrefs.getBoolean("map_help_seen", false)
    }

    LaunchedEffect(locationObtained, zonasCreadas.size) {
        if (locationObtained && zonasCreadas.isEmpty()) {
            delay(3000)
            showGestureHint = true
        } else {
            showGestureHint = false
        }
    }

    fun dismissMapHelp() {
        val sharedPrefs = context.getSharedPreferences("map_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("map_help_seen", true).apply()
        showMapHelp = false
    }

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

                    zonasCreadas = response.mapNotNull { zona ->
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
                        notificationViewModel.showSuccess(
                            context.getString(com.example.app.R.string.zones_loaded_success, zonasCreadas.size)
                        )
                    }
                } catch (e: Exception) {
                    Log.e("MapScreen", "Error cargando zonas: ${e.message}", e)
                    notificationViewModel.showError(
                        context.getString(com.example.app.R.string.error_loading_zones)
                    )
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
                        isDarkTheme = isDarkTheme,          // <-- ADD THIS
                        onLocationSelected = { lat, lon ->
                            // ... existing code unchanged
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
                            contentDescription = if (showLocationCards) {
                                context.getString(com.example.app.R.string.map_hide_info)
                            } else {
                                context.getString(com.example.app.R.string.map_show_info)
                            },
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp)
                            .offset(y = 40.dp),
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
                                        if (mostrarZonasPeligrosas) context.getString(com.example.app.R.string.map_zones_visible)
                                        else context.getString(com.example.app.R.string.map_zones_hidden)
                                    )
                                },
                                badge = zonasCreadas.size.toString()
                            )
                        }
                    }

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

                            SearchLocationCard(
                                currentAddress = currentAddress,
                                userLat = currentLat,
                                userLon = currentLon,
                                onSearchResult = { lat, lon, address ->
                                    mapCenterLat = lat
                                    mapCenterLon = lon
                                    selectedAddress = address
                                    recenterTrigger++
                                    notificationViewModel.showSuccess(
                                        context.getString(com.example.app.R.string.map_search_success)
                                    )
                                }
                            )

                            if (selectedAddress.isNotEmpty() && selectedAddress != currentAddress) {
                                CompactLocationCard(
                                    title = context.getString(com.example.app.R.string.map_selected_location),
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
                                        notificationViewModel.showSuccess(
                                            context.getString(com.example.app.R.string.map_destination_saved)
                                        )
                                        navController.popBackStack()
                                    } else {
                                        notificationViewModel.showError(
                                            error ?: context.getString(com.example.app.R.string.map_save_error)
                                        )
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
                        context.getString(com.example.app.R.string.map_getting_location),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        context.getString(com.example.app.R.string.map_getting_location_long),
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
                                notificationViewModel.showError(
                                    context.getString(com.example.app.R.string.map_location_error, e.message ?: "")
                                )
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

        /*
        // ═══════════════════════════════════════════════════════════════════
        // 🚀 ÚNICO DIÁLOGO DE SELECCIÓN DE RUTAS (sin duplicados)
        // ═══════════════════════════════════════════════════════════════════
        if (showRouteSelector) {
            RouteSelectorDialog(
                alternativeRoutes = alternativeRoutes,
                validacionSeguridad = validacionSeguridad,
                isRegenerating = isRegeneratingRoutes,
                rutasGeneradasEvitandoZonas = rutasGeneradasEvitandoZonas,
                onRouteSelected = { route ->
                    val ubicacionId = 1
                    val transporteTexto = "walking"

                    mapViewModel.selectRouteAlternative(
                        route,
                        token,
                        ubicacionId,
                        transporteTexto
                    )
                },
                onRegenerarEvitandoZonas = {
                    val ubicacionId = 1
                    val transporteTexto = "walking"

                    mapViewModel.regenerarRutasEvitandoZonasPeligrosas(
                        start = Pair(currentLat, currentLon),
                        end = Pair(mapCenterLat, mapCenterLon),
                        token = token,
                        ubicacionId = ubicacionId,
                        transporteTexto = transporteTexto
                    )
                },
                // 🔥 CALLBACK MEJORADO: Ahora re-valida rutas después de guardar
                onSavePublicZone = { zonaId ->
                    scope.launch {
                        try {
                            val zonaAdoptada = RetrofitClient.rutasApiService.adoptarZonaSugerida(
                                token = "Bearer $token",
                                zonaId = zonaId
                            )

                            notificationViewModel.showSuccess("✅ Zona guardada: ${zonaAdoptada.nombre}")

                            // 1️⃣ Agregar a la lista local
                            val centro = zonaAdoptada.poligono.firstOrNull()
                            if (centro != null) {
                                zonasCreadas = zonasCreadas + ZonaGuardada(
                                    lat = centro.lat,
                                    lon = centro.lon,
                                    radio = zonaAdoptada.radioMetros ?: 200,
                                    nombre = zonaAdoptada.nombre,
                                    nivel = zonaAdoptada.nivelPeligro
                                )
                            }

                            // 2️⃣ 🔥 RE-VALIDAR RUTAS PARA ACTUALIZAR ESTADO
                            val ubicacionId = 1  // O el ID real de la ubicación
                            mapViewModel.revalidarRutasActuales(token, ubicacionId)

                        } catch (e: Exception) {
                            val error = when {
                                e.message?.contains("Ya tienes una zona") == true ->
                                    "Ya tienes una zona con ese nombre"
                                else ->
                                    "Error al guardar zona: ${e.message}"
                            }
                            notificationViewModel.showError(error)
                        }
                    }
                },
                onDismiss = {
                    mapViewModel.hideRouteSelector()
                    mapViewModel.resetRegeneracionZonas()
                }
            )
        }*/
        // ═══════════════════════════════════════════════════════════════════
        // DIÁLOGO PARA CREAR ZONA PELIGROSA
        // ═══════════════════════════════════════════════════════════════════

        
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
                                context.getString(com.example.app.R.string.zone_created_success, response.nombre)
                            )

                            Log.d("MapScreen", "Zona creada: ID=${response.id}, Radio=${radio}m")

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
                                context.getString(com.example.app.R.string.error_creating_zone, e.message ?: "")
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

        if (showGpsButton) {
            GpsEnableButton(onEnableGps = { showGpsButton = false })
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// COMPONENTES AUXILIARES (sin cambios)
// ═══════════════════════════════════════════════════════════════════

@Composable
fun CompactLocationCard(
    title: String,
    location: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
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
                    text = if (location.isNotEmpty()) location else context.getString(com.example.app.R.string.map_select_location),
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
    val context = LocalContext.current
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
                        text = context.getString(com.example.app.R.string.map_naming_destination),
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
                    label = context.getString(com.example.app.R.string.map_destination_name_label),
                    placeholder = context.getString(com.example.app.R.string.map_destination_placeholder),
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = MaterialTheme.colorScheme.primary,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    )
                )

                Text(
                    text = context.getString(com.example.app.R.string.map_characters_count, locationName.length),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                AppButton(
                    text = context.getString(com.example.app.R.string.map_save_destination),
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