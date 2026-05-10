package com.rutai.app.screen.mapa

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
import com.rutai.app.models.UbicacionUsuarioResponse
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.config.Configuration
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import androidx.compose.runtime.DisposableEffect
import com.rutai.app.models.ZonaPeligrosaResponse
import com.rutai.app.ui.theme.DangerLevelColors
import com.rutai.app.viewmodel.MapViewModel
import com.rutai.app.viewmodel.decodePolyline

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
    zonasPeligrosas: List<ZonaPeligrosaResponse> = emptyList(),
    mostrarZonasPeligrosas: Boolean = true,
    viewModel: MapViewModel? = null
) {
    val mapView = rememberMapView(context, zoom)
    val isDarkTheme = isSystemInDarkTheme()

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    // Gestionar ciclo de vida del MapView para carga de tiles estable
    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

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
            map.overlays.clear()

            // ZONAS PELIGROSAS — color driven by DangerLevelColors
            if (mostrarZonasPeligrosas && zonasPeligrosas.isNotEmpty()) {
                zonasPeligrosas.forEach { zona ->
                    try {
                        val puntosCentro = zona.poligono.firstOrNull()
                        if (puntosCentro == null) {
                            Log.w("SimpleMapOSM", "⚠️ Zona ${zona.nombre} sin coordenadas")
                            return@forEach
                        }

                        val latCentro = puntosCentro.lat
                        val lonCentro = puntosCentro.lon
                        val radio = zona.radioMetros ?: 200

                        // Clamp level to 1-3 range used by DangerLevelColors
                        val nivelUI = DangerLevelColors.clampNivel(zona.nivelPeligro)

                        // ARGB fill color from DangerLevelColors (includes alpha)
                        val fillArgb = DangerLevelColors.getArgbColor(nivelUI, isDarkTheme)

                        // Solid border color (same ramp, no alpha)
                        val borderArgb = when (nivelUI) {
                            1 -> if (isDarkTheme)
                                android.graphics.Color.rgb(77, 182, 172)   // Teal dark
                            else
                                android.graphics.Color.rgb(0, 150, 136)    // Teal light
                            2 -> if (isDarkTheme)
                                android.graphics.Color.rgb(255, 152, 0)    // Orange dark
                            else
                                android.graphics.Color.rgb(230, 81, 0)     // Orange light
                            else -> if (isDarkTheme)
                                android.graphics.Color.rgb(239, 68, 68)    // Red dark
                            else
                                android.graphics.Color.rgb(220, 38, 38)    // Red light
                        }

                        Log.d("SimpleMapOSM", "📍 Dibujando zona: ${zona.nombre} nivel=$nivelUI")

                        val puntosCirculo = generarPuntosCirculo(latCentro, lonCentro, radio)

                        val circulo = Polygon(map).apply {
                            points = puntosCirculo
                            fillPaint.color = fillArgb
                            fillPaint.style = android.graphics.Paint.Style.FILL
                            outlinePaint.color = borderArgb
                            outlinePaint.strokeWidth = 3f
                            outlinePaint.style = android.graphics.Paint.Style.STROKE
                            title = zona.nombre
                            snippet = buildString {
                                append(context.getString(R.string.map_snippet_zone_level, DangerLevelColors.getNombreNivel(nivelUI, context)))
                                zona.tipo?.let { append("\n" + context.getString(R.string.map_snippet_type, it)) }
                                zona.notas?.let { append("\n$it") }
                            }
                        }
                        map.overlays.add(circulo)

                        // Central marker dot — same color ramp
                        val markerSize = when (nivelUI) {
                            1 -> 20
                            2 -> 24
                            else -> 28
                        }

                        val marcadorZona = Marker(map).apply {
                            position = GeoPoint(latCentro, lonCentro)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            icon = ShapeDrawable(OvalShape()).apply {
                                intrinsicHeight = markerSize
                                intrinsicWidth = markerSize
                                paint.apply {
                                    color = borderArgb
                                    style = android.graphics.Paint.Style.FILL
                                    isAntiAlias = true
                                }
                            }
                            title = "⚠️ ${zona.nombre}"
                            snippet = context.getString(
                                R.string.map_snippet_zone,
                                DangerLevelColors.getNombreNivel(nivelUI, context),
                                radio
                            )
                            setOnMarkerClickListener { marker, _ ->
                                marker.showInfoWindow()
                                true
                            }
                        }

                        map.overlays.add(marcadorZona)

                        Log.d("SimpleMapOSM", "✅ Zona ${zona.nombre} (nivel $nivelUI) dibujada")

                    } catch (e: Exception) {
                        Log.e("SimpleMapOSM", "❌ Error dibujando zona ${zona.nombre}: ${e.message}", e)
                    }
                }
            }

            // Route polyline (above zones)
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

            // User location marker (topmost)
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
                title = context.getString(R.string.map_your_location)
            }
            map.overlays.add(userMarker)

            // Destination markers
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

    LaunchedEffect(userLat, userLon) {
        if (userLat != 0.0 && userLon != 0.0) {
            mapView.controller.animateTo(GeoPoint(userLat, userLon))
        }
    }

    LaunchedEffect(recenterTrigger) {
        if (recenterTrigger > 0 && userLat != 0.0 && userLon != 0.0) {
            mapView.controller.animateTo(GeoPoint(userLat, userLon))
        }
    }

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

    val radioGradosLat = radioMetros / 111320.0
    val radioGradosLon = radioMetros / (111320.0 * Math.cos(Math.toRadians(lat)))

    for (i in 0..numPuntos) {
        val angulo = 2 * Math.PI * i / numPuntos
        val newLat = lat + (radioGradosLat * Math.sin(angulo))
        val newLon = lon + (radioGradosLon * Math.cos(angulo))
        puntos.add(GeoPoint(newLat, newLon))
    }

    return puntos
}