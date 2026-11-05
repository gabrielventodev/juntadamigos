package com.juntadamigos.app.ui

import com.juntadamigos.core.MeetingPointFinder
import com.juntadamigos.core.Suggestion
import com.juntadamigos.core.TravelMode
import kotlin.time.Duration

/**
 * Cómo se muestra qué tan parejo es un lugar. Es solo presentación: el orden lo decide el motor.
 * Umbrales (supuesto de diseño, no del motor): hasta 5 min de diferencia entre el que más y el
 * que menos tarda es "Parejo", hasta 10 "Medio parejo", más de 10 o alguien sin auto pasado de
 * su tope es "Desparejo".
 */
enum class Evenness { EVEN, MEDIUM, UNEVEN }

data class Fairness(val evenness: Evenness, val spreadMinutes: Long, val overCap: List<OverCap>)

/** Alguien sin auto que tarda más que su tope. */
data class OverCap(val personId: String, val minutes: Long, val capMinutes: Long)

/** Topes del motor por modo (DRIVE no tiene). Misma fuente que usa el cálculo. */
val NoCarCaps: Map<TravelMode, Duration> = MeetingPointFinder.Config().noCarMaxTravel

fun capFor(mode: TravelMode): Duration? = NoCarCaps[mode]

fun Duration.roundedMinutes(): Long = (inWholeSeconds + 30) / 60

fun fairnessOf(s: Suggestion, people: List<PersonInput>, caps: Boolean = true): Fairness =
    fairnessOf(s, people.associate { it.id to it.mode }, caps)

/**
 * [modes]: modo de viaje por id de persona. [caps] = false en zona elegida: el motor no aplica
 * los topes sin auto ahí, así que tampoco se marcan como superados.
 */
fun fairnessOf(s: Suggestion, modes: Map<String, TravelMode>, caps: Boolean = true): Fairness {
    val minutes = s.travelTimes.values.map { it.roundedMinutes() }
    val spread = (minutes.maxOrNull() ?: 0) - (minutes.minOrNull() ?: 0)
    val over = if (!caps) emptyList() else modes.mapNotNull { (id, mode) ->
        val cap = capFor(mode) ?: return@mapNotNull null
        val t = s.travelTimes[id] ?: return@mapNotNull null
        if (t > cap) OverCap(id, t.roundedMinutes(), cap.roundedMinutes()) else null
    }
    val evenness = when {
        over.isNotEmpty() || spread > 10 -> Evenness.UNEVEN
        spread > 5 -> Evenness.MEDIUM
        else -> Evenness.EVEN
    }
    return Fairness(evenness, spread, over)
}
