@file:OptIn(ExperimentalMaterial3Api::class)

package com.juntadamigos.app.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.juntadamigos.core.LatLng
import com.juntadamigos.core.MeetingResult
import com.juntadamigos.core.MeetingShare
import com.juntadamigos.core.Suggestion
import com.google.android.gms.maps.model.LatLng as MapLatLng

/** Resultados: mapa con amigos y top 3, y por cada lugar tiempos, Google Maps y WhatsApp. */
@Composable
fun ResultsScreen(
    result: MeetingResult,
    people: List<PersonInput>,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val names = remember(people) { people.associate { it.id to it.displayName } }
    var selected by rememberSaveable(result) { mutableIntStateOf(0) }
    // Qué encuadra el mapa: null = todos (amigos + lugares), i = zoom al lugar i.
    // focusRequest cambia en cada toque para re-centrar aunque se toque el mismo lugar.
    var focus by rememberSaveable(result) { mutableStateOf<Int?>(null) }
    var focusRequest by rememberSaveable(result) { mutableIntStateOf(0) }
    val focusPlace: (Int) -> Unit = { i ->
        selected = i
        focus = i
        focusRequest++
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mejores lugares") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ResultsMap(
                result = result,
                people = people,
                selected = selected,
                focus = focus,
                focusRequest = focusRequest,
                onSelect = focusPlace,
                onShowAll = {
                    focus = null
                    focusRequest++
                },
                modifier = Modifier.fillMaxWidth().height(320.dp),
            )
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { Summary(result) }
                itemsIndexed(result.suggestions, key = { _, s -> s.place.id }) { i, s ->
                    SuggestionCard(
                        rank = i + 1,
                        s = s,
                        names = names,
                        selected = i == selected,
                        onClick = { focusPlace(i) },
                        onOpenMaps = { openUrl(context, MeetingShare.mapsUrl(s.place)) },
                        onShare = { shareOnWhatsApp(context, MeetingShare.message(s, names)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultsMap(
    result: MeetingResult,
    people: List<PersonInput>,
    selected: Int,
    focus: Int?,
    focusRequest: Int,
    onSelect: (Int) -> Unit,
    onShowAll: () -> Unit,
    modifier: Modifier,
) {
    val camera = rememberCameraPositionState()
    var loaded by remember { mutableStateOf(false) }
    var firstFrame by remember { mutableStateOf(true) }
    val friends = people.mapNotNull { p -> p.resolved?.let { p to it.location.toMap() } }
    val places = result.suggestions.map { it.place.location.toMap() }

    // Mueve la cámara según el foco. newLatLngBounds necesita el mapa medido (loaded).
    // El primer encuadre es instantáneo; los siguientes, animados.
    LaunchedEffect(loaded, result, focusRequest) {
        if (!loaded) return@LaunchedEffect
        val target = focus?.let { places.getOrNull(it) }
        val update = if (target != null) {
            CameraUpdateFactory.newLatLngZoom(target, PLACE_ZOOM)
        } else {
            val points = friends.map { it.second } + places
            if (points.isEmpty()) return@LaunchedEffect
            val bounds = LatLngBounds.builder().apply { points.forEach { include(it) } }.build()
            CameraUpdateFactory.newLatLngBounds(bounds, 120)
        }
        if (firstFrame) {
            camera.move(update)
            firstFrame = false
        } else {
            camera.animate(update, durationMs = 700)
        }
    }

    Box(modifier) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = camera,
            uiSettings = MapUiSettings(zoomControlsEnabled = false, mapToolbarEnabled = false),
            onMapLoaded = { loaded = true },
        ) {
            friends.forEach { (person, position) ->
                Marker(
                    state = rememberMarkerState(key = "p-${person.id}", position = position),
                    title = person.displayName,
                    snippet = modeLabel(person.mode),
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                )
            }
            result.suggestions.forEachIndexed { i, s ->
                val markerState = rememberMarkerState(key = "l-${s.place.id}", position = places[i])
                Marker(
                    state = markerState,
                    title = "${i + 1}. ${s.place.name}",
                    snippet = "Total ${MeetingShare.formatDuration(s.totalTravelTime)}",
                    icon = BitmapDescriptorFactory.defaultMarker(
                        if (i == selected) BitmapDescriptorFactory.HUE_RED else BitmapDescriptorFactory.HUE_ORANGE,
                    ),
                    zIndex = if (i == selected) 2f else 1f,
                    onClick = {
                        onSelect(i)
                        markerState.showInfoWindow()
                        true // true = la cámara la movemos nosotros (con zoom), no el mapa
                    },
                )
            }
        }
        if (focus != null) {
            SmallFloatingActionButton(
                onClick = onShowAll,
                modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
            ) {
                Text("Ver todos", modifier = Modifier.padding(horizontal = 12.dp))
            }
        }
    }
}

/** Zoom al tocar un lugar: nivel calle, se ven las cuadras alrededor. */
private const val PLACE_ZOOM = 15f

@Composable
private fun Summary(result: MeetingResult) {
    val discarded = buildList {
        if (result.discardedUnreachable > 0) add("${result.discardedUnreachable} sin ruta para alguien")
        if (result.discardedOverCap > 0) add("${result.discardedOverCap} superan el tope")
    }.joinToString(", ")
    Column {
        if (result.suggestions.isEmpty()) {
            Text("No encontramos lugares que sirvan a todos", style = MaterialTheme.typography.titleMedium)
            Text("Probá otro tipo de lugar o subí el tope por persona.", style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            "Se evaluaron ${result.candidatesEvaluated} lugares" + if (discarded.isNotEmpty()) " ($discarded)." else ".",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun SuggestionCard(
    rank: Int,
    s: Suggestion,
    names: Map<String, String>,
    selected: Boolean,
    onClick: () -> Unit,
    onOpenMaps: () -> Unit,
    onShare: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = if (selected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("$rank. ${s.place.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            s.place.address?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            Text(
                "Total ${MeetingShare.formatDuration(s.totalTravelTime)} · peor viaje ${MeetingShare.formatDuration(s.maxTravelTime)}" +
                    (s.place.rating?.let { " · ★ %.1f".format(it) } ?: ""),
                style = MaterialTheme.typography.bodyMedium,
            )
            s.travelTimes.entries.sortedByDescending { it.value }.forEach { (id, d) ->
                Text("${names[id] ?: id}: ${MeetingShare.formatDuration(d)}", style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                OutlinedButton(onClick = onOpenMaps) {
                    Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Google Maps")
                }
                Button(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("WhatsApp")
                }
            }
        }
    }
}

private fun LatLng.toMap() = MapLatLng(latitude, longitude)

private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: ActivityNotFoundException) {
        // Sin navegador ni Maps: no hay nada que abrir.
    }
}

/** Abre WhatsApp con el mensaje listo; si no está instalado, el selector de apps del sistema. */
private fun shareOnWhatsApp(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    try {
        context.startActivity(Intent(send).setPackage("com.whatsapp"))
    } catch (_: ActivityNotFoundException) {
        context.startActivity(Intent.createChooser(send, "Compartir con"))
    }
}
