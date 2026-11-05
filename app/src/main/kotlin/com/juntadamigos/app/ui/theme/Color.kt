package com.juntadamigos.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/*
 * Paleta del diseño de Claude Design (tokens.json "Juntadamigos" v1).
 * Verde = acciones, terracota (tertiary) = LUGARES, friend-N = PERSONAS. Nunca mezclar.
 */

internal val LightScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF0F6E58),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC2EDE0),
    onPrimaryContainer = Color(0xFF00382B),
    secondary = Color(0xFF5E5646),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE9E0CF),
    onSecondaryContainer = Color(0xFF1E1B15),
    tertiary = Color(0xFFAD3317),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDAD0),
    onTertiaryContainer = Color(0xFF3A0A00),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFFBF8F2),
    onBackground = Color(0xFF1E1B15),
    surface = Color(0xFFFBF8F2),
    onSurface = Color(0xFF1E1B15),
    surfaceVariant = Color(0xFFEBE4D8),
    onSurfaceVariant = Color(0xFF4C463A),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF6F1E8),
    surfaceContainer = Color(0xFFF1EBE0),
    surfaceContainerHigh = Color(0xFFEBE4D8),
    surfaceContainerHighest = Color(0xFFE5DDD0),
    outline = Color(0xFF7B7363),
    outlineVariant = Color(0xFFD3C9B8),
    inverseSurface = Color(0xFF33302A),
    inverseOnSurface = Color(0xFFF7F0E6),
    inversePrimary = Color(0xFF7FD3B8),
    surfaceTint = Color(0xFF0F6E58),
)

internal val DarkScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF7FD3B8),
    onPrimary = Color(0xFF00382B),
    primaryContainer = Color(0xFF00513F),
    onPrimaryContainer = Color(0xFFC2EDE0),
    secondary = Color(0xFFCDC3AE),
    onSecondary = Color(0xFF342E20),
    secondaryContainer = Color(0xFF4B4435),
    onSecondaryContainer = Color(0xFFEDE3CF),
    tertiary = Color(0xFFFFB4A0),
    onTertiary = Color(0xFF5A1100),
    tertiaryContainer = Color(0xFF84250A),
    onTertiaryContainer = Color(0xFFFFDAD0),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF15120D),
    onBackground = Color(0xFFEBE2D5),
    surface = Color(0xFF15120D),
    onSurface = Color(0xFFEBE2D5),
    surfaceVariant = Color(0xFF2D2923),
    onSurfaceVariant = Color(0xFFCFC5B4),
    surfaceContainerLowest = Color(0xFF0F0C08),
    surfaceContainerLow = Color(0xFF1D1A14),
    surfaceContainer = Color(0xFF221E18),
    surfaceContainerHigh = Color(0xFF2D2923),
    surfaceContainerHighest = Color(0xFF38332D),
    outline = Color(0xFFA59C8C),
    outlineVariant = Color(0xFF4D473B),
    inverseSurface = Color(0xFFEBE2D5),
    inverseOnSurface = Color(0xFF33302A),
    inversePrimary = Color(0xFF0F6E58),
    surfaceTint = Color(0xFF7FD3B8),
)

/** Colores del diseño que Material 3 no tiene: personas, advertencia, rating y mapa. */
@Immutable
data class AppColors(
    val friends: List<Color>,
    val onFriend: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val rating: Color,
    val mapLand: Color,
    val mapRoad: Color,
    val mapRoadEdge: Color,
    val mapPark: Color,
    val mapWater: Color,
    val mapLabel: Color,
) {
    /** Color de la persona en la posición [index] (se repite después de 6). */
    fun friend(index: Int): Color = friends[index.mod(friends.size)]
}

internal val LightAppColors = AppColors(
    friends = listOf(
        Color(0xFF2B5BB5), Color(0xFF7B3FB0), Color(0xFF8A6100),
        Color(0xFFAD2E6B), Color(0xFF0A6F86), Color(0xFF5F5A4E),
    ),
    onFriend = Color(0xFFFFFFFF),
    warning = Color(0xFF8A5A00),
    warningContainer = Color(0xFFFFE2B3),
    onWarningContainer = Color(0xFF2C1C00),
    rating = Color(0xFF8A5A00),
    mapLand = Color(0xFFEFE9DD),
    mapRoad = Color(0xFFFFFFFF),
    mapRoadEdge = Color(0xFFD9CFBD),
    mapPark = Color(0xFFCBE3C3),
    mapWater = Color(0xFFBCD8E0),
    mapLabel = Color(0xFF5A5345),
)

internal val DarkAppColors = AppColors(
    friends = listOf(
        Color(0xFFA8C3FF), Color(0xFFD9B8FF), Color(0xFFF0C04F),
        Color(0xFFFFAFD0), Color(0xFF6ED3EB), Color(0xFFC9C3B4),
    ),
    onFriend = Color(0xFF14110C),
    warning = Color(0xFFFFC24D),
    warningContainer = Color(0xFF5C3D00),
    onWarningContainer = Color(0xFFFFE2B3),
    rating = Color(0xFFFFC24D),
    mapLand = Color(0xFF26221B),
    mapRoad = Color(0xFF3B362D),
    mapRoadEdge = Color(0xFF17140F),
    mapPark = Color(0xFF1E3A2B),
    mapWater = Color(0xFF1A3441),
    mapLabel = Color(0xFFBDB3A0),
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }
