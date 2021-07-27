package com.mapbox.navigation.route.internal.onboard

import android.content.Context
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigation.base.route.RouteRefreshCallback
import com.mapbox.navigation.base.route.RouteRefreshError
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.route.internal.util.ACCESS_TOKEN_QUERY_PARAM
import com.mapbox.navigation.route.internal.util.httpUrl
import com.mapbox.navigation.route.internal.util.redactQueryParam
import com.mapbox.navigation.route.offboard.RouteBuilderProvider
import com.mapbox.navigation.route.onboard.OfflineRoute
import com.mapbox.navigation.utils.internal.RequestMap
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigation.utils.internal.cancelRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

/**
 * MapboxOnboardRouter provides offline route fetching
 *
 * It uses offline storage path to store and retrieve data, setup endpoint,
 * tiles' version, token. Config is provided via [RoutingTilesOptions].
 *
 * @param navigatorNative Native Navigator
 * @param context application [Context]
 */
class MapboxOnboardRouter(
    private val accessToken: String,
    private val navigatorNative: MapboxNativeNavigator,
    private val context: Context
) : Router {

    private companion object {
        private val loggerTag = Tag("MbxOnboardRouter")
    }

    private val mainJobControl by lazy { ThreadController.getMainScopeAndRootJob() }
    private val requests = RequestMap<Job>()

    /**
     * Fetch route based on [RouteOptions]
     *
     * @param routeOptions RouteOptions
     * @param callback Callback that gets notified with the results of the request
     */
    override fun getRoute(
        routeOptions: RouteOptions,
        callback: RouterCallback
    ): Long {
        val httpUrl = RouteBuilderProvider
            .getBuilder(null)
            .accessToken(accessToken)
            .routeOptions(routeOptions)
            .build()
            .httpUrl()

        val offlineRoute = OfflineRoute.Builder(httpUrl.toUrl()).build()

        val requestId = requests.generateNextRequestId()
        val internalCallback = object : RouterCallback {
            override fun onRoutesReady(routes: List<DirectionsRoute>, routerOrigin: RouterOrigin) {
                requests.remove(requestId)
                callback.onRoutesReady(routes, routerOrigin)
            }

            override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
                requests.remove(requestId)
                callback.onFailure(reasons, routeOptions)
            }

            override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {
                requests.remove(requestId)
                callback.onCanceled(routeOptions, routerOrigin)
            }
        }
        requests.put(
            requestId,
            retrieveRoute(routeOptions, offlineRoute.buildUrl(), internalCallback)
        )
        return requestId
    }

    override fun cancelRouteRequest(requestId: Long) {
        requests.cancelRequest(requestId, loggerTag) {
            it.cancel()
        }
    }

    /**
     * Interrupts a route-fetching request if one is in progress.
     */
    override fun cancelAll() {
        requests.removeAll().forEach {
            it.cancel()
        }
    }

    /**
     * Release used resources.
     */
    override fun shutdown() {
        cancelAll()
    }

    /**
     * Refresh the traffic annotations for a given [DirectionsRoute]. [MapboxOnboardRouter] is not
     * supporting refresh route.
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
        callback.onError(
            RouteRefreshError("Route refresh is not available when offline.")
        )
        return -1
    }

    override fun cancelRouteRefreshRequest(requestId: Long) {
        // Do nothing
    }

    private fun retrieveRoute(
        routeOptions: RouteOptions,
        url: String,
        callback: RouterCallback
    ): Job {
        val javaUrl = URL(url.redactQueryParam(ACCESS_TOKEN_QUERY_PARAM))
        return mainJobControl.scope.launch {
            try {
                val routerResult = getRoute(url)
                if (routerResult.isValue) {
                    val routes = parseDirectionsRoutes(routerResult.value!!).map {
                        it.toBuilder().routeOptions(routeOptions).build()
                    }
                    callback.onRoutesReady(routes, RouterOrigin.Onboard)
                } else {
                    val error = routerResult.error!!
                    callback.onFailure(
                        listOf(
                            RouterFailure(
                                url = javaUrl,
                                routerOrigin = RouterOrigin.Onboard,
                                message = error.error,
                                code = error.code
                            )
                        ),
                        routeOptions
                    )
                }
            } catch (e: CancellationException) {
                callback.onCanceled(routeOptions, RouterOrigin.Onboard)
            }
        }
    }

    internal suspend fun getRoute(url: String) = withContext(ThreadController.IODispatcher) {
        navigatorNative.getRoute(url)
    }

    // todo Nav Native serializes route options, it probably shouldn't
    private suspend fun parseDirectionsRoutes(json: String): List<DirectionsRoute> =
        withContext(ThreadController.IODispatcher) {
            DirectionsResponse.fromJson(json).routes()
        }
}
