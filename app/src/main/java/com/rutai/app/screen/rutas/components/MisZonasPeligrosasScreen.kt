package com.rutai.app.screen.rutas.components

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.RadioButtonChecked
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.rutai.app.R
import com.rutai.app.models.ZonaGuardada
import com.rutai.app.models.ZonaPeligrosaCreate
import com.rutai.app.models.ZonaSugerida
import com.rutai.app.network.RetrofitClient
import com.rutai.app.screen.components.AppBackButton
import com.rutai.app.screen.mapa.GetCurrentLocation
import com.rutai.app.screen.mapa.GpsEnableButton
import com.rutai.app.screen.mapa.MapControlButton
import com.rutai.app.screen.mapa.OpenStreetMap
import com.rutai.app.ui.theme.AppColors
import com.rutai.app.ui.theme.DangerLevelColors
import com.rutai.app.utils.DialogoCrearZonaPeligrosa
import com.rutai.app.utils.LocationManager
import com.rutai.app.utils.SessionManager
import com.rutai.app.viewmodel.MapViewModel
import com.rutai.app.viewmodel.NotificationViewModel
import com.rutai.app.viewmodel.ZonasSugeridasViewModel
import kotlinx.coroutines.launch

// ─── MisZonasPeligrosasScreen ──────────────────────────────────────────────────

