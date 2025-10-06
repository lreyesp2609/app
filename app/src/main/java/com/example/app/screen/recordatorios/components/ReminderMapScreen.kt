package com.example.app.screen.recordatorios.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.app.network.NominatimClient
import com.example.app.screen.mapa.GetCurrentLocation
import com.example.app.screen.mapa.GpsEnableButton
import com.example.app.screen.mapa.OpenStreetMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ReminderMapScreen(
    navController: NavController,
    onLocationSelected: (lat: Double, lon: Double, address: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentLat by remember { mutableStateOf(0.0) }
    var currentLon by remember { mutableStateOf(0.0) }
    var locationObtained by remember { mutableStateOf(false) }
    var showGpsButton by remember { mutableStateOf(false) }

    var currentAddress by remember { mutableStateOf("") }
    var selectedAddress by remember { mutableStateOf("") }

    var recenterTrigger by remember { mutableStateOf(0) }
    var job by remember { mutableStateOf<Job?>(null) }

    // Guardar último centro del mapa
    var mapCenterLat by remember { mutableStateOf(currentLat) }
    var mapCenterLon by remember { mutableStateOf(currentLon) }

    Box(modifier = Modifier.fillMaxSize()) {

        if (locationObtained) {
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

                // 🎯 Usar el componente separado para los botones
                ReminderMapButtons(
                    selectedAddress = selectedAddress,
                    onConfirmClick = {
                        if (mapCenterLat != 0.0 && mapCenterLon != 0.0 && selectedAddress.isNotBlank()) {
                            onLocationSelected(mapCenterLat, mapCenterLon, selectedAddress)
                        }
                    },
                    onRecenterClick = { recenterTrigger++ },
                    onBackClick = { navController.popBackStack() }
                )
            }
        } else if (showGpsButton) {
            GpsEnableButton(onEnableGps = { showGpsButton = false })
        } else {
            // Loading antes de obtener ubicación
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
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            GetCurrentLocation(
                onLocationResult = { lat, lon ->
                    currentLat = lat
                    currentLon = lon
                    locationObtained = true
                    mapCenterLat = lat
                    mapCenterLon = lon

                    scope.launch {
                        try {
                            val response = NominatimClient.apiService.reverseGeocode(
                                lat = lat,
                                lon = lon
                            )
                            val address = response.display_name ?: ""
                            currentAddress = address
                            selectedAddress = address
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