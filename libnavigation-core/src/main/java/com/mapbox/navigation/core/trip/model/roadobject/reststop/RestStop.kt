package com.mapbox.navigation.core.trip.model.roadobject.reststop

import com.mapbox.navigation.base.trip.model.roadobject.RoadObject
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectGeometry
import com.mapbox.navigation.core.trip.model.roadobject.RoadObjectType

/**
 * Road object type that provides information about rest stops on the route.
 *
 * @param restStopType information about a rest stop. See [RestStopType].
 * @see RoadObject
 * @see RoadObjectType.REST_STOP
 */
class RestStop private constructor(
    distanceFromStartOfRoute: Double?,
    objectGeometry: RoadObjectGeometry,
    @RestStopType.Type val restStopType: Int
) : RoadObject(
    RoadObjectType.REST_STOP,
    distanceFromStartOfRoute,
    objectGeometry
) {

    /**
     * Transform this object into a builder to mutate the values.
     */
    fun toBuilder(): Builder = Builder(
        objectGeometry
    )
        .distanceFromStartOfRoute(distanceFromStartOfRoute)
        .restStopType(restStopType)

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as RestStop

        if (restStopType != other.restStopType) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + restStopType
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RestStopAlert(restStopType=$restStopType), ${super.toString()}"
    }

    /**
     * Use to create a new instance.
     *
     * @see RestStop
     */
    class Builder(
        private val objectGeometry: RoadObjectGeometry
    ) {
        private var distanceFromStartOfRoute: Double? = null

        @RestStopType.Type
        private var restStopType: Int = RestStopType.UNKNOWN

        /**
         * Add optional distance from start of route.
         * If [distanceFromStartOfRoute] is negative, `null` will be used.
         */
        fun distanceFromStartOfRoute(distanceFromStartOfRoute: Double?): Builder = apply {
            this.distanceFromStartOfRoute = distanceFromStartOfRoute
        }

        /**
         * Sets information about a rest stop. See [RestStopType].
         */
        fun restStopType(@RestStopType.Type restStopType: Int): Builder = apply {
            this.restStopType = restStopType
        }

        /**
         * Build the object instance.
         */
        fun build() =
            RestStop(
                distanceFromStartOfRoute,
                objectGeometry,
                restStopType
            )
    }
}
