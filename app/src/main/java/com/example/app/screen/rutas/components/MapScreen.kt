package com.example.app.screen.rutas.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.app.models.UbicacionUsuarioCreate
import com.example.app.screen.mapa.GetCurrentLocation
import com.example.app.screen.mapa.GpsEnableButton
import com.example.app.screen.mapa.OpenStreetMap
import com.example.app.network.NominatimClient
import com.example.app.screen.components.AppBackButton
import com.example.app.screen.components.AppButton
import com.example.app.screen.components.AppTextField
import com.example.app.screen.mapa.MapControlButton
import com.example.app.utils.SessionManager
import com.example.app.viewmodel.UbicacionesViewModel
import com.example.app.viewmodel.UbicacionesViewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MapScreen(navController: NavController, defaultLat: Double = 0.0,
              defaultLon: Double = 0.0, onConfirmClick: () -> Unit = {}) {
    val context = LocalContext.current
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

    var mapCenterLat by remember { mutableStateOf(currentLat) }
    var mapCenterLon by remember { mutableStateOf(currentLon) }
    var locationName by rememberSaveable { mutableStateOf("") }

    var zoomInTrigger by remember { mutableStateOf(0) }
    var zoomOutTrigger by remember { mutableStateOf(0) }

    val userLat = remember { mutableStateOf(defaultLat) }
    val userLon = remember { mutableStateOf(defaultLon) }

    // 🆕 Estado para mostrar/ocultar cards
    var showLocationCards by remember { mutableStateOf(true) }

    val ubicacionesViewModel: UbicacionesViewModel = viewModel(
        factory = UbicacionesViewModelFactory(token)
    )

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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
                        }
                    )

                    // 🆕 BOTÓN ATRÁS - Top Left
                    AppBackButton(
                        navController = navController,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .statusBarsPadding()
                            .padding(16.dp),
                        backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    )

                    // 🆕 BOTÓN TOGGLE CARDS - Top Right
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

                    // 📌 BOTONES DE ZOOM Y CENTRAR — MISMO DISEÑO QUE RutaMapa
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



                    // 🆕 CARDS COLAPSABLES (solo si showLocationCards = true)
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
                                .padding(top = 80.dp, start = 16.dp, end = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Card compacta de ubicación actual
                            CompactLocationCard(
                                title = "Tu ubicación",
                                location = currentAddress,
                                icon = Icons.Default.MyLocation,
                                iconColor = Color(0xFF10B981)
                            )

                            // Card compacta de ubicación seleccionada
                            if (selectedAddress.isNotEmpty()) {
                                CompactLocationCard(
                                    title = "Ubicación seleccionada",
                                    location = selectedAddress,
                                    icon = Icons.Default.LocationOn,
                                    iconColor = Color(0xFFEF4444)
                                )
                            }
                        }
                    }

                    // 🆕 PANEL INFERIOR MEJORADO
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
                                ubicacionesViewModel.crearUbicacion(nuevaUbicacion) {
                                    Toast.makeText(
                                        context,
                                        "Ubicación guardada exitosamente",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    navController.popBackStack()
                                }
                            }
                        }
                    )
                }
            }

            showGpsButton -> {
                GpsEnableButton(onEnableGps = { showGpsButton = false })
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
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Obteniendo ubicación...",
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.bodyMedium
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
                            } catch (e: Exception) {
                                currentAddress = "Error obteniendo dirección"
                                selectedAddress = "Error obteniendo dirección"
                            }
                        }
                    },
                    onError = { /* manejar error */ },
                    onGpsDisabled = { showGpsButton = true }
                )
            }
        }
    }
}

// 🆕 CARD COMPACTA (ocupa menos espacio)
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

// 🆕 PANEL INFERIOR OPTIMIZADO
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
                // Título
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

                // Campo de texto
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

                // Botón confirmar
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