package com.example.app.screen.mapa

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import com.example.app.R
import com.example.app.models.Feature
import com.example.app.models.ZonaGuardada
import com.example.app.models.getDisplayName
import com.example.app.screen.recordatorios.components.getIconResource
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.views.overlay.MapEventsOverlay

import org.osmdroid.views.overlay.Polygon

@Composable
fun OpenStreetMap(
    modifier: Modifier = Modifier,
    latitude: Double = 0.0,
    longitude: Double = 0.0,
    zoom: Double = 16.0,
    showUserLocation: Boolean = true,
    recenterTrigger: Int = 0,
    zoomInTrigger: Int = 0,
    zoomOutTrigger: Int = 0,
    context: Context = LocalContext.current,
    pois: List<Feature> = emptyList(),
    showCenterPin: Boolean = true,
    onLocationSelected: (lat: Double, lon: Double) -> Unit = { _, _ -> },
    onLocationLongPress: ((Double, Double) -> Unit)? = null,
    // Preview de zona en creación
    zonaPreviewLat: Double? = null,
    zonaPreviewLon: Double? = null,
    zonaPreviewRadio: Int? = null,
    // 🔥 ZONAS YA GUARDADAS
    zonasGuardadas: List<ZonaGuardada> = emptyList(),
    // 🆕 CALLBACK PARA DETECTAR TAP EN ZONA
    onZonaClick: ((ZonaGuardada) -> Unit)? = null
) {
    val mapView = rememberMapView(context, zoom)

    LaunchedEffect(Unit) {
        Configuration.getInstance().load(
            context,
            context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
    }

    // 🔥 Centrar el mapa cuando cambien las coordenadas del usuario
    LaunchedEffect(latitude, longitude) {
        if (latitude != 0.0 && longitude != 0.0) {
            Log.d("OpenStreetMap", "📍 Centrando mapa en: $latitude, $longitude")
            mapView.controller.setCenter(GeoPoint(latitude, longitude))
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { mapView },
        update = { map ->
            map.overlays.clear()

            // Detector de long press (debe ir PRIMERO)
            if (onLocationLongPress != null) {
                val mapEventsReceiver = object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean = false

                    override fun longPressHelper(p: GeoPoint?): Boolean {
                        if (p != null) {
                            Log.d("OpenStreetMap", "🔴 Long press: ${p.latitude}, ${p.longitude}")
                            onLocationLongPress.invoke(p.latitude, p.longitude)
                        }
                        return true
                    }
                }
                map.overlays.add(MapEventsOverlay(mapEventsReceiver))
            }

            // 🔥 DIBUJAR ZONAS GUARDADAS (primero, para que estén debajo)
            zonasGuardadas.forEach { zona ->
                try {
                    val center = GeoPoint(zona.lat, zona.lon)

                    Log.d("OpenStreetMap", "🎯 Dibujando zona guardada: ${zona.nombre}")
                    Log.d("OpenStreetMap", "   Centro: (${zona.lat}, ${zona.lon})")
                    Log.d("OpenStreetMap", "   Radio: ${zona.radio}m")

                    // 🔥 USAR FUNCIÓN PERSONALIZADA (NO Polygon.pointsAsCircle)
                    val puntosCirculo = crearCirculoPersonalizado(
                        lat = zona.lat,
                        lon = zona.lon,
                        radioMetros = zona.radio.toInt()  // ✅ Convertir Double a Int
                    )

                    // Círculo de la zona
                    val circle = Polygon(map).apply {
                        points = puntosCirculo
                        fillPaint.color = android.graphics.Color.parseColor("#33FF5252")
                        outlinePaint.color = android.graphics.Color.parseColor("#FFFF5252")
                        outlinePaint.strokeWidth = 3f
                    }
                    map.overlays.add(circle)

                    // 🆕 Marcador con nivel de peligro Y CLICK LISTENER
                    val markerColor = when (zona.nivel) {
                        1 -> "#FF4CAF50" // Verde
                        2 -> "#FFFFEB3B" // Amarillo
                        3 -> "#FFFF9800" // Naranja
                        4 -> "#FFFF5722" // Naranja oscuro
                        5 -> "#FFF44336" // Rojo
                        else -> "#FFFF5252"
                    }

                    val marker = Marker(map).apply {
                        position = center
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        icon = ShapeDrawable(OvalShape()).apply {
                            intrinsicHeight = 35
                            intrinsicWidth = 35
                            paint.color = android.graphics.Color.parseColor(markerColor)
                            paint.style = android.graphics.Paint.Style.FILL
                        }
                        title = zona.nombre
                        snippet = "Nivel: ${zona.nivel}/5 • Radio: ${zona.radio}m"

                        // 🔥 DETECTAR TAP EN ZONA
                        setOnMarkerClickListener { clickedMarker, mapView ->
                            Log.d("OpenStreetMap", "👆 Tap en zona: ${zona.nombre}")
                            onZonaClick?.invoke(zona)
                            true // Consumir el evento
                        }
                    }
                    map.overlays.add(marker)

                    Log.d("OpenStreetMap", "✅ Zona ${zona.nombre} dibujada con ${puntosCirculo.size} puntos")

                } catch (e: Exception) {
                    Log.e("OpenStreetMap", "❌ Error dibujando zona ${zona.nombre}: ${e.message}", e)
                }
            }

            // 🔥 PREVIEW DE ZONA EN CREACIÓN (si existe)
            if (zonaPreviewLat != null && zonaPreviewLon != null && zonaPreviewRadio != null) {
                try {
                    val center = GeoPoint(zonaPreviewLat, zonaPreviewLon)

                    Log.d("OpenStreetMap", "👁️ Preview zona en ($zonaPreviewLat, $zonaPreviewLon) - Radio: ${zonaPreviewRadio}m")

                    // 🔥 USAR FUNCIÓN PERSONALIZADA
                    val puntosPreview = crearCirculoPersonalizado(
                        lat = zonaPreviewLat,
                        lon = zonaPreviewLon,
                        radioMetros = zonaPreviewRadio
                    )

                    val circle = Polygon(map).apply {
                        points = puntosPreview
                        fillPaint.color = android.graphics.Color.parseColor("#44FF5252")
                        outlinePaint.color = android.graphics.Color.parseColor("#FFFF5252")
                        outlinePaint.strokeWidth = 4f
                        outlinePaint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(10f, 10f), 0f)
                    }
                    map.overlays.add(circle)

                    val centerMarker = Marker(map).apply {
                        position = center
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        icon = ShapeDrawable(OvalShape()).apply {
                            intrinsicHeight = 40
                            intrinsicWidth = 40
                            paint.color = android.graphics.Color.parseColor("#FFFF5252")
                            paint.style = android.graphics.Paint.Style.FILL
                        }
                        title = "Nueva Zona (${zonaPreviewRadio}m)"
                    }
                    map.overlays.add(centerMarker)

                    Log.d("OpenStreetMap", "✅ Preview dibujado con ${puntosPreview.size} puntos")

                } catch (e: Exception) {
                    Log.e("OpenStreetMap", "❌ Error dibujando preview: ${e.message}", e)
                }
            }

            // Usuario (punto azul)
            if (showUserLocation) {
                val geoPoint = GeoPoint(latitude, longitude)
                val circleMarker = Marker(map).apply {
                    position = geoPoint
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = ShapeDrawable(OvalShape()).apply {
                        intrinsicHeight = 40
                        intrinsicWidth = 40
                        paint.color = android.graphics.Color.BLUE
                        paint.style = android.graphics.Paint.Style.FILL
                    }
                    title = "Tú"
                }
                map.overlays.add(circleMarker)
            }

            // POIs
            if (pois.isNotEmpty()) {
                Log.d("POI_DEBUG", "🗺️ Dibujando ${pois.size} POIs en el mapa")
                pois.forEach { feature ->
                    val coords = feature.geometry?.coordinates
                    if (coords != null && coords.size >= 2) {
                        val marker = Marker(map).apply {
                            position = GeoPoint(coords[1], coords[0])
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            icon = ContextCompat.getDrawable(context, feature.getIconResource())
                            title = feature.getDisplayName()
                            snippet = buildString {
                                val category = feature.properties?.category_ids?.values?.firstOrNull()
                                if (category != null) {
                                    append("📁 ${category.category_group} - ${category.category_name}\n")
                                }
                                feature.properties?.osm_tags?.forEach { (key, value) ->
                                    if (key != "name") {
                                        append("$key: $value\n")
                                    }
                                }
                            }
                        }
                        map.overlays.add(marker)
                    }
                }
            }

            // Pin central
            if (showCenterPin) {
                map.overlays.add(CenteredPinOverlay(context))
            }

            // Forzar redibujado
            map.invalidate()
        }
    )

    // Recentrar mapa (con trigger manual)
    LaunchedEffect(recenterTrigger) {
        if (recenterTrigger > 0) {
            mapView.controller.animateTo(GeoPoint(latitude, longitude))
        }
    }

    // Zoom In
    LaunchedEffect(zoomInTrigger) {
        if (zoomInTrigger > 0) {
            mapView.controller.zoomIn()
        }
    }

    // Zoom Out
    LaunchedEffect(zoomOutTrigger) {
        if (zoomOutTrigger > 0) {
            mapView.controller.zoomOut()
        }
    }

    DisposableEffect(mapView) {
        val listener = object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {
                event?.source?.mapCenter?.let { center ->
                    onLocationSelected(center.latitude, center.longitude)
                }
                return true
            }

            override fun onZoom(event: ZoomEvent?): Boolean {
                event?.source?.mapCenter?.let { center ->
                    onLocationSelected(center.latitude, center.longitude)
                }
                return true
            }
        }
        mapView.addMapListener(listener)
        onDispose { mapView.removeMapListener(listener) }
    }
}

