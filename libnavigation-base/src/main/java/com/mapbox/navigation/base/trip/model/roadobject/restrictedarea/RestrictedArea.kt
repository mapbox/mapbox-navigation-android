package com.mapbox.navigation.base.trip.model.roadobject.restrictedarea

import com.mapbox.navigation.base.trip.model.roadobject.RoadObject
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectProvider
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectType
import com.mapbox.navigation.base.trip.model.roadobject.location.RoadObjectLocation

/**
 * Road object type that provides information about restricted areas on the road.
 *
 * @see RoadObject
 * @see RoadObjectType.RESTRICTED_AREA
 */
class RestrictedArea internal constructor(
    id: String,
    length: Double?,
    location: RoadObjectLocation,
    @RoadObjectProvider.Type provider: String,
    nativeRoadObject: com.mapbox.navigator.RoadObject,
) : RoadObject(
    id,
    RoadObjectType.RESTRICTED_AREA,
    length,
    location,
    provider,
    nativeRoadObject
) {

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RestrictedArea() ${super.toString()}"
    }
}
