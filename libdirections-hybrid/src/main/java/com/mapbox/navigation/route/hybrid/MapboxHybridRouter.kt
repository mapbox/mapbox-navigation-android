package com.mapbox.navigation.route.hybrid

import com.mapbox.annotation.module.MapboxModule
import com.mapbox.annotation.module.MapboxModuleType
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.route.RouteRefreshCallback
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.utils.internal.NetworkStatusService
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigation.utils.internal.monitorChannelWithException
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.Job

/**
 * MapboxHybridRouter combines onboard and offboard Routers.
 * Fetch route based on internet-connection state.
 *
 * @param onboardRouter Router
 * @param offboardRouter Router
 */
@MapboxModule(MapboxModuleType.NavigationRouter)
class MapboxHybridRouter(
    private val onboardRouter: Router,
    private val offboardRouter: Router,
    networkStatusService: NetworkStatusService
) : Router {

    private val jobControl = ThreadController.getIOScopeAndRootJob()
    private val offboardRouterHandler: RouterHandler by lazy {
        RouterHandler(mainRouter = offboardRouter, reserveRouter = onboardRouter)
    }
    private val onboardRouterHandler: RouterHandler by lazy {
        RouterHandler(mainRouter = onboardRouter, reserveRouter = offboardRouter)
    }
    private val networkStatusJob: Job

    /**
     * routeDispatchHandler always references a router, (on-board or off-board).
     * Internet availability determines which one.
     */
    private val routeDispatchHandler: AtomicReference<RouterDispatchInterface> =
        AtomicReference(offboardRouterHandler)

    /**
     * At init time, the network monitor is setup. isNetworkAvailable represents the current network state. Based
     * on that state we use either the off-board or on-board router.
     */
    init {
        networkStatusJob = jobControl.scope.monitorChannelWithException(networkStatusService.getNetworkStatusChannel(), { networkStatus ->
            when (networkStatus.isNetworkAvailable) {
                true -> {
                    routeDispatchHandler.set(offboardRouterHandler)
                }
                false -> {
                    routeDispatchHandler.set(onboardRouterHandler)
                }
            }
        }, networkStatusService::cleanup)
    }

    /**
     * Private interface used with handler classes here to call the correct router
     */
    private interface RouterDispatchInterface {
        fun getRoute(routeOptions: RouteOptions, clientCallback: Router.Callback)
        fun getRouteRefresh(route: DirectionsRoute, legIndex: Int, callback: RouteRefreshCallback)
    }

    private class RouterHandler(
        private val mainRouter: Router,
        private val reserveRouter: Router
    ) : RouterDispatchInterface, Router.Callback {

        private var reserveRouterCalled = false
        private lateinit var options: RouteOptions
        private lateinit var callback: Router.Callback
        private var fetchingInProgress = false
        private var pendingRequest: Pair<RouteOptions, Router.Callback>? = null

        override fun onResponse(routes: List<DirectionsRoute>) {
            fetchingInProgress = false
            callback.onResponse(routes)
            checkPendingRequest()
        }

        /**
         * onFailure is used as a fail-safe. If the initial call to onBoardRouter.getRoute()
         * fails, it is assumed that the offBoardRouter may be available. The call is made to the offBoardRouter.
         * The error returns remains the same as in the first call, but the flag value has changed. This time a failure
         * is propagated to the client. In short, call the onBoardRouter. If it fails call the offBoardRouter,
         * if that fails propagate the exception
         */
        override fun onFailure(throwable: Throwable) {
            when (reserveRouterCalled) {
                true -> {
                    reserveRouterCalled = false
                    fetchingInProgress = false
                    callback.onFailure(throwable)
                    checkPendingRequest()
                }
                false -> {
                    reserveRouterCalled = true
                    reserveRouter.getRoute(options, this)
                }
            }
        }

        override fun onCanceled() {
            fetchingInProgress = false
            callback.onCanceled()
            checkPendingRequest()
        }

        /**
         * This method is equivalent to calling .getRoute() with the additional parameter capture
         */
        override fun getRoute(routeOptions: RouteOptions, clientCallback: Router.Callback) {
            handleRouteRequest(routeOptions, clientCallback)
        }

        override fun getRouteRefresh(route: DirectionsRoute, legIndex: Int, callback: RouteRefreshCallback) {
            mainRouter.getRouteRefresh(route, legIndex, callback)
        }

        private fun handleRouteRequest(routeOptions: RouteOptions, clientCallback: Router.Callback) {
            if (fetchingInProgress) {
                pendingRequest = Pair(routeOptions, clientCallback)
                if (reserveRouterCalled) {
                    reserveRouter.cancel()
                } else {
                    mainRouter.cancel()
                }
            } else {
                fetchingInProgress = true
                reserveRouterCalled = false
                options = routeOptions
                callback = clientCallback
                mainRouter.getRoute(routeOptions, this)
            }
        }

        private fun checkPendingRequest() {
            pendingRequest?.let {
                getRoute(routeOptions = it.first, clientCallback = it.second)
            }
            pendingRequest = null
        }
    }

    /**
     * Fetch route based on [RouteOptions]
     *
     * @param routeOptions RouteOptions
     * @param callback Callback that gets notified with the results of the request
     */
    override fun getRoute(
        routeOptions: RouteOptions,
        callback: Router.Callback
    ) {
        routeDispatchHandler.get().getRoute(routeOptions, callback)
    }

    /**
     * Refresh the traffic annotations for a given [DirectionsRoute]
     *
     * @param route DirectionsRoute the direction route to refresh
     * @param legIndex Int the index of the current leg in the route
     * @param callback Callback that gets notified with the results of the request
     */
    override fun getRouteRefresh(route: DirectionsRoute, legIndex: Int, callback: RouteRefreshCallback) {
        routeDispatchHandler.get().getRouteRefresh(route, legIndex, callback)
    }

    /**
     * Interrupts a route-fetching request if one is in progress.
     */
    override fun cancel() {
        onboardRouter.cancel()
        offboardRouter.cancel()
    }

    /**
     * Release used resources.
     */
    override fun shutdown() {
        cancel()
        networkStatusJob.cancel()
    }
}
