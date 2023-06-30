package com.mapbox.navigation.base.trip.model.roadobject.merge

import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectType

/**
 * Details of RoadObject with type [RoadObjectType.MERGING_AREA].
 *
 * @param type Merging Area type, see [MergingAreaType] for possible values.
 */
internal class MergingAreaInfo internal constructor(
    @MergingAreaType.Type val type: String,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MergingAreaInfo

        if (type != other.type) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        return type.hashCode()
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "MergingAreaInfo(type='$type')"
    }
}
