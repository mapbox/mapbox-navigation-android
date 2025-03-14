package com.mapbox.navigation.ui.maps.route.callout.api

import androidx.annotation.RestrictTo
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.routealternatives.AlternativeRouteMetadata
import com.mapbox.navigation.ui.maps.route.callout.model.RouteCallout
import com.mapbox.navigation.ui.maps.route.callout.model.RouteCalloutData
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

/**
 * Responsible for generating route line annotation data which can be rendered on the map
 * to visualize a callout.
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
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@ExperimentalPreviewMapboxNavigationAPI
class MapboxRouteCalloutApi internal constructor() {

    /**
     * Sets the routes that will be operated on.
     *
     * @param newRoutes one or more routes. The first route in the collection will be considered
     * the primary route and any additional routes will be alternate routes.
     * @param alternativeRoutesMetadata if available, helps [MapboxRouteCalloutApi] find
     * the deviation point to extract different geometry segment the callout should be attaching to.
     * See [MapboxNavigation.getAlternativeMetadataFor].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    fun setNavigationRoutes(
        newRoutes: List<NavigationRoute>,
        alternativeRoutesMetadata: List<AlternativeRouteMetadata>,
    ): RouteCalloutData {
        return RouteCalloutData(createCallouts(newRoutes, alternativeRoutesMetadata))
    }

    private fun createCallouts(
        routes: List<NavigationRoute>,
        alternativeRoutesMetadata: List<AlternativeRouteMetadata>,
    ): List<RouteCallout> {
        val primaryRoute = routes.firstOrNull() ?: return emptyList()
        val alternativeRoutes = routes.drop(1)

        return createRouteCallouts(
            primaryRoute,
            alternativeRoutes,
            alternativeRoutesMetadata,
        )
    }

    private fun createRouteCallouts(
        primaryRoute: NavigationRoute,
        alternativeRoutes: List<NavigationRoute>,
        alternativeRouteMetadata: List<AlternativeRouteMetadata>,
    ): List<RouteCallout> {
        return buildList(capacity = alternativeRoutes.size + 1) {
            add(
                RouteCallout(
                    primaryRoute,
                    isPrimary = true,
                    durationDifferenceWithPrimary = 0.seconds,
                ),
            )

            alternativeRoutes.mapTo(destination = this) { alternativeRoute ->
                val altRouteDuration =
                    alternativeRouteMetadata.firstOrNull {
                        it.navigationRoute.id == alternativeRoute.id
                    }
                        ?.infoFromStartOfPrimary?.duration?.seconds
                        ?: alternativeRoute.directionsRoute.duration().seconds

                val durationDifference = calculateDurationDifference(
                    primaryRoute.directionsRoute.duration().seconds,
                    altRouteDuration,
                )

                RouteCallout(
                    alternativeRoute,
                    isPrimary = false,
                    durationDifference,
                )
            }
        }
    }

    private fun calculateDurationDifference(
        primaryDuration: Duration,
        alternativeDuration: Duration,
    ): Duration {
        val durationDiff =
            (primaryDuration - alternativeDuration).roundUpByAbs(DurationUnit.MINUTES)

        return durationDiff
    }
}
