package com.example.app.screen.rutas.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// ✅ Función NORMAL (sin @Composable) - para usar en ViewModel
fun getPreferenceDisplayName(preference: String): String {
    return when (preference) {
        "fastest" -> "Más Rápida"
        "shortest" -> "Más Corta"
        "recommended" -> "Recomendada"
        else -> preference
    }
}

// ✅ Funciones Composable - solo para UI
@Composable
fun getPreferenceIcon(preference: String): ImageVector {
    return when (preference) {
        "fastest" -> Icons.Default.Speed
        "shortest" -> Icons.Default.Straighten
        "recommended" -> Icons.Default.Star
        else -> Icons.Default.Route
    }
}

@Composable
fun getPreferenceColor(preference: String, isDarkTheme: Boolean = isSystemInDarkTheme()): Color {
    return when (preference) {
        "fastest" -> if (isDarkTheme) Color(0xFF90CAF9) else Color(0xFF1976D2)
        "shortest" -> if (isDarkTheme) Color(0xFFA5D6A7) else Color(0xFF388E3C)
        "recommended" -> if (isDarkTheme) Color(0xFFFFD54F) else Color(0xFFF57C00)
        else -> MaterialTheme.colorScheme.primary
    }
}

// ✅ Componente visual mejorado
@Composable
fun PreferenceChip(
    preference: String,
    isSelected: Boolean = false,
    isRecommended: Boolean = false,
    onClick: () -> Unit = {}
) {
    val isDarkTheme = isSystemInDarkTheme()

    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isRecommended -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    val contentColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isRecommended -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = Modifier
            .padding(4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        tonalElevation = if (isSelected) 4.dp else 1.dp,
        shadowElevation = if (isSelected) 4.dp else 0.dp,
        border = if (!isSelected) {
            BorderStroke(
                width = if (isRecommended) 2.dp else 1.dp,
                color = if (isRecommended) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                }
            )
        } else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = getPreferenceIcon(preference),
                contentDescription = null,
                tint = if (isSelected) contentColor else getPreferenceColor(preference, isDarkTheme),
                modifier = Modifier.size(20.dp)
            )

            Column {
                Text(
                    text = getPreferenceDisplayName(preference),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = contentColor
                )

                // Badge de "Recomendada" si aplica
                if (isRecommended && !isSelected) {
                    Text(
                        text = "ML Recomienda",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}