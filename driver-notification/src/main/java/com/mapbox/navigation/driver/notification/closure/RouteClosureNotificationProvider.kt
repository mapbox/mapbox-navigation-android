package com.mapbox.navigation.driver.notification.closure

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.route.isClosureAlternative
import com.mapbox.navigation.base.trip.model.roadobject.UpcomingRoadObject
import com.mapbox.navigation.base.trip.model.roadobject.incident.Incident
import com.mapbox.navigation.base.trip.model.roadobject.incident.IncidentType
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.internal.extensions.flowRouteProgress
import com.mapbox.navigation.core.internal.extensions.flowRoutesUpdated
import com.mapbox.navigation.driver.notification.DriverNotification
import com.mapbox.navigation.driver.notification.DriverNotificationProvider
import com.mapbox.navigation.utils.internal.logD
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge

/**
 * Provides notifications about road closures detected on the current route.
 *
 * Emits:
 * - [RouteClosureMonitoringNotification] when a closure is detected beyond the configured threshold.
 * - [RouteClosureAlternativeNotification] when a closure within the threshold is detected and a
 *   routes update arrives carrying a closure alternative (request `reason=closure`).
 * - [RouteClosureResolvedNotification] when no closure beyond the threshold is present.
 *
 * @param options configuration options for the provider
 *
 * @see [RouteClosureNotificationOptions] for configuring the provider
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class RouteClosureNotificationProvider(
    var options: RouteClosureNotificationOptions = RouteClosureNotificationOptions.Builder()
        .build(),
) : DriverNotificationProvider("route-closure") {

    private val alternativeTriggerThresholdMeters get() = options.alternativeTriggerThresholdMeters

    private val mapboxNavigationFlow = MutableStateFlow<MapboxNavigation?>(null)

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigationFlow.value = mapboxNavigation
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigationFlow.value = null
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun trackNotifications(): Flow<DriverNotification> =
        mapboxNavigationFlow.flatMapLatest { navigation ->
            navigation?.let {
                merge(
                    it.monitoringFlow(),
                    it.alternativeFlow(),
                )
            } ?: emptyFlow()
        }

    /** Continuously monitors route progress and emits monitoring/resolved notifications. */
    private fun MapboxNavigation.monitoringFlow(): Flow<DriverNotification> =
        flowRouteProgress()
            .map { routeProgress ->
                routeProgress.upcomingRoadObjects.firstClosure()
                    ?.takeIf { (dist, _) -> dist > alternativeTriggerThresholdMeters }
                    ?.let { (dist, id) ->
                        logD(TAG) {
                            "Far closure detected: incidentId=$id, distance=${dist.toLong()}m"
                        }
                        RouteClosureMonitoringNotification(id, dist)
                    }
                    ?: RouteClosureResolvedNotification()
            }
            .distinctUntilChanged()

    /**
     * Fires when a routes update delivers a closure alternative and a close closure is still
     * active on the live primary route. [firstClosure] on upcomingRoadObjects guards against
     * re-firing after the driver has switched onto a closure-avoiding primary: the new primary
     * no longer has the closure in upcomingRoadObjects.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun MapboxNavigation.alternativeFlow(): Flow<DriverNotification> =
        flowRoutesUpdated()
            .filter { it.hasClosureAlternative() }
            .mapLatest { routeUpdate ->
                val routeProgress = flowRouteProgress().first()
                val (distance, incidentId) = routeProgress.upcomingRoadObjects.firstClosure()
                    ?: return@mapLatest null
                if (distance > alternativeTriggerThresholdMeters) return@mapLatest null
                val altRoute = routeUpdate.navigationRoutes.getOrNull(1) ?: return@mapLatest null
                logD(TAG) {
                    "Emitting alternative: " +
                        "incidentId=$incidentId, " +
                        "distance=${distance.toLong()}m, " +
                        "altRouteId=${altRoute.id}"
                }
                RouteClosureAlternativeNotification(incidentId, distance, altRoute)
            }
            .mapNotNull { it }
            .distinctUntilChanged()

    private fun RoutesUpdatedResult.hasClosureAlternative(): Boolean =
        navigationRoutes.size > 1 && navigationRoutes.any { it.isClosureAlternative() }

    private fun List<UpcomingRoadObject>.firstClosure(): Pair<Double, String>? =
        asSequence()
            .mapNotNull { obj -> (obj.roadObject as? Incident)?.let { obj to it } }
            .filter { (_, incident) ->
                incident.info.isClosed && incident.info.type == IncidentType.ROAD_CLOSURE
            }
            .mapNotNull { (obj, incident) ->
                obj.distanceToStart?.takeIf { it >= 0.0 }?.let { it to incident.id }
            }
            .firstOrNull()

    private companion object {

        private const val TAG = "RouteClosureNotificationProvider"
    }
}
