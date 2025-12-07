package com.example.app.screen.rutas.components

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
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
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
import com.example.app.models.ZonaGuardada
import com.example.app.models.ZonaPeligrosaCreate
import com.example.app.network.RetrofitClient
import com.example.app.screen.components.AppBackButton
import com.example.app.screen.mapa.GetCurrentLocation
import com.example.app.screen.mapa.GpsEnableButton
import com.example.app.screen.mapa.MapControlButton
import com.example.app.screen.mapa.OpenStreetMap
import com.example.app.ui.theme.AppColors
import com.example.app.ui.theme.SecurityColors
import com.example.app.utils.DialogoCrearZonaPeligrosa
import com.example.app.utils.LocationManager
import com.example.app.utils.SessionManager
import com.example.app.viewmodel.MapViewModel
import com.example.app.viewmodel.NotificationViewModel
import kotlinx.coroutines.launch

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

    var currentLat by remember { mutableStateOf(0.0) }
    var currentLon by remember { mutableStateOf(0.0) }
    var locationObtained by remember { mutableStateOf(false) }
    var showGpsButton by remember { mutableStateOf(false) }

    var recenterTrigger by remember { mutableStateOf(0) }
    var zoomInTrigger by remember { mutableStateOf(0) }
    var zoomOutTrigger by remember { mutableStateOf(0) }

    // Estados para zonas peligrosas
    var zonasCreadas by remember { mutableStateOf<List<ZonaGuardada>>(emptyList()) }
    var mostrarDialogoZona by remember { mutableStateOf(false) }
    var coordenadasZonaSeleccionada by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var radioPreview by remember { mutableStateOf(200) }
    var mostrarZonasPeligrosas by remember { mutableStateOf(true) }
    var cargandoZonas by remember { mutableStateOf(false) }

    // Estado para edición/eliminación
    var zonaSeleccionadaParaEditar by remember { mutableStateOf<ZonaGuardada?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showBottomSheet by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val isDarkTheme = isSystemInDarkTheme()

    // Cargar ubicación desde caché
    LaunchedEffect(Unit) {
        val cachedLocation = locationManager.getLastKnownLocation()
        if (cachedLocation != null) {
            currentLat = cachedLocation.latitude
            currentLon = cachedLocation.longitude
            locationObtained = true
            Log.d("ZonasPeligrosas", "⚡ Ubicación en caché cargada")
        } else {
            Log.d("ZonasPeligrosas", "⏳ Obteniendo nueva ubicación...")
        }
    }

    // Cargar zonas desde el backend
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
                                id = zona.id // Asegúrate de tener este campo
                            )
                        } else {
                            Log.w("ZonasPeligrosas", "Zona sin coordenadas: ${zona.nombre}")
                            null
                        }
                    }

                    Log.d("ZonasPeligrosas", "✅ ${zonasCreadas.size} zonas cargadas")

                    if (zonasCreadas.isNotEmpty()) {
                        notificationViewModel.showSuccess("${zonasCreadas.size} zona(s) cargada(s)")
                    }
                } catch (e: Exception) {
                    Log.e("ZonasPeligrosas", "Error cargando zonas: ${e.message}", e)
                    notificationViewModel.showError("Error al cargar zonas peligrosas")
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
                    // Mapa
                    OpenStreetMap(
                        latitude = currentLat,
                        longitude = currentLon,
                        showUserLocation = true,
                        recenterTrigger = recenterTrigger,
                        zoomInTrigger = zoomInTrigger,
                        zoomOutTrigger = zoomOutTrigger,
                        modifier = Modifier.fillMaxSize(),
                        onLocationSelected = { lat, lon ->
                            // No hacer nada en tap simple
                        },
                        onLocationLongPress = { lat, lon ->
                            coordenadasZonaSeleccionada = Pair(lat, lon)
                            mostrarDialogoZona = true
                            radioPreview = 200
                        },
                        zonaPreviewLat = if (mostrarDialogoZona) coordenadasZonaSeleccionada?.first else null,
                        zonaPreviewLon = if (mostrarDialogoZona) coordenadasZonaSeleccionada?.second else null,
                        zonaPreviewRadio = if (mostrarDialogoZona) radioPreview else null,
                        zonasGuardadas = if (mostrarZonasPeligrosas) zonasCreadas else emptyList(),
                        onZonaClick = { zona ->
                            // Cuando se hace tap en una zona del mapa
                            zonaSeleccionadaParaEditar = zona
                            showBottomSheet = true
                        }
                    )

                    // Botón de regreso
                    AppBackButton(
                        navController = navController,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .statusBarsPadding()
                            .padding(16.dp),
                        backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    )

                    // Header con título
                    Card(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(top = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = SecurityColors.getDangerColor(isDarkTheme),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Mis zonas peligrosas",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (zonasCreadas.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .background(
                                            SecurityColors.getDangerColor(isDarkTheme),
                                            CircleShape
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        "${zonasCreadas.size}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // Botones de control
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
                            onClick = { recenterTrigger++ }
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
                                        if (mostrarZonasPeligrosas) "Zonas visibles"
                                        else "Zonas ocultas"
                                    )
                                },
                                badge = zonasCreadas.size.toString()
                            )
                        }
                    }

                    // Panel inferior con lista de zonas
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
                // Loading state
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
                    onError = { error ->
                        Log.e("ZonasPeligrosas", "Error: $error")
                    },
                    onGpsDisabled = { showGpsButton = true }
                )
            }
        }

        // Diálogo para crear zona
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

                            notificationViewModel.showSuccess("Zona creada: ${response.nombre}")

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
                            notificationViewModel.showError("Error al crear zona: ${e.message}")
                            Log.e("ZonasPeligrosas", "Error", e)
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

        // Bottom sheet para editar/eliminar zona
        if (showBottomSheet && zonaSeleccionadaParaEditar != null) {
            ZonaDetailBottomSheet(
                zona = zonaSeleccionadaParaEditar!!,
                onDismiss = {
                    showBottomSheet = false
                    zonaSeleccionadaParaEditar = null
                },
                onEliminar = {
                    showDeleteDialog = true
                },
                isDarkTheme = isDarkTheme
            )
        }

        // Diálogo de confirmación para eliminar
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
                title = {
                    Text(
                        "¿Eliminar zona?",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text("Estás a punto de eliminar:")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "\"${zonaSeleccionadaParaEditar!!.nombre}\"",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Esta acción no se puede deshacer.",
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
                                notificationViewModel.showError("Error: Zona sin ID")
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
                                        // ✅ Éxito
                                        zonasCreadas = zonasCreadas.filter { it.id != zonaId }
                                        notificationViewModel.showSuccess("Zona eliminada correctamente")

                                        // También actualizar el ViewModel si lo usas
                                        mapViewModel.cargarZonasPeligrosas(token)

                                        showDeleteDialog = false
                                        showBottomSheet = false
                                        zonaSeleccionadaParaEditar = null

                                        Log.d("ZonasScreen", "✅ Zona eliminada correctamente")
                                    } else {
                                        val errorMsg = "Error ${response.code()}: ${response.message()}"
                                        notificationViewModel.showError(errorMsg)
                                        Log.e("ZonasScreen", "❌ $errorMsg")
                                    }

                                } catch (e: Exception) {
                                    val errorMsg = e.message ?: "Error desconocido"
                                    notificationViewModel.showError("Error: $errorMsg")
                                    Log.e("ZonasScreen", "❌ Error: $errorMsg", e)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Danger
                        )
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Eliminar", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancelar")
                    }
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


