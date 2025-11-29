package com.example.app.screen.recordatorios.steps

import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.app.screen.components.AppButton
import com.example.app.screen.components.AppSlider
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Step3ReminderType(
    reminderType: String,
    selectedDays: Set<String>,
    selectedTime: Pair<Int, Int>?,
    proximityRadius: Float,
    triggerType: String,
    selectedAddress: String,
    onReminderTypeChange: (String) -> Unit,
    onSelectedDaysChange: (Set<String>) -> Unit,
    onSelectedTimeChange: (Pair<Int, Int>?) -> Unit,
    onProximityRadiusChange: (Float) -> Unit,
    onTriggerTypeChange: (String) -> Unit,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    var showDaysError by remember { mutableStateOf(false) }
    var showTimeError by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // TimePicker Dialog
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedTime?.first ?: 0,
            initialMinute = selectedTime?.second ?: 0
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onSelectedTimeChange(Pair(timePickerState.hour, timePickerState.minute))
                    showTimePicker = false
                    showTimeError = false
                }) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancelar")
                }
            },
            text = {
                TimePicker(state = timePickerState)
            }
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
            currentStep = 3,
            totalSteps = 4,
            stepTitle = "Configuración del recordatorio"
        )

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tipo de recordatorio
            Text(
                text = "Tipo de recordatorio",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppButton(
                    text = "Ubicación",
                    onClick = {
                        onReminderTypeChange("location")
                        showDaysError = false
                        showTimeError = false
                    },
                    modifier = Modifier.weight(1f),
                    outlined = reminderType != "location"
                )

                AppButton(
                    text = "Hora",
                    onClick = {
                        onReminderTypeChange("datetime")
                    },
                    modifier = Modifier.weight(1f),
                    outlined = reminderType != "datetime"
                )

                AppButton(
                    text = "Ambos",
                    onClick = {
                        onReminderTypeChange("both")
                    },
                    modifier = Modifier.weight(1f),
                    outlined = reminderType != "both"
                )
            }

            // Mensaje informativo
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = when (reminderType) {
                            "location" -> "Se activará cuando llegues al lugar"
                            "datetime" -> "Se activará en los días y hora seleccionados"
                            "both" -> "Se activará cuando llegues al lugar en los días y hora seleccionados"
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }

            // Configuración según tipo
            when (reminderType) {
                "datetime" -> {
                    DaysAndTimeSelector(
                        selectedDays = selectedDays,
                        selectedTime = selectedTime,
                        showDaysError = showDaysError,
                        showTimeError = showTimeError,
                        onSelectedDaysChange = {
                            onSelectedDaysChange(it)
                            showDaysError = false
                        },
                        onTimePickerClick = { showTimePicker = true }
                    )
                }
                "location" -> {
                    LocationProximityConfig(
                        selectedAddress = selectedAddress,
                        proximityRadius = proximityRadius,
                        triggerType = triggerType,
                        onProximityRadiusChange = onProximityRadiusChange,
                        onTriggerTypeChange = onTriggerTypeChange
                    )
                }
                "both" -> {
                    // Primero días y hora
                    DaysAndTimeSelector(
                        selectedDays = selectedDays,
                        selectedTime = selectedTime,
                        showDaysError = showDaysError,
                        showTimeError = showTimeError,
                        onSelectedDaysChange = {
                            onSelectedDaysChange(it)
                            showDaysError = false
                        },
                        onTimePickerClick = { showTimePicker = true }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Luego configuración de ubicación
                    LocationProximityConfig(
                        selectedAddress = selectedAddress,
                        proximityRadius = proximityRadius,
                        triggerType = triggerType,
                        onProximityRadiusChange = onProximityRadiusChange,
                        onTriggerTypeChange = onTriggerTypeChange
                    )
                }
            }
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
                text = "Siguiente",
                icon = Icons.Default.ArrowForward,
                onClick = {
                    var hasError = false

                    // Validar según el tipo
                    if (reminderType == "datetime" || reminderType == "both") {
                        if (selectedDays.isEmpty()) {
                            showDaysError = true
                            hasError = true
                            scope.launch {
                                scrollState.animateScrollTo(scrollState.maxValue)
                            }
                            Toast.makeText(context, "Debes seleccionar al menos un día", Toast.LENGTH_SHORT).show()
                        }
                        if (selectedTime == null) {
                            showTimeError = true
                            hasError = true
                            Toast.makeText(context, "Debes seleccionar una hora", Toast.LENGTH_SHORT).show()
                        }
                    }

                    if (!hasError) {
                        onNextClick()
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// 📅 Selector de días y hora
@Composable
fun DaysAndTimeSelector(
    selectedDays: Set<String>,
    selectedTime: Pair<Int, Int>?,
    showDaysError: Boolean,
    showTimeError: Boolean,
    onSelectedDaysChange: (Set<String>) -> Unit,
    onTimePickerClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Días de la semana *",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (showDaysError)
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val daysOfWeek = listOf(
                    "Lunes", "Martes", "Miércoles", "Jueves",
                    "Viernes", "Sábado", "Domingo"
                )

                // Botones rápidos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppButton(
                        text = "Todos",
                        onClick = { onSelectedDaysChange(daysOfWeek.toSet()) },
                        modifier = Modifier.weight(1f),
                        outlined = true
                    )
                    AppButton(
                        text = "Limpiar",
                        onClick = { onSelectedDaysChange(emptySet()) },
                        modifier = Modifier.weight(1f),
                        outlined = true
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Días individuales
                daysOfWeek.forEach { day ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onSelectedDaysChange(
                                    if (selectedDays.contains(day)) {
                                        selectedDays - day
                                    } else {
                                        selectedDays + day
                                    }
                                )
                            }
                            .background(
                                if (selectedDays.contains(day))
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    Color.Transparent
                            )
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = day,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (selectedDays.contains(day))
                                FontWeight.SemiBold
                            else
                                FontWeight.Normal
                        )
                        Checkbox(
                            checked = selectedDays.contains(day),
                            onCheckedChange = null
                        )
                    }
                }

                if (showDaysError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Debes seleccionar al menos un día",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Selector de hora
        Text(
            text = "Hora *",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Column {
            AppButton(
                text = if (selectedTime != null) {
                    String.format("%02d:%02d", selectedTime.first, selectedTime.second)
                } else "Seleccionar hora",
                onClick = onTimePickerClick,
                modifier = Modifier.fillMaxWidth(),
                outlined = true,
                leadingIcon = { Icon(Icons.Default.Notifications, contentDescription = null) },
                trailingIcon = { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) }
            )

            if (showTimeError) {
                Text(
                    text = "Debes seleccionar una hora",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
        }
    }
}

// 📍 Configuración de proximidad
@Composable
fun LocationProximityConfig(
    selectedAddress: String,
    proximityRadius: Float,
    triggerType: String,
    onProximityRadiusChange: (Float) -> Unit,
    onTriggerTypeChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Configuración de proximidad",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Ubicación
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = selectedAddress,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                AppSlider(
                    value = proximityRadius,
                    onValueChange = onProximityRadiusChange,
                    valueRange = 100f..5000f,
                    steps = 19,
                    label = "Radio de proximidad",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Activar cuando:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppButton(
                        text = "Entre",
                        onClick = { onTriggerTypeChange("enter") },
                        modifier = Modifier.weight(1f),
                        outlined = triggerType != "enter"
                    )
                    AppButton(
                        text = "Salga",
                        onClick = { onTriggerTypeChange("exit") },
                        modifier = Modifier.weight(1f),
                        outlined = triggerType != "exit"
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                AppButton(
                    text = "Entre o salga",
                    onClick = { onTriggerTypeChange("both") },
                    modifier = Modifier.fillMaxWidth(),
                    outlined = triggerType != "both"
                )
            }
        }
    }
}