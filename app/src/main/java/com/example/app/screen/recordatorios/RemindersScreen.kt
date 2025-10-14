package com.example.app.screen.recordatorios

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.app.models.Reminder
import com.example.app.network.AppDatabase
import com.example.app.screen.components.AppButton
import com.example.app.viewmodel.ReminderRepository
import com.example.app.viewmodel.ReminderViewModel
import com.example.app.viewmodel.ReminderViewModelFactory
import kotlinx.coroutines.delay

@Composable
fun RemindersScreen(
    navController: NavController,
    token: String,
    modifier: Modifier = Modifier
) {
    // Obtener el contexto
    val context = LocalContext.current

    // Crear el repositorio y el ViewModel con el factory
    val database = AppDatabase.getDatabase(context)
    val repository = ReminderRepository(database.reminderDao())
    val viewModel: ReminderViewModel = viewModel(
        factory = ReminderViewModelFactory(repository)
    )

    var showContent by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var reminderToDelete by remember { mutableStateOf<Reminder?>(null) }

    // Animación de entrada
    LaunchedEffect(Unit) {
        delay(200)
        showContent = true
        viewModel.fetchReminders(token)
        delay(400)
        showStats = true
    }

    val reminders = viewModel.reminders
    val isLoading = viewModel.isLoading
    val hasReminders = reminders.isNotEmpty()

    // Diálogo de confirmación de eliminación
    if (showDeleteDialog && reminderToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(text = "Eliminar recordatorio")
            },
            text = {
                Text(text = "¿Estás seguro de que deseas eliminar \"${reminderToDelete?.title}\"?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        reminderToDelete?.id?.let { id ->
                            viewModel.deleteReminder(context, id) {
                                showDeleteDialog = false
                                reminderToDelete = null
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header con título y botón
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(animationSpec = tween(600)) +
                        slideInVertically(initialOffsetY = { -it })
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Recordatorios",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Recordatorios",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    AppButton(
                        text = "Agregar recordatorio",
                        icon = Icons.Default.AddLocationAlt,
                        onClick = { navController.navigate("reminder_map") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Estadísticas rápidas (solo si hay recordatorios)
            AnimatedVisibility(
                visible = showStats && hasReminders,
                enter = fadeIn(animationSpec = tween(600)) +
                        expandVertically(animationSpec = tween(600)),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        icon = Icons.Default.CheckCircle,
                        value = reminders.count().toString(),
                        label = "Total",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        icon = Icons.Default.LocationOn,
                        value = reminders.count { it.reminder_type == "location" || it.reminder_type == "both" }.toString(),
                        label = "Ubicación",
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        icon = Icons.Default.Schedule,
                        value = reminders.count { it.reminder_type == "datetime" || it.reminder_type == "both" }.toString(),
                        label = "Fecha/Hora",
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Contenido principal
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(animationSpec = tween(800, delayMillis = 400)) +
                        slideInVertically(initialOffsetY = { it / 2 })
            ) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Cargando recordatorios...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    !hasReminders -> {
                        // Estado vacío con características
                        EmptyRemindersState()
                    }

                    else -> {
                        // Lista de recordatorios
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                count = reminders.size,
                                key = { index -> reminders[index].hashCode() }
                            ) { index ->
                                val reminder = reminders[index]
                                ReminderCard(
                                    reminder = reminder,
                                    onClick = { /* TODO: Navegar a detalle */ },
                                    onEdit = { /* TODO: Editar */ },
                                    onDelete = {
                                        reminderToDelete = reminder
                                        showDeleteDialog = true
                                    },
                                    onToggleActive = { isActive ->
                                        reminder.id?.let { id ->
                                            viewModel.toggleReminderActive(id, isActive, context)
                                        }
                                    }
                                )
                            }

                            // Espaciador final
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyRemindersState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Ícono animado
        var iconScale by remember { mutableStateOf(0f) }
        LaunchedEffect(Unit) {
            iconScale = 1f
        }

        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(
                    animateFloatAsState(
                        targetValue = iconScale,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ), label = ""
                    ).value
                )
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.secondaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AccessAlarm,
                contentDescription = "Alarma",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Card de características
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Características",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                ReminderFeatureItem(
                    icon = Icons.Default.LocationOn,
                    text = "Recordatorios basados en ubicación",
                    description = "Recibe alertas al entrar o salir de un lugar"
                )
                Spacer(modifier = Modifier.height(16.dp))

                ReminderFeatureItem(
                    icon = Icons.Default.Schedule,
                    text = "Alertas personalizadas por horario",
                    description = "Programa recordatorios en fecha y hora específica"
                )
                Spacer(modifier = Modifier.height(16.dp))

                ReminderFeatureItem(
                    icon = Icons.Default.Notifications,
                    text = "Notificaciones personalizables",
                    description = "Configura vibración, sonido y tipo de alerta"
                )
                Spacer(modifier = Modifier.height(16.dp))

                ReminderFeatureItem(
                    icon = Icons.Default.Map,
                    text = "Selección en mapa interactivo",
                    description = "Elige ubicaciones fácilmente con el mapa"
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun ReminderCard(
    reminder: Reminder,
    onClick: () -> Unit = {},
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onToggleActive: (Boolean) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    var isActive by remember(reminder.is_active) { mutableStateOf(reminder.is_active) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header del recordatorio con Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Ícono según tipo
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = when (reminder.reminder_type) {
                                    "location" -> MaterialTheme.colorScheme.tertiaryContainer
                                    "datetime" -> MaterialTheme.colorScheme.secondaryContainer
                                    else -> MaterialTheme.colorScheme.primaryContainer
                                },
                                shape = CircleShape
                            )
                            .alpha(if (isActive) 1f else 0.5f),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (reminder.reminder_type) {
                                "location" -> Icons.Default.LocationOn
                                "datetime" -> Icons.Default.Schedule
                                else -> Icons.Default.Notifications
                            },
                            contentDescription = null,
                            tint = when (reminder.reminder_type) {
                                "location" -> MaterialTheme.colorScheme.tertiary
                                "datetime" -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = reminder.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(
                                alpha = if (isActive) 1f else 0.5f
                            )
                        )
                        Text(
                            text = when (reminder.reminder_type) {
                                "location" -> "Ubicación"
                                "datetime" -> "Fecha y hora"
                                else -> "Ubicación y fecha"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(
                                alpha = if (isActive) 0.6f else 0.3f
                            )
                        )
                    }
                }

                // Switch para activar/desactivar
                Switch(
                    checked = isActive,
                    onCheckedChange = { newState ->
                        isActive = newState
                        onToggleActive(newState)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Contraer" else "Expandir",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Detalles expandibles
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // Descripción
                    reminder.description?.let { desc ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // Ubicación
                    if (reminder.reminder_type == "location" || reminder.reminder_type == "both") {
                        reminder.location?.let { loc ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Place,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = loc,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Radio: ${reminder.radius?.toInt() ?: 0}m • ${
                                            when (reminder.trigger_type) {
                                                "enter" -> "Al entrar"
                                                "exit" -> "Al salir"
                                                else -> "Al entrar o salir"
                                            }
                                        }",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }

                    // Fecha y hora
                    if (reminder.reminder_type == "datetime" || reminder.reminder_type == "both") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${reminder.days ?: "-"} • ${reminder.time ?: "-"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Configuración de notificación
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        if (reminder.vibration) {
                            Chip(label = "Vibración", icon = Icons.Default.Vibration)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        if (reminder.sound) {
                            Chip(
                                label = when (reminder.sound_type) {
                                    "gentle" -> "Sonido suave"
                                    "alert" -> "Alerta"
                                    "chime" -> "Campanilla"
                                    else -> "Sonido"
                                },
                                icon = Icons.Default.VolumeUp
                            )
                        }
                    }

                    // Botones de acción
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onEdit,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Editar")
                        }

                        OutlinedButton(
                            onClick = onDelete,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Eliminar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Chip(
    label: String,
    icon: ImageVector
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun ReminderFeatureItem(
    icon: ImageVector,
    text: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.align(Alignment.CenterVertically)
        ) {
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                lineHeight = 16.sp
            )
        }
    }
}