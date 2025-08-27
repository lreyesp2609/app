package com.example.app.screen.mapa

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import com.example.app.R

// Overlay que dibuja el pin rojo centrado
class CenteredPinOverlay(private val context: Context) : Overlay() {
    private val pinDrawable: Drawable? = ContextCompat.getDrawable(context, R.drawable.ic_marker_red)

    override fun draw(canvas: Canvas?, mapView: MapView?, shadow: Boolean) {
        if (canvas == null || mapView == null || shadow) return

        pinDrawable?.let { drawable ->
            val centerX = mapView.width / 2
            val centerY = mapView.height / 2
            val width = drawable.intrinsicWidth
            val height = drawable.intrinsicHeight
            val left = centerX - width / 2
            val top = centerY - height
            val right = left + width
            val bottom = centerY
            drawable.setBounds(left, top, right, bottom)
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
    context: Context = LocalContext.current
) {
    val mapView = remember { mutableStateOf<MapView?>(null) }

    LaunchedEffect(Unit) {
        Configuration.getInstance().load(
            context,
            context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(zoom)
                    setBuiltInZoomControls(false)
                    mapView.value = this
                }
            },
            update = { map ->
                map.overlays.clear()

                if (showUserLocation) {
                    val circleSize = 40
                    val shape = ShapeDrawable(OvalShape()).apply {
                        intrinsicHeight = circleSize
                        intrinsicWidth = circleSize
                        paint.color = android.graphics.Color.BLUE
                        paint.style = android.graphics.Paint.Style.FILL
                    }
                    val geoPoint = GeoPoint(latitude, longitude)
                    val circleMarker = Marker(map).apply {
                        position = geoPoint
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        icon = shape
                    }
                    map.overlays.add(circleMarker)
                }

                map.overlays.add(CenteredPinOverlay(context))
            }
        )

        // Centrar mapa al cambiar recenterTrigger
        LaunchedEffect(recenterTrigger) {
            mapView.value?.controller?.animateTo(GeoPoint(latitude, longitude))
        }

        FloatingActionButton(
            onClick = { mapView.value?.controller?.animateTo(GeoPoint(latitude, longitude)) },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            containerColor = Color(0xFF2196F3),
            contentColor = Color.White
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "Centrar en mi ubicación"
            )
        }
    }
}