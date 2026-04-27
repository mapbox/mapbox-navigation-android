@file:OptIn(com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI::class)

package com.mapbox.navigation.core.internal.router

import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.core.mapmatching.MapMatchingOptions

/**
 * Builds a minimal [RouteOptions] from [MapMatchingOptions] for use in [NavigationRouterCallback]
 * (e.g. onFailure, onCanceled) when the request was made with [MapMatchingOptions].
 *
 * Parses the coordinates string (semicolon-separated "lng,lat") into [Point] list.
 */
internal fun MapMatchingOptions.toRouteOptionsForCallback(): RouteOptions {
    val points = coordinates.split(";").mapNotNull { part ->
        val pair = part.trim().split(",")
        if (pair.size >= 2) {
            val lng = pair[0].trim().toDoubleOrNull()
            val lat = pair[1].trim().toDoubleOrNull()
            if (lng != null && lat != null) Point.fromLngLat(lng, lat) else null
        } else {
            null
        }
    }
    return RouteOptions.builder()
        .coordinatesList(points)
        .profile(profile)
        .user(user)
        .baseUrl(baseUrl)
        .build()
}
