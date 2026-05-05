package com.rutai.app.screen.recordatorios.steps

import android.content.Context
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rutai.app.R
import com.rutai.app.models.NotificationSound
import com.rutai.app.models.Reminder
import com.rutai.app.screen.components.AppButton
import com.rutai.app.viewmodel.NotificationViewModel
import com.rutai.app.viewmodel.ReminderViewModel
import com.rutai.app.viewmodel.rememberSystemNotificationSounds

@Composable
fun Step4Notifications(
    enableVibration: Boolean,
    enableSound: Boolean,
    selectedSoundUri: String,
    onEnableVibrationChange: (Boolean) -> Unit,
    onEnableSoundChange: (Boolean) -> Unit,
    onSelectedSoundUriChange: (String) -> Unit,
    onBackClick: () -> Unit,
    viewModel: ReminderViewModel,
    context: Context,
    title: String,
    description: String,
    reminderType: String,
    selectedDays: Set<String>,
    selectedTime: Pair<Int, Int>?,
    proximityRadius: Float,
    triggerType: String,
    selectedAddress: String,
    latitude: Double,
    longitude: Double,
    onSaveSuccess: () -> Unit,
    notificationViewModel: NotificationViewModel,
    isEditMode: Boolean = false,
    editReminderId: Int? = null
) {
    val scrollState = rememberScrollState()
    var showSoundPicker by remember { mutableStateOf(false) }
    val systemSounds = rememberSystemNotificationSounds(context)

    // Obtener el título del sonido seleccionado
    val selectedSoundTitle = remember(selectedSoundUri, systemSounds) {
        systemSounds.find { it.uri == selectedSoundUri }?.title ?: context.getString(R.string.sound_default_system)
    }

    // Dialog selector de sonido
    if (showSoundPicker) {
        SoundPickerDialog(
            systemSounds = systemSounds,
            selectedSoundUri = selectedSoundUri,
            onSoundSelected = { uri ->
                onSelectedSoundUriChange(uri)
                viewModel.playPreviewSound(context, uri)
            },
            onDismiss = { showSoundPicker = false },
            viewModel = viewModel,
            context = context
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        StepIndicator(
            currentStep = 4,
            totalSteps = 4,
            stepTitle = stringResource(R.string.step4_title)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card de configuración de notificaciones
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.label_notification_settings),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Switch para vibración
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onEnableVibrationChange(!enableVibration) },
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = stringResource(R.string.vibration),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = stringResource(R.string.desc_vibration),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = enableVibration,
                                onCheckedChange = onEnableVibrationChange
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Switch para sonido
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onEnableSoundChange(!enableSound) },
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = stringResource(R.string.sound),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = stringResource(R.string.desc_sound),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = enableSound,
                                onCheckedChange = onEnableSoundChange
                            )
                        }
                    }

                    // Selector de tono (solo si el sonido está habilitado)
                    if (enableSound) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showSoundPicker = true },
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column {
                                        Text(
                                            text = stringResource(R.string.label_notification_tone),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = selectedSoundTitle,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                Icon(
                                    Icons.Default.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Resumen del recordatorio
            ReminderSummaryCard(
                title = title,
                description = description,
                reminderType = reminderType,
                selectedAddress = selectedAddress,
                selectedDays = selectedDays,
                selectedTime = selectedTime,
                proximityRadius = proximityRadius,
                triggerType = triggerType
            )
        }

        // Botones de navegación
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppButton(
                text = stringResource(R.string.back),
                leadingIcon = { Icon(Icons.Default.ArrowBack, contentDescription = null) },
                onClick = onBackClick,
                modifier = Modifier.weight(1f),
                outlined = true
            )
            AppButton(
                text = if (isEditMode) stringResource(R.string.edit) else stringResource(R.string.save),
                icon = Icons.Default.Check,
                onClick = {
                    // 🔥 VALIDACIONES CON NOTIFICACIONES
                    when {
                        viewModel.isLoading -> {
                            notificationViewModel.showError(context.getString(R.string.msg_saving_reminder))
                            return@AppButton
                        }

                        title.isBlank() -> {
                            notificationViewModel.showError(context.getString(R.string.error_empty_title))
                            return@AppButton
                        }

                        reminderType == "datetime" && selectedDays.isEmpty() -> {
                            notificationViewModel.showError(context.getString(R.string.error_select_at_least_one_day))
                            return@AppButton
                        }

                        reminderType == "datetime" && selectedTime == null -> {
                            notificationViewModel.showError(context.getString(R.string.error_select_time))
                            return@AppButton
                        }

                        reminderType == "location" && selectedAddress.isBlank() -> {
                            notificationViewModel.showError(context.getString(R.string.error_select_location_step))
                            return@AppButton
                        }

                        reminderType == "both" && (selectedDays.isEmpty() || selectedTime == null) -> {
                            notificationViewModel.showError(context.getString(R.string.error_complete_days_time))
                            return@AppButton
                        }

                        reminderType == "both" && selectedAddress.isBlank() -> {
                            notificationViewModel.showError(context.getString(R.string.error_select_location_step))
                            return@AppButton
                        }

                        else -> {
                            // ✅ Validaciones pasadas, crear o actualizar recordatorio
                            val timeStr = selectedTime?.let {
                                "${it.first.toString().padStart(2, '0')}:${
                                    it.second.toString().padStart(2, '0')
                                }:00"
                            }

                            val daysList =
                                if (selectedDays.isEmpty()) null else selectedDays.toList()

                            val reminder = Reminder(
                                title = title,
                                description = description.ifEmpty { null },
                                reminder_type = reminderType,
                                trigger_type = triggerType,
                                vibration = enableVibration,
                                sound = enableSound,
                                sound_uri = if (enableSound) selectedSoundUri else null,
                                days = daysList,
                                time = timeStr,
                                location = selectedAddress,
                                latitude = latitude,
                                longitude = longitude,
                                radius = proximityRadius.toDouble()
                            )

                            Log.d("Step4", "🔵 Reminder ANTES de actualizar:")
                            Log.d("Step4", "   reminder_type = ${reminder.reminder_type}")
                            Log.d("Step4", "   title = ${reminder.title}")
                            Log.d("Step4", "   days = ${reminder.days}")
                            Log.d("Step4", "   time = ${reminder.time}")

                            // 🔥 DECIDIR SI CREAR O ACTUALIZAR
                            if (isEditMode && editReminderId != null) {
                                // MODO EDICIÓN
                                viewModel.updateReminder(
                                    editReminderId,
                                    reminder,
                                    context
                                ) { success ->
                                    if (success) {
                                        notificationViewModel.showSuccess(context.getString(R.string.success_reminder_updated))
                                        onSaveSuccess()
                                    } else {
                                        val errorMessage =
                                            viewModel.error.value ?: context.getString(R.string.error_reminder_update_fail)
                                        notificationViewModel.showError(errorMessage)
                                    }
                                }
                            } else {
                                // MODO CREACIÓN
                                viewModel.createReminder(reminder, context) { success ->
                                    if (success) {
                                        notificationViewModel.showSuccess(context.getString(R.string.success_reminder_created))
                                        onSaveSuccess()
                                    } else {
                                        val errorMessage = viewModel.error.value
                                            ?: context.getString(R.string.error_reminder_create_fail)
                                        notificationViewModel.showError(errorMessage)
                                    }
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !viewModel.isLoading
            )
        }
    }
}

@Composable
fun SoundPickerDialog(
    systemSounds: List<NotificationSound>,
    selectedSoundUri: String,
    onSoundSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    viewModel: ReminderViewModel,
    context: Context
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close), color = MaterialTheme.colorScheme.primary)
            }
        },
        title = {
            Text(
                stringResource(R.string.title_select_tone),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                systemSounds.forEach { sound ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSoundSelected(sound.uri) },
                        color = if (selectedSoundUri == sound.uri)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                RadioButton(
                                    selected = selectedSoundUri == sound.uri,
                                    onClick = { onSoundSelected(sound.uri) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Text(
                                    text = if (sound.uri == "") stringResource(R.string.sound_default_system) else sound.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (selectedSoundUri == sound.uri)
                                        MaterialTheme.colorScheme.onSurface
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (selectedSoundUri == sound.uri)
                                        FontWeight.SemiBold
                                    else
                                        FontWeight.Normal
                                )
                            }

                            IconButton(
                                onClick = {
                                    onSoundSelected(sound.uri)
                                    viewModel.playPreviewSound(context, sound.uri)
                                }
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = stringResource(R.string.cd_play_sound),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun ReminderSummaryCard(
    title: String,
    description: String,
    reminderType: String,
    selectedAddress: String,
    selectedDays: Set<String>,
    selectedTime: Pair<Int, Int>?,
    proximityRadius: Float,
    triggerType: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = stringResource(R.string.title_summary),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Título
            SummaryItem(label = stringResource(R.string.label_title), value = title)

            // Descripción (si existe)
            if (description.isNotEmpty()) {
                SummaryItem(label = stringResource(R.string.label_description), value = description)
            }

            // Tipo
            SummaryItem(
                label = stringResource(R.string.label_reminder_type),
                value = when (reminderType) {
                    "location" -> stringResource(R.string.summary_type_location)
                    "datetime" -> stringResource(R.string.summary_type_datetime)
                    "both" -> stringResource(R.string.summary_type_both)
                    else -> reminderType
                }
            )

            // Ubicación
            if (reminderType == "location" || reminderType == "both") {
                SummaryItem(label = stringResource(R.string.cd_location), value = selectedAddress)
                SummaryItem(
                    label = stringResource(R.string.label_radius_meters),
                    value = "${proximityRadius.toInt()}m - ${
                        when (triggerType) {
                            "enter" -> stringResource(R.string.trigger_enter_short)
                            "exit" -> stringResource(R.string.trigger_exit_short)
                            else -> stringResource(R.string.trigger_both_short)
                        }
                    }"
                )
            }

            // Días y hora
            if (reminderType == "datetime" || reminderType == "both") {
                val daysOfWeekMap = mapOf(
                    "Lunes" to R.string.day_monday,
                    "Martes" to R.string.day_tuesday,
                    "Miércoles" to R.string.day_wednesday,
                    "Jueves" to R.string.day_thursday,
                    "Viernes" to R.string.day_friday,
                    "Sábado" to R.string.day_saturday,
                    "Domingo" to R.string.day_sunday
                )
                
                SummaryItem(
                    label = stringResource(R.string.label_days_of_week),
                    value = if (selectedDays.size == 7) {
                        stringResource(R.string.all_days_summary)
                    } else {
                        selectedDays.map { stringResource(daysOfWeekMap[it] ?: R.string.day_monday) }.joinToString(", ")
                    }
                )
                selectedTime?.let {
                    SummaryItem(
                        label = stringResource(R.string.label_time),
                        value = String.format("%02d:%02d", it.first, it.second)
                    )
                }
            }
        }
    }
}

@Composable
fun SummaryItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}