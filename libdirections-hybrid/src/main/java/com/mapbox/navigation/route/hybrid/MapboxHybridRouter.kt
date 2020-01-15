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
    private val onboardRouter: Router,
    private val offboardRouter: Router,
    context: Context
) : Router {

    private val navigationMonitorStateException = NetworkStatusService(context)
    private val jobControl = ThreadController.getIOScopeAndRootJob()
    private var routeDispatchHandler: AtomicReference<RouterDispatchInterface> = AtomicReference(OffBoardRouterHandler(offboardRouter, onboardRouter))

    init {
        jobControl.scope.monitorChannelWithException(navigationMonitorStateException.getNetworkStatusChannel()) { networkStatus ->
            when (networkStatus.isNetworkAvailable) {
                true -> {
                    routeDispatchHandler.set(OffBoardRouterHandler(offboardRouter, onboardRouter))
                }
                false -> {
                    routeDispatchHandler.set(OnBoardRouterHandler(offboardRouter, onboardRouter))
                }
            }
        }
    }

    private interface RouterDispatchInterface {
        fun execute(routeOptions: RouteOptions, clientCallback: Router.Callback)
    }

    //Off board router is used if an internet connection is available
    private class OffBoardRouterHandler(private val offBoardRouter: Router, private val onBoardRouter: Router) : RouterDispatchInterface, Router.Callback {
        private var onBoardRouterCalled = false
        private var options: RouteOptions? = null
        private var callback: Router.Callback? = null

        override fun onResponse(routes: List<DirectionsRoute>) {
            callback?.onResponse(routes)
        }

        override fun onFailure(throwable: Throwable) {
            when (onBoardRouterCalled) {
                true -> {
                    onBoardRouterCalled = false
                    callback?.onFailure(throwable)
                }
                false -> {
                    onBoardRouterCalled = true
                    options?.let { options ->
                        onBoardRouter.getRoute(options, this)
                    }
                }
            }
        }

        override fun execute(routeOptions: RouteOptions, clientCallback: Router.Callback) {
            onBoardRouterCalled = false
            options = routeOptions
            callback = clientCallback
            offBoardRouter.getRoute(routeOptions, this)
        }
    }

    //On Board router used if an internet connection is not available.
    private class OnBoardRouterHandler(private val offBoardRouter: Router, private val onBoardRouter: Router) : RouterDispatchInterface, Router.Callback {
        private var isOffboardRouterCalled = false
        private var options: RouteOptions? = null
        private var callback: Router.Callback? = null
        override fun onResponse(routes: List<DirectionsRoute>) {
            callback?.onResponse(routes)
        }

        override fun onFailure(throwable: Throwable) {
            when (isOffboardRouterCalled) {
                true -> {
                    isOffboardRouterCalled = false
                    callback?.onFailure(throwable)
                }
                false -> {
                    isOffboardRouterCalled = true
                    options?.let { options ->
                        offBoardRouter.getRoute(options, this)
                    }
                }
            }
        }
        override fun execute(routeOptions: RouteOptions, clientCallback: Router.Callback) {
            isOffboardRouterCalled = false
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
        onboardRouter.cancel()
        offboardRouter.cancel()
    }
}
