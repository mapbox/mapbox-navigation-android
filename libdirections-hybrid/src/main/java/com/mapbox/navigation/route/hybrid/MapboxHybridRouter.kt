package com.mapbox.navigation.route.hybrid

import com.mapbox.annotation.navigation.module.MapboxNavigationModule
import com.mapbox.annotation.navigation.module.MapboxNavigationModuleType
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.utils.network.NetworkStatusService
import com.mapbox.navigation.utils.thread.ThreadController
import com.mapbox.navigation.utils.thread.monitorChannelWithException
import java.util.concurrent.atomic.AtomicReference

/**
 * MapboxHybridRouter combines onboard and offboard Routers.
 * Fetch route based on internet-connection state.
 */
@MapboxNavigationModule(MapboxNavigationModuleType.HybridRouter, skipConfiguration = true)
class MapboxHybridRouter(
    private val onboardRouter: Router,
    private val offboardRouter: Router,
    networkStatusService: NetworkStatusService
) : Router {

    private val jobControl = ThreadController.getIOScopeAndRootJob()
    private val offBoardRouterHandler: RouterHandler by lazy {
        RouterHandler(mainRouter = offboardRouter, reserveRouter = onboardRouter)
    }
    private val onBoardRouterHandler: RouterHandler by lazy {
        RouterHandler(mainRouter = onboardRouter, reserveRouter = offboardRouter)
    }

    /**
     * routeDispatchHandler always references a router, (on-board or off-board).
     * Internet availability determines which one.
     */
    private val routeDispatchHandler: AtomicReference<RouterDispatchInterface> =
        AtomicReference(offBoardRouterHandler)

    /**
     * At init time, the network monitor is setup. isNetworkAvailable represents the current network state. Based
     * on that state we use either the off-board or on-board router.
     */
    init {
        jobControl.scope.monitorChannelWithException(networkStatusService.getNetworkStatusChannel(), { networkStatus ->
            when (networkStatus.isNetworkAvailable) {
                true -> {
                    routeDispatchHandler.set(offBoardRouterHandler)
                }
                false -> {
                    routeDispatchHandler.set(onBoardRouterHandler)
                }
            }
        }, networkStatusService::cleanup)
    }

    /**
     * Private interface used with handler classes here to call the correct router
     */
    private interface RouterDispatchInterface {
        fun execute(routeOptions: RouteOptions, clientCallback: Router.Callback)
    }

    private class RouterHandler(
        private val mainRouter: Router,
        private val reserveRouter: Router
    ) : RouterDispatchInterface, Router.Callback {

        private var reserveRouterCalled = false
        private lateinit var options: RouteOptions
        private lateinit var callback: Router.Callback

        override fun onResponse(routes: List<DirectionsRoute>) {
            callback.onResponse(routes)
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
                    callback.onFailure(throwable)
                }
                false -> {
                    reserveRouterCalled = true
                    reserveRouter.getRoute(options, this)
                }
            }
        }

        override fun onCanceled() {
            callback.onCanceled()
        }

        /**
         * This method is equivalent to calling .getRoute() with the additional parameter capture
         */
        override fun execute(routeOptions: RouteOptions, clientCallback: Router.Callback) {
            reserveRouterCalled = false
            options = routeOptions
            callback = clientCallback
            mainRouter.getRoute(routeOptions, this)
        }
    }

    override fun getRoute(
        routeOptions: RouteOptions,
        callback: Router.Callback
    ) {
        routeDispatchHandler.get().execute(routeOptions, callback)
    }

    override fun cancel() {
        onboardRouter.cancel()
        offboardRouter.cancel()
    }

    override fun shutdown() {
        cancel()
    }
}
