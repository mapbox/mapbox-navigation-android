package com.mapbox.navigation.base.internal.route.parsing.models.nn

import androidx.annotation.RestrictTo
import com.mapbox.navigation.base.internal.utils.AlternativesParsingResult
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigator.RouteInterface

@RestrictTo(RestrictTo.Scope.LIBRARY)
data class ContinuousAlternativesParsingSuccessfulResult(
    val routes: List<NavigationRoute>,
)

/**
 * Entry point for parsing multiple route interfaces into navigation routes.
 * This API is supposed to be used for continuous alternatives parsing.
 * @see [com.mapbox.navigation.base.internal.route.parsing.parser.nn.JsonResponseOptimizedRouteInterfaceParser]
 */
interface RouteInterfacesParser {
    suspend fun parserContinuousAlternatives(
        routes: List<RouteInterface>,
    ): AlternativesParsingResult<Result<ContinuousAlternativesParsingSuccessfulResult>>
}
