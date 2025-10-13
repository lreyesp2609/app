package com.example.app.screen.mapa

import android.content.Context
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.app.models.UbicacionUsuarioResponse
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.config.Configuration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.app.R
import com.example.app.viewmodel.decodePolyline

@Composable
fun SimpleMapOSM(
    modifier: Modifier = Modifier,
    userLat: Double = 0.0,
    userLon: Double = 0.0,
    ubicaciones: List<UbicacionUsuarioResponse> = emptyList(),
    zoom: Double = 16.0,
    recenterTrigger: Int = 0,
    zoomInTrigger: Int = 0,
    zoomOutTrigger: Int = 0,
    context: Context = LocalContext.current,
    transportMode: String,
    routeGeometry: String? = null,
) {
    val mapView = rememberMapView(context, zoom)

    // Obtener colores del tema actual
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    // Inicializar configuración
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(
            context,
            context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
    }

    // Separar overlays del control de cámara
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { mapView },
        update = { map ->
            // Solo actualizar overlays
            map.overlays.clear()

            // Ruta con colores del tema
            routeGeometry?.let { geometry ->
                try {
                    val routePoints = geometry.decodePolyline()
                    if (routePoints.isNotEmpty()) {
                        val polyline = Polyline().apply {
                            setPoints(routePoints)
                            color = when (transportMode) {
                                "foot-walking" -> Color(0xFF4CAF50).toArgb() // Verde para caminar (mantener por visibilidad)
                                "cycling-regular" -> secondaryColor.toArgb() // Usar color secundario del tema
                                "driving-car" -> Color(0xFF9C27B0).toArgb() // Púrpura en lugar de naranja
                                else -> primaryColor.toArgb() // Color principal del tema como fallback
                            }
                            width = 12f
                        }
                        map.overlays.add(polyline)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SimpleMapOSM", "Error decoding route geometry", e)
                }
            }

            // Marcador usuario (azul como estaba originalmente)
            val userMarker = Marker(map).apply {
                position = GeoPoint(userLat, userLon)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = ShapeDrawable(OvalShape()).apply {
                    intrinsicHeight = 36
                    intrinsicWidth = 36
                    paint.apply {
                        color = android.graphics.Color.rgb(33, 150, 243) // Azul original
                        style = android.graphics.Paint.Style.FILL
                        isAntiAlias = true
                    }
                }
                title = "Tu ubicación"
            }
            map.overlays.add(userMarker)

            // Marcadores destinos
            val drawable = ContextCompat.getDrawable(context, R.drawable.ic_marker_red)
            ubicaciones.forEach { ubicacion ->
                val marker = Marker(map).apply {
                    position = GeoPoint(ubicacion.latitud, ubicacion.longitud)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    icon = drawable
                    title = ubicacion.nombre
                    snippet = ubicacion.direccion_completa
                }
                map.overlays.add(marker)
            }

            map.invalidate()
        }
    )

    // CAMBIO PRINCIPAL: Seguir la ubicación del usuario cuando cambie
    LaunchedEffect(userLat, userLon) {
        if (userLat != 0.0 && userLon != 0.0) {
            // Mantener el zoom actual del usuario y solo cambiar la posición
            mapView.controller.animateTo(GeoPoint(userLat, userLon))
        }
    }

    // Recentrar mapa al presionar el botón - AHORA CENTRADO EN EL USUARIO
    LaunchedEffect(recenterTrigger) {
        if (recenterTrigger > 0 && userLat != 0.0 && userLon != 0.0) {
            mapView.controller.animateTo(GeoPoint(userLat, userLon))
        }
    }

    // Ajustar cámara al cargar nueva ruta - SOLO cuando hay nueva ruta
    LaunchedEffect(routeGeometry) {
        routeGeometry?.let { geometry ->
            try {
                val routePoints = geometry.decodePolyline()
                if (routePoints.isNotEmpty()) {
                    val allPoints = mutableListOf<GeoPoint>().apply {
                        addAll(routePoints)
                        add(GeoPoint(userLat, userLon))
                        ubicaciones.forEach { add(GeoPoint(it.latitud, it.longitud)) }
                    }
                    mapView.zoomToBoundingBox(
                        org.osmdroid.util.BoundingBox.fromGeoPoints(allPoints),
                        true,
                        100
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("SimpleMapOSM", "Error calculating bounding box", e)
            }
        }
    }

    // Zoom in/out manual
    LaunchedEffect(zoomInTrigger) {
        if (zoomInTrigger > 0) {
            val currentZoom = mapView.zoomLevelDouble
            if (currentZoom < mapView.maxZoomLevel) mapView.controller.zoomTo(currentZoom + 1.0)
        }
    }

    LaunchedEffect(zoomOutTrigger) {
        if (zoomOutTrigger > 0) {
            val currentZoom = mapView.zoomLevelDouble
            if (currentZoom > mapView.minZoomLevel) mapView.controller.zoomTo(currentZoom - 1.0)
        }
    }
}