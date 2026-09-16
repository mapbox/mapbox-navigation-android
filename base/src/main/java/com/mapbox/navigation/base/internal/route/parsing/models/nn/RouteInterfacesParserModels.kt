package com.mapbox.navigation.base.internal.route.parsing.models.nn

import androidx.annotation.RestrictTo
import com.mapbox.navigation.base.internal.utils.AlternativesParsingResult
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigator.RouteInterface

@RestrictTo(RestrictTo.Scope.LIBRARY)
data class ContinuousAlternativesParsingSuccessfulResult(
    val routes: List<NavigationRoute>,
)

interface RouteInterfacesParser {
    suspend fun parserContinuousAlternatives(
        routes: List<RouteInterface>,
    ): AlternativesParsingResult<Result<ContinuousAlternativesParsingSuccessfulResult>>

    suspend fun parseRoutes(
        routes: List<RouteInterface>,
    ): Result<ContinuousAlternativesParsingSuccessfulResult>
}
