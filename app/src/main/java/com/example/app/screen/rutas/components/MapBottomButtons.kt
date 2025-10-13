package com.example.app.screen.rutas.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.app.ui.theme.getBackgroundGradient
import androidx.compose.ui.text.input.ImeAction
import com.example.app.screen.components.AppButton
import com.example.app.screen.components.AppTextField

@Composable
fun MapBottomButtons(
    modifier: Modifier = Modifier,
    userLocation: String = "",
    selectedLocation: String = "",
    locationName: String = "",
    onLocationNameChange: (String) -> Unit = {},
    onConfirmClick: () -> Unit = {}
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Cards de ubicación
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LocationCard(
                title = "Tu ubicación actual",
                location = userLocation,
                icon = Icons.Default.MyLocation,
                cardType = LocationCardType.Current
            )
            LocationCard(
                title = "Ubicación seleccionada",
                location = selectedLocation,
                icon = Icons.Default.LocationOn,
                cardType = LocationCardType.Selected
            )

            // Nuevo card para el nombre de la ubicación
            if (selectedLocation.isNotEmpty()) {
                LocationNameCard(
                    locationName = locationName,
                    onLocationNameChange = onLocationNameChange
                )
            }
        }

        // Botón de confirmar (habilitado solo si hay nombre y ubicación seleccionada)
        val canConfirm = selectedLocation.isNotEmpty() && locationName.trim().isNotEmpty()

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
fun LocationNameCard(
    locationName: String,
    onLocationNameChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val iconColor = Color(0xFF3B82F6)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box(
            modifier = Modifier
                .background(brush = getBackgroundGradient())
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(color = iconColor.copy(alpha = 0.2f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Nombre de la ubicación",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    AppTextField(
                        value = locationName,
                        onValueChange = { newValue ->
                            if (newValue.length <= 100) onLocationNameChange(newValue)
                        },
                        label = "Nombre de la ubicación",
                        placeholder = "ej. Casa, Trabajo...",
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = Color(0xFF3B82F6), // color de borde opcional
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${locationName.length}/100 caracteres",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

enum class LocationCardType {
    Current,
    Selected
}

@Composable
fun LocationCard(
    title: String,
    location: String,
    icon: ImageVector,
    cardType: LocationCardType,
    modifier: Modifier = Modifier
) {
    // Definir colores de icono según el tipo de card
    val iconColor = when (cardType) {
        LocationCardType.Current -> Color(0xFF10B981) // verde
        LocationCardType.Selected -> Color(0xFFEF4444) // rojo
    }

    Card(
        modifier = modifier
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
                // Icono con fondo circular
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(color = iconColor.copy(alpha = 0.2f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (location.isNotEmpty()) location else "Selecciona una ubicación",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (location.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(color = iconColor, shape = CircleShape)
                    )
                }
            }
        }
    }
}