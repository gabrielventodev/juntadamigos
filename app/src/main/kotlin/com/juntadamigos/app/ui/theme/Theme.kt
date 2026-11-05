package com.juntadamigos.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp

/** Radios del diseño: xs 4, sm 8, md 12 (campos), lg 16 (tarjetas), xl 28 (hoja inferior). */
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * Tema fijo del diseño, claro u oscuro según el sistema. Sin color dinámico (Material You):
 * los colores de personas y lugares tienen que ser siempre los mismos para que el mapa se lea.
 */
@Composable
fun JuntadamigosTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAppColors provides if (dark) DarkAppColors else LightAppColors) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

/** Atajo a los colores extra del diseño: `AppTheme.colors.friend(i)`. */
object AppTheme {
    val colors: AppColors
        @Composable get() = LocalAppColors.current
}
