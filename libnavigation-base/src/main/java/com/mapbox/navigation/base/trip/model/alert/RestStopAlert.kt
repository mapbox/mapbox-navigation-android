package com.mapbox.navigation.base.trip.model.alert

import com.mapbox.geojson.Point

/**
 * Route alert type that provides information about rest stops on the route.
 *
 * @param restStopType information about a rest stop. See [RestStopType].
 * @see RouteAlert
 * @see RouteAlertType.RestStop
 */
class RestStopAlert private constructor(
    coordinate: Point,
    distance: Double,
    alertGeometry: RouteAlertGeometry?,
    val restStopType: Int
) : RouteAlert(
    RouteAlertType.RestStop,
    coordinate,
    distance,
    alertGeometry
) {

    /**
     * Transform this object into a builder to mutate the values.
     */
    fun toBuilder(): Builder = Builder(coordinate, distance)
        .alertGeometry(alertGeometry)
        .restStopType(restStopType)

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as RestStopAlert

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
     * @see RestStopAlert
     */
    class Builder(
        private val coordinate: Point,
        private val distance: Double
    ) {
        private var alertGeometry: RouteAlertGeometry? = null
        private var restStopType: Int = RestStopType.Unknown

        /**
         * Add optional geometry if the alert has a length.
         */
        fun alertGeometry(alertGeometry: RouteAlertGeometry?): Builder = apply {
            this.alertGeometry = alertGeometry
        }

        /**
         * Sets information about a rest stop. See [RestStopType].
         */
        fun restStopType(restStopType: Int): Builder = apply {
            this.restStopType = restStopType
        }

        /**
         * Build the object instance.
         */
        fun build() = RestStopAlert(coordinate, distance, alertGeometry, restStopType)
    }
}
