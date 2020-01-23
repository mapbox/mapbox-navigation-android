package com.mapbox.navigation.core.directions.session

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.core.NavigationComponentProvider
import java.util.concurrent.CopyOnWriteArrayList

// todo make internal
class MapboxDirectionsSession(
    private val router: Router
) : DirectionsSession {

    private val fasterRouteInterval = 2 * 60 * 1000L // 2 minutes
    private val routeObservers = CopyOnWriteArrayList<RouteObserver>()
    private var _routeOptions: RouteOptions? = null
    private var currentRoutes: List<DirectionsRoute> = emptyList()
    private val fasterRouteTimer =
        NavigationComponentProvider.createMapboxTimer(fasterRouteInterval) {
            _routeOptions?.let { requestFasterRoute(it) }
        }

    private var state: State = State.NoRoutesAvailable
        set(value) {
            if (field == value) {
                return
            }
            field = value
            when (value) {
                is State.NoRoutesAvailable -> routeObservers.forEach {
                    it.onRoutesChanged(
                        emptyList()
                    )
                }
                is State.RoutesAvailable -> {
                    // start the timer only when the route has been requested at least once
                    fasterRouteTimer.start()
                    routeObservers.forEach {
                        it.onRoutesChanged(
                            currentRoutes
                        )
                    }
                }
                is State.UserRoutesRequestInProgress -> routeObservers.forEach { it.onRoutesRequested() }
                is State.UserRoutesRequestFailed -> {
                    routeObservers.forEach {
                        it.onRoutesRequestFailure(value.throwable)
                    }
                }
            }
        }

    // todo thanks to the state machine, we can expose the routes setter as well, we'll need the canceled callback as well though
    override fun getRoutes() = currentRoutes

    override fun getRouteOptions(): RouteOptions? = _routeOptions

    override fun cancel() {
        router.cancel() // todo expose onCanceled in Router.Callback
    }

    override fun requestRoutes(routeOptions: RouteOptions) {
        router.cancel()

        this._routeOptions = routeOptions
        currentRoutes = emptyList()
        state = State.UserRoutesRequestInProgress
        router.getRoute(routeOptions, object : Router.Callback {
            override fun onResponse(routes: List<DirectionsRoute>) {
                currentRoutes = routes
                state = State.RoutesAvailable
            }

            override fun onFailure(throwable: Throwable) {
                state = State.UserRoutesRequestFailed(throwable)
            }
        })
    }

    override fun registerRouteObserver(routeObserver: RouteObserver) {
        routeObservers.add(routeObserver)
        when (val localState = state) {
            is State.NoRoutesAvailable -> Unit
            is State.RoutesAvailable -> routeObserver.onRoutesChanged(currentRoutes)
            is State.UserRoutesRequestInProgress -> routeObserver.onRoutesRequested()
            is State.UserRoutesRequestFailed ->
                routeObserver.onRoutesRequestFailure(localState.throwable)
        }
    }

    override fun unregisterRouteObserver(routeObserver: RouteObserver) {
        routeObservers.remove(routeObserver)
    }

    override fun shutDownSession() {
        fasterRouteTimer.stop()
    }

    private fun requestFasterRoute(routeOptions: RouteOptions) {
        if (state != State.RoutesAvailable) {
            return
        }
        router.getRoute(routeOptions, object : Router.Callback {
            override fun onResponse(routes: List<DirectionsRoute>) {
                if (isRouteFaster(routes[0])) {
                    currentRoutes = routes
                    routeObservers.forEach {
                        it.onRoutesChanged(currentRoutes)
                    }
                }
            }

            override fun onFailure(throwable: Throwable) {
                // do nothing
            }
        })
    }

    private fun isRouteFaster(newRoute: DirectionsRoute): Boolean {
        // TODO: Implement the logic to check if the route is faster
        return false
    }

    private sealed class State {
        object NoRoutesAvailable : State()
        object RoutesAvailable : State()
        object UserRoutesRequestInProgress : State()
        class UserRoutesRequestFailed(val throwable: Throwable) : State()
    }
}
