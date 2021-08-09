package com.mapbox.navigation.core.reroute

import androidx.annotation.MainThread
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.base.common.logger.Logger
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.directions.session.DirectionsSession
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

    private var requestId: Long? = null

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

    override fun reroute(routesCallback: RerouteController.RoutesCallback) {
        interrupt()
        state = RerouteState.FetchingRoute
        logger.d(
            Tag(TAG),
            Message("Fetching route")
        )
        routeOptionsUpdater.update(
            directionsSession.getPrimaryRouteOptions(),
            tripSession.getRouteProgress(),
            tripSession.getEnhancedLocation()
        )
            .let { routeOptionsResult ->
                when (routeOptionsResult) {
                    is RouteOptionsUpdater.RouteOptionsResult.Success -> {
                        request(routesCallback, routeOptionsResult.routeOptions)
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
            val id = requestId
            checkNotNull(id)
            directionsSession.cancelRouteRequest(id)
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

    private fun request(
        routesCallback: RerouteController.RoutesCallback,
        routeOptions: RouteOptions
    ) {
        requestId = directionsSession.requestRoutes(
            routeOptions,
            object : RouterCallback {
                override fun onRoutesReady(
                    routes: List<DirectionsRoute>,
                    routerOrigin: RouterOrigin
                ) {
                    mainJobController.scope.launch {
                        state = RerouteState.RouteFetched(routerOrigin)
                        state = RerouteState.Idle
                        routesCallback.onNewRoutes(routes)
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

    private companion object {
        private const val TAG = "MbxRerouteController"
    }
}
