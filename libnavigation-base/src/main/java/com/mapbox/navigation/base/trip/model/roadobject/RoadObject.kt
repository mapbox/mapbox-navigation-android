package com.mapbox.navigation.base.trip.model.roadobject

import com.mapbox.navigation.base.trip.model.eh.mapToRoadObjectLocation
import com.mapbox.navigation.base.trip.model.roadobject.location.RoadObjectLocation
import com.mapbox.navigation.base.trip.model.roadobject.location.RoadObjectLocationType

/**
 * Abstract class that serves as a base for all road objects.
 * There are two sources of road objects: active route and the electronic horizon.
 * Objects coming from different sources might be duplicated and they will not have the same IDs.
 *
 * @param id id of the road object. If we get the same objects (e.g. [RoadObjectType.TUNNEL]) from
 * the electronic horizon and the active route, they will not have the same IDs.
 * @param objectType constant describing the object type, see [RoadObjectType].
 * Available road object types are:
 * - [RoadObjectType.TUNNEL]
 * - [RoadObjectType.COUNTRY_BORDER_CROSSING]
 * - [RoadObjectType.TOLL_COLLECTION]
 * - [RoadObjectType.REST_STOP]
 * - [RoadObjectType.RESTRICTED_AREA]
 * - [RoadObjectType.BRIDGE]
 * - [RoadObjectType.INCIDENT]
 * - [RoadObjectType.CUSTOM]
 * - [RoadObjectType.IC]
 * - [RoadObjectType.JCT]
 *
 * @param length length of the object, null if the object is point-like.
 * @param provider provider of the road object
 * @param isUrban **true** whenever [RoadObject] is in urban area, **false** otherwise. **null** if
 * road object cannot be defined if one is in urban or not (most probably is in both at the same time)
 */
abstract class RoadObject internal constructor(
    val id: String,
    val objectType: Int,
    val length: Double?,
    val provider: String,
    val isUrban: Boolean?,
    val nativeRoadObject: com.mapbox.navigator.RoadObject,
) {
    /**
     * Location of the road object.
     *
     * Road objects coming from the electronic horizon might have the next [RoadObjectLocationType]:
     * - [RoadObjectLocationType.GANTRY]
     * - [RoadObjectLocationType.OPEN_LR_LINE]
     * - [RoadObjectLocationType.OPEN_LR_POINT]
     * - [RoadObjectLocationType.POINT]
     * - [RoadObjectLocationType.POLYGON]
     * - [RoadObjectLocationType.POLYLINE]
     * - [RoadObjectLocationType.POLYLINE]
     *
     * Road objects coming from the active route will have only [RoadObjectLocationType]:
     * - [RoadObjectLocationType.ROUTE_ALERT]
     */
    val location: RoadObjectLocation by lazy {
        nativeRoadObject.location.mapToRoadObjectLocation()
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RoadObject

        if (id != other.id) return false
        if (objectType != other.objectType) return false
        if (length != other.length) return false
        if (location != other.location) return false
        if (provider != other.provider) return false
        if (nativeRoadObject != other.nativeRoadObject) return false
        if (isUrban != other.isUrban) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + objectType
        result = 31 * result + length.hashCode()
        result = 31 * result + location.hashCode()
        result = 31 * result + provider.hashCode()
        result = 31 * result + nativeRoadObject.hashCode()
        result = 31 * result + isUrban.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RoadObject(" +
            "id='$id', " +
            "objectType=$objectType, " +
            "length=$length, " +
            "location=$location, " +
            "provider=$provider, " +
            "isUrban=$isUrban" +
            ")"
    }
}
