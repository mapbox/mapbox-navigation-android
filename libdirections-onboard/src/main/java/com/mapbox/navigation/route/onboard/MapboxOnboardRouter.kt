package com.mapbox.navigation.route.onboard

import com.google.gson.Gson
import com.mapbox.annotation.navigation.module.MapboxNavigationModule
import com.mapbox.annotation.navigation.module.MapboxNavigationModuleType
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.logger.Logger
import com.mapbox.navigation.base.logger.model.Message
import com.mapbox.navigation.base.logger.model.Tag
import com.mapbox.navigation.base.options.MapboxOnboardRouterConfig
import com.mapbox.navigation.base.route.RouteUrl
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.navigator.MapboxNativeNavigator
import com.mapbox.navigation.navigator.MapboxNativeNavigatorImpl
import com.mapbox.navigation.route.onboard.model.OfflineRouteError
import com.mapbox.navigation.route.onboard.network.HttpClient
import com.mapbox.navigation.utils.exceptions.NavigationException
import com.mapbox.navigation.utils.thread.ThreadController
import com.mapbox.navigator.RouterParams
import com.mapbox.navigator.TileEndpointConfiguration
import java.io.File
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * MapboxOnboardRouter provides offline route fetching
 *
 * It uses offline storage path for storing and retrieving data, setup endpoint,
 * tiles' version, token. Config provides via [MapboxOnboardRouterConfig].
 */
@MapboxNavigationModule(MapboxNavigationModuleType.OnboardRouter, skipConfiguration = true)
class MapboxOnboardRouter : Router {

    companion object {
        private const val TILES_DIR_NAME = "tiles"
    }

    private val navigatorNative: MapboxNativeNavigator
    private val config: MapboxOnboardRouterConfig
    private val logger: Logger?
    private val mainJobControl = ThreadController.getMainScopeAndRootJob()
    private val gson = Gson()

    /**
     * @param config OnbooardRouter config
     * @param logger [Logger]
     */
    constructor(config: MapboxOnboardRouterConfig, logger: Logger?) {
        val tileDir = File(config.tilePath, TILES_DIR_NAME)
        if (!tileDir.exists()) {
            tileDir.mkdirs()
        }

        this.navigatorNative = MapboxNativeNavigatorImpl
        this.config = config
        this.logger = logger
        val httpClient = HttpClient()
        val routerParams = RouterParams(
            config.tilePath,
            config.inMemoryTileCache,
            config.mapMatchingSpatialCache,
            config.threadsCount,
            config.endpoint?.let {
                TileEndpointConfiguration(
                    it.host,
                    it.version,
                    it.token,
                    httpClient.userAgent,
                    ""
                )
            })
        MapboxNativeNavigatorImpl.configureRouter(routerParams, httpClient)
    }

    // Package private for testing purposes
    internal constructor(
        navigator: MapboxNativeNavigator,
        config: MapboxOnboardRouterConfig,
        logger: Logger
    ) {
        this.navigatorNative = navigator
        this.config = config
        this.logger = logger
    }

    override fun getRoute(
        routeOptions: RouteOptions,
        callback: Router.Callback
    ) {

        val origin = routeOptions.coordinates().first()
        val destination = routeOptions.coordinates().last()
        routeOptions.coordinates().drop(1).dropLast(1)

        val offlineRouter = OfflineRoute.builder(

            RouteUrl(
                accessToken = routeOptions.accessToken(),
                user = routeOptions.user(),
                profile = routeOptions.profile(),
                orgin = origin,
                waypoints = routeOptions.coordinates(),
                destination = destination,
                steps = routeOptions.steps() ?: false,
                voiceIntruction = routeOptions.voiceInstructions() ?: false,
                bannerIntruction = routeOptions.bannerInstructions() ?: false,
                roundaboutExits = routeOptions.roundaboutExits() ?: false
            )
        ).build()

        retrieveRoute(offlineRouter.buildUrl(), callback)
    }

    override fun cancel() {
        mainJobControl.scope.cancel()
    }

    private fun retrieveRoute(url: String, callback: Router.Callback) {
        mainJobControl.scope.launch {
            val routerResult = withContext(ThreadController.IODispatcher) {
                navigatorNative.getRoute(url)
            }

            val routes: List<DirectionsRoute> = try {
                DirectionsResponse.fromJson(routerResult.json).routes()
            } catch (e: RuntimeException) {
                emptyList()
            }

            when {
                !routes.isNullOrEmpty() -> callback.onResponse(routes)
                else -> callback.onFailure(NavigationException(generateErrorMessage(routerResult.json)))
            }
        }
    }

    private fun generateErrorMessage(response: String): String {
        val (_, _, error, errorCode) = gson.fromJson(response, OfflineRouteError::class.java)
        val errorMessage = "Error occurred fetching offline route: $error - Code: $errorCode"
        logger?.e(Tag("MapboxOnboardRouter"), Message(errorMessage))
        return errorMessage
    }
}
