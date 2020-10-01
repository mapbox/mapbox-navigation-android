package com.mapbox.navigation.base.trip.model.alert

import com.mapbox.geojson.Point

/**
 * Route alert type that provides information about tunnels on the route.
 *
 * @param info tunnel information
 * @see RouteAlert
 * @see RouteAlertType.TunnelEntrance
 */
class TunnelEntranceAlert private constructor(
    coordinate: Point,
    distance: Double,
    alertGeometry: RouteAlertGeometry?,
    val info: TunnelInfo?
) : RouteAlert(
    RouteAlertType.TunnelEntrance,
    coordinate,
    distance,
    alertGeometry
) {

    /**
     * Transform this object into a builder to mutate the values.
     */
    fun toBuilder(): Builder = Builder(coordinate, distance)
        .alertGeometry(alertGeometry)
        .info(info)

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as TunnelEntranceAlert

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
        return "TunnelEntranceAlert(info=$info), ${super.toString()}"
    }

    /**
     * Use to create a new instance.
     *
     * @see TunnelEntranceAlert
     */
    class Builder(
        private val coordinate: Point,
        private val distance: Double
    ) {

        private var alertGeometry: RouteAlertGeometry? = null
        private var info: TunnelInfo? = null

        /**
         * Add optional geometry if the alert has a length.
         */
        fun alertGeometry(alertGeometry: RouteAlertGeometry?): Builder = apply {
            this.alertGeometry = alertGeometry
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
            TunnelEntranceAlert(
                coordinate,
                distance,
                alertGeometry,
                info
            )
    }
}
