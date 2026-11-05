package com.juntadamigos.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Raíz de la app: elige la pantalla según el estado del ViewModel y maneja "atrás". */
@Composable
fun MeetingScreen(viewModel: MeetingViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbar.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    // En Inicio, "atrás" sale de la app (comportamiento del sistema).
    BackHandler(enabled = state.screen != Screen.HOME) {
        when (state.screen) {
            Screen.PERSON -> viewModel.closeDraft()
            Screen.PLACE_TYPE -> viewModel.goHome()
            else -> viewModel.backToPlaceType()
        }
    }

    val result = state.result
    // Surface (no un Box con fondo): fija LocalContentColor = onSurface. Sin esto, todo Text e
    // Icon sin color explícito sale negro, invisible en modo oscuro.
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Box(Modifier.fillMaxSize()) {
            if (state.screen == Screen.RESULTS && result != null) {
                // El mapa va de borde a borde: la pantalla maneja sus propios insets.
                ResultsScreen(
                    result, state.people, state.category,
                    zoneLabel = state.zoneLabel?.takeIf { state.inZone },
                    onBack = viewModel::backToPlaceType,
                )
            } else {
                Box(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
                    when (state.screen) {
                        Screen.HOME, Screen.RESULTS -> HomeScreen(
                            state = state,
                            onAddMe = viewModel::startAddMe,
                            onAddFriend = viewModel::startAddFriend,
                            onEdit = viewModel::startEdit,
                            onChoosePlace = viewModel::choosePlace,
                        )
                        Screen.PERSON -> state.draft?.let { draft ->
                            PersonScreen(
                                draft = draft,
                                onBack = viewModel::closeDraft,
                                onName = viewModel::onDraftName,
                                onQuery = viewModel::onDraftQuery,
                                onClearQuery = viewModel::clearDraftQuery,
                                onPick = viewModel::onDraftSuggestion,
                                onMode = viewModel::onDraftMode,
                                onSave = viewModel::saveDraft,
                                onRemove = viewModel::removeEditing,
                            )
                        }
                        Screen.PLACE_TYPE -> PlaceTypeScreen(
                            state = state,
                            onBack = viewModel::goHome,
                            onCategory = viewModel::onCategoryChange,
                            onZoneEnabled = viewModel::onZoneEnabled,
                            onZoneQuery = viewModel::onZoneQuery,
                            onZonePick = viewModel::onZonePick,
                            onZoneClear = viewModel::clearZone,
                            onSearch = viewModel::search,
                        )
                        Screen.LOADING -> LoadingScreen(state, onCancel = viewModel::backToPlaceType)
                        Screen.NO_RESULTS -> NoResultsScreen(
                            state = state,
                            onBack = viewModel::backToPlaceType,
                            onOtherType = viewModel::backToPlaceType,
                            onChangeAddresses = viewModel::goHome,
                        )
                        Screen.OFFLINE -> OfflineScreen(
                            state = state,
                            onBack = viewModel::backToPlaceType,
                            onRetry = viewModel::search,
                            onSeeMeeting = viewModel::goHome,
                        )
                    }
                }
            }
            SnackbarHost(
                snackbar,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().imePadding().padding(bottom = 88.dp),
            ) { Snackbar(it, containerColor = MaterialTheme.colorScheme.inverseSurface, contentColor = MaterialTheme.colorScheme.inverseOnSurface) }
        }
    }
}
