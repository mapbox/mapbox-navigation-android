package com.mapbox.navigation.base.trip.model

import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.navigation.base.internal.utils.safeCompareTo

/**
 * Represents data used for trip notifications
 */
sealed class TripNotificationState {
    /**
     * Represents data related to trip notifications.
     *
     * @param bannerInstructions an optional [BannerInstructions]
     * @param distanceRemaining an optional value representing the distance remaining
     * @param durationRemaining an optional value representing the trip duration remaining
     * @param drivingSide an optional value representing the driving side
     */
    class TripNotificationData internal constructor(
        val bannerInstructions: BannerInstructions?,
        val distanceRemaining: Double?,
        val durationRemaining: Double?,
        val drivingSide: String?,
    ) : TripNotificationState() {

        /**
         * Indicates whether some other object is "equal to" this one.
         */
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as TripNotificationData

            if (bannerInstructions != other.bannerInstructions) return false
            if (!distanceRemaining.safeCompareTo(other.distanceRemaining)) return false
            if (!durationRemaining.safeCompareTo(other.durationRemaining)) return false
            return drivingSide == other.drivingSide
        }

        /**
         * Returns a hash code value for the object.
         */
        override fun hashCode(): Int {
            var result = bannerInstructions?.hashCode() ?: 0
            result = 31 * result + (distanceRemaining?.hashCode() ?: 0)
            result = 31 * result + (durationRemaining?.hashCode() ?: 0)
            result = 31 * result + (drivingSide?.hashCode() ?: 0)
            return result
        }

        /**
         * Returns a string representation of the object.
         */
        override fun toString(): String {
            return "TripNotificationData(" +
                "bannerInstructions=$bannerInstructions, " +
                "distanceRemaining=$distanceRemaining, " +
                "durationRemaining=$durationRemaining, " +
                "drivingSide=$drivingSide" +
                ")"
        }
    }

    /**
     * Represents a data-less state.
     */
    class TripNotificationFreeState internal constructor() : TripNotificationState() {

        /**
         * Indicates whether some other object is "equal to" this one.
         */
        override fun equals(other: Any?): Boolean {
            return this === other
        }

        /**
         * Returns a hash code value for the object.
         */
        override fun hashCode(): Int {
            return System.identityHashCode(this)
        }
    }
}
