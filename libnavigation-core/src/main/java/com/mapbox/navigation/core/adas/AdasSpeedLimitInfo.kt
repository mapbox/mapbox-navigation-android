package com.mapbox.navigation.core.adas

import androidx.annotation.IntDef
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.speed.model.SpeedUnit
import com.mapbox.navigator.SpeedLimitUnit

/**
 * Speed limit info.
 *
 * @param value the numerical value of the limit
 * @param speedUnit the unit the value is specified in
 * @param speedLimitType speed limit type, see [SpeedLimitType]
 * @param restriction speed limit restriction, see [AdasSpeedLimitRestriction]
 */
@ExperimentalPreviewMapboxNavigationAPI
class AdasSpeedLimitInfo private constructor(
    val value: Int,
    val speedUnit: SpeedUnit,
    @SpeedLimitType.Type val speedLimitType: Int,
    val restriction: AdasSpeedLimitRestriction,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AdasSpeedLimitInfo

        if (value != other.value) return false
        if (speedUnit != other.speedUnit) return false
        if (speedLimitType != other.speedLimitType) return false
        return restriction == other.restriction
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = value
        result = 31 * result + speedUnit.hashCode()
        result = 31 * result + speedLimitType.hashCode()
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
            "speedLimitInfoType=$speedLimitType, " +
            "restriction=$restriction" +
            ")"
    }

    /**
     * Speed limit type.
     */
    object SpeedLimitType {

        /**
         * Implicit type.
         */
        const val IMPLICIT = 0

        /**
         * Explicit type.
         */
        const val EXPLICIT = 1

        /**
         * Edge does not start the way, no sign on the edge.
         * Speed limit time is the same of on previous edge.
         */
        const val PROLONGED = 2

        /**
         * Type is unknown.
         */
        const val UNKNOWN = 3

        /**
         * Retention policy for the [SpeedLimitType]
         */
        @Retention(AnnotationRetention.BINARY)
        @IntDef(
            IMPLICIT,
            EXPLICIT,
            PROLONGED,
            UNKNOWN,
        )
        annotation class Type
    }

    internal companion object {

        @JvmSynthetic
        fun createFromNativeObject(
            nativeObj: com.mapbox.navigator.SpeedLimitInfo,
        ): AdasSpeedLimitInfo {
            val speedUnit = when (nativeObj.unit) {
                SpeedLimitUnit.KILOMETRES_PER_HOUR -> SpeedUnit.KILOMETERS_PER_HOUR
                SpeedLimitUnit.MILES_PER_HOUR -> SpeedUnit.MILES_PER_HOUR
            }
            return AdasSpeedLimitInfo(
                value = nativeObj.value,
                speedUnit = speedUnit,
                speedLimitType = createSpeedLimitType(nativeObj.type),
                restriction = AdasSpeedLimitRestriction.createFromNativeObject(
                    nativeObj.restriction,
                ),
            )
        }

        @JvmSynthetic
        @SpeedLimitType.Type
        private fun createSpeedLimitType(nativeObj: com.mapbox.navigator.SpeedLimitType): Int {
            return when (nativeObj) {
                com.mapbox.navigator.SpeedLimitType.IMPLICIT -> SpeedLimitType.IMPLICIT
                com.mapbox.navigator.SpeedLimitType.EXPLICIT -> SpeedLimitType.EXPLICIT
                com.mapbox.navigator.SpeedLimitType.PROLONGED -> SpeedLimitType.PROLONGED
                com.mapbox.navigator.SpeedLimitType.UNKNOWN -> SpeedLimitType.UNKNOWN
            }
        }
    }
}
