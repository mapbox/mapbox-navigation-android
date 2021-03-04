package com.mapbox.navigation.base.trip.model.roadobject.bridge

import com.mapbox.navigation.base.trip.model.roadobject.RoadObject
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectGeometry
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectType

/**
 * Road object type that provides information about bridges on the route.
 *
 * @see RoadObject
 * @see RoadObjectType.BRIDGE_ENTRANCE
 */
class BridgeExit private constructor(
    distanceFromStartOfRoute: Double?,
    objectGeometry: RoadObjectGeometry
) : RoadObject(
    RoadObjectType.BRIDGE_EXIT,
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
        return "BridgeExit() ${super.toString()}"
    }

    /**
     * Use to create a new instance.
     *
     * @see BridgeExit
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
        fun build() =
            BridgeExit(
                distanceFromStartOfRoute,
                objectGeometry
            )
    }
}