@Composable
fun MisZonasPeligrosasScreen(
    navController: NavController,
    notificationViewModel: NotificationViewModel,
    mapViewModel: MapViewModel
) {

    val context = LocalContext.current
    val locationManager = remember { LocationManager.getInstance() }
    val sessionManager = remember { SessionManager.getInstance(context) }
    val token = sessionManager.getAccessToken() ?: return

    var zonaSugeridaPreview by remember { mutableStateOf<ZonaSugerida?>(null) }
    val zonasSugeridasVM = remember { ZonasSugeridasViewModel(token) }

    var currentLat by remember { mutableStateOf(0.0) }
    var currentLon by remember { mutableStateOf(0.0) }
    var locationObtained by remember { mutableStateOf(false) }
    var showGpsButton by remember { mutableStateOf(false) }

    var recenterTrigger by remember { mutableStateOf(0) }
    var zoomInTrigger by remember { mutableStateOf(0) }
    var zoomOutTrigger by remember { mutableStateOf(0) }

    var zonasCreadas by remember { mutableStateOf<List<ZonaGuardada>>(emptyList()) }
    var mostrarDialogoZona by remember { mutableStateOf(false) }
    var coordenadasZonaSeleccionada by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var radioPreview by remember { mutableStateOf(200) }
    var mostrarZonasPeligrosas by remember { mutableStateOf(true) }
    var cargandoZonas by remember { mutableStateOf(false) }

    var zonaSeleccionadaParaEditar by remember { mutableStateOf<ZonaGuardada?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showBottomSheet by remember { mutableStateOf(false) }

    var mostrarDialogoSugerencias by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val isDarkTheme = isSystemInDarkTheme()

    var mapCenterLat by remember { mutableStateOf(currentLat) }
    var mapCenterLon by remember { mutableStateOf(currentLon) }

    LaunchedEffect(currentLat, currentLon) {
        if (currentLat != 0.0 && currentLon != 0.0) {
            mapCenterLat = currentLat
            mapCenterLon = currentLon
        }
    }

    LaunchedEffect(locationObtained) {
        if (locationObtained && currentLat != 0.0) {
            zonasSugeridasVM.verificarYCargarSugerencias(
                lat = currentLat,
                lon = currentLon,
                radioKm = 10.0f
            )
        }
    }

    fun flyToZona(lat: Double, lon: Double) {
        mapCenterLat = lat
        mapCenterLon = lon
        recenterTrigger++
    }

    LaunchedEffect(Unit) {
        val cachedLocation = locationManager.getLastKnownLocation()
        if (cachedLocation != null) {
            currentLat = cachedLocation.latitude
            currentLon = cachedLocation.longitude
            locationObtained = true
            Log.d("ZonasPeligrosas", "⚡ Ubicación en caché cargada")

            zonasSugeridasVM.verificarYCargarSugerencias(
                lat = currentLat,
                lon = currentLon,
                radioKm = 10.0f
            )
        } else {
            Log.d("ZonasPeligrosas", "⏳ Obteniendo nueva ubicación...")
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
                                nivel = zona.nivelPeligro,
                                id = zona.id
                            )
                        } else {
                            Log.w("ZonasPeligrosas", "Zona sin coordenadas: ${zona.nombre}")
                            null
                        }
                    }

                    Log.d("ZonasPeligrosas", "✅ ${zonasCreadas.size} zonas cargadas")

                    if (zonasCreadas.isNotEmpty()) {
                        notificationViewModel.showSuccess(context.getString(R.string.zones_loaded_success, zonasCreadas.size))
                    }
                } catch (e: Exception) {
                    Log.e("ZonasPeligrosas", "Error cargando zonas: ${e.message}", e)
                    notificationViewModel.showError(context.getString(R.string.error_loading_zones))
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
                        centerLat = mapCenterLat,
                        centerLon = mapCenterLon,
                        showUserLocation = true,
                        recenterTrigger = recenterTrigger,
                        zoomInTrigger = zoomInTrigger,
                        zoomOutTrigger = zoomOutTrigger,
                        modifier = Modifier.fillMaxSize(),
                        isDarkTheme = isDarkTheme,
                        onLocationSelected = { _, _ -> },
                        onLocationLongPress = { lat, lon ->
                            coordenadasZonaSeleccionada = Pair(lat, lon)
                            mostrarDialogoZona = true
                            radioPreview = 200
                        },
                        zonaPreviewLat = zonaSugeridaPreview?.zonaOriginal?.poligono?.firstOrNull()?.lat
                            ?: if (mostrarDialogoZona) coordenadasZonaSeleccionada?.first else null,
                        zonaPreviewLon = zonaSugeridaPreview?.zonaOriginal?.poligono?.firstOrNull()?.lon
                            ?: if (mostrarDialogoZona) coordenadasZonaSeleccionada?.second else null,
                        zonaPreviewRadio = zonaSugeridaPreview?.zonaOriginal?.radioMetros
                            ?: if (mostrarDialogoZona) radioPreview else null,
                        zonasGuardadas = if (mostrarZonasPeligrosas) zonasCreadas else emptyList(),
                        onZonaClick = { zona ->
                            zonaSeleccionadaParaEditar = zona
                            showBottomSheet = true
                        }
                    )

                    if (zonasSugeridasVM.mostrarSugerencias && zonasSugeridasVM.zonasSugeridas.isNotEmpty()) {
                        BannerZonasSugeridas(
                            zonasSugeridas = zonasSugeridasVM.zonasSugeridas,
                            onVerSugerencias = { mostrarDialogoSugerencias = true },
                            onDismiss = { zonasSugeridasVM.reset() },
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .statusBarsPadding()
                                .padding(top = 80.dp)
                        )
                    }

                    AppBackButton(
                        navController = navController,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .statusBarsPadding()
                            .padding(16.dp),
                        backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    )

                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(top = 16.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                shape = RoundedCornerShape(50.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            context.getString(R.string.my_zones_title),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (zonasCreadas.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(AppColors.Danger, CircleShape)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "${zonasCreadas.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp)
                            .offset(y = 40.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MapControlButton(icon = Icons.Default.Add, onClick = { zoomInTrigger++ })
                        MapControlButton(icon = Icons.Default.Remove, onClick = { zoomOutTrigger++ })
                        MapControlButton(icon = Icons.Default.MyLocation, onClick = { recenterTrigger++ })

                        if (zonasCreadas.isNotEmpty()) {
                            MapControlButton(
                                icon = if (mostrarZonasPeligrosas) Icons.Default.Visibility
                                else Icons.Default.VisibilityOff,
                                onClick = {
                                    mostrarZonasPeligrosas = !mostrarZonasPeligrosas
                                    notificationViewModel.showInfo(
                                        if (mostrarZonasPeligrosas) context.getString(R.string.map_zones_visible) else context.getString(R.string.map_zones_hidden)
                                    )
                                },
                                badge = zonasCreadas.size.toString()
                            )
                        }
                    }

                    BottomZonasPanel(
                        zonas = zonasCreadas,
                        isLoading = cargandoZonas,
                        onZonaClick = { zona ->
                            zonaSeleccionadaParaEditar = zona
                            showBottomSheet = true
                        },
                        modifier = Modifier.align(Alignment.BottomCenter)
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
                        context.getString(R.string.map_getting_location),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                GetCurrentLocation(
                    onLocationResult = { lat, lon ->
                        currentLat = lat
                        currentLon = lon
                        locationObtained = true
                        locationManager.updateLocation(lat, lon, "")
                        Log.d("ZonasPeligrosas", "✅ Ubicación obtenida")
                    },
                    onError = { error -> Log.e("ZonasPeligrosas", "Error: $error") },
                    onGpsDisabled = { showGpsButton = true }
                )
            }
        }

        if (mostrarDialogoSugerencias) {
            DialogoZonasSugeridas(
                zonas = zonasSugeridasVM.zonasSugeridas,
                onVerEnMapa = { lat, lon, zona ->
                    zonaSugeridaPreview = zona
                    flyToZona(lat, lon)
                    mostrarDialogoSugerencias = false
                },
                onAdoptar = { zonaId ->
                    zonaSugeridaPreview = null
                    zonasSugeridasVM.adoptarZona(
                        zonaId = zonaId,
                        onSuccess = { zonaAdoptada ->
                            val centro = zonaAdoptada.poligono.firstOrNull()
                            if (centro != null) {
                                zonasCreadas = zonasCreadas + ZonaGuardada(
                                    lat = centro.lat,
                                    lon = centro.lon,
                                    radio = zonaAdoptada.radioMetros ?: 200,
                                    nombre = zonaAdoptada.nombre,
                                    nivel = zonaAdoptada.nivelPeligro,
                                    id = zonaAdoptada.id
                                )
                                flyToZona(centro.lat, centro.lon)
                            }
                            notificationViewModel.showSuccess(context.getString(R.string.zone_saved) + ": ${zonaAdoptada.nombre}")
                        },
                        onError = { error -> notificationViewModel.showError(error) }
                    )
                },
                onDescartar = { zonaId ->
                    zonaSugeridaPreview = null
                    zonasSugeridasVM.descartarZona(zonaId)
                },
                onDismiss = {
                    mostrarDialogoSugerencias = false
                    zonaSugeridaPreview = null
                },
                isDarkTheme = isDarkTheme
            )
        }

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

                            notificationViewModel.showSuccess(context.getString(R.string.zone_created_success, response.nombre))

                            zonasCreadas = zonasCreadas + ZonaGuardada(
                                lat = coordenadasZonaSeleccionada!!.first,
                                lon = coordenadasZonaSeleccionada!!.second,
                                radio = radio,
                                nombre = nombre,
                                nivel = nivel,
                                id = response.id
                            )

                            mostrarDialogoZona = false
                            radioPreview = 200

                        } catch (e: Exception) {
                            notificationViewModel.showError(context.getString(R.string.error_creating_zone, e.message ?: ""))
                            Log.e("ZonasPeligrosas", "Error", e)
                        }
                    }
                },
                onCancelar = {
                    mostrarDialogoZona = false
                    radioPreview = 200
                },
                onRadioChanged = { nuevoRadio -> radioPreview = nuevoRadio }
            )
        }

        if (showBottomSheet && zonaSeleccionadaParaEditar != null) {
            ZonaDetailBottomSheet(
                zona = zonaSeleccionadaParaEditar!!,
                onDismiss = {
                    showBottomSheet = false
                    zonaSeleccionadaParaEditar = null
                },
                onEliminar = { showDeleteDialog = true },
                isDarkTheme = isDarkTheme
            )
        }

        if (showDeleteDialog && zonaSeleccionadaParaEditar != null) {
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
                title = { Text(context.getString(R.string.delete_zone_title), fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(context.getString(R.string.delete_zone_confirmation))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "\"${zonaSeleccionadaParaEditar!!.nombre}\"",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            context.getString(R.string.cannot_be_undone_warning),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                },
                confirmButton = {
                    val scope = rememberCoroutineScope()
                    Button(
                        onClick = {
                            val zonaId = zonaSeleccionadaParaEditar?.id

                            if (zonaId == null) {
                                notificationViewModel.showError(context.getString(R.string.error_zone_no_id))
                                showDeleteDialog = false
                                return@Button
                            }

                            scope.launch {
                                try {
                                    Log.d("ZonasScreen", "🗑️ Eliminando zona ID: $zonaId")

                                    val response = RetrofitClient.rutasApiService.eliminarZonaPeligrosa(
                                        token = "Bearer $token",
                                        zonaId = zonaId
                                    )

                                    if (response.isSuccessful) {
                                        zonasCreadas = zonasCreadas.filter { it.id != zonaId }
                                        notificationViewModel.showSuccess(context.getString(R.string.zone_deleted_success))
                                        mapViewModel.cargarZonasPeligrosas(token)
                                        showDeleteDialog = false
                                        showBottomSheet = false
                                        zonaSeleccionadaParaEditar = null
                                        Log.d("ZonasScreen", "✅ Zona eliminada correctamente")
                                    } else {
                                        val errorMsg = context.getString(R.string.generic_error, "${response.code()}: ${response.message()}")
                                        notificationViewModel.showError(errorMsg)
                                        Log.e("ZonasScreen", "❌ $errorMsg")
                                    }
                                } catch (e: Exception) {
                                    val errorMsg = e.message ?: "Error desconocido"
                                    notificationViewModel.showError(context.getString(R.string.generic_error, errorMsg))
                                    Log.e("ZonasScreen", "❌ Error: $errorMsg", e)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Danger)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(context.getString(R.string.delete), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text(context.getString(R.string.cancel)) }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp)
            )
        }

        if (showGpsButton) {
            GpsEnableButton(onEnableGps = { showGpsButton = false })
        }
    }
}

// ─── BottomZonasPanel ─────────────────────────────────────────────────────────

@Composable
fun BottomZonasPanel(
    zonas: List<ZonaGuardada>,
    isLoading: Boolean,
    onZonaClick: (ZonaGuardada) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp)
            .navigationBarsPadding(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.List,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        context.getString(R.string.registered_zones_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (zonas.isNotEmpty()) {
                    Text(
                        context.getString(R.string.zones_count_label, zonas.size),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                zonas.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            context.getString(R.string.no_zones_registered),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            context.getString(R.string.long_press_map_to_add),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items = zonas, key = { it.id ?: it.hashCode() }) { zona ->
                            ZonaListItem(zona = zona, onClick = { onZonaClick(zona) }, isDarkTheme = isDarkTheme)
                        }
                    }
                }
            }
        }
    }
}

