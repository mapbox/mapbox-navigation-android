package com.mapbox.navigation.base.trip.model

import com.mapbox.api.directions.v5.models.BannerInstructions

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
    data class TripNotificationData internal constructor(
        val bannerInstructions: BannerInstructions?,
        val distanceRemaining: Double?,
        val durationRemaining: Double?,
        val drivingSide: String?
    ) : TripNotificationState()

    /**
     * Represents a data-less state.
     */
    class TripNotificationFreeState internal constructor() : TripNotificationState()
}
