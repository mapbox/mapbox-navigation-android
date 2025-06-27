package com.mapbox.navigation.ui.maps.internal.route.callout.api.compose

import androidx.annotation.RestrictTo
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.ui.maps.route.callout.api.RoutesAttachedToLayersDataProvider
import com.mapbox.navigation.ui.maps.route.callout.api.RoutesSetToRouteLineDataProvider
import com.mapbox.navigation.ui.maps.route.callout.api.compose.CalloutComposeUiState
import com.mapbox.navigation.ui.maps.route.callout.api.compose.CalloutComposeUiStateData
import com.mapbox.navigation.ui.maps.route.callout.api.compose.CalloutComposeUiStateProvider
import com.mapbox.navigation.ui.maps.route.callout.model.RouteCallout
import kotlin.time.Duration

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
object RouteCalloutObjectsFactory {

    fun createCalloutComposeUiStateData(
        callouts: List<CalloutComposeUiState>,
    ): CalloutComposeUiStateData {
        return CalloutComposeUiStateData(callouts)
    }

    fun createCalloutComposeUiState(
        routeCallout: RouteCallout,
        layerId: String,
    ): CalloutComposeUiState {
        return CalloutComposeUiState(routeCallout, layerId)
    }

    fun createRouteCallout(
        route: NavigationRoute,
        isPrimary: Boolean,
        durationDifferenceWithPrimary: Duration,
    ): RouteCallout {
        return RouteCallout(route, isPrimary, durationDifferenceWithPrimary)
    }

    fun createCalloutComposeUiStateProvider(
        routesSetToRouteLineDataProvider: RoutesSetToRouteLineDataProvider,
        routesAttachedToLayersDataProvider: RoutesAttachedToLayersDataProvider,
    ): CalloutComposeUiStateProvider {
        return CalloutComposeUiStateProvider(
            routesSetToRouteLineDataProvider,
            routesAttachedToLayersDataProvider,
        )
    }
}
