package com.mapbox.navigation.base.internal.factory

import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.TripNotificationState
import com.mapbox.navigation.base.utils.ifNonNull

object TripNotificationStateFactory {

    fun buildTripNotificationState(
        bannerInstructions: BannerInstructions?,
        distanceRemaining: Double?,
        durationRemaining: Double?,
        drivingSide: String?,
    ): TripNotificationState {
        return TripNotificationState.TripNotificationData(
            bannerInstructions,
            distanceRemaining,
            durationRemaining,
            drivingSide,
        )
    }

    fun buildTripNotificationState(routeProgress: RouteProgress?): TripNotificationState {
        return ifNonNull(routeProgress) { progress ->
            buildTripNotificationState(
                progress.bannerInstructions,
                progress.currentLegProgress?.currentStepProgress?.distanceRemaining?.toDouble(),
                progress.currentLegProgress?.durationRemaining,
                progress.currentLegProgress?.currentStepProgress?.step?.drivingSide(),
            )
        } ?: TripNotificationState.TripNotificationFreeState()
    }
}
