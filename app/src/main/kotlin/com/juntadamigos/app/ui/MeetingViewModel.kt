package com.juntadamigos.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.juntadamigos.core.AddressAutocomplete
import com.juntadamigos.core.AddressSuggestion
import com.juntadamigos.core.Geo
import com.juntadamigos.core.MeetingPointFinder
import com.juntadamigos.core.MeetingResult
import com.juntadamigos.core.Participant
import com.juntadamigos.core.PlaceCategory
import com.juntadamigos.core.ResolvedAddress
import com.juntadamigos.core.TravelMode
import com.juntadamigos.core.google.GoogleApiException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID

/** Una persona ya cargada en la juntada. La primera es el dueño de la app. */
data class PersonInput(
    val id: String,
    val name: String,
    val isMe: Boolean = false,
    val resolved: ResolvedAddress,
    val mode: TravelMode = TravelMode.TRANSIT,
    /** Posición fija en la paleta friend-1..6: no cambia si se quita a otro. */
    val colorIndex: Int = 0,
) {
    /** Cómo se la nombra en pantalla. */
    val displayName: String get() = name.ifBlank { if (isMe) "Vos" else "Sin nombre" }

    /** Cómo se la nombra en el mensaje que se comparte con los demás. */
    val shareName: String get() = name.ifBlank { if (isMe) "Yo" else "Sin nombre" }

    val initial: String get() = displayName.trim().take(1).uppercase()
}

/** Formulario de "Sumar amigo" (o "Sumarme"), también para editar a alguien. */
data class PersonDraft(
    /** null = persona nueva. */
    val editingId: String?,
    val isMe: Boolean,
    val name: String = "",
    val query: String = "",
    val resolved: ResolvedAddress? = null,
    val suggestions: List<AddressSuggestion> = emptyList(),
    val mode: TravelMode = TravelMode.TRANSIT,
    val colorIndex: Int = 0,
) {
    val canSave: Boolean get() = resolved != null && (isMe || name.isNotBlank())
}

enum class Screen { HOME, PERSON, PLACE_TYPE, LOADING, RESULTS, NO_RESULTS, OFFLINE }

data class MeetingUiState(
    val people: List<PersonInput> = emptyList(),
    val draft: PersonDraft? = null,
    val category: PlaceCategory = PlaceCategory.CAFE,
    val screen: Screen = Screen.HOME,
    val result: MeetingResult? = null,
    val error: String? = null,
    val missingApiKey: Boolean = false,
) {
    val me: PersonInput? get() = people.firstOrNull { it.isMe }
    val canAddFriend: Boolean get() = people.size < MAX_PEOPLE
    val canChoosePlace: Boolean get() = people.size >= 2

    companion object {
        /** Tope del diseño ("Hasta 6 personas por juntada"): 6 colores de persona distintos. */
        const val MAX_PEOPLE = 6
    }
}

