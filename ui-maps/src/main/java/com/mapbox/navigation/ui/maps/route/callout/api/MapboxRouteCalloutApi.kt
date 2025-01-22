package com.mapbox.navigation.ui.maps.route.callout.api

import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.utils.DecodeUtils.completeGeometryToPoints
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.routealternatives.AlternativeRouteMetadata
import com.mapbox.navigation.ui.maps.internal.route.callout.model.DurationDifferenceType
import com.mapbox.navigation.ui.maps.internal.route.callout.model.RouteCallout
import com.mapbox.navigation.ui.maps.route.callout.model.MapboxRouteCalloutApiOptions
import com.mapbox.navigation.ui.maps.route.callout.model.RouteCalloutData
import com.mapbox.navigation.ui.maps.route.callout.model.RouteCalloutType
import com.mapbox.navigation.utils.internal.logW
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

/**
 * Responsible for generating route line annotation data which can be rendered on the map
 * to visualize a callout representing the duration of the route (total ETA or relative diff
 * with the primary route).
 * The callout is calculated based on the routes and the data returned should
 * be rendered on the map using the [MapboxRouteCalloutView] class. Generally this class should be
 * called once new route (or set of routes) is available
 *
 * The two principal classes for the route callouts are the [MapboxRouteCalloutApi] and the
 * [MapboxRouteCalloutView].
 *
 * Like the route line components the [MapboxRouteCalloutApi] consumes data from the Navigation SDK,
 * specifically the [NavigationRoute], and produces data for rendering on the map by the
 * [MapboxRouteCalloutView].
 *
 * @param apiOptions used for determining the appearance and/or behavior of the route callouts
 */
@ExperimentalPreviewMapboxNavigationAPI
class MapboxRouteCalloutApi(
    apiOptions: MapboxRouteCalloutApiOptions = MapboxRouteCalloutApiOptions.Builder().build(),
) {
    var options: MapboxRouteCalloutApiOptions = apiOptions
        private set
    private val routes: MutableList<NavigationRoute> = mutableListOf()
    private val primaryRoutePoints: MutableList<Point> = mutableListOf()
    private val alternativeRoutesMetadata: MutableList<AlternativeRouteMetadata> = mutableListOf()

    /**
     * Update a subset of route callout options.
     *
     * @param options new options
     *
     * Note that updating options doesn't re-render anything automatically.
     * For these options to be applied, you need to do the following:
     * ```kotlin
     * val routeCalloutResult = mapboxRouteCalloutApi.updateOptions(newOptions)
     * mapboxRouteCalloutView.renderCallouts(routeCalloutResult)
     * ```
     */
    fun updateOptions(options: MapboxRouteCalloutApiOptions): RouteCalloutData {
        this.options = options

        return RouteCalloutData(createCallouts())
    }

    /**
     * Sets the routes that will be operated on.
     *
     * @param newRoutes one or more routes. The first route in the collection will be considered
     * the primary route and any additional routes will be alternate routes.
     */
    fun setNavigationRoutes(newRoutes: List<NavigationRoute>): RouteCalloutData {
        return setNavigationRoutes(newRoutes, emptyList())
    }

    /**
     * Sets the routes that will be operated on.
     *
     * @param newRoutes one or more routes. The first route in the collection will be considered
     * the primary route and any additional routes will be alternate routes.
     * @param alternativeRoutesMetadata if available, helps [MapboxRouteCalloutApi] find
     * the deviation point to extract different geometry segment the callout should be attaching to.
     * See [MapboxNavigation.getAlternativeMetadataFor].
     */
    fun setNavigationRoutes(
        newRoutes: List<NavigationRoute>,
        alternativeRoutesMetadata: List<AlternativeRouteMetadata>,
    ): RouteCalloutData {
        val distinctNewRoutes = newRoutes.distinctBy { it.id }
        if (distinctNewRoutes.size < newRoutes.size) {
            logW(
                "Routes provided to MapboxRouteCalloutApi contain duplicates " +
                    "(based on NavigationRoute#id) - using only distinct instances",
                LOG_CATEGORY,
            )
        }

        this.alternativeRoutesMetadata.addAll(alternativeRoutesMetadata)

        routes.clear()
        routes.addAll(distinctNewRoutes)
        routes.firstOrNull()?.run {
            primaryRoutePoints.clear()
            primaryRoutePoints.addAll(
                this.directionsRoute.completeGeometryToPoints(),
            )
        }

        return RouteCalloutData(createCallouts())
    }

    private fun createCallouts(): List<RouteCallout> {
        val primaryRoute = routes.firstOrNull() ?: return emptyList()
        val alternativeRoutes = routes.drop(1)

        return when (options.routeCalloutType) {
            RouteCalloutType.RouteDurations -> createRoutePreviewCallouts(
                primaryRoute,
                alternativeRoutes,
            )

            RouteCalloutType.RelativeDurationsOnAlternative -> createActiveGuidanceRouteCallouts(
                primaryRoute,
                alternativeRoutes,
                alternativeRoutesMetadata,
            )
        }
    }

    private fun extractDifferentGeometry(
        primaryRoutePoints: List<Point>,
        alternativeRoute: NavigationRoute,
        metadata: AlternativeRouteMetadata?,
    ): LineString {
        val deviationPoint = metadata?.forkIntersectionOfAlternativeRoute?.geometryIndexInRoute
        val differentPoints = PointDifferenceFinder.extractDifference(
            primaryRoutePoints,
            alternativeRoute.directionsRoute.completeGeometryToPoints(),
            deviationPoint,
        )

        return LineString.fromLngLats(differentPoints)
    }

    private fun createActiveGuidanceRouteCallouts(
        primaryRoute: NavigationRoute,
        alternativeRoutes: List<NavigationRoute>,
        alternativeRouteMetadata: List<AlternativeRouteMetadata>,
    ): List<RouteCallout.DurationDifference> {
        return alternativeRoutes.map { route ->
            val altRouteDuration =
                alternativeRouteMetadata.firstOrNull { it.navigationRoute.id == route.id }
                    ?.infoFromStartOfPrimary?.duration?.seconds
                    ?: route.directionsRoute.duration().seconds

            val (durationDifference, type) = calculateDurationDifference(
                primaryRoute.directionsRoute.duration().seconds,
                altRouteDuration,
            )

            RouteCallout.DurationDifference(
                route,
                durationDifference,
                type,
            )
        }
    }

    private fun calculateDurationDifference(
        primaryDuration: Duration,
        alternativeDuration: Duration,
    ): Pair<Duration, DurationDifferenceType> {
        val durationDiff =
            (primaryDuration - alternativeDuration).roundUpByAbs(DurationUnit.MINUTES)
        val durationDiffAbsoluteValue = durationDiff.absoluteValue

        return durationDiffAbsoluteValue to when {
            durationDiffAbsoluteValue <= options.similarDurationDelta -> DurationDifferenceType.Same

            durationDiff < 0.seconds -> DurationDifferenceType.Slower

            else -> DurationDifferenceType.Faster
        }
    }

    private fun createRoutePreviewCallouts(
        primaryRoute: NavigationRoute,
        alternativeRoutes: List<NavigationRoute>,
    ): List<RouteCallout.Eta> {
        return buildList(capacity = alternativeRoutes.size + 1) {
            add(RouteCallout.Eta(primaryRoute, isPrimary = true))

            alternativeRoutes.mapTo(destination = this) { alternativeRoute ->
                RouteCallout.Eta(alternativeRoute, isPrimary = false)
            }
        }
    }

    private companion object {

        private const val LOG_CATEGORY = "MapboxRouteCalloutApi"
    }
}
