package com.example.app.screen.recordatorios.components

import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Looper
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.os.Handler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderScreen(
    navController: NavController,
    selectedAddress: String,
    latitude: Double? = null,
    longitude: Double? = null
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf<Long?>(null) }
    var selectedTime by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    // Campos para geolocalización
    var reminderType by remember { mutableStateOf("location") }
    var proximityRadius by remember { mutableStateOf(500f) }
    var triggerType by remember { mutableStateOf("enter") }

    // NUEVOS: Configuración de notificaciones
    var enableVibration by remember { mutableStateOf(true) }
    var enableSound by remember { mutableStateOf(true) }
    var selectedSoundType by remember { mutableStateOf("default") } // default, gentle, alert, chime
    var showSoundPicker by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // MediaPlayer para previsualizar sonidos
    val mediaPlayer = remember { MediaPlayer() }

    // Limpieza del MediaPlayer
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer.release()
        }
    }

    // Función para reproducir sonido de previsualización
    fun playPreviewSound() {
        try {
            // Detener cualquier reproducción en curso
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
            }
            mediaPlayer.reset()

            val soundUri = when (selectedSoundType) {
                "gentle" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                "alert" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                "chime" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }

            mediaPlayer.setDataSource(context, soundUri)
            mediaPlayer.prepare()

            // Configurar para que NO se repita
            mediaPlayer.isLooping = false

            // Detener automáticamente cuando termine
            mediaPlayer.setOnCompletionListener {
                it.reset()
            }

            mediaPlayer.start()

            // Limitar la duración a 3 segundos máximo
            Handler(Looper.getMainLooper()).postDelayed({
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                    mediaPlayer.reset()
                }
            }, 3000)

        } catch (e: Exception) {
            Toast.makeText(context, "Error al reproducir sonido", Toast.LENGTH_SHORT).show()
        }
    }

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
                                    playPreviewSound()
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
                                        playPreviewSound()
                                    }
                                )
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }

                            IconButton(onClick = {
                                selectedSoundType = value
                                playPreviewSound()
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
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
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
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = selectedAddress,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
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
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Título
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Nombre del recordatorio") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(Icons.Default.Edit, contentDescription = null)
            },
            singleLine = true
        )

        // Descripción
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Descripción (opcional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
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
            FilterChip(
                selected = reminderType == "location",
                onClick = { reminderType = "location" },
                label = { Text("Por ubicación") },
                leadingIcon = if (reminderType == "location") {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else null,
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = reminderType == "datetime",
                onClick = { reminderType = "datetime" },
                label = { Text("Por fecha") },
                leadingIcon = if (reminderType == "datetime") {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else null,
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = reminderType == "both",
                onClick = { reminderType = "both" },
                label = { Text("Ambos") },
                leadingIcon = if (reminderType == "both") {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else null,
                modifier = Modifier.weight(1f)
            )
        }

        // NUEVA SECCIÓN: Configuración de notificaciones
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
                            imageVector = Icons.Default.Add, // Usar como ícono de volumen
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

                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showSoundPicker = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        text = "Tono de notificación",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = when (selectedSoundType) {
                                            "gentle" -> "Suave"
                                            "alert" -> "Alerta"
                                            "chime" -> "Campanilla"
                                            else -> "Predeterminado"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Botón de reproducir
                                IconButton(
                                    onClick = { playPreviewSound() }
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = "Reproducir tono",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
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
        }

        // Configuración de proximidad (solo si incluye ubicación)
        if (reminderType == "location" || reminderType == "both") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Radio de proximidad",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = when {
                            proximityRadius < 1000 -> "${proximityRadius.toInt()} metros"
                            else -> "${(proximityRadius / 1000).let { String.format("%.1f", it) }} km"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )

                    Slider(
                        value = proximityRadius,
                        onValueChange = { proximityRadius = it },
                        valueRange = 100f..5000f,
                        steps = 19,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("100m", style = MaterialTheme.typography.bodySmall)
                        Text("5km", style = MaterialTheme.typography.bodySmall)
                    }

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
                        FilterChip(
                            selected = triggerType == "enter",
                            onClick = { triggerType = "enter" },
                            label = { Text("Entre a la zona") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = triggerType == "exit",
                            onClick = { triggerType = "exit" },
                            label = { Text("Salga de la zona") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    FilterChip(
                        selected = triggerType == "both",
                        onClick = { triggerType = "both" },
                        label = { Text("Entre o salga de la zona") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Selector de fecha y hora (solo si incluye fecha)
        if (reminderType == "datetime" || reminderType == "both") {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showDatePicker = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DateRange, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (selectedDate != null) {
                                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                dateFormat.format(Date(selectedDate!!))
                            } else {
                                "Seleccionar fecha"
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Icon(
                        Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showTimePicker = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (selectedTime != null) {
                                String.format("%02d:%02d", selectedTime!!.first, selectedTime!!.second)
                            } else {
                                "Seleccionar hora"
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Icon(
                        Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Botón guardar
        Button(
            onClick = {
                println("🕒 Recordatorio guardado:")
                println(" - Título: $title")
                println(" - Descripción: $description")
                println(" - Tipo: $reminderType")
                println(" - Ubicación: $selectedAddress")
                println(" - Coordenadas: Lat $latitude, Lng $longitude")
                println(" - Radio: ${proximityRadius}m")
                println(" - Activación: $triggerType")
                println(" - Fecha: ${selectedDate?.let { Date(it) }}")
                println(" - Hora: ${selectedTime?.let { "${it.first}:${it.second}" }}")
                println(" - Vibración: $enableVibration")
                println(" - Sonido: $enableSound")
                println(" - Tipo de sonido: $selectedSoundType")
                navController.popBackStack()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = title.isNotEmpty() &&
                    ((reminderType == "location" || reminderType == "both") ||
                            (reminderType == "datetime" && selectedDate != null && selectedTime != null)) &&
                    ((reminderType == "datetime" || reminderType == "both") ||
                            (reminderType == "location" && latitude != null && longitude != null)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Guardar recordatorio", style = MaterialTheme.typography.titleMedium)
        }
    }
}