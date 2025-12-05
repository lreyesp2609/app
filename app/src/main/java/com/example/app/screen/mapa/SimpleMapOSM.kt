package com.example.app.screen.mapa

import android.content.Context
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.util.Log
import androidx.compose.foundation.isSystemInDarkTheme
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
import com.example.app.models.ZonaPeligrosaResponse
import com.example.app.viewmodel.MapViewModel
import com.example.app.viewmodel.decodePolyline
import org.osmdroid.views.overlay.Polygon

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
    // 🆕 NUEVOS PARÁMETROS
    zonasPeligrosas: List<ZonaPeligrosaResponse> = emptyList(),
    mostrarZonasPeligrosas: Boolean = true,
    viewModel: MapViewModel? = null
) {
    val mapView = rememberMapView(context, zoom)
    val isDarkTheme = isSystemInDarkTheme()

    // Obtener colores del tema actual
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    // Inicializar configuración
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(
            context,
            context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { mapView },
        update = { map ->
            // Limpiar overlays existentes
            map.overlays.clear()

            // 🆕 AGREGAR ZONAS PELIGROSAS (PRIMERO, para que estén debajo de la ruta)
            // 🆕 AGREGAR ZONAS PELIGROSAS (PRIMERO, para que estén debajo de la ruta)
            if (mostrarZonasPeligrosas && zonasPeligrosas.isNotEmpty()) {
                zonasPeligrosas.forEach { zona ->
                    try {
                        // 🔍 Verificar que tenga coordenadas válidas
                        val puntosCentro = zona.poligono.firstOrNull()
                        if (puntosCentro == null) {
                            Log.w("SimpleMapOSM", "⚠️ Zona ${zona.nombre} sin coordenadas")
                            return@forEach
                        }

                        val latCentro = puntosCentro.lat
                        val lonCentro = puntosCentro.lon
                        val radio = zona.radioMetros ?: 200

                        Log.d("SimpleMapOSM", "📍 Dibujando zona: ${zona.nombre}")
                        Log.d("SimpleMapOSM", "   Centro: ($latCentro, $lonCentro)")
                        Log.d("SimpleMapOSM", "   Radio: ${radio}m")

                        // 🔥 SIEMPRE REGENERAR LOCALMENTE (ignorar puntos del backend)
                        Log.d("SimpleMapOSM", "🔄 Regenerando círculo localmente con código corregido")
                        val puntosCirculo = generarPuntosCirculo(latCentro, lonCentro, radio)

                        // Dibujar el círculo/polígono
                        val circulo = Polygon(map).apply {
                            points = puntosCirculo

                            // Color según nivel de peligro
                            val color = viewModel?.getColorForDangerLevel(zona.nivelPeligro, isDarkTheme)
                                ?: android.graphics.Color.argb(100, 239, 68, 68)

                            fillPaint.color = color
                            fillPaint.style = android.graphics.Paint.Style.FILL

                            // Borde más visible
                            outlinePaint.color = when (zona.nivelPeligro) {
                                1, 2 -> android.graphics.Color.rgb(245, 158, 11)
                                3, 4 -> android.graphics.Color.rgb(234, 88, 12)
                                5 -> android.graphics.Color.rgb(220, 38, 38)
                                else -> android.graphics.Color.rgb(156, 163, 175)
                            }
                            outlinePaint.strokeWidth = 3f
                            outlinePaint.style = android.graphics.Paint.Style.STROKE

                            // Información al hacer tap
                            title = zona.nombre
                            snippet = buildString {
                                append("Nivel de peligro: ${zona.nivelPeligro}/5")
                                zona.tipo?.let { append("\nTipo: $it") }
                                zona.notas?.let { append("\n$it") }
                            }
                        }

                        map.overlays.add(circulo)

                        // 🆕 Marcador central de la zona
                        val marcadorZona = Marker(map).apply {
                            position = GeoPoint(latCentro, lonCentro)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)

                            icon = ShapeDrawable(OvalShape()).apply {
                                intrinsicHeight = when (zona.nivelPeligro) {
                                    1, 2 -> 20
                                    3, 4 -> 24
                                    5 -> 28
                                    else -> 20
                                }
                                intrinsicWidth = intrinsicHeight
                                paint.apply {
                                    color = when (zona.nivelPeligro) {
                                        1, 2 -> android.graphics.Color.rgb(245, 158, 11)
                                        3, 4 -> android.graphics.Color.rgb(234, 88, 12)
                                        5 -> android.graphics.Color.rgb(220, 38, 38)
                                        else -> android.graphics.Color.rgb(156, 163, 175)
                                    }
                                    style = android.graphics.Paint.Style.FILL
                                    isAntiAlias = true
                                }
                            }

                            title = "⚠️ ${zona.nombre}"
                            snippet = "Nivel ${zona.nivelPeligro}/5 • Radio ${radio}m"

                            setOnMarkerClickListener { marker, _ ->
                                marker.showInfoWindow()
                                true
                            }
                        }
                        map.overlays.add(marcadorZona)

                        Log.d("SimpleMapOSM", "✅ Zona ${zona.nombre} dibujada correctamente")

                    } catch (e: Exception) {
                        Log.e("SimpleMapOSM", "❌ Error dibujando zona ${zona.nombre}: ${e.message}", e)
                    }
                }
            }

            // Ruta con colores del tema (encima de las zonas)
            routeGeometry?.let { geometry ->
                try {
                    val routePoints = geometry.decodePolyline()
                    if (routePoints.isNotEmpty()) {
                        val polyline = Polyline().apply {
                            setPoints(routePoints)
                            color = when (transportMode) {
                                "foot-walking" -> android.graphics.Color.rgb(76, 175, 80)
                                "cycling-regular" -> secondaryColor.toArgb()
                                "driving-car" -> android.graphics.Color.rgb(156, 39, 176)
                                else -> primaryColor.toArgb()
                            }
                            width = 12f
                        }
                        map.overlays.add(polyline)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SimpleMapOSM", "Error decoding route geometry", e)
                }
            }

            // Marcador usuario (encima de todo)
            val userMarker = Marker(map).apply {
                position = GeoPoint(userLat, userLon)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = ShapeDrawable(OvalShape()).apply {
                    intrinsicHeight = 36
                    intrinsicWidth = 36
                    paint.apply {
                        color = android.graphics.Color.rgb(33, 150, 243)
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

    // SEGUIR UBICACIÓN DEL USUARIO
    LaunchedEffect(userLat, userLon) {
        if (userLat != 0.0 && userLon != 0.0) {
            mapView.controller.animateTo(GeoPoint(userLat, userLon))
        }
    }

    // Recentrar mapa
    LaunchedEffect(recenterTrigger) {
        if (recenterTrigger > 0 && userLat != 0.0 && userLon != 0.0) {
            mapView.controller.animateTo(GeoPoint(userLat, userLon))
        }
    }

    // Ajustar cámara al cargar nueva ruta
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

private fun generarPuntosCirculo(
    lat: Double,
    lon: Double,
    radioMetros: Int
): List<GeoPoint> {
    val puntos = mutableListOf<GeoPoint>()
    val numPuntos = 32

    // 🔥 FIX: Conversión correcta de metros a grados
    val radioGradosLat = radioMetros / 111320.0
    val radioGradosLon = radioMetros / (111320.0 * Math.cos(Math.toRadians(lat)))

    for (i in 0..numPuntos) {
        val angulo = 2 * Math.PI * i / numPuntos

        // 🔥 CORRECCIÓN: Sin() para latitud, Cos() para longitud
        val newLat = lat + (radioGradosLat * Math.sin(angulo))  // ✅ Cambio aquí
        val newLon = lon + (radioGradosLon * Math.cos(angulo))  // ✅ Cambio aquí

        puntos.add(GeoPoint(newLat, newLon))
    }

    return puntos
}