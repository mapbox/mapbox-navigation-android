package com.mapbox.navigation.core.adas

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Road item.
 *
 * @param type the type of the road item, see [AdasRoadItemType]
 * @param location the location of the road item. See [AdasRoadItemLocation].
 * @param lanes the lanes of the road item, enabled only if location is [AdasRoadItemLocation.ABOVE_LANE]
 * @param value the value on the road sign. Enabled only for types [AdasRoadItemType.SPEED_LIMIT_SIGN],
 *  `AdasRoadItemType.RoadCam*`, [AdasRoadItemType.STEEP_ASCENT_SIGN], [AdasRoadItemType.STEEP_DESCENT_SIGN]
 */
@ExperimentalPreviewMapboxNavigationAPI
class AdasRoadItem private constructor(
    @AdasRoadItemType.Type val type: Int,
    @AdasRoadItemLocation.Location val location: Int?,
    val lanes: List<Byte>,
    val value: Int?,
) {

    internal companion object {

        @JvmSynthetic
        fun createFromNativeObject(nativeObj: com.mapbox.navigator.RoadItem): AdasRoadItem {
            return AdasRoadItem(
                AdasRoadItemType.createFromNativeObject(nativeObj.type),
                nativeObj.location?.let { AdasRoadItemLocation.createFromNativeObject(it) },
                nativeObj.lanes,
                nativeObj.value,
            )
        }
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AdasRoadItem

        if (type != other.type) return false
        if (value != other.value) return false
        if (location != other.location) return false
        if (lanes != other.lanes) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = type
        result = 31 * result + (value ?: 0)
        result = 31 * result + (location?.hashCode() ?: 0)
        result = 31 * result + lanes.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "AdasRoadItem(" +
            "type=$type, " +
            "location=$location, " +
            "lanes=$lanes, " +
            "value=$value" +
            ")"
    }
}
