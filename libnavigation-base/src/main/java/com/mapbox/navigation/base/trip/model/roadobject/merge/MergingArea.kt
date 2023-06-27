package com.mapbox.navigation.base.trip.model.roadobject.merge

import com.mapbox.navigation.base.trip.model.roadobject.RoadObject
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectProvider
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectType

/**
 * Road object type that provides information about merging areas on the road.
 *
 * @param info Merging Area specific details, see [MergingAreaInfo].
 * @see RoadObject
 * @see RoadObjectType.MERGING_AREA
 */
class MergingArea internal constructor(
    id: String,
    val info: MergingAreaInfo,
    length: Double?,
    @RoadObjectProvider.Type provider: String,
    isUrban: Boolean?,
    nativeRoadObject: com.mapbox.navigator.RoadObject,
) : RoadObject(
    id,
    RoadObjectType.MERGING_AREA,
    length,
    provider,
    isUrban,
    nativeRoadObject
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as MergingArea

        if (info != other.info) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + info.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "MergingArea(info=$info) ${super.toString()}"
    }
}
