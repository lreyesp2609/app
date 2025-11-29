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

    // 🔍 DIAGNÓSTICO: Log para verificar datos
    LaunchedEffect(miembrosGrupo) {
        Log.d("GrupoMap", "═══════════════════════════════════════")
        Log.d("GrupoMap", "🗺️ ACTUALIZANDO MARCADORES EN EL MAPA")
        Log.d("GrupoMap", "═══════════════════════════════════════")
        Log.d("GrupoMap", "📍 Mi ubicación: ($latitude, $longitude)")
        Log.d("GrupoMap", "👤 Mi ID: $currentUserId")
        Log.d("GrupoMap", "👥 Miembros recibidos: ${miembrosGrupo.size}")

        miembrosGrupo.forEachIndexed { index, miembro ->
            Log.d("GrupoMap", "   [$index] ID:${miembro.usuarioId} - ${miembro.nombre} en (${miembro.lat}, ${miembro.lon})")
        }
        Log.d("GrupoMap", "═══════════════════════════════════════")
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { mapView },
        update = { map ->
            map.overlays.clear()

            // 🆕 Crear lista de marcadores con iniciales
            val userMarkers = mutableListOf<UserMarker>()

            // ✅ PASO 1: Agregar TU marcador (usuario actual)
            userMarkers.add(
                UserMarker(
                    position = GeoPoint(latitude, longitude),
                    name = "Tú",
                    initial = currentUserName.getInitial(),
                    backgroundColor = MarkerColors.CURRENT_USER_BG,
                    textColor = MarkerColors.CURRENT_USER_TEXT,
                    borderColor = MarkerColors.CURRENT_USER_BORDER,
                    isCurrentUser = true,
                    showName = true
                )
            )

            Log.d("GrupoMap", "✅ Marcador propio agregado: Tú en ($latitude, $longitude)")

            // ✅ PASO 2: Agregar marcadores de OTROS miembros
            if (miembrosGrupo.isNotEmpty()) {
                Log.d("GrupoMap", "📍 Agregando ${miembrosGrupo.size} marcadores de otros miembros")

                miembrosGrupo.forEachIndexed { index, miembro ->
                    // 🔍 VERIFICACIÓN: Asegurar que no agregamos nuestro propio ID
                    if (miembro.usuarioId == currentUserId) {
                        Log.w("GrupoMap", "⚠️ ADVERTENCIA: Se intentó agregar el propio usuario (ID: ${miembro.usuarioId})")
                        return@forEachIndexed
                    }

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

                    Log.d("GrupoMap", "   ✅ Agregado: ${miembro.nombre} (ID:${miembro.usuarioId}) en (${miembro.lat}, ${miembro.lon})")
                }
            } else {
                Log.w("GrupoMap", "⚠️ Lista de miembros está vacía - solo se muestra el usuario actual")
            }

            Log.d("GrupoMap", "📊 Total de marcadores en el mapa: ${userMarkers.size}")

            // Agregar el overlay de marcadores personalizados
            if (userMarkers.isNotEmpty()) {
                map.overlays.add(UserMarkerOverlay(context, userMarkers))
                Log.d("GrupoMap", "✅ Overlay agregado al mapa con ${userMarkers.size} marcadores")
            }

            // Forzar redibujado
            map.invalidate()
        }
    )

    LaunchedEffect(recenterTrigger) {
        if (recenterTrigger > 0) {
            Log.d("GrupoMap", "🎯 Recentrando mapa en ($latitude, $longitude)")
        }
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