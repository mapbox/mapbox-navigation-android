package com.mapbox.navigation.core

import android.content.Context
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.annotation.navigation.module.MapboxNavigationModuleType
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.extensions.applyDefaultParams
import com.mapbox.navigation.base.extensions.bearings
import com.mapbox.navigation.base.extensions.ifNonNull
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.base.trip.TripNotification
import com.mapbox.navigation.base.typedef.METRIC
import com.mapbox.navigation.base.typedef.ROUNDING_INCREMENT_FIFTY
import com.mapbox.navigation.base.typedef.TWELVE_HOURS
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.directions.session.RouteObserver
import com.mapbox.navigation.core.module.NavigationModuleProvider
import com.mapbox.navigation.core.trip.service.TripService
import com.mapbox.navigation.core.trip.session.BannerInstructionsObserver
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.OffRouteObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import com.mapbox.navigation.navigator.MapboxNativeNavigatorImpl
import com.mapbox.navigation.trip.notification.NotificationAction
import com.mapbox.navigation.utils.thread.JobControl
import com.mapbox.navigation.utils.thread.ThreadController
import com.mapbox.navigation.utils.thread.monitorChannelWithException
import java.lang.reflect.Field
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.channels.ReceiveChannel

class MapboxNavigation(
    private val context: Context,
    private val accessToken: String?,
    locationEngine: LocationEngine = LocationEngineProvider.getBestLocationEngine(context.applicationContext),
    locationEngineRequest: LocationEngineRequest = LocationEngineRequest.Builder(1000L)
        .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
        .build()
) {

    private val bannerInstructionsObservers = CopyOnWriteArrayList<BannerInstructionsObserver>()
    private val voiceInstructionsObservers = CopyOnWriteArrayList<VoiceInstructionsObserver>()

    private var mainJobController: JobControl = ThreadController.getMainScopeAndRootJob()
    private val directionsSession: DirectionsSession
    private val tripService: TripService
    private val tripSession: TripSession

    private var notificationChannelField: Field? = null

    init {
        ThreadController.init()
        directionsSession = NavigationComponentProvider.createDirectionsSession(
            NavigationModuleProvider.createModule(
                MapboxNavigationModuleType.OffboardRouter,
                ::paramsProvider
            )
        )
        val notification: TripNotification = NavigationModuleProvider.createModule(
            MapboxNavigationModuleType.TripNotification,
            ::paramsProvider
        )
        if (notification.javaClass.name == "com.mapbox.navigation.trip.notification.MapboxTripNotification") {
            notificationChannelField =
                notification.javaClass.getDeclaredField("notificationActionButtonChannel").apply {
                    isAccessible = true
                }
        }

        tripService = NavigationComponentProvider.createTripService(
            context.applicationContext,
            notification
        )
        tripSession = NavigationComponentProvider.createTripSession(
            tripService,
            locationEngine,
            locationEngineRequest
        )

        directionsSession.registerRouteObserver(createInternalRouteObserver())
        tripSession.registerOffRouteObserver(createInternalOffRouteObserver())
    }

    fun startTripSession() {
        tripSession.start()
        notificationChannelField?.let {
            monitorNotificationActionButton(it.get(null) as ReceiveChannel<NotificationAction>)
        }
    }

    fun stopTripSession() {
        tripSession.stop()
    }

    fun requestRoutes(routeOptions: RouteOptions) {
        directionsSession.requestRoutes(routeOptions)
    }

    fun onDestroy() {
        ThreadController.cancelAllNonUICoroutines()
        ThreadController.cancelAllUICoroutines()
        directionsSession.shutDownSession()
        // todo cleanup listeners?
    }

    fun retrieveHistory(): String {
        return MapboxNativeNavigatorImpl.getHistory()
    }

    fun toggleHistory(isEnabled: Boolean) {
        MapboxNativeNavigatorImpl.toggleHistory(isEnabled)
    }

    fun addHistoryEvent(eventType: String, eventJsonProperties: String) {
        MapboxNativeNavigatorImpl.addHistoryEvent(eventType, eventJsonProperties)
    }

    fun registerLocationObserver(locationObserver: LocationObserver) {
        tripSession.registerLocationObserver(locationObserver)
    }

    fun unregisterLocationObserver(locationObserver: LocationObserver) {
        tripSession.unregisterLocationObserver(locationObserver)
    }

    fun registerRouteProgressObserver(routeProgressObserver: RouteProgressObserver) {
        tripSession.registerRouteProgressObserver(routeProgressObserver)
    }

    fun unregisterRouteProgressObserver(routeProgressObserver: RouteProgressObserver) {
        tripSession.unregisterRouteProgressObserver(routeProgressObserver)
    }

    fun registerOffRouteObserver(offRouteObserver: OffRouteObserver) {
        tripSession.registerOffRouteObserver(offRouteObserver)
    }

    fun unregisterOffRouteObserver(offRouteObserver: OffRouteObserver) {
        tripSession.unregisterOffRouteObserver(offRouteObserver)
    }

    fun registerRouteObserver(routeObserver: RouteObserver) {
        directionsSession.registerRouteObserver(routeObserver)
    }

    fun unregisterRouteObserver(routeObserver: RouteObserver) {
        directionsSession.unregisterRouteObserver(routeObserver)
    }

    fun registerBannerInstructionsObserver(bannerInstructionsObserver: BannerInstructionsObserver) {
        tripSession.registerBannerInstructionsObserver(bannerInstructionsObserver)
    }

    fun unregisterBannerInstructionsObserver(bannerInstructionsObserver: BannerInstructionsObserver) {
        tripSession.unregisterBannerInstructionsObserver(bannerInstructionsObserver)
    }

    fun registerVoiceInstructionsObserver(voiceInstructionsObserver: VoiceInstructionsObserver) {
        tripSession.registerVoiceInstructionsObserver(voiceInstructionsObserver)
    }

    fun unregisterVoiceInstructionsObserver(voiceInstructionsObserver: VoiceInstructionsObserver) {
        tripSession.unregisterVoiceInstructionsObserver(voiceInstructionsObserver)
    }

    fun registerTripSessionStateObserver(tripSessionStateObserver: TripSessionStateObserver) {
        tripSession.registerStateObserver(tripSessionStateObserver)
    }

    fun unregisterTripSessionStateObserver(tripSessionStateObserver: TripSessionStateObserver) {
        tripSession.unregisterStateObserver(tripSessionStateObserver)
    }

    private fun createInternalRouteObserver() = object : RouteObserver {
        override fun onRoutesChanged(routes: List<DirectionsRoute>) {
            if (routes.isNotEmpty()) {
                tripSession.route = routes[0]
            } else {
                tripSession.route = null
            }
        }

        override fun onRoutesRequested() {
            tripSession.route = null
        }

        override fun onRoutesRequestFailure(throwable: Throwable) {
            tripSession.route = null
            // todo retry logic with delay
            /*tripSession.registerOffRouteObserver(object : OffRouteObserver {
                override fun onOffRouteStateChanged(offRoute: Boolean) {
                    if (offRoute) {
                        reRoute()
                    }
                    tripSession.unregisterOffRouteObserver(this)
                }
            })*/
        }
    }

    private fun createInternalOffRouteObserver() = object : OffRouteObserver {
        override fun onOffRouteStateChanged(offRoute: Boolean) {
            if (offRoute) {
                reRoute()
            }
        }
    }

    private fun reRoute() {
        ifNonNull(
            directionsSession.getRouteOptions(),
            tripSession.getRawLocation()
        ) { options, location ->
            val optionsBuilder = options.toBuilder()
            val coordinates = options.coordinates()
            tripSession.getRouteProgress()?.currentLegProgress()?.legIndex()?.let { index ->
                optionsBuilder.coordinates(
                    coordinates.drop(index + 1).toMutableList().apply {
                        add(0, Point.fromLngLat(location.longitude, location.latitude))
                    }
                )

                val bearingElements = options.bearings()?.split(";")
                val originTolerance =
                    bearingElements?.getOrNull(0)?.split(",")?.getOrNull(1)?.toDouble()
                bearingElements?.subList(index + 1, coordinates.size)?.map { element ->
                    element.split(",").let { components ->
                        if (components.size == 2) {
                            Pair(components[0].toDouble(), components[1].toDouble())
                        } else {
                            null
                        }
                    }
                }?.toMutableList()?.also { pairs ->
                    pairs.add(
                        0,
                        Pair(
                            location.bearing.toDouble(),
                            originTolerance ?: DEFAULT_REROUTE_BEARING_TOLERANCE
                        )
                    )
                    optionsBuilder.bearings(*pairs.toTypedArray())
                }

                // todo implement options.radiuses
                // todo implement options.approaches
                // todo implement options.waypointIndices
                // todo implement options.waypointNames
                // todo implement options.waypointTargets
            }

            val optionsRebuilt = optionsBuilder.build()
            directionsSession.requestRoutes(optionsRebuilt)
        }
    }

    private fun monitorNotificationActionButton(channel: ReceiveChannel<NotificationAction>) {
        mainJobController.scope.monitorChannelWithException(channel) { notificationAction ->
            when (notificationAction) {
                NotificationAction.END_NAVIGATION -> tripSession.stop()
            }
        }
    }

    /**
     * Provides parameters for Mapbox default modules, recursively if a module depends on other Mapbox modules.
     */
    private fun paramsProvider(type: MapboxNavigationModuleType): Array<Pair<Class<*>?, Any?>> {
        return when (type) {
            MapboxNavigationModuleType.HybridRouter -> arrayOf(
                Router::class.java to NavigationModuleProvider.createModule(
                    MapboxNavigationModuleType.OnboardRouter,
                    ::paramsProvider
                ),
                Router::class.java to NavigationModuleProvider.createModule(
                    MapboxNavigationModuleType.OffboardRouter,
                    ::paramsProvider
                )
            )
            MapboxNavigationModuleType.OffboardRouter -> arrayOf(
                String::class.java to (accessToken
                    ?: throw RuntimeException("You need to provide an access in order to use the default OffboardRouter.")),
                Context::class.java to context
            )
            MapboxNavigationModuleType.OnboardRouter -> arrayOf()
            MapboxNavigationModuleType.DirectionsSession -> throw NotImplementedError() // going to be removed when next base version
            MapboxNavigationModuleType.TripNotification -> arrayOf(
                Context::class.java to context.applicationContext,
                NavigationOptions::class.java to NavigationOptions.Builder(
                    TWELVE_HOURS,
                    ROUNDING_INCREMENT_FIFTY,
                    MapboxDistanceFormatter(
                        context.applicationContext,
                        "ja",
                        METRIC,
                        ROUNDING_INCREMENT_FIFTY
                    )
                ).build()
            )
            MapboxNavigationModuleType.TripService -> throw NotImplementedError() // going to be removed when next base version
            MapboxNavigationModuleType.TripSession -> throw NotImplementedError() // going to be removed when next base version
            MapboxNavigationModuleType.Logger -> arrayOf()
        }
    }

    companion object {
        private const val DEFAULT_REROUTE_BEARING_TOLERANCE = 90.0
    }
}

private fun RouteOptions.toBuilder(): RouteOptions.Builder {
    val builder = RouteOptions.builder()
        .applyDefaultParams()
        .accessToken(this.accessToken())
        .alternatives(this.alternatives())
        .annotations(this.annotations())
        .approaches(this.approaches())
        .bannerInstructions(this.bannerInstructions())
        .baseUrl(this.baseUrl())
        .bearings(this.bearings())
        .continueStraight(this.continueStraight())
        .coordinates(this.coordinates())
        .geometries(this.geometries())
        .language(this.language())
        .overview(this.overview())
        .profile(this.profile())
        .radiuses(this.radiuses())
        .requestUuid(this.requestUuid())
        .roundaboutExits(this.roundaboutExits())
        .steps(this.steps())
        .user(this.user())
        .voiceInstructions(this.voiceInstructions())
        .voiceUnits(this.voiceUnits())
        .waypointIndices(this.waypointIndices())
        .waypointNames(this.waypointNames())
        .waypointTargets(this.waypointTargets())

    // todo fix mapbox-java annotation
    this.exclude()?.let {
        builder.exclude(it)
    }
    this.walkingOptions()?.let {
        builder.walkingOptions(it)
    }

    return builder
}
