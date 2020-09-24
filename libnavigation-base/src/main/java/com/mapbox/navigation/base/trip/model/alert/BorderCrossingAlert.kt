package com.mapbox.navigation.base.trip.model.alert

import com.mapbox.geojson.Point

/**
 * Route alert type that provides information about border crossings on the route.
 *
 * @param from origin administrative info when crossing the border
 * @param to destination administrative info when crossing the border
 * @see RouteAlert
 * @see RouteAlertType.BorderCrossing
 */
class BorderCrossingAlert private constructor(
    coordinate: Point,
    distance: Double,
    alertGeometry: RouteAlertGeometry?,
    val from: BorderCrossingAdminInfo?,
    val to: BorderCrossingAdminInfo?
) : RouteAlert(
    RouteAlertType.BorderCrossing,
    coordinate,
    distance,
    alertGeometry
) {

    /**
     * Transform this object into a builder to mutate the values.
     */
    fun toBuilder(): Builder = Builder(coordinate, distance)
        .alertGeometry(alertGeometry)
        .from(from)
        .to(to)

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as BorderCrossingAlert

        if (from != other.from) return false
        if (to != other.to) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (from?.hashCode() ?: 0)
        result = 31 * result + (to?.hashCode() ?: 0)
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "BorderCrossingAlert(from=$from, to=$to), ${super.toString()}"
    }

    /**
     * Use to create a new instance.
     *
     * @see BorderCrossingAlert
     */
    class Builder(
        private val coordinate: Point,
        private val distance: Double
    ) {
        private var alertGeometry: RouteAlertGeometry? = null
        private var from: BorderCrossingAdminInfo? = null
        private var to: BorderCrossingAdminInfo? = null

        /**
         * Add optional geometry if the alert has a length.
         */
        fun alertGeometry(alertGeometry: RouteAlertGeometry?): Builder = apply {
            this.alertGeometry = alertGeometry
        }

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
        fun build() =
            BorderCrossingAlert(
                coordinate = coordinate,
                distance = distance,
                alertGeometry = alertGeometry,
                from = from,
                to = to
            )
    }
}
