package com.example.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// 🎨 Paleta de colores mejorada
object AppColors {
    // === TEMA CLARO ===
    // Primarios
    val Crimson = Color(0xFFDC2626)           // Rojo vibrante para acciones principales
    val CrimsonLight = Color(0xFFEF4444)      // Variante más clara
    val CrimsonDark = Color(0xFFB91C1C)       // Variante más oscura

    // Fondo y superficies
    val Cream = Color(0xFFFFFBF5)             // Fondo principal más cálido
    val Sand = Color(0xFFFEF3E2)              // Superficie secundaria
    val SandDark = Color(0xFFF5E6D3)          // Superficie con más contraste

    // === TEMA OSCURO ===
    // Primarios
    val Indigo = Color(0xFF6366F1)            // Púrpura-azul moderno
    val IndigoLight = Color(0xFF818CF8)       // Variante más clara
    val IndigoDark = Color(0xFF4F46E5)        // Variante más oscura

    // Fondo y superficies
    val Slate = Color(0xFF0F172A)             // Fondo principal oscuro
    val SlateLight = Color(0xFF1E293B)        // Superficie elevada
    val SlateLighter = Color(0xFF334155)      // Superficie más elevada

    // === COLORES DE ESTADO (universal) ===
    // Éxito/Seguro
    val Success = Color(0xFF10B981)           // Verde esmeralda
    val SuccessLight = Color(0xFF34D399)
    val SuccessDark = Color(0xFF059669)

    // Advertencia
    val Warning = Color(0xFFF59E0B)           // Ámbar
    val WarningLight = Color(0xFFFBBF24)
    val WarningDark = Color(0xFFD97706)

    // Peligro/Error
    val Danger = Color(0xFFEF4444)            // Rojo brillante
    val DangerLight = Color(0xFFF87171)
    val DangerDark = Color(0xFFDC2626)

    // Info
    val Info = Color(0xFF3B82F6)              // Azul brillante
    val InfoLight = Color(0xFF60A5FA)
    val InfoDark = Color(0xFF2563EB)

    // === COLORES COMPLEMENTARIOS ===
    val Gray50 = Color(0xFFF9FAFB)
    val Gray100 = Color(0xFFF3F4F6)
    val Gray200 = Color(0xFFE5E7EB)
    val Gray300 = Color(0xFFD1D5DB)
    val Gray400 = Color(0xFF9CA3AF)
    val Gray500 = Color(0xFF6B7280)
    val Gray600 = Color(0xFF4B5563)
    val Gray700 = Color(0xFF374151)
    val Gray800 = Color(0xFF1F2937)
    val Gray900 = Color(0xFF111827)
}

// 🌞 TEMA CLARO
val LightColorScheme = lightColorScheme(
    // Primarios - Cambiado a un color más neutral
    primary = AppColors.InfoDark,
    onPrimary = Color.White,
    primaryContainer = AppColors.InfoLight.copy(alpha = 0.1f),
    onPrimaryContainer = AppColors.InfoDark,

    // Secundarios - También cambiado
    secondary = AppColors.Info,
    onSecondary = Color.White,
    secondaryContainer = AppColors.Sand,
    onSecondaryContainer = AppColors.Gray900,

    // Terciarios (para elementos de énfasis medio)
    tertiary = AppColors.Info,
    onTertiary = Color.White,
    tertiaryContainer = AppColors.InfoLight.copy(alpha = 0.1f),
    onTertiaryContainer = AppColors.InfoDark,

    // Fondos y superficies
    background = AppColors.Cream,
    onBackground = AppColors.Gray900,
    surface = Color.White,
    onSurface = AppColors.Gray900,
    surfaceVariant = AppColors.Sand,
    onSurfaceVariant = AppColors.Gray700,

    // Estados
    error = AppColors.Danger,
    onError = Color.White,
    errorContainer = AppColors.DangerLight.copy(alpha = 0.1f),
    onErrorContainer = AppColors.DangerDark,

    // Bordes y separadores
    outline = AppColors.Gray300,
    outlineVariant = AppColors.Gray200,

    // Overlays
    scrim = Color.Black.copy(alpha = 0.5f),
    inverseSurface = AppColors.Gray900,
    inverseOnSurface = AppColors.Gray50,
    inversePrimary = AppColors.CrimsonLight,

    // Tonos de superficie
    surfaceTint = AppColors.Crimson,
)

