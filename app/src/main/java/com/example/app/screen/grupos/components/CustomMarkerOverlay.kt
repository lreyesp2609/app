package com.example.app.screen.grupos.components

import android.content.Context
import android.graphics.*
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.util.GeoPoint

/**
 * Overlay personalizado para mostrar marcadores con iniciales de usuarios
 * Estilo Material Design limpio y bonito
 */
class UserMarkerOverlay(
    private val context: Context,
    private val userMarkers: List<UserMarker>
) : Overlay() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        color = Color.WHITE
    }
    private val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }

    // Tamaños más bonitos
    private val markerSize = 120f  // Un poco más grande
    private val strokeWidth = 4f   // Borde más delgado

    override fun draw(canvas: Canvas?, mapView: MapView?, shadow: Boolean) {
        if (canvas == null || mapView == null || shadow) return

        userMarkers.forEach { marker ->
            val point = mapView.projection.toPixels(marker.position, null)
            val x = point.x.toFloat()
            val y = point.y.toFloat()
            val radius = markerSize / 2

            // Sombra sutil
            paint.style = Paint.Style.FILL
            paint.color = Color.argb(40, 0, 0, 0)
            paint.maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
            canvas.drawCircle(x, y + 4f, radius, paint)
            paint.maskFilter = null

            // Círculo de fondo
            paint.style = Paint.Style.FILL
            paint.color = marker.backgroundColor
            canvas.drawCircle(x, y, radius, paint)

            // Borde blanco
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = strokeWidth
            paint.color = Color.WHITE
            canvas.drawCircle(x, y, radius, paint)

            // Inicial centrada perfectamente
            textPaint.textSize = markerSize * 0.45f
            textPaint.color = marker.textColor

            // Calcular posición Y para centrado vertical perfecto
            val textBounds = Rect()
            textPaint.getTextBounds(marker.initial, 0, marker.initial.length, textBounds)
            val textY = y - textBounds.exactCenterY()

            canvas.drawText(marker.initial, x, textY, textPaint)

            // Nombre debajo con fondo semitransparente
            if (marker.showName) {
                val nameTextSize = 28f
                namePaint.textSize = nameTextSize
                namePaint.color = Color.BLACK

                // Medir el texto para el fondo
                val nameBounds = Rect()
                namePaint.getTextBounds(marker.name, 0, marker.name.length, nameBounds)
                val nameWidth = nameBounds.width() + 24f
                val nameHeight = nameBounds.height() + 12f
                val nameY = y + radius + 20f

                // Fondo redondeado blanco
                paint.style = Paint.Style.FILL
                paint.color = Color.WHITE
                paint.setShadowLayer(6f, 0f, 2f, Color.argb(50, 0, 0, 0))

                val rect = RectF(
                    x - nameWidth / 2,
                    nameY - nameHeight / 2,
                    x + nameWidth / 2,
                    nameY + nameHeight / 2
                )
                canvas.drawRoundRect(rect, 12f, 12f, paint)
                paint.clearShadowLayer()

                // Texto del nombre
                namePaint.color = Color.parseColor("#212121")
                canvas.drawText(marker.name, x, nameY + nameBounds.height() / 2, namePaint)
            }
        }
    }
}

/**
 * Clase de datos para representar un marcador de usuario
 */
data class UserMarker(
    val position: GeoPoint,
    val name: String,
    val initial: String,
    val backgroundColor: Int,
    val textColor: Int,
    val borderColor: Int = Color.WHITE,
    val isCurrentUser: Boolean = false,
    val showName: Boolean = true
)

/**
 * Helper para crear colores de Material Design (más bonitos)
 */
object MarkerColors {
    // Usuario actual - Azul vibrante
    val CURRENT_USER_BG = Color.parseColor("#2196F3")      // Blue 500
    val CURRENT_USER_TEXT = Color.WHITE
    val CURRENT_USER_BORDER = Color.WHITE

    // Creador del grupo - Morado elegante
    val CREATOR_BG = Color.parseColor("#9C27B0")           // Purple 500
    val CREATOR_TEXT = Color.WHITE
    val CREATOR_BORDER = Color.WHITE

    // Miembros regulares - Paleta vibrante y balanceada
    val MEMBER_COLORS = listOf(
        Color.parseColor("#4CAF50"),  // Green 500
        Color.parseColor("#FF9800"),  // Orange 500
        Color.parseColor("#F44336"),  // Red 500
        Color.parseColor("#E91E63"),  // Pink 500
        Color.parseColor("#00BCD4"),  // Cyan 500
        Color.parseColor("#3F51B5"),  // Indigo 500
        Color.parseColor("#FF5722"),  // Deep Orange 500
        Color.parseColor("#009688")   // Teal 500
    )

    fun getMemberColor(index: Int): Int {
        return MEMBER_COLORS[index % MEMBER_COLORS.size]
    }

    fun getDarkerShade(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] *= 0.8f // Reduce brightness by 20%
        return Color.HSVToColor(hsv)
    }
}

/**
 * Extension para obtener la inicial de un nombre
 */
fun String.getInitial(): String {
    return this.trim().firstOrNull()?.uppercase() ?: "?"
}