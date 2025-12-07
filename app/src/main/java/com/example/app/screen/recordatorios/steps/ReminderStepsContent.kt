package com.example.app.screen.recordatorios.steps

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.app.viewmodel.NotificationViewModel
import com.example.app.viewmodel.ReminderViewModel

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
    selectedSoundUri: String,  // ← CAMBIO: selectedSoundType → selectedSoundUri
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onReminderTypeChange: (String) -> Unit,
    onSelectedDaysChange: (Set<String>) -> Unit,
    onSelectedTimeChange: (Pair<Int, Int>?) -> Unit,
    onProximityRadiusChange: (Float) -> Unit,
    onTriggerTypeChange: (String) -> Unit,
    onEnableVibrationChange: (Boolean) -> Unit,
    onEnableSoundChange: (Boolean) -> Unit,
    onSelectedSoundUriChange: (String) -> Unit,  // ← CAMBIO: onSelectedSoundTypeChange → onSelectedSoundUriChange
    onNextStep: () -> Unit,
    onPreviousStep: () -> Unit,
    onRecenterClick: () -> Unit,
    onZoomInClick: () -> Unit,
    onZoomOutClick: () -> Unit,
    viewModel: ReminderViewModel,
    context: Context,
    onSaveSuccess: () -> Unit,
    notificationViewModel: NotificationViewModel
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
            onBackClick = onPreviousStep,
            notificationViewModel = notificationViewModel,
            latitude = latitude,
            longitude = longitude
        )

        4 -> Step4Notifications(
            enableVibration = enableVibration,
            enableSound = enableSound,
            selectedSoundUri = selectedSoundUri,  // ← CAMBIO: selectedSoundType → selectedSoundUri
            onEnableVibrationChange = onEnableVibrationChange,
            onEnableSoundChange = onEnableSoundChange,
            onSelectedSoundUriChange = onSelectedSoundUriChange,  // ← CAMBIO: onSelectedSoundTypeChange → onSelectedSoundUriChange
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
            onSaveSuccess = onSaveSuccess,
            notificationViewModel = notificationViewModel,
        )
    }
}