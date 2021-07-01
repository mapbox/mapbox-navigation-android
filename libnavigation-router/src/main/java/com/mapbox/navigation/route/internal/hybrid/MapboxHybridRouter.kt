package com.mapbox.navigation.route.internal.hybrid

import android.content.Context
import com.mapbox.annotation.module.MapboxModule
import com.mapbox.annotation.module.MapboxModuleType
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.base.common.logger.Logger
import com.mapbox.navigation.base.internal.accounts.UrlSkuTokenProvider
import com.mapbox.navigation.base.route.RouteRefreshCallback
import com.mapbox.navigation.base.route.RouteRefreshError
import com.mapbox.navigation.base.route.RouteWrapper
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.route.internal.offboard.MapboxOffboardRouter
import com.mapbox.navigation.route.internal.onboard.MapboxOnboardRouter
import com.mapbox.navigation.utils.internal.ConnectivityHandler
import com.mapbox.navigation.utils.internal.RequestMap
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigation.utils.internal.monitorChannelWithException
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
    networkStatusService: ConnectivityHandler,
    private val logger: Logger
) : Router {

    private val directionRequests = RequestMap<HybridRouterHandler.Directions>()
    private val refreshRequests = RequestMap<HybridRouterHandler.Refresh>()

    constructor(
        accessToken: String,
        context: Context,
        urlSkuTokenProvider: UrlSkuTokenProvider,
        navigatorNative: MapboxNativeNavigator,
        logger: Logger,
        networkStatusService: ConnectivityHandler,
        refreshEnabled: Boolean
    ) : this(
        onboardRouter = MapboxOnboardRouter(
            navigatorNative,
            context,
            logger
        ),
        offboardRouter = MapboxOffboardRouter(
            accessToken,
            context,
            urlSkuTokenProvider,
            refreshEnabled,
            logger
        ),
        networkStatusService = networkStatusService,
        logger = logger
    )

    private val jobControl = ThreadController.getIOScopeAndRootJob()
    internal val networkStatusJob: Job
    private var isNetworkAvailable = true

    /**
     * At init time, the network monitor is setup. isNetworkAvailable represents the current network state. Based
     * on that state we use either the off-board or on-board router.
     */
    init {
        networkStatusJob = jobControl.scope.monitorChannelWithException(
            networkStatusService.getNetworkStatusChannel(),
            ::onNetworkStatusChanged
        )
    }

    internal suspend fun onNetworkStatusChanged(status: Boolean) {
        isNetworkAvailable = status
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
    ): Long {
        val routerHandler = createDirectionsHandler()
        val id = directionRequests.put(routerHandler)
        routerHandler.getRoute(
            routeOptions,
            object : Router.Callback {
                override fun onResponse(routeWrapper: RouteWrapper) {
                    directionRequests.remove(id)
                    callback.onResponse(routeWrapper)
                }

                override fun onFailure(throwable: Throwable) {
                    directionRequests.remove(id)
                    callback.onFailure(throwable)
                }

                override fun onCanceled() {
                    directionRequests.remove(id)
                    callback.onCanceled()
                }
            }
        )
        return id
    }

    /**
     * Cancel a request with ID from [getRoute].
     */
    override fun cancelRouteRequest(requestId: Long) {
        directionRequests.remove(requestId)?.cancelRouteRequest()
    }

    /**
     * Refresh the traffic annotations for a given [DirectionsRoute]
     *
     * @param route DirectionsRoute the direction route to refresh
     * @param legIndex Int the index of the current leg in the route
     * @param callback Callback that gets notified with the results of the request
     */
    override fun getRouteRefresh(
        route: DirectionsRoute,
        legIndex: Int,
        callback: RouteRefreshCallback
    ): Long {
        val routerHandler = createRefreshHandler()
        val id = refreshRequests.put(routerHandler)
        routerHandler.getRouteRefresh(
            route,
            legIndex,
            object : RouteRefreshCallback {
                override fun onRefresh(directionsRoute: DirectionsRoute) {
                    refreshRequests.remove(id)
                    callback.onRefresh(directionsRoute)
                }

                override fun onError(error: RouteRefreshError) {
                    refreshRequests.remove(id)
                    callback.onError(error)
                }
            }
        )
        return id
    }

    /**
     * Cancel a request with ID from [getRouteRefresh].
     */
    override fun cancelRouteRefreshRequest(requestId: Long) {
        refreshRequests.remove(requestId)?.cancelRouteRefreshRequest()
    }

    /**
     * Cancel all running requests.
     */
    override fun cancelAll() {
        offboardRouter.cancelAll()
        onboardRouter.cancelAll()
    }

    /**
     * Release used resources.
     */
    override fun shutdown() {
        offboardRouter.shutdown()
        onboardRouter.shutdown()
        networkStatusJob.cancel()
    }

    private fun createDirectionsHandler(): HybridRouterHandler.Directions {
        return if (isNetworkAvailable) {
            HybridRouterHandler.Directions(offboardRouter, onboardRouter, logger)
        } else {
            HybridRouterHandler.Directions(onboardRouter, offboardRouter, logger)
        }
    }

    private fun createRefreshHandler(): HybridRouterHandler.Refresh {
        return if (isNetworkAvailable) {
            HybridRouterHandler.Refresh(offboardRouter, onboardRouter, logger)
        } else {
            HybridRouterHandler.Refresh(onboardRouter, offboardRouter, logger)
        }
    }
}
