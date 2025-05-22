@file:OptIn(ExperimentalMapboxNavigationAPI::class, ExperimentalPreviewMapboxNavigationAPI::class)

package com.mapbox.navigation.core.reroute

import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.extensions.internalAlternativeRouteIndices
import com.mapbox.navigation.base.internal.route.routerOrigin
import com.mapbox.navigation.base.internal.utils.mapToSdkRouteOrigin
import com.mapbox.navigation.base.options.RerouteDisabled
import com.mapbox.navigation.base.options.RerouteOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.ResponseOriginAPI
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.directions.session.routesPlusIgnored
import com.mapbox.navigation.core.ev.EVDynamicDataHolder
import com.mapbox.navigation.core.internal.router.GetRouteSignature
import com.mapbox.navigation.core.routeoptions.RouteOptionsUpdater
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigation.utils.internal.logD
import com.mapbox.navigation.utils.internal.logI
import com.mapbox.navigation.utils.internal.logW
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
) : InternalRerouteController() {

    private val observers = CopyOnWriteArraySet<RerouteStateObserver>()

    private val mainJobController: JobControl = threadController.getMainScopeAndRootJob()

    private var rerouteJob: Job? = null

    private var runningRerouteCausedByRouteReplan = false

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
        MapboxRerouteOptionsAdapter(
            evDynamicDataHolder,
            RouteHistoryOptionsAdapter(tripSessionRouteProgressProvider(tripSession)),
            RerouteContextReasonOptionsAdapter(),
            CleanupCARelatedParamsAdapter(),
        ),
    )

    override var state: RerouteState = RerouteState.Idle
        private set(value) {
            if (field == value) {
                return
            }
            field = value
            if (value is RerouteState.Idle) {
                runningRerouteCausedByRouteReplan = false
            }
            observers.forEach { it.onRerouteStateChanged(field) }
        }

    private companion object {

        private const val LOG_CATEGORY = "MapboxRerouteController"

        /**
         * Apply reroute options. Speed must be provided as **m/s**
         */
        private fun RouteOptions?.applyRerouteOptions(
            rerouteOptions: RerouteOptions,
            speed: Double?,
        ): RouteOptions? {
            if (this == null || speed == null) {
                return this
            }
            return this.applyAvoidManeuvers(
                rerouteOptions.avoidManeuverSeconds,
                speed,
            )
        }
    }

    /**
     * Can only be invoked publicly by customer.
     */
    override fun reroute(callback: RerouteController.RoutesCallback) {
        rerouteInternal(
            appTriggeredRerouteSignature,
        ) { result: RerouteResult ->
            callback.onNewRoutes(result.routes, result.origin)
        }
    }

    override fun rerouteOnDeviation(callback: RoutesCallback) {
        rerouteInternal(deviationSignature, callback)
    }

    override fun rerouteOnParametersChange(callback: RoutesCallback) {
        runningRerouteCausedByRouteReplan = true
        rerouteInternal(parametersChangeSignature, callback)
    }

    private fun rerouteInternal(
        signature: GetRouteSignature,
        callback: RoutesCallback,
    ) {
        val ignoreDeviationToAlternatives = runningRerouteCausedByRouteReplan
        logD(LOG_CATEGORY) {
            "Starting reroute"
        }
        interrupt()
        state = RerouteState.FetchingRoute
        logD("Fetching route", LOG_CATEGORY)

        val routeProgress = tripSession.getRouteProgress()
        val routeAlternativeId = routeProgress?.routeAlternativeId
        val routes = directionsSession.routesPlusIgnored
        if (!ignoreDeviationToAlternatives && routeAlternativeId != null) {
            val relevantAlternative = routes.find { it.id == routeAlternativeId }
            if (relevantAlternative != null) {
                val alternativeLegIndex = tripSession.getRouteProgress()
                    ?.internalAlternativeRouteIndices()?.get(routeAlternativeId)?.legIndex ?: 0
                val newList = routes.toMutableList().apply {
                    remove(relevantAlternative)
                    add(0, relevantAlternative)
                }

                logD("Reroute switch to alternative", LOG_CATEGORY)

                val origin = relevantAlternative.routerOrigin.mapToSdkRouteOrigin()

                state = RerouteState.RouteFetched(origin)
                callback.onNewRoutes(RerouteResult(newList, alternativeLegIndex, origin))
                state = RerouteState.Idle
                return
            }
        }

        val primaryRoute = directionsSession.routes.firstOrNull()
        if (primaryRoute == null) {
            val message = "Primary route is null while rerouting"
            logW(LOG_CATEGORY) { "$message, failing reroute." }
            state = RerouteState.Failed(message)
            state = RerouteState.Idle
            return
        }

        if (primaryRoute.origin == RouterOrigin.CUSTOM_EXTERNAL) {
            val message = "Reroute is not supported for CUSTOM_EXTERNAL route."
            logW(LOG_CATEGORY) {
                "$message, failing reroute."
            }
            state = RerouteState.Failed(message)
            state = RerouteState.Idle
            return
        }

        val responseOriginAPI = primaryRoute.responseOriginAPI
        val rerouteStrategyForMapMatchedRoutes = rerouteOptions.rerouteStrategyForMapMatchedRoutes
        if (responseOriginAPI == ResponseOriginAPI.MAP_MATCHING_API &&
            rerouteStrategyForMapMatchedRoutes == RerouteDisabled
        ) {
            val message = "According to rerouteStrategyForMapMatchedRoutes new routes " +
                "calculation for routes from Mapbox Map Matching API is disabled."
            logW(LOG_CATEGORY) {
                "$message, failing reroute."
            }
            state = RerouteState.Failed(message)
            state = RerouteState.Idle
            return
        }

        val routeOptions = directionsSession.getPrimaryRouteOptions()
            ?.applyRerouteOptions(
                rerouteOptions,
                tripSession.locationMatcherResult?.enhancedLocation?.speed,
            )

        routeOptionsUpdater.update(
            routeOptions,
            tripSession.getRouteProgress(),
            tripSession.locationMatcherResult,
            responseOriginAPI,
            rerouteStrategyForMapMatchedRoutes,
        )
            .let { routeOptionsResult ->
                when (routeOptionsResult) {
                    is RouteOptionsUpdater.RouteOptionsResult.Success -> {
                        val modifiedRerouteOption = compositeRerouteOptionsAdapter.onRouteOptions(
                            routeOptionsResult.routeOptions,
                            RouteOptionsAdapterParams(
                                signature,
                            ),
                        )
                        request(callback, modifiedRerouteOption, signature)
                    }

                    is RouteOptionsUpdater.RouteOptionsResult.Error -> {
                        state = RerouteState.Failed(
                            message = "Cannot combine route options",
                            throwable = routeOptionsResult.error,
                            reasons = null,
                            preRouterReasons = listOfNotNull(routeOptionsResult.reason),
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
        rerouteStateObserver: RerouteStateObserver,
    ): Boolean {
        val result = observers.add(rerouteStateObserver)
        rerouteStateObserver.onRerouteStateChanged(state)
        return result
    }

    override fun unregisterRerouteStateObserver(
        rerouteStateObserver: RerouteStateObserver,
    ): Boolean {
        return observers.remove(rerouteStateObserver)
    }

    private fun request(
        callback: RoutesCallback,
        routeOptions: RouteOptions,
        signature: GetRouteSignature,
    ) {
        rerouteJob = mainJobController.scope.launch {
            when (val result = requestAsync(routeOptions, signature)) {
                is RouteRequestResult.Success -> {
                    state = RerouteState.RouteFetched(result.routerOrigin)
                    state = RerouteState.Idle
                    callback.onNewRoutes(RerouteResult(result.routes, 0, result.routerOrigin))
                }

                is RouteRequestResult.Failure -> {
                    state = RerouteState.Failed(
                        "Route request failed",
                        reasons = result.reasons,
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

    override fun setRerouteOptionsAdapter(rerouteOptionsAdapter: RerouteOptionsAdapter?) {
        compositeRerouteOptionsAdapter.externalOptionsAdapter = rerouteOptionsAdapter
    }

    private fun onRequestInterrupted() {
        if (state == RerouteState.FetchingRoute) {
            state = RerouteState.Interrupted
            state = RerouteState.Idle
        }
    }

    private suspend fun requestAsync(
        routeOptions: RouteOptions,
        signature: GetRouteSignature,
    ): RouteRequestResult {
        return suspendCancellableCoroutine { cont ->
            val requestId = directionsSession.requestRoutes(
                routeOptions,
                signature,
                object : NavigationRouterCallback {
                    override fun onRoutesReady(
                        routes: List<NavigationRoute>,
                        @RouterOrigin
                        routerOrigin: String,
                    ) {
                        if (cont.isActive) {
                            cont.resume(RouteRequestResult.Success(routes, routerOrigin))
                        }
                    }

                    override fun onFailure(
                        reasons: List<RouterFailure>,
                        routeOptions: RouteOptions,
                    ) {
                        if (cont.isActive) {
                            cont.resume(RouteRequestResult.Failure(reasons))
                        }
                    }

                    override fun onCanceled(
                        routeOptions: RouteOptions,
                        @RouterOrigin
                        routerOrigin: String,
                    ) {
                        if (cont.isActive) {
                            cont.resume(RouteRequestResult.Cancellation)
                        }
                    }
                },
            )
            cont.invokeOnCancellation {
                mainJobController.scope.launch(Dispatchers.Main.immediate) {
                    directionsSession.cancelRouteRequest(requestId)
                }
            }
        }
    }
}

private sealed class RouteRequestResult {

    class Success(
        val routes: List<NavigationRoute>,
        @RouterOrigin
        val routerOrigin: String,
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
    speed: Double?,
): RouteOptions {
    val builder = toBuilder()

    if (this.profile() == DirectionsCriteria.PROFILE_DRIVING ||
        this.profile() == DirectionsCriteria.PROFILE_DRIVING_TRAFFIC
    ) {
        val avoidManeuverRadius = avoidManeuverSeconds
            .let { (speed ?: 0.0) * it }.toDouble()
            .takeIf { it >= 1 }
            ?.coerceAtMost(MAX_DANGEROUS_MANEUVERS_RADIUS)

        builder.avoidManeuverRadius(avoidManeuverRadius)
    }

    return builder.build()
}

@get:VisibleForTesting
internal val deviationSignature = GetRouteSignature(
    GetRouteSignature.Reason.REROUTE_BY_DEVIATION,
    GetRouteSignature.Origin.SDK,
)

@get:VisibleForTesting
internal val appTriggeredRerouteSignature = GetRouteSignature(
    GetRouteSignature.Reason.REROUTE_OTHER,
    GetRouteSignature.Origin.APP,
)

@get:VisibleForTesting
internal val parametersChangeSignature = GetRouteSignature(
    // TODO: adopt different reason from NN when it's ready
    // https://mapbox.atlassian.net/browse/NN-2222
    GetRouteSignature.Reason.REROUTE_OTHER,
    GetRouteSignature.Origin.APP,
)
