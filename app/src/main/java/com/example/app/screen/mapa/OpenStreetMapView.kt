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
import com.example.app.ui.theme.DangerLevelColors
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
    zonaPreviewLat: Double? = null,
    zonaPreviewLon: Double? = null,
    zonaPreviewRadio: Int? = null,
    zonasGuardadas: List<ZonaGuardada> = emptyList(),
    onZonaClick: ((ZonaGuardada) -> Unit)? = null,
    centerLat: Double = latitude,
    centerLon: Double = longitude,
    // Pass isDarkTheme from the composable caller so AndroidView update block can use it
    isDarkTheme: Boolean = false
) {
    val mapView = rememberMapView(context, zoom)

    LaunchedEffect(Unit) {
        Configuration.getInstance().load(
            context,
            context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
    }

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

            // Long press detector (must go first)
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

            // SAVED DANGER ZONES — color driven by DangerLevelColors
            zonasGuardadas.forEach { zona ->
                try {
                    val center = GeoPoint(zona.lat, zona.lon)

                    Log.d("OpenStreetMap", "🎯 Dibujando zona guardada: ${zona.nombre}")

                    val nivelUI = DangerLevelColors.clampNivel(zona.nivel)

                    val fillArgb = DangerLevelColors.getArgbColor(nivelUI, isDarkTheme)

                    val borderArgb = when (nivelUI) {
                        1 -> if (isDarkTheme)
                            android.graphics.Color.rgb(77, 182, 172)
                        else
                            android.graphics.Color.rgb(0, 150, 136)
                        2 -> if (isDarkTheme)
                            android.graphics.Color.rgb(255, 152, 0)
                        else
                            android.graphics.Color.rgb(230, 81, 0)
                        else -> if (isDarkTheme)
                            android.graphics.Color.rgb(239, 68, 68)
                        else
                            android.graphics.Color.rgb(220, 38, 38)
                    }

                    val puntosCirculo = crearCirculoPersonalizado(
                        lat = zona.lat,
                        lon = zona.lon,
                        radioMetros = zona.radio.toInt()
                    )

                    val circle = Polygon(map).apply {
                        points = puntosCirculo
                        fillPaint.color = fillArgb
                        fillPaint.style = android.graphics.Paint.Style.FILL
                        outlinePaint.color = borderArgb
                        outlinePaint.strokeWidth = 3f
                    }
                    map.overlays.add(circle)

                    val marker = Marker(map).apply {
                        position = center
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        icon = ShapeDrawable(OvalShape()).apply {
                            val size = when (nivelUI) { 1 -> 28; 2 -> 32; else -> 36 }
                            intrinsicHeight = size
                            intrinsicWidth = size
                            paint.color = borderArgb
                            paint.style = android.graphics.Paint.Style.FILL
                            paint.isAntiAlias = true
                        }
                        title = zona.nombre
                        snippet = "Nivel: ${DangerLevelColors.getNombreNivel(nivelUI)} • Radio: ${zona.radio}m"
                        setOnMarkerClickListener { clickedMarker, mapView ->
                            Log.d("OpenStreetMap", "👆 Tap en zona: ${zona.nombre}")
                            onZonaClick?.invoke(zona)
                            true
                        }
                    }
                    map.overlays.add(marker)

                    Log.d("OpenStreetMap", "✅ Zona ${zona.nombre} (nivel $nivelUI) dibujada con ${puntosCirculo.size} puntos")

                } catch (e: Exception) {
                    Log.e("OpenStreetMap", "❌ Error dibujando zona ${zona.nombre}: ${e.message}", e)
                }
            }

            // ZONE PREVIEW while creating (dashed red — always red for new/unsaved)
            if (zonaPreviewLat != null && zonaPreviewLon != null && zonaPreviewRadio != null) {
                try {
                    val center = GeoPoint(zonaPreviewLat, zonaPreviewLon)

                    Log.d("OpenStreetMap", "👁️ Preview zona en ($zonaPreviewLat, $zonaPreviewLon) - Radio: ${zonaPreviewRadio}m")

                    val puntosPreview = crearCirculoPersonalizado(
                        lat = zonaPreviewLat,
                        lon = zonaPreviewLon,
                        radioMetros = zonaPreviewRadio
                    )

                    val circle = Polygon(map).apply {
                        points = puntosPreview
                        fillPaint.color = android.graphics.Color.argb(68, 255, 82, 82)
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

            // User location dot (blue)
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

            // Center pin
            if (showCenterPin) {
                map.overlays.add(CenteredPinOverlay(context))
            }

            map.invalidate()
        }
    )

    LaunchedEffect(recenterTrigger) {
        if (recenterTrigger > 0) {
            Log.d("OpenStreetMap", "🎯 Recentrando a: $centerLat, $centerLon")
            mapView.controller.animateTo(GeoPoint(centerLat, centerLon))
        }
    }

    LaunchedEffect(zoomInTrigger) {
        if (zoomInTrigger > 0) {
            mapView.controller.zoomIn()
        }
    }

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

// Uses Cos for lat, Sin for lon (correct formula)
private fun crearCirculoPersonalizado(
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
        val newLat = lat + (radioGradosLat * Math.cos(angulo))
        val newLon = lon + (radioGradosLon * Math.sin(angulo))
        puntos.add(GeoPoint(newLat, newLon))
    }

    return puntos
}