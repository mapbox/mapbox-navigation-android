package com.mapbox.navigation.base.trip.model.roadobject.jct

import com.mapbox.navigation.base.trip.model.roadobject.LocalizedString
import com.mapbox.navigation.base.trip.model.roadobject.RoadObject
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectProvider
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectType

/**
 * Road object type that provides information about junctions on the route.
 *
 * @see RoadObject
 * @see RoadObjectType.JCT
 *
 * @param name localized name of the junction
 */
class Junction internal constructor(
    id: String,
    val name: List<LocalizedString>,
    length: Double?,
    @RoadObjectProvider.Type provider: String,
    isUrban: Boolean?,
    nativeRoadObject: com.mapbox.navigator.RoadObject,
) : RoadObject(
    id,
    RoadObjectType.JCT,
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

        other as Junction

        if (name != other.name) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        return name.hashCode()
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "Junction(name='$name')"
    }
}
