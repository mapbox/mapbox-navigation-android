package com.mapbox.navigation.utils.internal

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.VoiceInstructions
import kotlin.math.abs

object GroupedVoiceInstructionsFactory {

    /**
     * Will examine a route and determine voice instructions that are close together.
     * [VoiceInstructions] will be grouped to reflect the beginning and ending range of
     * the voice instructions that are within a specified distance of each other.
     *
     * @param route the route to use
     * @param routeId the route identifier
     * @param rangeDistance the distance in meters for the grouping. For example if the value is
     * 100 any voice instructions within 100 meters of each other will be grouped together. The
     * resulting grouping range can be greater than this distance. For example, voice instruction A
     * occurring at 50 meters into the route, voice instruction B at 100 meters and voice instruction C at 150 meters.
     * All three of these voice instructions would be grouped together. B is with in 100 meters of A and C is with
     * in 100 meters of B.
     *
     * @return [GroupedVoiceInstructionRanges] which contains ranges
     */
    fun getGroupedAnnouncementRanges(
        route: DirectionsRoute,
        routeId: String,
        rangeDistance: Double,
    ): GroupedVoiceInstructionRanges {
        var runningDist = 0.0
        val intermediateDistances = mutableListOf<Double>()
        val ranges = mutableListOf<IntRange>()
        val voiceInstructionDistances = mutableListOf<Double>()

        ifNonNull(route.legs()?.first()?.steps()) { legSteps ->
            legSteps.forEach { legStep ->
                runningDist += legStep.distance()
                legStep.voiceInstructions()?.forEach {
                    val absoluteDistance = runningDist - (it.distanceAlongGeometry() ?: 0.0)
                    voiceInstructionDistances.add(absoluteDistance)
                }
            }
        }

        voiceInstructionDistances.forEachIndexed { index, distance ->
            if (index > 1) {
                if (abs(distance - voiceInstructionDistances[index - 1]) < rangeDistance) {
                    if (intermediateDistances.isEmpty()) {
                        intermediateDistances.add(voiceInstructionDistances[index - 1])
                        intermediateDistances.add(distance)
                    } else {
                        intermediateDistances.add(distance)
                    }
                } else {
                    if (intermediateDistances.size >= 2) {
                        ranges.add(
                            IntRange(
                                intermediateDistances.first().toInt(),
                                intermediateDistances.last().toInt(),
                            ),
                        )
                    }
                    intermediateDistances.clear()
                }
            }
        }
        if (intermediateDistances.size >= 2) {
            ranges.add(
                IntRange(
                    intermediateDistances.first().toInt(),
                    intermediateDistances.last().toInt(),
                ),
            )
        }

        return GroupedVoiceInstructionRanges(routeId, ranges)
    }
}

class GroupedVoiceInstructionRanges internal constructor(
    val routeId: String,
    private val ranges: List<IntRange>,
) {
    fun isInRange(distance: Int): Boolean {
        return ranges.any { it.contains(distance) }
    }
}
