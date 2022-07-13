package com.mapbox.navigation.base.internal.utils

import com.mapbox.api.directions.v5.models.DirectionsResponse

/**
 * Queries params, which add data to [DirectionsResponse] and the SDK cannot mock it
 * (See [https://docs.mapbox.com/api/navigation/directions](https://docs.mapbox.com/api/navigation/directions)):
 * - `engine=electric` - electronic vehicle routing. [https://docs.mapbox.com/api/navigation/directions/#electric-vehicle-routing](https://docs.mapbox.com/api/navigation/directions/#electric-vehicle-routing)
 */
internal object QueriesProvider {
    internal val exclusiveQueries: Map<String, String> = mapOf(
        "engine" to "electric",
    )
}
