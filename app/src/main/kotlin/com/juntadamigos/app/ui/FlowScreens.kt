package com.juntadamigos.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.juntadamigos.core.PlaceCategory

// --- 3 · Tipo de lugar ---

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlaceTypeScreen(state: MeetingUiState, onBack: () -> Unit, onCategory: (PlaceCategory) -> Unit, onSearch: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize()) {
        BackBar(onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                Text("¿Qué plan armamos?", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Buscamos lugares que queden parejos para ${groupLabel(state.people.size)}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            FlowRow(
                Modifier.padding(start = 16.dp, end = 16.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.people.forEach { p ->
                    Row(
                        Modifier.height(40.dp).clip(CircleShape).background(cs.surfaceContainer).padding(start = 6.dp, end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Avatar(p, 28.dp)
                        Text(p.displayName, style = MaterialTheme.typography.labelLarge)
                        Icon(AppIcons.mode(p.mode), contentDescription = modeLabel(p.mode), modifier = Modifier.size(16.dp), tint = cs.onSurfaceVariant)
                    }
                }
            }
            Text("Tipo de lugar", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp))
            Column(Modifier.padding(horizontal = 16.dp).selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PlaceCategory.entries.forEach { c -> CategoryOption(c, selected = c == state.category) { onCategory(c) } }
            }
        }
        BottomActions { PrimaryButton("Buscar lugar", onSearch, leading = AppIcons.Search) }
    }
}

@Composable
private fun CategoryOption(c: PlaceCategory, selected: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().height(64.dp).clip(shape)
            .background(if (selected) cs.primaryContainer else cs.surfaceContainerLowest)
            .border(if (selected) BorderStroke(2.dp, cs.primary) else BorderStroke(1.5.dp, cs.outline), shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(if (selected) cs.surfaceContainerLowest else cs.surfaceContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(AppIcons.category(c), contentDescription = null, tint = if (selected) cs.primary else cs.onSurface)
        }
        Column(Modifier.weight(1f)) {
            Text(
                categoryLabel(c),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (selected) cs.onPrimaryContainer else cs.onSurface,
            )
            Text(
                categoryHint(c),
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) cs.onPrimaryContainer else cs.onSurfaceVariant,
            )
        }
        if (selected) {
            Box(Modifier.size(24.dp).clip(CircleShape).background(cs.primary), contentAlignment = Alignment.Center) {
                Icon(AppIcons.Check, contentDescription = null, tint = cs.onPrimary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// --- 4 · Cargando ---

/**
 * El motor no informa avance por persona, así que en lugar de "Listo / Calculando" por fila
 * (como en el diseño) se muestra la lista y una barra indeterminada.
 */
@Composable
fun LoadingScreen(state: MeetingUiState, onCancel: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize()) {
        BackBar(onCancel, icon = AppIcons.Close, label = "Cancelar búsqueda")
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            ConvergeIllustration(state.people.size, "Amigos y un lugar en el centro, calculando recorridos", rings = true)
            Text("Calculando tiempos reales…", style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 16.dp))
            Text(
                "Miramos cómo llega cada uno, con el tránsito de ahora.",
                style = MaterialTheme.typography.bodyLarge,
                color = cs.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        AppCard(Modifier.padding(horizontal = 16.dp).fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }) {
            state.people.forEachIndexed { i, p ->
                if (i > 0) HorizontalDivider(color = cs.outlineVariant)
                Row(
                    Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Avatar(p, 28.dp)
                    Text("${p.displayName} · ${modeLabel(p.mode)}", style = MaterialTheme.typography.labelLarge)
                }
            }
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = cs.primary,
                trackColor = cs.surfaceContainerHighest,
                gapSize = 0.dp,
            )
        }
        BottomActions { PlainButton("Cancelar", onCancel) }
    }
}

// --- 6a · Sin resultados ---

@Composable
fun NoResultsScreen(state: MeetingUiState, onBack: () -> Unit, onOtherType: () -> Unit, onChangeAddresses: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val r = state.result
    Column(Modifier.fillMaxSize()) {
        BackBar(onBack)
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            ConvergeIllustration(state.people.size, "Amigos lejos entre sí, sin un lugar en el medio", resolved = false)
            Text("No encontramos un lugar", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 16.dp))
            Text(
                noResultsReason(state),
                style = MaterialTheme.typography.bodyLarge,
                color = cs.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )
            if (r != null && r.candidatesEvaluated > 0) {
                Text(
                    "Se evaluaron ${r.candidatesEvaluated} lugares",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        BottomActions {
            PrimaryButton("Probar otro tipo de lugar", onOtherType, leading = AppIcons.category(state.category))
            PlainButton("Cambiar direcciones", onChangeAddresses)
        }
    }
}

private fun noResultsReason(state: MeetingUiState): String {
    val plural = categoryPlural(state.category).lowercase()
    val r = state.result
    return if (r != null && r.discardedUnreachable > 0) {
        "Hay $plural cerca, pero alguien no tiene cómo llegar (por ejemplo, sin transporte a esa hora)."
    } else {
        "Google no encontró $plural entre ${groupLabel(state.people.size)}."
    }
}

// --- 6b · Sin conexión ---

@Composable
fun OfflineScreen(state: MeetingUiState, onBack: () -> Unit, onRetry: () -> Unit, onSeeMeeting: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize()) {
        BackBar(onBack)
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            OfflineIllustration()
            Text("Sin conexión", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 16.dp))
            Text(
                "Tus amigos siguen cargados: no perdés nada.",
                style = MaterialTheme.typography.bodyLarge,
                color = cs.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )
            AppCard(Modifier.padding(top = 24.dp).fillMaxWidth()) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box {
                        state.people.take(4).forEachIndexed { i, p ->
                            Box(Modifier.offset(x = (i * 22).dp)) { Avatar(p, 32.dp, ring = 2.dp) }
                        }
                        Box(Modifier.size(width = (36 + 22 * (state.people.take(4).size - 1)).dp, height = 36.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Tu juntada", style = MaterialTheme.typography.titleMedium)
                        Text(
                            state.people.joinToString(", ") { it.displayName } + " · " + categoryLabel(state.category),
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant,
                            maxLines = 2,
                        )
                    }
                }
            }
        }
        BottomActions {
            PrimaryButton("Reintentar", onRetry, leading = AppIcons.Refresh)
            PlainButton("Ver mi juntada", onSeeMeeting)
        }
    }
}

// --- Textos ---

/** "los 3", "los 2". */
internal fun groupLabel(n: Int) = "los $n"

internal fun categoryLabel(c: PlaceCategory) = when (c) {
    PlaceCategory.CAFE -> "Café"
    PlaceCategory.BAR -> "Bar"
    PlaceCategory.RESTAURANT -> "Restaurante"
    PlaceCategory.PARK -> "Parque"
    PlaceCategory.SHOPPING_MALL -> "Shopping"
}

internal fun categoryPlural(c: PlaceCategory) = when (c) {
    PlaceCategory.CAFE -> "Cafés"
    PlaceCategory.BAR -> "Bares"
    PlaceCategory.RESTAURANT -> "Restaurantes"
    PlaceCategory.PARK -> "Parques"
    PlaceCategory.SHOPPING_MALL -> "Shoppings"
}

private fun categoryHint(c: PlaceCategory) = when (c) {
    PlaceCategory.CAFE -> "Para charlar"
    PlaceCategory.BAR -> "Para tomar algo"
    PlaceCategory.RESTAURANT -> "Para comer juntos"
    PlaceCategory.PARK -> "Al aire libre"
    PlaceCategory.SHOPPING_MALL -> "Para dar vueltas"
}
