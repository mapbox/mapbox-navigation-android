package com.mapbox.navigation.base.trip.model.roadobject.border

import com.mapbox.navigation.base.trip.model.roadobject.RoadObject
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectProvider
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectType
import com.mapbox.navigation.base.trip.model.roadobject.location.RoadObjectLocation

/**
 * Road object type that provides information about country border crossings on the route.
 *
 * @param countryBorderCrossingInfo administrative info when crossing the country border
 * @see RoadObject
 * @see RoadObjectType.COUNTRY_BORDER_CROSSING
 */
class CountryBorderCrossing internal constructor(
    id: String,
    val countryBorderCrossingInfo: CountryBorderCrossingInfo,
    length: Double?,
    location: RoadObjectLocation,
    @RoadObjectProvider.Type provider: String,
    nativeRoadObject: com.mapbox.navigator.RoadObject,
) : RoadObject(
    id,
    RoadObjectType.COUNTRY_BORDER_CROSSING,
    length,
    location,
    provider,
    nativeRoadObject
) {

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
        return "CountryBorderCrossing(" +
            "countryBorderCrossingInfo=$countryBorderCrossingInfo), ${super.toString()}"
    }
}
