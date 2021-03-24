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
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.route.internal.util.httpUrl
import com.mapbox.navigation.route.offboard.RouteBuilderProvider
import com.mapbox.navigation.route.offboard.router.routeOptions
import com.mapbox.navigation.route.onboard.OfflineRoute
import com.mapbox.navigation.utils.NavigationException
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigator.RouterError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelChildren
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
        private val loggerTag = Tag("MapboxOnboardRouter")
    }

    private val mainJobControl by lazy { ThreadController.getMainScopeAndRootJob() }

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
        val httpUrl = RouteBuilderProvider
            .getBuilder(context, null)
            .routeOptions(routeOptions, false)
            .build()
            .httpUrl()

        val offlineRoute = OfflineRoute.Builder(httpUrl.url()).build()
        retrieveRoute(offlineRoute.buildUrl(), callback)
    }

    /**
     * Interrupts a route-fetching request if one is in progress.
     */
    override fun cancel() {
        mainJobControl.job.cancelChildren()
    }

    /**
     * Release used resources.
     */
    override fun shutdown() {
        cancel()
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
    ) {
        // Does nothing
    }

    private fun retrieveRoute(url: String, callback: Router.Callback) {
        mainJobControl.scope.launch {
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
