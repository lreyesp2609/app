package com.example.app.screen.grupos.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.app.screen.components.AppBackButton
import com.example.app.screen.mapa.GetCurrentLocation
import com.example.app.screen.mapa.GpsEnableButton
import com.example.app.screen.mapa.LocationTracker
import com.example.app.screen.mapa.OpenStreetMap
import com.example.app.viewmodel.LocationGrupoViewModel

@Composable
fun GrupoMapScreen(
    navController: NavController,
    grupoId: Int,
    onLocationSelected: (lat: Double, lon: Double, address: String) -> Unit,
    onBackToChat: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ViewModel de ubicaciones
    val locationViewModel: LocationGrupoViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return LocationGrupoViewModel(context) as T
            }
        }
    )

    var currentLat by remember { mutableStateOf(0.0) }
    var currentLon by remember { mutableStateOf(0.0) }
    var locationObtained by remember { mutableStateOf(false) }
    var showGpsButton by remember { mutableStateOf(false) }
    var recenterTrigger by remember { mutableStateOf(0) }

    var selectedAddress by remember { mutableStateOf("Selecciona una ubicación") }

    // Observar ubicaciones de otros miembros
    val ubicacionesMiembros by locationViewModel.ubicacionesMiembros.collectAsState()
    val isConnected by locationViewModel.isConnected.collectAsState()

    // 🆕 CAMBIO PRINCIPAL: Suscribirse al WebSocket existente
    LaunchedEffect(grupoId) {
        Log.d("GrupoMapScreen", "📢 Suscribiéndose al WebSocket para grupo $grupoId")
        locationViewModel.suscribirseAUbicaciones()
    }

    // ⚠️ ELIMINAR: Ya no necesitas enviar ubicaciones manualmente
    // El LocationService ya lo hace en background
    // Solo trackea para actualizar el mapa local
    if (locationObtained) {
        LocationTracker { lat, lon ->
            currentLat = lat
            currentLon = lon
            Log.d("GrupoMapScreen", "📍 Actualizando mapa local: $lat, $lon")
            // ❌ NO enviar aquí, el servicio ya lo hace
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        if (locationObtained) {
            OpenStreetMap(
                latitude = currentLat,
                longitude = currentLon,
                showUserLocation = true,
                showCenterPin = false,
                miembrosGrupo = ubicacionesMiembros,  // Mostrar ubicaciones de otros
                recenterTrigger = recenterTrigger,
                modifier = Modifier.fillMaxSize()
            )

            // Botón superior para volver al chat
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.TopStart
            ) {
                AppBackButton(
                    navController = navController,
                    onClick = onBackToChat
                )
            }

            // Botones inferiores
            GrupoMapButtons(
                navController = navController,
                selectedAddress = selectedAddress,
                onConfirmClick = { /* Vacío por ahora */ },
                onRecenterClick = { recenterTrigger++ },
                onBackClick = onBackToChat
            )

        } else if (showGpsButton) {
            GpsEnableButton(onEnableGps = { showGpsButton = false })
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Obteniendo ubicación...",
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            GetCurrentLocation(
                onLocationResult = { lat, lon ->
                    currentLat = lat
                    currentLon = lon
                    locationObtained = true
                },
                onError = { /* Manejar error */ },
                onGpsDisabled = { showGpsButton = true }
            )
        }
    }
}