package com.mapbox.navigation.base.trip.model.alert

import com.mapbox.geojson.Point

/**
 * Route alert type that provides information about restricted areas on the route.
 *
 * @see RouteAlert
 * @see RouteAlertType.RestrictedArea
 */
class RestrictedAreaAlert private constructor(
    metadata: Metadata,
    coordinate: Point,
    distance: Double,
    alertGeometry: RouteAlertGeometry?
) : RouteAlert<RestrictedAreaAlert.Metadata>(
    RouteAlertType.RestrictedArea,
    metadata,
    coordinate,
    distance,
    alertGeometry
) {

    /**
     * Transform this object into a builder to mutate the values.
     */
    fun toBuilder(): Builder = Builder(metadata, coordinate, distance).alertGeometry(alertGeometry)

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
        private val metadata: Metadata,
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
                metadata,
                coordinate,
                distance,
                alertGeometry
            )
    }

    /**
     * Metadata specific to this alert. Currently contains no additional data.
     */
    class Metadata private constructor() {

        /**
         * Transform this object into a builder to mutate the values.
         */
        fun toBuilder(): Builder = Builder()

        /**
         * Indicates whether some other object is "equal to" this one.
         */
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            return true
        }

        /**
         * Returns a hash code value for the object.
         */
        override fun hashCode(): Int {
            return javaClass.hashCode()
        }

        /**
         * Returns a string representation of the object.
         */
        override fun toString(): String {
            return "Metadata()"
        }

        /**
         * Use to create a new instance.
         *
         * @see Metadata
         */
        class Builder {

            /**
             * Build the object instance.
             */
            fun build() = Metadata()
        }
    }
}
