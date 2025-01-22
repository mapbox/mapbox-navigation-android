package com.mapbox.navigation.core.trip.session.location

import androidx.annotation.IntRange
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI

/**
 * Dead-reckoning sensor fusion status
 *
 * @param state State of internal sensor fuser
 * @param progressInitDistance Calculated progress of initialization distance (percentage, range 0..100*)
 * @param progressCorrectionBuckets Calculated progress of correction buckets (percentage, range *0..100*)
 */
@ExperimentalMapboxNavigationAPI
class DRSensorFusionStatus internal constructor(
    @DRSensorFusionState.State val state: String,
    @IntRange(0, 100) val progressInitDistance: Byte,
    @IntRange(0, 100) val progressCorrectionBuckets: Byte,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DRSensorFusionStatus

        if (state != other.state) return false
        if (progressInitDistance != other.progressInitDistance) return false
        if (progressCorrectionBuckets != other.progressCorrectionBuckets) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = state.hashCode()
        result = 31 * result + progressInitDistance
        result = 31 * result + progressCorrectionBuckets
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "DRSensorFusionStatus(" +
            "state='$state', " +
            "progressInitDistance=$progressInitDistance, " +
            "progressCorrectionBuckets=$progressCorrectionBuckets" +
            ")"
    }

    internal companion object {

        @JvmSynthetic
        fun createFromNativeObject(
            nativeObj: com.mapbox.navigator.DRSensorFusionStatus?,
        ): DRSensorFusionStatus? {
            nativeObj ?: return null
            return DRSensorFusionStatus(
                state = DRSensorFusionState.createFromNativeObject(nativeObj.state),
                progressInitDistance = nativeObj.progressInitDistance,
                progressCorrectionBuckets = nativeObj.progressCorrectionBuckets,
            )
        }
    }
}
