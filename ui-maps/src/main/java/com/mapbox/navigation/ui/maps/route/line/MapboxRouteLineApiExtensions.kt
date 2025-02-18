package com.mapbox.navigation.ui.maps.route.line

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.bindgen.Expected
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxMap
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.routealternatives.AlternativeRouteMetadata
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.ClosestRouteValue
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineApiOptions
import com.mapbox.navigation.ui.maps.route.line.model.NavigationRouteLine
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineClearValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineError
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineUpdateValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteNotFound
import com.mapbox.navigation.ui.maps.route.line.model.RouteSetValue
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Extension functions for [MapboxRouteLineApi] calls that are implemented as callbacks. This offers
 * an alternative to those callbacks by providing Kotlin oriented suspend functions.
 */
object MapboxRouteLineApiExtensions {

    /**
     * Sets the routes that will be operated on.
     *
     * @param newRoutes one or more routes. The first route in the collection will be considered
     * the primary route and any additional routes will be alternate routes.
     *
     * @return a state which contains the side effects to be applied to the map
     */
    suspend fun MapboxRouteLineApi.setNavigationRouteLines(
        newRoutes: List<NavigationRouteLine>,
    ): Expected<RouteLineError, RouteSetValue> {
        return setNavigationRouteLines(
            newRoutes = newRoutes,
            alternativeRoutesMetadata = emptyList(),
        )
    }

    /**
     * Sets the routes that will be operated on.
     *
     * @param newRoutes one or more routes. The first route in the collection will be considered
     * @param activeLegIndex the index of the currently active leg of the primary route.
     *  This is used when [MapboxRouteLineApiOptions.styleInactiveRouteLegsIndependently] is enabled.
     * the primary route and any additional routes will be alternate routes.
     *
     * @return a state which contains the side effects to be applied to the map
     */
    suspend fun MapboxRouteLineApi.setNavigationRouteLines(
        newRoutes: List<NavigationRouteLine>,
        activeLegIndex: Int,
    ): Expected<RouteLineError, RouteSetValue> {
        return setNavigationRouteLines(
            newRoutes = newRoutes,
            activeLegIndex = activeLegIndex,
            alternativeRoutesMetadata = emptyList(),
        )
    }

    /**
     * Sets the routes that will be operated on.
     *
     * @param newRoutes one or more routes. The first route in the collection will be considered
     * the primary route and any additional routes will be alternate routes.
     * @param alternativeRoutesMetadata if available, the update will hide the portions of the alternative routes
     * until the deviation point with the primary route. See [MapboxNavigation.getAlternativeMetadataFor].
     *
     * @return a state which contains the side effects to be applied to the map
     */
    suspend fun MapboxRouteLineApi.setNavigationRouteLines(
        newRoutes: List<NavigationRouteLine>,
        alternativeRoutesMetadata: List<AlternativeRouteMetadata>,
    ): Expected<RouteLineError, RouteSetValue> {
        return setNavigationRouteLines(newRoutes, 0, alternativeRoutesMetadata)
    }

    /**
     * Sets the routes that will be operated on.
     *
     * @param newRoutes one or more routes. The first route in the collection will be considered
     * the primary route and any additional routes will be alternate routes.
     * @param activeLegIndex the index of the currently active leg of the primary route.
     *  This is used when [MapboxRouteLineApiOptions.styleInactiveRouteLegsIndependently] is enabled.
     * @param alternativeRoutesMetadata if available, the update will hide the portions of the alternative routes
     * until the deviation point with the primary route. See [MapboxNavigation.getAlternativeMetadataFor].
     *
     * @return a state which contains the side effects to be applied to the map
     */
    suspend fun MapboxRouteLineApi.setNavigationRouteLines(
        newRoutes: List<NavigationRouteLine>,
        activeLegIndex: Int,
        alternativeRoutesMetadata: List<AlternativeRouteMetadata>,
    ): Expected<RouteLineError, RouteSetValue> {
        return suspendCancellableCoroutine { continuation ->
            this.setNavigationRouteLines(
                newRoutes,
                activeLegIndex,
                alternativeRoutesMetadata,
            ) { value -> continuation.resume(value) }

            continuation.invokeOnCancellation {
                this.cancel()
            }
        }
    }

    /**
     * Sets the routes that will be operated on.
     *
     * @param newRoutes one or more routes. The first route in the collection will be considered
     * the primary route and any additional routes will be alternate routes.
     *
     * @return a state which contains the side effects to be applied to the map
     */
    suspend fun MapboxRouteLineApi.setNavigationRoutes(
        newRoutes: List<NavigationRoute>,
    ): Expected<RouteLineError, RouteSetValue> {
        return setNavigationRoutes(newRoutes = newRoutes, alternativeRoutesMetadata = emptyList())
    }

    /**
     * Sets the routes that will be operated on.
     *
     * @param newRoutes one or more routes. The first route in the collection will be considered
     * the primary route and any additional routes will be alternate routes.
     * @param activeLegIndex the index of the currently active leg of the primary route.
     *  This is used when [MapboxRouteLineApiOptions.styleInactiveRouteLegsIndependently] is enabled.
     *
     * @return a state which contains the side effects to be applied to the map
     */
    suspend fun MapboxRouteLineApi.setNavigationRoutes(
        newRoutes: List<NavigationRoute>,
        activeLegIndex: Int,
    ): Expected<RouteLineError, RouteSetValue> {
        return setNavigationRoutes(
            newRoutes = newRoutes,
            activeLegIndex = activeLegIndex,
            alternativeRoutesMetadata = emptyList(),
        )
    }

