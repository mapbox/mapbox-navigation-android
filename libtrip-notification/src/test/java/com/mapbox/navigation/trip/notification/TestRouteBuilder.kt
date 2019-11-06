package com.mapbox.navigation.trip.notification

import com.google.gson.Gson
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.model.route.Route
import com.mapbox.navigation.base.model.route.RouteOptions
import com.mapbox.navigation.base.model.route.RouteResponse
import com.mapbox.navigation.trip.notification.BaseTest.Companion.ACCESS_TOKEN
import java.io.IOException
import java.util.Scanner
import kotlin.text.Charsets.UTF_8

class TestRouteBuilder {

    val DIRECTIONS_PRECISION_6 = "directions_v5_precision_6.json"

    @Throws(IOException::class)
    fun loadJsonFixture(filename: String): String {
        val classLoader = javaClass.classLoader
        val inputStream = classLoader!!.getResourceAsStream(filename)
        val scanner = Scanner(inputStream, UTF_8.name()).useDelimiter("\\A")
        return if (scanner.hasNext()) scanner.next() else ""
    }

    @Throws(IOException::class)
    fun buildTestDirectionsRoute(fixtureName: String?): Route {
        var fixtureName = fixtureName
        fixtureName = checkNullFixtureName(fixtureName)
        val gson = Gson()
        val body = loadJsonFixture(fixtureName)
        val response = gson.fromJson<RouteResponse>(body, RouteResponse::class.java)
        val route = response.routes()[0]
        return buildRouteWithOptions(route)
    }

    @Throws(IOException::class)
    private fun buildRouteWithOptions(route: Route): Route {
        val coordinates = ArrayList<Point>()
        val routeOptionsWithoutVoiceInstructions = RouteOptions.Builder()
                .baseUrl("https://api.mapbox.com")
                .user("user")
                .profile("profile")
                .accessToken(ACCESS_TOKEN)
                .requestUuid("uuid")
                .geometries("mocked_geometries")
                .voiceInstructions(true)
                .bannerInstructions(true)
                .coordinates(coordinates).build()

        return route.builder()
                .routeOptions(routeOptionsWithoutVoiceInstructions)
                .build()
    }

    private fun checkNullFixtureName(fixtureName: String?): String {
        return fixtureName?.let {
            fixName -> return@let fixName
        } ?: DIRECTIONS_PRECISION_6
    }
}
