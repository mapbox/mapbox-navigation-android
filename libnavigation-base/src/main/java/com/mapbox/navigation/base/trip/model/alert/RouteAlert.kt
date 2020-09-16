package com.mapbox.navigation.base.trip.model.alert

import com.mapbox.geojson.Point

/**
 * Abstract class that serves as a base for all route alerts.
 *
 * @param type type constant describing the alert, especially useful for Java integration.
 * Kotlin integration can be based on inheritance types.
 * @param metadata type-safe metadata of each of the event.
 * @param coordinate location of the alert or its start point in case it has a length
 * @param distance distance to this alert since the start of the route
 * @param alertGeometry optional geometry details of the alert if it has a length
 */
sealed class RouteAlert<Metadata>(
    val type: RouteAlertType,
    val metadata: Metadata,
    val coordinate: Point,
    val distance: Double,
    val alertGeometry: RouteAlertGeometry?
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RouteAlert<*>

        if (type != other.type) return false
        if (metadata != other.metadata) return false
        if (coordinate != other.coordinate) return false
        if (distance != other.distance) return false
        if (alertGeometry != other.alertGeometry) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + (metadata?.hashCode() ?: 0)
        result = 31 * result + coordinate.hashCode()
        result = 31 * result + distance.hashCode()
        result = 31 * result + (alertGeometry?.hashCode() ?: 0)
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RouteAlert(type=$type, " +
            "metadata=$metadata, " +
            "coordinate=$coordinate, " +
            "distance=$distance, " +
            "alertGeometry=$alertGeometry)"
    }
}

/**
 * Route alert type that provides an information about tunnels on the route.
 *
 * @see RouteAlert
 */
class TunnelEntranceAlert private constructor(
    metadata: Metadata,
    coordinate: Point,
    distance: Double,
    alertGeometry: RouteAlertGeometry?
) : RouteAlert<TunnelEntranceAlert.Metadata>(
    RouteAlertType.TunnelEntrance,
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
        return "TunnelEntranceAlert() ${super.toString()}"
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false
        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        return super.hashCode()
    }

    /**
     * Use to create a new instance.
     *
     * @see TunnelEntranceAlert
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
        fun build() = TunnelEntranceAlert(metadata, coordinate, distance, alertGeometry)
    }

    /**
     * Metadata specific to this alert.
     *
     * @param tunnelName name of the tunnel if available.
     */
    class Metadata private constructor(
        val tunnelName: String?
    ) {

        /**
         * Transform this object into a builder to mutate the values.
         */
        fun toBuilder(): Builder = Builder().tunnelName(tunnelName)

        /**
         * Use to create a new instance.
         *
         * @see TunnelEntranceAlert.Metadata
         */
        class Builder {
            private var tunnelName: String? = null

            /**
             * Add optional tunnel name if available.
             */
            fun tunnelName(tunnelName: String?): Builder = apply { this.tunnelName = tunnelName }

            /**
             * Build the object instance.
             */
            fun build() = Metadata(tunnelName)
        }

        /**
         * Indicates whether some other object is "equal to" this one.
         */
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Metadata

            if (tunnelName != other.tunnelName) return false

            return true
        }

        /**
         * Returns a hash code value for the object.
         */
        override fun hashCode(): Int {
            return tunnelName?.hashCode() ?: 0
        }

        /**
         * Returns a string representation of the object.
         */
        override fun toString(): String {
            return "Metadata(tunnelName=$tunnelName)"
        }
    }
}
