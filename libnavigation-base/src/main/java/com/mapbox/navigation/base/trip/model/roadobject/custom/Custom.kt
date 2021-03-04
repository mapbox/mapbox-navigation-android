package com.mapbox.navigation.base.trip.model.roadobject.custom

import com.mapbox.navigation.base.trip.model.roadobject.RoadObject
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectGeometry
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectType

/**
 * Road object type that provides information about custom objects.
 *
 * @see RoadObject
 * @see RoadObjectType.CUSTOM
 */
class Custom private constructor(
    distanceFromStartOfRoute: Double?,
    objectGeometry: RoadObjectGeometry
) : RoadObject(
    RoadObjectType.CUSTOM,
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

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "Custom() ${super.toString()}"
    }

    /**
     * Use to create a new instance.
     *
     * @see Custom
     */
    class Builder(
        private val objectGeometry: RoadObjectGeometry
    ) {
        private var distanceFromStartOfRoute: Double? = null

        /**
         * Add optional distance from start of route.
         * If [distanceFromStartOfRoute] is negative, `null` will be used.
         */
        fun distanceFromStartOfRoute(distanceFromStartOfRoute: Double?): Builder = apply {
            this.distanceFromStartOfRoute = distanceFromStartOfRoute
        }

        /**
         * Build the object instance.
         */
        fun build() = Custom(
            distanceFromStartOfRoute,
            objectGeometry
        )
    }
}
