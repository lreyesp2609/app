package com.rutai.app.screen.recordatorios.steps

import android.content.Context
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.res.stringResource
import com.rutai.app.R
import com.rutai.app.screen.components.AppButton
import com.rutai.app.screen.components.AppSlider
import com.rutai.app.screen.components.AppTextField
import com.rutai.app.viewmodel.NotificationViewModel
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polygon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Step3ReminderType(
    reminderType: String,
    selectedDays: Set<String>,
    selectedTime: Pair<Int, Int>?,
    proximityRadius: Float,
    triggerType: String,
    selectedAddress: String,
    latitude: Double,  // 🆕 Nuevo parámetro
    longitude: Double, // 🆕 Nuevo parámetro
    onReminderTypeChange: (String) -> Unit,
    onSelectedDaysChange: (Set<String>) -> Unit,
    onSelectedTimeChange: (Pair<Int, Int>?) -> Unit,
    onProximityRadiusChange: (Float) -> Unit,
    onTriggerTypeChange: (String) -> Unit,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
    notificationViewModel: NotificationViewModel
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
                    Text(stringResource(R.string.ok), color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.cancel))
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
            stepTitle = stringResource(R.string.step3_title)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Tipo de recordatorio
            Text(
                text = stringResource(R.string.label_reminder_type),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppButton(
                    text = stringResource(R.string.type_location_short),
                    onClick = {
                        onReminderTypeChange("location")
                        showDaysError = false
                        showTimeError = false
                    },
                    modifier = Modifier.weight(1f),
                    outlined = reminderType != "location"
                )

                AppButton(
                    text = stringResource(R.string.type_time_short),
                    onClick = {
                        onReminderTypeChange("datetime")
                    },
                    modifier = Modifier.weight(1f),
                    outlined = reminderType != "datetime"
                )

                AppButton(
                    text = stringResource(R.string.type_both_short),
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
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = when (reminderType) {
                            "location" -> stringResource(R.string.desc_location_trigger)
                            "datetime" -> stringResource(R.string.desc_datetime_trigger)
                            "both" -> stringResource(R.string.desc_both_trigger)
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Configuración según tipo
            when (reminderType) {
                "location" -> {
                    LocationProximityConfig(
                        selectedAddress = selectedAddress,
                        proximityRadius = proximityRadius,
                        triggerType = triggerType,
                        latitude = latitude,
                        longitude = longitude,
                        onProximityRadiusChange = onProximityRadiusChange,
                        onTriggerTypeChange = onTriggerTypeChange
                    )
                }

                // 🔥 AGREGAR ESTE CASO
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

                "both" -> {
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

                    LocationProximityConfig(
                        selectedAddress = selectedAddress,
                        proximityRadius = proximityRadius,
                        triggerType = triggerType,
                        latitude = latitude,
                        longitude = longitude,
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
                text = stringResource(R.string.back),
                leadingIcon = { Icon(Icons.Default.ArrowBack, contentDescription = null) },
                onClick = onBackClick,
                modifier = Modifier.weight(1f),
                outlined = true
            )
            AppButton(
                text = stringResource(R.string.next),
                icon = Icons.Default.ArrowForward,
                onClick = {
                    var hasError = false

                    if (reminderType == "datetime" || reminderType == "both") {
                        if (selectedDays.isEmpty()) {
                            showDaysError = true
                            hasError = true
                            scope.launch {
                                scrollState.animateScrollTo(scrollState.maxValue)
                            }
                            notificationViewModel.showError(context.getString(R.string.error_select_at_least_one_day))
                        }
                        if (selectedTime == null) {
                            showTimeError = true
                            hasError = true
                            notificationViewModel.showError(context.getString(R.string.error_select_time))
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
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(R.string.label_days_of_week),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (showDaysError)
                    MaterialTheme.colorScheme.errorContainer
                else
                    MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (showDaysError) 0.dp else 2.dp
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val daysOfWeekMap = mapOf(
                    "Lunes" to R.string.day_monday,
                    "Martes" to R.string.day_tuesday,
                    "Miércoles" to R.string.day_wednesday,
                    "Jueves" to R.string.day_thursday,
                    "Viernes" to R.string.day_friday,
                    "Sábado" to R.string.day_saturday,
                    "Domingo" to R.string.day_sunday
                )

                // Botones rápidos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onSelectedDaysChange(daysOfWeekMap.keys) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Text(stringResource(R.string.all_days))
                    }
                    OutlinedButton(
                        onClick = { onSelectedDaysChange(emptySet()) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text(stringResource(R.string.clear))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Días individuales
                daysOfWeekMap.forEach { (dayKey, dayResId) ->
                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onSelectedDaysChange(
                                    if (selectedDays.contains(dayKey)) {
                                        selectedDays - dayKey
                                    } else {
                                        selectedDays + dayKey
                                    }
                                )
                            },
                        color = if (selectedDays.contains(dayKey))
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(dayResId),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (selectedDays.contains(dayKey))
                                    FontWeight.SemiBold
                                else
                                    FontWeight.Normal,
                                color = if (selectedDays.contains(dayKey))
                                    MaterialTheme.colorScheme.onSurface
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Checkbox(
                                checked = selectedDays.contains(dayKey),
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }

                if (showDaysError) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.error_select_at_least_one_day),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // Selector de hora
        Text(
            text = stringResource(R.string.label_time),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Column {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onTimePickerClick),
                color = if (showTimeError)
                    MaterialTheme.colorScheme.errorContainer
                else
                    MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp,
                    if (showTimeError)
                        MaterialTheme.colorScheme.error
                    else if (selectedTime != null)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.outline
                )
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
                            Icons.Default.Notifications,
                            contentDescription = null,
                            tint = if (selectedTime != null)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = if (selectedTime != null) {
                                String.format("%02d:%02d", selectedTime.first, selectedTime.second)
                            } else stringResource(R.string.select_time_prompt),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (selectedTime != null) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selectedTime != null)
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (showTimeError) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.padding(start = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.error_select_time),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
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
    latitude: Double,          // 🆕 Nuevo parámetro
    longitude: Double,         // 🆕 Nuevo parámetro
    onProximityRadiusChange: (Float) -> Unit,
    onTriggerTypeChange: (String) -> Unit
) {
    val minRadius = 20f
    val maxRadius = 5000f
    var radiusInput by remember { mutableStateOf(proximityRadius.toInt().toString()) }

    LaunchedEffect(proximityRadius) {
        val currentRadiusText = proximityRadius.toInt().toString()
        if (radiusInput != currentRadiusText) {
            radiusInput = currentRadiusText
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(R.string.label_proximity_config),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Ubicación
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = selectedAddress,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 🗺️ MAPA DE PREVIEW DEL RADIO
                ProximityMapPreview(
                    latitude = latitude,
                    longitude = longitude,
                    radiusMeters = proximityRadius,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                AppSlider(
                    value = proximityRadius,
                    onValueChange = { newValue ->
                        val roundedValue = newValue.toInt().toFloat()
                        radiusInput = roundedValue.toInt().toString()
                        onProximityRadiusChange(roundedValue)
                    },
                    valueRange = minRadius..maxRadius,
                    steps = 0,
                    label = stringResource(R.string.label_proximity_radius),
                    modifier = Modifier.fillMaxWidth()
                )

                AppTextField(
                    value = radiusInput,
                    onValueChange = { newValue ->
                        val digitsOnly = newValue.filter { it.isDigit() }
                        radiusInput = digitsOnly

                        val parsedValue = digitsOnly.toIntOrNull() ?: return@AppTextField
                        val normalizedValue = parsedValue.coerceIn(minRadius.toInt(), maxRadius.toInt()).toFloat()
                        onProximityRadiusChange(normalizedValue)
                    },
                    label = stringResource(R.string.label_radius_meters),
                    placeholder = stringResource(R.string.placeholder_radius),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = stringResource(R.string.label_activate_when),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppButton(
                        text = stringResource(R.string.trigger_enter_short),
                        onClick = { onTriggerTypeChange("enter") },
                        modifier = Modifier.weight(1f),
                        outlined = triggerType != "enter"
                    )
                    AppButton(
                        text = stringResource(R.string.trigger_exit_short),
                        onClick = { onTriggerTypeChange("exit") },
                        modifier = Modifier.weight(1f),
                        outlined = triggerType != "exit"
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                AppButton(
                    text = stringResource(R.string.trigger_both_short),
                    onClick = { onTriggerTypeChange("both") },
                    modifier = Modifier.fillMaxWidth(),
                    outlined = triggerType != "both"
                )
            }
        }
    }
}


@Composable
fun ProximityMapPreview(
    latitude: Double,
    longitude: Double,
    radiusMeters: Float,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mapView = rememberProximityMapView(context, latitude, longitude)

    // Actualizar el círculo cuando cambie el radio
    LaunchedEffect(radiusMeters) {
        updateProximityCircle(mapView, latitude, longitude, radiusMeters)
    }

    // Inicializar configuración de OSM
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(
            context,
            context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp)),
            factory = { mapView },
            update = { map ->
                // Actualizar overlays
                updateProximityCircle(map, latitude, longitude, radiusMeters)
            }
        )

        // Overlay con información del radio
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "${radiusMeters.toInt()}m",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Indicador de ubicación central
        Icon(
            Icons.Default.LocationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .align(Alignment.Center)
                .size(32.dp)
                .offset(y = (-16).dp) // Ajuste para que la punta del pin esté en el centro
        )
    }
}

@Composable
private fun rememberProximityMapView(
    context: Context,
    latitude: Double,
    longitude: Double
): MapView {
    return remember(latitude, longitude) {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(false) // 🔥 Deshabilitar interacción
            setBuiltInZoomControls(false)
            isFlingEnabled = false
            isVerticalMapRepetitionEnabled = false
            isHorizontalMapRepetitionEnabled = false

            // Centrar en la ubicación
            controller.setCenter(GeoPoint(latitude, longitude))
            controller.setZoom(16.0)
        }
    }
}

private fun updateProximityCircle(
    mapView: MapView,
    latitude: Double,
    longitude: Double,
    radiusMeters: Float
) {
    try {
        mapView.overlays.clear()

        val center = GeoPoint(latitude, longitude)

        // Crear puntos del círculo
        val circlePoints = crearCirculoProximidad(
            lat = latitude,
            lon = longitude,
            radioMetros = radiusMeters.toInt()
        )

        // Círculo de proximidad (relleno semitransparente)
        val circle = Polygon(mapView).apply {
            points = circlePoints
            fillPaint.color = android.graphics.Color.parseColor("#4400BCD4") // Cyan semitransparente
            outlinePaint.color = android.graphics.Color.parseColor("#FF00BCD4") // Cyan sólido
            outlinePaint.strokeWidth = 3f
        }
        mapView.overlays.add(circle)

        // Ajustar zoom para que el círculo sea visible
        val zoomLevel = calculateZoomLevel(radiusMeters)
        mapView.controller.setZoom(zoomLevel)
        mapView.controller.setCenter(center)

        mapView.invalidate()

        Log.d("ProximityMapPreview", "✅ Círculo actualizado: ${radiusMeters.toInt()}m, zoom: $zoomLevel")

    } catch (e: Exception) {
        Log.e("ProximityMapPreview", "❌ Error actualizando círculo: ${e.message}", e)
    }
}

private fun crearCirculoProximidad(
    lat: Double,
    lon: Double,
    radioMetros: Int
): List<GeoPoint> {
    val puntos = mutableListOf<GeoPoint>()
    val numPuntos = 32

    val radioGradosLat = radioMetros / 111320.0
    val radioGradosLon = radioMetros / (111320.0 * Math.cos(Math.toRadians(lat)))

    for (i in 0..numPuntos) {
        val angulo = 2 * Math.PI * i / numPuntos
        val newLat = lat + (radioGradosLat * Math.cos(angulo))
        val newLon = lon + (radioGradosLon * Math.sin(angulo))
        puntos.add(GeoPoint(newLat, newLon))
    }

    return puntos
}

// Calcular nivel de zoom apropiado según el radio
private fun calculateZoomLevel(radiusMeters: Float): Double {
    return when {
        radiusMeters <= 200f -> 17.0
        radiusMeters <= 500f -> 16.0
        radiusMeters <= 1000f -> 15.0
        radiusMeters <= 2000f -> 14.5
        radiusMeters <= 3000f -> 14.0
        else -> 13.5
    }
}