package com.example.app.screen.recordatorios.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.app.screen.components.AppBackButton
import com.example.app.screen.components.AppButton
import com.example.app.screen.mapa.MapControlButton
import com.example.app.screen.rutas.components.CompactLocationCard

@Composable
fun Step1SelectLocation(
    navController: NavController,
    selectedAddress: String,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
    onRecenterClick: () -> Unit,
    onZoomInClick: () -> Unit,
    onZoomOutClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .statusBarsPadding()
        ) {
            AppBackButton(
                navController = navController,
                onClick = onBackClick
            )

            Spacer(modifier = Modifier.height(12.dp))

            StepIndicator(
                currentStep = 1,
                totalSteps = 4,
                stepTitle = "Seleccionar ubicación"
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedAddress.isNotEmpty()) {
                CompactLocationCard(
                    title = "Ubicación seleccionada",
                    location = selectedAddress,
                    icon = Icons.Default.LocationOn,
                    iconColor = Color(0xFFEF4444)
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MapControlButton(icon = Icons.Default.Add, onClick = onZoomInClick)
            MapControlButton(icon = Icons.Default.Remove, onClick = onZoomOutClick)
            MapControlButton(icon = Icons.Default.MyLocation, onClick = onRecenterClick)
        }

        val canContinue = selectedAddress.isNotEmpty()
        AppButton(
            text = "Siguiente",
            icon = Icons.Default.ArrowForward,
            onClick = onNextClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .imePadding()
                .navigationBarsPadding()
                .padding(16.dp),
            enabled = canContinue,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary
        )
    }
}
