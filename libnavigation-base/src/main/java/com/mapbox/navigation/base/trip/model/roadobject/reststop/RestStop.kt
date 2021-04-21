package com.mapbox.navigation.base.trip.model.roadobject.reststop

import com.mapbox.navigation.base.trip.model.roadobject.RoadObject
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectProvider
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectType
import com.mapbox.navigation.base.trip.model.roadobject.location.RoadObjectLocation

/**
 * Road object type that provides information about rest stops on the road.
 *
 * @param restStopType information about a rest stop. See [RestStopType].
 * @see RoadObject
 * @see RoadObjectType.REST_STOP
 */
class RestStop internal constructor(
    id: String,
    @RestStopType.Type val restStopType: Int,
    length: Double?,
    location: RoadObjectLocation,
    @RoadObjectProvider.Type provider: String,
    nativeRoadObject: com.mapbox.navigator.RoadObject,
) : RoadObject(id, RoadObjectType.REST_STOP, length, location, provider, nativeRoadObject) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as RestStop

        if (restStopType != other.restStopType) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + restStopType
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RestStopAlert(restStopType=$restStopType), ${super.toString()}"
    }
}
