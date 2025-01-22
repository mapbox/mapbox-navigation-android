package com.mapbox.navigation.core.preview

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.routealternatives.AlternativeRouteMetadata

/***
 * @param routesList - previewed routes ordered in way specific for mapbox navigation: [primary, alternative1, alternative2].
 * @param alternativesMetadata - alternative metadata for valid alternatives from [routesList].
 * @param originalRoutesList - original routes list which doesn't change order no matter which primary route is selected.
 * @param primaryRouteIndex - index of primary route from the [originalRoutesList].
 *
 * Use [routesList] when you want to pass routes to other Navigation SDK components,
 * for example start active guidance calling [MapboxNavigation.setNavigationRoutes] or
 * display a route using route line API. The majority of the Navigation SDK's API accepts routes in this format.
 *
 * Use [originalRoutesList] and [primaryRouteIndex] if you want to display routes as a list on UI.
 * In this case routes' order shouldn't change on UI when users pick different routes as primary.
 * [MapboxNavigation.changeRoutesPreviewPrimaryRoute] is designed to select a new primary route without
 * changing the order of the [originalRoutesList].
 */
@ExperimentalPreviewMapboxNavigationAPI
class RoutesPreview internal constructor(
    val routesList: List<NavigationRoute>,
    val alternativesMetadata: List<AlternativeRouteMetadata>,
    val originalRoutesList: List<NavigationRoute>,
    val primaryRouteIndex: Int,
) {
    /***
     * Primary route used for preview
     */
    val primaryRoute = originalRoutesList[primaryRouteIndex]

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RoutesPreview

        if (routesList != other.routesList) return false
        if (alternativesMetadata != other.alternativesMetadata) return false
        if (originalRoutesList != other.originalRoutesList) return false
        if (primaryRouteIndex != other.primaryRouteIndex) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = routesList.hashCode()
        result = 31 * result + alternativesMetadata.hashCode()
        result = 31 * result + originalRoutesList.hashCode()
        result = 31 * result + primaryRouteIndex.hashCode()
        return result
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun toString(): String {
        return "RoutesPreview(" +
            "routesList=$routesList, " +
            "alternativesMetadata=$alternativesMetadata, " +
            "originalRoutesList=$originalRoutesList, " +
            "primaryRouteIndex=$primaryRouteIndex" +
            ")"
    }
}
