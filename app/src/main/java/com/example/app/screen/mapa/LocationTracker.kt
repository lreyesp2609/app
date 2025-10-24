package com.example.app.screen.mapa

import android.content.pm.PackageManager
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import android.Manifest

@Composable
fun LocationTracker(
    onLocationUpdate: (lat: Double, lon: Double) -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L // cada 5s
        ).build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    onLocationUpdate(location.latitude, location.longitude)
                }
            }
        }

        try {
            if (
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    callback,
                    Looper.getMainLooper()
                )
            } else {
                // Aquí decides: mostrar un Toast, lanzar tu permissionLauncher, etc.
                Log.w("LocationTracker", "Permisos de ubicación no concedidos")
            }
        } catch (e: SecurityException) {
            Log.e("LocationTracker", "Error al solicitar actualizaciones", e)
        }
    }
}
fun calcularDistancia(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
    val start = android.location.Location("start").apply {
        latitude = lat1
        longitude = lon1
    }
    val end = android.location.Location("end").apply {
        latitude = lat2
        longitude = lon2
    }
    return start.distanceTo(end) // en metros
}