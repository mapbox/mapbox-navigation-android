package com.mapbox.navigation.core.adasis

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.speed.model.SpeedUnit
import com.mapbox.navigator.SpeedLimitType
import com.mapbox.navigator.SpeedLimitUnit

/**
 * Speed limit info.
 *
 * @param value the numerical value of the limit
 * @param speedUnit the unit the value is specified in
 * @param type speed limit type, see [Type]
 * @param restriction speed limit restriction, see [AdasisSpeedLimitRestriction]
 */
@ExperimentalPreviewMapboxNavigationAPI
class AdasisSpeedLimitInfo private constructor(
    val value: Int,
    val speedUnit: SpeedUnit,
    val type: Type,
    val restriction: AdasisSpeedLimitRestriction,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AdasisSpeedLimitInfo

        if (value != other.value) return false
        if (speedUnit != other.speedUnit) return false
        if (type != other.type) return false
        return restriction == other.restriction
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = value
        result = 31 * result + speedUnit.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + restriction.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "SpeedLimitInfo(" +
            "value=$value, " +
            "speedUnit=$speedUnit, " +
            "type=$type, " +
            "restriction=$restriction" +
            ")"
    }

    /**
     * Speed limit type.
     */
    abstract class Type private constructor() {

        /**
         * Implicit type.
         */
        object Implicit : Type()

        /**
         * Explicit type.
         */
        object Explicit : Type()

        /**
         * Edge does not start the way, no sign on the edge.
         * Speed limit time is the same of on previous edge.
         */
        object Prolonged : Type()

        /**
         * Type is unknown.
         */
        object Unknown : Type()

        internal companion object {

            @JvmSynthetic
            fun createFromNativeObject(nativeObj: SpeedLimitType): Type {
                return when (nativeObj) {
                    SpeedLimitType.IMPLICIT -> Implicit
                    SpeedLimitType.EXPLICIT -> Explicit
                    SpeedLimitType.PROLONGED -> Prolonged
                    SpeedLimitType.UNKNOWN -> Unknown
                }
            }
        }
    }

    internal companion object {

        @JvmSynthetic
        fun createFromNativeObject(nativeObj: com.mapbox.navigator.SpeedLimitInfo): AdasisSpeedLimitInfo {
            val speedUnit = when (nativeObj.unit) {
                SpeedLimitUnit.KILOMETRES_PER_HOUR -> SpeedUnit.KILOMETERS_PER_HOUR
                SpeedLimitUnit.MILES_PER_HOUR -> SpeedUnit.MILES_PER_HOUR
            }
            return AdasisSpeedLimitInfo(
                value = nativeObj.value,
                speedUnit = speedUnit,
                type = Type.createFromNativeObject(nativeObj.type),
                restriction = AdasisSpeedLimitRestriction.createFromNativeObject(nativeObj.restriction)
            )
        }
    }
}
