package com.juntadamigos.core

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.time.Duration

/**
 * Elige el lugar real que minimiza el tiempo total de viaje del grupo, ponderado por modo:
 * un minuto de quien va sin auto pesa más que uno de quien maneja ([Config.modeWeights]).
 *
 * Algoritmo:
 * 1. Arma puntos de muestra: el centro geográfico y puntos sobre el segmento entre el centro
 *    y cada participante (incluida su propia ubicación).
 * 2. Pide los tiempos reales de todos a esos puntos y se queda con las [Config.searchAreas]
 *    zonas de menor tiempo total.
 * 3. Busca lugares de la categoría elegida alrededor de esas zonas.
 * 4. Para cada modo de viaje, pide una matriz de tiempos (participantes con ese modo × candidatos).
 * 5. Descarta candidatos donde alguien no tiene ruta o donde alguien supera el tope individual.
 * 6. Ordena por menor tiempo total ponderado; desempata por menor peor viaje individual.
 *
 * Por qué no buscar solo alrededor del centro geográfico: si uno va en auto y otro en
 * colectivo, el menor tiempo total está corrido hacia quien viaja más lento, a veces casi en
 * su casa. Un círculo en el centro nunca llega ahí y además Places devuelve los lugares más
 * populares del círculo (el microcentro), que favorecen al que va en auto. Muestrear tiempos
 * reales primero lleva la búsqueda a donde el grupo de verdad tarda menos.
 *
 * Por qué ponderar: con transporte público el tiempo total sin ponderar suele ganarlo el
 * microcentro (los trenes llegan rápido ahí), y el que va en colectivo termina viajando más
 * que el que maneja. Validado con direcciones reales de San Isidro (auto) y Lomas (colectivo).
 */
