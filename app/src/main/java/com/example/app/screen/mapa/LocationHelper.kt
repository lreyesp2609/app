package com.example.app.screen.mapa

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*

@Composable
fun GetCurrentLocation(
    onLocationResult: (latitude: Double, longitude: Double) -> Unit,
    onError: (String) -> Unit,
    onGpsDisabled: () -> Unit = {},
    onPermissionGranted: () -> Unit = {},
    hasPermission: Boolean = false,
    retryCounter: Int = 0 // Cambiado de Boolean a Int para permitir múltiples reintentos
) {
    val context = LocalContext.current
    var internalHasPermission by remember { mutableStateOf(hasPermission) }
    var shouldRetryLocation by remember { mutableStateOf(false) }
    var gpsDisabled by remember { mutableStateOf(false) }

    // Usar un contador interno para controlar cuándo permitir mostrar el diálogo
    var lastRetryCounter by remember { mutableIntStateOf(0) }

    // Resetear estados cuando se solicite reintento
    LaunchedEffect(retryCounter) {
        if (retryCounter > lastRetryCounter) {
            gpsDisabled = false
            shouldRetryLocation = true
            lastRetryCounter = retryCounter
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        internalHasPermission = fineLocationGranted || coarseLocationGranted
        if (internalHasPermission) {
            onPermissionGranted()
            shouldRetryLocation = true
            gpsDisabled = false
        } else {
            onError("Permiso de ubicación denegado")
        }
    }

    val gpsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            shouldRetryLocation = true
            gpsDisabled = false
        } else {
            gpsDisabled = true
            onGpsDisabled()
        }
    }

    // Verificar permisos inicialmente
    LaunchedEffect(Unit) {
        if (!internalHasPermission) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                internalHasPermission = true
                onPermissionGranted()
                shouldRetryLocation = true
            } else {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    fun startLocationUpdates(client: FusedLocationProviderClient, request: LocationRequest) {
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation
                if (location != null) {
                    onLocationResult(location.latitude, location.longitude)
                } else {
                    onError("No se pudo obtener la ubicación")
                }
                client.removeLocationUpdates(this)
            }
        }

        try {
            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            onError("Error de permisos de ubicación")
        }
    }

    // Intentar obtener ubicación cuando sea necesario
    LaunchedEffect(internalHasPermission, shouldRetryLocation, retryCounter) {
        if (!internalHasPermission || gpsDisabled || !shouldRetryLocation) return@LaunchedEffect

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            1000L
        ).setMinUpdateIntervalMillis(500)
            .setMaxUpdateDelayMillis(2000)
            .build()

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true) // Esto ayuda a mostrar el diálogo siempre

        val client: SettingsClient = LocationServices.getSettingsClient(context)
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            // La configuración de ubicación está habilitada, intentar obtener ubicación
            startLocationUpdates(fusedLocationClient, locationRequest)
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    val intentSenderRequest = IntentSenderRequest.Builder(exception.resolution).build()
                    gpsLauncher.launch(intentSenderRequest)
                } catch (sendEx: Exception) {
                    onError("Error al activar GPS: ${sendEx.message}")
                }
            } else {
                gpsDisabled = true
                onGpsDisabled()
            }
        }

        shouldRetryLocation = false
    }
}

@Composable
fun GpsEnableButton(
    onEnableGps: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.LocationOff,
            contentDescription = "GPS deshabilitado",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Para mejor experiencia\nhabilita el GPS",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onEnableGps,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(25.dp),
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Habilitar GPS",
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}