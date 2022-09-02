package com.mapbox.androidauto.car.preview

import androidx.annotation.UiThread
import com.mapbox.androidauto.car.search.PlaceRecord
import com.mapbox.androidauto.internal.logAndroidAuto
import com.mapbox.androidauto.internal.logAndroidAutoFailure
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.formatter.UnitType
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * This is a view interface. Each callback function represents a view that will be
 * shown for the situations.
 */
interface CarRouteRequestCallback {
    fun onRoutesReady(placeRecord: PlaceRecord, routes: List<NavigationRoute>)
    fun onUnknownCurrentLocation()
    fun onDestinationLocationUnknown()
    fun onNoRoutesFound()
}

/**
 * Service class that requests routes for the preview screen.
 */
class CarRouteRequest(
    val mapboxNavigation: MapboxNavigation,
    private val routeOptionsInterceptor: CarRouteOptionsInterceptor,
    private val navigationLocationProvider: NavigationLocationProvider,
) {
    internal var currentRequestId: Long? = null

    @UiThread
    suspend fun requestSync(placeRecord: PlaceRecord): List<NavigationRoute>? {
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { cancelRequest() }
            request(
                placeRecord,
                object : CarRouteRequestCallback {

                    override fun onRoutesReady(
                        placeRecord: PlaceRecord,
                        routes: List<NavigationRoute>
                    ) {
                        continuation.resume(routes)
                    }

                    override fun onUnknownCurrentLocation() {
                        continuation.resume(value = null)
                    }

                    override fun onDestinationLocationUnknown() {
                        continuation.resume(value = null)
                    }

                    override fun onNoRoutesFound() {
                        continuation.resume(value = null)
                    }
                }
            )
        }
    }

    /**
     * When a search result was selected, request a route.
     *
     * @param searchResults potential destinations for directions
     */
    @UiThread
    fun request(placeRecord: PlaceRecord, callback: CarRouteRequestCallback) {
        currentRequestId?.let { mapboxNavigation.cancelRouteRequest(it) }

        val location = navigationLocationProvider.lastLocation
        if (location == null) {
            logAndroidAutoFailure("CarRouteRequest.onUnknownCurrentLocation")
            callback.onUnknownCurrentLocation()
            return
        }
        val origin = Point.fromLngLat(location.longitude, location.latitude)

        when (placeRecord.coordinate) {
            null -> {
                logAndroidAutoFailure("CarRouteRequest.onSearchResultLocationUnknown")
                callback.onDestinationLocationUnknown()
            }
            else -> {
                currentRequestId = mapboxNavigation.requestRoutes(
                    carRouteOptions(origin, placeRecord.coordinate),
                    carCallbackTransformer(placeRecord, callback)
                )
            }
        }
    }

    @UiThread
    fun cancelRequest() {
        currentRequestId?.let { mapboxNavigation.cancelRouteRequest(it) }
    }

    /**
     * Default [RouteOptions] for the car.
     */
    private fun carRouteOptions(origin: Point, destination: Point) = RouteOptions.builder()
        .applyDefaultNavigationOptions()
        .language(mapboxNavigation.navigationOptions.distanceFormatterOptions.locale.language)
        .voiceUnits(
            when (mapboxNavigation.navigationOptions.distanceFormatterOptions.unitType) {
                UnitType.IMPERIAL -> DirectionsCriteria.IMPERIAL
                UnitType.METRIC -> DirectionsCriteria.METRIC
            },
        )
        .alternatives(true)
        .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
        .coordinatesList(listOf(origin, destination))
        .layersList(listOf(mapboxNavigation.getZLevel(), null))
        .metadata(true)
        .let { routeOptionsInterceptor.intercept(it) }
        .build()

    /**
     * This creates a callback that transforms
     * [RouterCallback] into [CarRouteRequestCallback]
     */
    private fun carCallbackTransformer(
        searchResult: PlaceRecord,
        callback: CarRouteRequestCallback
    ): NavigationRouterCallback {
        return object : NavigationRouterCallback {
            override fun onRoutesReady(routes: List<NavigationRoute>, routerOrigin: RouterOrigin) {
                currentRequestId = null

                logAndroidAuto("onRoutesReady ${routes.size}")
                callback.onRoutesReady(searchResult, routes)
            }

            override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {
                currentRequestId = null

                logAndroidAutoFailure("onCanceled $routeOptions")
                callback.onNoRoutesFound()
            }

            override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
                currentRequestId = null

                logAndroidAutoFailure("onRoutesRequestFailure $routeOptions $reasons")
                callback.onNoRoutesFound()
            }
        }
    }
}
