package com.mapbox.navigation.core.trip.session.location

import com.mapbox.common.location.Location
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.core.navigator.toLocation
import com.mapbox.navigation.core.trip.session.LocationMatcherResult

/**
 * Corrected GPS location data, the result of corrections applied to the input location, if any.
 * Users still need to use [LocationMatcherResult.enhancedLocation]
 *
 * @param location Corrected location
 * @param isStill The flag indicating whether a vehicle is in still mode
 * @param drSensorFusionStatus Status of sensor fuser
 */
@ExperimentalMapboxNavigationAPI
class CorrectedLocationData internal constructor(
    val location: Location,
    val isStill: Boolean,
    val drSensorFusionStatus: DRSensorFusionStatus?,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CorrectedLocationData

        if (location != other.location) return false
        if (isStill != other.isStill) return false
        if (drSensorFusionStatus != other.drSensorFusionStatus) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = location.hashCode()
        result = 31 * result + isStill.hashCode()
        result = 31 * result + (drSensorFusionStatus?.hashCode() ?: 0)
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "CorrectedLocationData(" +
            "location=$location, " +
            "isStill=$isStill, " +
            "drSensorFusionStatus=$drSensorFusionStatus" +
            ")"
    }

    internal companion object {

        @JvmSynthetic
        fun createFromNativeObject(
            nativeObj: com.mapbox.navigator.CorrectedLocationData?,
        ): CorrectedLocationData? {
            nativeObj ?: return null
            return CorrectedLocationData(
                location = nativeObj.location.toLocation(),
                isStill = nativeObj.isStill,
                drSensorFusionStatus = DRSensorFusionStatus.createFromNativeObject(
                    nativeObj.drSensorFusionStatus,
                ),
            )
        }
    }
}
