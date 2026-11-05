package com.juntadamigos.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.juntadamigos.app.ui.theme.AppTheme
import com.juntadamigos.app.ui.theme.Display
import com.juntadamigos.app.ui.theme.Minutes
import com.juntadamigos.core.MeetingShare
import com.juntadamigos.core.TravelMode
import kotlin.time.Duration

/** Barra superior de 64 dp con "Volver" (o el ícono que se pase). */
@Composable
fun BackBar(onBack: () -> Unit, icon: ImageVector = AppIcons.Back, label: String = "Volver") {
    Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

/** Marca: dos círculos superpuestos (friend-1 y friend-2) + "Juntadamigos". */
@Composable
fun BrandBar() {
    val c = AppTheme.colors
    Row(
        Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.width(36.dp).height(24.dp)) {
            Box(Modifier.size(24.dp).clip(CircleShape).background(c.friend(0)))
            Box(Modifier.offset(x = 12.dp).size(24.dp).clip(CircleShape).background(c.friend(1).copy(alpha = 0.9f)))
        }
        Text("Juntadamigos", fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 20.sp, letterSpacing = (-0.2).sp)
    }
}

/** Botón principal de pantalla: 56 dp, píldora, primary. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leading: ImageVector? = null,
    trailing: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = CircleShape,
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) { ButtonContent(text, leading, trailing) }
}

/** Botón tonal (primary-container): acciones secundarias de peso. */
@Composable
fun TonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: ImageVector? = null,
    height: Dp = 56.dp,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(height),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        contentPadding = PaddingValues(horizontal = 12.dp),
    ) { ButtonContent(text, leading, null) }
}

@Composable
fun PlainButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    TextButton(onClick = onClick, modifier = modifier.height(48.dp), shape = CircleShape) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

@Composable
private fun RowScope.ButtonContent(text: String, leading: ImageVector?, trailing: ImageVector?) {
    leading?.let { Icon(it, contentDescription = null, modifier = Modifier.size(20.dp)) }
    if (leading != null) Box(Modifier.width(8.dp))
    Text(text, style = MaterialTheme.typography.labelLarge)
    if (trailing != null) Box(Modifier.width(8.dp))
    trailing?.let { Icon(it, contentDescription = null, modifier = Modifier.size(20.dp)) }
}

/** Zona inferior fija con los botones de la pantalla. */
@Composable
fun BottomActions(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

/** Disco de persona con la inicial, en su color friend-N. */
@Composable
fun Avatar(person: PersonInput, size: Dp = 40.dp, ring: Dp = 0.dp) {
    val c = AppTheme.colors
    Box(
        Modifier.size(size + ring * 2).clip(CircleShape)
            .background(if (ring > 0.dp) MaterialTheme.colorScheme.surfaceContainerLowest else Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(size).clip(CircleShape).background(c.friend(person.colorIndex)), contentAlignment = Alignment.Center) {
            Text(
                person.initial,
                color = c.onFriend,
                fontFamily = Display,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.45f).sp,
            )
        }
    }
}

