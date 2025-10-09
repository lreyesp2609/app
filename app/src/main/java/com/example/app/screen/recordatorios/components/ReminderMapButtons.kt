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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.app.screen.components.AppBackButton
import com.example.app.screen.components.AppButton
import com.example.app.ui.theme.getBackgroundGradient

@Composable
fun ReminderMapButtons(
    navController: NavController,
    modifier: Modifier = Modifier,
    selectedAddress: String = "",
    onConfirmClick: () -> Unit = {},
    onRecenterClick: () -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    Box(modifier = modifier.fillMaxSize()) {

        // Columna superior: botón de regresar + card
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

            // Card de ubicación seleccionada
            if (selectedAddress.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .background(brush = getBackgroundGradient())
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = Color(0xFFEF4444).copy(alpha = 0.2f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Ubicación seleccionada",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = selectedAddress,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Botón flotante de recentrar (lado derecho)
        FloatingActionButton(
            onClick = onRecenterClick,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "Centrar mapa"
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
