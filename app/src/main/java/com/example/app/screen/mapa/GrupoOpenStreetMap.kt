package com.example.app.screen.mapa

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import android.util.Log
import com.example.app.models.MiembroUbicacion
import com.example.app.screen.grupos.components.MarkerColors
import com.example.app.screen.grupos.components.UserMarker
import com.example.app.screen.grupos.components.UserMarkerOverlay
import com.example.app.screen.grupos.components.getInitial

/**
 * Componente de mapa exclusivo para grupos
 * Muestra marcadores con iniciales de los miembros
 */
@Composable
fun GrupoOpenStreetMap(
    modifier: Modifier = Modifier,
    latitude: Double = 0.0,
    longitude: Double = 0.0,
    zoom: Double = 16.0,
    recenterTrigger: Int = 0,
    context: Context = LocalContext.current,
    miembrosGrupo: List<MiembroUbicacion> = emptyList(),
    currentUserId: Int = 0,
    currentUserName: String = "Tú",
    onLocationSelected: (lat: Double, lon: Double) -> Unit = { _, _ -> }
) {
    val mapView = rememberMapViewForGrupo(context, zoom)

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

            // 🆕 Crear lista de marcadores con iniciales
            val userMarkers = mutableListOf<UserMarker>()

            // Agregar usuario actual
            userMarkers.add(
                UserMarker(
                    position = GeoPoint(latitude, longitude),
                    name = "Tú",  // 👈 Siempre mostrar "Tú" para el usuario actual
                    initial = currentUserName.getInitial(),
                    backgroundColor = MarkerColors.CURRENT_USER_BG,
                    textColor = MarkerColors.CURRENT_USER_TEXT,
                    borderColor = MarkerColors.CURRENT_USER_BORDER,
                    isCurrentUser = true,
                    showName = true
                )
            )

            // 🆕 Agregar miembros del grupo con colores diferentes
            if (miembrosGrupo.isNotEmpty()) {
                Log.d("GrupoMap", "🗺️ Dibujando ${miembrosGrupo.size} miembros con iniciales")

                miembrosGrupo.forEachIndexed { index, miembro ->
                    val backgroundColor = if (miembro.esCreador) {
                        MarkerColors.CREATOR_BG
                    } else {
                        MarkerColors.getMemberColor(index)
                    }

                    val borderColor = if (miembro.esCreador) {
                        MarkerColors.CREATOR_BORDER
                    } else {
                        MarkerColors.getDarkerShade(backgroundColor)
                    }

                    userMarkers.add(
                        UserMarker(
                            position = GeoPoint(miembro.lat, miembro.lon),
                            name = miembro.nombre,
                            initial = miembro.nombre.getInitial(),
                            backgroundColor = backgroundColor,
                            textColor = android.graphics.Color.WHITE,
                            borderColor = borderColor,
                            isCurrentUser = false,
                            showName = true
                        )
                    )
                }
            }

            // Agregar el overlay de marcadores personalizados
            if (userMarkers.isNotEmpty()) {
                map.overlays.add(UserMarkerOverlay(context, userMarkers))
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
fun rememberMapViewForGrupo(context: Context, zoom: Double = 16.0): MapView {
    return remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(zoom)
            setBuiltInZoomControls(false)
        }
    }
}