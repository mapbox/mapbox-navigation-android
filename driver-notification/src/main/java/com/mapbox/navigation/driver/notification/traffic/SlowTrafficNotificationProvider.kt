package com.mapbox.navigation.driver.notification.traffic

import android.os.SystemClock
import com.mapbox.api.directions.v5.models.LegAnnotation
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.flowRouteProgress
import com.mapbox.navigation.driver.notification.DriverNotification
import com.mapbox.navigation.driver.notification.DriverNotificationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

/**
 * Provides notifications for slow traffic conditions along the route.
 *
 * The `SlowTrafficNotificationProvider` generates `SlowTrafficNotification` instances
 * when slow traffic is detected on the current route. It evaluates traffic conditions
 * based on route progress and configurable options, such as congestion thresholds and
 * traffic delay tolerances.
 *
 * This provider listens to route progress updates and identifies slow traffic segments
 * by analyzing congestion levels, distances, and durations. Notifications are generated
 * when the delay caused by slow traffic exceeds the configured threshold.
 *
 * **Required Annotations from the navigation backend**:
 * - **Distance** (`List<Double>`, from [LegAnnotation.distance]): The distance of each geometry segment in meters, used to calculate the total affected distance.
 * - **Duration** (`List<Double>`, from [LegAnnotation.duration]): The duration of each geometry segment in seconds, used to calculate the total affected time.
 * - **Free-flow speed** (`List<Integer?>`, from [LegAnnotation.freeflowSpeed]): The speed under free-flow conditions for each geometry segment in km/h, used to estimate the free-flow duration.
 * - **Congestion numeric** (`List<Integer?>`, from [LegAnnotation.congestionNumeric]): The congestion level for each geometry segment as a numeric value, used to identify slow traffic conditions.
 *
 * @param options configuration options for the provider
 *
 * @see [SlowTrafficNotification] for the notification generated by this provider
 * @see [SlowTrafficNotificationOptions] for configuring the provider
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class SlowTrafficNotificationProvider(
    var options: SlowTrafficNotificationOptions = SlowTrafficNotificationOptions.Builder().build(),
) : DriverNotificationProvider() {

    private var lastUpdate = 0L
    private var mapboxNavigationFlow = MutableStateFlow<MapboxNavigation?>(null)

    /**
     * Attaches the `MapboxNavigation` instance to the provider.
     */
    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        this.mapboxNavigationFlow.value = mapboxNavigation
    }

    /**
     * Detaches the `MapboxNavigation` instance from the provider.
     */
    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        this.mapboxNavigationFlow.value = null
    }

    /**
     * Tracks notifications for slow traffic conditions.
     *
     * This method returns a `Flow` of `DriverNotification` instances, which includes
     * `SlowTrafficNotification` when slow traffic is detected along the route.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun trackNotifications(): Flow<DriverNotification> =
        mapboxNavigationFlow
            .flatMapLatest { it?.flowRouteProgress() ?: emptyFlow() }
            .mapNotNull { getSlowTrafficNotification(it) }

    private suspend fun getSlowTrafficNotification(
        routeProgress: RouteProgress,
    ): SlowTrafficNotification? {
        if (lastUpdate == 0L ||
            SystemClock.elapsedRealtime() - lastUpdate >=
            options.slowTrafficPeriodCheck.inWholeMilliseconds
        ) {
            lastUpdate = SystemClock.elapsedRealtime()
            val legProgress = routeProgress.currentLegProgress ?: return null
            val legDistances =
                legProgress.routeLeg?.annotation()?.distance() ?: return null
            val legDurations =
                legProgress.routeLeg?.annotation()?.duration() ?: return null
            val legFreeFlowSpeeds =
                legProgress.routeLeg?.annotation()?.freeflowSpeed() ?: return null
            val legCongestions = legProgress.routeLeg?.annotation()?.congestionNumeric()
                ?: return null
            val slowTrafficRange = options.slowTrafficCongestionRange

            return withContext(Dispatchers.Default) {
                var slowTrafficDistance = 0.0
                var slowTrafficDurationSec = 0.0
                var freeFlowDurationSec = 0.0
                var i = legProgress.geometryIndex

                // Check traffic only until the end of the current leg because of the waypoint
                val minTrafficDelaySec = options.trafficDelay.inWholeSeconds
                while (i < legDistances.size &&
                    i < legCongestions.size &&
                    i < legDurations.size &&
                    i < legFreeFlowSpeeds.size &&
                    legCongestions[i].isSlowTraffic(slowTrafficRange)
                ) {
                    slowTrafficDistance += legDistances[i]
                    slowTrafficDurationSec += legDurations[i]

                    val legFreeFlowSpeed = legFreeFlowSpeeds[i]
                    freeFlowDurationSec += if (legFreeFlowSpeed != null) {
                        // Speed is km/h, but distance is in the meters. Applying conversion of the speed from km/h -> m/s
                        legDistances[i] * KM_PER_H_TO_M_PER_SEC_RATE / legFreeFlowSpeed
                    } else {
                        // Adding average duration based on the previous geometry
                        freeFlowDurationSec / (i - legProgress.geometryIndex)
                    }
                    i++
                }
                if (slowTrafficDurationSec - freeFlowDurationSec >= minTrafficDelaySec) {
                    SlowTrafficNotification(
                        legProgress.legIndex,
                        legProgress.geometryIndex..i,
                        freeFlowDurationSec.seconds,
                        slowTrafficDurationSec.seconds,
                        slowTrafficDistance,
                    )
                } else {
                    null
                }
            }
        }
        return null
    }

    private fun Int?.isSlowTraffic(range: IntRange) = range.contains(this)

    private companion object {

        // Calc time with seconds:
        // dist_m / speed_km_h = dist_m / (speed_km_h * 1000 / 3600) = dist_m * 3.6 / speed_km_h = duration_sec
        private const val KM_PER_H_TO_M_PER_SEC_RATE = 3.6
    }
}
