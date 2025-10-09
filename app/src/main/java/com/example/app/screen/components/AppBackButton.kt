package com.example.app.screen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun AppBackButton(
    navController: NavController,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
    iconColor: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null
) {
    IconButton(
        onClick = { onClick?.invoke() ?: navController.popBackStack() },
        modifier = modifier
            .size(48.dp)
            .background(backgroundColor, RoundedCornerShape(12.dp))
    ) {
        Icon(
            imageVector = Icons.Default.ArrowBack,
            contentDescription = "Volver",
            tint = iconColor
        )
    }
}
