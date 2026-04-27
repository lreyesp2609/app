package com.rutai.app.screen.rutas.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.rutai.app.models.RouteAlternative
import kotlin.math.roundToInt
import com.rutai.app.R
@Composable
fun RutaCard(
    route: RouteAlternative,
    onClick: () -> Unit,
    onSavePublicZone: (Int) -> Unit,
    isDarkTheme: Boolean = isSystemInDarkTheme()
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                route.isRecommended -> Color(0xFFE8F5E9)
                route.esSegura == false -> SecurityColors.getDangerBackground(isDarkTheme)
                else -> Color.White
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // HEADER: Nombre + Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    route.displayName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Badge ML
                    if (route.isRecommended) {
                        Badge(containerColor = Color(0xFF4CAF50)) {
                            Text(stringResource(R.string.route_ml_badge), fontSize = 10.sp, color = Color.White)
                        }
                    }

                    // Badge de Seguridad
                    route.esSegura?.let { esSegura ->
                        Badge(
                            containerColor = if (esSegura) Color(0xFF4CAF50) else Color(0xFFF44336)
                        ) {
                            Text(
                                if (esSegura) stringResource(R.string.route_safe_badge) else stringResource(R.string.route_risk_badge),
                                fontSize = 10.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // DISTANCIA Y DURACIÓN
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.route_distance_km_format, (route.distance / 1000).roundToInt()), fontSize = 14.sp)
                Text(stringResource(R.string.route_duration_min_format, (route.duration / 60).toInt()), fontSize = 14.sp)
            }

            // MENSAJE DE SEGURIDAD (zonas PROPIAS)
            route.mensajeSeguridad?.let { mensaje ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    mensaje,
                    fontSize = 12.sp,
                    color = Color(0xFFF44336),
                    fontStyle = FontStyle.Italic
                )
            }

            // ZONAS PROPIAS DETECTADAS
            route.zonasDetectadas?.let { zonas ->
                if (zonas.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.route_danger_detected, zonas.size),
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            // 🚀 ZONAS PÚBLICAS AGRUPADAS
            route.zonasPublicasDetectadas?.let { zonasPublicas ->
                if (zonasPublicas.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))

                    PublicZonesGroupedCard(
                        zones = zonasPublicas,
                        onSaveZone = onSavePublicZone,
                        isDarkTheme = isDarkTheme
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// SecurityColors (si no lo tienes, cópialo aquí)
// ═══════════════════════════════════════════════════════════
object SecurityColors {
    fun getDangerBackground(isDarkTheme: Boolean): Color {
        return if (isDarkTheme) Color(0xFF3D1F1F) else Color(0xFFFFEBEE)
    }

    fun getDangerColor(isDarkTheme: Boolean): Color {
        return if (isDarkTheme) Color(0xFFEF5350) else Color(0xFFD32F2F)
    }

    fun getSafeBackground(isDarkTheme: Boolean): Color {
        return if (isDarkTheme) Color(0xFF1B3A1F) else Color(0xFFE8F5E9)
    }

    fun getSafeColor(isDarkTheme: Boolean): Color {
        return if (isDarkTheme) Color(0xFF66BB6A) else Color(0xFF2E7D32)
    }

    fun getWarningBackground(isDarkTheme: Boolean): Color {
        return if (isDarkTheme) Color(0xFF3D2F1F) else Color(0xFFFFF3CD)
    }

    fun getWarningColor(isDarkTheme: Boolean): Color {
        return if (isDarkTheme) Color(0xFFFFB74D) else Color(0xFFFF9800)
    }
}
