package com.juntadamigos.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import com.juntadamigos.core.PlaceCategory
import com.juntadamigos.core.TravelMode

/**
 * Íconos del diseño: grilla 24, trazo 2, puntas redondeadas (los mismos paths SVG).
 * El color lo pone `Icon(tint = ...)`; acá van en negro como en cualquier ImageVector.
 */
object AppIcons {
    val Back = icon("M15 5l-7 7 7 7")
    val Add = icon("M12 5v14M5 12h14")
    val Next = icon("M5 12h14M13 6l6 6-6 6")
    val Close = icon("M6 6l12 12M18 6L6 18")
    val Edit = icon("M4 20h4L19 9l-4-4L4 16z", "M13.5 6.5l4 4")
    val Check = icon("M5 12.5l4.5 4.5L19 7.5")
    val Search = icon(circle(11f, 11f, 6.5f), "M16 16l4.5 4.5")
    val Pin = icon("M12 21s-6.5-6-6.5-11a6.5 6.5 0 0 1 13 0c0 5-6.5 11-6.5 11z", circle(12f, 10f, 2.2f))
    val Navigate = icon("M4 11l16-7-7 16-2-7z")
    val Share = icon("M4 12v7a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1v-7M12 15V3M8 7l4-4 4 4")
    val List = icon("M8 6h12M8 12h12M8 18h12M4 6h.01M4 12h.01M4 18h.01")
    val Refresh = icon("M20 12a8 8 0 1 1-2.3-5.6M20 4v5h-5")
    val Info = icon(circle(12f, 12f, 9f), "M12 7.5v5.5M12 16.5v.01")
    val Warning = icon("M12 4l9.5 16.5h-19z", "M12 10v4.5M12 17.7v.1")
    val WifiOff = icon(
        "M3 3l18 18M8.5 16.5a5 5 0 0 1 7 0M5 12.9a10 10 0 0 1 4-2.4M19 12.9a10 10 0 0 0-4.3-2.5" +
            "M2 8.8a15 15 0 0 1 4-2.5M22 8.8A15 15 0 0 0 12 5M12 20h.01",
    )
    val Star = icon("M12 3l2.7 5.6 6.1.8-4.5 4.2 1.1 6-5.4-3-5.4 3 1.1-6L3.2 9.4l6.1-.8z", filled = true)

    // Modos de viaje
    val Car = icon(
        "M5 17H4a1 1 0 0 1-1-1v-4l2-5a2 2 0 0 1 1.9-1.3h10.2A2 2 0 0 1 19 7l2 5v4a1 1 0 0 1-1 1h-1M5 12h14M9 17h6",
        circle(7.5f, 17f, 1.5f), circle(16.5f, 17f, 1.5f),
    )
    val Bus = icon(
        "M7 3.5h10a3 3 0 0 1 3 3v8a3 3 0 0 1-3 3H7a3 3 0 0 1-3-3v-8a3 3 0 0 1 3-3z",
        "M4 11h16M8 21v-3.5M16 21v-3.5", circle(8.5f, 14.3f, 0.8f), circle(15.5f, 14.3f, 0.8f),
    )
    val Bike = icon(
        circle(6f, 16f, 3.5f), circle(18f, 16f, 3.5f),
        "M6 16l3.5-8h5.5l3 8M9.5 8L12 16l3-8M8 6h3M14 5.5h2.5",
    )
    val Walk = icon(circle(13f, 4.5f, 1.8f), "M11 21l1.5-5.5-2.5-2.5 1-5 3.5 1.5 2.5 3 2.5.5M12.5 15.5l3 1.5.5 4M10 8L7.5 10.5")

    // Tipos de lugar
    val Cafe = icon("M4 9h12v5a4 4 0 0 1-4 4H8a4 4 0 0 1-4-4z", "M16 10h1.5a2.5 2.5 0 0 1 0 5H16M8 3v2M12 3v2")
    val Bar = icon("M5 4h14l-7 8z", "M12 12v8M8 20h8")
    val Restaurant = icon("M5 3v6a2 2 0 0 0 4 0V3M7 11v10", "M17 21V3c-2.2 1.2-3 4-3 7 0 1.5 1 2.5 3 2.5")
    val Park = icon("M12 21v-5", "M12 3a5.5 5.5 0 0 0-4.5 8.7A4.5 4.5 0 0 0 9 16h6a4.5 4.5 0 0 0 1.5-4.3A5.5 5.5 0 0 0 12 3z")
    val Shopping = icon("M5 8h14l-1 12H6z", "M9 8V6a3 3 0 0 1 6 0v2")

    fun mode(m: TravelMode) = when (m) {
        TravelMode.DRIVE -> Car
        TravelMode.TRANSIT -> Bus
        TravelMode.BICYCLE -> Bike
        TravelMode.WALK -> Walk
    }

    fun category(c: PlaceCategory) = when (c) {
        PlaceCategory.CAFE -> Cafe
        PlaceCategory.BAR -> Bar
        PlaceCategory.RESTAURANT -> Restaurant
        PlaceCategory.PARK -> Park
        PlaceCategory.SHOPPING_MALL -> Shopping
    }
}

private fun circle(cx: Float, cy: Float, r: Float) = "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0z"

private fun icon(vararg paths: String, filled: Boolean = false): ImageVector =
    ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
        paths.forEach { d ->
            addPath(
                pathData = PathParser().parsePathString(d).toNodes(),
                fill = if (filled) SolidColor(Color.Black) else null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()
