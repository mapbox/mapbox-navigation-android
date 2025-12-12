package com.mapbox.navigation.navigator.internal.utils

import androidx.annotation.RestrictTo
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.mapbox.api.directionsrefresh.v1.models.DirectionsRefreshResponse
import com.mapbox.api.directionsrefresh.v1.models.DirectionsRouteRefresh
import com.mapbox.api.directionsrefresh.v1.models.RouteLegRefresh
import com.mapbox.navigation.base.internal.utils.Constants
import com.mapbox.navigation.base.route.NavigationRoute

@RestrictTo(RestrictTo.Scope.LIBRARY)
fun NavigationRoute.toDirectionsRefreshResponse(): DirectionsRefreshResponse {
    val refreshedLegs: List<RouteLegRefresh>? = directionsRoute.legs()?.map { routeLeg ->
        RouteLegRefresh.builder()
            .annotation(routeLeg.annotation())
            .incidents(routeLeg.incidents())
            .notifications(routeLeg.notifications())
            .build()
    }
    val refreshedWaypoints = waypoints
    val refreshRoute: DirectionsRouteRefresh = DirectionsRouteRefresh.builder()
        .legs(refreshedLegs)
        .unrecognizedJsonProperties(
            refreshedWaypoints?.let { waypoints ->
                mapOf(
                    Constants.RouteResponse.KEY_WAYPOINTS to JsonArray().apply {
                        waypoints.forEach { waypoint ->
                            add(JsonParser.parseString(waypoint.toJson()))
                        }
                    },
                )
            },
        )
        .build()

    return DirectionsRefreshResponse.builder()
        .code("200")
        .route(refreshRoute)
        .build()
}
