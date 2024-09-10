package com.mapbox.navigation.core.reroute

import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigator.RouteAlternativesControllerInterface

/***
 * [RouteAlternativesControllerInterface] adds additional private parameters for route requests to
 * help Directions API provide a better alternative route. [CleanupCARelatedParamsAdapter] makes
 * sure that reroute requests won't have parameters applicable only to CA.
 */
internal class CleanupCARelatedParamsAdapter : InternalRerouteOptionsAdapter {
    override fun onRouteOptions(
        routeOptions: RouteOptions,
        params: RouteOptionsAdapterParams,
    ): RouteOptions {
        return routeOptions.toBuilder()
            .unrecognizedJsonProperties(
                routeOptions.unrecognizedJsonProperties
                    ?.minus(
                        listOf(
                            "current_alternatives",
                            "optimize_alternatives",
                        ),
                    ),
            )
            .build()
    }
}
