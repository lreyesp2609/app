package com.example.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun getBackgroundGradient(isDarkTheme: Boolean): Brush {
    return if (isDarkTheme) {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF1A1A2E), Color(0xFF16213E))
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(Color(0xFFF8F9FA), Color(0xFFE3F2FD))
        )
    }
}
