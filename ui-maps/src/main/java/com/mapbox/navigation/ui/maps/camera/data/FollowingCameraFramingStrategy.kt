package com.mapbox.navigation.ui.maps.camera.data

import com.mapbox.geojson.Point
import com.mapbox.navigation.base.trip.model.RouteProgress

/**
 * Strategy used to calculate points to be framed for the FOLLOWING camera mode.
 */
interface FollowingCameraFramingStrategy {

    /**
     * Returns points to be framed on the remainder of the current step.
     */
    fun getPointsToFrameOnCurrentStep(
        routeProgress: RouteProgress,
        followingFrameOptions: FollowingFrameOptions,
        averageIntersectionDistancesOnRoute: List<List<Double>>,
    ): List<Point>

    /**
     * Finds the points to frame after maneuver based on preprocessed data and settings.
     */
    fun getPointsToFrameAfterCurrentManeuver(
        routeProgress: RouteProgress,
        followingFrameOptions: FollowingFrameOptions,
        postManeuverFramingPoints: List<List<List<Point>>>,
    ): List<Point>

    companion object {
        /**
         * Default [FollowingCameraFramingStrategy]
         */
        val Default: FollowingCameraFramingStrategy = MapboxFollowingCameraFramingStrategy
    }
}
