package com.mapbox.navigation.base.internal.utils

import androidx.annotation.VisibleForTesting
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

internal object DirectionsRouteMissingConditionsCheck {

    @VisibleForTesting
    internal val ERROR_MESSAGE_TEMPLATE =
        """DirectionsRoute doesn't contain enough data for turn by turn navigation. 
                The SDK must consume directly (without mappers) NavigationRoute.
                See `MapboxNavigation#requestRoutes(RouteOptions, NavigationRouterCallback)` 
                and `NavigationRoute#create`). Params cannot be processed with DirectionsRoute: """
            .trimIndent()

    /**
     * Some **new** Navigation API features bring data with [DirectionsResponse] that is required
     * for turn-by-turn navigation. The SDK cannot mock this data, so turn-by-turn experience can
     * be inconsistent (for instance, `EV routing` brings metadata with [DirectionsResponse] which
     * is required for navigator).
     *
     * The method checks for potential inconsistency in [DirectionsRoute] and [RouteOptions], where
     * navigation needs [DirectionsResponse] as well and throws an exception if any is found.
     *
     * @throws IllegalStateException if [DirectionsRoute] and [RouteOptions] don't contain enough
     * data to assemble [DirectionsResponse] based on them. See [QueriesProvider.exclusiveQueries].
     */
    internal fun checkDirectionsRoute(route: DirectionsRoute) {
        val url = route.routeOptions()?.toUrl("")?.toHttpUrlOrNull() ?: return
        val crossingQueries = mutableListOf<String>()
        url.queryParameterNames.forEach { urlQueryKey ->
            QueriesProvider.exclusiveQueries[urlQueryKey]?.let { exclusiveQueryValue ->
                val urlQueryValue = url.queryParameterValues(urlQueryKey).firstOrNull()
                if (urlQueryValue == exclusiveQueryValue) {
                    crossingQueries.add("$urlQueryKey=$urlQueryValue")
                }
            }
        }
        if (crossingQueries.isNotEmpty()) {
            throw IllegalStateException(provideErrorMessage(crossingQueries))
        }
    }

    private fun provideErrorMessage(crossingQueries: List<String>): String =
        """$ERROR_MESSAGE_TEMPLATE ${
        crossingQueries.joinToString(
            separator = ";",
            prefix = "[",
            postfix = "]",
            transform = { str -> "($str)" },
        )
        }
        """.trimIndent()
}
