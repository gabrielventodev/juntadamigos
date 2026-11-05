package com.juntadamigos.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.juntadamigos.app.R

/*
 * Tipografía del diseño: Bricolage Grotesque para títulos y minutos, Figtree para texto.
 * Las dos son variables (OFL, ver docs/fonts); cada peso es una instancia del mismo archivo.
 */

@OptIn(ExperimentalTextApi::class)
private fun variable(res: Int, weight: Int, vararg extra: FontVariation.Setting) =
    Font(res, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight), *extra))

@OptIn(ExperimentalTextApi::class)
val Display = FontFamily(
    // opsz 32: el corte "display" de Bricolage, como se ve en el diseño a 20-36 px.
    listOf(500, 600, 700, 800).map { variable(R.font.bricolage_grotesque, it, FontVariation.Setting("opsz", 32f)) },
)

val Sans = FontFamily(listOf(400, 500, 600, 700).map { variable(R.font.figtree, it) })

private fun display(size: Int, line: Int, weight: Int = 700, tracking: Double = 0.0) = TextStyle(
    fontFamily = Display, fontWeight = FontWeight(weight), fontSize = size.sp, lineHeight = line.sp, letterSpacing = tracking.sp,
)

private fun sans(size: Int, line: Int, weight: Int, tracking: Double) = TextStyle(
    fontFamily = Sans, fontWeight = FontWeight(weight), fontSize = size.sp, lineHeight = line.sp, letterSpacing = tracking.sp,
)

internal val AppTypography = Typography(
    displaySmall = display(36, 44, tracking = -0.3),
    headlineLarge = display(32, 40, tracking = -0.3),
    headlineMedium = display(28, 36, tracking = -0.2),
    headlineSmall = display(24, 32),
    titleLarge = display(22, 28, weight = 600),
    titleMedium = sans(16, 24, 600, 0.1),
    titleSmall = sans(14, 20, 600, 0.1),
    bodyLarge = sans(16, 24, 400, 0.2),
    bodyMedium = sans(14, 20, 400, 0.2),
    bodySmall = sans(12, 16, 400, 0.3),
    labelLarge = sans(14, 20, 600, 0.1),
    labelMedium = sans(12, 16, 600, 0.4),
    labelSmall = sans(11, 16, 600, 0.5),
)

/** Minutos destacados (estilos minutes-xl / minutes-md del diseño). */
object Minutes {
    val Xl = display(32, 36)
    val Md = display(16, 20)
}
