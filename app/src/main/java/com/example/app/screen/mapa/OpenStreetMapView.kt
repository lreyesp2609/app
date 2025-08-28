package com.example.app.screen.mapa

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
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
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent

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
    onLocationSelected: (lat: Double, lon: Double) -> Unit = { _, _ -> }
) {
    // ✅ Solo se crea una vez y se recuerda
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
                }
                map.overlays.add(circleMarker)
            }

            map.overlays.add(CenteredPinOverlay(context))
        }
    )

    // Centrar mapa cuando se presione botón
    LaunchedEffect(recenterTrigger) {
        mapView.controller.animateTo(GeoPoint(latitude, longitude))
    }

    // Detectar movimiento del mapa
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