@Composable
fun rememberMapView(context: Context, zoom: Double = 16.0): MapView {
    return remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(zoom)
            setBuiltInZoomControls(false)
        }
    }
}

class CenteredPinOverlay(private val context: Context) : Overlay() {
    private val pinDrawable: Drawable? = ContextCompat.getDrawable(context, R.drawable.ic_marker_red)

    override fun draw(canvas: Canvas?, mapView: MapView?, shadow: Boolean) {
        if (canvas == null || mapView == null || shadow) return
        pinDrawable?.let { drawable ->
            val centerX = mapView.width / 2
            val centerY = mapView.height / 2
            val width = drawable.intrinsicWidth
            val height = drawable.intrinsicHeight
            drawable.setBounds(centerX - width / 2, centerY - height, centerX + width / 2, centerY)
            drawable.draw(canvas)
        }
    }
}

// 🔥 FUNCIÓN PERSONALIZADA - NUNCA USAR Polygon.pointsAsCircle()
private fun crearCirculoPersonalizado(
    lat: Double,
    lon: Double,
    radioMetros: Int
): List<GeoPoint> {
    val puntos = mutableListOf<GeoPoint>()
    val numPuntos = 32

    // Conversión correcta de metros a grados
    val radioGradosLat = radioMetros / 111320.0
    val radioGradosLon = radioMetros / (111320.0 * Math.cos(Math.toRadians(lat)))

    for (i in 0..numPuntos) {
        val angulo = 2 * Math.PI * i / numPuntos

        // ✅ CORRECTO: Cos() para latitud, Sin() para longitud
        val newLat = lat + (radioGradosLat * Math.cos(angulo))
        val newLon = lon + (radioGradosLon * Math.sin(angulo))

        puntos.add(GeoPoint(newLat, newLon))
    }

    return puntos
}