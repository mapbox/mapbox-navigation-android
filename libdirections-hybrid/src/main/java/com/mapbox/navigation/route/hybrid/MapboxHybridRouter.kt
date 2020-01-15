package com.mapbox.navigation.route.hybrid

import android.content.Context
import com.mapbox.annotation.navigation.module.MapboxNavigationModule
import com.mapbox.annotation.navigation.module.MapboxNavigationModuleType
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.trip.service.NetworkStatusService
import com.mapbox.navigation.utils.thread.ThreadController
import com.mapbox.navigation.utils.thread.monitorChannelWithException
import java.util.concurrent.atomic.AtomicReference

@MapboxNavigationModule(MapboxNavigationModuleType.HybridRouter, skipConfiguration = true)
class MapboxHybridRouter(
    private val onboardRouter: Router,
    private val offboardRouter: Router,
    context: Context
) : Router {

    private interface RouterDispatchInterface {
        fun execute(routeOptions: RouteOptions, clientCallback: Router.Callback)
    }

    private class OnBoardRouterHandler(private val offRoute: Router, private val onboaredRouter: Router) : RouterDispatchInterface, Router.Callback {
        private var offrouterCalled = false
        private var args: RouteOptions? = null
        private var callback: Router.Callback? = null

        override fun onResponse(routes: List<DirectionsRoute>) {
            callback?.onResponse(routes)
        }

        override fun onFailure(throwable: Throwable) {
            when (offrouterCalled) {
                true -> {
                    offrouterCalled = false
                    callback?.onFailure(throwable)
                }
                false -> {
                    offrouterCalled = true
                    args?.let { options ->
                        offRoute.getRoute(options, this)
                    }
                }
            }
        }

        override fun execute(routeOptions: RouteOptions, clientCallback: Router.Callback) {
            offrouterCalled = false
            args = routeOptions
            callback = clientCallback
            onboaredRouter.getRoute(routeOptions, this)
        }
    }

    private class OffBoardRouterHandler(private val offboaredRouter: Router) : RouterDispatchInterface, Router.Callback {
        private var callback: Router.Callback? = null
        override fun onResponse(routes: List<DirectionsRoute>) {
            callback?.onResponse(routes)
        }

        override fun onFailure(throwable: Throwable) {
            callback?.onFailure(throwable)
        }

        override fun execute(routeOptions: RouteOptions, clientCallback: Router.Callback) {
            callback = clientCallback
            offboaredRouter.getRoute(routeOptions, this)
        }
    }

    private val navigationMonitorStateException = NetworkStatusService(context)
    private val jobControl = ThreadController.getIOScopeAndRootJob()
    private var routeDispatchHandler: AtomicReference<RouterDispatchInterface> = AtomicReference(OnBoardRouterHandler(offboardRouter, onboardRouter))

    init {
        jobControl.scope.monitorChannelWithException(navigationMonitorStateException.getNetworkStatusChannel()) { networkStatus ->
            when (networkStatus.isNetworkAvailable) {
                true -> {
                    routeDispatchHandler.set(OnBoardRouterHandler(offboardRouter, onboardRouter))
                }
                false -> {
                    routeDispatchHandler.set(OffBoardRouterHandler(offboardRouter))
                }
            }
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
