package com.mapbox.navigation.base.trip.model.alert

import com.mapbox.geojson.Point

/**
 * Route alert type that provides information about tunnels on the route.
 *
 * @see RouteAlert
 * @see RouteAlertType.TunnelEntrance
 */
class TunnelEntranceAlert private constructor(
    coordinate: Point,
    distance: Double,
    alertGeometry: RouteAlertGeometry?
) : RouteAlert(
    RouteAlertType.TunnelEntrance,
    coordinate,
    distance,
    alertGeometry
) {

    /**
     * Transform this object into a builder to mutate the values.
     */
    fun toBuilder(): Builder = Builder(coordinate, distance).alertGeometry(alertGeometry)

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "TunnelEntranceAlert() ${super.toString()}"
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

        /**
         * Add optional geometry if the alert has a length.
         */
        fun alertGeometry(alertGeometry: RouteAlertGeometry?): Builder = apply {
            this.alertGeometry = alertGeometry
        }

        /**
         * Build the object instance.
         */
        fun build() =
            TunnelEntranceAlert(
                coordinate,
                distance,
                alertGeometry
            )
    }
}