    /**
     * Sets the routes that will be operated on.
     *
     * @param newRoutes one or more routes. The first route in the collection will be considered
     * the primary route and any additional routes will be alternate routes.
     * @param alternativeRoutesMetadata if available, the update will hide the portions of the alternative routes
     * until the deviation point with the primary route. See [MapboxNavigation.getAlternativeMetadataFor].
     *
     * @return a state which contains the side effects to be applied to the map
     */
    suspend fun MapboxRouteLineApi.setNavigationRoutes(
        newRoutes: List<NavigationRoute>,
        alternativeRoutesMetadata: List<AlternativeRouteMetadata>,
    ): Expected<RouteLineError, RouteSetValue> {
        return setNavigationRoutes(newRoutes, 0, alternativeRoutesMetadata)
    }

    /**
     * Sets the routes that will be operated on.
     *
     * @param newRoutes one or more routes. The first route in the collection will be considered
     * the primary route and any additional routes will be alternate routes.
     * @param activeLegIndex the index of the currently active leg of the primary route.
     *  This is used when [MapboxRouteLineApiOptions.styleInactiveRouteLegsIndependently] is enabled.
     * @param alternativeRoutesMetadata if available, the update will hide the portions of the alternative routes
     * until the deviation point with the primary route. See [MapboxNavigation.getAlternativeMetadataFor].
     *
     * @return a state which contains the side effects to be applied to the map
     */
    suspend fun MapboxRouteLineApi.setNavigationRoutes(
        newRoutes: List<NavigationRoute>,
        activeLegIndex: Int,
        alternativeRoutesMetadata: List<AlternativeRouteMetadata>,
    ): Expected<RouteLineError, RouteSetValue> {
        return suspendCancellableCoroutine { continuation ->
            this.setNavigationRoutes(
                newRoutes,
                activeLegIndex,
                alternativeRoutesMetadata,
            ) { value -> continuation.resume(value) }

            continuation.invokeOnCancellation {
                this.cancel()
            }
        }
    }

    /**
     * @return a state which contains the side effects to be applied to the map. The data
     * can be used to draw the current route line(s) on the map.
     */
    suspend fun MapboxRouteLineApi.getRouteDrawData(): Expected<RouteLineError, RouteSetValue> {
        return suspendCancellableCoroutine { continuation ->
            this.getRouteDrawData { value -> continuation.resume(value) }

            continuation.invokeOnCancellation {
                this.cancel()
            }
        }
    }

    /**
     * Updates the state of the route line based on data in the [RouteProgress] passing a result
     * to the consumer that should be rendered by the [MapboxRouteLineView].
     *
     * Calling this method and rendering the result is required in order to use the vanishing
     * route line feature and/or to style inactive route legs independently and/or display multi-leg
     * routes with the active leg appearing to overlap the inactive leg(s).
     *
     * This method will execute tasks on a background thread.
     * There is a cancel method which will cancel the background tasks.
     *
     * @param routeProgress a route progress object
     * @return a value containing the [RouteLineUpdateValue] or an error
     */
    suspend fun MapboxRouteLineApi.updateWithRouteProgress(
        routeProgress: RouteProgress,
    ): Expected<RouteLineError, RouteLineUpdateValue> {
        return suspendCancellableCoroutine { continuation ->
            this.updateWithRouteProgress(routeProgress) { value -> continuation.resume(value) }

            continuation.invokeOnCancellation {
                this.cancel()
            }
        }
    }

    /**
     * The map will be queried for a route line feature at the target point or a bounding box
     * centered at the target point with a padding value determining the box's size. If a route
     * feature is found the index of that route in this class's route collection is returned. The
     * primary route is given precedence if more than one route is found.
     *
     * @param target a target latitude/longitude serving as the search point
     * @param mapboxMap a reference to the [MapboxMap] that will be queried
     * @param padding a sizing value added to all sides of the target point for creating a bounding
     * box to search in.
     *
     * @return a value containing the [DirectionsRoute] found or an error indicating no route was
     * found.
     */
    suspend fun MapboxRouteLineApi.findClosestRoute(
        target: Point,
        mapboxMap: MapboxMap,
        padding: Float,
    ): Expected<RouteNotFound, ClosestRouteValue> {
        return suspendCancellableCoroutine { continuation ->
            this.findClosestRoute(
                target,
                mapboxMap,
                padding,
            ) { value -> continuation.resume(value) }

            continuation.invokeOnCancellation {
                this.cancel()
            }
        }
    }

    /**
     * Clears the route line data.
     *
     * @return a state representing the side effects to be rendered on the map. In this case
     * the map should appear without any route lines.
     */
    suspend fun MapboxRouteLineApi.clearRouteLine(): Expected<RouteLineError, RouteLineClearValue> {
        return suspendCancellableCoroutine { continuation ->
            this.clearRouteLine { value -> continuation.resume(value) }

            continuation.invokeOnCancellation {
                this.cancel()
            }
        }
    }
}
