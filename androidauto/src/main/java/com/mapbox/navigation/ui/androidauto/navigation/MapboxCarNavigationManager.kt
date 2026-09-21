package com.mapbox.navigation.ui.androidauto.navigation

import android.os.SystemClock
import androidx.car.app.CarContext
import androidx.car.app.navigation.NavigationManager
import androidx.car.app.navigation.NavigationManagerCallback
import androidx.car.app.navigation.model.Trip
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.core.formatter.MapboxDistanceUtil
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.tripdata.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.ui.androidauto.internal.logAndroidAuto
import com.mapbox.navigation.ui.androidauto.internal.logAndroidAutoFailure
import com.mapbox.navigation.ui.androidauto.navigation.maneuver.CarManeuverMapper
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreenManager
import com.mapbox.navigation.ui.androidauto.telemetry.MapboxCarTelemetry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Register this observer using [MapboxNavigationApp.registerObserver]. As long as it is
 * registered, the trip status of [MapboxNavigation] will be sent to the [NavigationManager].
 * This is needed to keep the vehicle cluster display updated.
 */
class MapboxCarNavigationManager @JvmOverloads internal constructor(
    carContext: CarContext,
    private val elapsedRealtimeMillis: () -> Long = SystemClock::elapsedRealtime,
) : MapboxNavigationObserver {

    private val navigationManager: NavigationManager by lazy {
        carContext.getCarService(NavigationManager::class.java)
    }

    private var maneuverApi: MapboxManeuverApi? = null
    private var mapboxNavigation: MapboxNavigation? = null
    private var distanceFormatterOptions: DistanceFormatterOptions? = null
    private val carTelemetry = MapboxCarTelemetry()

    private val routeProgressObserver = RouteProgressObserver { routeProgress ->
        val maneuverApi = maneuverApi ?: return@RouteProgressObserver
        if (!routeProgress.shouldUpdateTrip()) return@RouteProgressObserver
        val trip = CarManeuverMapper.from(routeProgress, maneuverApi)
        onUpdateTrip(routeProgress, trip)
    }

    private var inActiveNavigation = false
    private var lastRouteProgress: RouteProgress? = null
    private var lastTripUpdateMillis = Long.MIN_VALUE

    private val routesObserver = RoutesObserver {
        if (it.navigationRoutes.isEmpty()) {
            endActiveNavigation()
        } else {
            startActiveNavigation()
        }
    }

    private val navigationManagerCallback = object : NavigationManagerCallback {
        override fun onStopNavigation() {
            logAndroidAuto("$LOG_CATEGORY onStopNavigation")
            super.onStopNavigation()
            endActiveNavigation()
            mapboxNavigation?.setNavigationRoutes(emptyList())
            if (MapboxScreenManager.current()?.key != MapboxScreen.NAVIGATION) {
                MapboxScreenManager.replaceTop(MapboxScreen.FREE_DRIVE)
            }
        }

        override fun onAutoDriveEnabled() {
            _autoDriveEnabled.value = true
        }
    }

    private val _autoDriveEnabled = MutableStateFlow(false)

    /**
     * Observe the onAutoDriveEnabled callback state. This is false by default, and at any point
     * can become true. The state does not go back to false.
     */
    val autoDriveEnabledFlow: StateFlow<Boolean> = _autoDriveEnabled

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        logAndroidAuto("$LOG_CATEGORY onAttached")
        this.mapboxNavigation = mapboxNavigation
        carTelemetry.onAttached(mapboxNavigation)
        val distanceFormatterOptions =
            mapboxNavigation.navigationOptions.distanceFormatterOptions
        this.distanceFormatterOptions = distanceFormatterOptions
        val distanceFormatter = MapboxDistanceFormatter(distanceFormatterOptions)
        maneuverApi = MapboxManeuverApi(distanceFormatter)
        navigationManager.setNavigationManagerCallback(navigationManagerCallback)
        mapboxNavigation.registerRoutesObserver(routesObserver)
        mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        logAndroidAuto("$LOG_CATEGORY onDetached")
        this.mapboxNavigation = null
        distanceFormatterOptions = null
        carTelemetry.onDetached(mapboxNavigation)
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.unregisterRoutesObserver(routesObserver)

        // Tell android auto navigation stopped. Navigation can continue with mapbox navigation
        // on another device.
        endActiveNavigation()
        navigationManager.clearNavigationManagerCallback()
    }

    private fun onUpdateTrip(routeProgress: RouteProgress, trip: Trip) {
        try {
            navigationManager.updateTrip(trip)
            lastRouteProgress = routeProgress
            lastTripUpdateMillis = elapsedRealtimeMillis()
        } catch (e: IllegalStateException) {
            logAndroidAutoFailure("$LOG_CATEGORY updateTrip failed", e)
        }
    }

    private fun clearTripState() {
        lastRouteProgress = null
        lastTripUpdateMillis = Long.MIN_VALUE
    }

    private fun startActiveNavigation() {
        if (inActiveNavigation) return
        logAndroidAuto("$LOG_CATEGORY start active navigation")
        inActiveNavigation = true
        clearTripState()
        navigationManager.navigationStarted()
    }

    private fun endActiveNavigation() {
        if (!inActiveNavigation) return
        logAndroidAuto("$LOG_CATEGORY stop active navigation")
        inActiveNavigation = false
        clearTripState()
        navigationManager.navigationEnded()
    }

    private fun RouteProgress.shouldUpdateTrip(): Boolean {
        if (!inActiveNavigation) return false
        val previous = lastRouteProgress ?: return true
        if (hasSameVisibleStateAs(previous)) return false
        return hasMeaningfulChangeFrom(previous) ||
            elapsedRealtimeMillis() - lastTripUpdateMillis >= TRIP_UPDATE_INTERVAL_MILLIS
    }

    private fun RouteProgress.hasSameVisibleStateAs(previous: RouteProgress): Boolean {
        return hasSameManeuverAs(previous) &&
            stepDistanceRemaining() == previous.stepDistanceRemaining() &&
            stepDurationRemaining() == previous.stepDurationRemaining() &&
            distanceRemaining == previous.distanceRemaining &&
            durationRemaining == previous.durationRemaining
    }

    private fun RouteProgress.hasMeaningfulChangeFrom(previous: RouteProgress): Boolean {
        return !hasSameManeuverAs(previous) ||
            stepDistanceRemaining().formattedDistance() !=
            previous.stepDistanceRemaining().formattedDistance() ||
            distanceRemaining.toDouble().formattedDistance() !=
            previous.distanceRemaining.toDouble().formattedDistance() ||
            stepDurationRemaining().hasMeaningfulChangeFrom(previous.stepDurationRemaining()) ||
            durationRemaining.hasMeaningfulChangeFrom(previous.durationRemaining)
    }

    private fun RouteProgress.hasSameManeuverAs(previous: RouteProgress): Boolean {
        val legProgress = currentLegProgress
        val previousLegProgress = previous.currentLegProgress
        val stepProgress = legProgress?.currentStepProgress
        val previousStepProgress = previousLegProgress?.currentStepProgress
        return navigationRoute.id == previous.navigationRoute.id &&
            legProgress?.legIndex == previousLegProgress?.legIndex &&
            stepProgress?.stepIndex == previousStepProgress?.stepIndex &&
            stepProgress?.instructionIndex == previousStepProgress?.instructionIndex &&
            bannerInstructions == previous.bannerInstructions
    }

    private fun RouteProgress.stepDistanceRemaining(): Double =
        currentLegProgress?.currentStepProgress?.distanceRemaining?.toDouble()
            ?: distanceRemaining.toDouble()

    private fun RouteProgress.stepDurationRemaining(): Double =
        currentLegProgress?.currentStepProgress?.durationRemaining ?: durationRemaining

    private fun Double.formattedDistance(): Double {
        val options = checkNotNull(distanceFormatterOptions)
        return MapboxDistanceUtil.formatDistance(
            this,
            options.roundingIncrement,
            options.unitType,
            options.locale,
        )
    }

    private fun Double.hasMeaningfulChangeFrom(previous: Double): Boolean =
        kotlin.math.abs(toRemainingTimeSeconds() - previous.toRemainingTimeSeconds()) >=
            MEANINGFUL_TIME_CHANGE_SECONDS

    private fun Double.toRemainingTimeSeconds(): Long =
        if (isFinite() && this >= 0.0) toLong() else UNKNOWN_REMAINING_TIME_SECONDS

    private companion object {
        private const val LOG_CATEGORY = "MapboxCarNavigationManager"
        private const val TRIP_UPDATE_INTERVAL_MILLIS = 1_000L
        private const val MEANINGFUL_TIME_CHANGE_SECONDS = 10L
        private const val UNKNOWN_REMAINING_TIME_SECONDS = -1L
    }
}
