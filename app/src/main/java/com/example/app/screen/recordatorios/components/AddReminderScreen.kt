package com.example.app.screen.recordatorios.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import android.widget.Toast
import com.example.app.models.Reminder
import com.example.app.screen.components.AppBackButton
import com.example.app.screen.components.AppButton
import com.example.app.screen.components.AppSlider
import com.example.app.screen.components.AppTextField
import com.example.app.screen.recordatorios.ViewModel.ReminderViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderScreen(
    navController: NavController,
    selectedAddress: String,
    latitude: Double? = null,
    longitude: Double? = null,
    viewModel: ReminderViewModel = viewModel(),
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf<Long?>(null) }
    var selectedTime by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    // Campos para geolocalización
    var reminderType by remember { mutableStateOf("location") }
    var proximityRadius by remember { mutableStateOf(500f) }
    var triggerType by remember { mutableStateOf("enter") }

    // Configuración de notificaciones
    var enableVibration by remember { mutableStateOf(true) }
    var enableSound by remember { mutableStateOf(true) }
    var selectedSoundType by remember { mutableStateOf("default") }
    var showSoundPicker by remember { mutableStateOf(false) }

    // Estados de validación
    var showTitleError by remember { mutableStateOf(false) }
    var showDateError by remember { mutableStateOf(false) }
    var showTimeError by remember { mutableStateOf(false) }
    var showLocationError by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // DatePicker Dialog
    val datePickerState = rememberDatePickerState()
    var showDatePicker by remember { mutableStateOf(false) }

    // TimePicker Dialog
    var showTimePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDate = datePickerState.selectedDateMillis
                    showDatePicker = false
                    showDateError = false
                }) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState()
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedTime = Pair(timePickerState.hour, timePickerState.minute)
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

    // Dialog selector de sonido
    if (showSoundPicker) {
        AlertDialog(
            onDismissRequest = { showSoundPicker = false },
            confirmButton = {
                TextButton(onClick = { showSoundPicker = false }) {
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
                                .clickable {
                                    selectedSoundType = value
                                    viewModel.playPreviewSound(context, value)
                                }
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
                                    onClick = {
                                        selectedSoundType = value
                                        viewModel.playPreviewSound(context, value)
                                    }
                                )
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }

                            IconButton(onClick = {
                                selectedSoundType = value
                                viewModel.playPreviewSound(context, value)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppBackButton(navController = navController)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Nuevo recordatorio",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tarjeta de ubicación
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (showLocationError)
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                else
                    MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = if (showLocationError)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = selectedAddress,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (showLocationError)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                if (latitude != null && longitude != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Lat: ${String.format("%.6f", latitude)}, Lng: ${String.format("%.6f", longitude)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }
                if (showLocationError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⚠️ Ubicación no válida. Regresa y selecciona una ubicación.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Título con indicador de obligatorio
        Column {
            AppTextField(
                value = title,
                onValueChange = {
                    title = it
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

        // Descripción
        AppTextField(
            value = description,
            onValueChange = { description = it },
            label = "Descripción (opcional)",
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            maxLines = 5
        )

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
                    reminderType = "location"
                    // Resetear errores de fecha/hora
                    showDateError = false
                    showTimeError = false
                },
                modifier = Modifier.weight(1f),
                outlined = reminderType != "location"
            )

            AppButton(
                text = "Fecha",
                onClick = {
                    reminderType = "datetime"
                    // Resetear error de ubicación
                    showLocationError = false
                },
                modifier = Modifier.weight(1f),
                outlined = reminderType != "datetime"
            )

            AppButton(
                text = "Ambos",
                onClick = {
                    reminderType = "both"
                },
                modifier = Modifier.weight(1f),
                outlined = reminderType != "both"
            )
        }

        // Mensaje informativo según el tipo
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
                        "datetime" -> "Campos obligatorios: fecha y hora *"
                        "both" -> "Campos obligatorios: fecha, hora y ubicación *"
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }

        // Configuración de proximidad (solo si incluye ubicación)
        if (reminderType == "location" || reminderType == "both") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    AppSlider(
                        value = proximityRadius,
                        onValueChange = { proximityRadius = it },
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
                            text = "Entre a la zona",
                            onClick = { triggerType = "enter" },
                            modifier = Modifier.weight(1f),
                            outlined = triggerType != "enter"
                        )
                        AppButton(
                            text = "Salga de la zona",
                            onClick = { triggerType = "exit" },
                            modifier = Modifier.weight(1f),
                            outlined = triggerType != "exit"
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    AppButton(
                        text = "Entre o salga de la zona",
                        onClick = { triggerType = "both" },
                        modifier = Modifier.fillMaxWidth(),
                        outlined = triggerType != "both"
                    )
                }
            }
        }

        // Selector de fecha y hora (solo si incluye fecha)
        if (reminderType == "datetime" || reminderType == "both") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column {
                    AppButton(
                        text = if (selectedDate != null) {
                            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            dateFormat.format(Date(selectedDate!!))
                        } else "Seleccionar fecha *",
                        onClick = {
                            showDatePicker = true
                            if (selectedDate != null) showDateError = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        outlined = true,
                        leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                        trailingIcon = { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) }
                    )
                    AnimatedVisibility(
                        visible = showDateError,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Text(
                            text = "Debes seleccionar una fecha",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }
                }

                Column {
                    AppButton(
                        text = if (selectedTime != null) {
                            String.format("%02d:%02d", selectedTime!!.first, selectedTime!!.second)
                        } else "Seleccionar hora *",
                        onClick = {
                            showTimePicker = true
                            if (selectedTime != null) showTimeError = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        outlined = true,
                        leadingIcon = { Icon(Icons.Default.Notifications, contentDescription = null) },
                        trailingIcon = { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) }
                    )
                    AnimatedVisibility(
                        visible = showTimeError,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
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

        Spacer(modifier = Modifier.weight(1f))

        // Configuración de notificaciones
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
                        .clickable { enableVibration = !enableVibration }
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
                        onCheckedChange = { enableVibration = it }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Switch para sonido
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { enableSound = !enableSound }
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
                        onCheckedChange = { enableSound = it }
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
                        onClick = {
                            showSoundPicker = true
                            viewModel.playPreviewSound(context, selectedSoundType)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        outlined = true
                    )
                }
            }
        }

        // Botón guardar
        AppButton(
            text = "Guardar recordatorio",
            onClick = {
                // Validar campos según el tipo de recordatorio
                var hasError = false

                // Validar título (siempre obligatorio)
                if (title.isEmpty()) {
                    showTitleError = true
                    hasError = true
                    coroutineScope.launch {
                        scrollState.animateScrollTo(0) // Scroll al inicio donde está el título
                    }
                    Toast.makeText(context, "Debes ingresar un título", Toast.LENGTH_SHORT).show()
                    return@AppButton
                }

                // Validar fecha y hora si es necesario
                if (reminderType == "datetime" || reminderType == "both") {
                    if (selectedDate == null) {
                        showDateError = true
                        hasError = true
                        Toast.makeText(context, "Debes seleccionar una fecha", Toast.LENGTH_SHORT).show()
                        return@AppButton
                    }
                    if (selectedTime == null) {
                        showTimeError = true
                        hasError = true
                        Toast.makeText(context, "Debes seleccionar una hora", Toast.LENGTH_SHORT).show()
                        return@AppButton
                    }
                }

                // Validar ubicación si es necesario
                if ((reminderType == "location" || reminderType == "both") &&
                    (latitude == null || longitude == null)) {
                    showLocationError = true
                    hasError = true
                    coroutineScope.launch {
                        scrollState.animateScrollTo(0) // Scroll al inicio donde está la ubicación
                    }
                    Toast.makeText(context, "Debes seleccionar una ubicación válida", Toast.LENGTH_SHORT).show()
                    return@AppButton
                }

                // Si todo está bien, crear el recordatorio
                if (!hasError) {
                    val dateStr = selectedDate?.let {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        sdf.format(Date(it))
                    }
                    val timeStr = selectedTime?.let {
                        "${it.first.toString().padStart(2,'0')}:${it.second.toString().padStart(2,'0')}:00"
                    }

                    val reminder = Reminder(
                        title = title,
                        description = description.ifEmpty { null },
                        reminder_type = reminderType,
                        trigger_type = triggerType,
                        vibration = enableVibration,
                        sound = enableSound,
                        sound_type = if (enableSound) selectedSoundType else null,
                        date = dateStr,
                        time = timeStr,
                        location = selectedAddress,
                        latitude = latitude,
                        longitude = longitude,
                        radius = proximityRadius.toDouble()
                    )

                    viewModel.createReminder(reminder, context) {
                        navController.navigate("home?tab=2") {
                            popUpTo("home") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Check, contentDescription = null) },
            outlined = false
        )
    }
}