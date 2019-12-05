package com.mapbox.services.android.navigation.v5.navigation

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.location.Location
import android.os.Build
import android.os.IBinder
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigator.Navigator
import com.mapbox.services.android.navigation.BuildConfig
import com.mapbox.services.android.navigation.v5.internal.navigation.*
import com.mapbox.services.android.navigation.v5.internal.navigation.NavigationService.LocalBinder
import com.mapbox.services.android.navigation.v5.internal.navigation.metrics.FeedbackEvent.FeedbackSource
import com.mapbox.services.android.navigation.v5.internal.navigation.metrics.FeedbackEvent.FeedbackType
import com.mapbox.services.android.navigation.v5.internal.utils.ValidationUtils.Companion.validDirectionsRoute
import com.mapbox.services.android.navigation.v5.location.RawLocationListener
import com.mapbox.services.android.navigation.v5.milestone.BannerInstructionMilestone
import com.mapbox.services.android.navigation.v5.milestone.Milestone
import com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener
import com.mapbox.services.android.navigation.v5.milestone.VoiceInstructionMilestone
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.BANNER_INSTRUCTION_MILESTONE_ID
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.VOICE_INSTRUCTION_MILESTONE_ID
import com.mapbox.services.android.navigation.v5.navigation.NavigationLibraryLoader.Companion.load
import com.mapbox.services.android.navigation.v5.navigation.camera.Camera
import com.mapbox.services.android.navigation.v5.navigation.metrics.MapboxMetricsReporter
import com.mapbox.services.android.navigation.v5.navigation.metrics.MapboxMetricsReporter.disable
import com.mapbox.services.android.navigation.v5.navigation.metrics.MapboxMetricsReporter.init
import com.mapbox.services.android.navigation.v5.offroute.OffRoute
import com.mapbox.services.android.navigation.v5.offroute.OffRouteListener
import com.mapbox.services.android.navigation.v5.route.FasterRoute
import com.mapbox.services.android.navigation.v5.route.FasterRouteListener
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import com.mapbox.services.android.navigation.v5.snap.Snap
import timber.log.Timber
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A MapboxNavigation class for interacting with and customizing a navigation session.
 *
 *
 * Instance of this class are used to setup, customize, start, and end a navigation session.
 *
 * @see [Navigation documentation](https://www.mapbox.com/android-docs/navigation/)
 *
 * @since 0.1.0
 */
class MapboxNavigation : ServiceConnection {
    // TODO public?
    var eventDispatcher: NavigationEventDispatcher = NavigationEventDispatcher()
    private var navigationEngineFactory: NavigationEngineFactory
    private var navigationTelemetry = NavigationTelemetry
    private var navigationService: NavigationService? = null
    private lateinit var mapboxNavigator: MapboxNavigator
    // TODO public?
    lateinit var route: DirectionsRoute
        private set
    private var options: MapboxNavigationOptions
    private var _locationEngine: LocationEngine
    val locationEngine: LocationEngine
        get() {
            return _locationEngine
        }
    private lateinit var freeDriveLocationUpdater: FreeDriveLocationUpdater
    private lateinit var locationEngineRequest: LocationEngineRequest
    private val _milestones = mutableSetOf<Milestone>()
    val milestones: Set<Milestone>
        get() {
            return _milestones
        }
    private val accessToken: String
    private var applicationContext: Context
    private val isBound = AtomicBoolean(false)
    private lateinit var routeRefresher: RouteRefresher
    private val isFreeDriveEnabled = AtomicBoolean(false)
    private val isFreeDriveConfigured = AtomicBoolean(false)
    private val isActiveGuidanceOnGoing = AtomicBoolean(false)
    private var serviceIntent: Intent


    companion object {
        private const val MAPBOX_NAVIGATION_USER_AGENT_BASE = "mapbox-navigation-android"
        private const val MAPBOX_NAVIGATION_UI_USER_AGENT_BASE = "mapbox-navigation-ui-android"
        private const val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 1000
        private const val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS: Long = 500
        private fun obtainUserAgent(options: MapboxNavigationOptions): String {
            return if (options.isFromNavigationUi) {
                MAPBOX_NAVIGATION_UI_USER_AGENT_BASE + BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME
            } else {
                MAPBOX_NAVIGATION_USER_AGENT_BASE + BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME
            }
        }

        init {
            load()
        }
    }
    /**
     * Constructs a new instance of this class using a custom built options class. Building a custom
     * [MapboxNavigationOptions] object and passing it in allows you to further customize the
     * user experience. While many of the default values have been tested thoroughly, you might find
     * that your app requires special tweaking. Once this class is initialized, the options specified
     * through the options class cannot be modified.
     *
     *
     * Initialization will also add the default milestones and create a new location engine
     * which will be used during navigation unless a different engine gets passed in through
     * [.setLocationEngine].
     *
     *
     * @param context     required in order to create and bind the navigation service
     * @param options     a custom built `MapboxNavigationOptions` class
     * @param accessToken a valid Mapbox access token
     * @see MapboxNavigationOptions
     *
     * @since 0.5.0
     */
    /**
     * Constructs a new instance of this class using the default options. This should be used over
     * [.MapboxNavigation] if all the default options
     * fit your needs.
     *
     *
     * Initialization will also add the default milestones and create a new location engine
     * which will be used during navigation unless a different engine gets passed in through
     * [.setLocationEngine].
     *
     *
     * @param context     required in order to create and bind the navigation service
     * @param accessToken a valid Mapbox access token
     * @since 0.5.0
     */
    @JvmOverloads
    constructor(context: Context, accessToken: String,
                options: MapboxNavigationOptions = MapboxNavigationOptions.Builder().build()) {
        applicationContext = context
        this.accessToken = accessToken
        this.options = options
        this.navigationEngineFactory = NavigationEngineFactory()
        this.serviceIntent = Intent(applicationContext, NavigationService::class.java)
        _locationEngine = LocationEngineProvider.getBestLocationEngine(context)
        initialize(context)
    }

    /**
     * Constructs a new instance of this class using a custom built options class. Building a custom
     * [MapboxNavigationOptions] object and passing it in allows you to further customize the
     * user experience. Once this class is initialized, the options specified
     * through the options class cannot be modified.
     *
     * @param context        required in order to create and bind the navigation service
     * @param accessToken    a valid Mapbox access token
     * @param options        a custom built `MapboxNavigationOptions` class
     * @param locationEngine a LocationEngine to provide Location updates
     * @see MapboxNavigationOptions
     *
     * @since 0.19.0
     */
    constructor(context: Context, accessToken: String,
                options: MapboxNavigationOptions, locationEngine: LocationEngine) {
        applicationContext = context
        this.accessToken = accessToken
        this.options = options
        initialize(context)
        this.navigationEngineFactory = NavigationEngineFactory()
        this.serviceIntent = Intent(applicationContext, NavigationService::class.java)
        this._locationEngine = locationEngine
    }

    // TODO public?
// Package private (no modifier) for testing purposes
    constructor(context: Context, accessToken: String,
                navigationTelemetry: NavigationTelemetry, locationEngine: LocationEngine,
                mapboxNavigator: MapboxNavigator) {
        applicationContext = context
        this.accessToken = accessToken
        options = MapboxNavigationOptions.Builder().build()
        this.navigationTelemetry = navigationTelemetry
        this._locationEngine = locationEngine
        this.mapboxNavigator = mapboxNavigator
        this.navigationEngineFactory = NavigationEngineFactory()
        this.serviceIntent = Intent(applicationContext, NavigationService::class.java)
        initializeForTest(context)
    }

    // Package private (no modifier) for testing purposes
    internal constructor(context: Context, accessToken: String,
                         options: MapboxNavigationOptions, navigationTelemetry: NavigationTelemetry,
                         locationEngine: LocationEngine, navigator: Navigator,
                         freeDriveLocationUpdater: FreeDriveLocationUpdater?) {
        applicationContext = context
        this.accessToken = accessToken
        this.options = options
        this.navigationTelemetry = navigationTelemetry
        this._locationEngine = locationEngine
        this.navigationEngineFactory = NavigationEngineFactory()
        mapboxNavigator = MapboxNavigator(navigator)
        this.serviceIntent = Intent(applicationContext, NavigationService::class.java)
        initializeForTest(context, freeDriveLocationUpdater)
    }
    // Lifecycle
    /**
     * Critical to place inside your navigation activity so that when your application gets destroyed
     * the navigation service unbinds and gets destroyed, preventing any memory leaks. Calling this
     * also removes all listeners that have been attached.
     */
    fun onDestroy() {
        killNavigation()
        removeOffRouteListener(null)
        removeProgressChangeListener(null)
        removeMilestoneEventListener(null)
        removeNavigationEventListener(null)
        removeFasterRouteListener(null)
        removeRawLocationListener(null)
        removeEnhancedLocationListener(null)
    }
    // Public APIs
    /**
     * Navigation [Milestone]s provide a powerful way to give your user instructions at custom
     * defined locations along their route. Default milestones are automatically added unless
     * [MapboxNavigationOptions.getDefaultMilestonesEnabled] is set to false but they can also
     * be individually removed using the [.removeMilestone] API. Once a custom
     * milestone is built, it will need to be passed into the navigation SDK through this method.
     *
     *
     * Milestones can only be added once and must be removed and added back if any changes are
     * desired.
     *
     *
     * @param milestone a custom built milestone
     * @since 0.4.0
     */
    fun addMilestone(milestone: Milestone) {
        val milestoneAdded = _milestones.add(milestone)
        if (!milestoneAdded) {
            Timber.w("Milestone has already been added to the stack.")
        }
    }

    /**
     * Adds the given list of [Milestone] to be triggered during navigation.
     *
     *
     * Milestones can only be added once and must be removed and added back if any changes are
     * desired.
     *
     *
     * @param milestones a list of custom built milestone
     * @since 0.14.0
     */
    fun addMilestones(milestones: List<Milestone>) {
        val milestonesAdded = this._milestones.addAll(milestones)
        if (!milestonesAdded) {
            Timber.w("These milestones have already been added to the stack.")
        }
    }

    /**
     * Remove a specific milestone by passing in the instance of it. Removal of all the milestones can
     * be achieved by passing in null rather than a specific milestone.
     *
     * @param milestone a milestone you'd like to have removed or null if you'd like to remove all
     * milestones
     * @since 0.4.0
     */
    // Public exposed for usage outside SDK
    fun removeMilestone(milestone: Milestone?) {
        if (milestone == null) {
            _milestones.clear()
            return
        } else if (!milestones.contains(milestone)) {
            Timber.w("Milestone attempting to remove does not exist in stack.")
            return
        }
        _milestones.remove(milestone)
    }

    /**
     * Remove a specific milestone by passing in the identifier associated with the milestone you'd
     * like to remove. If the identifier passed in does not match one of the milestones in the list,
     * a warning will return in the log.
     *
     * @param milestoneIdentifier identifier matching one of the milestones
     * @since 0.5.0
     */
    // Public exposed for usage outside SDK
    fun removeMilestone(milestoneIdentifier: Int) {
        for (milestone in milestones) {
            if (milestoneIdentifier == milestone.identifier) {
                removeMilestone(milestone)
                return
            }
        }
        Timber.w("No milestone found with the specified identifier.")
    }

    /**
     * Navigation needs an instance of location engine in order to acquire user location information
     * and handle events based off of the current information. By default, a [LocationEngine] is
     * created using [LocationEngineProvider.getBestLocationEngine].
     *
     *
     * In ideal conditions, the Navigation SDK will receive location updates once every second with
     * mild to high horizontal accuracy. The location update must also contain all information an
     * Android location object would expect including bearing, speed, timestamp, and
     * latitude/longitude.
     *
     *
     *
     * This method can be called during an active navigation session.  The active [LocationEngine] will be
     * replaced and the new one (passed via this method) will be activated with the current [LocationEngineRequest].
     *
     *
     * @param locationEngine a [LocationEngine] used for the navigation session
     * @since 0.1.0
     */
    fun setLocationEngine(locationEngine: LocationEngine) {
        this._locationEngine = locationEngine
        freeDriveLocationUpdater.updateLocationEngine(locationEngine)
        // Setup telemetry with new engine
        navigationTelemetry.updateLocationEngineNameAndSimulation(locationEngine)
        // Notify service to get new location engine.
        if (isBound.get()) {
            navigationService?.updateLocationEngine(locationEngine)
        }
    }

    /**
     * This method updates the [LocationEngineRequest] that is used with the [LocationEngine].
     *
     *
     * If a request is not provided via [MapboxNavigation.setLocationEngineRequest],
     * a default will be provided with optimized settings for navigation.
     *
     *
     *
     * This method can be called during an active navigation session.  The active [LocationEngineRequest] will be
     * replaced and the new one (passed via this method) will be activated with the current [LocationEngine].
     *
     *
     * @param locationEngineRequest to be used with the current [LocationEngine]
     */
    fun setLocationEngineRequest(locationEngineRequest: LocationEngineRequest) {
        this.locationEngineRequest = locationEngineRequest
        freeDriveLocationUpdater.updateLocationEngineRequest(locationEngineRequest)
        if (isBound.get()) {
            navigationService?.updateLocationEngineRequest(locationEngineRequest)
        }
    }

    /**
     * Calling this method begins a new navigation session using the provided directions route. This API is
     * also intended to be used when a reroute occurs passing in the updated directions route.
     *
     *
     * On initial start of the navigation session, the navigation services gets created and bound to
     * your activity. Unless disabled, a notification will be displayed to the user and will remain
     * until the service stops running in the background.
     *
     *
     * The directions route should be acquired by building a [NavigationRoute] object and
     * calling [NavigationRoute.getRoute] on it. Using navigation route request a
     * route with the required parameters needed while at the same time, allowing for flexibility in
     * other parts of the request.
     *
     *
     * @param directionsRoute a [DirectionsRoute] that makes up the path your user should
     * traverse along
     * @since 0.1.0
     */
    fun startNavigation(directionsRoute: DirectionsRoute) {
        startNavigationWith(directionsRoute, DirectionsRouteType.NEW_ROUTE)
    }

    /**
     * Calling this method with [DirectionsRouteType.NEW_ROUTE] begins a new navigation session using the
     * provided directions route.  If called with [DirectionsRouteType.FRESH_ROUTE], only leg annotation data
     * will be update - can be used with [RouteRefresh].
     *
     * @param directionsRoute a [DirectionsRoute] that makes up the path your user should
     * traverse along
     * @param routeType       either new or fresh to determine what data navigation should consider
     * @see MapboxNavigation.startNavigation
     */
    fun startNavigation(directionsRoute: DirectionsRoute, routeType: DirectionsRouteType) {
        startNavigationWith(directionsRoute, routeType)
    }

    /**
     * Call this when the navigation session needs to end before the user reaches their final
     * destination.
     *
     *
     * Ending the navigation session ends and unbinds the navigation service meaning any milestone,
     * progress change, or off-route listeners will not be invoked anymore. A call returning false
     * will occur to [NavigationEventListener.onRunning] to notify you when the service
     * ends.
     *
     *
     * @since 0.1.0
     */
    fun stopNavigation() {
        isActiveGuidanceOnGoing.set(false)
        if (isFreeDriveEnabled.get()) {
            enableFreeDrive()
        }
        stopNavigationService()
    }

    private fun killNavigation() {
        killFreeDrive()
        stopNavigationService()
    }

    private fun killFreeDrive() {
        if (isFreeDriveConfigured.get()) {
            freeDriveLocationUpdater.kill()
        }
    }

    private fun stopNavigationService() {
        Timber.d("MapboxNavigation stopped")
        if (isBound.compareAndSet(true, false)) {
            navigationTelemetry.stopSession()
            applicationContext.unbindService(this)
            navigationService?.endNavigation()
            disable()
            navigationService?.stopSelf()
            eventDispatcher.onNavigationEvent(false)
        }
    }
    // Listeners
    /**
     * This adds a new milestone event listener which is invoked when a milestone gets triggered. If
     * more then one milestone gets triggered on a location update, each milestone event listener will
     * be invoked for each of those milestones. This is important to consider if you are using voice
     * instructions since this would cause multiple instructions to be said at once. Ideally the
     * milestones setup should avoid triggering too close to each other.
     *
     *
     * It is not possible to add the same listener implementation more then once and a warning will be
     * printed in the log if attempted.
     *
     *
     * @param milestoneEventListener an implementation of `MilestoneEventListener` which hasn't
     * already been added
     * @see MilestoneEventListener
     *
     * @since 0.4.0
     */
    fun addMilestoneEventListener(milestoneEventListener: MilestoneEventListener) {
        eventDispatcher.addMilestoneEventListener(milestoneEventListener)
    }

    /**
     * This removes a specific milestone event listener by passing in the instance of it or you can
     * pass in null to remove all the listeners. When [.onDestroy] is called, all listeners
     * get removed automatically, removing the requirement for developers to manually handle this.
     *
     *
     * If the listener you are trying to remove does not exist in the list, a warning will be printed
     * in the log.
     *
     *
     * @param milestoneEventListener an implementation of `MilestoneEventListener` which
     * currently exist in the milestoneEventListener list
     * @see MilestoneEventListener
     *
     * @since 0.4.0
     */
    // Public exposed for usage outside SDK
    fun removeMilestoneEventListener(milestoneEventListener: MilestoneEventListener?) {
        eventDispatcher.removeMilestoneEventListener(milestoneEventListener)
    }

    /**
     * This adds a new progress change listener which is invoked when a location change occurs and the
     * navigation engine successfully runs it's calculations on it.
     *
     *
     * It is not possible to add the same listener implementation more then once and a warning will be
     * printed in the log if attempted.
     *
     *
     * @param progressChangeListener an implementation of `ProgressChangeListener` which hasn't
     * already been added
     * @see ProgressChangeListener
     *
     * @since 0.1.0
     */
    fun addProgressChangeListener(progressChangeListener: ProgressChangeListener) {
        eventDispatcher.addProgressChangeListener(progressChangeListener)
    }

    /**
     * This removes a specific progress change listener by passing in the instance of it or you can
     * pass in null to remove all the listeners. When [.onDestroy] is called, all listeners
     * get removed automatically, removing the requirement for developers to manually handle this.
     *
     *
     * If the listener you are trying to remove does not exist in the list, a warning will be printed
     * in the log.
     *
     *
     * @param progressChangeListener an implementation of `ProgressChangeListener` which
     * currently exist in the progressChangeListener list
     * @see ProgressChangeListener
     *
     * @since 0.1.0
     */
    fun removeProgressChangeListener(progressChangeListener: ProgressChangeListener?) {
        eventDispatcher.removeProgressChangeListener(progressChangeListener)
    }

    /**
     * This adds a new off route listener which is invoked when the devices location veers off the
     * route and the specified criteria's in [MapboxNavigationOptions] have been met.
     *
     *
     * The behavior that causes this listeners callback to get invoked vary depending on whether a
     * custom off route engine has been set using [.setOffRouteEngine].
     *
     *
     * It is not possible to add the same listener implementation more then once and a warning will be
     * printed in the log if attempted.
     *
     *
     * @param offRouteListener an implementation of `OffRouteListener` which hasn't already been
     * added
     * @see OffRouteListener
     *
     * @since 0.2.0
     */
    fun addOffRouteListener(offRouteListener: OffRouteListener) {
        eventDispatcher.addOffRouteListener(offRouteListener)
    }

    /**
     * This removes a specific off route listener by passing in the instance of it or you can pass in
     * null to remove all the listeners. When [.onDestroy] is called, all listeners
     * get removed automatically, removing the requirement for developers to manually handle this.
     *
     *
     * If the listener you are trying to remove does not exist in the list, a warning will be printed
     * in the log.
     *
     *
     * @param offRouteListener an implementation of `OffRouteListener` which currently exist in
     * the offRouteListener list
     * @see OffRouteListener
     *
     * @since 0.2.0
     */
    // Public exposed for usage outside SDK
    fun removeOffRouteListener(offRouteListener: OffRouteListener?) {
        eventDispatcher.removeOffRouteListener(offRouteListener)
    }

    /**
     * This adds a new navigation event listener which is invoked when navigation service begins
     * running in the background and again when the service gets destroyed.
     *
     *
     * It is not possible to add the same listener implementation more then once and a warning will be
     * printed in the log if attempted.
     *
     *
     * @param navigationEventListener an implementation of `NavigationEventListener` which
     * hasn't already been added
     * @see NavigationEventListener
     *
     * @since 0.1.0
     */
    fun addNavigationEventListener(navigationEventListener: NavigationEventListener) {
        eventDispatcher.addNavigationEventListener(navigationEventListener)
    }

    /**
     * This removes a specific navigation event listener by passing in the instance of it or you can
     * pass in null to remove all the listeners. When [.onDestroy] is called, all listeners
     * get removed automatically, removing the requirement for developers to manually handle this.
     *
     *
     * If the listener you are trying to remove does not exist in the list, a warning will be printed
     * in the log.
     *
     *
     * @param navigationEventListener an implementation of `NavigationEventListener` which
     * currently exist in the navigationEventListener list
     * @see NavigationEventListener
     *
     * @since 0.1.0
     */
    fun removeNavigationEventListener(navigationEventListener: NavigationEventListener?) {
        eventDispatcher.removeNavigationEventListener(navigationEventListener)
    }

    /**
     * This adds a new faster route listener which is invoked when a new, faster [DirectionsRoute]
     * has been retrieved by the specified criteria in [FasterRoute].
     *
     *
     * The behavior that causes this listeners callback to get invoked vary depending on whether a
     * custom faster route engine has been set using [.setFasterRouteEngine].
     *
     *
     * It is not possible to add the same listener implementation more then once and a warning will be
     * printed in the log if attempted.
     *
     *
     * @param fasterRouteListener an implementation of `FasterRouteListener`
     * @see FasterRouteListener
     *
     * @since 0.9.0
     */
    fun addFasterRouteListener(fasterRouteListener: FasterRouteListener) {
        eventDispatcher.addFasterRouteListener(fasterRouteListener)
    }

    /**
     * This removes a specific faster route listener by passing in the instance of it or you can pass in
     * null to remove all the listeners. When [.onDestroy] is called, all listeners
     * get removed automatically, removing the requirement for developers to manually handle this.
     *
     *
     * If the listener you are trying to remove does not exist in the list, a warning will be printed
     * in the log.
     *
     *
     * @param fasterRouteListener an implementation of `FasterRouteListener` which currently exist in
     * the fasterRouteListeners list
     * @see FasterRouteListener
     *
     * @since 0.9.0
     */
    // Public exposed for usage outside SDK
    fun removeFasterRouteListener(fasterRouteListener: FasterRouteListener?) {
        eventDispatcher.removeFasterRouteListener(fasterRouteListener)
    }

    /**
     * This adds a new raw location listener which is invoked when a new [android.location.Location]
     * has been pushed by the [LocationEngine].
     *
     *
     * It is not possible to add the same listener implementation more then once and a warning will be
     * printed in the log if attempted.
     *
     * @param rawLocationListener an implementation of `RawLocationListener`
     */
    fun addRawLocationListener(rawLocationListener: RawLocationListener) {
        eventDispatcher.addRawLocationListener(rawLocationListener)
    }

    /**
     * This removes a specific raw location listener by passing in the instance of it or you can pass in
     * null to remove all the listeners. When [.onDestroy] is called, all listeners
     * get removed automatically, removing the requirement for developers to manually handle this.
     *
     *
     * If the listener you are trying to remove does not exist in the list, a warning will be printed
     * in the log.
     *
     * @param rawLocationListener an implementation of `RawLocationListener`
     */
    fun removeRawLocationListener(rawLocationListener: RawLocationListener?) {
        eventDispatcher.removeRawLocationListener(rawLocationListener)
    }

    /**
     * This adds a new enhanced location listener which is invoked when the best enhanced
     * [android.location.Location] has been pushed. Either snapped (active guidance),
     * map matched (free drive) or raw.
     *
     *
     * The behavior that causes this listeners callback to get invoked vary depending on whether
     * free drive has been enabled using [.enableFreeDrive] or disabled using
     * [.disableFreeDrive].
     *
     *
     * It is not possible to add the same listener implementation more then once and a warning will be
     * printed in the log if attempted.
     *
     *
     * @param enhancedLocationListener an implementation of `EnhancedLocationListener`
     */
    fun addEnhancedLocationListener(enhancedLocationListener: EnhancedLocationListener) {
        eventDispatcher.addEnhancedLocationListener(enhancedLocationListener)
    }

    /**
     * This removes a specific enhanced location listener by passing in the instance of it or you can
     * pass in null to remove all the listeners. When [.onDestroy] is called, all listeners
     * get removed automatically, removing the requirement for developers to manually handle this.
     *
     *
     * If the listener you are trying to remove does not exist in the list, a warning will be printed
     * in the log.
     *
     * @param enhancedLocationListener an implementation of `EnhancedLocationListener`
     */
    fun removeEnhancedLocationListener(enhancedLocationListener: EnhancedLocationListener?) {
        eventDispatcher.removeEnhancedLocationListener(enhancedLocationListener)
    }

    /**
     * Calling this method enables free drive mode.
     *
     *
     * Best enhanced [Location] updates are received if an [EnhancedLocationListener] has been
     * added using [.addEnhancedLocationListener].
     */
    fun enableFreeDrive() {
        isFreeDriveEnabled.set(true)
        if (!isFreeDriveConfigured.get()) {
            val tilePath = File(applicationContext.filesDir, "2019_04_13-00_00_11")
                    .absolutePath
            freeDriveLocationUpdater.configure(tilePath, object : OnOfflineTilesConfiguredCallback {
                override fun onConfigured(numberOfTiles: Int) {
                    Timber.d("DEBUG: onConfigured %d", numberOfTiles)
                    isFreeDriveConfigured.set(true)
                    if (!isActiveGuidanceOnGoing.get() && isFreeDriveEnabled.get()) {
                        freeDriveLocationUpdater.start()
                    }
                }

                override fun onConfigurationError(error: OfflineError) {
                    Timber.e("Free drive: onConfigurationError %s", error.message)
                    isFreeDriveConfigured.set(false)
                }
            })
        } else {
            if (!isActiveGuidanceOnGoing.get()) {
                freeDriveLocationUpdater.start()
            }
        }
    }

    /**
     * Calling this method disables free drive mode.
     */
    fun disableFreeDrive() {
        isFreeDriveEnabled.set(false)
        if (isFreeDriveConfigured.get()) {
            freeDriveLocationUpdater.stop()
        }
    }
    // Custom engines

    /**
     * Returns the current camera engine used to configure the camera position while routing. By default,
     * a [SimpleCamera] is used.
     *
     * @return camera engine used to configure camera position while routing
     * @since 0.10.0
     */
    /**
     * Navigation uses a camera engine to determine the camera position while routing.
     * By default, it uses a [SimpleCamera]. If you would like to customize how the camera is
     * positioned, create a new [Camera] and set it here.
     *
     * @param cameraEngine camera engine used to configure camera position while routing
     * @since 0.10.0
     */
    var cameraEngine: Camera
        get() = navigationEngineFactory.retrieveCameraEngine()
        set(cameraEngine) {
            navigationEngineFactory.updateCameraEngine(cameraEngine)
        }

    /**
     * This API is used to pass in a custom implementation of the snapping logic, A default
     * snap-to-route engine is attached when this class is first initialized; setting a custom one
     * will replace it with your own implementation.
     *
     *
     * In general, snap logic can be anything that modifies the device's true location. For more
     * information see the implementation notes in [Snap].
     *
     *
     * The engine can be changed at anytime, even during a navigation session.
     *
     *
     * @param snapEngine a custom implementation of the `Snap` class
     * @see Snap
     *
     * @since 0.5.0
     */
    // Public exposed for usage outside SDK
    /**
     * This will return the currently set snap engine which will or is being used during the
     * navigation session. If no snap engine has been set yet, the default engine will be returned.
     *
     * @return the snap engine currently set and will/is being used for the navigation session
     * @see Snap
     *
     * @since 0.5.0
     */
    // Public exposed for usage outside SDK
    var snapEngine: Snap
        get() = navigationEngineFactory.retrieveSnapEngine()
        set(snapEngine) {
            navigationEngineFactory.updateSnapEngine(snapEngine)
        }

    /**
     * This will return the currently set off-route engine which will or is being used during the
     * navigation session. If no off-route engine has been set yet, the default engine will be
     * returned.
     *
     * @return the off-route engine currently set and will/is being used for the navigation session
     * @see OffRoute
     *
     * @since 0.5.0
     */
    /**
     * This API is used to pass in a custom implementation of the off-route logic, A default
     * off-route detection engine is attached when this class is first initialized; setting a custom
     * one will replace it with your own implementation.
     *
     *
     * The engine can be changed at anytime, even during a navigation session.
     *
     *
     * @param offRouteEngine a custom implementation of the `OffRoute` class
     * @see OffRoute
     *
     * @since 0.5.0
     */
    // Public exposed for usage outside SDK
    var offRouteEngine: OffRoute
        get() = navigationEngineFactory.retrieveOffRouteEngine()
        set(offRouteEngine) {
            navigationEngineFactory.updateOffRouteEngine(offRouteEngine)
        }

    /**
     * This will return the currently set faster-route engine which will or is being used during the
     * navigation session. If no faster-route engine has been set yet, the default engine will be
     * returned.
     *
     * @return the faster-route engine currently set and will/is being used for the navigation session
     * @see FasterRoute
     *
     * @since 0.9.0
     */
    /**
     * This API is used to pass in a custom implementation of the faster-route detection logic, A default
     * faster-route detection engine is attached when this class is first initialized; setting a custom
     * one will replace it with your own implementation.
     *
     *
     * The engine can be changed at anytime, even during a navigation session.
     *
     *
     * @param fasterRouteEngine a custom implementation of the [FasterRoute] class
     * @see FasterRoute
     *
     * @since 0.9.0
     */
    // Public exposed for usage outside SDK
    var fasterRouteEngine: FasterRoute
        get() = navigationEngineFactory.retrieveFasterRouteEngine()
        set(fasterRouteEngine) {
            navigationEngineFactory.updateFasterRouteEngine(fasterRouteEngine)
        }

    /**
     * Creates a new [FeedbackEvent] with a given type, description, and source.
     *
     *
     * Returns a [String] feedbackId that can be used to update or cancel this feedback event.
     * There is a 20 second time period set after this method is called to do so.
     *
     * @param feedbackType from list of set feedback types
     * @param description  an option description to provide more detail about the feedback
     * @param source       either from the drop-in UI or a reroute
     * @return String feedbackId
     * @since 0.7.0
     */
    fun recordFeedback(@FeedbackType feedbackType: String,
                       description: String, @FeedbackSource source: String): String {
        return navigationTelemetry.recordFeedbackEvent(feedbackType, description, source)
    }

    /**
     * Updates an existing feedback event generated by [MapboxNavigation.recordFeedback].
     *
     *
     * Uses a feedback ID to find the correct event and then adjusts the feedbackType and description.
     *
     * @param feedbackId   generated from [MapboxNavigation.recordFeedback]
     * @param feedbackType from list of set feedback types
     * @param description  an optional description to provide more detail about the feedback
     * @param screenshot   an optional encoded screenshot to provide more detail about the feedback
     * @since 0.8.0
     */
    fun updateFeedback(feedbackId: String, @FeedbackType feedbackType: String,
                       description: String, screenshot: String) {
        navigationTelemetry.updateFeedbackEvent(feedbackId, feedbackType, description, screenshot)
    }

    /**
     * Cancels an existing feedback event generated by [MapboxNavigation.recordFeedback].
     *
     *
     * Uses a feedback ID to find the correct event and then cancels it (will no longer be recorded).
     *
     * @param feedbackId generated from [MapboxNavigation.recordFeedback]
     * @since 0.7.0
     */
    fun cancelFeedback(feedbackId: String) {
        navigationTelemetry.cancelFeedback(feedbackId)
    }

    /**
     * Use this method to update the leg index of the current [DirectionsRoute]
     * being traveled along.
     *
     *
     * An index passed here that is not valid will be ignored.  Please note, the leg index
     * will automatically increment by default.  To disable this,
     * use [MapboxNavigationOptions.enableAutoIncrementLegIndex].
     *
     * @param legIndex to be set
     * @return true if leg index updated, false otherwise
     */
    fun updateRouteLegIndex(legIndex: Int): Boolean {
        if (checkInvalidLegIndex(legIndex)) {
            return false
        }
        mapboxNavigator.updateLegIndex(legIndex)
        return true
    }

    fun retrieveHistory(): String {
        return mapboxNavigator.retrieveHistory()
    }

    fun toggleHistory(isEnabled: Boolean) {
        mapboxNavigator.toggleHistory(isEnabled)
    }

    fun addHistoryEvent(eventType: String, eventJsonProperties: String) {
        mapboxNavigator.addHistoryEvent(eventType, eventJsonProperties)
    }

    fun retrieveSsmlAnnouncementInstruction(index: Int): String {
        return mapboxNavigator.retrieveVoiceInstruction(index)?.ssmlAnnouncement ?: " "
    }

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        Timber.d("Connected to service.")
        if (isBound.compareAndSet(false, true)) {
            val binder = service as LocalBinder
            navigationService = binder.getService()
            navigationService?.startNavigation(this)
        }
    }

    override fun onServiceDisconnected(name: ComponentName) {
        Timber.d("Disconnected from service.")
        navigationService = null
        isBound.set(false)
    }

    // TODO public?
    fun obtainAccessToken(): String {
        return accessToken
    }

    // TODO public?
    fun getMilestones(): List<Milestone> {
        return ArrayList(milestones)
    }

    // TODO public?
    fun options(): MapboxNavigationOptions {
        return options
    }

    // TODO public?
    fun retrieveEngineFactory(): NavigationEngineFactory {
        return navigationEngineFactory
    }

    // TODO public?
    fun retrieveMapboxNavigator(): MapboxNavigator {
        return mapboxNavigator
    }

    // TODO public?
    fun retrieveLocationEngineRequest(): LocationEngineRequest {
        return locationEngineRequest
    }

    // TODO public?
    fun retrieveRouteRefresher(): RouteRefresher? {
        return routeRefresher
    }

    private fun initializeForTest(context: Context, freeDrive: FreeDriveLocationUpdater? = null) { // Initialize event dispatcher and add internal listeners
        eventDispatcher = NavigationEventDispatcher()
        navigationEngineFactory = NavigationEngineFactory()
        routeRefresher = RouteRefresher(this, RouteRefresh(accessToken))
        _locationEngine = LocationEngineProvider.getBestLocationEngine(context)
        this.locationEngineRequest = LocationEngineRequest.Builder(UPDATE_INTERVAL_IN_MILLISECONDS)
                .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                .setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS)
                .build()

        val offlineNavigator = OfflineNavigator(mapboxNavigator.navigator,
                "2019_04_13-00_00_11", "https://api-routing-tiles-staging.tilestream.net",
                accessToken)
        freeDriveLocationUpdater = freeDrive
                ?: FreeDriveLocationUpdater(_locationEngine, locationEngineRequest,
                        eventDispatcher, mapboxNavigator, offlineNavigator,
                        Executors.newSingleThreadScheduledExecutor())
        initializeTelemetry(context)
        // Create and add default milestones if enabled.
        _milestones.clear()
        if (options.defaultMilestonesEnabled) {
            addMilestone(VoiceInstructionMilestone.Builder().setIdentifier(VOICE_INSTRUCTION_MILESTONE_ID).build())
            addMilestone(BannerInstructionMilestone.Builder().setIdentifier(BANNER_INSTRUCTION_MILESTONE_ID).build())
        }
    }

    /**
     * In-charge of initializing all variables needed to begin a navigation session. Many values can
     * be changed later on using their corresponding setter. An internal progressChangeListeners used
     * to prevent users from removing it.
     */
    private fun initialize(context: Context) {
        navigationEngineFactory = NavigationEngineFactory()
        _locationEngine = LocationEngineProvider.getBestLocationEngine(context)
        mapboxNavigator = MapboxNavigator(configureNavigator())
        // Initialize event dispatcher and add internal listeners
        eventDispatcher = NavigationEventDispatcher()
        eventDispatcher.addProgressChangeListener(object : ProgressChangeListener {
            override fun onProgressChange(location: Location, routeProgress: RouteProgress) {
                eventDispatcher.onEnhancedLocationUpdate(location)
            }
        })
        this.locationEngineRequest = LocationEngineRequest.Builder(UPDATE_INTERVAL_IN_MILLISECONDS)
                .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                .setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS)
                .build()

        val offlineNavigator = OfflineNavigator(mapboxNavigator.navigator,
                "2019_04_13-00_00_11", "https://api-routing-tiles-staging.tilestream.net",
                accessToken)
        freeDriveLocationUpdater = FreeDriveLocationUpdater(_locationEngine, locationEngineRequest,
                eventDispatcher, mapboxNavigator, offlineNavigator,
                Executors.newSingleThreadScheduledExecutor())
        routeRefresher = RouteRefresher(this, RouteRefresh(accessToken))
        initializeTelemetry(applicationContext)
        // Create and add default milestones if enabled.
        _milestones.clear()
        if (options.defaultMilestonesEnabled) {
            addMilestone(VoiceInstructionMilestone.Builder().setIdentifier(VOICE_INSTRUCTION_MILESTONE_ID).build())
            addMilestone(BannerInstructionMilestone.Builder().setIdentifier(BANNER_INSTRUCTION_MILESTONE_ID).build())
        }
    }

    private fun configureNavigator(): Navigator {
        val navigator = Navigator()
        val navigatorConfig = navigator.config
        navigatorConfig.offRouteThreshold = options.offRouteThreshold
        navigatorConfig.offRouteThresholdWhenNearIntersection = options.offRouteThresholdWhenNearIntersection
        navigatorConfig.intersectionRadiusForOffRouteDetection = options.intersectionRadiusForOffRouteDetection
        navigator.setConfig(navigatorConfig)
        return navigator
    }

    private fun initializeTelemetry(context: Context) {
        init(
                context,
                accessToken,
                obtainUserAgent(options)
        )
        navigationTelemetry.initialize(
                applicationContext,
                accessToken,
                this,
                MapboxMetricsReporter
        )
    }

    private fun startNavigationWith(directionsRoute: DirectionsRoute, routeType: DirectionsRouteType) {
        validDirectionsRoute(directionsRoute, options.defaultMilestonesEnabled)
        route = directionsRoute
        mapboxNavigator.updateRoute(directionsRoute, routeType)
        isActiveGuidanceOnGoing.set(true)
        if (!isBound.get()) {
            disableFreeDrive()
            navigationTelemetry.startSession(directionsRoute, _locationEngine)
            startNavigationService()
            eventDispatcher.onNavigationEvent(true)
        } else {
            navigationTelemetry.updateSessionRoute(directionsRoute)
        }
    }

    private fun startNavigationService() {
        val intent = serviceIntent
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            applicationContext.startForegroundService(intent)
        } else {
            applicationContext.startService(intent)
        }
        applicationContext.bindService(intent, this, Context.BIND_AUTO_CREATE)
    }

    private fun checkInvalidLegIndex(legIndex: Int) =
            route.legs()?.size?.let { legSize ->
                if (legIndex < 0 || legIndex > legSize - 1) {
                    Timber.e("Invalid leg index update: %s Current leg index size: %s", legIndex, legSize)
                    true
                } else {
                    false
                }
            } ?: false
}