package com.mapbox.navigation.base.internal.route.parsing.models.directions

import androidx.annotation.RestrictTo
import com.mapbox.navigation.base.internal.route.parsing.ResponseToParse
import com.mapbox.navigation.base.route.NavigationRoute

@RestrictTo(RestrictTo.Scope.LIBRARY)
data class NavigationRouteParsingSuccessfulResult(
    val routes: List<NavigationRoute>,
)

/**
 * Entry point for parsing directions response in Android Nav SDK including unit tests.
 * This API is supposed to be used for initial route as well as reroute responses.
 * @see [com.mapbox.navigation.base.internal.route.parsing.parser.directions.NnAndModelsParallelNavigationRoutesParser]
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
interface NavigationRoutesParser {
    suspend fun parseDirectionsResponse(
        response: ResponseToParse,
    ): Result<NavigationRouteParsingSuccessfulResult>
}
