package com.mapbox.navigation.core.internal.utils

import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import org.apache.commons.io.IOUtils
import java.net.URL

fun Any.readRouteObserverResults(packageName: String): List<RoutesUpdatedResult> {
    val clazz = this.javaClass
    val content = IOUtils.toString(clazz.classLoader.getResource(packageName), "UTF-8")
    if (content.isNullOrBlank()) {
        error("no files found in package $packageName")
    }
    return content.split("\n")
        .filterNot { it.isBlank() }
        .map {
            val parsedName = it.substring(0, it.length - 4).split("-")
            val fileContent = IOUtils.toString(clazz.classLoader.getResource("$packageName/$it"),"UTF-8")
            Triple(parsedName[0].toInt(), parsedName[1], fileContent)
        }
        .sortedBy { it.first }
        .map {
            RoutesUpdatedResult(
                createNavigationRoutes(it.third),
                it.second
            )
        }
}

fun createNavigationRoutes(fileContent: String): List<NavigationRoute> {
    val result = mutableListOf<NavigationRoute>()
    val lines = fileContent.split("\n")
    for (i in lines.indices step 3) {
        val routeIndex = lines[i].toInt()
        val routeOptions = RouteOptions.fromUrl(URL(lines[i+1]))
        val directionsResponse = DirectionsResponse.fromJson(lines[i+2])
        val routes = com.mapbox.navigation.testing.factories.createNavigationRoutes(
            directionsResponse,
            routeOptions,
            RouterOrigin.Offboard
        )
        result.add(routes[routeIndex])
    }
    return result
}

//private fun resourceAsString(
//    name: String,
//    packageName: String = "com.mapbox.navigation.core.internal.utils.similarroutes"
//): String {
//    val inputStream = javaClass.classLoader?.getResourceAsStream("$packageName/$name")
//    return IOUtils.toString(inputStream, "UTF-8")
//}