@Composable
fun BottomZonasPanel(
    zonas: List<ZonaGuardada>,
    isLoading: Boolean,
    onZonaClick: (ZonaGuardada) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = isSystemInDarkTheme()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp)
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
            // Header
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
                        "Zonas registradas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (zonas.isNotEmpty()) {
                    Text(
                        "${zonas.size} zona(s)",
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
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
                            "No hay zonas registradas",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Mantén presionado el mapa para agregar",
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
                        items(
                            items = zonas,
                            key = { it.id ?: it.hashCode() }
                        ) { zona ->
                            ZonaListItem(
                                zona = zona,
                                onClick = { onZonaClick(zona) },
                                isDarkTheme = isDarkTheme
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ZonaListItem(
    zona: ZonaGuardada,
    onClick: () -> Unit,
    isDarkTheme: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = SecurityColors.getDangerBackground(isDarkTheme)
        ),
        border = BorderStroke(
            1.dp,
            SecurityColors.getDangerColor(isDarkTheme).copy(alpha = 0.3f)
        )
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
                    .background(
                        SecurityColors.getDangerColor(isDarkTheme).copy(alpha = 0.2f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = SecurityColors.getDangerColor(isDarkTheme),
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
                    "Radio: ${zona.radio}m • Nivel: ${zona.nivel}",
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZonaDetailBottomSheet(
    zona: ZonaGuardada,
    onDismiss: () -> Unit,
    onEliminar: () -> Unit,
    isDarkTheme: Boolean
) {
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            SecurityColors.getDangerBackground(isDarkTheme),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = SecurityColors.getDangerColor(isDarkTheme),
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
                        "Zona peligrosa",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Detalles
            DetailRow(
                icon = Icons.Default.RadioButtonChecked,
                label = "Radio",
                value = "${zona.radio} metros"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 🔥 Nivel de peligro con estrellas
            DetailRow(
                icon = Icons.Default.Warning,
                label = "Nivel de peligro",
                value = buildString {
                    append("⭐".repeat(zona.nivel))
                    append(" (${zona.nivel}/5)")
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            DetailRow(
                icon = Icons.Default.LocationOn,
                label = "Coordenadas",
                value = "${String.format("%.5f", zona.lat)}, ${String.format("%.5f", zona.lon)}"
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Botón eliminar
            Button(
                onClick = onEliminar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Danger
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Eliminar zona",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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