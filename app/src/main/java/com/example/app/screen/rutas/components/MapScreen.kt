package com.example.app.screen.rutas.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.app.screen.mapa.GetCurrentLocation
import com.example.app.screen.mapa.GpsEnableButton
import com.example.app.screen.mapa.OpenStreetMap
import androidx.compose.material3.Icon

@Composable
fun MapView(
    latitude: Double,
    longitude: Double,
    modifier: Modifier = Modifier
) {
    OpenStreetMap(
        latitude = latitude,
        longitude = longitude,
        modifier = modifier.fillMaxSize(),
        showUserLocation = true
    )
}



@Composable
fun MapScreen(
    onConfirmClick: () -> Unit = {}
) {
    var currentLat by remember { mutableStateOf(0.0) }
    var currentLon by remember { mutableStateOf(0.0) }
    var locationObtained by remember { mutableStateOf(false) }
    var showGpsButton by remember { mutableStateOf(false) }

    // Estado para forzar recenter
    var recenterTrigger by remember { mutableStateOf(0) }

    Box(modifier = Modifier.fillMaxSize()) {

        when {
            locationObtained -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    OpenStreetMap(
                        latitude = currentLat,
                        longitude = currentLon,
                        showUserLocation = true,
                        recenterTrigger = recenterTrigger,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Botón para centrar en ubicación
                    FloatingActionButton(
                        onClick = { recenterTrigger++ },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(16.dp),
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "Centrar mapa",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    // Botón de confirmar
                    MapBottomButtons(onConfirmClick = onConfirmClick)
                }
            }
            showGpsButton -> {
                GpsEnableButton(onEnableGps = { showGpsButton = false })
            }
            else -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Obteniendo ubicación...")
                }

                GetCurrentLocation(
                    onLocationResult = { lat, lon ->
                        currentLat = lat
                        currentLon = lon
                        locationObtained = true
                    },
                    onError = { /* manejar error */ },
                    onGpsDisabled = { showGpsButton = true }
                )
            }
        }
    }
}
