package com.mapbox.navigation.core.reroute

import androidx.annotation.VisibleForTesting
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.internal.utils.mapToSdkRouteOrigin
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.toDirectionsRoutes
import com.mapbox.navigation.base.route.toNavigationRoutes
import com.mapbox.navigator.RerouteError
import com.mapbox.navigator.RerouteObserver
import com.mapbox.navigator.RouteInterface
import com.mapbox.navigator.RouterOrigin
import java.net.URL
import java.util.concurrent.CopyOnWriteArraySet

internal class MapboxRerouteControllerFacade private constructor(
    private val rerouteObserver: RerouteControllersManager.Observer,
    private val nativeRerouteController: NativeExtendedRerouteControllerInterface,
) : NavigationRerouteController {

    override var state: RerouteState = RerouteState.Idle
        @VisibleForTesting
        internal set(value) {
            if (value == field) return
            field = value
            observers.forEach { it.onRerouteStateChanged(field) }
        }

    private val observers = CopyOnWriteArraySet<RerouteController.RerouteStateObserver>()

    internal companion object {
        private const val LOG_CATEGORY = "MapboxRerouteControllerFacade"

        internal operator fun invoke(
            rerouteObserver: RerouteControllersManager.Observer,
            nativeRerouteController: NativeExtendedRerouteControllerInterface,
        ): MapboxRerouteControllerFacade = MapboxRerouteControllerFacade(
            rerouteObserver,
            nativeRerouteController,
        )
    }

    init {
        nativeRerouteController.addRerouteObserver(object : RerouteObserver {
            override fun onRerouteDetected(routeRequest: String): Boolean {
                state = RerouteState.FetchingRoute
                return true
            }

            override fun onRerouteReceived(
                routeResponse: String,
                routeRequest: String,
                origin: RouterOrigin
            ) {
                state = RerouteState.RouteFetched(origin.mapToSdkRouteOrigin())
                rerouteObserver.onNewRoutes(
                    DirectionsResponse.fromJson(
                        routeResponse, RouteOptions.fromUrl(URL(routeRequest))
                    )
                        .routes().toNavigationRoutes(origin.mapToSdkRouteOrigin()),
                )
                state = RerouteState.Idle
            }

            override fun onRerouteCancelled() {
                state = RerouteState.Interrupted
                state = RerouteState.Idle
            }

            override fun onRerouteFailed(error: RerouteError) {
                state = RerouteState.Failed(error.message)
                state = RerouteState.Idle
            }

            override fun onSwitchToAlternative(route: RouteInterface) {
                rerouteObserver.onNewRoutes(
                    NavigationRoute.create(
                        route.responseJson,
                        route.requestUri,
                        route.routerOrigin.mapToSdkRouteOrigin(),
                    ),
                )
            }
        })
    }

    override fun reroute(callback: NavigationRerouteController.RoutesCallback) {
        if (state is RerouteState.FetchingRoute) {
            interrupt()
        }
        nativeRerouteController.setRerouteCallbackListener { expected ->
            // listen for the very first reroute callback, most probably invoked
            // by `nativeRerouteController.forceReroute()`. Event if no it's not a problem as a
            // reroute request happens in any case
            nativeRerouteController.setRerouteCallbackListener(null)
            expected.fold({
                // do nothing
            }, { rerouteInfo ->
                val directionsResponse = DirectionsResponse.fromJson(
                    rerouteInfo.routeResponse, RouteOptions.fromUrl(URL(rerouteInfo.routeRequest))
                )
                val navRoutes = directionsResponse.routes()
                    .toNavigationRoutes(rerouteInfo.origin.mapToSdkRouteOrigin())
                callback.onNewRoutes(navRoutes, rerouteInfo.origin.mapToSdkRouteOrigin())
            })
        }
        nativeRerouteController.forceReroute()
    }

    override fun reroute(routesCallback: RerouteController.RoutesCallback) {
        reroute { navRoutes, _ ->
            routesCallback.onNewRoutes(navRoutes.toDirectionsRoutes())
        }
    }

    override fun interrupt() {
        nativeRerouteController.cancel()
    }

    override fun registerRerouteStateObserver(
        rerouteStateObserver: RerouteController.RerouteStateObserver
    ): Boolean {
        rerouteStateObserver.onRerouteStateChanged(state)
        return observers.add(rerouteStateObserver)
    }

    override fun unregisterRerouteStateObserver(
        rerouteStateObserver: RerouteController.RerouteStateObserver
    ): Boolean =
        observers.remove(rerouteStateObserver)

    fun setRerouteOptionsAdapter(rerouteOptionsAdapter: RerouteOptionsAdapter?) {
        nativeRerouteController.setRerouteOptionsAdapter(rerouteOptionsAdapter)
    }
}
