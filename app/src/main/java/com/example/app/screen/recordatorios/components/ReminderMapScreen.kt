package com.example.app.screen.recordatorios.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.app.models.Feature
import com.example.app.models.GeoJson
import com.example.app.models.Geometry
import com.example.app.models.PoisRequest
import com.example.app.network.NominatimClient
import com.example.app.network.RetrofitInstance
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
    var zoomInTrigger by remember { mutableStateOf(0) }    // ✅ AGREGADO
    var zoomOutTrigger by remember { mutableStateOf(0) }   // ✅ AGREGADO
    var addressJob by remember { mutableStateOf<Job?>(null) }
    var poisJob by remember { mutableStateOf<Job?>(null) }

    var mapCenterLat by remember { mutableStateOf(currentLat) }
    var mapCenterLon by remember { mutableStateOf(currentLon) }

    var poisList by remember { mutableStateOf<List<Feature>>(emptyList()) }

    // 🆕 Función para cargar POIs
    fun loadPOIsForLocation(lat: Double, lon: Double) {
        poisJob?.cancel()
        poisJob = scope.launch {
            delay(800)

            Log.d("POI_DEBUG", "🔄 Cargando POIs para: lat=$lat, lon=$lon")
            try {
                val poisRequest = PoisRequest(
                    geometry = Geometry(
                        geojson = GeoJson(
                            type = "Point",
                            coordinates = listOf(lon, lat)
                        ),
                        buffer = 500
                    )
                )

                val poisResponse = RetrofitInstance.poisApi.getPOIs(poisRequest)
                val newPois = poisResponse.features ?: emptyList()

                Log.d("POI_DEBUG", "✅ Nuevos POIs: ${newPois.size}")
                poisList = newPois

            } catch (e: Exception) {
                Log.e("POI_DEBUG", "❌ Error cargando POIs: ${e.message}", e)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        if (locationObtained) {
            Box(modifier = Modifier.fillMaxSize()) {

                OpenStreetMap(
                    latitude = currentLat,
                    longitude = currentLon,
                    showUserLocation = true,
                    recenterTrigger = recenterTrigger,
                    zoomInTrigger = zoomInTrigger,      // ✅ AGREGADO
                    zoomOutTrigger = zoomOutTrigger,    // ✅ AGREGADO
                    pois = poisList,
                    modifier = Modifier.fillMaxSize(),
                    onLocationSelected = { lat, lon ->
                        mapCenterLat = lat
                        mapCenterLon = lon

                        // Obtener dirección (con debounce)
                        addressJob?.cancel()
                        addressJob = scope.launch {
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

                        // 🆕 Cargar POIs de la nueva ubicación
                        loadPOIsForLocation(lat, lon)
                    }
                )

                ReminderMapButtons(
                    navController = navController,
                    selectedAddress = selectedAddress,
                    onConfirmClick = {
                        if (mapCenterLat != 0.0 && mapCenterLon != 0.0 && selectedAddress.isNotBlank()) {
                            onLocationSelected(mapCenterLat, mapCenterLon, selectedAddress)
                        }
                    },
                    onRecenterClick = { recenterTrigger++ },
                    onZoomInClick = { zoomInTrigger++ },    // ✅ AGREGADO
                    onZoomOutClick = { zoomOutTrigger++ },  // ✅ AGREGADO
                    onBackClick = { navController.popBackStack() }
                )
            }
        } else if (showGpsButton) {
            GpsEnableButton(onEnableGps = { showGpsButton = false })
        } else {
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
                        // Obtener dirección inicial
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

                        // 🆕 Cargar POIs iniciales
                        loadPOIsForLocation(lat, lon)
                    }
                },
                onError = { /* manejar error */ },
                onGpsDisabled = { showGpsButton = true }
            )
        }
    }
}