// 🌙 TEMA OSCURO
val DarkColorScheme = darkColorScheme(
    // Primarios
    primary = AppColors.Indigo,
    onPrimary = Color.White,
    primaryContainer = AppColors.IndigoDark,
    onPrimaryContainer = AppColors.IndigoLight,

    // Secundarios
    secondary = AppColors.IndigoLight,
    onSecondary = AppColors.Slate,
    secondaryContainer = AppColors.SlateLight,
    onSecondaryContainer = AppColors.IndigoLight,

    // Terciarios
    tertiary = AppColors.Info,
    onTertiary = Color.White,
    tertiaryContainer = AppColors.InfoDark,
    onTertiaryContainer = AppColors.InfoLight,

    // Fondos y superficies
    background = AppColors.Slate,
    onBackground = AppColors.Gray100,
    surface = AppColors.SlateLight,
    onSurface = AppColors.Gray100,
    surfaceVariant = AppColors.SlateLighter,
    onSurfaceVariant = AppColors.Gray300,

    // Estados
    error = AppColors.DangerLight,
    onError = AppColors.Slate,
    errorContainer = AppColors.DangerDark,
    onErrorContainer = AppColors.DangerLight,

    // Bordes y separadores
    outline = AppColors.Gray700,
    outlineVariant = AppColors.Gray800,

    // Overlays
    scrim = Color.Black.copy(alpha = 0.7f),
    inverseSurface = AppColors.Gray100,
    inverseOnSurface = AppColors.Gray900,
    inversePrimary = AppColors.IndigoDark,

    // Tonos de superficie
    surfaceTint = AppColors.Indigo,
)

// 🎨 Función para obtener el esquema de colores
@Composable
fun getColorScheme(darkTheme: Boolean = isSystemInDarkTheme()): ColorScheme {
    return if (darkTheme) DarkColorScheme else LightColorScheme
}

// 🌈 Gradientes mejorados
@Composable
fun getBackgroundGradient(isDarkTheme: Boolean = isSystemInDarkTheme()): Brush {
    return if (isDarkTheme) {
        Brush.verticalGradient(
            colors = listOf(
                AppColors.Slate,
                AppColors.SlateLight.copy(alpha = 0.6f),
                AppColors.Slate
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                AppColors.Cream,
                AppColors.Sand.copy(alpha = 0.4f),
                AppColors.Cream
            )
        )
    }
}

// 🎯 Gradiente para tarjetas destacadas
@Composable
fun getCardGradient(isDarkTheme: Boolean = isSystemInDarkTheme()): Brush {
    return if (isDarkTheme) {
        Brush.linearGradient(
            colors = listOf(
                AppColors.Indigo.copy(alpha = 0.2f),
                AppColors.IndigoLight.copy(alpha = 0.1f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                AppColors.Crimson.copy(alpha = 0.05f),
                AppColors.CrimsonLight.copy(alpha = 0.02f)
            )
        )
    }
}

// 🚨 Colores específicos para estados de seguridad
object SecurityColors {
    @Composable
    fun getSafeColor(isDarkTheme: Boolean = isSystemInDarkTheme()): Color {
        return AppColors.Success
    }

    @Composable
    fun getWarningColor(isDarkTheme: Boolean = isSystemInDarkTheme()): Color {
        return AppColors.Warning
    }

    @Composable
    fun getDangerColor(isDarkTheme: Boolean = isSystemInDarkTheme()): Color {
        return AppColors.Danger
    }

    @Composable
    fun getSafeBackground(isDarkTheme: Boolean = isSystemInDarkTheme()): Color {
        return if (isDarkTheme) {
            AppColors.Success.copy(alpha = 0.15f)
        } else {
            AppColors.Success.copy(alpha = 0.1f)
        }
    }

    @Composable
    fun getWarningBackground(isDarkTheme: Boolean = isSystemInDarkTheme()): Color {
        return if (isDarkTheme) {
            AppColors.Warning.copy(alpha = 0.15f)
        } else {
            AppColors.Warning.copy(alpha = 0.1f)
        }
    }

    @Composable
    fun getDangerBackground(isDarkTheme: Boolean = isSystemInDarkTheme()): Color {
        return if (isDarkTheme) {
            AppColors.Danger.copy(alpha = 0.15f)
        } else {
            AppColors.Danger.copy(alpha = 0.1f)
        }
    }
}

// 📍 Colores para el mapa y rutas
object MapColors {
    val UserLocation = Color(0xFF10B981)        // Verde para ubicación del usuario
    val SelectedLocation = Color(0xFFEF4444)    // Rojo para ubicación seleccionada
    val SafeRoute = Color(0xFF10B981)           // Verde para ruta segura
    val WarningRoute = Color(0xFFF59E0B)        // Ámbar para ruta con advertencia
    val DangerRoute = Color(0xFFEF4444)         // Rojo para ruta peligrosa
    val DangerZone = Color(0xFFDC2626)          // Rojo oscuro para zonas peligrosas
    val DangerZoneOverlay = Color(0xFFDC2626).copy(alpha = 0.2f)
}