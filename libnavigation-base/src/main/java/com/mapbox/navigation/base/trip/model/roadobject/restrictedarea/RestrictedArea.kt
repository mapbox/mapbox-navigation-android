package com.mapbox.navigation.base.trip.model.roadobject.restrictedarea

import com.mapbox.navigation.base.trip.model.roadobject.RoadObject
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectProvider
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectType

/**
 * Road object type that provides information about restricted areas on the road.
 *
 * @see RoadObject
 * @see RoadObjectType.RESTRICTED_AREA
 */
class RestrictedArea internal constructor(
    id: String,
    length: Double?,
    @RoadObjectProvider.Type provider: String,
    isUrban: Boolean?,
    nativeRoadObject: com.mapbox.navigator.RoadObject,
) : RoadObject(
    id,
    RoadObjectType.RESTRICTED_AREA,
    length,
    provider,
    isUrban,
    nativeRoadObject,
) {

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RestrictedArea() ${super.toString()}"
    }
}
