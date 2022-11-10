package com.mapbox.navigation.base.trip.model.roadobject.bridge

import com.mapbox.navigation.base.trip.model.roadobject.RoadObject
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectProvider
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectType

/**
 * Road object type that provides information about bridges on the route.
 *
 * @see RoadObject
 * @see RoadObjectType.BRIDGE
 */
class Bridge internal constructor(
    id: String,
    length: Double?,
    @RoadObjectProvider.Type provider: String,
    isUrban: Boolean?,
    nativeRoadObject: com.mapbox.navigator.RoadObject,
) : RoadObject(id, RoadObjectType.BRIDGE, length, provider, isUrban, nativeRoadObject) {

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "Bridge() ${super.toString()}"
    }
}
