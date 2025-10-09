package com.example.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Definir colores personalizados
object AppColors {
    // Colores para tema claro
    val Chestnut = Color(0xFFB84B44)
    val Linen = Color(0xFFFAE7E0)

    // Colores para tema oscuro
    val WaikawaGray = Color(0xFF616D9F)
    val Mirage = Color(0xFF1C1D31)
}

// Crear los esquemas de colores
val LightColorScheme = lightColorScheme(
    primary = AppColors.Chestnut,
    secondary = AppColors.Chestnut,
    background = AppColors.Linen,
    surface = AppColors.Linen,
    tertiary = AppColors.Chestnut.copy(alpha = 0.5f),
    tertiaryContainer = AppColors.Linen.copy(alpha = 0.3f),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black,
    onTertiary = Color.White,
    onTertiaryContainer = Color.Black
)

val DarkColorScheme = darkColorScheme(
    primary = AppColors.WaikawaGray,
    secondary = AppColors.WaikawaGray,
    background = AppColors.Mirage,
    surface = AppColors.Mirage,
    tertiary = AppColors.WaikawaGray.copy(alpha = 0.7f),
    tertiaryContainer = AppColors.Mirage.copy(alpha = 0.5f),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onTertiary = Color.White,
    onTertiaryContainer = Color.White
)

// Función para obtener el esquema de colores según el tema
@Composable
fun getColorScheme(darkTheme: Boolean = isSystemInDarkTheme()): ColorScheme {
    return if (darkTheme) DarkColorScheme else LightColorScheme
}

// Gradiente de fondo adaptado
@Composable
fun getBackgroundGradient(isDarkTheme: Boolean = isSystemInDarkTheme()): Brush {
    return if (isDarkTheme) {
        Brush.verticalGradient(
            colors = listOf(AppColors.Mirage, AppColors.WaikawaGray.copy(alpha = 0.3f))
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(AppColors.Linen, AppColors.Chestnut.copy(alpha = 0.1f))
        )
    }
}

// Función adicional para obtener colores específicos
@Composable
fun getPrimaryColor(isDarkTheme: Boolean = isSystemInDarkTheme()): Color {
    return if (isDarkTheme) AppColors.WaikawaGray else AppColors.Chestnut
}

@Composable
fun getBackgroundColor(isDarkTheme: Boolean = isSystemInDarkTheme()): Color {
    return if (isDarkTheme) AppColors.Mirage else AppColors.Linen
}
