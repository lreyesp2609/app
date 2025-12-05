package com.example.app.screen.rutas.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.models.RouteAlternative
import com.example.app.models.ValidarRutasResponse
import kotlin.math.roundToInt

@Composable
fun RouteSelectorDialog(
    alternativeRoutes: List<RouteAlternative>,
    validacionSeguridad: ValidarRutasResponse?,
    isRegenerating: Boolean,
    rutasGeneradasEvitandoZonas: Boolean,
    onRouteSelected: (RouteAlternative) -> Unit,
    onRegenerarEvitandoZonas: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    "Selecciona tu ruta",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
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
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Rutas evitando zonas peligrosas",
                            fontSize = 12.sp,
                            color = Color(0xFF10B981),
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
                    .verticalScroll(rememberScrollState())
            ) {
                // 🚨 Advertencia general de seguridad
                validacionSeguridad?.advertenciaGeneral?.let { advertencia ->
                    if (!rutasGeneradasEvitandoZonas) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFEBEE)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color.Red,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    advertencia,
                                    fontSize = 13.sp,
                                    color = Color(0xFFD32F2F),
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                // 🔄 BOTÓN DE REGENERAR (solo si NO se ha regenerado aún)
                if (!rutasGeneradasEvitandoZonas &&
                    validacionSeguridad?.totalZonasUsuario != null &&
                    validacionSeguridad.totalZonasUsuario > 0) {

                    Button(
                        onClick = onRegenerarEvitandoZonas,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(bottom = 16.dp),
                        enabled = !isRegenerating,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isRegenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generando rutas seguras...")
                        } else {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "🔄 Regenerar evitando ${validacionSeguridad.totalZonasUsuario} zona(s) peligrosa(s)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // 📍 Lista de rutas alternativas
                alternativeRoutes.forEach { route ->
                    RutaCard(
                        route = route,
                        onClick = { onRouteSelected(route) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}