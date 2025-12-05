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
                                "Zona Peligrosa",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = onCancelar, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, "Cerrar", modifier = Modifier.size(20.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Nombre compacto
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { if (it.length <= 50) nombre = it },
                        label = { Text("Nombre", fontSize = 13.sp) },
                        placeholder = { Text("Ej: Callejón oscuro", fontSize = 13.sp) },
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
                                Text("Radio", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "$radio m",
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
                                Text("Nivel", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Box(
                                    modifier = Modifier
                                        .background(
                                            getNivelPeligroColor(nivel),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        "$nivel/5",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (nivel >= 4) Color.White else Color.Black
                                    )
                                }
                            }
                            Slider(
                                value = nivel.toFloat(),
                                onValueChange = { nivel = it.toInt() },
                                valueRange = 1f..5f,
                                steps = 3,
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
                        Text("Tipo", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "asalto" to "🔪",
                                "trafico" to "🚗",
                                "oscuro" to "🌙",
                                "otro" to "⚠️"
                            ).forEach { (t, emoji) ->
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
                                                t.replaceFirstChar { it.uppercase() },
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
                            "Agregar notas",
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
                                placeholder = { Text("Detalles adicionales...", fontSize = 12.sp) },
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
                            Text("Cancelar", fontSize = 14.sp)
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
                            Text("Marcar", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

fun getNivelPeligroColor(nivel: Int): Color {
    return when (nivel) {
        1 -> Color(0xFF4CAF50)
        2 -> Color(0xFFFFEB3B)
        3 -> Color(0xFFFF9800)
        4 -> Color(0xFFFF5722)
        5 -> Color(0xFFF44336)
        else -> Color.Gray
    }
}
/**
 * Devuelve el nombre legible del modo de transporte
 */
fun getModeDisplayName(mode: String): String {
    return when (mode) {
        "foot-walking" -> "Caminar"
        "driving-car" -> "Carro"
        "cycling-regular" -> "Bicicleta"
        else -> "Caminar"
    }
}