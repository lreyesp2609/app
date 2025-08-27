package com.example.app.screen.rutas.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MapBottomButtons(
    modifier: Modifier = Modifier,
    onConfirmClick: () -> Unit = {}
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        FloatingActionButton(
            onClick = onConfirmClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .navigationBarsPadding(),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Confirmar ubicación",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}