package com.example.app.screen.recordatorios.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.app.screen.components.AppBackButton
import com.example.app.screen.components.AppButton
import com.example.app.screen.rutas.components.CompactLocationCard
import com.example.app.ui.theme.getBackgroundGradient

@Composable
fun ReminderMapButtons(
    navController: NavController,
    modifier: Modifier = Modifier,
    selectedAddress: String = "",
    onConfirmClick: () -> Unit = {},
    onRecenterClick: () -> Unit = {},
    onZoomInClick: () -> Unit = {},
    onZoomOutClick: () -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    Box(modifier = modifier.fillMaxSize()) {

        // Columna superior: botón de regresar + card compacta
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .statusBarsPadding()
        ) {
            // Botón de regreso
            AppBackButton(
                navController = navController,
                onClick = onBackClick
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Card compacta de ubicación seleccionada
            if (selectedAddress.isNotEmpty()) {
                CompactLocationCard(
                    title = "Ubicación seleccionada",
                    location = selectedAddress,
                    icon = Icons.Default.LocationOn,
                    iconColor = Color(0xFFEF4444)
                )
            }
        }

        // Columna de botones de control del mapa (lado derecho)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ➕ Zoom In
            MapControlButton(
                icon = Icons.Default.Add,
                onClick = onZoomInClick
            )

            // ➖ Zoom Out
            MapControlButton(
                icon = Icons.Default.Remove,
                onClick = onZoomOutClick
            )

            // 🎯 Centrar mapa
            MapControlButton(
                icon = Icons.Default.MyLocation,
                onClick = onRecenterClick
            )
        }

        // Botón de confirmar (inferior)
        val canConfirm = selectedAddress.isNotEmpty()
        AppButton(
            text = "Confirmar ubicación",
            icon = Icons.Default.Check,
            onClick = onConfirmClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .imePadding()
                .navigationBarsPadding()
                .padding(16.dp),
            enabled = canConfirm,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
fun MapControlButton(
    icon: ImageVector,
    onClick: () -> Unit
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        contentColor = MaterialTheme.colorScheme.primary,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
    }
}