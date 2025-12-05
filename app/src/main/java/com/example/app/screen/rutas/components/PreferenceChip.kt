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