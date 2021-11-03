package com.mapbox.androidauto.car.navigation

import androidx.car.app.navigation.model.NavigationTemplate
import androidx.car.app.navigation.model.TravelEstimate
import com.mapbox.androidauto.logAndroidAuto
import com.mapbox.bindgen.Expected
import com.mapbox.maps.MapboxExperimental
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.ui.maneuver.model.Maneuver
import com.mapbox.navigation.ui.maneuver.model.ManeuverError
import com.mapbox.navigation.ui.shield.model.RouteShield

/**
 * Observe MapboxNavigation properties that create NavigationInfo.
 *
 * Attach the [start] [stop] functions to start observing navigation info.
 */
@OptIn(MapboxExperimental::class)
class CarNavigationInfoObserver(
    private val carActiveGuidanceCarContext: CarActiveGuidanceCarContext
) {
    private var onNavigationInfoChanged: (() -> Unit)? = null
    private var currentShields = emptyList<RouteShield>()
    private val mapUserStyleObserver = MapUserStyleObserver()

    var navigationInfo: NavigationTemplate.NavigationInfo? = null
        private set(value) {
            if (field != value) {
                logAndroidAuto("CarNavigationInfoObserver navigationInfo changed")
                field = value
                onNavigationInfoChanged?.invoke()
            }
        }

    var travelEstimateInfo: TravelEstimate? = null

    private val routeProgressObserver = RouteProgressObserver { routeProgress ->
        val expectedManeuvers = carActiveGuidanceCarContext.maneuverApi.getManeuvers(routeProgress)
        updateNavigationInfo(expectedManeuvers, currentShields, routeProgress)

        expectedManeuvers.onValue { maneuvers ->
            carActiveGuidanceCarContext.maneuverApi.getRoadShields(
                mapUserStyleObserver.userId,
                mapUserStyleObserver.styleId,
                carActiveGuidanceCarContext.mapboxNavigation.navigationOptions.accessToken,
                maneuvers,
            ) { shieldResult ->
                val newShields = shieldResult.mapNotNull { it.value?.shield }
                if (currentShields != newShields) {
                    currentShields = newShields
                    updateNavigationInfo(expectedManeuvers, newShields, routeProgress)
                }
            }
        }
    }

    private fun updateNavigationInfo(
        maneuvers: Expected<ManeuverError, List<Maneuver>>,
        shields: List<RouteShield>,
        routeProgress: RouteProgress,
    ) {
        this.navigationInfo = carActiveGuidanceCarContext.navigationInfoMapper
            .mapNavigationInfo(maneuvers, shields, routeProgress)

        this.travelEstimateInfo = carActiveGuidanceCarContext.tripProgressMapper.from(routeProgress)
    }

    fun start(onNavigationInfoChanged: () -> Unit) {
        this.onNavigationInfoChanged = onNavigationInfoChanged
        carActiveGuidanceCarContext.mapboxCarMap.registerObserver(mapUserStyleObserver)
        logAndroidAuto("CarRouteProgressObserver onStart")
        val mapboxNavigation = carActiveGuidanceCarContext.mapboxNavigation
        mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
    }

    fun stop() {
        logAndroidAuto("CarRouteProgressObserver onStop")
        onNavigationInfoChanged = null
        carActiveGuidanceCarContext.mapboxCarMap.unregisterObserver(mapUserStyleObserver)
        val mapboxNavigation = carActiveGuidanceCarContext.mapboxNavigation
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        carActiveGuidanceCarContext.maneuverApi.cancel()
    }
}
