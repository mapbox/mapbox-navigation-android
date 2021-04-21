package com.mapbox.navigation.base.trip.model.roadobject.tollcollection

import com.mapbox.navigation.base.trip.model.roadobject.RoadObject
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectProvider
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectType
import com.mapbox.navigation.base.trip.model.roadobject.location.RoadObjectLocation

/**
 * Road object type that provides information about toll collection points on the route.
 *
 * @param tollCollectionType information about a toll collection point. See [TollCollectionType].
 * @see RoadObject
 * @see RoadObjectType.TOLL_COLLECTION
 */
class TollCollection internal constructor(
    id: String,
    @TollCollectionType.Type val tollCollectionType: Int,
    length: Double?,
    location: RoadObjectLocation,
    @RoadObjectProvider.Type provider: String,
    nativeRoadObject: com.mapbox.navigator.RoadObject,
) : RoadObject(
    id,
    RoadObjectType.TOLL_COLLECTION,
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

        other as TollCollection

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
}
