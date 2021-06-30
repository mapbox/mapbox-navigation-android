package com.mapbox.navigation.route.internal.onboard

import android.content.Context
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.base.common.logger.Logger
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigation.base.route.RouteRefreshCallback
import com.mapbox.navigation.base.route.RouteRefreshError
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.route.internal.util.httpUrl
import com.mapbox.navigation.route.offboard.RouteBuilderProvider
import com.mapbox.navigation.route.onboard.OfflineRoute
import com.mapbox.navigation.utils.NavigationException
import com.mapbox.navigation.utils.internal.RequestMap
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigation.utils.internal.cancelRequest
import com.mapbox.navigator.RouterError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * MapboxOnboardRouter provides offline route fetching
 *
 * It uses offline storage path to store and retrieve data, setup endpoint,
 * tiles' version, token. Config is provided via [RoutingTilesOptions].
 *
 * @param navigatorNative Native Navigator
 * @param context application [Context]
 * @param logger interface for logging any events
 */
class MapboxOnboardRouter(
    private val navigatorNative: MapboxNativeNavigator,
    private val context: Context,
    private val logger: Logger
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
        callback: Router.Callback
    ): Long {
        val httpUrl = RouteBuilderProvider
            .getBuilder(null)
            .routeOptions(routeOptions)
            .build()
            .httpUrl()

        val offlineRoute = OfflineRoute.Builder(httpUrl.toUrl()).build()

        val requestId = requests.generateNextRequestId()
        val internalCallback = object : Router.Callback {
            override fun onResponse(routes: List<DirectionsRoute>) {
                requests.remove(requestId)
                callback.onResponse(routes)
            }

            override fun onFailure(throwable: Throwable) {
                requests.remove(requestId)
                callback.onFailure(throwable)
            }

            override fun onCanceled() {
                requests.remove(requestId)
                callback.onCanceled()
            }
        }
        requests.put(requestId, retrieveRoute(offlineRoute.buildUrl(), internalCallback))
        return requestId
    }

    override fun cancelRouteRequest(requestId: Long) {
        requests.cancelRequest(requestId, logger, loggerTag) {
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

    private fun retrieveRoute(url: String, callback: Router.Callback): Job {
        return mainJobControl.scope.launch {
            try {
                val routerResult = getRoute(url)
                if (routerResult.isValue) {
                    val routes: List<DirectionsRoute> = parseDirectionsRoutes(routerResult.value!!)
                    callback.onResponse(routes)
                } else {
                    callback
                        .onFailure(NavigationException(generateErrorMessage(routerResult.error!!)))
                }
            } catch (e: CancellationException) {
                callback.onCanceled()
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

    private fun generateErrorMessage(error: RouterError): String {
        val errorMessage =
            "Error occurred fetching offline route: ${error.error} - Code: ${error.code}"
        logger.e(loggerTag, Message(errorMessage))
        return errorMessage
    }
}
