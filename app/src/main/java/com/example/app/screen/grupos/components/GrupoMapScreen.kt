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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
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
import com.example.app.BuildConfig
import com.example.app.screen.components.AppBackButton
import com.example.app.screen.mapa.GetCurrentLocation
import com.example.app.screen.mapa.GpsEnableButton
import com.example.app.screen.mapa.GrupoOpenStreetMap
import com.example.app.screen.mapa.LocationTracker
import com.example.app.screen.mapa.MapControlButton
import com.example.app.screen.mapa.OpenStreetMap
import com.example.app.services.LocationTrackingService
import com.example.app.utils.SessionManager
import com.example.app.viewmodel.LocationGrupoViewModel
import com.example.app.websocket.WebSocketLocationManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

@Composable
fun GrupoMapScreen(
    navController: NavController,
    grupoId: Int,
    onLocationSelected: (lat: Double, lon: Double, address: String) -> Unit,
    onBackToChat: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val sessionManager = SessionManager.getInstance(context)
    val currentUser = sessionManager.getUser()
    val currentUserId = currentUser?.id ?: 0
    val currentUserName = currentUser?.nombre ?: "Tú"

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
    var zoomInTrigger by remember { mutableStateOf(0) }
    var zoomOutTrigger by remember { mutableStateOf(0) }
    var selectedAddress by remember { mutableStateOf("Selecciona una ubicación") }

    val ubicacionesMiembros by locationViewModel.ubicacionesMiembros.collectAsState()
    val isConnected by locationViewModel.isConnected.collectAsState()

    LaunchedEffect(grupoId) {
        Log.d("GrupoMapScreen", "🚀 INICIANDO RASTREO AUTOMÁTICO")
        val grupoNombre = "Grupo $grupoId"
        LocationTrackingService.startTracking(
            context = context,
            grupoId = grupoId,
            grupoNombre = grupoNombre
        )
        delay(2000)
        locationViewModel.suscribirseAUbicaciones(grupoId)
    }

    // 🔥 NUEVO: Forzar recentrado cuando se obtiene la ubicación por primera vez
    LaunchedEffect(locationObtained) {
        if (locationObtained) {
            recenterTrigger++
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            scope.launch {
                locationViewModel.desuscribirse()
            }
        }
    }

    if (locationObtained) {
        LocationTracker { lat, lon ->
            currentLat = lat
            currentLon = lon
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (locationObtained) {
            GrupoOpenStreetMap(
                latitude = currentLat,
                longitude = currentLon,
                miembrosGrupo = ubicacionesMiembros,
                currentUserId = currentUserId,
                currentUserName = currentUserName,
                recenterTrigger = recenterTrigger,
                zoomInTrigger = zoomInTrigger,
                zoomOutTrigger = zoomOutTrigger,
                modifier = Modifier.fillMaxSize()
            )

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

            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MapControlButton(icon = Icons.Default.Add, onClick = { zoomInTrigger++ })
                MapControlButton(icon = Icons.Default.Remove, onClick = { zoomOutTrigger++ })
                MapControlButton(icon = Icons.Default.MyLocation, onClick = { recenterTrigger++ })
            }

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