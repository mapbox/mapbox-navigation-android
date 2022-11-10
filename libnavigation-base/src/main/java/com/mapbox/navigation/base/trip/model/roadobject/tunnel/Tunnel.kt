package com.mapbox.navigation.base.trip.model.roadobject.tunnel

import com.mapbox.navigation.base.trip.model.roadobject.RoadObject
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectProvider
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectType

/**
 * Road object type that provides information about tunnels on the route.
 *
 * @param info tunnel information
 * @see RoadObject
 * @see RoadObjectType.TUNNEL
 */
class Tunnel internal constructor(
    id: String,
    val info: TunnelInfo,
    length: Double?,
    @RoadObjectProvider.Type provider: String,
    isUrban: Boolean?,
    nativeRoadObject: com.mapbox.navigator.RoadObject,
) : RoadObject(id, RoadObjectType.TUNNEL, length, provider, isUrban, nativeRoadObject) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as Tunnel

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
        return "Tunnel(info=$info), ${super.toString()}"
    }
}
