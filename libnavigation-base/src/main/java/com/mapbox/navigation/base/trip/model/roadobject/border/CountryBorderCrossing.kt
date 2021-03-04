package com.mapbox.navigation.base.trip.model.roadobject.border

import com.mapbox.navigation.base.trip.model.roadobject.RoadObject
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectGeometry
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectType

/**
 * Road object type that provides information about country border crossings on the route.
 *
 * @param countryBorderCrossingInfo administrative info when crossing the country border
 * @see RoadObject
 * @see RoadObjectType.COUNTRY_BORDER_CROSSING
 */
class CountryBorderCrossing private constructor(
    distanceFromStartOfRoute: Double?,
    objectGeometry: RoadObjectGeometry,
    val countryBorderCrossingInfo: CountryBorderCrossingInfo?,
) : RoadObject(
    RoadObjectType.COUNTRY_BORDER_CROSSING,
    distanceFromStartOfRoute,
    objectGeometry
) {

    /**
     * Transform this object into a builder to mutate the values.
     */
    fun toBuilder(): Builder = Builder(
        objectGeometry
    )
        .distanceFromStartOfRoute(distanceFromStartOfRoute)
        .countryBorderCrossingInfo(countryBorderCrossingInfo)

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as CountryBorderCrossing

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
     * @see CountryBorderCrossing
     */
    class Builder(
        private val objectGeometry: RoadObjectGeometry
    ) {
        private var distanceFromStartOfRoute: Double? = null
        private var countryBorderCrossingInfo: CountryBorderCrossingInfo? = null

        /**
         * Add optional distance from start of route.
         * If [distanceFromStartOfRoute] is negative, `null` will be used.
         */
        fun distanceFromStartOfRoute(distanceFromStartOfRoute: Double?): Builder = apply {
            this.distanceFromStartOfRoute = distanceFromStartOfRoute
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
            CountryBorderCrossing(
                distanceFromStartOfRoute,
                objectGeometry,
                countryBorderCrossingInfo
            )
    }
}
