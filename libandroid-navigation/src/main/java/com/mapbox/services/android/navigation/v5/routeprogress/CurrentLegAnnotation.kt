package com.mapbox.services.android.navigation.v5.routeprogress

import com.mapbox.api.directions.v5.models.MaxSpeed
import java.io.Serializable

/**
 * This class represents the current annotation being traveled along at a given time
 * during navigation.
 *
 *
 * The Mapbox Directions API gives a list of annotations, each item in the list representing an
 * annotation between two points along the leg.
 */
data class CurrentLegAnnotation(
    val index: Int,
    val distanceToAnnotation: Double,
    val distance: Double?,
    val duration: Double?,
    val speed: Double?,
    val maxspeed: MaxSpeed?,
    val congestion: String?
) : Serializable {

    companion object {
        /**
         * Create a new instance of this class by using the [CurrentLegAnnotation.Builder] class.
         *
         * @return this classes [CurrentLegAnnotation.Builder] for creating a new instance
         */
        @JvmStatic
        fun builder() = Builder()

        class Builder {
            private var index: Int = 0
            private var distanceToAnnotation: Double = 0.0
            private var distance: Double? = null
            private var duration: Double? = null
            private var speed: Double? = null
            private var maxspeed: MaxSpeed? = null
            private var congestion: String? = null

            fun index(index: Int) = apply { this.index = index }
            fun distanceToAnnotation(distanceToAnnotation: Double) = apply { this.distanceToAnnotation = distanceToAnnotation }
            fun distance(distance: Double) = apply { this.distance = distance }
            fun duration(duration: Double) = apply { this.duration = duration }
            fun speed(speed: Double) = apply { this.speed = speed }
            fun maxspeed(maxspeed: MaxSpeed) = apply { this.maxspeed = maxspeed }
            fun congestion(congestion: String) = apply { this.congestion = congestion }

            fun build() = CurrentLegAnnotation(
                    index,
                    distanceToAnnotation,
                    distance,
                    duration,
                    speed,
                    maxspeed,
                    congestion
            )
        }
    }
}
