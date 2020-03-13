package com.mapbox.navigation.route.onboard

import android.util.Log
import com.google.gson.Gson
import com.mapbox.annotation.navigation.module.MapboxNavigationModule
import com.mapbox.annotation.navigation.module.MapboxNavigationModuleType
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.options.MapboxOnboardRouterConfig
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.base.route.internal.RouteUrl
import com.mapbox.navigation.navigator.MapboxNativeNavigator
import com.mapbox.navigation.route.onboard.model.OfflineRouteError
import com.mapbox.navigation.utils.exceptions.NavigationException
import com.mapbox.navigation.utils.thread.ThreadController
import com.mapbox.navigator.RouterParams
import com.mapbox.navigator.TileEndpointConfiguration
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * MapboxOnboardRouter provides offline route fetching
 *
 * It uses offline storage path to store and retrieve data, setup endpoint,
 * tiles' version, token. Config is provided via [MapboxOnboardRouterConfig].
 */
@MapboxNavigationModule(MapboxNavigationModuleType.OnboardRouter, skipConfiguration = true)
class MapboxOnboardRouter(
    private val navigatorNative: MapboxNativeNavigator,
    config: MapboxOnboardRouterConfig
) : Router {

    companion object {
        private const val TILES_DIR_NAME = "tiles"
    }

    private val mainJobControl by lazy {
        ThreadController.getMainScopeAndRootJob()
    }
    private val gson = Gson()

    init {
        if (config.tilePath.isNotEmpty()) {
            val tileDir = File(config.tilePath, TILES_DIR_NAME)
            if (!tileDir.exists()) {
                tileDir.mkdirs()
            }
            val routerParams = RouterParams(
                tileDir.absolutePath,
                config.inMemoryTileCache,
                config.mapMatchingSpatialCache,
                config.threadsCount,
                config.endpoint?.let {
                    TileEndpointConfiguration(
                        it.host,
                        it.version,
                        it.token,
                        it.userAgent,
                        ""
                    )
                })
            navigatorNative.configureRouter(routerParams, null)
        }
    }

    override fun getRoute(
        routeOptions: RouteOptions,
        callback: Router.Callback
    ) {
        val origin = routeOptions.coordinates().first()
        val destination = routeOptions.coordinates().last()
        val waypoints = routeOptions.coordinates().drop(1).dropLast(1)

        val offlineRouter = OfflineRoute.builder(
            RouteUrl(
                accessToken = routeOptions.accessToken(),
                user = routeOptions.user(),
                profile = routeOptions.profile(),
                origin = origin,
                waypoints = waypoints,
                destination = destination,
                steps = routeOptions.steps() ?: RouteUrl.STEPS_DEFAULT_VALUE,
                voiceInstruction = routeOptions.voiceInstructions()
                    ?: RouteUrl.VOICE_INSTRUCTION_DEFAULT_VALUE,
                voiceUnits = routeOptions.voiceUnits(),
                bannerInstruction = routeOptions.bannerInstructions()
                    ?: RouteUrl.BANNER_INSTRUCTION_DEFAULT_VALUE,
                roundaboutExits = routeOptions.roundaboutExits()
                    ?: RouteUrl.ROUNDABOUT_EXITS_DEFAULT_VALUE,
                alternatives = routeOptions.alternatives() ?: RouteUrl.ALTERNATIVES_DEFAULT_VALUE,
                continueStraight = routeOptions.continueStraight(),
                exclude = routeOptions.exclude(),
                language = routeOptions.language(),
                bearings = routeOptions.bearings(),
                waypointNames = routeOptions.waypointNames(),
                waypointTargets = routeOptions.waypointTargets(),
                waypointIndices = routeOptions.waypointIndices(),
                approaches = routeOptions.approaches(),
                radiuses = routeOptions.radiuses(),
                walkingSpeed = routeOptions.walkingOptions()?.walkingSpeed(),
                walkwayBias = routeOptions.walkingOptions()?.walkwayBias(),
                alleyBias = routeOptions.walkingOptions()?.alleyBias()
            )
        ).build()

        retrieveRoute(offlineRouter.buildUrl(), callback)
    }

    override fun cancel() {
        mainJobControl.job.cancelChildren()
    }

    private fun retrieveRoute(url: String, callback: Router.Callback) {
        mainJobControl.scope.launch {
            try {
                val routerResult = getRoute(url)

                val routes: List<DirectionsRoute> = try {
                    parseDirectionsRoutes(routerResult.json)
                } catch (e: RuntimeException) {
                    emptyList()
                }

                when {
                    !routes.isNullOrEmpty() -> callback.onResponse(routes)
                    else -> callback.onFailure(NavigationException(generateErrorMessage(routerResult.json)))
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

    private fun generateErrorMessage(response: String): String {
        val (_, _, error, errorCode) = gson.fromJson(response, OfflineRouteError::class.java)
        val errorMessage = "Error occurred fetching offline route: $error - Code: $errorCode"
        Log.e("MapboxOnboardRouter", errorMessage)
        return errorMessage
    }
}
