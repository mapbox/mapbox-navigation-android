package com.mapbox.navigation.core.trip.model.roadobject.incident

import com.mapbox.navigation.base.trip.model.roadobject.RoadObject
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectGeometry
import com.mapbox.navigation.core.trip.model.roadobject.RoadObjectType

/**
 * Incident alert provides information about incidents on a way like *congestion*, *mass transit*,
 * etc. More types of incidents see [IncidentType].
 *
 * @param info incident alert info.
 */
class Incident private constructor(
    distanceFromStartOfRoute: Double?,
    objectGeometry: RoadObjectGeometry,
    val info: IncidentInfo
) : RoadObject(RoadObjectType.INCIDENT, distanceFromStartOfRoute, objectGeometry) {

    /**
     * Transform this object into a builder to mutate the values.
     */
    fun toBuilder() = Builder(objectGeometry, info)
        .distanceFromStartOfRoute(distanceFromStartOfRoute)

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as Incident

        if (info != other.info) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + info.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "IncidentAlert(" +
            "info=$info" +
            "), ${super.toString()}"
    }

    /**
     * Use to create a new instance.
     *
     * @see Incident
     */
    class Builder(
        private val objectGeometry: RoadObjectGeometry,
        private val info: IncidentInfo
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
        fun build() = Incident(
            distanceFromStartOfRoute,
            objectGeometry,
            info
        )
    }
}
