package com.juntadamigos.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.juntadamigos.core.AddressSuggestion
import com.juntadamigos.core.TravelMode

/** Pantalla 2: sumar (o editar) a una persona. */
@Composable
fun PersonScreen(
    draft: PersonDraft,
    onBack: () -> Unit,
    onName: (String) -> Unit,
    onQuery: (String) -> Unit,
    onClearQuery: () -> Unit,
    onPick: (AddressSuggestion) -> Unit,
    onMode: (TravelMode) -> Unit,
    onSave: () -> Unit,
    onRemove: () -> Unit,
) {
    val editing = draft.editingId != null
    val title = when {
        draft.isMe && !editing -> "Sumarme"
        draft.isMe -> "Tus datos"
        editing -> "Editar a ${draft.name.ifBlank { "tu amigo" }}"
        else -> "Sumar amigo"
    }
    Column(Modifier.fillMaxSize()) {
        BackBar(onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp))
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Field(if (draft.isMe) "Tu nombre (opcional)" else "Nombre") {
                    OutlinedTextField(
                        value = draft.name,
                        onValueChange = onName,
                        singleLine = true,
                        placeholder = { if (draft.isMe) Text("Vos") },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                        shape = RoundedCornerShape(12.dp),
                        colors = fieldColors(),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                val pickHint: (@Composable () -> Unit)? =
                    if (draft.resolved == null && draft.query.isNotBlank() && draft.suggestions.isEmpty()) {
                        { Text("Elegí una opción de la lista") }
                    } else {
                        null
                    }
                Field("Dirección") {
                    OutlinedTextField(
                        value = draft.query,
                        onValueChange = onQuery,
                        singleLine = true,
                        placeholder = { Text("Calle y número") },
                        leadingIcon = { Icon(AppIcons.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        trailingIcon = {
                            when {
                                draft.resolved != null -> Icon(AppIcons.Check, contentDescription = "Dirección confirmada", tint = MaterialTheme.colorScheme.primary)
                                draft.query.isNotEmpty() -> IconButton(onClick = onClearQuery) {
                                    Icon(AppIcons.Close, contentDescription = "Borrar dirección", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        },
                        supportingText = pickHint,
                        shape = RoundedCornerShape(12.dp),
                        colors = fieldColors(),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (draft.suggestions.isNotEmpty()) Suggestions(draft.suggestions, onPick)
                }
                ModePicker(draft, onMode)
            }
        }
        BottomActions {
            PrimaryButton(
                text = when {
                    editing -> "Guardar"
                    draft.isMe -> "Sumarme"
                    else -> "Sumar amigo"
                },
                onClick = onSave,
                enabled = draft.canSave,
                leading = if (editing) AppIcons.Check else AppIcons.Add,
            )
            if (editing && !draft.isMe) PlainButton("Quitar de la juntada", onRemove, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun Field(label: String, content: @Composable () -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 4.dp))
        content()
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
)

@Composable
private fun Suggestions(list: List<AddressSuggestion>, onPick: (AddressSuggestion) -> Unit) {
    Surface(
        modifier = Modifier.padding(top = 4.dp).fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 4.dp,
    ) {
        Column {
            list.forEachIndexed { i, s ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    Modifier.fillMaxWidth().clickable(role = Role.Button) { onPick(s) }.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(AppIcons.Pin, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    Column {
                        Text(s.primaryText, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        s.secondaryText?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            // Atribución obligatoria de Places cuando las sugerencias no se muestran sobre un mapa de Google.
            Text(
                "powered by Google",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().height(28.dp).padding(horizontal = 16.dp, vertical = 6.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
            )
        }
    }
}

/** Selector de modo: 4 tiles de 80 dp; el elegido en primary-container con tilde. */
@Composable
private fun ModePicker(draft: PersonDraft, onMode: (TravelMode) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val order = listOf(TravelMode.DRIVE, TravelMode.TRANSIT, TravelMode.BICYCLE, TravelMode.WALK)
    Column {
        Text(if (draft.isMe) "¿Cómo viajás?" else "¿Cómo viaja?", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 8.dp))
        Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            order.forEach { mode ->
                val selected = mode == draft.mode
                Box(Modifier.weight(1f)) {
                    Column(
                        Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(16.dp))
                            .background(if (selected) cs.primaryContainer else cs.surfaceContainerLowest)
                            .border(
                                if (selected) BorderStroke(2.dp, cs.primary) else BorderStroke(1.5.dp, cs.outline),
                                RoundedCornerShape(16.dp),
                            )
                            .selectable(selected = selected, role = Role.RadioButton, onClick = { onMode(mode) }),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
                    ) {
                        val fg = if (selected) cs.onPrimaryContainer else cs.onSurface
                        Icon(AppIcons.mode(mode), contentDescription = null, tint = fg)
                        Text(
                            modeLabel(mode),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                            color = fg,
                            maxLines = 1,
                        )
                    }
                    if (selected) {
                        Box(
                            Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-8).dp).size(20.dp).clip(CircleShape).background(cs.primary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(AppIcons.Check, contentDescription = null, tint = cs.onPrimary, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
        Text(
            modeHint(draft),
            style = MaterialTheme.typography.bodySmall,
            color = cs.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/** Explica el tope del motor para el modo elegido (Config.noCarMaxTravel). */
private fun modeHint(draft: PersonDraft): String {
    val who = when {
        draft.isMe -> "vos"
        draft.name.isNotBlank() -> draft.name.trim()
        else -> "tu amigo"
    }
    val cap = capFor(draft.mode)?.roundedMinutes()
    return when (draft.mode) {
        TravelMode.DRIVE -> "En auto no hay tope: buscamos que todos lleguen parejo."
        TravelMode.TRANSIT -> "En transporte buscamos lugares a hasta $cap min de $who."
        TravelMode.BICYCLE -> "En bici buscamos lugares a hasta $cap min de $who."
        TravelMode.WALK -> "A pie buscamos lugares a hasta $cap min de $who."
    }
}
