@file:OptIn(ExperimentalLayoutApi::class)

package com.juntadamigos.app.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.Dot
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.juntadamigos.app.ui.theme.AppColors
import com.juntadamigos.app.ui.theme.AppTheme
import com.juntadamigos.app.ui.theme.Display
import com.juntadamigos.core.LatLng
import com.juntadamigos.core.MeetingResult
import com.juntadamigos.core.MeetingShare
import com.juntadamigos.core.PlaceCategory
import com.juntadamigos.core.Suggestion
import com.google.android.gms.maps.model.LatLng as MapLatLng

/** Pantalla 5: mapa arriba y hoja con el top 3 (el elegido abierto, el resto compacto). */
@Composable
fun ResultsScreen(
    result: MeetingResult,
    people: List<PersonInput>,
    category: PlaceCategory,
    /** Zona elegida ("Palermo"); null = se buscó en el medio. Sin topes sin auto si hay zona. */
    zoneLabel: String?,
    onBack: () -> Unit,
) {
    val caps = zoneLabel == null
    val context = LocalContext.current
    val shareNames = remember(people) { people.associate { it.id to it.shareName } }
    var selected by rememberSaveable(result) { mutableIntStateOf(0) }
    // Qué encuadra el mapa: null = todos (amigos + lugares), i = zoom al lugar i.
    // focusRequest cambia en cada toque para re-centrar aunque se toque el mismo lugar.
    var focus by rememberSaveable(result) { mutableStateOf<Int?>(null) }
    var focusRequest by rememberSaveable(result) { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val focusPlace: (Int) -> Unit = { i ->
        selected = i
        focus = i
        focusRequest++
    }
    LaunchedEffect(selected) { listState.animateScrollToItem(selected) }

    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val mapHeight = top + 340.dp
    val sheetTop = mapHeight - 44.dp

    Box(Modifier.fillMaxSize()) {
        ResultsMap(
            result = result,
            people = people,
            selected = selected,
            focus = focus,
            focusRequest = focusRequest,
            onSelect = focusPlace,
            topPadding = top + 64.dp,
            modifier = Modifier.fillMaxWidth().height(mapHeight),
        )
        Row(
            Modifier.padding(top = top + 8.dp, start = 8.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp).floating(CircleShape),
                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            ) {
                Icon(AppIcons.Back, contentDescription = "Volver", tint = MaterialTheme.colorScheme.onSurface)
            }
            Box(
                Modifier.height(48.dp).floating(CircleShape).padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (zoneLabel != null) "${categoryPlural(category)} en $zoneLabel" else "${categoryPlural(category)} para ${groupLabel(people.size)}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontFamily = Display,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                )
            }
        }
        if (focus != null) {
            Row(
                Modifier.align(Alignment.TopEnd).padding(top = sheetTop - 60.dp, end = 16.dp).height(48.dp)
                    .floating(CircleShape)
                    .clickable(role = Role.Button) {
                        focus = null
                        focusRequest++
                    }
                    .padding(start = 14.dp, end = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(AppIcons.List, contentDescription = null, modifier = Modifier.size(20.dp))
                Text("Ver todos", style = MaterialTheme.typography.labelLarge)
            }
        }
        Surface(
            modifier = Modifier.fillMaxSize().padding(top = sheetTop),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shadowElevation = 8.dp,
        ) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp, top = 20.dp,
                    bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(result.suggestions, key = { _, s -> s.place.id }) { i, s ->
                    if (i == selected) {
                        SelectedPlaceCard(
                            rank = i + 1,
                            s = s,
                            people = people,
                            category = category,
                            caps = caps,
                            onOpenMaps = { openUrl(context, MeetingShare.mapsUrl(s.place)) },
                            onShare = { shareOnWhatsApp(context, MeetingShare.message(s, shareNames)) },
                        )
                    } else {
                        CompactPlaceCard(rank = i + 1, s = s, people = people, caps = caps, onClick = { focusPlace(i) })
                    }
                }
                item { Summary(result) }
            }
        }
        // Tirador decorativo de la hoja.
        Box(
            Modifier.align(Alignment.TopCenter).padding(top = sheetTop + 8.dp).width(32.dp).height(4.dp)
                .clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.outline),
        )
    }
}

/** Fondo blanco + sombra elev-2 de los controles que flotan sobre el mapa. */
@Composable
private fun Modifier.floating(shape: androidx.compose.ui.graphics.Shape): Modifier =
    this.elevated(2, shape).clip(shape).background(MaterialTheme.colorScheme.surfaceContainerLowest)

