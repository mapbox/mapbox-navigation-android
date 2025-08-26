package com.mapbox.navigation.ui.maps.internal.route.callout.api.compose

import androidx.annotation.RestrictTo
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.ui.maps.route.callout.api.RoutesAttachedToLayersDataProvider
import com.mapbox.navigation.ui.maps.route.callout.api.RoutesSetToRouteLineDataProvider
import com.mapbox.navigation.ui.maps.route.callout.api.compose.CalloutUiState
import com.mapbox.navigation.ui.maps.route.callout.api.compose.CalloutUiStateData
import com.mapbox.navigation.ui.maps.route.callout.api.compose.CalloutUiStateProvider
import com.mapbox.navigation.ui.maps.route.callout.model.RouteCallout
import kotlin.time.Duration

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
object RouteCalloutObjectsFactory {

    fun createCalloutUiStateData(
        callouts: List<CalloutUiState>,
    ): CalloutUiStateData {
        return CalloutUiStateData(callouts)
    }

    fun createCalloutUiState(
        routeCallout: RouteCallout,
        layerId: String,
    ): CalloutUiState {
        return CalloutUiState(routeCallout, layerId)
    }

    fun createRouteCallout(
        route: NavigationRoute,
        isPrimary: Boolean,
        durationDifferenceWithPrimary: Duration,
    ): RouteCallout {
        return RouteCallout(route, isPrimary, durationDifferenceWithPrimary)
    }

    fun createCalloutUiStateProvider(
        routesSetToRouteLineDataProvider: RoutesSetToRouteLineDataProvider,
        routesAttachedToLayersDataProvider: RoutesAttachedToLayersDataProvider,
    ): CalloutUiStateProvider {
        return CalloutUiStateProvider(
            routesSetToRouteLineDataProvider,
            routesAttachedToLayersDataProvider,
        )
    }
}
