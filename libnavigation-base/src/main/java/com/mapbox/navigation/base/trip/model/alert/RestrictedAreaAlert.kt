package com.mapbox.navigation.base.trip.model.alert

import com.mapbox.geojson.Point

/**
 * Route alert type that provides information about restricted areas on the route.
 *
 * @see RouteAlert
 * @see RouteAlertType.RestrictedArea
 */
class RestrictedAreaAlert private constructor(
    coordinate: Point,
    distance: Double,
    alertGeometry: RouteAlertGeometry?
) : RouteAlert(
    RouteAlertType.RestrictedArea,
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
        return "RestrictedAreaAlert() ${super.toString()}"
    }

    /**
     * Use to create a new instance.
     *
     * @see RestrictedAreaAlert
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
            RestrictedAreaAlert(
                coordinate,
                distance,
                alertGeometry
            )
    }
}
