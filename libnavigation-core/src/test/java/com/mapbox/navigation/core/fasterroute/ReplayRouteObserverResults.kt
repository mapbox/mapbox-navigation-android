package com.mapbox.navigation.core.fasterroute

import com.google.gson.GsonBuilder
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.internal.fasterroute.NavigationRouteTypeAdapter
import com.mapbox.navigation.core.routealternatives.AlternativeRouteMetadata
import org.apache.commons.io.IOUtils
import java.net.URL

/**
 * Replays route updates recorded with RecordRouteObserverResults
 */
fun Any.readRouteObserverResults(packageName: String): List<RecordedRoutesUpdateResult> {
    val clazz = this.javaClass
    val content = IOUtils.toString(clazz.classLoader.getResource(packageName), "UTF-8")
    if (content.isNullOrBlank()) {
        error("no files found in package $packageName")
    }
    return content.split("\n")
        .filterNot { it.isBlank() }
        .map {
            val parsedName = it.substring(0, it.length - 4).split("-")
            val fileContent = IOUtils.toString(
                clazz.classLoader.getResource("$packageName/$it"),
                "UTF-8"
            )
            Triple(parsedName[0].toInt(), parsedName[1], fileContent)
        }
        .sortedBy { it.first }
        .map {
            val (alternativeIds, routes) = createNavigationRoutes(it.third)
            RecordedRoutesUpdateResult(
                RoutesUpdatedResult(
                    routes,
                    it.second
                ),
                alternativeIds
            )
        }
}

private fun createNavigationRoutes(
    fileContent: String
): Pair<Map<String, AlternativeRouteMetadata>, List<NavigationRoute>> {
    val result = mutableListOf<NavigationRoute>()
    val alternativeIds = mutableMapOf<String, AlternativeRouteMetadata>()
    val lines = fileContent.split("\n")
    for (i in lines.indices step 4) {
        val routeIndex = lines[i].toInt()
        val routeOptions = RouteOptions.fromUrl(URL(lines[i + 1]))
        val directionsResponse = DirectionsResponse.fromJson(lines[i + 2])
        val routes = com.mapbox.navigation.testing.factories.createNavigationRoutes(
            directionsResponse,
            routeOptions,
            RouterOrigin.Offboard
        )
        val alternativeMetadata = lines[i + 3]

        val route = routes[routeIndex]
        result.add(route)
        if (alternativeMetadata.isNotBlank()) {
            alternativeIds[route.id] = fromJson(alternativeMetadata, route)
        }
    }
    return Pair(alternativeIds, result)
}

private fun fromJson(json: String, navigationRoute: NavigationRoute): AlternativeRouteMetadata {
    val gson = GsonBuilder()
        .registerTypeAdapter(
            NavigationRoute::class.java,
            NavigationRouteTypeAdapter {
                if (it == navigationRoute.id) {
                    navigationRoute
                } else {
                    error("$it doesn't match route id")
                }
            }
        )
        .create()
    return gson.fromJson(json, AlternativeRouteMetadata::class.java)
}

data class RecordedRoutesUpdateResult(
    val update: RoutesUpdatedResult,
    val alternativeMetadata: Map<String, AlternativeRouteMetadata>
)
