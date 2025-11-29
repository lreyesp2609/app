package com.example.app.screen.recordatorios.steps

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
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
import com.example.app.models.Reminder
import com.example.app.screen.components.AppButton
import com.example.app.viewmodel.ReminderViewModel

@Composable
fun Step4Notifications(
    enableVibration: Boolean,
    enableSound: Boolean,
    selectedSoundType: String,
    onEnableVibrationChange: (Boolean) -> Unit,
    onEnableSoundChange: (Boolean) -> Unit,
    onSelectedSoundTypeChange: (String) -> Unit,
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
    onSaveSuccess: () -> Unit
) {
    val scrollState = rememberScrollState()
    var showSoundPicker by remember { mutableStateOf(false) }

    // Dialog selector de sonido
    if (showSoundPicker) {
        SoundPickerDialog(
            selectedSoundType = selectedSoundType,
            onSoundSelected = { soundType ->
                onSelectedSoundTypeChange(soundType)
                viewModel.playPreviewSound(context, soundType)
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
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Configuración de notificación",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Switch para vibración
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onEnableVibrationChange(!enableVibration) }
                            .padding(vertical = 8.dp),
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
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column {
                                Text(
                                    text = "Vibración",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Vibrar al recibir notificación",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                        Switch(
                            checked = enableVibration,
                            onCheckedChange = onEnableVibrationChange
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Switch para sonido
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onEnableSoundChange(!enableSound) }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column {
                                Text(
                                    text = "Sonido",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Reproducir tono de notificación",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                        Switch(
                            checked = enableSound,
                            onCheckedChange = onEnableSoundChange
                        )
                    }

                    // Selector de tono (solo si el sonido está habilitado)
                    if (enableSound) {
                        Spacer(modifier = Modifier.height(12.dp))

                        AppButton(
                            text = when (selectedSoundType) {
                                "gentle" -> "Suave"
                                "alert" -> "Alerta"
                                "chime" -> "Campanilla"
                                else -> "Predeterminado"
                            },
                            onClick = { showSoundPicker = true },
                            modifier = Modifier.fillMaxWidth(),
                            outlined = true,
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) }
                        )
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
                text = "Guardar",
                icon = Icons.Default.Check,
                onClick = {
                    if (viewModel.isLoading) return@AppButton

                    val timeStr = selectedTime?.let {
                        "${it.first.toString().padStart(2,'0')}:${it.second.toString().padStart(2,'0')}:00"
                    }

                    val daysList = if (selectedDays.isEmpty()) null else selectedDays.toList()

                    val reminder = Reminder(
                        title = title,
                        description = description.ifEmpty { null },
                        reminder_type = reminderType,
                        trigger_type = triggerType,
                        vibration = enableVibration,
                        sound = enableSound,
                        sound_type = if (enableSound) selectedSoundType else null,
                        days = daysList,
                        time = timeStr,
                        location = selectedAddress,
                        latitude = latitude,
                        longitude = longitude,
                        radius = proximityRadius.toDouble()
                    )

                    viewModel.createReminder(reminder, context) {
                        onSaveSuccess()
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
    selectedSoundType: String,
    onSoundSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    viewModel: ReminderViewModel,
    context: Context
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        },
        title = { Text("Seleccionar tono") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val soundOptions = listOf(
                    "default" to "Predeterminado",
                    "gentle" to "Suave",
                    "alert" to "Alerta",
                    "chime" to "Campanilla"
                )

                soundOptions.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSoundSelected(value) }
                            .background(
                                if (selectedSoundType == value)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    Color.Transparent
                            )
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RadioButton(
                                selected = selectedSoundType == value,
                                onClick = { onSoundSelected(value) }
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }

                        IconButton(onClick = {
                            onSoundSelected(value)
                        }) {
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
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Resumen",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

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
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}