/** Avatar con la insignia del modo de viaje abajo a la derecha. */
@Composable
fun AvatarWithMode(person: PersonInput, size: Dp = 40.dp) {
    val badge = if (size >= 40.dp) 20.dp else 16.dp
    Box(Modifier.size(size)) {
        Avatar(person, size)
        Box(
            Modifier.align(Alignment.BottomEnd).offset(x = 5.dp, y = 5.dp).size(badge).clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(AppIcons.mode(person.mode), contentDescription = null, modifier = Modifier.size(badge * 0.6f), tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

/** Tarjeta blanca con elevación 1 (surface-container-lowest). */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 1.dp,
        border = border,
    ) { Column(content = content) }
}

/** Píldora de estado (Parejo / Medio parejo / Desparejo) con ícono: nunca solo color. */
@Composable
fun EvennessPill(evenness: Evenness, text: String, compact: Boolean = false) {
    val (bg, fg, icon) = evennessStyle(evenness)
    Row(
        Modifier.height(if (compact) 28.dp else 32.dp).clip(CircleShape).background(bg)
            .padding(start = if (compact) 6.dp else 8.dp, end = if (compact) 10.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(if (compact) 14.dp else 16.dp))
        Text(text, color = fg, style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun evennessStyle(e: Evenness): Triple<Color, Color, ImageVector> {
    val cs = MaterialTheme.colorScheme
    val c = AppTheme.colors
    return when (e) {
        Evenness.EVEN -> Triple(cs.primaryContainer, cs.onPrimaryContainer, AppIcons.Check)
        Evenness.MEDIUM -> Triple(c.warningContainer, c.onWarningContainer, AppIcons.Info)
        Evenness.UNEVEN -> Triple(cs.errorContainer, cs.onErrorContainer, AppIcons.Warning)
    }
}

fun evennessLabel(e: Evenness) = when (e) {
    Evenness.EVEN -> "Parejo"
    Evenness.MEDIUM -> "Medio parejo"
    Evenness.UNEVEN -> "Desparejo"
}

/**
 * Barra de tiempo de una persona: largo = minutos sobre [scaleMinutes], marca del tope si va
 * sin auto, y rayas de error si lo supera.
 */
@Composable
fun TimeBar(person: PersonInput, time: Duration, scaleMinutes: Long) {
    val cs = MaterialTheme.colorScheme
    val minutes = time.roundedMinutes()
    val cap = capFor(person.mode)?.roundedMinutes()
    val over = cap != null && minutes > cap
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        AvatarWithMode(person, 28.dp)
        Column(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth().height(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    person.displayName,
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(" · ${modeLabel(person.mode)}", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                Box(Modifier.weight(1f))
                if (over) Icon(AppIcons.Warning, contentDescription = null, tint = cs.error, modifier = Modifier.size(16.dp).padding(end = 2.dp))
                Text(MeetingShare.formatDuration(time), style = Minutes.Md)
            }
            BoxWithConstraints(Modifier.padding(top = 4.dp).fillMaxWidth().height(14.dp), contentAlignment = Alignment.CenterStart) {
                val w = maxWidth
                Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(8.dp)).background(cs.surfaceContainerHighest))
                val fraction = (minutes.toFloat() / scaleMinutes).coerceIn(0.02f, 1f)
                val fill = Modifier.width(w * fraction).height(10.dp).clip(RoundedCornerShape(8.dp))
                if (over) {
                    Box(
                        fill.background(
                            Brush.linearGradient(
                                0f to cs.error, 0.5f to cs.error, 0.5f to cs.errorContainer, 1f to cs.errorContainer,
                                start = Offset.Zero, end = Offset(8f, 8f), tileMode = TileMode.Repeated,
                            ),
                        ).border(1.5.dp, cs.error, RoundedCornerShape(8.dp)),
                    )
                } else {
                    Box(fill.background(cs.primary))
                }
                if (cap != null) {
                    val x = w * (cap.toFloat() / scaleMinutes).coerceAtMost(1f)
                    Box(
                        Modifier.offset(x = x - 1.dp).width(2.dp).fillMaxHeight().clip(RoundedCornerShape(4.dp))
                            .background(if (over) cs.onSurface else cs.onSurfaceVariant),
                    )
                }
            }
            if (over) {
                Text(
                    "${minutes - cap!!} min más que su tope de $cap",
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

/** Chip compacto "V 12 min" de las tarjetas no seleccionadas. */
@Composable
fun TimeChip(person: PersonInput, time: Duration) {
    val c = AppTheme.colors
    Row(
        Modifier.height(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(start = 4.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(Modifier.size(20.dp).clip(CircleShape).background(c.friend(person.colorIndex)), contentAlignment = Alignment.Center) {
            Text(person.initial, color = c.onFriend, fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
        Text("${time.roundedMinutes()} min", fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

/** Rating con estrella; la estrella nunca va sola. */
@Composable
fun Rating(value: Double, extra: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(AppIcons.Star, contentDescription = null, tint = AppTheme.colors.rating, modifier = Modifier.size(14.dp))
        Text(
            "%.1f".format(value).replace('.', ','),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        extra?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

/** Insignia de ranking de lugar: terracota, nunca colores de persona. */
@Composable
fun RankBadge(rank: Int, size: Dp, strong: Boolean) {
    val cs = MaterialTheme.colorScheme
    Box(
        Modifier.size(size).clip(CircleShape).background(if (strong) cs.tertiary else cs.tertiaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "$rank",
            color = if (strong) cs.onTertiary else cs.onTertiaryContainer,
            fontFamily = Display,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.5f).sp,
        )
    }
}

fun Modifier.elevated(level: Int, shape: androidx.compose.ui.graphics.Shape): Modifier =
    shadow(if (level >= 2) 4.dp else 1.dp, shape, clip = false)

internal fun modeLabel(m: TravelMode) = when (m) {
    TravelMode.DRIVE -> "Auto"
    TravelMode.TRANSIT -> "Transporte"
    TravelMode.WALK -> "A pie"
    TravelMode.BICYCLE -> "Bici"
}
