package com.example.app.utils

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoCrearZonaPeligrosa(
    coordenadas: Pair<Double, Double>,
    onConfirmar: (nombre: String, radio: Int, nivel: Int, tipo: String, notas: String?) -> Unit,
    onCancelar: () -> Unit,
    onRadioChanged: ((Int) -> Unit)? = null
) {
    var nombre by remember { mutableStateOf("") }
    var radio by remember { mutableStateOf(200) }
    var nivel by remember { mutableStateOf(3) }
    var tipo by remember { mutableStateOf("asalto") }
    var mostrarNotas by remember { mutableStateOf(false) }
    var notas by remember { mutableStateOf("") }

    // 🔥 Estado para detectar cuando se está ajustando el radio
    var ajustandoRadio by remember { mutableStateOf(false) }

    // Notificar cambios de radio al mapa
    LaunchedEffect(radio) {
        onRadioChanged?.invoke(radio)
    }

    // 🔥 Detectar cuando el teclado está visible
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val scrollState = rememberScrollState()

    // 🔥 Animación de transparencia
    val modalAlpha by animateFloatAsState(
        targetValue = if (ajustandoRadio) 0.3f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "modalAlpha"
    )

    // 🔥 MODAL ULTRA COMPACTO
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onCancelar
            )
    ) {
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .wrapContentHeight()
                .imePadding()
                .navigationBarsPadding()
                .alpha(modalAlpha) // 🔥 Aplicar transparencia animada
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                ),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 🔥 CONTENIDO SCROLLABLE
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(16.dp)
                ) {
                    // 🔥 HEADER MINIMALISTA
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(com.example.app.R.string.danger_zone_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = onCancelar, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, stringResource(com.example.app.R.string.close), modifier = Modifier.size(20.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Nombre compacto
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { if (it.length <= 50) nombre = it },
                        label = { Text(stringResource(com.example.app.R.string.group_name_field_label), fontSize = 13.sp) },
                        placeholder = { Text(stringResource(com.example.app.R.string.placeholder_danger_zone_name), fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Radio y Nivel en una fila
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Radio
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(stringResource(com.example.app.R.string.radius_label_simple), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "$radio ${stringResource(com.example.app.R.string.meters_unit).first()}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = radio.toFloat(),
                                onValueChange = {
                                    radio = it.toInt()
                                    ajustandoRadio = true // 🔥 Activar transparencia
                                },
                                onValueChangeFinished = {
                                    ajustandoRadio = false // 🔥 Restaurar opacidad
                                },
                                valueRange = 100f..500f,
                                steps = 7,
                                modifier = Modifier.height(32.dp)
                            )
                        }

                        // Nivel
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(stringResource(com.example.app.R.string.label_level), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Box(
                                    modifier = Modifier
                                        .background(
                                            getNivelPeligroColor(nivel),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        "$nivel/3",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White  // blanco siempre sobre verde/naranja/rojo
                                    )
                                }
                            }
                            Slider(
                                value = nivel.toFloat(),
                                onValueChange = { nivel = it.toInt() },
                                valueRange = 1f..3f,
                                steps = 1,
                                modifier = Modifier.height(32.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = getNivelPeligroColor(nivel),
                                    activeTrackColor = getNivelPeligroColor(nivel)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tipo de peligro - chips horizontales compactos
                    Column {
                        Text(stringResource(com.example.app.R.string.label_type), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "asalto" to "🔪" to com.example.app.R.string.type_assault,
                                "trafico" to "🚗" to com.example.app.R.string.type_traffic,
                                "oscuro" to "🌙" to com.example.app.R.string.type_dark,
                                "otro" to "⚠️" to com.example.app.R.string.type_other
                            ).forEach { (pair, resId) ->
                                val (t, emoji) = pair
                                FilterChip(
                                    selected = tipo == t,
                                    onClick = { tipo = t },
                                    label = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(emoji, fontSize = 14.sp)
                                            Text(
                                                stringResource(resId).replaceFirstChar { it.uppercase() },
                                                fontSize = 11.sp
                                            )
                                        }
                                    },
                                    modifier = Modifier.height(32.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Toggle para notas opcionales
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { mostrarNotas = !mostrarNotas }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(com.example.app.R.string.label_add_notes),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            if (mostrarNotas) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Notas expandibles
                    AnimatedVisibility(visible = mostrarNotas) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = notas,
                                onValueChange = { if (it.length <= 200) notas = it },
                                placeholder = { Text(stringResource(com.example.app.R.string.placeholder_notes), fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 2,
                                shape = RoundedCornerShape(10.dp),
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 🔥 BOTONES FIJOS FUERA DEL SCROLL (siempre visibles)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onCancelar,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(stringResource(com.example.app.R.string.cancel), fontSize = 14.sp)
                        }

                        Button(
                            onClick = {
                                onConfirmar(nombre, radio, nivel, tipo, notas.ifBlank { null })
                            },
                            enabled = nombre.isNotBlank(),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF5252)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(stringResource(com.example.app.R.string.button_mark), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

fun getNivelPeligroColor(nivel: Int): Color = when {
    nivel <= 1 -> Color(0xFF4CAF50)  // Verde
    nivel == 2 -> Color(0xFFFF9800)  // Naranja
    else       -> Color(0xFFF44336)  // Rojo
}


/**
 * Devuelve el nombre legible del modo de transporte
 */
fun getModeDisplayName(mode: String, context: android.content.Context): String {
    return when (mode) {
        "foot-walking" -> context.getString(com.example.app.R.string.mode_walking)
        "driving-car" -> context.getString(com.example.app.R.string.mode_driving)
        "cycling-regular" -> context.getString(com.example.app.R.string.mode_cycling)
        else -> context.getString(com.example.app.R.string.mode_walking)
    }
}