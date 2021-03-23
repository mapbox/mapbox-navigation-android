package com.mapbox.navigation.core.trip.model.roadobject.tunnel

import com.mapbox.navigation.base.trip.model.roadobject.RoadObject
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectGeometry
import com.mapbox.navigation.core.trip.model.roadobject.RoadObjectType

/**
 * Road object type that provides information about tunnels on the route.
 *
 * @param info tunnel information
 * @see RoadObject
 * @see RoadObjectType.TUNNEL_ENTRANCE
 */
class TunnelEntrance private constructor(
    distanceFromStartOfRoute: Double?,
    objectGeometry: RoadObjectGeometry,
    val info: TunnelInfo
) : RoadObject(
    RoadObjectType.TUNNEL_ENTRANCE,
    distanceFromStartOfRoute,
    objectGeometry
) {

    /**
     * Transform this object into a builder to mutate the values.
     */
    fun toBuilder(): Builder = Builder(objectGeometry, info)
        .distanceFromStartOfRoute(distanceFromStartOfRoute)

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as TunnelEntrance

        if (info != other.info) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (info.hashCode())
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "TunnelEntranceAlert(info=$info), ${super.toString()}"
    }

    /**
     * Use to create a new instance.
     *
     * @see TunnelEntrance
     */
    class Builder(
        private val objectGeometry: RoadObjectGeometry,
        private val info: TunnelInfo
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
            TunnelEntrance(
                distanceFromStartOfRoute,
                objectGeometry,
                info
            )
    }
}
