package com.example.app.screen.rutas.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.models.RouteAlternative
import com.example.app.models.ValidarRutasResponse
import kotlin.collections.forEach

@Composable
fun RouteAlternativesDialogWithSecurity(
    alternatives: List<RouteAlternative>,
    validacionSeguridad: ValidarRutasResponse?,
    transportMode: String,
    onSelectRoute: (RouteAlternative) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedRoute by remember { mutableStateOf<RouteAlternative?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Selecciona tu ruta",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Advertencia general si ninguna es segura
                validacionSeguridad?.advertenciaGeneral?.let { advertencia ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
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
                                color = Color(0xFFD32F2F)
                            )
                        }
                    }
                }

                // Lista de rutas con chips mejorados
                alternatives.forEach { route ->
                    RouteChipCard(
                        route = route,
                        isSelected = selectedRoute == route,
                        onClick = {
                            selectedRoute = route
                        }
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
                enabled = selectedRoute != null
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}