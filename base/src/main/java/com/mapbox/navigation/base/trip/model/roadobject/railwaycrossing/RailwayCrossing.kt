package com.mapbox.navigation.base.trip.model.roadobject.railwaycrossing

import com.mapbox.navigation.base.trip.model.roadobject.RoadObject
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectProvider
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectType

/**
 * Road object type that provides information about railway crossing on the route.
 *
 * @param info railway crossing information
 * @see RoadObject
 * @see RoadObjectType.RAILWAY_CROSSING
 */
class RailwayCrossing internal constructor(
    id: String,
    val info: RailwayCrossingInfo,
    length: Double?,
    @RoadObjectProvider.Type provider: String,
    isUrban: Boolean?,
    nativeRoadObject: com.mapbox.navigator.RoadObject,
) : RoadObject(
    id,
    RoadObjectType.RAILWAY_CROSSING,
    length,
    provider,
    isUrban,
    nativeRoadObject,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as RailwayCrossing

        if (info != other.info) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (info.hashCode())
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RailwayCrossing(info=$info), ${super.toString()})"
    }
}
