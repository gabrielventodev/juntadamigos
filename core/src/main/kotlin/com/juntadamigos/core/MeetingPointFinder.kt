package com.juntadamigos.core

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.time.Duration

/**
 * Elige el lugar real que minimiza el tiempo total de viaje del grupo.
 *
 * Algoritmo:
 * 1. Calcula el centro geográfico de los participantes.
 * 2. Busca lugares candidatos de la categoría elegida alrededor de ese centro.
 * 3. Para cada modo de viaje, pide una matriz de tiempos (participantes con ese modo × candidatos).
 * 4. Descarta candidatos donde alguien no tiene ruta o donde alguien supera [Config.maxIndividualTravel].
 * 5. Ordena por menor tiempo total; desempata por menor peor viaje individual.
 *
 * Por qué el centro geográfico solo sirve para buscar candidatos: si uno va en auto y
 * otro en colectivo, el lugar más justo suele estar corrido hacia quien viaja más lento.
 * Por eso el radio de búsqueda cubre una buena parte del área entre participantes.
 */
class MeetingPointFinder(
    private val placeSearch: PlaceSearch,
    private val travelTimeMatrix: TravelTimeMatrix,
    private val config: Config = Config(),
) {
    data class Config(
        /** Cantidad de candidatos a evaluar. Más candidatos = mejor resultado y más costo de API. */
        val maxCandidates: Int = 15,
        /** Fracción de la distancia máxima al centro usada como radio de búsqueda. */
        val radiusFactor: Double = 0.5,
        val minRadiusMeters: Double = 1_000.0,
        /** Límite de la Places API (New) para searchNearby. */
        val maxRadiusMeters: Double = 50_000.0,
        /**
         * Elementos máximos por llamada a la matriz. Routes API admite 625 en general
         * y 100 con TRANSIT; usamos 100 para todos y partimos en lotes.
         */
        val maxMatrixElements: Int = 100,
    ) {
        init {
            require(maxCandidates > 0)
            require(minRadiusMeters in 1.0..maxRadiusMeters)
            require(maxMatrixElements > 0)
        }
    }

    suspend fun find(
        participants: List<Participant>,
        category: PlaceCategory,
        /** Tope opcional: ningún participante puede viajar más que esto. */
        maxIndividualTravel: Duration? = null,
        topN: Int = 3,
    ): MeetingResult {
        require(participants.size >= 2) { "Se necesitan al menos 2 participantes" }
        require(participants.map { it.id }.toSet().size == participants.size) { "Ids de participantes repetidos" }

        val center = Geo.centroid(participants.map { it.location })
        val radius = searchRadius(center, participants)
        val candidates = placeSearch
            .searchNearby(center, radius, category, config.maxCandidates)
            .distinctBy { it.id }
            .take(config.maxCandidates)

        if (candidates.isEmpty()) {
            return MeetingResult(emptyList(), center, radius, 0, 0, 0)
        }

        val times = travelTimesByParticipant(participants, candidates)

        var unreachable = 0
        var overCap = 0
        val suggestions = candidates.mapIndexedNotNull { j, place ->
            val perPerson = participants.associate { p -> p.id to times.getValue(p.id)[j] }
            if (perPerson.values.any { it == null }) {
                unreachable++
                return@mapIndexedNotNull null
            }
            @Suppress("UNCHECKED_CAST")
            val suggestion = Suggestion(place, perPerson as Map<String, Duration>)
            if (maxIndividualTravel != null && suggestion.maxTravelTime > maxIndividualTravel) {
                overCap++
                return@mapIndexedNotNull null
            }
            suggestion
        }

        val ranked = suggestions.sortedWith(
            // Redondeo a segundos: diferencias menores no significan nada y las APIs devuelven segundos.
            compareBy<Suggestion> { it.totalTravelTime.inWholeSeconds }
                .thenBy { it.maxTravelTime.inWholeSeconds }
                .thenByDescending { it.place.rating ?: 0.0 },
        )

        return MeetingResult(
            suggestions = ranked.take(topN),
            searchCenter = center,
            searchRadiusMeters = radius,
            candidatesEvaluated = candidates.size,
            discardedUnreachable = unreachable,
            discardedOverCap = overCap,
        )
    }

    internal fun searchRadius(center: LatLng, participants: List<Participant>): Double {
        val farthest = participants.maxOf { Geo.distanceMeters(center, it.location) }
        return (farthest * config.radiusFactor).coerceIn(config.minRadiusMeters, config.maxRadiusMeters)
    }

    /** Devuelve, por id de participante, la lista de tiempos a cada candidato (mismo orden). */
    private suspend fun travelTimesByParticipant(
        participants: List<Participant>,
        candidates: List<Place>,
    ): Map<String, List<Duration?>> = coroutineScope {
        val destinations = candidates.map { it.location }
        participants.groupBy { it.mode }.map { (mode, group) ->
            async {
                val rows = matrixInBatches(group.map { it.location }, destinations, mode)
                group.mapIndexed { i, p -> p.id to rows[i] }
            }
        }.awaitAll().flatten().toMap()
    }

    /** Parte la matriz para respetar el límite de elementos por llamada. */
    private suspend fun matrixInBatches(
        origins: List<LatLng>,
        destinations: List<LatLng>,
        mode: TravelMode,
    ): List<List<Duration?>> {
        val destPerBatch = minOf(destinations.size, config.maxMatrixElements).coerceAtLeast(1)
        val originsPerBatch = (config.maxMatrixElements / destPerBatch).coerceAtLeast(1)
        val result = List(origins.size) { arrayOfNulls<Duration>(destinations.size) }

        for (oStart in origins.indices step originsPerBatch) {
            val oChunk = origins.subList(oStart, minOf(oStart + originsPerBatch, origins.size))
            for (dStart in destinations.indices step destPerBatch) {
                val dChunk = destinations.subList(dStart, minOf(dStart + destPerBatch, destinations.size))
                val rows = travelTimeMatrix.travelTimes(oChunk, dChunk, mode)
                check(rows.size == oChunk.size && rows.all { it.size == dChunk.size }) {
                    "La matriz devuelta no coincide con ${oChunk.size}x${dChunk.size}"
                }
                rows.forEachIndexed { i, row ->
                    row.forEachIndexed { j, d -> result[oStart + i][dStart + j] = d }
                }
            }
        }
        return result.map { it.toList() }
    }
}
