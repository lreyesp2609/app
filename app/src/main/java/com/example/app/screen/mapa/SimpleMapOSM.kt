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
    mapCenterLat: Double = 0.0,
    mapCenterLon: Double = 0.0,
    zoom: Double = 16.0,
    recenterTrigger: Int = 0,
    context: Context = LocalContext.current,
    transportMode: String,
    routeGeometry: String? = null, // PARÁMETRO PARA LA RUTA
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

            // AGREGAR LA RUTA PRIMERO (para que esté debajo de los marcadores)
            routeGeometry?.let { geometry ->
                try {
                    val routePoints = geometry.decodePolyline()
                    if (routePoints.isNotEmpty()) {
                        val polyline = Polyline().apply {
                            setPoints(routePoints)

                            // Color según el modo de transporte
                            color = when (transportMode) {
                                "walking" -> Color(0xFF4CAF50).toArgb() // Verde para caminar
                                "cycling" -> Color(0xFF2196F3).toArgb() // Azul para bicicleta
                                "driving" -> Color(0xFFFF9800).toArgb() // Naranja para carro
                                else -> Color(0xFF4CAF50).toArgb()
                            }

                            width = 12f // Grosor de la línea
                        }
                        map.overlays.add(polyline)
                    }
                } catch (e: Exception) {
                    // Manejar error de decodificación
                    android.util.Log.e("SimpleMapOSM", "Error decoding route geometry", e)
                }
            }

            // Punto azul del usuario
            val userMarker = Marker(map).apply {
                position = GeoPoint(userLat, userLon)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)

                icon = ShapeDrawable(OvalShape()).apply {
                    intrinsicHeight = 36
                    intrinsicWidth = 36
                    paint.apply {
                        color = android.graphics.Color.rgb(33, 150, 243) // Azul material
                        style = android.graphics.Paint.Style.FILL
                        isAntiAlias = true
                    }
                }
                title = "Tu ubicación"
            }
            map.overlays.add(userMarker)

            // Puntos rojos guardados (destinos)
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

            // Si hay una ruta activa, ajustar el zoom para mostrar toda la ruta
            routeGeometry?.let { geometry ->
                try {
                    val routePoints = geometry.decodePolyline()
                    if (routePoints.isNotEmpty()) {
                        // Calcular bounding box de la ruta
                        val allPoints = mutableListOf<GeoPoint>().apply {
                            addAll(routePoints)
                            add(GeoPoint(userLat, userLon)) // Incluir posición del usuario
                            ubicaciones.forEach {
                                add(GeoPoint(it.latitud, it.longitud))
                            }
                        }

                        if (allPoints.size > 1) {
                            map.zoomToBoundingBox(
                                org.osmdroid.util.BoundingBox.fromGeoPoints(allPoints),
                                true,
                                100 // padding en pixels
                            )
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SimpleMapOSM", "Error calculating bounding box", e)
                }
            }

            map.invalidate()
        }
    )

    // Efecto para recentrar el mapa
    LaunchedEffect(recenterTrigger) {
        if (routeGeometry == null) {
            // Solo recentrar manualmente si no hay ruta activa
            mapView.controller.animateTo(GeoPoint(mapCenterLat, mapCenterLon))
        }
    }
}