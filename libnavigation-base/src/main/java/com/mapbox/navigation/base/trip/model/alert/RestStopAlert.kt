package com.mapbox.navigation.base.trip.model.alert

import com.mapbox.geojson.Point

/**
 * Route alert type that provides information about rest stops on the route.
 *
 * @see RouteAlert
 * @see RouteAlertType.RestStop
 */
class RestStopAlert private constructor(
    metadata: Metadata,
    coordinate: Point,
    distance: Double,
    alertGeometry: RouteAlertGeometry?
) : RouteAlert<RestStopAlert.Metadata>(
    RouteAlertType.RestStop,
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
        return "RestStopAlert() ${super.toString()}"
    }

    /**
     * Use to create a new instance.
     *
     * @see RestStopAlert
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
        fun build() = RestStopAlert(metadata, coordinate, distance, alertGeometry)
    }

    /**
     * Metadata specific to this alert.
     *
     * @param type information about a rest stop. See [RestStopType].
     */
    class Metadata private constructor(val type: Int) {

        /**
         * Transform this object into a builder to mutate the values.
         */
        fun toBuilder(): Builder = Builder().type(type)

        /**
         * Indicates whether some other object is "equal to" this one.
         */
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Metadata

            if (type != other.type) return false

            return true
        }

        /**
         * Returns a hash code value for the object.
         */
        override fun hashCode(): Int {
            return type
        }

        /**
         * Returns a string representation of the object.
         */
        override fun toString(): String {
            return "Metadata(type=$type)"
        }

        /**
         * Use to create a new instance.
         *
         * @see Metadata
         */
        class Builder {
            private var type: Int = RestStopType.Unknown

            /**
             * Sets information about a rest stop. See [RestStopType].
             */
            fun type(type: Int): Builder = apply {
                this.type = type
            }

            /**
             * Build the object instance.
             */
            fun build() = Metadata(type)
        }
    }
}
