package com.mapbox.navigation.base.trip.model.roadobject.notification

import com.mapbox.navigation.base.trip.model.roadobject.RoadObject
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectProvider
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectType

/**
 * Road object type that provides information about notification on the route.
 *
 * @see RoadObject
 * @see RoadObjectType.NOTIFICATION
 */
internal class Notification internal constructor(
    id: String,
    length: Double?,
    @RoadObjectProvider.Type provider: String,
    isUrban: Boolean?,
    nativeRoadObject: com.mapbox.navigator.RoadObject,
) : RoadObject(
    id,
    RoadObjectType.NOTIFICATION,
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

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        return super.hashCode()
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "Notification"
    }
}
