package com.mapbox.navigation.core.trip.roadobject.reststop

import com.mapbox.navigation.base.trip.model.eh.EHorizon
import com.mapbox.navigation.base.trip.model.roadobject.RoadObject
import com.mapbox.navigation.base.trip.model.roadobject.UpcomingRoadObject
import com.mapbox.navigation.base.trip.model.roadobject.reststop.RestStop

/**
 * The class provides access to [RestStop] available in [UpcomingRoadObject].
 *
 * @param restStop [RestStop]
 * @param distanceToStart remaining distance to the start of the object.
 * This value will be negative after passing the start of the object and until we cross the finish
 * point of the [RoadObject]s geometry for objects that are on the actively navigated route,
 * but it will be zero for [EHorizon] objects. It will be null if couldn't be determined.
 */
class RestStopFromRoadObject private constructor(
    val restStop: RestStop,
    val distanceToStart: Double?
) {

    /**
     * @return the [Builder] that created the [RestStopFromRoadObject]
     */
    fun toBuilder(): Builder = Builder(restStop).apply {
        distanceToStart(distanceToStart)
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RestStopFromRoadObject

        if (restStop != other.restStop) return false
        if (distanceToStart != other.distanceToStart) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = restStop.hashCode()
        result = 31 * result + (distanceToStart.hashCode())
        return result
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun toString(): String {
        return "RestStopFromRoadObject(restStop=$restStop, distanceToStart=$distanceToStart)"
    }

    /**
     * Builder of [RestStopFromRoadObject]
     *
     * @param restStop [RestStop]
     */
    class Builder(private val restStop: RestStop) {

        private var distanceToStart: Double? = null

        /**
         * Apply distance to start.
         *
         * @param distanceToStart
         */
        fun distanceToStart(distanceToStart: Double?): Builder =
            apply { this.distanceToStart = distanceToStart }

        /**
         * Build an instance of [RestStopFromRoadObject]
         */
        fun build(): RestStopFromRoadObject {
            return RestStopFromRoadObject(
                restStop = restStop,
                distanceToStart = distanceToStart,
            )
        }
    }
}