class MeetingPointFinder(
    private val placeSearch: PlaceSearch,
    private val travelTimeMatrix: TravelTimeMatrix,
    private val config: Config = Config(),
) {
    data class Config(
        /**
         * Peso de cada minuto de viaje según el modo. 1.5 = un minuto a pie o en colectivo
         * cuenta como uno y medio en auto. Los tiempos que se muestran siguen siendo los reales.
         */
        val modeWeights: Map<TravelMode, Double> = mapOf(
            TravelMode.DRIVE to 1.0,
            TravelMode.TRANSIT to 1.5,
            TravelMode.WALK to 1.5,
            TravelMode.BICYCLE to 1.5,
        ),
        /** Cantidad de candidatos a evaluar. Más candidatos = mejor resultado y más costo de API. */
        val maxCandidates: Int = 15,
        /** Puntos de muestra entre el centro y cada participante (el último es su ubicación). */
        val samplesPerParticipant: Int = 4,
        /** En cuántas de las mejores zonas muestreadas se buscan lugares. */
        val searchAreas: Int = 3,
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
            require(TravelMode.entries.all { (modeWeights[it] ?: 0.0) > 0.0 }) { "Falta un peso positivo por modo" }
            require(maxCandidates > 0)
            require(samplesPerParticipant > 0)
            require(searchAreas > 0)
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
        val areas = bestAreas(participants, center, radius, maxIndividualTravel)
        val perArea = (config.maxCandidates + areas.size - 1) / areas.size
        val candidates = coroutineScope {
            areas.map { a -> async { placeSearch.searchNearby(a.center, a.radiusMeters, category, perArea) } }
                .awaitAll()
        }
            // Intercalar las zonas para que el recorte no deje afuera a la segunda.
            .let { lists -> (0 until (lists.maxOfOrNull { it.size } ?: 0)).flatMap { i -> lists.mapNotNull { it.getOrNull(i) } } }
            .distinctBy { it.id }
            .take(config.maxCandidates)

        if (candidates.isEmpty()) {
            return MeetingResult(emptyList(), areas, 0, 0, 0)
        }

        val times = travelTimesByParticipant(participants, candidates.map { it.location })

        var unreachable = 0
        var overCap = 0
        val suggestions = candidates.mapIndexedNotNull { j, place ->
            val perPerson = participants.associate { p -> p.id to times.getValue(p.id)[j] }
            if (perPerson.values.any { it == null }) {
                unreachable++
                return@mapIndexedNotNull null
            }
            @Suppress("UNCHECKED_CAST")
            val real = perPerson as Map<String, Duration>
            val suggestion = Suggestion(place, real, score = weightedTotal(participants) { real.getValue(it.id) })
            if (maxIndividualTravel != null && suggestion.maxTravelTime > maxIndividualTravel) {
                overCap++
                return@mapIndexedNotNull null
            }
            suggestion
        }

        val ranked = suggestions.sortedWith(
            // Redondeo a segundos: diferencias menores no significan nada y las APIs devuelven segundos.
            compareBy<Suggestion> { it.score.inWholeSeconds }
                .thenBy { it.maxTravelTime.inWholeSeconds }
                .thenByDescending { it.place.rating ?: 0.0 },
        )

        return MeetingResult(
            suggestions = ranked.take(topN),
            searchAreas = areas,
            candidatesEvaluated = candidates.size,
            discardedUnreachable = unreachable,
            discardedOverCap = overCap,
        )
    }

    private inline fun weightedTotal(participants: List<Participant>, time: (Participant) -> Duration): Duration =
        participants.fold(Duration.ZERO) { acc, p -> acc + time(p) * config.modeWeights.getValue(p.mode) }

    /** Radio de cada zona: el paso entre puntos de muestra, para cubrir el hueco entre ellos. */
    internal fun searchRadius(center: LatLng, participants: List<Participant>): Double {
        val farthest = participants.maxOf { Geo.distanceMeters(center, it.location) }
        return (farthest / config.samplesPerParticipant).coerceIn(config.minRadiusMeters, config.maxRadiusMeters)
    }

    internal fun samplePoints(center: LatLng, participants: List<Participant>): List<LatLng> {
        val n = config.samplesPerParticipant
        return listOf(center) + participants.flatMap { p ->
            (1..n).map { k -> Geo.interpolate(center, p.location, k.toDouble() / n) }
        }
    }

    /**
     * Mide tiempos reales a los puntos de muestra y devuelve las mejores zonas, separadas
     * entre sí al menos un radio. Si ningún punto tiene ruta para todos, busca en el centro.
     */
    private suspend fun bestAreas(
        participants: List<Participant>,
        center: LatLng,
        radius: Double,
        maxIndividualTravel: Duration?,
    ): List<SearchArea> {
        val samples = samplePoints(center, participants)
        val times = travelTimesByParticipant(participants, samples)
        val scored = samples.indices.mapNotNull { j ->
            val perPerson = participants.associateWith { times.getValue(it.id)[j] ?: return@mapNotNull null }
            val total = weightedTotal(participants) { perPerson.getValue(it) }
            // Con tope, preferir zonas que lo cumplen; las demás quedan como último recurso.
            val overCap = maxIndividualTravel != null && perPerson.values.max() > maxIndividualTravel
            Triple(samples[j], overCap, total)
        }.sortedWith(compareBy<Triple<LatLng, Boolean, Duration>> { it.second }.thenBy { it.third })

        val chosen = mutableListOf<LatLng>()
        for ((point, _, _) in scored) {
            if (chosen.size == config.searchAreas) break
            if (chosen.none { Geo.distanceMeters(it, point) < radius }) chosen += point
        }
        if (chosen.isEmpty()) chosen += center
        return chosen.map { SearchArea(it, radius) }
    }

    /** Devuelve, por id de participante, la lista de tiempos a cada candidato (mismo orden). */
    private suspend fun travelTimesByParticipant(
        participants: List<Participant>,
        destinations: List<LatLng>,
    ): Map<String, List<Duration?>> = coroutineScope {
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
