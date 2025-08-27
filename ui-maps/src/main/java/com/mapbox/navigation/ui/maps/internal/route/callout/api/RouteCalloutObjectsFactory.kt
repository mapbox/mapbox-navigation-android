package com.mapbox.navigation.ui.maps.internal.route.callout.api

import androidx.annotation.RestrictTo
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.ui.maps.route.callout.api.RouteCalloutUiState
import com.mapbox.navigation.ui.maps.route.callout.api.RouteCalloutUiStateData
import com.mapbox.navigation.ui.maps.route.callout.api.RouteCalloutUiStateProvider
import com.mapbox.navigation.ui.maps.route.callout.model.RouteCallout
import kotlin.time.Duration

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
object RouteCalloutObjectsFactory {

    fun createRouteCalloutUiStateData(
        callouts: List<RouteCalloutUiState>,
    ): RouteCalloutUiStateData {
        return RouteCalloutUiStateData(callouts)
    }

    fun createRouteCalloutUiState(
        routeCallout: RouteCallout,
        layerId: String,
    ): RouteCalloutUiState {
        return RouteCalloutUiState(routeCallout, layerId)
    }

    fun createRouteCallout(
        route: NavigationRoute,
        isPrimary: Boolean,
        durationDifferenceWithPrimary: Duration,
    ): RouteCallout {
        return RouteCallout(route, isPrimary, durationDifferenceWithPrimary)
    }

    fun createRouteCalloutUiStateProvider(
        routesSetToRouteLineDataProvider: RoutesSetToRouteLineDataProvider,
        routesAttachedToLayersDataProvider: RoutesAttachedToLayersDataProvider,
    ): RouteCalloutUiStateProvider {
        return RouteCalloutUiStateProvider(
            routesSetToRouteLineDataProvider,
            routesAttachedToLayersDataProvider,
        )
    }
}
