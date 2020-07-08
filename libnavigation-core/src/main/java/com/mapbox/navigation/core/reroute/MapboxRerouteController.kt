package com.mapbox.navigation.core.reroute

import androidx.annotation.MainThread
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.base.common.logger.Logger
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.routeoptions.RouteOptionsProvider
import com.mapbox.navigation.core.trip.session.OffRouteObserver
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.utils.internal.ThreadController
import java.util.concurrent.CopyOnWriteArraySet
import kotlinx.coroutines.launch

/**
 * Default implementation of [RerouteController]
 */
internal class MapboxRerouteController(
    private val directionsSession: DirectionsSession,
    private val tripSession: TripSession,
    private val routeOptionsProvider: RouteOptionsProvider,
    threadController: ThreadController = ThreadController,
    private val logger: Logger
) : RerouteController {

    private val observers = CopyOnWriteArraySet<RerouteController.RerouteStateObserver>()

    private val rerouteObservers = CopyOnWriteArraySet<RerouteController.RoutesCallback>()

    private val mainJobController: JobControl = threadController.getMainScopeAndRootJob()

    override var state: RerouteState = RerouteState.Idle
        private set(value) {
            if (field == value) {
                return
            }
            field = value
            observers.forEach { it.onRerouteStateChanged(field) }
        }

    private companion object {
        const val TAG = "MapboxRerouteController"
    }

    override fun reroute() {
        state = RerouteState.FetchingRoute
        logger.d(
            Tag(TAG),
            Message("Fetching route")
        )
        routeOptionsProvider.update(
            directionsSession.getRouteOptions(),
            tripSession.getRouteProgress(),
            tripSession.getEnhancedLocation()
        )
            .let { routeOptionsResult ->
                when (routeOptionsResult) {
                    is RouteOptionsProvider.RouteOptionsResult.Success -> {
                        request(routeOptionsResult.routeOptions)
                    }
                    is RouteOptionsProvider.RouteOptionsResult.Error -> {
                        mainJobController.scope.launch {
                            state = RerouteState.Failed(
                                "Cannot combine route options", routeOptionsResult.error
                            )
                            state = RerouteState.Idle
                        }
                    }
                }
            }
    }

    private fun request(routeOptions: RouteOptions) {
        directionsSession.requestRoutes(routeOptions, object : RoutesRequestCallback {
            override fun onRoutesReady(routes: List<DirectionsRoute>) {
                logger.d(
                    Tag(TAG),
                    Message("Route fetched")
                )
                mainJobController.scope.launch {
                    state = RerouteState.RouteFetched
                    rerouteObservers.forEach { it.onNewRoutes(routes) }
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
        })
    }

    @MainThread
    override fun interrupt() {
        if (state == RerouteState.FetchingRoute) {
            directionsSession.cancel() // do not change state here because it's changed into onRoutesRequestCanceled callback
            logger.d(
                Tag(TAG),
                Message("Route request interrupted")
            )
        }
    }

    override fun registerRerouteStateObserver(rerouteStateObserver: RerouteController.RerouteStateObserver): Boolean {
        mainJobController.scope.launch {
            rerouteStateObserver.onRerouteStateChanged(state)
        }
        return observers.add(rerouteStateObserver)
    }

    override fun unregisterRerouteStateObserver(rerouteStateObserver: RerouteController.RerouteStateObserver): Boolean {
        return observers.remove(rerouteStateObserver)
    }

    override fun registerRerouteObserver(rerouteObserver: RerouteController.RoutesCallback) {
        rerouteObservers.add(rerouteObserver)
    }

    override fun unregisterRerouteObserver(rerouteObserver: RerouteController.RoutesCallback) {
        rerouteObservers.remove(rerouteObserver)
    }

    override fun onOffRouteStateChanged(offRoute: Boolean) {
        if (offRoute){
            reroute()
        } else {
            interrupt()
        }
    }
}
