@file:OptIn(ExperimentalMaterial3Api::class)

package com.juntadamigos.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.juntadamigos.core.PlaceCategory
import com.juntadamigos.core.TravelMode

@Composable
fun MeetingScreen(viewModel: MeetingViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val result = state.result
    if (state.showingResults && result != null) {
        ResultsScreen(
            result = result,
            people = state.people,
            onBack = viewModel::closeResults,
        )
        return
    }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbar.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("¿Dónde nos juntamos?") }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.missingApiKey) {
                item {
                    Text(
                        "Falta la API key: agregá MAPS_API_KEY en local.properties y recompilá.",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            items(state.people, key = { it.id }) { person ->
                PersonCard(
                    person = person,
                    canRemove = !person.isMe && state.people.size > 2,
                    onName = { viewModel.onNameChange(person.id, it) },
                    onQuery = { viewModel.onQueryChange(person.id, it) },
                    onPick = { viewModel.onSuggestionPicked(person.id, it) },
                    onMode = { viewModel.onModeChange(person.id, it) },
                    onRemove = { viewModel.removeFriend(person.id) },
                )
            }
            item {
                OutlinedButton(onClick = viewModel::addFriend) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Agregar amigo")
                }
            }
            item { OptionsSection(state, viewModel) }
            item {
                Button(
                    onClick = viewModel::search,
                    enabled = state.canSearch,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.searching) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (state.searching) "Buscando…" else "Buscar lugar")
                }
            }
            if (state.result != null && !state.showingResults) {
                item {
                    OutlinedButton(onClick = viewModel::reopenResults, modifier = Modifier.fillMaxWidth()) {
                        Text("Ver resultados")
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonCard(
    person: PersonInput,
    canRemove: Boolean,
    onName: (String) -> Unit,
    onQuery: (String) -> Unit,
    onPick: (com.juntadamigos.core.AddressSuggestion) -> Unit,
    onMode: (TravelMode) -> Unit,
    onRemove: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = person.name,
                    onValueChange = onName,
                    label = { Text(if (person.isMe) "Tu nombre" else "Nombre") },
                    placeholder = { if (person.isMe) Text("Yo") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f),
                )
                if (canRemove) {
                    IconButton(onClick = onRemove) { Icon(Icons.Default.Close, contentDescription = "Quitar") }
                }
            }
            OutlinedTextField(
                value = person.query,
                onValueChange = onQuery,
                label = { Text("Dirección") },
                singleLine = true,
                trailingIcon = {
                    if (person.resolved != null) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Dirección confirmada", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                supportingText = {
                    when {
                        person.resolved != null -> Text(person.resolved.address)
                        person.query.isNotBlank() -> Text("Elegí una opción de la lista")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            if (person.suggestions.isNotEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    person.suggestions.forEachIndexed { i, s ->
                        if (i > 0) HorizontalDivider()
                        Column(
                            Modifier.fillMaxWidth().clickable { onPick(s) }.padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Text(s.primaryText, style = MaterialTheme.typography.bodyLarge)
                            s.secondaryText?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
            }
            ChipRow(TravelMode.entries, selected = person.mode, label = ::modeLabel, onSelect = onMode)
        }
    }
}

@Composable
private fun OptionsSection(state: MeetingUiState, viewModel: MeetingViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("¿Qué tipo de lugar?", style = MaterialTheme.typography.titleMedium)
        ChipRow(PlaceCategory.entries, selected = state.category, label = ::categoryLabel, onSelect = viewModel::onCategoryChange)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Tope por persona", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (state.capEnabled) "Nadie viaja más de ${state.capMinutes} min" else "Sin tope: gana el más parejo (sin auto: máx. 20 min, a pie 35)",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(checked = state.capEnabled, onCheckedChange = viewModel::onCapEnabledChange)
        }
        if (state.capEnabled) {
            Slider(
                value = state.capMinutes.toFloat(),
                onValueChange = { viewModel.onCapMinutesChange(it.toInt()) },
                valueRange = 15f..120f,
                steps = (120 - 15) / 5 - 1,
            )
        }
    }
}

@Composable
private fun <T> ChipRow(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(selected = option == selected, onClick = { onSelect(option) }, label = { Text(label(option)) })
        }
    }
}

internal fun modeLabel(m: TravelMode) = when (m) {
    TravelMode.DRIVE -> "Auto"
    TravelMode.TRANSIT -> "Transporte"
    TravelMode.WALK -> "A pie"
    TravelMode.BICYCLE -> "Bici"
}

private fun categoryLabel(c: PlaceCategory) = when (c) {
    PlaceCategory.CAFE -> "Café"
    PlaceCategory.BAR -> "Bar"
    PlaceCategory.RESTAURANT -> "Restaurante"
    PlaceCategory.PARK -> "Parque"
    PlaceCategory.SHOPPING_MALL -> "Shopping"
}
