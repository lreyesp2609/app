package com.example.app.screen.mapa

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.add
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
import com.example.app.models.MiembroUbicacion
import com.example.app.models.getDisplayName
import com.example.app.screen.recordatorios.components.getIconResource
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import kotlin.text.clear

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

@Composable
fun OpenStreetMap(
    modifier: Modifier = Modifier,
    latitude: Double = 0.0,
    longitude: Double = 0.0,
    zoom: Double = 16.0,
    showUserLocation: Boolean = true,
    recenterTrigger: Int = 0,
    context: Context = LocalContext.current,
    pois: List<Feature> = emptyList(),
    showCenterPin: Boolean = true,
    miembrosGrupo: List<MiembroUbicacion> = emptyList(),  // 🆕 Nuevo parámetro
    onLocationSelected: (lat: Double, lon: Double) -> Unit = { _, _ -> }
) {
    val mapView = rememberMapView(context, zoom)

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

            // 🆕 Miembros del grupo (puntos verdes)
            if (miembrosGrupo.isNotEmpty()) {
                Log.d("OpenStreetMap", "🗺️ Dibujando ${miembrosGrupo.size} miembros en el mapa")
                miembrosGrupo.forEach { miembro ->
                    val marker = Marker(map).apply {
                        position = GeoPoint(miembro.lat, miembro.lon)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        icon = ShapeDrawable(OvalShape()).apply {
                            intrinsicHeight = 36
                            intrinsicWidth = 36
                            paint.color = android.graphics.Color.GREEN  // Verde para miembros
                            paint.style = android.graphics.Paint.Style.FILL
                        }
                        title = miembro.nombre
                        snippet = "Última actualización: ${miembro.timestamp}"
                    }
                    map.overlays.add(marker)
                }
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

    LaunchedEffect(recenterTrigger) {
        mapView.controller.animateTo(GeoPoint(latitude, longitude))
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