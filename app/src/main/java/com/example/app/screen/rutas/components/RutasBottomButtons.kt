package com.example.app.screen.rutas.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Text
import com.example.app.ui.theme.getBackgroundGradient

@Composable
fun RutasBottomButtons(
    modifier: Modifier = Modifier,
    onAgregarClick: () -> Unit = {},
    onRutasClick: () -> Unit = {},
    onUbicacionClick: () -> Unit = {}
) {
    val isDarkTheme = isSystemInDarkTheme()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(getBackgroundGradient(isDarkTheme)) // fondo degradado
            .padding(vertical = 12.dp)
            .navigationBarsPadding(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Botón Agregar ubicación
        FloatingActionButton(
            onClick = onAgregarClick,
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
            modifier = Modifier.size(56.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Agregar")
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Agregar",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        // Botón Rutas alternas
        FloatingActionButton(
            onClick = onRutasClick,
            containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f),
            modifier = Modifier.size(56.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(imageVector = Icons.Default.Directions, contentDescription = "Rutas")
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Rutas",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        // Botón centrar GPS
        FloatingActionButton(
            onClick = onUbicacionClick,
            containerColor = if (isDarkTheme) Color(0xFF64B5F6).copy(alpha = 0.9f)
            else Color(0xFF1976D2).copy(alpha = 0.9f),
            modifier = Modifier.size(56.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(imageVector = Icons.Default.LocationOn, contentDescription = "GPS")
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "GPS",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
