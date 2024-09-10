package com.mapbox.navigation.ui.androidauto.navigation

import androidx.annotation.VisibleForTesting
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.car.app.navigation.model.TravelEstimate
import androidx.lifecycle.lifecycleScope
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.bindgen.Expected
import com.mapbox.maps.extension.androidauto.MapboxCarMapObserver
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.trip.session.BannerInstructionsObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.tripdata.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.tripdata.maneuver.model.Maneuver
import com.mapbox.navigation.tripdata.maneuver.model.ManeuverError
import com.mapbox.navigation.tripdata.shield.model.RouteShield
import com.mapbox.navigation.ui.androidauto.internal.extensions.mapboxNavigationForward
import com.mapbox.navigation.ui.androidauto.internal.logAndroidAuto
import com.mapbox.navigation.ui.maps.guidance.junction.api.MapboxJunctionApi
import com.mapbox.navigation.ui.maps.guidance.junction.model.JunctionValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Provides the [NavigationTemplate.NavigationInfo] and [TravelEstimate] populated with Mapbox
 * navigation data. This can be used with a [Screen] that shows a [NavigationTemplate].
 *
 * @see [ActiveGuidanceScreen] for an example.
 */
class CarNavigationInfoProvider
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal constructor(
    private val services: CarNavigationInfoServices,
) : MapboxCarMapObserver {
    /**
     * Public constructor and the internal constructor is for unit testing.
     */
    constructor() : this(CarNavigationInfoServices())

    private val mapUserStyleObserver = services.mapUserStyleObserver()
    private val routeProgressObserver = RouteProgressObserver(this::onRouteProgress)
    private val bannerInstructionsObserver =
        BannerInstructionsObserver(this::onNewBannerInstructions)
    private val navigationObserver = mapboxNavigationForward(this::onAttached, this::onDetached)
    private val _carNavigationInfo = MutableStateFlow(CarNavigationInfo())
    private var currentShields = emptyList<RouteShield>()
    private var currentJunctionValue: JunctionValue? = null

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var carContext: CarContext? = null

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var navigationInfoMapper: CarNavigationInfoMapper? = null

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var navigationEtaMapper: CarNavigationEtaMapper? = null

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var maneuverApi: MapboxManeuverApi? = null

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var junctionApi: MapboxJunctionApi? = null

    /**
     * Contains data that helps populate the [NavigationTemplate] with navigation data.
     */
    val carNavigationInfo: StateFlow<CarNavigationInfo> = _carNavigationInfo.asStateFlow()

    /**
     * When you have a [NavigationTemplate.Builder], you can set the navigation info and travel
     * estimate
     */
    fun setNavigationInfo(builder: NavigationTemplate.Builder): NavigationTemplate.Builder {
        with(carNavigationInfo.value) {
            navigationInfo?.let {
                builder.setNavigationInfo(it)
            }
            destinationTravelEstimate?.let {
                builder.setDestinationTravelEstimate(it)
            }
        }
        return builder
    }

    /**
     * Helper function to allow you to hook up the provider to a screen. The screen will be
     * invalidated when the data changes, and you can populate the [NavigationTemplate] with
     * the navigation info inside [Screen.onGetTemplate].
     */
    fun invalidateOnChange(screen: Screen) = apply {
        carNavigationInfo.drop(1).onEach {
            screen.invalidate()
        }.launchIn(screen.lifecycleScope)
    }

    /**
     * MapboxCarMapSurface is attached
     */
    override fun onAttached(mapboxCarMapSurface: MapboxCarMapSurface) {
        this.carContext = mapboxCarMapSurface.carContext
        mapUserStyleObserver.onAttached(mapboxCarMapSurface)
        MapboxNavigationApp.registerObserver(navigationObserver)
        logAndroidAuto("CarRouteProgressObserver onAttached")
    }

    /**
     * MapboxCarMapSurface is detached
     */
    override fun onDetached(mapboxCarMapSurface: MapboxCarMapSurface) {
        mapUserStyleObserver.onDetached(mapboxCarMapSurface)
        MapboxNavigationApp.unregisterObserver(navigationObserver)
        this.carContext = null
        logAndroidAuto("CarRouteProgressObserver onDetached")
    }

    private fun onAttached(mapboxNavigation: MapboxNavigation) {
        val carContext = carContext!!
        maneuverApi = services.maneuverApi(mapboxNavigation)
        junctionApi = services.junctionApi()
        navigationEtaMapper = services.carNavigationEtaMapper(carContext)
        navigationInfoMapper = services.carNavigationInfoMapper(carContext, mapboxNavigation)
        mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.registerBannerInstructionsObserver(bannerInstructionsObserver)
    }

    private fun onDetached(mapboxNavigation: MapboxNavigation) {
        maneuverApi!!.cancel()
        junctionApi!!.cancelAll()
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.unregisterBannerInstructionsObserver(bannerInstructionsObserver)
        maneuverApi = null
        junctionApi = null
        currentJunctionValue = null
        navigationEtaMapper = null
        navigationInfoMapper = null
        _carNavigationInfo.value = CarNavigationInfo()
    }

    private fun onRouteProgress(routeProgress: RouteProgress) {
        val expectedManeuvers = maneuverApi?.getManeuvers(routeProgress) ?: return
        updateNavigationInfo(expectedManeuvers, routeProgress)

        expectedManeuvers.onValue { maneuvers ->
            maneuverApi?.getRoadShields(
                mapUserStyleObserver.userId,
                mapUserStyleObserver.styleId,
                maneuvers,
            ) { shieldResult ->
                val newShields = shieldResult.mapNotNull { it.value?.shield }
                if (currentShields != newShields) {
                    currentShields = newShields
                    updateNavigationInfo(expectedManeuvers, routeProgress)
                }
            }
        }
    }

    private fun onNewBannerInstructions(bannerInstructions: BannerInstructions) {
        junctionApi?.generateJunction(bannerInstructions) {
            currentJunctionValue = it.value
        }
    }

    private fun updateNavigationInfo(
        maneuvers: Expected<ManeuverError, List<Maneuver>>,
        routeProgress: RouteProgress,
    ) {
        _carNavigationInfo.value = CarNavigationInfo(
            navigationInfo = navigationInfoMapper
                ?.mapNavigationInfo(maneuvers, currentShields, routeProgress, currentJunctionValue),
            destinationTravelEstimate = navigationEtaMapper
                ?.getDestinationTravelEstimate(routeProgress),
        )
    }
}
