package com.example.app.screen.rutas.components

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.app.models.UbicacionUsuarioCreate
import com.example.app.screen.mapa.GetCurrentLocation
import com.example.app.screen.mapa.GpsEnableButton
import com.example.app.screen.mapa.OpenStreetMap
import com.example.app.network.NominatimClient
import com.example.app.viewmodel.SessionManager
import com.example.app.viewmodel.UbicacionesViewModel
import com.example.app.viewmodel.UbicacionesViewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MapScreen(navController: NavController, onConfirmClick: () -> Unit = {}) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val token = sessionManager.getAccessToken() ?: return

    var currentLat by remember { mutableStateOf(0.0) }
    var currentLon by remember { mutableStateOf(0.0) }
    var locationObtained by remember { mutableStateOf(false) }
    var showGpsButton by remember { mutableStateOf(false) }

    var currentAddress by remember { mutableStateOf("") }
    var selectedAddress by remember { mutableStateOf("") }

    var recenterTrigger by remember { mutableStateOf(0) }
    var job by remember { mutableStateOf<Job?>(null) }

    // Guardar el último centro del mapa
    var mapCenterLat by remember { mutableStateOf(currentLat) }
    var mapCenterLon by remember { mutableStateOf(currentLon) }
    var locationName by rememberSaveable { mutableStateOf("") }

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
                        userLocation = "Tu dirección actual",
                        selectedLocation = selectedAddress,
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
                                    Toast.makeText(context, "Ubicación creada exitosamente!", Toast.LENGTH_SHORT).show()
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
