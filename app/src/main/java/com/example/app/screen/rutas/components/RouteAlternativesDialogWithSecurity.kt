package com.example.app.screen.rutas.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.models.RouteAlternative
import com.example.app.models.ValidarRutasResponse
import com.example.app.ui.theme.SecurityColors
import kotlin.collections.forEach
import kotlin.math.roundToInt

@Composable
fun RouteAlternativesDialogWithSecurity(
    alternatives: List<RouteAlternative>,
    validacionSeguridad: ValidarRutasResponse?,
    transportMode: String,
    isRegenerating: Boolean = false,
    rutasGeneradasEvitandoZonas: Boolean = false,
    onSelectRoute: (RouteAlternative) -> Unit,
    onRegenerarEvitandoZonas: () -> Unit = {},
    onDismiss: () -> Unit
) {
    var selectedRoute by remember { mutableStateOf<RouteAlternative?>(null) }
    val isDarkTheme = isSystemInDarkTheme()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    "Selecciona tu ruta",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Indicador si ya se generaron rutas evitando zonas
                if (rutasGeneradasEvitandoZonas) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                SecurityColors.getSafeBackground(isDarkTheme),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            tint = SecurityColors.getSafeColor(isDarkTheme),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Rutas evitando zonas peligrosas",
                            fontSize = 13.sp,
                            color = SecurityColors.getSafeColor(isDarkTheme),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 🚨 Advertencia general - Ahora como banner sin Card
                validacionSeguridad?.advertenciaGeneral?.let { advertencia ->
                    if (!rutasGeneradasEvitandoZonas) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    SecurityColors.getDangerBackground(isDarkTheme),
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    1.dp,
                                    SecurityColors.getDangerColor(isDarkTheme).copy(alpha = 0.3f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = SecurityColors.getDangerColor(isDarkTheme),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                advertencia,
                                fontSize = 13.sp,
                                color = SecurityColors.getDangerColor(isDarkTheme),
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // 🆕 BOTÓN DE REGENERAR - Estilo más moderno
                if (!rutasGeneradasEvitandoZonas &&
                    validacionSeguridad?.totalZonasUsuario != null &&
                    validacionSeguridad.totalZonasUsuario > 0) {

                    Button(
                        onClick = onRegenerarEvitandoZonas,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        enabled = !isRegenerating,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SecurityColors.getSafeColor(isDarkTheme),
                            contentColor = Color.White,
                            disabledContainerColor = SecurityColors.getSafeColor(isDarkTheme).copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 3.dp,
                            pressedElevation = 6.dp,
                            disabledElevation = 0.dp
                        )
                    ) {
                        if (isRegenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Generando rutas seguras...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        } else {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Regenerar evitando ${validacionSeguridad.totalZonasUsuario} zona(s) peligrosa(s)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // 📍 Lista de rutas - Sin Cards anidadas
                alternatives.forEach { route ->
                    RouteChipCard(
                        route = route,
                        isSelected = selectedRoute == route,
                        onClick = { selectedRoute = route },
                        isDarkTheme = isDarkTheme
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedRoute?.let { onSelectRoute(it) }
                    onDismiss()
                },
                enabled = selectedRoute != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Confirmar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    "Cancelar",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun RouteChipCard(
    route: RouteAlternative,
    isSelected: Boolean,
    onClick: () -> Unit,
    isDarkTheme: Boolean
) {
    // 🎨 Colores simplificados y más claros
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        route.esSegura == false -> SecurityColors.getDangerBackground(isDarkTheme)
        route.esSegura == true -> SecurityColors.getSafeBackground(isDarkTheme)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    val borderColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        route.esSegura == false -> SecurityColors.getDangerColor(isDarkTheme)
        route.esSegura == true -> SecurityColors.getSafeColor(isDarkTheme)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }

    val borderWidth = if (isSelected) 2.5.dp else 1.dp

    // 🔲 Contenedor principal SIN Card - solo Box con bordes
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(14.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column {
            // 📌 Header: Nombre + Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icono + Nombre
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Círculo con icono
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getPreferenceIcon(route.type),
                            contentDescription = null,
                            tint = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = route.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Badges compactos
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Badge ML
                    if (route.isRecommended) {
                        Box(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "🤖 ML",
                                fontSize = 10.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // 🔥 Badge Seguridad SIEMPRE SE MUESTRA
                    Box(
                        modifier = Modifier
                            .background(
                                when {
                                    route.esSegura == true -> SecurityColors.getSafeColor(isDarkTheme)
                                    route.esSegura == false -> SecurityColors.getDangerColor(isDarkTheme)
                                    else -> SecurityColors.getWarningColor(isDarkTheme) // ← Para null
                                },
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            when {
                                route.esSegura == true -> "✓"
                                route.esSegura == false -> "⚠"
                                else -> "?" // ← Para null
                            },
                            fontSize = 11.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 📊 Info: Distancia y Tiempo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                InfoChip(
                    icon = Icons.Default.Straighten,
                    text = "${(route.distance / 1000).roundToInt()} km"
                )
                InfoChip(
                    icon = Icons.Default.Schedule,
                    text = "${(route.duration / 60).toInt()} min"
                )
            }

            // 💬 Mensaje de seguridad
            route.mensajeSeguridad?.let { mensaje ->
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (route.esSegura == false)
                                SecurityColors.getDangerColor(isDarkTheme).copy(alpha = 0.08f)
                            else
                                SecurityColors.getWarningColor(isDarkTheme).copy(alpha = 0.08f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (route.esSegura == false)
                            Icons.Default.Warning
                        else
                            Icons.Default.Info,
                        contentDescription = null,
                        tint = if (route.esSegura == false)
                            SecurityColors.getDangerColor(isDarkTheme)
                        else
                            SecurityColors.getWarningColor(isDarkTheme),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        mensaje,
                        fontSize = 12.sp,
                        color = if (route.esSegura == false)
                            SecurityColors.getDangerColor(isDarkTheme)
                        else
                            SecurityColors.getWarningColor(isDarkTheme),
                        fontWeight = FontWeight.Medium,
                        lineHeight = 16.sp
                    )
                }
            }

            // 🗺️ Zonas detectadas
            route.zonasDetectadas?.let { zonas ->
                if (zonas.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .background(
                                SecurityColors.getDangerColor(isDarkTheme).copy(alpha = 0.08f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = SecurityColors.getDangerColor(isDarkTheme),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "${zonas.size} zona(s) de riesgo detectada(s)",
                            fontSize = 12.sp,
                            color = SecurityColors.getDangerColor(isDarkTheme),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InfoChip(
    icon: ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}