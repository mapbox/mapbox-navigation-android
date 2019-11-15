package com.mapbox.navigation.route.onboard

import android.app.Application
import com.mapbox.annotation.navigation.module.MapboxNavigationModule
import com.mapbox.annotation.navigation.module.MapboxNavigationModuleType
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.navigator.MapboxNativeNavigator
import com.mapbox.navigation.navigator.MapboxNativeNavigatorImpl
import com.mapbox.navigation.route.common.NavigationRoute
import com.mapbox.navigation.route.common.extension.mapToRoute
import com.mapbox.navigation.route.onboard.model.OfflineError
import com.mapbox.navigation.route.onboard.task.OfflineRouteRetrievalTask
import com.mapbox.navigation.utils.exceptions.NavigationException
import java.io.File

@MapboxNavigationModule(MapboxNavigationModuleType.OnboardRouter, skipConfiguration = true)
class MapboxOnboardRouter : Router {

    companion object {

        private const val TILE_PATH_NAME = "tiles"
    }

    private val tilePath: String
    private val offlineTileVersions: OfflineTileVersions
    private val navigatorNative: MapboxNativeNavigator
    private val application: Application
    private val accessToken: String

    /**
     * Creates an offline router which uses the specified offline path for storing and retrieving
     * data.
     *
     * @param offlinePath directory path where the offline data is located
     */
    constructor(offlinePath: String, application: Application, accessToken: String) {
        val tileDir = File(offlinePath, TILE_PATH_NAME)
        if (!tileDir.exists()) {
            tileDir.mkdirs()
        }

        this.tilePath = tileDir.absolutePath
        offlineTileVersions = OfflineTileVersions()
        this.navigatorNative = MapboxNativeNavigatorImpl
        this.application = application
        this.accessToken = accessToken
    }

    // Package private for testing purposes
    internal constructor(
        tilePath: String,
        offlineTileVersions: OfflineTileVersions,
        navigator: MapboxNativeNavigator,
        application: Application,
        accessToken: String
    ) {
        this.tilePath = tilePath
        this.offlineTileVersions = offlineTileVersions
        this.navigatorNative = navigator
        this.application = application
        this.accessToken = accessToken
    }

    override fun getRoute(
        routeOptions: RouteOptionsNavigation,
        callback: Router.Callback
    ) = Unit
        origin: Point,
        waypoints: List<Point>?,
        destination: Point,
        callback: Router.RouteCallback
    ) {
        val offlineRouter = OfflineRoute.builder(
            NavigationRoute.builder(application)
                .accessToken(accessToken)
                .origin(origin)
                .apply { waypoints?.forEach { addWaypoint(it) } }
                .destination(destination)
        )
            .build()

        OfflineRouteRetrievalTask(navigatorNative, object : OnOfflineRouteFoundCallback {
            override fun onRouteFound(route: DirectionsRoute) {
                callback.onRouteReady(route.mapToRoute())
            }

            override fun onError(error: OfflineError) {
                callback.onFailure(NavigationException(error.message))
            }
        })
            .execute(offlineRouter)
    }

    override fun cancel() = Unit

    class Config {
        fun compile(): String = TODO("not implemented")
    }
}
