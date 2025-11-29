package com.example.app.screen.recordatorios.components

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.example.app.screen.components.AppBackButton
import com.example.app.screen.components.AppButton
import com.example.app.screen.components.AppTextField
import com.example.app.screen.mapa.GetCurrentLocation
import com.example.app.screen.mapa.GpsEnableButton
import com.example.app.screen.mapa.MapControlButton
import com.example.app.screen.mapa.OpenStreetMap
import com.example.app.screen.recordatorios.steps.ReminderStepsContent
import com.example.app.screen.recordatorios.steps.Step3ReminderType
import com.example.app.screen.recordatorios.steps.Step4Notifications
import com.example.app.screen.recordatorios.steps.StepIndicator
import com.example.app.screen.rutas.components.CompactLocationCard
import com.example.app.viewmodel.ReminderViewModel
import com.example.app.viewmodel.ReminderViewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.compareTo
import kotlin.dec
import kotlin.inc

@Composable
fun ReminderMapScreen(
    navController: NavController,
    onLocationSelected: (lat: Double, lon: Double, address: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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

    Box(modifier = Modifier.fillMaxSize()) {
        if (locationObtained) {
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
}

// 🎯 Componente principal que maneja los pasos
@Composable
fun ReminderStepsContent(
    currentStep: Int,
    totalSteps: Int,
    navController: NavController,
    selectedAddress: String,
    latitude: Double,
    longitude: Double,
    title: String,
    description: String,
    reminderType: String,
    selectedDays: Set<String>,
    selectedTime: Pair<Int, Int>?,
    proximityRadius: Float,
    triggerType: String,
    enableVibration: Boolean,
    enableSound: Boolean,
    selectedSoundType: String,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onReminderTypeChange: (String) -> Unit,
    onSelectedDaysChange: (Set<String>) -> Unit,
    onSelectedTimeChange: (Pair<Int, Int>?) -> Unit,
    onProximityRadiusChange: (Float) -> Unit,
    onTriggerTypeChange: (String) -> Unit,
    onEnableVibrationChange: (Boolean) -> Unit,
    onEnableSoundChange: (Boolean) -> Unit,
    onSelectedSoundTypeChange: (String) -> Unit,
    onNextStep: () -> Unit,
    onPreviousStep: () -> Unit,
    onRecenterClick: () -> Unit,
    onZoomInClick: () -> Unit,
    onZoomOutClick: () -> Unit,
    viewModel: ReminderViewModel,
    context: Context,
    onSaveSuccess: () -> Unit
) {
    when (currentStep) {
        1 -> Step1SelectLocation(
            navController = navController,
            selectedAddress = selectedAddress,
            onNextClick = onNextStep,
            onBackClick = onPreviousStep,
            onRecenterClick = onRecenterClick,
            onZoomInClick = onZoomInClick,
            onZoomOutClick = onZoomOutClick
        )

        2 -> Step2BasicInfo(
            title = title,
            description = description,
            onTitleChange = onTitleChange,
            onDescriptionChange = onDescriptionChange,
            onNextClick = onNextStep,
            onBackClick = onPreviousStep
        )

        3 -> Step3ReminderType(
            reminderType = reminderType,
            selectedDays = selectedDays,
            selectedTime = selectedTime,
            proximityRadius = proximityRadius,
            triggerType = triggerType,
            selectedAddress = selectedAddress,
            onReminderTypeChange = onReminderTypeChange,
            onSelectedDaysChange = onSelectedDaysChange,
            onSelectedTimeChange = onSelectedTimeChange,
            onProximityRadiusChange = onProximityRadiusChange,
            onTriggerTypeChange = onTriggerTypeChange,
            onNextClick = onNextStep,
            onBackClick = onPreviousStep
        )

        4 -> Step4Notifications(
            enableVibration = enableVibration,
            enableSound = enableSound,
            selectedSoundType = selectedSoundType,
            onEnableVibrationChange = onEnableVibrationChange,
            onEnableSoundChange = onEnableSoundChange,
            onSelectedSoundTypeChange = onSelectedSoundTypeChange,
            onBackClick = onPreviousStep,
            viewModel = viewModel,
            context = context,
            // Datos para guardar
            title = title,
            description = description,
            reminderType = reminderType,
            selectedDays = selectedDays,
            selectedTime = selectedTime,
            proximityRadius = proximityRadius,
            triggerType = triggerType,
            selectedAddress = selectedAddress,
            latitude = latitude,
            longitude = longitude,
            onSaveSuccess = onSaveSuccess
        )
    }
}

// 📍 PASO 1: Seleccionar ubicación
@Composable
fun Step1SelectLocation(
    navController: NavController,
    selectedAddress: String,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
    onRecenterClick: () -> Unit,
    onZoomInClick: () -> Unit,
    onZoomOutClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .statusBarsPadding()
        ) {
            AppBackButton(
                navController = navController,
                onClick = onBackClick
            )

            Spacer(modifier = Modifier.height(12.dp))

            StepIndicator(
                currentStep = 1,
                totalSteps = 4,
                stepTitle = "Seleccionar ubicación"
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedAddress.isNotEmpty()) {
                CompactLocationCard(
                    title = "Ubicación seleccionada",
                    location = selectedAddress,
                    icon = Icons.Default.LocationOn,
                    iconColor = Color(0xFFEF4444)
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MapControlButton(icon = Icons.Default.Add, onClick = onZoomInClick)
            MapControlButton(icon = Icons.Default.Remove, onClick = onZoomOutClick)
            MapControlButton(icon = Icons.Default.MyLocation, onClick = onRecenterClick)
        }

        val canContinue = selectedAddress.isNotEmpty()
        AppButton(
            text = "Siguiente",
            icon = Icons.Default.ArrowForward,
            onClick = onNextClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .imePadding()
                .navigationBarsPadding()
                .padding(16.dp),
            enabled = canContinue,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary
        )
    }
}

// 📝 PASO 2: Información básica
@Composable
fun Step2BasicInfo(
    title: String,
    description: String,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit
) {
    var showTitleError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        StepIndicator(
            currentStep = 2,
            totalSteps = 4,
            stepTitle = "Información del recordatorio"
        )

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                AppTextField(
                    value = title,
                    onValueChange = {
                        onTitleChange(it)
                        if (it.isNotEmpty()) showTitleError = false
                    },
                    label = "Nombre del recordatorio *",
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )
                if (showTitleError) {
                    Text(
                        text = "El título es obligatorio",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }

            AppTextField(
                value = description,
                onValueChange = onDescriptionChange,
                label = "Descripción (opcional)",
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 5
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppButton(
                text = "Atrás",
                leadingIcon = { Icon(Icons.Default.ArrowBack, contentDescription = null) },
                onClick = onBackClick,
                modifier = Modifier.weight(1f),
                outlined = true
            )
            AppButton(
                text = "Siguiente",
                icon = Icons.Default.ArrowForward,
                onClick = {
                    if (title.isEmpty()) {
                        showTitleError = true
                    } else {
                        onNextClick()
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}