package com.rutai.app.screen.rutas.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

import android.content.Context
import com.rutai.app.R

// ✅ Función NORMAL (sin @Composable) - para usar en ViewModel
fun getPreferenceDisplayName(context: Context, preference: String): String {
    return when (preference) {
        "fastest" -> context.getString(R.string.pref_fastest)
        "shortest" -> context.getString(R.string.pref_shortest)
        "recommended" -> context.getString(R.string.pref_recommended)
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