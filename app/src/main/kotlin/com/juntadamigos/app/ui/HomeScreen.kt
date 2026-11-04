package com.juntadamigos.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Pantallas 1a (vacía) y 1b (con personas). */
@Composable
fun HomeScreen(
    state: MeetingUiState,
    onAddMe: () -> Unit,
    onAddFriend: () -> Unit,
    onEdit: (String) -> Unit,
    onChoosePlace: () -> Unit,
) {
    if (state.people.isEmpty()) EmptyHome(state, onAddMe) else PeopleHome(state, onAddFriend, onEdit, onChoosePlace)
}

@Composable
private fun EmptyHome(state: MeetingUiState, onAddMe: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        BrandBar()
        ApiKeyWarning(state)
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            ConvergeIllustration(3, "Tres amigos que convergen en un lugar")
            Text(
                "¿Con quiénes te juntás?",
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 24.dp),
            )
            Text(
                "Sumate vos primero y después a tus amigos.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        BottomActions {
            PrimaryButton("Sumarme", onAddMe, leading = AppIcons.Add)
            Text(
                "Hasta ${MeetingUiState.MAX_PEOPLE} personas por juntada",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PeopleHome(state: MeetingUiState, onAddFriend: () -> Unit, onEdit: (String) -> Unit, onChoosePlace: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        BrandBar()
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            ApiKeyWarning(state)
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)) {
                Text("Tu juntada", style = MaterialTheme.typography.headlineLarge)
                val n = state.people.size
                Text(
                    if (n == 1) "Solo vos por ahora · sumá al menos un amigo" else "$n personas · falta elegir el lugar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.people.forEach { PersonRow(it, onEdit = { onEdit(it.id) }) }
                if (state.canAddFriend) AddFriendButton(state.people.size, onAddFriend)
            }
        }
        BottomActions {
            PrimaryButton("Elegir lugar", onChoosePlace, enabled = state.canChoosePlace, trailing = AppIcons.Next)
        }
    }
}

@Composable
private fun PersonRow(person: PersonInput, onEdit: () -> Unit) {
    AppCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.heightIn(min = 72.dp).padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AvatarWithMode(person)
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(person.displayName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        " · ${modeLabel(person.mode)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    person.resolved.address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
                Icon(AppIcons.Edit, contentDescription = "Editar a ${person.displayName}", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Botón con borde punteado "Sumar amigo · n de 6". */
@Composable
private fun AddFriendButton(count: Int, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.padding(top = 4.dp).fillMaxWidth().height(56.dp).clip(shape)
            .drawBehind {
                val w = 2.dp.toPx()
                drawRoundRect(
                    cs.outline,
                    topLeft = androidx.compose.ui.geometry.Offset(w / 2, w / 2),
                    size = androidx.compose.ui.geometry.Size(size.width - w, size.height - w),
                    cornerRadius = CornerRadius(16.dp.toPx() - w / 2),
                    style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 5.dp.toPx()))),
                )
            }
            .clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(AppIcons.Add, contentDescription = null, tint = cs.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.size(8.dp))
        Text("Sumar amigo", style = MaterialTheme.typography.labelLarge, color = cs.primary)
        Text(
            " · $count de ${MeetingUiState.MAX_PEOPLE}",
            style = MaterialTheme.typography.bodySmall,
            color = cs.onSurfaceVariant,
        )
    }
}

@Composable
private fun ApiKeyWarning(state: MeetingUiState) {
    if (!state.missingApiKey) return
    Text(
        "Falta la API key: agregá MAPS_API_KEY en local.properties y recompilá.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.errorContainer).padding(12.dp),
    )
}
