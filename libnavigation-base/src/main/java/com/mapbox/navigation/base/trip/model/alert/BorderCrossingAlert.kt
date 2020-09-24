package com.mapbox.navigation.base.trip.model.alert

import com.mapbox.geojson.Point

/**
 * Route alert type that provides information about border crossings on the route.
 *
 * @see RouteAlert
 * @see RouteAlertType.BorderCrossing
 */
class BorderCrossingAlert private constructor(
    metadata: Metadata,
    coordinate: Point,
    distance: Double,
    alertGeometry: RouteAlertGeometry?
) : RouteAlert<BorderCrossingAlert.Metadata>(
    RouteAlertType.BorderCrossing,
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
        return "BorderCrossingAlert() ${super.toString()}"
    }

    /**
     * Use to create a new instance.
     *
     * @see BorderCrossingAlert
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
            BorderCrossingAlert(
                metadata,
                coordinate,
                distance,
                alertGeometry
            )
    }

    /**
     * Metadata specific to this alert.
     *
     * @param from origin administrative info when crossing the border
     * @param to destination administrative info when crossing the border
     */
    class Metadata private constructor(
        val from: BorderCrossingAdminInfo?,
        val to: BorderCrossingAdminInfo?,
    ) {

        /**
         * Transform this object into a builder to mutate the values.
         */
        fun toBuilder(): Builder = Builder().from(from).to(to)

        /**
         * Indicates whether some other object is "equal to" this one.
         */
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Metadata

            if (from != other.from) return false
            if (to != other.to) return false

            return true
        }

        /**
         * Returns a hash code value for the object.
         */
        override fun hashCode(): Int {
            var result = from.hashCode()
            result = 31 * result + to.hashCode()
            return result
        }

        /**
         * Returns a string representation of the object.
         */
        override fun toString(): String {
            return "Metadata(from=$from, to=$to)"
        }

        /**
         * Use to create a new instance.
         *
         * @see Metadata
         */
        class Builder {
            private var from: BorderCrossingAdminInfo? = null
            private var to: BorderCrossingAdminInfo? = null

            /**
             * Add the origin administrative info when crossing the border.
             */
            fun from(from: BorderCrossingAdminInfo?): Builder = apply {
                this.from = from
            }

            /**
             * Add the destination administrative info when crossing the border.
             */
            fun to(to: BorderCrossingAdminInfo?): Builder = apply {
                this.to = to
            }

            /**
             * Build the object instance.
             */
            fun build() = Metadata(from = from, to = to)
        }
    }
}
