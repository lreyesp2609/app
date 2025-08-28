package com.example.app.screen.mapa

import android.content.Context
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.app.models.UbicacionUsuarioResponse
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.config.Configuration
import com.example.app.R

@Composable
fun SimpleMapOSM(
    modifier: Modifier = Modifier,
    userLat: Double = 0.0,
    userLon: Double = 0.0,
    ubicaciones: List<UbicacionUsuarioResponse> = emptyList(),
    mapCenterLat: Double = 0.0,
    mapCenterLon: Double = 0.0,
    zoom: Double = 16.0,
    recenterTrigger: Int = 0,
    context: Context = LocalContext.current
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

            // Punto azul del usuario
            val userMarker = Marker(map).apply {
                position = GeoPoint(userLat, userLon)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = ShapeDrawable(OvalShape()).apply {
                    intrinsicHeight = 40
                    intrinsicWidth = 40
                    paint.color = android.graphics.Color.BLUE
                    paint.style = android.graphics.Paint.Style.FILL
                }
                title = "Tu ubicación"
            }
            map.overlays.add(userMarker)

            // Puntos rojos guardados
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

    LaunchedEffect(recenterTrigger) {
        mapView.controller.animateTo(GeoPoint(mapCenterLat, mapCenterLon))
    }
}
