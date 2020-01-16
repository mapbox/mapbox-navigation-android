package com.mapbox.navigation.route.hybrid

import android.content.Context
import com.mapbox.annotation.navigation.module.MapboxNavigationModule
import com.mapbox.annotation.navigation.module.MapboxNavigationModuleType
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.utils.network.NetworkStatusService
import com.mapbox.navigation.utils.thread.ThreadController
import com.mapbox.navigation.utils.thread.monitorChannelWithException
import java.util.concurrent.atomic.AtomicReference

@MapboxNavigationModule(MapboxNavigationModuleType.HybridRouter, skipConfiguration = true)
class MapboxHybridRouter(
    private val onBoardRouter: Router,
    private val offBoardRouter: Router,
    context: Context
) : Router {

    private val navigationMonitorStateException = NetworkStatusService(context)
    private val jobControl = ThreadController.getIOScopeAndRootJob()

    /**
     * routeDispatchHandler always references a router, (on-board or off-board).
     * Internet availability determines which one.
     */
    private val routeDispatchHandler: AtomicReference<RouterDispatchInterface> = AtomicReference(
        OffBoardRouterHandler(offBoardRouter, onBoardRouter)
    )

    /**
     * At init time, the network monitor is setup. isNetworkAvailable represents the current network state. Based
     * on that state we use either the off-board or on-board router.
     */
    init {
        jobControl.scope.monitorChannelWithException(
            navigationMonitorStateException.getNetworkStatusChannel()
        ) { networkStatus ->
            when (networkStatus.isNetworkAvailable) {
                true -> {
                    routeDispatchHandler.set(OffBoardRouterHandler(offBoardRouter, onBoardRouter))
                }
                false -> {
                    routeDispatchHandler.set(OnBoardRouterHandler(offBoardRouter, onBoardRouter))
                }
            }
        }
    }

    /**
     * Private interface used with handler classes here to call the correct router
     */
    private interface RouterDispatchInterface {
        fun execute(routeOptions: RouteOptions, clientCallback: Router.Callback)
    }

    // Off board router is used if an internet connection is available
    private class OffBoardRouterHandler(
        private val offBoardRouter: Router,
        private val onBoardRouter: Router
    ) : RouterDispatchInterface, Router.Callback {
        private var isOnBoardRouterCalled = false
        private var options: RouteOptions? = null
        private var callback: Router.Callback? = null

        override fun onResponse(routes: List<DirectionsRoute>) {
            callback?.onResponse(routes)
        }

        override fun onFailure(throwable: Throwable) {
            when (isOnBoardRouterCalled) {
                true -> {
                    isOnBoardRouterCalled = false
                    callback?.onFailure(throwable)
                }
                false -> {
                    isOnBoardRouterCalled = true
                    options?.let { options ->
                        onBoardRouter.getRoute(options, this)
                    }
                }
            }
        }

        /**
         * This method is equivalent to calling .getRoute() with the additional parameter capture
         */
        override fun execute(routeOptions: RouteOptions, clientCallback: Router.Callback) {
            isOnBoardRouterCalled = false
            options = routeOptions
            callback = clientCallback
            offBoardRouter.getRoute(routeOptions, this)
        }
    }

    // On Board router used if an internet connection is not available.
    private class OnBoardRouterHandler(
        private val offBoardRouter: Router,
        private val onBoardRouter: Router
    ) : RouterDispatchInterface, Router.Callback {
        private var isOffBoardRouterCalled = false
        private var options: RouteOptions? = null
        private var callback: Router.Callback? = null

        override fun onResponse(routes: List<DirectionsRoute>) {
            callback?.onResponse(routes)
        }

        /**
         * onFailure is used as a fail-safe. If the initial call to onBoardRouter.getRoute()
         * fails, it is assumed that the offBoardRouter may be available. The call is made to the offBoardRouter.
         * The error returns remains the same as in the first call, but the flag value has changed. This time a failure
         * is propagated to the client. In short, call the onBoardRouter. If it fails call the offBoardRouter,
         * if that fails propagate the exception
         */
        override fun onFailure(throwable: Throwable) {
            when (isOffBoardRouterCalled) {
                true -> {
                    isOffBoardRouterCalled = false
                    callback?.onFailure(throwable)
                }
                false -> {
                    isOffBoardRouterCalled = true
                    options?.let { options ->
                        offBoardRouter.getRoute(options, this)
                    }
                }
            }
        }
        override fun execute(routeOptions: RouteOptions, clientCallback: Router.Callback) {
            isOffBoardRouterCalled = false
            options = routeOptions
            callback = clientCallback
            onBoardRouter.getRoute(routeOptions, this)
        }
    }

    override fun getRoute(
        routeOptions: RouteOptions,
        callback: Router.Callback
    ) {
        routeDispatchHandler.get().execute(routeOptions, callback)
    }

    override fun cancel() {
        onBoardRouter.cancel()
        offBoardRouter.cancel()
    }
}
