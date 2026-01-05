package com.example.app.screen.grupos.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.app.ui.theme.AppColors

@Composable
fun MiniSwipeIndicator(
    currentPage: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = isSystemInDarkTheme()

    Surface(
        modifier = modifier
            .size(48.dp)
            .clickable(
                indication = ripple(bounded = false, radius = 24.dp),
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            ),
        shape = CircleShape,
        color = if (isDarkTheme) {
            AppColors.Indigo.copy(alpha = 0.9f)
        } else {
            AppColors.InfoDark  // #2563EB - Azul oscuro vibrante
        },
        shadowElevation = 4.dp
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = if (currentPage == 0) {
                    Icons.Default.Map
                } else {
                    Icons.AutoMirrored.Filled.Chat
                },
                contentDescription = if (currentPage == 0) "Ir al mapa" else "Volver al chat",
                tint = Color.White,  // Siempre blanco para contraste
                modifier = Modifier.size(24.dp)
            )
        }
    }
}