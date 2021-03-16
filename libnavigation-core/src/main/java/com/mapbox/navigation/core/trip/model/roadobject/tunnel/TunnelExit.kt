package com.mapbox.navigation.core.trip.model.roadobject.tunnel

import com.mapbox.navigation.base.trip.model.roadobject.RoadObject
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectGeometry
import com.mapbox.navigation.core.trip.model.roadobject.RoadObjectType

/**
 * Road object type that provides information about tunnels on the route.
 *
 * @param info tunnel information
 * @see RoadObject
 * @see RoadObjectType.TUNNEL_EXIT
 */
class TunnelExit private constructor(
    distanceFromStartOfRoute: Double?,
    objectGeometry: RoadObjectGeometry,
    val info: TunnelInfo?
) : RoadObject(
    RoadObjectType.TUNNEL_EXIT,
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
        .info(info)

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as TunnelExit

        if (info != other.info) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (info?.hashCode() ?: 0)
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "TunnelExitAlert(info=$info), ${super.toString()}"
    }

    /**
     * Use to create a new instance.
     *
     * @see TunnelExit
     */
    class Builder(
        private val objectGeometry: RoadObjectGeometry
    ) {

        private var distanceFromStartOfRoute: Double? = null
        private var info: TunnelInfo? = null

        /**
         * Add optional distance from start of route.
         * If [distanceFromStartOfRoute] is negative, `null` will be used.
         */
        fun distanceFromStartOfRoute(distanceFromStartOfRoute: Double?): Builder = apply {
            this.distanceFromStartOfRoute = distanceFromStartOfRoute
        }

        /**
         * Add the tunnel info.
         */
        fun info(info: TunnelInfo?): Builder = apply {
            this.info = info
        }

        /**
         * Build the object instance.
         */
        fun build() =
            TunnelExit(
                distanceFromStartOfRoute,
                objectGeometry,
                info
            )
    }
}
