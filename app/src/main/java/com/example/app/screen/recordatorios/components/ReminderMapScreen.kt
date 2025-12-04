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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.app.models.Feature
import com.example.app.models.GeoJson
import com.example.app.models.Geometry
import com.example.app.models.PoisRequest
import com.example.app.network.AppDatabase
import com.example.app.network.NominatimClient
import com.example.app.network.RetrofitInstance
import com.example.app.repository.ReminderRepository
import com.example.app.screen.mapa.GetCurrentLocation
import com.example.app.screen.mapa.GpsEnableButton
import com.example.app.screen.mapa.OpenStreetMap
import com.example.app.screen.recordatorios.steps.ReminderStepsContent
import com.example.app.utils.LocationManager
import com.example.app.viewmodel.ReminderViewModel
import com.example.app.viewmodel.ReminderViewModelFactory
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
    val locationManager = remember { LocationManager.getInstance() }

    // Estados de ubicación
    var currentLat by remember { mutableStateOf(0.0) }
    var currentLon by remember { mutableStateOf(0.0) }
    var locationObtained by remember { mutableStateOf(false) }
    var showGpsButton by remember { mutableStateOf(false) }

    // Estados del mapa
    var mapCenterLat by remember { mutableStateOf(0.0) }
    var mapCenterLon by remember { mutableStateOf(0.0) }
    var selectedAddress by remember { mutableStateOf("") }

    var recenterTrigger by remember { mutableStateOf(0) }
    var zoomInTrigger by remember { mutableStateOf(0) }
    var zoomOutTrigger by remember { mutableStateOf(0) }

    var addressJob by remember { mutableStateOf<Job?>(null) }
    var poisJob by remember { mutableStateOf<Job?>(null) }
    var poisList by remember { mutableStateOf<List<Feature>>(emptyList()) }

    // 🆕 PASO ACTUAL
    var currentStep by remember { mutableStateOf(1) }

    // 🆕 DATOS DEL FORMULARIO (compartidos entre pasos)
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var reminderType by remember { mutableStateOf("location") }
    var selectedDays by remember { mutableStateOf(setOf<String>()) }
    var selectedTime by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var proximityRadius by remember { mutableStateOf(500f) }
    var triggerType by remember { mutableStateOf("enter") }
    var enableVibration by remember { mutableStateOf(true) }
    var enableSound by remember { mutableStateOf(true) }
    var selectedSoundType by remember { mutableStateOf("default") }

    // ViewModel
    val database = remember { AppDatabase.getDatabase(context) }
    val repository = remember { ReminderRepository(database.reminderDao()) }
    val viewModel: ReminderViewModel = viewModel(
        factory = ReminderViewModelFactory(repository)
    )

    // Función para cargar POIs
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

    LaunchedEffect(Unit) {
        val cachedLocation = locationManager.getLastKnownLocation()
        if (cachedLocation != null) {
            Log.d("ReminderMapScreen", "⚡ Usando ubicación en caché")
            currentLat = cachedLocation.latitude
            currentLon = cachedLocation.longitude
            mapCenterLat = cachedLocation.latitude
            mapCenterLon = cachedLocation.longitude
            selectedAddress = cachedLocation.address
            locationObtained = true
        }
    }

    // 🔥 Box principal que contiene todo
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            locationObtained -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Solo mostrar mapa en el paso 1
                    if (currentStep == 1) {
                        OpenStreetMap(
                            latitude = currentLat,
                            longitude = currentLon,
                            showUserLocation = true,
                            recenterTrigger = recenterTrigger,
                            zoomInTrigger = zoomInTrigger,
                            zoomOutTrigger = zoomOutTrigger,
                            pois = poisList,
                            modifier = Modifier.fillMaxSize(),
                            onLocationSelected = { lat, lon ->
                                mapCenterLat = lat
                                mapCenterLon = lon

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
                                loadPOIsForLocation(lat, lon)
                            }
                        )
                    }

                    // Sistema de pasos
                    ReminderStepsContent(
                        currentStep = currentStep,
                        totalSteps = 4,
                        navController = navController,
                        // Datos del formulario
                        selectedAddress = selectedAddress,
                        latitude = mapCenterLat,
                        longitude = mapCenterLon,
                        title = title,
                        description = description,
                        reminderType = reminderType,
                        selectedDays = selectedDays,
                        selectedTime = selectedTime,
                        proximityRadius = proximityRadius,
                        triggerType = triggerType,
                        enableVibration = enableVibration,
                        enableSound = enableSound,
                        selectedSoundType = selectedSoundType,
                        // Callbacks de actualización
                        onTitleChange = { title = it },
                        onDescriptionChange = { description = it },
                        onReminderTypeChange = { reminderType = it },
                        onSelectedDaysChange = { selectedDays = it },
                        onSelectedTimeChange = { selectedTime = it },
                        onProximityRadiusChange = { proximityRadius = it },
                        onTriggerTypeChange = { triggerType = it },
                        onEnableVibrationChange = { enableVibration = it },
                        onEnableSoundChange = { enableSound = it },
                        onSelectedSoundTypeChange = { selectedSoundType = it },
                        // Navegación
                        onNextStep = { currentStep++ },
                        onPreviousStep = {
                            if (currentStep > 1) currentStep--
                            else navController.popBackStack()
                        },
                        // Controles del mapa
                        onRecenterClick = { recenterTrigger++ },
                        onZoomInClick = { zoomInTrigger++ },
                        onZoomOutClick = { zoomOutTrigger++ },
                        // Guardar
                        viewModel = viewModel,
                        context = context,
                        onSaveSuccess = {
                            navController.navigate("home?tab=2") {
                                popUpTo("home") { inclusive = false }
                                launchSingleTop = true
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
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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
                                selectedAddress = response.display_name ?: ""
                                locationManager.updateLocation(lat, lon, selectedAddress)
                            } catch (e: Exception) {
                                selectedAddress = "Error obteniendo dirección"
                            }
                            loadPOIsForLocation(lat, lon)
                        }
                    },
                    onError = { /* manejar error */ },
                    onGpsDisabled = { showGpsButton = true }
                )
            }
        }

        // 🔥 GPS BUTTON SE SUPERPONE - SIEMPRE AL FINAL
        if (showGpsButton) {
            GpsEnableButton(onEnableGps = { showGpsButton = false })
        }
    }
}