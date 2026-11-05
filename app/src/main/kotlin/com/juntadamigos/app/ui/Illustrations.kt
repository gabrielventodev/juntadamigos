package com.juntadamigos.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.juntadamigos.app.ui.theme.AppTheme

/** Gota de lugar del diseño, con la punta en (0,0) y 36×44 de tamaño. */
internal val PlacePinPath: Path = PathParser()
    .parsePathString("M0 0C-4-8-18-16-18-27A18 18 0 0 1 18-27C18-16 4-8 0 0Z")
    .toPath()

internal fun DrawScope.placePin(tip: Offset, fill: Color, ring: Color, ringWidth: Float = 3f, factor: Float = 1f, dashed: Boolean = false) {
    translate(tip.x, tip.y) {
        scale(factor, pivot = Offset.Zero) {
            if (!dashed) drawPath(PlacePinPath, fill, style = Fill)
            drawPath(
                PlacePinPath,
                if (dashed) fill else ring,
                style = Stroke(ringWidth, pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(5f, 5f)) else null),
            )
        }
    }
}

private fun DrawScope.dotted(from: Offset, to: Offset, color: Color, width: Float = 2f) =
    drawLine(color, from, to, strokeWidth = width, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 7f)))

/**
 * Personas (en sus colores) que convergen en un lugar. Coordenadas del SVG del diseño
 * (260×210), escaladas por densidad. [resolved] = false dibuja el pin vacío con "?" (sin resultados).
 */
@Composable
fun ConvergeIllustration(people: Int, description: String, resolved: Boolean = true, rings: Boolean = false) {
    val c = AppTheme.colors
    val cs = MaterialTheme.colorScheme
    val n = people.coerceIn(2, 3)
    Canvas(Modifier.size(260.dp, 210.dp).semantics { contentDescription = description }) {
        val u = size.width / 260f
        scale(u, pivot = Offset.Zero) {
            val center = Offset(130f, 150f)
            if (rings) {
                listOf(38f, 70f, 100f).forEach {
                    drawCircle(cs.outlineVariant, it, Offset(130f, 112f), style = Stroke(2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 6f))))
                }
            }
            val spots = if (n == 2) listOf(Offset(60f, 46f), Offset(200f, 46f)) else listOf(Offset(50f, 46f), Offset(130f, 40f), Offset(210f, 46f))
            spots.forEach { dotted(Offset(it.x, it.y + 32f), Offset(center.x + (it.x - center.x) * 0.15f, center.y - 8f), cs.outline) }
            spots.forEachIndexed { i, p ->
                drawCircle(cs.surfaceContainerLowest, 31f, p)
                drawCircle(c.friend(i), 28f, p)
                // Silueta de persona en on-friend
                drawCircle(c.onFriend, 6f, Offset(p.x, p.y - 6f), style = Stroke(2.5f))
                drawArc(c.onFriend, 180f, 180f, false, Offset(p.x - 12f, p.y + 2f), androidx.compose.ui.geometry.Size(24f, 24f), style = Stroke(2.5f, cap = StrokeCap.Round))
            }
            placePin(Offset(130f, 198f), cs.tertiary, cs.surfaceContainerLowest, factor = 1.15f, dashed = !resolved)
            if (resolved) drawCircle(cs.onTertiary, 6.5f * 1.15f, Offset(130f, 198f - 27f * 1.15f))
        }
    }
}

/** Señal de wifi tachada sobre un disco (pantalla "Sin conexión"). */
@Composable
fun OfflineIllustration() {
    val cs = MaterialTheme.colorScheme
    val wifi = remember {
        PathParser().parsePathString(
            "M3 3l18 18M8.5 16.5a5 5 0 0 1 7 0M5 12.9a10 10 0 0 1 4-2.4M19 12.9a10 10 0 0 0-4.3-2.5" +
                "M2 8.8a15 15 0 0 1 4-2.5M22 8.8A15 15 0 0 0 12 5M12 20h.01",
        ).toPath()
    }
    Canvas(Modifier.size(200.dp, 160.dp).semantics { contentDescription = "Señal de wifi tachada" }) {
        val u = size.width / 200f
        scale(u, pivot = Offset.Zero) {
            drawCircle(cs.outlineVariant, 74f, Offset(100f, 80f), style = Stroke(2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 7f))))
            drawCircle(cs.surfaceContainer, 52f, Offset(100f, 80f))
            translate(70f, 50f) {
                scale(2.5f, pivot = Offset.Zero) {
                    drawPath(wifi, cs.onSurface, style = Stroke(2f, cap = StrokeCap.Round))
                }
            }
        }
    }
}
