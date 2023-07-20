package com.mapbox.navigation.core.reroute

import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.internal.extensions.internalAlternativeRouteIndices
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
import com.mapbox.navigation.utils.internal.logI
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.coroutines.resume

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
    private val workerDispatcher: CoroutineDispatcher,
) : InternalRerouteController {

    private val observers = CopyOnWriteArraySet<RerouteController.RerouteStateObserver>()

    private val mainJobController: JobControl = threadController.getMainScopeAndRootJob()

    private var rerouteJob: Job? = null

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
        MapboxRerouteOptionsAdapter(evDynamicDataHolder),
        Dispatchers.Default
    )

    override var state: RerouteState = RerouteState.Idle
        private set(value) {
            if (field == value) {
                return
            }
            field = value
            observers.forEach { it.onRerouteStateChanged(field) }
        }

    private companion object {
        private const val LOG_CATEGORY = "MapboxRerouteController"

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
            return this.applyAvoidManeuvers(
                rerouteOptions.avoidManeuverSeconds,
                speed
            )
        }
    }

    override fun reroute(routesCallback: RerouteController.RoutesCallback) {
        reroute { result: RerouteResult ->
            routesCallback.onNewRoutes(result.routes.toDirectionsRoutes())
        }
    }

    override fun reroute(callback: NavigationRerouteController.RoutesCallback) {
        reroute { result: RerouteResult ->
            callback.onNewRoutes(result.routes, result.origin)
        }
    }

    override fun reroute(callback: InternalRerouteController.RoutesCallback) {
        interrupt()
        state = RerouteState.FetchingRoute
        logD("Fetching route", LOG_CATEGORY)

        ifNonNull(
            directionsSession.routes,
            tripSession.getRouteProgress()?.routeAlternativeId,
        ) { routes, routeAlternativeId ->
            val relevantAlternative = routes.find { it.id == routeAlternativeId }
            if (relevantAlternative != null) {
                val alternativeLegIndex = tripSession.getRouteProgress()
                    ?.internalAlternativeRouteIndices()?.get(routeAlternativeId)?.legIndex ?: 0
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
                callback.onNewRoutes(RerouteResult(newList, alternativeLegIndex ?: 0, origin))
                state = RerouteState.Idle
                return
            }
        }

        val routeOptions = directionsSession.getPrimaryRouteOptions()
            ?.applyRerouteOptions(
                rerouteOptions,
                tripSession.locationMatcherResult?.enhancedLocation?.speed
            )
        val routeProgress = tripSession.getRouteProgress()
        routeOptionsUpdater.update(
            routeOptions,
            routeProgress,
            tripSession.locationMatcherResult,
        )
            .let { routeOptionsResult ->
                when (routeOptionsResult) {
                    is RouteOptionsUpdater.RouteOptionsResult.Success -> {
                        val modifiedRerouteOption = compositeRerouteOptionsAdapter.onRouteOptions(
                            routeOptionsResult.routeOptions
                        )
                        // route progress can't be null at this point
                        request(callback, modifiedRerouteOption, routeProgress!!.navigationRoute)
                    }
                    is RouteOptionsUpdater.RouteOptionsResult.Error -> {
                        state = RerouteState.Failed(
                            message = "Cannot combine route options",
                            throwable = routeOptionsResult.error
                        )
                        state = RerouteState.Idle
                    }
                }
            }
    }

    @MainThread
    override fun interrupt() {
        rerouteJob?.cancel()
        rerouteJob = null
        if (state == RerouteState.FetchingRoute) {
            logI(LOG_CATEGORY) {
                "Request interrupted via controller"
            }
        }
        onRequestInterrupted()
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
        callback: InternalRerouteController.RoutesCallback,
        routeOptions: RouteOptions,
        currentRoute: NavigationRoute
    ) {
        rerouteJob = mainJobController.scope.launch {
            when (val result = requestAsync(routeOptions)) {
                is RouteRequestResult.Success -> {
                    val routesWithEvStations = restoreChargingStationsMetadata(
                        currentRoute,
                        result.routes,
                        workerDispatcher,
                    )
                    state = RerouteState.RouteFetched(result.routerOrigin)
                    state = RerouteState.Idle
                    callback.onNewRoutes(RerouteResult(routesWithEvStations, 0, result.routerOrigin))
                }
                is RouteRequestResult.Failure -> {
                    state = RerouteState.Failed(
                        "Route request failed",
                        reasons = result.reasons
                    )
                    state = RerouteState.Idle
                }
                is RouteRequestResult.Cancellation -> {
                    if (state == RerouteState.FetchingRoute) {
                        logI("Request canceled via router")
                    }
                    onRequestInterrupted()
                }
            }
        }
    }

    internal fun setRerouteOptionsAdapter(rerouteOptionsAdapter: RerouteOptionsAdapter?) {
        compositeRerouteOptionsAdapter.externalOptionsAdapter = rerouteOptionsAdapter
    }

    private fun onRequestInterrupted() {
        if (state == RerouteState.FetchingRoute) {
            state = RerouteState.Interrupted
            state = RerouteState.Idle
        }
    }

    private suspend fun requestAsync(routeOptions: RouteOptions): RouteRequestResult {
        return suspendCancellableCoroutine { cont ->
            val requestId = directionsSession.requestRoutes(
                routeOptions,
                object : NavigationRouterCallback {
                    override fun onRoutesReady(
                        routes: List<NavigationRoute>,
                        routerOrigin: RouterOrigin
                    ) {
                        if (cont.isActive) {
                            cont.resume(RouteRequestResult.Success(routes, routerOrigin))
                        }
                    }

                    override fun onFailure(
                        reasons: List<RouterFailure>,
                        routeOptions: RouteOptions
                    ) {
                        if (cont.isActive) {
                            cont.resume(RouteRequestResult.Failure(reasons))
                        }
                    }

                    override fun onCanceled(
                        routeOptions: RouteOptions,
                        routerOrigin: RouterOrigin
                    ) {
                        if (cont.isActive) {
                            cont.resume(RouteRequestResult.Cancellation)
                        }
                    }
                }
            )
            cont.invokeOnCancellation {
                directionsSession.cancelRouteRequest(requestId)
            }
        }
    }
}

private sealed class RouteRequestResult {

    class Success(
        val routes: List<NavigationRoute>,
        val routerOrigin: RouterOrigin
    ) : RouteRequestResult()

    class Failure(val reasons: List<RouterFailure>) : RouteRequestResult()

    object Cancellation : RouteRequestResult()
}

/**
 * Max dangerous maneuvers radius meters. See [RouteOptions.avoidManeuverRadius]
 */
private const val MAX_DANGEROUS_MANEUVERS_RADIUS = 1000.0

internal fun RouteOptions.applyAvoidManeuvers(
    avoidManeuverSeconds: Int,
    speed: Float
): RouteOptions {
    val builder = toBuilder()

    if (this.profile() == DirectionsCriteria.PROFILE_DRIVING ||
        this.profile() == DirectionsCriteria.PROFILE_DRIVING_TRAFFIC
    ) {
        val avoidManeuverRadius = avoidManeuverSeconds
            .let { speed * it }.toDouble()
            .takeIf { it >= 1 }
            ?.coerceAtMost(MAX_DANGEROUS_MANEUVERS_RADIUS)

        builder.avoidManeuverRadius(avoidManeuverRadius)
    }

    return builder.build()
}