@Composable
private fun SelectedPlaceCard(
    rank: Int,
    s: Suggestion,
    people: List<PersonInput>,
    category: PlaceCategory,
    caps: Boolean,
    onOpenMaps: () -> Unit,
    onShare: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val fairness = fairnessOf(s, people, caps)
    val maxMinutes = s.travelTimes.values.maxOfOrNull { it.roundedMinutes() } ?: 0
    val scale = maxOf(45L, (maxMinutes + 4) / 5 * 5)
    AppCard(Modifier.fillMaxWidth(), border = BorderStroke(2.dp, cs.tertiary)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RankBadge(rank, 32.dp, strong = true)
                Column(Modifier.weight(1f)) {
                    Text(s.place.name, style = MaterialTheme.typography.titleLarge)
                    val type = categoryLabel(category)
                    if (s.place.rating != null) {
                        Rating(s.place.rating!!, "· $type")
                    } else {
                        Text(type, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                    }
                    s.place.address?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                people.forEach { p -> s.travelTimes[p.id]?.let { TimeBar(p, it, scale, showCap = caps) } }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                EvennessPill(fairness.evenness, "${evennessLabel(fairness.evenness)} · ${fairness.spreadMinutes} min de diferencia")
                Box(Modifier.weight(1f))
                if (caps && people.any { capFor(it.mode) != null }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.width(2.dp).height(12.dp).clip(RoundedCornerShape(4.dp)).background(cs.onSurfaceVariant))
                        Text("tope", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TonalButton("Abrir en\nGoogle Maps", onOpenMaps, Modifier.weight(1f), leading = AppIcons.Navigate)
                androidx.compose.material3.Button(
                    onClick = onShare,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 12.dp),
                ) {
                    Icon(AppIcons.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                    Box(Modifier.width(8.dp))
                    Text("Compartir por\nWhatsApp", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun CompactPlaceCard(rank: Int, s: Suggestion, people: List<PersonInput>, caps: Boolean, onClick: () -> Unit) {
    val fairness = fairnessOf(s, people, caps)
    val label = "${s.place.name}, lugar $rank, ${evennessLabel(fairness.evenness).lowercase()}, ${fairness.spreadMinutes} minutos de diferencia"
    AppCard(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = label }) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RankBadge(rank, 24.dp, strong = false)
                Text(s.place.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                s.place.rating?.let { Rating(it) }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                people.forEach { p -> s.travelTimes[p.id]?.let { TimeChip(p, it) } }
                EvennessPill(fairness.evenness, "±${fairness.spreadMinutes} min", compact = true)
            }
        }
    }
}

@Composable
private fun Summary(result: MeetingResult) {
    val discarded = buildList {
        if (result.discardedUnreachable > 0) add("${result.discardedUnreachable} sin ruta para alguien")
        if (result.discardedOverCap > 0) add("${result.discardedOverCap} superan el tope")
    }.joinToString(", ")
    Text(
        "Se evaluaron ${result.candidatesEvaluated} lugares" + if (discarded.isNotEmpty()) " ($discarded)." else ".",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp, start = 4.dp),
    )
}

// --- Mapa ---

@Composable
private fun ResultsMap(
    result: MeetingResult,
    people: List<PersonInput>,
    selected: Int,
    focus: Int?,
    focusRequest: Int,
    onSelect: (Int) -> Unit,
    topPadding: androidx.compose.ui.unit.Dp,
    modifier: Modifier,
) {
    val camera = rememberCameraPositionState()
    var loaded by remember { mutableStateOf(false) }
    var firstFrame by remember { mutableStateOf(true) }
    val friends = people.map { it to it.resolved.location.toMap() }
    val places = result.suggestions.map { it.place.location.toMap() }
    val colors = AppTheme.colors
    val cs = MaterialTheme.colorScheme
    val dark = isSystemInDarkTheme()
    val style = remember(dark) { MapStyleOptions(mapStyleJson(colors)) }

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
            CameraUpdateFactory.newLatLngBounds(bounds, 80)
        }
        if (firstFrame) {
            camera.move(update)
            firstFrame = false
        } else {
            camera.animate(update, durationMs = 700)
        }
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = camera,
        properties = MapProperties(mapStyleOptions = style),
        uiSettings = MapUiSettings(zoomControlsEnabled = false, mapToolbarEnabled = false),
        // Arriba: barra flotante; abajo: la hoja tapa 44 dp del mapa.
        contentPadding = PaddingValues(top = topPadding, bottom = 44.dp),
        onMapLoaded = { loaded = true },
    ) {
        // Recorrido punteado de cada persona al lugar elegido, en su color.
        places.getOrNull(selected)?.let { dest ->
            friends.forEach { (person, from) ->
                Polyline(
                    points = listOf(from, dest),
                    color = colors.friend(person.colorIndex),
                    pattern = listOf(Dot(), Gap(16f)),
                    width = 10f,
                )
            }
        }
        friends.forEach { (person, position) ->
            MarkerComposable(
                person.id, person.colorIndex, person.mode, dark,
                state = rememberMarkerState(key = "p-${person.id}", position = position),
                title = person.displayName,
                snippet = modeLabel(person.mode),
                anchor = Offset(0.5f, 0.5f),
            ) {
                // El tema no llega al bitmap del marcador: se pasa todo explícito.
                FriendPin(person, colors, cs.surfaceContainerLowest, cs.outline, cs.onSurface)
            }
        }
        result.suggestions.forEachIndexed { i, s ->
            val isSelected = i == selected
            MarkerComposable(
                s.place.id, isSelected, dark,
                state = rememberMarkerState(key = "l-${s.place.id}", position = places[i]),
                title = "${i + 1}. ${s.place.name}",
                zIndex = if (isSelected) 3f else 2f,
                onClick = {
                    onSelect(i)
                    true // true = la cámara la movemos nosotros (con zoom), no el mapa
                },
            ) {
                PlacePin(i + 1, isSelected, cs.tertiary, cs.onTertiary, cs.surfaceContainerLowest, cs.onSurface)
            }
        }
    }
}

@Composable
private fun FriendPin(person: PersonInput, colors: AppColors, ring: Color, outline: Color, onSurface: Color) {
    Box(Modifier.size(48.dp)) {
        Box(Modifier.align(Alignment.Center).size(42.dp).clip(CircleShape).background(ring), contentAlignment = Alignment.Center) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(colors.friend(person.colorIndex)), contentAlignment = Alignment.Center) {
                Text(person.initial, color = colors.onFriend, fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        }
        Box(
            Modifier.align(Alignment.BottomEnd).size(18.dp).clip(CircleShape).background(ring)
                .border(1.5.dp, outline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(AppIcons.mode(person.mode), contentDescription = null, tint = onSurface, modifier = Modifier.size(11.dp))
        }
    }
}

@Composable
private fun PlacePin(number: Int, selected: Boolean, fill: Color, onFill: Color, ring: Color, selectedRing: Color) {
    val scale = if (selected) 1.25f else 1f
    val w = 36.dp * scale + 6.dp
    val h = 44.dp * scale + 4.dp
    Box(Modifier.size(w, h)) {
        Canvas(Modifier.size(w, h)) {
            val u = density * scale
            placePin(
                tip = Offset(size.width / 2, size.height - 2.dp.toPx()),
                fill = fill,
                ring = if (selected) selectedRing else ring,
                ringWidth = 3f,
                factor = u,
            )
        }
        Text(
            "$number",
            color = onFill,
            fontFamily = Display,
            fontWeight = FontWeight.Bold,
            fontSize = (17 * scale).sp,
            lineHeight = (22 * scale).sp,
            // Centro del círculo de la gota: 27 unidades sobre la punta (que está 2 dp sobre el borde).
            modifier = Modifier.align(Alignment.TopCenter).padding(top = h - 2.dp - 27.dp * scale - 11.dp * scale),
        )
    }
}

/** Estilo del mapa con los colores del diseño (tierra, calles, parques, agua, etiquetas). */
private fun mapStyleJson(c: AppColors): String {
    fun hex(color: Color) = "#%06X".format(0xFFFFFF and color.toArgb())
    fun rule(feature: String?, element: String?, vararg stylers: String) = buildString {
        append("{")
        feature?.let { append("\"featureType\":\"$it\",") }
        element?.let { append("\"elementType\":\"$it\",") }
        append("\"stylers\":[${stylers.joinToString(",") { "{$it}" }}]}")
    }
    return listOf(
        rule(null, "geometry", "\"color\":\"${hex(c.mapLand)}\""),
        rule(null, "labels.text.fill", "\"color\":\"${hex(c.mapLabel)}\""),
        rule(null, "labels.text.stroke", "\"color\":\"${hex(c.mapLand)}\""),
        rule("road", "geometry.fill", "\"color\":\"${hex(c.mapRoad)}\""),
        rule("road", "geometry.stroke", "\"color\":\"${hex(c.mapRoadEdge)}\""),
        rule("poi.park", "geometry", "\"color\":\"${hex(c.mapPark)}\""),
        rule("water", "geometry", "\"color\":\"${hex(c.mapWater)}\""),
        // Sin íconos de otros comercios: los únicos lugares marcados son los sugeridos.
        rule("poi.business", null, "\"visibility\":\"off\""),
        rule("transit", "labels.icon", "\"visibility\":\"off\""),
    ).joinToString(",", prefix = "[", postfix = "]")
}

/** Zoom al tocar un lugar: nivel calle, se ven las cuadras alrededor. */
private const val PLACE_ZOOM = 15f

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
