package com.rutai.app.ui.theme

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.rutai.app.R

/**
 * Colores para los 3 niveles de peligro:
 * Nivel 1 = Turquesa (bajo riesgo)
 * Nivel 2 = Naranja  (riesgo medio)
 * Nivel 3 = Rojo     (riesgo alto)
 */
object DangerLevelColors {

    // ─── Turquesa (Nivel 1) ───────────────────────────────────────────────
    val Level1Light    = Color(0xFF009688)  // Turquesa
    val Level1Dark     = Color(0xFF4DB6AC)
    val Level1BgLight  = Color(0xFFE0F2F1)
    val Level1BgDark   = Color(0xFF1B3A38)

    // ─── Naranja (Nivel 2) ────────────────────────────────────────────────
    val Level2Light    = Color(0xFFE65100)  // Naranja oscuro
    val Level2Dark     = Color(0xFFFF9800)
    val Level2BgLight  = Color(0xFFFFF3E0)
    val Level2BgDark   = Color(0xFF3D2A00)

    // ─── Rojo (Nivel 3) ───────────────────────────────────────────────────
    val Level3Light    = Color(0xFFD32F2F)  // Rojo
    val Level3Dark     = Color(0xFFEF5350)
    val Level3BgLight  = Color(0xFFFFEBEE)
    val Level3BgDark   = Color(0xFF3D1F1F)

    /** Retorna el color principal del nivel según el tema */
    fun getColor(nivel: Int, isDarkTheme: Boolean): Color = when (nivel) {
        1    -> if (isDarkTheme) Level1Dark  else Level1Light
        2    -> if (isDarkTheme) Level2Dark  else Level2Light
        3    -> if (isDarkTheme) Level3Dark  else Level3Light
        else -> if (isDarkTheme) Level3Dark  else Level3Light  // fallback al máximo
    }

    /** Retorna el color de fondo del nivel según el tema */
    fun getBackground(nivel: Int, isDarkTheme: Boolean): Color = when (nivel) {
        1    -> if (isDarkTheme) Level1BgDark  else Level1BgLight
        2    -> if (isDarkTheme) Level2BgDark  else Level2BgLight
        3    -> if (isDarkTheme) Level3BgDark  else Level3BgLight
        else -> if (isDarkTheme) Level3BgDark  else Level3BgLight
    }

    /** Nombre descriptivo del nivel */
    fun getNombreNivel(nivel: Int, context: Context): String = when (nivel) {
        1    -> context.getString(R.string.risk_low)
        2    -> context.getString(R.string.risk_medium)
        3    -> context.getString(R.string.risk_high)
        else -> context.getString(R.string.risk_high)
    }

    /** Emoji / indicador visual del nivel */
    fun getIndicador(nivel: Int): String = when (nivel) {
        1    -> "●"   // Un punto turquesa
        2    -> "●●"  // Dos puntos naranja
        3    -> "●●●" // Tres puntos rojo
        else -> "●●●"
    }

    /** Color ARGB para usar en OSMDroid/Android Canvas */
    fun getArgbColor(nivel: Int, isDarkTheme: Boolean): Int {
        return when (nivel) {
            1 -> if (isDarkTheme)
                android.graphics.Color.argb(120, 77, 182, 172)   // Turquesa dark
            else
                android.graphics.Color.argb(120, 0, 150, 136)    // Turquesa light
            2 -> if (isDarkTheme)
                android.graphics.Color.argb(130, 255, 152, 0)    // Naranja dark
            else
                android.graphics.Color.argb(130, 230, 81, 0)     // Naranja light
            3 -> if (isDarkTheme)
                android.graphics.Color.argb(140, 239, 68, 68)    // Rojo dark
            else
                android.graphics.Color.argb(140, 220, 38, 38)    // Rojo light
            else ->
                android.graphics.Color.argb(100, 156, 163, 175)  // Gris fallback
        }
    }

    /** Clamp: convierte cualquier nivel (1-5) al nuevo rango (1-3) */
    fun clampNivel(nivel: Int): Int = when {
        nivel <= 1 -> 1
        nivel >= 3 -> 3
        else       -> nivel
    }
}