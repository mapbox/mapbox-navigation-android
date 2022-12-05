package com.mapbox.navigation.core.reroute

import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.internal.route.routerOrigin
import com.mapbox.navigation.base.internal.utils.mapToSdkRouteOrigin
import com.mapbox.navigation.base.options.RerouteOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.route.toDirectionsRoutes
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.ev.EVDynamicDataHolder
import com.mapbox.navigation.core.routeoptions.RouteOptionsUpdater
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigation.utils.internal.ifNonNull
import com.mapbox.navigation.utils.internal.logD
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Default implementation of [RerouteController]
 */
internal class MapboxRerouteController @VisibleForTesting constructor(
    private val directionsSession: DirectionsSession,
    private val tripSession: TripSession,
    private val routeOptionsUpdater: RouteOptionsUpdater,
    private val rerouteOptions: RerouteOptions,
    threadController: ThreadController,
    private val compositeRerouteOptionsAdapter: MapboxRerouteOptionsAdapter,
) : NavigationRerouteController {

    private val observers = CopyOnWriteArraySet<RerouteController.RerouteStateObserver>()

    private val mainJobController: JobControl = threadController.getMainScopeAndRootJob()

    private var requestId: Long? = null

    constructor(
        directionsSession: DirectionsSession,
        tripSession: TripSession,
        routeOptionsUpdater: RouteOptionsUpdater,
        rerouteOptions: RerouteOptions,
        threadController: ThreadController,
        evDynamicDataHolder: EVDynamicDataHolder,
    ) : this(
        directionsSession,
        tripSession,
        routeOptionsUpdater,
        rerouteOptions,
        threadController,
        MapboxRerouteOptionsAdapter(evDynamicDataHolder)
    )

    override var state: RerouteState = RerouteState.Idle
        private set(value) {
            if (field == value) {
                return
            }
            field = value
            when (value) {
                RerouteState.Idle,
                RerouteState.Interrupted,
                is RerouteState.Failed,
                is RerouteState.RouteFetched -> {
                    requestId = null
                }
                RerouteState.FetchingRoute -> {
                    // no impl
                }
            }
            observers.forEach { it.onRerouteStateChanged(field) }
        }

    private companion object {
        private const val LOG_CATEGORY = "MapboxRerouteController"

        /**
         * Max dangerous maneuvers radius meters. See [RouteOptions.avoidManeuverRadius]
         */
        private const val MAX_DANGEROUS_MANEUVERS_RADIUS = 1000.0

        /**
         * Apply reroute options. Speed must be provided as **m/s**
         */
        private fun RouteOptions?.applyRerouteOptions(
            rerouteOptions: RerouteOptions,
            speed: Float?
        ): RouteOptions? {
            if (this == null || speed == null) {
                return this
            }

            val builder = toBuilder()

            if (this.profile() == DirectionsCriteria.PROFILE_DRIVING ||
                this.profile() == DirectionsCriteria.PROFILE_DRIVING_TRAFFIC
            ) {
                val avoidManeuverRadius = rerouteOptions.avoidManeuverSeconds
                    .let { speed * it }.toDouble()
                    .takeIf { it >= 1 }
                    ?.coerceAtMost(MAX_DANGEROUS_MANEUVERS_RADIUS)

                builder.avoidManeuverRadius(avoidManeuverRadius)
            }

            return builder.build()
        }
    }

    override fun reroute(routesCallback: RerouteController.RoutesCallback) {
        reroute { routes, _ ->
            routesCallback.onNewRoutes(routes.toDirectionsRoutes())
        }
    }

    override fun reroute(callback: NavigationRerouteController.RoutesCallback) {
        interrupt()
        state = RerouteState.FetchingRoute
        logD("Fetching route", LOG_CATEGORY)

        ifNonNull(
            directionsSession.routes,
            tripSession.getRouteProgress()?.routeAlternativeId,
        ) { routes, routeAlternativeId ->
            val relevantAlternative = routes.find { it.id == routeAlternativeId }
            if (relevantAlternative != null) {
                val newList = mutableListOf(relevantAlternative).apply {
                    addAll(
                        routes.toMutableList().apply {
                            removeFirst()
                            remove(relevantAlternative)
                        }
                    )
                }

                logD("Reroute switch to alternative", LOG_CATEGORY)

                val origin = relevantAlternative.routerOrigin.mapToSdkRouteOrigin()

                state = RerouteState.RouteFetched(origin)
                callback.onNewRoutes(newList, origin)
                state = RerouteState.Idle
                return
            }
        }

        val routeOptions = directionsSession.getPrimaryRouteOptions()
            ?.applyRerouteOptions(
                rerouteOptions,
                tripSession.locationMatcherResult?.enhancedLocation?.speed
            )

        routeOptionsUpdater.update(
            routeOptions,
            tripSession.getRouteProgress(),
            tripSession.locationMatcherResult,
        )
            .let { routeOptionsResult ->
                when (routeOptionsResult) {
                    is RouteOptionsUpdater.RouteOptionsResult.Success -> {
                        val modifiedRerouteOption = compositeRerouteOptionsAdapter.onRouteOptions(
                            routeOptionsResult.routeOptions
                        )
                        request(callback, modifiedRerouteOption)
                    }
                    is RouteOptionsUpdater.RouteOptionsResult.Error -> {
                        mainJobController.scope.launch {
                            state = RerouteState.Failed(
                                message = "Cannot combine route options",
                                throwable = routeOptionsResult.error
                            )
                            state = RerouteState.Idle
                        }
                    }
                }
            }
    }

    @MainThread
    override fun interrupt() {
        if (state == RerouteState.FetchingRoute) {
            requestId?.also { id ->
                directionsSession.cancelRouteRequest(id)
                logD(
                    "Route request interrupted",
                    LOG_CATEGORY
                )
            }
        }
    }

    override fun registerRerouteStateObserver(
        rerouteStateObserver: RerouteController.RerouteStateObserver
    ): Boolean {
        mainJobController.scope.launch {
            rerouteStateObserver.onRerouteStateChanged(state)
        }
        return observers.add(rerouteStateObserver)
    }

    override fun unregisterRerouteStateObserver(
        rerouteStateObserver: RerouteController.RerouteStateObserver
    ): Boolean {
        return observers.remove(rerouteStateObserver)
    }

    private fun request(
        callback: NavigationRerouteController.RoutesCallback,
        routeOptions: RouteOptions
    ) {
        requestId = directionsSession.requestRoutes(
            routeOptions,
            object : NavigationRouterCallback {
                override fun onRoutesReady(
                    routes: List<NavigationRoute>,
                    routerOrigin: RouterOrigin
                ) {
                    mainJobController.scope.launch {
                        state = RerouteState.RouteFetched(routerOrigin)
                        state = RerouteState.Idle
                        callback.onNewRoutes(routes, routerOrigin)
                    }
                }

                override fun onFailure(
                    reasons: List<RouterFailure>,
                    routeOptions: RouteOptions
                ) {
                    mainJobController.scope.launch {
                        state = RerouteState.Failed("Route request failed", reasons = reasons)
                        state = RerouteState.Idle
                    }
                }

                override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {
                    mainJobController.scope.launch {
                        state = RerouteState.Interrupted
                        state = RerouteState.Idle
                    }
                }
            }
        )
    }

    internal fun setRerouteOptionsAdapter(rerouteOptionsAdapter: RerouteOptionsAdapter?) {
        compositeRerouteOptionsAdapter.externalOptionsAdapter = rerouteOptionsAdapter
    }
}
