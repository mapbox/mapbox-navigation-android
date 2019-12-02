package com.mapbox.services.android.navigation.v5.navigation

import androidx.annotation.ColorRes
import com.mapbox.services.android.navigation.R
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.NAVIGATION_INTERSECTION_RADIUS_FOR_OFF_ROUTE_DETECTION
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.NAVIGATION_LOCATION_ENGINE_INTERVAL_LAG
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.NAVIGATION_OFF_ROUTE_THRESHOLD
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.NAVIGATION_OFF_ROUTE_THRESHOLD_WHEN_NEAR_INTERSECTION
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.ROUNDING_INCREMENT_FIFTY
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.ROUTE_REFRESH_INTERVAL
import com.mapbox.services.android.navigation.v5.navigation.notification.NavigationNotification

/**
 * Immutable and can't be changed after passing into [MapboxNavigation].
 */
data class MapboxNavigationOptions(
    val defaultMilestonesEnabled: Boolean,
    val enableFasterRouteDetection: Boolean,
    val enableAutoIncrementLegIndex: Boolean,
    val enableRefreshRoute: Boolean,
    val refreshIntervalInMilliseconds: Long,
    val isFromNavigationUi: Boolean,
    val isDebugLoggingEnabled: Boolean,
    val navigationNotification: NavigationNotification?,
    val roundingIncrement: Int,
    val timeFormatType: Int,
    val navigationLocationEngineIntervalLagInMilliseconds: Int,
    val defaultNotificationColorId: Int,
    val offRouteThreshold: Float,
    val offRouteThresholdWhenNearIntersection: Float,
    val intersectionRadiusForOffRouteDetection: Float,
    var builder: Builder

) {

    fun enableFasterRouteDetection() = enableFasterRouteDetection

    fun enableAutoIncrementLegIndex() = enableAutoIncrementLegIndex

    /**
     * This value indicates if route refresh is enabled or disabled.
     *
     * @return whether route refresh is enabled or not
     */
    fun enableRefreshRoute() = enableRefreshRoute

    /**
     * This value indicates the route refresh interval.
     *
     * @return route refresh interval in milliseconds
     */
    fun refreshIntervalInMilliseconds() = refreshIntervalInMilliseconds

    fun navigationNotification() = navigationNotification

    @NavigationConstants.RoundingIncrement
    fun roundingIncrement() = roundingIncrement

    @TimeFormatType
    fun timeFormatType() = timeFormatType

    fun navigationLocationEngineIntervalLagInMilliseconds() =
        navigationLocationEngineIntervalLagInMilliseconds

    /**
     * The color resource id for the default notification.  This will be ignored
     * if a [NavigationNotification] is set.
     *
     * @return color resource id for notification
     */
    @ColorRes
    fun defaultNotificationColorId() = defaultNotificationColorId

    fun toBuilder() = builder

    class Builder {
        var defaultMilestonesEnabled = true
        var enableFasterRouteDetection = false
        var enableAutoIncrementLegIndex = true
        var enableRefreshRoute = true
        var refreshIntervalInMilliseconds = ROUTE_REFRESH_INTERVAL
        var isFromNavigationUi = false
        var isDebugLoggingEnabled = false
        var navigationNotification: NavigationNotification? = null
        var roundingIncrement = ROUNDING_INCREMENT_FIFTY
        var timeFormatType = NONE_SPECIFIED
        var navigationLocationEngineIntervalLagInMilliseconds =
            NAVIGATION_LOCATION_ENGINE_INTERVAL_LAG
        var defaultNotificationColorId = R.color.mapboxNotificationBlue
        var offRouteThreshold = NAVIGATION_OFF_ROUTE_THRESHOLD
        var offRouteThresholdWhenNearIntersection =
            NAVIGATION_OFF_ROUTE_THRESHOLD_WHEN_NEAR_INTERSECTION
        var intersectionRadiusForOffRouteDetection =
            NAVIGATION_INTERSECTION_RADIUS_FOR_OFF_ROUTE_DETECTION

        fun defaultMilestonesEnabled(defaultMilestonesEnabled: Boolean) =
            apply { this.defaultMilestonesEnabled = defaultMilestonesEnabled }

        fun enableFasterRouteDetection(enableFasterRouteDetection: Boolean) =
            apply { this.enableFasterRouteDetection = enableFasterRouteDetection }

        fun enableAutoIncrementLegIndex(enableAutoIncrementLegIndex: Boolean) =
            apply { this.enableAutoIncrementLegIndex = enableAutoIncrementLegIndex }

        /**
         * This enables / disables refresh route. If not specified, it's enabled by default.
         *
         * @param enableRefreshRoute whether or not to enable route refresh
         * @return this builder for chaining options together
         */
        fun enableRefreshRoute(enableRefreshRoute: Boolean) =
            apply { this.enableRefreshRoute = enableRefreshRoute }

        /**
         * This sets the route refresh interval. If not specified, the interval is 5 minutes by default.
         *
         * @param intervalInMilliseconds for route refresh
         * @return this builder for chaining options together
         */
        fun refreshIntervalInMilliseconds(intervalInMilliseconds: Long) =
            apply { this.refreshIntervalInMilliseconds = intervalInMilliseconds }

        fun isFromNavigationUi(isFromNavigationUi: Boolean) =
            apply { this.isFromNavigationUi = isFromNavigationUi }

        fun isDebugLoggingEnabled(debugLoggingEnabled: Boolean) =
            apply { this.isDebugLoggingEnabled = isDebugLoggingEnabled }

        fun navigationNotification(notification: NavigationNotification) =
            apply { this.navigationNotification = navigationNotification }

        fun roundingIncrement(@NavigationConstants.RoundingIncrement roundingIncrement: Int) =
            apply { this.roundingIncrement = roundingIncrement }

        fun timeFormatType(@TimeFormatType type: Int) =
            apply { this.timeFormatType = timeFormatType }

        fun navigationLocationEngineIntervalLagInMilliseconds(lagInMilliseconds: Int) = apply {
            this.navigationLocationEngineIntervalLagInMilliseconds =
                navigationLocationEngineIntervalLagInMilliseconds
        }

        /**
         * Optionally, set the background color of the default notification.
         *
         * @param defaultNotificationColorId the color resource to be used
         * @return this builder for chaining operations together
         */
        fun defaultNotificationColorId(@ColorRes defaultNotificationColorId: Int) =
            apply { this.defaultNotificationColorId = defaultNotificationColorId }

        /**
         * This sets the off route threshold in meters. If not specified, the threshold is 50 meters.
         *
         * @param thresholdInMeters off route threshold in meters to be used
         * @return this builder for chaining options together
         */
        fun offRouteThreshold(thresholdInMeters: Float) =
            apply { this.offRouteThreshold = thresholdInMeters }

        /**
         * This sets the off route threshold in meters when near an intersection which is more prone
         * to inaccurate gps fixes. If not specified, the threshold is 25 meters.
         *
         * @param thresholdInMeters off route threshold in meters when near an intersection to be used
         * @return this builder for chaining options together
         */
        fun offRouteThresholdWhenNearIntersection(thresholdInMeters: Float) =
            apply { this.offRouteThresholdWhenNearIntersection = thresholdInMeters }

        /**
         * This sets the radius in meters for off route detection near intersection. If not specified, the radius is 40 meters.
         *
         * @param radiusInMeters radius in meters for off route detection near intersection to be used
         * @return this builder for chaining options together
         */
        fun intersectionRadiusForOffRouteDetection(radiusInMeters: Float) =
            apply { this.intersectionRadiusForOffRouteDetection = radiusInMeters }

        fun build(): MapboxNavigationOptions {
            return MapboxNavigationOptions(
                defaultMilestonesEnabled,
                enableFasterRouteDetection,
                enableAutoIncrementLegIndex,
                enableRefreshRoute,
                refreshIntervalInMilliseconds,
                isFromNavigationUi,
                isDebugLoggingEnabled,
                navigationNotification,
                roundingIncrement,
                timeFormatType,
                navigationLocationEngineIntervalLagInMilliseconds,
                defaultNotificationColorId,
                offRouteThreshold,
                offRouteThresholdWhenNearIntersection,
                intersectionRadiusForOffRouteDetection,
                this
            )
        }
    }
}
