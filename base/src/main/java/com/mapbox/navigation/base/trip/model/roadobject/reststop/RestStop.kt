package com.mapbox.navigation.base.trip.model.roadobject.reststop

import com.mapbox.navigation.base.trip.model.roadobject.RoadObject
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectProvider
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectType

/**
 * Road object type that provides information about rest stops on the road.
 *
 * @param restStopType information about a rest stop. See [RestStopType].
 * @param name name of the rest stop.
 * @param amenities facilities associated with the rest stop. See [AmenityType]
 * @param guideMapUri URI to guide map image. To be able to access the resource, you need to
 * append an `access_token=` query parameter. Data availability may vary.
 * @see RoadObject
 * @see RoadObjectType.REST_STOP
 */
class RestStop internal constructor(
    id: String,
    @RestStopType.Type val restStopType: Int,
    val name: String?,
    val amenities: List<Amenity>?,
    val guideMapUri: String?,
    length: Double?,
    @RoadObjectProvider.Type provider: String,
    isUrban: Boolean?,
    nativeRoadObject: com.mapbox.navigator.RoadObject,
) : RoadObject(
    id,
    RoadObjectType.REST_STOP,
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

        other as RestStop

        if (restStopType != other.restStopType) return false
        if (name != other.name) return false
        if (amenities != other.amenities) return false
        if (guideMapUri != other.guideMapUri) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + restStopType
        result = 31 * result + name.hashCode()
        result = 31 * result + amenities.hashCode()
        result = 31 * result + guideMapUri.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RestStop(" +
            "restStopType=$restStopType, " +
            "amenities=$amenities, " +
            "name=$name, " +
            "guideMapUri=$guideMapUri" +
            "), ${super.toString()}"
    }
}
