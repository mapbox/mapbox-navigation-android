package com.mapbox.navigation.core.trip

import android.annotation.SuppressLint
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.replay.route.ReplayRouteSession
import com.mapbox.navigation.core.replay.route.ReplayRouteSessionOptions
import com.mapbox.navigation.core.trip.MapboxTripStarter.Companion.getRegisteredInstance
import com.mapbox.navigation.utils.internal.logI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * The [MapboxTripStarter] makes it simpler to switch between a trip session and replay.
 *
 * This is not able to observe when location permissions change, so you may need to refresh the
 * state with [refreshLocationPermissions]. Location permissions are not required for replay.
 *
 * There should be one instance of this class at a time. For example, an app Activity and car
 * Session will need to use the same instance. That will be done automatically if you use
 * [getRegisteredInstance].
 */
@ExperimentalPreviewMapboxNavigationAPI
class MapboxTripStarter internal constructor() : MapboxNavigationObserver {

    private val tripType = MutableStateFlow<MapboxTripStarterType>(
        MapboxTripStarterType.MapMatching
    )
    private val replayRouteSessionOptions = MutableStateFlow(
        ReplayRouteSessionOptions.Builder().build()
    )
    private val isLocationPermissionGranted = MutableStateFlow(false)
    private var replayRouteTripSession: ReplayRouteSession? = null
    private var mapboxNavigation: MapboxNavigation? = null

    private lateinit var coroutineScope: CoroutineScope

    /**
     * Signals that the [mapboxNavigation] instance is ready for use.
     *
     * @param mapboxNavigation
     */
    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        this.mapboxNavigation = mapboxNavigation
        coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        // Initialize the options to be aware of the location permissions
        val context = mapboxNavigation.navigationOptions.applicationContext
        val granted = PermissionsManager.areLocationPermissionsGranted(context)
        isLocationPermissionGranted.value = granted

        // Observe changes to state
        observeStateFlow(mapboxNavigation).launchIn(coroutineScope)
    }

    /**
     * Signals that the [mapboxNavigation] instance is being detached.
     *
     * @param mapboxNavigation
     */
    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        coroutineScope.cancel()
        onTripDisabled(mapboxNavigation)
        this.mapboxNavigation = null
    }

    /**
     * [enableMapMatching] will not work unless location permissions have been granted. Refresh
     * the location permissions after they are granted to ensure the trip session will start.
     */
    fun refreshLocationPermissions() = apply {
        mapboxNavigation?.navigationOptions?.applicationContext?.let { context ->
            val granted = PermissionsManager.areLocationPermissionsGranted(context)
            isLocationPermissionGranted.value = granted
        }
    }

    /**
     * This is the default mode for the [MapboxTripStarter]. This can be used to disable
     * [enableReplayRoute]. Make sure location permissions have been accepted or this will have no
     * effect on the experience.
     */
    fun enableMapMatching() = apply {
        if (!isLocationPermissionGranted.value) {
            refreshLocationPermissions()
        }
        tripType.value = MapboxTripStarterType.MapMatching
    }

    /**
     * Get the current [ReplayRouteSessionOptions]. This can be used with [enableReplayRoute] to
     * make minor adjustments to the current options.
     */
    fun getReplayRouteSessionOptions(): ReplayRouteSessionOptions = replayRouteSessionOptions.value

    /**
     * Enables a mode where the primary route is simulated by an artificial driver. Set the route
     * with [MapboxNavigation.setNavigationRoutes]. Can be used with [getReplayRouteSessionOptions]
     * to make minor adjustments to the current options.
     *
     * @param options optional options to use for route replay.
     */
    fun enableReplayRoute(
        options: ReplayRouteSessionOptions? = null
    ) = apply {
        options?.let { options -> replayRouteSessionOptions.value = options }
        tripType.value = MapboxTripStarterType.ReplayRoute
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeStateFlow(mapboxNavigation: MapboxNavigation): Flow<*> {
        return tripType.flatMapLatest { tripType ->
            when (tripType) {
                MapboxTripStarterType.ReplayRoute ->
                    replayRouteSessionOptions.onEach { options ->
                        onReplayTripEnabled(mapboxNavigation, options)
                    }
                MapboxTripStarterType.MapMatching ->
                    isLocationPermissionGranted.onEach { granted ->
                        onMapMatchingEnabled(mapboxNavigation, granted)
                    }
            }
        }
    }

    /**
     * Internally called when the trip type has been set to replay route.
     *
     * @param mapboxNavigation
     * @param options parameters for the [ReplayRouteSession]
     */
    private fun onReplayTripEnabled(
        mapboxNavigation: MapboxNavigation,
        options: ReplayRouteSessionOptions
    ) {
        replayRouteTripSession?.onDetached(mapboxNavigation)
        replayRouteTripSession = ReplayRouteSession().also {
            it.setOptions(options)
            it.onAttached(mapboxNavigation)
        }
    }

    /**
     * Internally called when the trip type has been set to map matching.
     *
     * @param mapboxNavigation
     * @param granted true when location permissions are accepted, false otherwise
     */
    @SuppressLint("MissingPermission")
    private fun onMapMatchingEnabled(mapboxNavigation: MapboxNavigation, granted: Boolean) {
        if (granted) {
            replayRouteTripSession?.onDetached(mapboxNavigation)
            replayRouteTripSession = null
            mapboxNavigation.startTripSession()
        } else {
            logI(LOG_CATEGORY) {
                "startTripSession was not called. Accept location permissions and call " +
                    "mapboxTripStarter.refreshLocationPermissions()"
            }
            onTripDisabled(mapboxNavigation)
        }
    }

    /**
     * Internally called when the trip session needs to be stopped.
     *
     * @param mapboxNavigation
     */
    private fun onTripDisabled(mapboxNavigation: MapboxNavigation) {
        replayRouteTripSession?.onDetached(mapboxNavigation)
        replayRouteTripSession = null
        mapboxNavigation.stopTripSession()
    }

    companion object {
        private const val LOG_CATEGORY = "MapboxTripStarter"

        /**
         * Construct an instance without registering to [MapboxNavigationApp].
         */
        @JvmStatic
        fun create() = MapboxTripStarter()

        /**
         * Get the registered instance or create one and register it to [MapboxNavigationApp].
         */
        @JvmStatic
        fun getRegisteredInstance(): MapboxTripStarter = MapboxNavigationApp
            .getObservers(MapboxTripStarter::class)
            .firstOrNull() ?: MapboxTripStarter().also { MapboxNavigationApp.registerObserver(it) }
    }
}
