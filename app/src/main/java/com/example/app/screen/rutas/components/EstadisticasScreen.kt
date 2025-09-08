package com.example.app.screen.rutas.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.app.models.EstadisticasResponse
import com.example.app.repository.RutasRepository
import kotlinx.coroutines.launch

@Composable
fun EstadisticasScreen(
    ubicacionId: Int,
    token: String,
    isDarkTheme: Boolean,
    primaryColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    navController: NavController
) {
    val repository = remember { RutasRepository() }
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var estadisticas by remember { mutableStateOf<EstadisticasResponse?>(null) }

    var isVisible by remember { mutableStateOf(false) }
    val accentColor = Color(0xFFFF6B6B)

    val logoScale by animateFloatAsState(if (isVisible) 1f else 0f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    val logoRotation by animateFloatAsState(if (isVisible) 0f else 360f,
        tween(1000))

    val backgroundGradient = if (isDarkTheme)
        Brush.verticalGradient(listOf(Color(0xFF1A1A2E), Color(0xFF16213E)))
    else Brush.verticalGradient(listOf(Color(0xFFF8F9FA), Color(0xFFE3F2FD)))

    LaunchedEffect(ubicacionId) {
        isLoading = true
        isVisible = true
        val result = repository.obtenerEstadisticas(token, ubicacionId)
        result.fold(
            onSuccess = { estadisticas = it; isLoading = false },
            onFailure = { e -> error = e.message; isLoading = false }
        )
    }

    // SIN SCAFFOLD - Solo contenido con navegación simple
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .statusBarsPadding() // Respeta la status bar
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // TopAppBar personalizada con botón de regreso
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón de regreso
                IconButton(
                    onClick = { navController.popBackStack() }
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Volver",
                        tint = primaryColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Logo animado
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .scale(logoScale)
                        .rotate(logoRotation),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(40.dp)
                    )
                    Icon(
                        Icons.Default.AccessAlarm,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier
                            .size(16.dp)
                            .offset(12.dp, (-12).dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Título
                AnimatedVisibility(visible = isVisible) {
                    Column {
                        Text(
                            "RecuerdaGo",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                        Text(
                            "Estadísticas",
                            fontSize = 14.sp,
                            color = textColor.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Contenido principal
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = primaryColor,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Cargando estadísticas...",
                                color = textColor.copy(alpha = 0.7f),
                                fontSize = 16.sp
                            )
                        }
                    }
                }
                error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Error: $error",
                                color = Color.Red,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    isLoading = true
                                    error = null
                                    estadisticas = null
                                    scope.launch {
                                        repository.obtenerEstadisticas(token, ubicacionId).fold(
                                            onSuccess = { estadisticas = it; isLoading = false },
                                            onFailure = { e -> error = e.message; isLoading = false }
                                        )
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                            ) {
                                Text("Reintentar")
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        // Resumen general
                        item {
                            val completadas = estadisticas?.rutas_completadas ?: 0
                            val canceladas = estadisticas?.rutas_canceladas ?: 0
                            val total = (completadas + canceladas).takeIf { it > 0 } ?: 1
                            val completionPercent = completadas.toFloat() / total

                            StatCard("Resumen General", isDarkTheme, textColor, primaryColor) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    StatItem(
                                        total.toString(),
                                        "Total de rutas",
                                        Icons.Default.DirectionsCar,
                                        textColor,
                                        primaryColor
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Completadas vs Canceladas", fontSize = 14.sp, color = textColor)
                                    LinearProgressIndicator(
                                        progress = completionPercent,
                                        color = primaryColor,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "$completadas completadas / $canceladas canceladas",
                                        fontSize = 12.sp,
                                        color = textColor.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }

                        // Tiempo promedio por tipo
                        item {
                            StatCard("Tiempo promedio por tipo de ruta", isDarkTheme, textColor, primaryColor) {
                                estadisticas?.tiempo_promedio_por_tipo?.forEach { (tipo, tiempo) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(tipo, color = textColor, fontSize = 14.sp)

                                        val tiempoFormateado = when {
                                            tiempo >= 60 -> "${(tiempo / 60).toInt()} min ${(tiempo % 60).toInt()} seg"
                                            else -> "${tiempo.toInt()} seg"
                                        }

                                        Text(
                                            tiempoFormateado,
                                            color = primaryColor,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }
                        }

                        // Ruta más usada
                        item {
                            val rutaMasUsada = estadisticas?.bandits?.maxByOrNull { it.total_usos }?.tipo_ruta ?: "N/A"
                            StatCard("Ruta más usada", isDarkTheme, textColor, primaryColor) {
                                Text(
                                    "Ruta: $rutaMasUsada",
                                    color = primaryColor,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    isDarkTheme: Boolean,
    textColor: Color,
    primaryColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) Color(0xFF2D2D44) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    color = textColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(primaryColor, CircleShape)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun StatItem(
    value: String,
    label: String,
    icon: ImageVector,
    textColor: Color,
    primaryColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = label, tint = primaryColor, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = value, color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = textColor.copy(alpha = 0.7f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp
        )
    }
}
