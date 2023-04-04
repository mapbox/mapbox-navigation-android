package com.mapbox.navigation.ui.maps.route.line

import androidx.annotation.ColorInt
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.routealternatives.AlternativeRouteMetadata
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.model.ClosestRouteValue
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.NavigationRouteLine
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineClearValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineError
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineExpressionProvider
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineUpdateValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteNotFound
import com.mapbox.navigation.ui.maps.route.line.model.RouteSetValue
import com.mapbox.navigation.ui.maps.route.line.model.toNavigationRouteLines
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
    @Deprecated(
        "use #setNavigationRouteLines(List<NavigationRouteLine>) instead",
        ReplaceWith(
            "setNavigationRouteLines(newRoutes.toNavigationRouteLines(), consumer)",
            "com.mapbox.navigation.ui.maps.route.line.model.toNavigationRouteLines"
        )
    )
    suspend fun MapboxRouteLineApi.setRoutes(newRoutes: List<RouteLine>):
        Expected<RouteLineError, RouteSetValue> {
        return suspendCancellableCoroutine { continuation ->
            this.setNavigationRouteLines(
                newRoutes.toNavigationRouteLines()
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
    suspend fun MapboxRouteLineApi.setNavigationRouteLines(
        newRoutes: List<NavigationRouteLine>
    ): Expected<RouteLineError, RouteSetValue> {
        return setNavigationRouteLines(
            newRoutes = newRoutes,
            alternativeRoutesMetadata = emptyList()
        )
    }

    /**
     * Sets the routes that will be operated on.
     *
     * @param newRoutes one or more routes. The first route in the collection will be considered
     * @param activeLegIndex the index of the currently active leg of the primary route.
     *  This is used when [MapboxRouteLineOptions.styleInactiveRouteLegsIndependently] is enabled.
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
            alternativeRoutesMetadata = emptyList()
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
        alternativeRoutesMetadata: List<AlternativeRouteMetadata>
    ): Expected<RouteLineError, RouteSetValue> {
        return setNavigationRouteLines(newRoutes, 0, alternativeRoutesMetadata)
    }

    /**
     * Sets the routes that will be operated on.
     *
     * @param newRoutes one or more routes. The first route in the collection will be considered
     * the primary route and any additional routes will be alternate routes.
     * @param activeLegIndex the index of the currently active leg of the primary route.
     *  This is used when [MapboxRouteLineOptions.styleInactiveRouteLegsIndependently] is enabled.
     * @param alternativeRoutesMetadata if available, the update will hide the portions of the alternative routes
     * until the deviation point with the primary route. See [MapboxNavigation.getAlternativeMetadataFor].
     *
     * @return a state which contains the side effects to be applied to the map
     */
    suspend fun MapboxRouteLineApi.setNavigationRouteLines(
        newRoutes: List<NavigationRouteLine>,
        activeLegIndex: Int,
        alternativeRoutesMetadata: List<AlternativeRouteMetadata>
    ): Expected<RouteLineError, RouteSetValue> {
        return suspendCancellableCoroutine { continuation ->
            this.setNavigationRouteLines(
                newRoutes,
                activeLegIndex,
                alternativeRoutesMetadata
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
        newRoutes: List<NavigationRoute>
    ): Expected<RouteLineError, RouteSetValue> {
        return setNavigationRoutes(newRoutes = newRoutes, alternativeRoutesMetadata = emptyList())
    }

    /**
     * Sets the routes that will be operated on.
     *
     * @param newRoutes one or more routes. The first route in the collection will be considered
     * the primary route and any additional routes will be alternate routes.
     * @param activeLegIndex the index of the currently active leg of the primary route.
     *  This is used when [MapboxRouteLineOptions.styleInactiveRouteLegsIndependently] is enabled.
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
            alternativeRoutesMetadata = emptyList()
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
        alternativeRoutesMetadata: List<AlternativeRouteMetadata>
    ): Expected<RouteLineError, RouteSetValue> {
        return setNavigationRoutes(newRoutes, 0, alternativeRoutesMetadata)
    }

    /**
     * Sets the routes that will be operated on.
     *
     * @param newRoutes one or more routes. The first route in the collection will be considered
     * the primary route and any additional routes will be alternate routes.
     * @param activeLegIndex the index of the currently active leg of the primary route.
     *  This is used when [MapboxRouteLineOptions.styleInactiveRouteLegsIndependently] is enabled.
     * @param alternativeRoutesMetadata if available, the update will hide the portions of the alternative routes
     * until the deviation point with the primary route. See [MapboxNavigation.getAlternativeMetadataFor].
     *
     * @return a state which contains the side effects to be applied to the map
     */
    suspend fun MapboxRouteLineApi.setNavigationRoutes(
        newRoutes: List<NavigationRoute>,
        activeLegIndex: Int,
        alternativeRoutesMetadata: List<AlternativeRouteMetadata>
    ): Expected<RouteLineError, RouteSetValue> {
        return suspendCancellableCoroutine { continuation ->
            this.setNavigationRoutes(
                newRoutes,
                activeLegIndex,
                alternativeRoutesMetadata
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
                padding
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

    /**
     * If successful this method returns a [RouteLineUpdateValue] that when rendered will
     * display the route line with the route leg indicated by the provided leg index highlighted.
     * All the other legs will only show a simple line with
     * [RouteLineColorResources.inActiveRouteLegsColor].
     *
     * This is intended to be used with routes that have multiple waypoints.
     * In addition, calling this method does not change the state of the route line.
     *
     * This method can be useful for showing a route overview with a specific route leg highlighted.
     *
     * @param legIndex the route leg index that should appear most prominent.
     * @return a [RouteLineUpdateValue] for rendering or an error
     */
    suspend fun MapboxRouteLineApi.showRouteWithLegIndexHighlighted(legIndex: Int):
        Expected<RouteLineError, RouteLineUpdateValue> {
        return suspendCancellableCoroutine { continuation ->
            this.showRouteWithLegIndexHighlighted(legIndex) { value ->
                continuation.resume(value)
            }

            continuation.invokeOnCancellation {
                this.cancel()
            }
        }
    }

    /**
     * Overrides the color [Expression] for the primary traffic line with the color indicated.
     * The entire traffic line will take on the color provided. If the [Expected] is an error
     * the same [Expected] object is returned.
     *
     * @param color the color to use for the primary traffic line.
     * @return an [Expected] with the color mutation applied
     */
    fun Expected<RouteLineError, RouteSetValue>.setPrimaryTrafficColor(@ColorInt color: Int):
        Expected<RouteLineError, RouteSetValue> {
        return this.fold({
            this
        }, {
            val expression = MapboxRouteLineUtils.getRouteLineExpression(0.0, color, color)
            this.setPrimaryTrafficColor(expression)
        })
    }

    /**
     * Overrides the color [Expression] for the primary traffic line with the [Expression] provided.
     * If the [Expected] is an error the same [Expected] object is returned.
     *
     * @param expression the [Expression] to use for the primary traffic line.
     * @return an [Expected] with the expression mutation applied
     */
    fun Expected<RouteLineError, RouteSetValue>.setPrimaryTrafficColor(expression: Expression):
        Expected<RouteLineError, RouteSetValue> {
        return this.fold({
            this
        }, { routeSetValue ->
            val updatedValue = routeSetValue.toMutableValue().also {
                it.primaryRouteLineData = replaceColorExpression(
                    routeSetValue.primaryRouteLineData,
                    expression
                )
            }.toImmutableValue()
            ExpectedFactory.createValue(updatedValue)
        })
    }

    /**
     * Overrides the color [Expression] for the alternative traffic lines with the color indicated.
     * The entire traffic line will take on the color provided. If the [Expected] is an error
     * the same [Expected] object is returned.
     *
     * @param color the color to use for the alternative traffic lines.
     * @return an [Expected] with the color mutation applied
     */
    fun Expected<RouteLineError, RouteSetValue>.setAlternativeTrafficColor(@ColorInt color: Int):
        Expected<RouteLineError, RouteSetValue> {
        return this.fold({
            this
        }, {
            val expression = MapboxRouteLineUtils.getRouteLineExpression(0.0, color, color)
            this.setAlternativeTrafficColor(expression)
        })
    }

    /**
     * Overrides the color [Expression] for the alternative traffic lines with the [Expression] provided.
     * If the [Expected] is an error the same [Expected] object is returned.
     *
     * @param expression the [Expression] to use for the alternative traffic lines.
     * @return an [Expected] with the expression mutation applied
     */
    fun Expected<RouteLineError, RouteSetValue>.setAlternativeTrafficColor(expression: Expression):
        Expected<RouteLineError, RouteSetValue> {
        return this.fold({
            this
        }, { routeSetValue ->
            val updatedValue = routeSetValue.toMutableValue().also {
                it.alternativeRouteLinesData = it.alternativeRouteLinesData.map { routeLineData ->
                    replaceColorExpression(routeLineData, expression)
                }
            }.toImmutableValue()
            ExpectedFactory.createValue(updatedValue)
        })
    }

    private fun replaceColorExpression(routeLineData: RouteLineData, expression: Expression):
        RouteLineData {
        val updatedDynamicData = routeLineData.dynamicData.toMutableValue().also {
            it.trafficExpressionProvider = RouteLineExpressionProvider {
                expression
            }
        }.toImmutableValue()

        return routeLineData.toMutableValue().also {
            it.dynamicData = updatedDynamicData
        }.toImmutableValue()
    }
}