class MeetingViewModel(
    private val autocomplete: AddressAutocomplete,
    private val finder: MeetingPointFinder,
    hasApiKey: Boolean,
) : ViewModel() {

    private val _state = MutableStateFlow(MeetingUiState(missingApiKey = !hasApiKey))
    val state: StateFlow<MeetingUiState> = _state.asStateFlow()

    /** Token de sesión del autocompletado del formulario; se renueva al elegir una dirección. */
    private var sessionToken: String? = null
    private var suggestJob: Job? = null
    private var searchJob: Job? = null
    private var nextId = 1

    private fun token() = sessionToken ?: UUID.randomUUID().toString().also { sessionToken = it }

    // --- Formulario de persona ---

    fun startAddMe() = openDraft(PersonDraft(editingId = null, isMe = true, mode = TravelMode.DRIVE, colorIndex = 0))

    fun startAddFriend() {
        val s = _state.value
        if (!s.canAddFriend) return
        val used = s.people.map { it.colorIndex }.toSet()
        val color = (0 until MeetingUiState.MAX_PEOPLE).first { it !in used }
        openDraft(PersonDraft(editingId = null, isMe = false, colorIndex = color))
    }

    fun startEdit(id: String) {
        val p = _state.value.people.firstOrNull { it.id == id } ?: return
        openDraft(
            PersonDraft(
                editingId = p.id, isMe = p.isMe, name = p.name, query = p.resolved.address,
                resolved = p.resolved, mode = p.mode, colorIndex = p.colorIndex,
            ),
        )
    }

    private fun openDraft(draft: PersonDraft) {
        resetAutocomplete()
        _state.update { it.copy(draft = draft, screen = Screen.PERSON) }
    }

    fun closeDraft() {
        resetAutocomplete()
        _state.update { it.copy(draft = null, screen = Screen.HOME) }
    }

    private fun resetAutocomplete() {
        suggestJob?.cancel()
        sessionToken = null
    }

    private fun updateDraft(f: (PersonDraft) -> PersonDraft) =
        _state.update { s -> s.copy(draft = s.draft?.let(f)) }

    fun onDraftName(name: String) = updateDraft { it.copy(name = name) }

    fun onDraftMode(mode: TravelMode) = updateDraft { it.copy(mode = mode) }

    fun onDraftQuery(query: String) {
        updateDraft { it.copy(query = query, resolved = null) }
        suggestJob?.cancel()
        if (query.trim().length < 3) {
            updateDraft { it.copy(suggestions = emptyList()) }
            return
        }
        suggestJob = viewModelScope.launch {
            delay(350) // debounce: no pagar una request por tecla
            try {
                val list = autocomplete.suggest(query, token(), bias = groupCenter())
                updateDraft { if (it.query == query) it.copy(suggestions = list) else it }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = describe(e)) }
            }
        }
    }

    fun onDraftSuggestion(suggestion: AddressSuggestion) {
        suggestJob?.cancel()
        val label = listOfNotNull(suggestion.primaryText, suggestion.secondaryText).joinToString(", ")
        updateDraft { it.copy(query = label, suggestions = emptyList()) }
        viewModelScope.launch {
            try {
                val resolved = autocomplete.resolve(suggestion.placeId, token())
                sessionToken = null // la sesión termina con el Place Details
                if (resolved == null) {
                    _state.update { it.copy(error = "Google no devolvió coordenadas para esa dirección.") }
                } else {
                    updateDraft { if (it.query == label) it.copy(resolved = resolved) else it }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = describe(e)) }
            }
        }
    }

    fun clearDraftQuery() = onDraftQuery("")

    fun saveDraft() {
        val d = _state.value.draft ?: return
        val resolved = d.resolved
        if (resolved == null) {
            _state.update { it.copy(error = "Elegí la dirección de la lista.") }
            return
        }
        if (!d.canSave) return
        val person = PersonInput(
            id = d.editingId ?: if (d.isMe) "me" else "f${nextId++}",
            name = d.name.trim(),
            isMe = d.isMe,
            resolved = resolved,
            mode = d.mode,
            colorIndex = d.colorIndex,
        )
        resetAutocomplete()
        _state.update { s ->
            val people = if (d.editingId == null) {
                if (person.isMe) listOf(person) + s.people else s.people + person
            } else {
                s.people.map { if (it.id == d.editingId) person else it }
            }
            s.copy(people = people, draft = null, screen = Screen.HOME, result = null)
        }
    }

    fun removeEditing() {
        val id = _state.value.draft?.editingId ?: return
        resetAutocomplete()
        _state.update { s ->
            s.copy(people = s.people.filterNot { it.id == id && !it.isMe }, draft = null, screen = Screen.HOME, result = null)
        }
    }

    // --- Tipo de lugar y búsqueda ---

    fun choosePlace() = _state.update { if (it.canChoosePlace) it.copy(screen = Screen.PLACE_TYPE) else it }

    fun onCategoryChange(category: PlaceCategory) =
        _state.update { it.copy(category = category, result = if (category == it.category) it.result else null) }

    fun goHome() = _state.update { it.copy(screen = Screen.HOME) }

    fun backToPlaceType() {
        searchJob?.cancel()
        _state.update { it.copy(screen = Screen.PLACE_TYPE) }
    }

    fun dismissError() = _state.update { it.copy(error = null) }

    fun search() {
        val s = _state.value
        if (!s.canChoosePlace) return
        // Si ya hay resultado para esta juntada y este tipo, no se vuelve a pagar la búsqueda.
        if (s.result != null) {
            _state.update { it.copy(screen = screenFor(s.result)) }
            return
        }
        val participants = s.people.map { p -> Participant(p.id, p.shareName, p.resolved.location, p.mode) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _state.update { it.copy(screen = Screen.LOADING, error = null, result = null) }
            try {
                val result = finder.find(participants = participants, category = s.category)
                _state.update { it.copy(result = result, screen = screenFor(result)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _state.update { it.copy(screen = Screen.OFFLINE) }
            } catch (e: Exception) {
                _state.update { it.copy(screen = Screen.PLACE_TYPE, error = describe(e)) }
            }
        }
    }

    private fun screenFor(result: MeetingResult) =
        if (result.suggestions.isEmpty()) Screen.NO_RESULTS else Screen.RESULTS

    /** Sesgo del autocompletado: el centro de las direcciones ya cargadas. */
    private fun groupCenter() = _state.value.people.map { it.resolved.location }
        .takeIf { it.isNotEmpty() }
        ?.let(Geo::centroid)

    private fun describe(e: Exception): String = when (e) {
        is GoogleApiException -> when (e.httpCode) {
            400 -> "Google rechazó la búsqueda (400). Probá con otras direcciones."
            403 -> "Google rechazó la key (403): revisá que Places API (New) y Routes API estén habilitadas y que la restricción de la key incluya esta app."
            429 -> "Se superó la cuota de Google. Probá en un rato."
            else -> "Error de Google (${e.httpCode})."
        }
        is IOException -> "Sin conexión o Google no respondió."
        else -> e.message ?: "Error inesperado."
    }

    companion object {
        fun factory(
            autocomplete: AddressAutocomplete,
            finder: MeetingPointFinder,
            hasApiKey: Boolean,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { MeetingViewModel(autocomplete, finder, hasApiKey) }
        }
    }
}
