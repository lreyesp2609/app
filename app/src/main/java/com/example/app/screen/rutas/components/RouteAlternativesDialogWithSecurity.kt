package com.example.app.screen.rutas.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            tint = SecurityColors.getSafeColor(isDarkTheme),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Rutas evitando zonas peligrosas",
                            fontSize = 12.sp,
                            color = SecurityColors.getSafeColor(isDarkTheme),
                            fontWeight = FontWeight.Medium
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 🚨 Advertencia general si ninguna es segura
                validacionSeguridad?.advertenciaGeneral?.let { advertencia ->
                    if (!rutasGeneradasEvitandoZonas) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = SecurityColors.getDangerBackground(isDarkTheme)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = SecurityColors.getDangerColor(isDarkTheme),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
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
                }

                // 🆕 BOTÓN DE REGENERAR
                if (!rutasGeneradasEvitandoZonas &&
                    validacionSeguridad?.totalZonasUsuario != null &&
                    validacionSeguridad.totalZonasUsuario > 0) {

                    Button(
                        onClick = onRegenerarEvitandoZonas,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !isRegenerating,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SecurityColors.getSafeColor(isDarkTheme),
                            contentColor = Color.White,
                            disabledContainerColor = SecurityColors.getSafeColor(isDarkTheme).copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp
                        )
                    ) {
                        if (isRegenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Generando rutas seguras...",
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Regenerar evitando ${validacionSeguridad.totalZonasUsuario} zona(s) peligrosa(s)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Lista de rutas
                alternatives.forEach { route ->
                    RouteChipCard(
                        route = route,
                        isSelected = selectedRoute == route,
                        onClick = { selectedRoute = route }
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
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Confirmar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Cancelar",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun RouteChipCard(
    route: RouteAlternative,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()

    // 🎨 Determinar colores según nivel de peligrosidad
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        route.esSegura == false -> SecurityColors.getDangerBackground(isDarkTheme)
        route.esSegura == true -> SecurityColors.getSafeBackground(isDarkTheme)
        route.isRecommended -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val borderColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        route.esSegura == false -> SecurityColors.getDangerColor(isDarkTheme)
        route.esSegura == true -> SecurityColors.getSafeColor(isDarkTheme)
        else -> Color.Transparent
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = borderColor
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = getPreferenceIcon(route.type),
                        contentDescription = null,
                        tint = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        },
                        modifier = Modifier.size(24.dp)
                    )

                    Text(
                        text = route.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Badge de ML
                    if (route.isRecommended) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                "🤖 ML",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Badge de seguridad (con colores progresivos)
                    route.esSegura?.let { esSegura ->
                        Badge(
                            containerColor = if (esSegura) {
                                SecurityColors.getSafeColor(isDarkTheme)
                            } else {
                                SecurityColors.getDangerColor(isDarkTheme)
                            }
                        ) {
                            Text(
                                if (esSegura) "✓ SEGURA" else "⚠ RIESGO",
                                fontSize = 10.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                InfoChip(
                    icon = Icons.Default.Straighten,
                    text = "${(route.distance / 1000).roundToInt()} km",
                    contentColor = contentColor
                )
                InfoChip(
                    icon = Icons.Default.Schedule,
                    text = "${(route.duration / 60).toInt()} min",
                    contentColor = contentColor
                )
            }

            // Mensaje de seguridad (con color según peligrosidad)
            route.mensajeSeguridad?.let { mensaje ->
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (route.esSegura == false) {
                            Icons.Default.Warning
                        } else {
                            Icons.Default.Info
                        },
                        contentDescription = null,
                        tint = if (route.esSegura == false) {
                            SecurityColors.getDangerColor(isDarkTheme)
                        } else {
                            SecurityColors.getWarningColor(isDarkTheme)
                        },
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        mensaje,
                        fontSize = 12.sp,
                        color = if (route.esSegura == false) {
                            SecurityColors.getDangerColor(isDarkTheme)
                        } else {
                            SecurityColors.getWarningColor(isDarkTheme)
                        },
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Zonas detectadas
            route.zonasDetectadas?.let { zonas ->
                if (zonas.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = SecurityColors.getDangerColor(isDarkTheme),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            "${zonas.size} zona(s) de riesgo detectada(s)",
                            fontSize = 11.sp,
                            color = SecurityColors.getDangerColor(isDarkTheme),
                            fontWeight = FontWeight.Medium
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
    text: String,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = contentColor
        )
    }
}