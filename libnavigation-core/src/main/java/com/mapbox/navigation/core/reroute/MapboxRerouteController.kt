package com.mapbox.navigation.core.reroute

import androidx.annotation.MainThread
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.base.common.logger.Logger
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.routeoptions.RouteOptionsUpdater
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.utils.internal.ThreadController
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Default implementation of [RerouteController]
 */
internal class MapboxRerouteController(
    private val directionsSession: DirectionsSession,
    private val tripSession: TripSession,
    private val routeOptionsUpdater: RouteOptionsUpdater,
    threadController: ThreadController = ThreadController,
    private val logger: Logger
) : RerouteController {

    private val observers = CopyOnWriteArraySet<RerouteController.RerouteStateObserver>()

    private val mainJobController: JobControl = threadController.getMainScopeAndRootJob()

    override var state: RerouteState = RerouteState.Idle
        private set(value) {
            if (field == value) {
                return
            }
            field = value
            observers.forEach { it.onRerouteStateChanged(field) }
        }

    // current implementation ignores `routesCallback` callback because `DirectionsSession` update routes internally
    override fun reroute(routesCallback: RerouteController.RoutesCallback) {
        interrupt()
        state = RerouteState.FetchingRoute
        logger.d(
            Tag(TAG),
            Message("Fetching route")
        )
        routeOptionsUpdater.update(
            directionsSession.getRouteOptions(),
            tripSession.getRouteProgress(),
            tripSession.getEnhancedLocation()
        )
            .let { routeOptionsResult ->
                when (routeOptionsResult) {
                    is RouteOptionsUpdater.RouteOptionsResult.Success -> {
                        request(routeOptionsResult.routeOptions)
                    }
                    is RouteOptionsUpdater.RouteOptionsResult.Error -> {
                        mainJobController.scope.launch {
                            state = RerouteState.Failed(
                                "Cannot combine route options",
                                routeOptionsResult.error
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
            // do not change state here because it's changed into onRoutesRequestCanceled callback
            directionsSession.cancel()
            logger.d(
                Tag(TAG),
                Message("Route request interrupted")
            )
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

    private fun request(routeOptions: RouteOptions) {
        directionsSession.requestRoutes(
            routeOptions,
            object : RoutesRequestCallback {
                // ignore result, DirectionsSession sets routes internally
                override fun onRoutesReady(routes: List<DirectionsRoute>) {
                    logger.d(
                        Tag(TAG),
                        Message("Route fetched")
                    )
                    mainJobController.scope.launch {
                        state = RerouteState.RouteFetched
                        state = RerouteState.Idle
                    }
                }

                override fun onRoutesRequestFailure(
                    throwable: Throwable,
                    routeOptions: RouteOptions
                ) {
                    logger.e(
                        Tag(TAG),
                        Message("Route request failed"),
                        throwable
                    )

                    mainJobController.scope.launch {
                        state = RerouteState.Failed("Route request failed", throwable)
                        state = RerouteState.Idle
                    }
                }

                override fun onRoutesRequestCanceled(routeOptions: RouteOptions) {
                    logger.d(
                        Tag(TAG),
                        Message("Route request canceled")
                    )
                    mainJobController.scope.launch {
                        state = RerouteState.Interrupted
                        state = RerouteState.Idle
                    }
                }
            }
        )
    }

    private companion object {
        const val TAG = "MapboxRerouteController"
    }
}
