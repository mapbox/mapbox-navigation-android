package com.mapbox.navigation.base.trip.model.roadobject.custom

import com.mapbox.navigation.base.trip.model.roadobject.RoadObject
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectProvider
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectType

/**
 * Road object type that provides information about custom objects.
 *
 * @see RoadObject
 * @see RoadObjectType.CUSTOM
 */
class Custom internal constructor(
    id: String,
    length: Double?,
    @RoadObjectProvider.Type provider: String,
    isUrban: Boolean?,
    nativeRoadObject: com.mapbox.navigator.RoadObject,
) : RoadObject(id, RoadObjectType.CUSTOM, length, provider, isUrban, nativeRoadObject) {

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "Custom() ${super.toString()}"
    }
}
