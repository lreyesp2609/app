package com.example.app.screen.rutas.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.app.screen.mapa.GetCurrentLocation
import com.example.app.screen.mapa.GpsEnableButton
import com.example.app.screen.mapa.OpenStreetMap
import com.example.app.network.NominatimClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MapScreen(onConfirmClick: () -> Unit = {}) {
    var currentLat by remember { mutableStateOf(0.0) }
    var currentLon by remember { mutableStateOf(0.0) }
    var locationObtained by remember { mutableStateOf(false) }
    var showGpsButton by remember { mutableStateOf(false) }

    var currentAddress by remember { mutableStateOf("") }
    var selectedAddress by remember { mutableStateOf("") }

    var recenterTrigger by remember { mutableStateOf(0) }
    var job by remember { mutableStateOf<Job?>(null) }

    val scope = rememberCoroutineScope()

    // Guardar el último centro del mapa
    var mapCenterLat by remember { mutableStateOf(currentLat) }
    var mapCenterLon by remember { mutableStateOf(currentLon) }
    var locationName by rememberSaveable { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            locationObtained -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    OpenStreetMap(
                        latitude = currentLat,
                        longitude = currentLon,
                        showUserLocation = true,
                        recenterTrigger = recenterTrigger,
                        modifier = Modifier.fillMaxSize(),
                        onLocationSelected = { lat, lon ->
                            mapCenterLat = lat
                            mapCenterLon = lon
                            // Cancelar petición anterior si el usuario sigue moviendo
                            job?.cancel()
                            job = scope.launch {
                                delay(500) // Debounce para no saturar la API
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

                    // Botón para centrar en ubicación actual
                    FloatingActionButton(
                        onClick = { recenterTrigger++ },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp),
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "Centrar mapa",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    MapBottomButtons(
                        userLocation = currentAddress,
                        selectedLocation = selectedAddress,
                        locationName = locationName,
                        onLocationNameChange = { locationName = it },
                        onConfirmClick = onConfirmClick
                    )
                }
            }
            showGpsButton -> {
                GpsEnableButton(onEnableGps = { showGpsButton = false })
            }
            else -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Obteniendo ubicación...")
                }

                GetCurrentLocation(
                    onLocationResult = { lat, lon ->
                        currentLat = lat
                        currentLon = lon
                        locationObtained = true

                        // Dirección inicial para ambos
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
