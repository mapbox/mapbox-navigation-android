package com.mapbox.navigation.base.trip.model.alert

import com.mapbox.geojson.Point

/**
 * Route alert type that provides information about country border crossings on the route.
 *
 * @param countryBorderCrossingInfo administrative info when crossing the country border
 * @see RouteAlert
 * @see RouteAlertType.CountryBorderCrossing
 */
class CountryBorderCrossingAlert private constructor(
    coordinate: Point,
    distance: Double,
    alertGeometry: RouteAlertGeometry?,
    val countryBorderCrossingInfo: CountryBorderCrossingInfo?,
) : RouteAlert(
    RouteAlertType.CountryBorderCrossing,
    coordinate,
    distance,
    alertGeometry
) {

    /**
     * Transform this object into a builder to mutate the values.
     */
    fun toBuilder(): Builder = Builder(coordinate, distance)
        .alertGeometry(alertGeometry)
        .countryBorderCrossingInfo(countryBorderCrossingInfo)

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as CountryBorderCrossingAlert

        if (countryBorderCrossingInfo != other.countryBorderCrossingInfo) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (countryBorderCrossingInfo?.hashCode() ?: 0)
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "CountryBorderCrossingAlert(" +
            "countryBorderCrossingInfo=$countryBorderCrossingInfo), ${super.toString()}"
    }

    /**
     * Use to create a new instance.
     *
     * @see CountryBorderCrossingAlert
     */
    class Builder(
        private val coordinate: Point,
        private val distance: Double
    ) {

        private var alertGeometry: RouteAlertGeometry? = null
        private var countryBorderCrossingInfo: CountryBorderCrossingInfo? = null

        /**
         * Add optional geometry if the alert has a length.
         */
        fun alertGeometry(alertGeometry: RouteAlertGeometry?): Builder = apply {
            this.alertGeometry = alertGeometry
        }

        /**
         * Add the administrative info when crossing the country border.
         */
        fun countryBorderCrossingInfo(borderCrossingInfo: CountryBorderCrossingInfo?): Builder =
            apply {
                this.countryBorderCrossingInfo = borderCrossingInfo
            }

        /**
         * Build the object instance.
         */
        fun build() =
            CountryBorderCrossingAlert(
                coordinate = coordinate,
                distance = distance,
                alertGeometry = alertGeometry,
                countryBorderCrossingInfo = countryBorderCrossingInfo
            )
    }
}