// ─── ZonaListItem — color dinámico por nivel ──────────────────────────────────

@Composable
fun ZonaListItem(
    zona: ZonaGuardada,
    onClick: () -> Unit,
    isDarkTheme: Boolean
) {
    val context = LocalContext.current
    val nivelUI    = DangerLevelColors.clampNivel(zona.nivel)
    val nivelColor = DangerLevelColors.getColor(nivelUI, isDarkTheme)
    val nivelBg    = DangerLevelColors.getBackground(nivelUI, isDarkTheme)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = nivelBg),
        border = BorderStroke(1.dp, nivelColor.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(nivelColor.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = nivelColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    zona.nombre,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "${context.getString(R.string.radius_label_simple)}: ${zona.radio}${context.getString(R.string.meters_unit).first()} • ${DangerLevelColors.getNombreNivel(nivelUI, context)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ─── ZonaDetailBottomSheet — color dinámico por nivel ────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZonaDetailBottomSheet(
    zona: ZonaGuardada,
    onDismiss: () -> Unit,
    onEliminar: () -> Unit,
    isDarkTheme: Boolean
) {
    val context = LocalContext.current
    val nivelUI    = DangerLevelColors.clampNivel(zona.nivel)
    val nivelColor = DangerLevelColors.getColor(nivelUI, isDarkTheme)
    val nivelBg    = DangerLevelColors.getBackground(nivelUI, isDarkTheme)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(nivelBg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = nivelColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        zona.nombre,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        context.getString(R.string.danger_zone_label),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            DetailRow(
                icon = Icons.Default.RadioButtonChecked,
                label = context.getString(R.string.radius_label_simple),
                value = "${zona.radio} ${context.getString(R.string.meters_unit)}"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Nivel con nombre descriptivo y color — sin estrellas ni "/5"
            DetailRow(
                icon = Icons.Default.Warning,
                label = context.getString(R.string.risk_level_label_simple),
                value = context.getString(R.string.risk_level_format, DangerLevelColors.getNombreNivel(nivelUI, context), nivelUI)
            )

            Spacer(modifier = Modifier.height(12.dp))

            DetailRow(
                icon = Icons.Default.LocationOn,
                label = context.getString(R.string.coordinates_label),
                value = "${String.format("%.5f", zona.lat)}, ${String.format("%.5f", zona.lon)}"
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onEliminar,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Danger),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(context.getString(R.string.delete_zone_button), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

// ─── DetailRow ────────────────────────────────────────────────────────────────

@Composable
fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                value,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ─── BannerZonasSugeridas ─────────────────────────────────────────────────────

@Composable
fun BannerZonasSugeridas(
    zonasSugeridas: List<ZonaSugerida>,
    onVerSugerencias: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            context.getString(R.string.suggested_zones_nearby),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            context.getString(R.string.zones_reported_by_others, zonasSugeridas.size),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = context.getString(R.string.close),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onVerSugerencias,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(context.getString(R.string.view_suggestions), fontWeight = FontWeight.Bold)
            }
        }
    }
}
