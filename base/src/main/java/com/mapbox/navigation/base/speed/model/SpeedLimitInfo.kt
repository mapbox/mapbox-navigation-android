package com.mapbox.navigation.base.speed.model

/**
 * Object that holds speed limit properties.
 *
 * @param speed speed limit in units specified in `unit` field.
 * @param unit unit in which speed is specified, see [SpeedUnit].
 * @param sign see [SpeedLimitSign].
 */
class SpeedLimitInfo internal constructor(
    val speed: Int?,
    val unit: SpeedUnit,
    val sign: SpeedLimitSign,
) {

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "SpeedLimitInfo(speed=$speed, unit=$unit, sign=$sign)"
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SpeedLimitInfo

        if (speed != other.speed) return false
        if (unit != other.unit) return false
        if (sign != other.sign) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = speed ?: 0
        result = 31 * result + unit.hashCode()
        result = 31 * result + sign.hashCode()
        return result
    }
}
