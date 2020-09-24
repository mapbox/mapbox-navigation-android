package com.mapbox.navigation.base.trip.model.alert

import com.mapbox.geojson.Point

/**
 * Route alert type that provides information about toll collection points on the route.
 *
 * @see RouteAlert
 * @see RouteAlertType.TollCollection
 */
class TollCollectionAlert private constructor(
    metadata: Metadata,
    coordinate: Point,
    distance: Double,
    alertGeometry: RouteAlertGeometry?
) : RouteAlert<TollCollectionAlert.Metadata>(
    RouteAlertType.TollCollection,
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
        return "TollCollectionAlert() ${super.toString()}"
    }

    /**
     * Use to create a new instance.
     *
     * @see TollCollectionAlert
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
            TollCollectionAlert(
                metadata,
                coordinate,
                distance,
                alertGeometry
            )
    }

    /**
     * Metadata specific to this alert.
     *
     * @param type information about a toll collection point. See [TollCollectionType].
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
            private var type: Int = TollCollectionType.Unknown

            /**
             * Sets information about a toll collection point. See [TollCollectionType].
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
