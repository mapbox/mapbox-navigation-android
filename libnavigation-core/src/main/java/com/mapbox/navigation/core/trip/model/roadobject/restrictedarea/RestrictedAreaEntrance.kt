package com.mapbox.navigation.core.trip.model.roadobject.restrictedarea

import com.mapbox.navigation.base.trip.model.roadobject.RoadObject
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectGeometry
import com.mapbox.navigation.core.trip.model.roadobject.RoadObjectType

/**
 * Road object type that provides information about restricted areas on the route.
 *
 * @see RoadObject
 * @see RoadObjectType.RESTRICTED_AREA_ENTRANCE
 */
class RestrictedAreaEntrance private constructor(
    distanceFromStartOfRoute: Double?,
    objectGeometry: RoadObjectGeometry
) : RoadObject(
    RoadObjectType.RESTRICTED_AREA_ENTRANCE,
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
        return "RestrictedAreaEntranceAlert() ${super.toString()}"
    }

    /**
     * Use to create a new instance.
     *
     * @see RestrictedAreaEntrance
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
            RestrictedAreaEntrance(
                distanceFromStartOfRoute,
                objectGeometry
            )
    }
}
