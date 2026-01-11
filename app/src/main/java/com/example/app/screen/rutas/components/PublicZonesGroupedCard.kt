package com.example.app.screen.rutas.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.models.ZonaPublicaDetectada
import kotlinx.coroutines.delay

@Composable
fun PublicZonesGroupedCard(
    zones: List<ZonaPublicaDetectada>,
    onSaveZone: (Int) -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var guardadasLocalmente by remember { mutableStateOf(setOf<Int>()) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3CD)
        ),
        border = BorderStroke(1.5.dp, Color(0xFFFFC107))
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // ═══════════════════════════════════════════
            // HEADER PRINCIPAL (siempre visible)
            // ═══════════════════════════════════════════
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            "Zonas reportadas por otros usuarios",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF856404)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "${zones.size} zona(s) detectada(s) en esta ruta",
                            fontSize = 11.sp,
                            color = Color(0xFF856404).copy(alpha = 0.7f)
                        )
                    }
                }

                // Icono expandir/contraer
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Ocultar" else "Mostrar",
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(24.dp)
                )
            }

            // ═══════════════════════════════════════════
            // CONTENIDO EXPANDIBLE (lista de zonas)
            // ═══════════════════════════════════════════
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = tween(300)) + fadeIn(),
                exit = shrinkVertically(animationSpec = tween(300)) + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    zones.forEachIndexed { index, zone ->
                        PublicZoneItemCompact(
                            zone = zone,
                            isGuardada = zone.zonaId in guardadasLocalmente,
                            onSave = {
                                onSaveZone(zone.zonaId)
                                guardadasLocalmente = guardadasLocalmente + zone.zonaId
                            },
                            isDarkTheme = isDarkTheme
                        )

                        // Separador entre zonas (excepto la última)
                        if (index < zones.size - 1) {
                            HorizontalDivider(
                                color = Color(0xFFFF9800).copy(alpha = 0.2f),
                                thickness = 1.dp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // ═══════════════════════════════════════════
            // RESUMEN CUANDO ESTÁ COLAPSADO
            // ═══════════════════════════════════════════
            if (!expanded) {
                Spacer(Modifier.height(8.dp))

                // Mostrar niveles de peligro de todas las zonas
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Niveles:",
                        fontSize = 11.sp,
                        color = Color(0xFF856404).copy(alpha = 0.7f)
                    )

                    zones.take(5).forEach { zone ->  // Máximo 5 para no saturar
                        Box(
                            modifier = Modifier
                                .background(
                                    Color(0xFFFF9800).copy(alpha = 0.3f),
                                    CircleShape
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "${zone.nivelPeligro}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF856404)
                            )
                        }
                    }

                    if (zones.size > 5) {
                        Text(
                            "+${zones.size - 5}",
                            fontSize = 10.sp,
                            color = Color(0xFF856404).copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// COMPONENTE COMPACTO PARA CADA ZONA INDIVIDUAL
// ═══════════════════════════════════════════════════════════
@Composable
private fun PublicZoneItemCompact(
    zone: ZonaPublicaDetectada,
    isGuardada: Boolean,
    onSave: () -> Unit,
    isDarkTheme: Boolean
) {
    var guardando by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Nombre + Nivel
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    zone.nombre,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF856404)
                )
                Spacer(Modifier.height(4.dp))

                // Detalles en una línea
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Nivel de peligro
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            "Nivel ${zone.nivelPeligro}",
                            fontSize = 11.sp,
                            color = Color(0xFF856404).copy(alpha = 0.8f)
                        )
                    }

                    // Distancia
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Place,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            "${String.format("%.1f", zone.distanciaKm)} km",
                            fontSize = 11.sp,
                            color = Color(0xFF856404).copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Botón de acción
            if (!isGuardada && zone.puedeGuardar) {
                IconButton(
                    onClick = {
                        guardando = true
                        onSave()
                    },
                    enabled = !guardando,
                    modifier = Modifier.size(36.dp)
                ) {
                    if (guardando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color(0xFFFF9800),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Guardar zona",
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else if (isGuardada) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Guardada",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    // Listener para marcar como guardada
    LaunchedEffect(guardando) {
        if (guardando) {
            delay(500)
            guardando = false
        }
    }
}