package com.mapbox.navigation.base.trip.model.alert

import com.mapbox.geojson.Point

/**
 * Route alert type that provides information about toll collection points on the route.
 *
 * @param tollCollectionType information about a toll collection point. See [TollCollectionType].
 * @see RouteAlert
 * @see RouteAlertType.TollCollection
 */
class TollCollectionAlert private constructor(
    coordinate: Point,
    distance: Double,
    alertGeometry: RouteAlertGeometry?,
    val tollCollectionType: Int
) : RouteAlert(
    RouteAlertType.TollCollection,
    coordinate,
    distance,
    alertGeometry
) {

    /**
     * Transform this object into a builder to mutate the values.
     */
    fun toBuilder(): Builder = Builder(coordinate, distance)
        .alertGeometry(alertGeometry)
        .tollCollectionType(tollCollectionType)

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as TollCollectionAlert

        if (tollCollectionType != other.tollCollectionType) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + tollCollectionType
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "TollCollectionAlert(tollCollectionType=$tollCollectionType), ${super.toString()}"
    }

    /**
     * Use to create a new instance.
     *
     * @see TollCollectionAlert
     */
    class Builder(
        private val coordinate: Point,
        private val distance: Double
    ) {
        private var alertGeometry: RouteAlertGeometry? = null
        private var tollCollectionType: Int = TollCollectionType.Unknown

        /**
         * Add optional geometry if the alert has a length.
         */
        fun alertGeometry(alertGeometry: RouteAlertGeometry?): Builder = apply {
            this.alertGeometry = alertGeometry
        }

        /**
         * Sets information about a toll collection point. See [TollCollectionType].
         */
        fun tollCollectionType(tollCollectionType: Int): Builder = apply {
            this.tollCollectionType = tollCollectionType
        }

        /**
         * Build the object instance.
         */
        fun build() =
            TollCollectionAlert(
                coordinate,
                distance,
                alertGeometry,
                tollCollectionType
            )
    }
}
