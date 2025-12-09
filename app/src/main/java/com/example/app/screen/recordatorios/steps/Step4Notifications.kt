package com.example.app.screen.recordatorios.steps

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.app.models.NotificationSound
import com.example.app.models.Reminder
import com.example.app.screen.components.AppButton
import com.example.app.viewmodel.NotificationViewModel
import com.example.app.viewmodel.ReminderViewModel
import com.example.app.viewmodel.rememberSystemNotificationSounds

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
        systemSounds.find { it.uri == selectedSoundUri }?.title ?: "Predeterminado del sistema"
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
            stepTitle = "Notificaciones"
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
                            text = "Configuración de notificación",
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
                                        text = "Vibración",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Vibrar al recibir notificación",
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
                                        text = "Sonido",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Reproducir tono de notificación",
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
                                            text = "Tono de notificación",
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
                text = "Atrás",
                leadingIcon = { Icon(Icons.Default.ArrowBack, contentDescription = null) },
                onClick = onBackClick,
                modifier = Modifier.weight(1f),
                outlined = true
            )
            AppButton(
                text = if (isEditMode) "Actualizar" else "Guardar", // 🔥 Cambiar texto según modo
                icon = Icons.Default.Check,
                onClick = {
                    // 🔥 VALIDACIONES CON NOTIFICACIONES
                    when {
                        viewModel.isLoading -> {
                            notificationViewModel.showError("Espera, guardando recordatorio...")
                            return@AppButton
                        }

                        title.isBlank() -> {
                            notificationViewModel.showError("El título no puede estar vacío")
                            return@AppButton
                        }

                        reminderType == "datetime" && selectedDays.isEmpty() -> {
                            notificationViewModel.showError("Debes seleccionar al menos un día")
                            return@AppButton
                        }

                        reminderType == "datetime" && selectedTime == null -> {
                            notificationViewModel.showError("Debes seleccionar una hora")
                            return@AppButton
                        }

                        reminderType == "location" && selectedAddress.isBlank() -> {
                            notificationViewModel.showError("Debes seleccionar una ubicación")
                            return@AppButton
                        }

                        reminderType == "both" && (selectedDays.isEmpty() || selectedTime == null) -> {
                            notificationViewModel.showError("Debes completar días y hora")
                            return@AppButton
                        }

                        reminderType == "both" && selectedAddress.isBlank() -> {
                            notificationViewModel.showError("Debes seleccionar una ubicación")
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
                                        notificationViewModel.showSuccess("¡Recordatorio actualizado!")
                                        onSaveSuccess()
                                    } else {
                                        val errorMessage =
                                            viewModel.error.value ?: "Error al actualizar"
                                        notificationViewModel.showError(errorMessage)
                                    }
                                }
                            } else {
                                // MODO CREACIÓN
                                viewModel.createReminder(reminder, context) { success ->
                                    if (success) {
                                        notificationViewModel.showSuccess("¡Recordatorio creado!")
                                        onSaveSuccess()
                                    } else {
                                        val errorMessage = viewModel.error.value
                                            ?: "Error al crear el recordatorio"
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
                Text("Cerrar", color = MaterialTheme.colorScheme.primary)
            }
        },
        title = {
            Text(
                "Seleccionar tono",
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
                                    text = sound.title,
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
                                    contentDescription = "Reproducir",
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
                    text = "Resumen",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Título
            SummaryItem(label = "Título", value = title)

            // Descripción (si existe)
            if (description.isNotEmpty()) {
                SummaryItem(label = "Descripción", value = description)
            }

            // Tipo
            SummaryItem(
                label = "Tipo",
                value = when (reminderType) {
                    "location" -> "Por ubicación"
                    "datetime" -> "Por fecha y hora"
                    "both" -> "Ubicación + Fecha y hora"
                    else -> reminderType
                }
            )

            // Ubicación
            if (reminderType == "location" || reminderType == "both") {
                SummaryItem(label = "Ubicación", value = selectedAddress)
                SummaryItem(
                    label = "Radio",
                    value = "${proximityRadius.toInt()}m - ${
                        when (triggerType) {
                            "enter" -> "Al entrar"
                            "exit" -> "Al salir"
                            else -> "Al entrar o salir"
                        }
                    }"
                )
            }

            // Días y hora
            if (reminderType == "datetime" || reminderType == "both") {
                SummaryItem(
                    label = "Días",
                    value = if (selectedDays.size == 7) "Todos los días" else selectedDays.joinToString(", ")
                )
                selectedTime?.let {
                    SummaryItem(
                        label = "Hora",
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