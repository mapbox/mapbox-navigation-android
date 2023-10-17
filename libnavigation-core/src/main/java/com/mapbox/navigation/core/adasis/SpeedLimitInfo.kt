package com.mapbox.navigation.core.adasis

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.speed.model.SpeedUnit
import com.mapbox.navigator.SpeedLimitType
import com.mapbox.navigator.SpeedLimitUnit

/**
 * Speed limit info.
 *
 * @param value TODO
 * @param speedUnit TODO
 * @param type TODO
 * @param restriction TODO
 */
@ExperimentalPreviewMapboxNavigationAPI
class SpeedLimitInfo private constructor(
    val value: Byte,
    val speedUnit: SpeedUnit,
    val type: Type,
    val restriction: SpeedLimitRestriction,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SpeedLimitInfo

        if (value != other.value) return false
        if (speedUnit != other.speedUnit) return false
        if (type != other.type) return false
        if (restriction != other.restriction) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = value.toInt()
        result = 31 * result + speedUnit.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + restriction.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "SpeedLimitInfo(value=$value, speedUnit=$speedUnit, type=$type, restriction=$restriction)"
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
         * Type is unknown.
         */
        object Unknown : Type()

        internal companion object {

            @JvmSynthetic
            fun createFromNativeObject(nativeObj: SpeedLimitType): Type {
                return when (nativeObj) {
                    SpeedLimitType.IMPLICIT -> Implicit
                    SpeedLimitType.EXPLICIT -> Explicit
                    SpeedLimitType.UNKNOWN -> Unknown
                }
            }

        }
    }

    internal companion object {

        @JvmSynthetic
        fun createFromNativeObject(nativeObj: com.mapbox.navigator.SpeedLimitInfo): SpeedLimitInfo {
            val speedUnit = when (nativeObj.unit) {
                SpeedLimitUnit.KILOMETRES_PER_HOUR -> SpeedUnit.KILOMETERS_PER_HOUR
                SpeedLimitUnit.MILES_PER_HOUR -> SpeedUnit.MILES_PER_HOUR
            }
            return SpeedLimitInfo(
                value = nativeObj.value,
                speedUnit = speedUnit,
                type = Type.createFromNativeObject(nativeObj.type),
                restriction = SpeedLimitRestriction.createFromNativeObject(nativeObj.restriction)
            )
        }
    }
}
