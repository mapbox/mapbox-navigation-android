package com.mapbox.navigation.base.trip.model.roadobject.bridge

import com.mapbox.navigation.base.trip.model.roadobject.RoadObject
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectProvider
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectType
import com.mapbox.navigation.base.trip.model.roadobject.location.RoadObjectLocation

/**
 * Road object type that provides information about bridges on the route.
 *
 * @see RoadObject
 * @see RoadObjectType.BRIDGE
 */
class Bridge internal constructor(
    id: String,
    length: Double?,
    location: RoadObjectLocation,
    @RoadObjectProvider.Type provider: String,
    nativeRoadObject: com.mapbox.navigator.RoadObject,
) : RoadObject(id, RoadObjectType.BRIDGE, length, location, provider, nativeRoadObject) {

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "Bridge() ${super.toString()}"
    }
}
