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
import kotlin.time.Duration.Companion.minutes

/** Una persona en el formulario. La primera es el dueño de la app. */
data class PersonInput(
    val id: String,
    val name: String,
    val isMe: Boolean = false,
    val query: String = "",
    val resolved: ResolvedAddress? = null,
    val suggestions: List<AddressSuggestion> = emptyList(),
    val mode: TravelMode = TravelMode.TRANSIT,
) {
    val displayName: String get() = name.ifBlank { if (isMe) "Yo" else "Sin nombre" }
}

data class MeetingUiState(
    val people: List<PersonInput>,
    val category: PlaceCategory = PlaceCategory.CAFE,
    val capEnabled: Boolean = false,
    val capMinutes: Int = 45,
    val searching: Boolean = false,
    val result: MeetingResult? = null,
    val error: String? = null,
    val missingApiKey: Boolean = false,
) {
    val canSearch: Boolean get() = !searching && people.size >= 2 && people.all { it.resolved != null }
}

class MeetingViewModel(
    private val autocomplete: AddressAutocomplete,
    private val finder: MeetingPointFinder,
    hasApiKey: Boolean,
) : ViewModel() {

    private var nextFriend = 1
    private val _state = MutableStateFlow(
        MeetingUiState(
            people = listOf(PersonInput(id = "me", name = "", isMe = true), newFriend()),
            missingApiKey = !hasApiKey,
        ),
    )
    val state: StateFlow<MeetingUiState> = _state.asStateFlow()

    /** Un token de sesión de autocompletado por persona; se renueva al elegir una dirección. */
    private val sessionTokens = mutableMapOf<String, String>()
    private val suggestJobs = mutableMapOf<String, Job>()
    private var searchJob: Job? = null

    private fun newFriend() = PersonInput(id = "f${nextFriend}", name = "Amigo ${nextFriend++}")

    private fun token(id: String) = sessionTokens.getOrPut(id) { UUID.randomUUID().toString() }

    private fun updatePerson(id: String, f: (PersonInput) -> PersonInput) =
        _state.update { s -> s.copy(people = s.people.map { if (it.id == id) f(it) else it }, result = null) }

    fun addFriend() = _state.update { it.copy(people = it.people + newFriend(), result = null) }

    fun removeFriend(id: String) {
        suggestJobs.remove(id)?.cancel()
        sessionTokens.remove(id)
        _state.update { s -> s.copy(people = s.people.filterNot { it.id == id && !it.isMe }, result = null) }
    }

    fun onNameChange(id: String, name: String) = updatePerson(id) { it.copy(name = name) }

    fun onModeChange(id: String, mode: TravelMode) = updatePerson(id) { it.copy(mode = mode) }

    fun onQueryChange(id: String, query: String) {
        updatePerson(id) { it.copy(query = query, resolved = null) }
        suggestJobs.remove(id)?.cancel()
        if (query.trim().length < 3) {
            updatePerson(id) { it.copy(suggestions = emptyList()) }
            return
        }
        suggestJobs[id] = viewModelScope.launch {
            delay(350) // debounce: no pagar una request por tecla
            try {
                val list = autocomplete.suggest(query, token(id), bias = groupCenter())
                updatePerson(id) { if (it.query == query) it.copy(suggestions = list) else it }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = describe(e)) }
            }
        }
    }

    fun onSuggestionPicked(id: String, suggestion: AddressSuggestion) {
        suggestJobs.remove(id)?.cancel()
        updatePerson(id) { it.copy(query = suggestion.primaryText, suggestions = emptyList()) }
        viewModelScope.launch {
            try {
                val resolved = autocomplete.resolve(suggestion.placeId, token(id))
                sessionTokens.remove(id) // la sesión termina con el Place Details
                if (resolved == null) {
                    _state.update { it.copy(error = "Google no devolvió coordenadas para esa dirección.") }
                } else {
                    updatePerson(id) { it.copy(resolved = resolved) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = describe(e)) }
            }
        }
    }

    fun onCategoryChange(category: PlaceCategory) = _state.update { it.copy(category = category, result = null) }

    fun onCapEnabledChange(enabled: Boolean) = _state.update { it.copy(capEnabled = enabled, result = null) }

    fun onCapMinutesChange(minutes: Int) = _state.update { it.copy(capMinutes = minutes, result = null) }

    fun dismissError() = _state.update { it.copy(error = null) }

    fun search() {
        val s = _state.value
        val missing = s.people.firstOrNull { it.resolved == null }
        if (missing != null) {
            _state.update { it.copy(error = "Falta elegir la dirección de ${missing.displayName} de la lista.") }
            return
        }
        if (s.people.size < 2) return
        val participants = s.people.map { p ->
            Participant(p.id, p.displayName, p.resolved!!.location, p.mode)
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _state.update { it.copy(searching = true, error = null, result = null) }
            try {
                val result = finder.find(
                    participants = participants,
                    category = s.category,
                    maxIndividualTravel = if (s.capEnabled) s.capMinutes.minutes else null,
                )
                _state.update { it.copy(searching = false, result = result) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(searching = false, error = describe(e)) }
            }
        }
    }

    /** Sesgo del autocompletado: el centro de las direcciones ya cargadas. */
    private fun groupCenter() = _state.value.people.mapNotNull { it.resolved?.location }
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
