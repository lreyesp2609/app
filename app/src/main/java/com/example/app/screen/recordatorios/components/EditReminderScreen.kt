package com.example.app.screen.recordatorios.components

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.example.app.models.toReminder
import com.example.app.network.NominatimClient
import com.example.app.screen.mapa.OpenStreetMap
import com.example.app.screen.recordatorios.steps.ReminderStepsContent
import com.example.app.viewmodel.NotificationViewModel
import com.example.app.viewmodel.ReminderViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EditReminderScreen(
    reminderId: Int,
    navController: NavController,
    viewModel: ReminderViewModel,
    notificationViewModel: NotificationViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Estados para los pasos
    var currentStep by remember { mutableIntStateOf(1) }
    var selectedAddress by remember { mutableStateOf("") }
    var latitude by remember { mutableDoubleStateOf(0.0) }
    var longitude by remember { mutableDoubleStateOf(0.0) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    // 🔥 CAMBIO: Volver a var normal, NO derivado
    var reminderType by remember { mutableStateOf("location") }

    var selectedDays by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedTime by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var proximityRadius by remember { mutableFloatStateOf(500f) }
    var triggerType by remember { mutableStateOf("enter") }
    var enableVibration by remember { mutableStateOf(true) }
    var enableSound by remember { mutableStateOf(true) }
    var selectedSoundUri by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(true) }

    // 🆕 Estados del mapa
    var recenterTrigger by remember { mutableStateOf(0) }
    var zoomInTrigger by remember { mutableStateOf(0) }
    var zoomOutTrigger by remember { mutableStateOf(0) }
    var addressJob by remember { mutableStateOf<Job?>(null) }

    // 🔥 Cargar datos del recordatorio desde la BASE DE DATOS
    LaunchedEffect(reminderId) {
        try {
            viewModel.getReminderById(reminderId) { reminderEntity ->
                if (reminderEntity != null) {
                    val reminder = reminderEntity.toReminder()

                    title = reminder.title
                    description = reminder.description ?: ""
                    reminderType = reminder.reminder_type // 🔥 Se carga normalmente
                    selectedAddress = reminder.location ?: ""
                    latitude = reminder.latitude ?: 0.0
                    longitude = reminder.longitude ?: 0.0
                    proximityRadius = reminder.radius?.toFloat() ?: 500f
                    triggerType = reminder.trigger_type
                    enableVibration = reminder.vibration
                    enableSound = reminder.sound
                    selectedSoundUri = reminder.sound_uri ?: ""

                    reminder.days?.let { days ->
                        selectedDays = days.toSet()
                    }

                    reminder.time?.let { timeStr ->
                        val parts = timeStr.split(":")
                        if (parts.size >= 2) {
                            selectedTime = Pair(parts[0].toInt(), parts[1].toInt())
                        }
                    }

                    isLoading = false
                } else {
                    notificationViewModel.showError("Recordatorio no encontrado")
                    navController.popBackStack()
                }
            }
        } catch (e: Exception) {
            Log.e("EditReminder", "Error cargando recordatorio: ${e.message}")
            notificationViewModel.showError("Error al cargar recordatorio")
            navController.popBackStack()
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            if (currentStep == 1) {
                OpenStreetMap(
                    latitude = latitude,
                    longitude = longitude,
                    showUserLocation = false,
                    recenterTrigger = recenterTrigger,
                    zoomInTrigger = zoomInTrigger,
                    zoomOutTrigger = zoomOutTrigger,
                    pois = emptyList(),
                    modifier = Modifier.fillMaxSize(),
                    onLocationSelected = { lat, lon ->
                        latitude = lat
                        longitude = lon

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
                    }
                )
            }

            ReminderStepsContent(
                currentStep = currentStep,
                totalSteps = 4,
                navController = navController,
                selectedAddress = selectedAddress,
                latitude = latitude,
                longitude = longitude,
                title = title,
                description = description,
                reminderType = reminderType,
                selectedDays = selectedDays,
                selectedTime = selectedTime,
                proximityRadius = proximityRadius,
                triggerType = triggerType,
                enableVibration = enableVibration,
                enableSound = enableSound,
                selectedSoundUri = selectedSoundUri,
                onTitleChange = { title = it },
                onDescriptionChange = { description = it },
                onReminderTypeChange = {
                    reminderType = it // 🔥 El usuario controla el tipo manualmente
                },
                onSelectedDaysChange = { selectedDays = it },
                onSelectedTimeChange = { selectedTime = it },
                onProximityRadiusChange = { proximityRadius = it },
                onTriggerTypeChange = { triggerType = it },
                onEnableVibrationChange = { enableVibration = it },
                onEnableSoundChange = { enableSound = it },
                onSelectedSoundUriChange = { selectedSoundUri = it },
                onNextStep = { currentStep++ },
                onPreviousStep = {
                    if (currentStep > 1) currentStep--
                    else navController.popBackStack()
                },
                onRecenterClick = { recenterTrigger++ },
                onZoomInClick = { zoomInTrigger++ },
                onZoomOutClick = { zoomOutTrigger++ },
                viewModel = viewModel,
                context = context,
                onSaveSuccess = {
                    navController.popBackStack()
                },
                notificationViewModel = notificationViewModel,
                isEditMode = true,
                editReminderId = reminderId
            )
        }
    }
}