package com.mapbox.navigation.trip.notification

import com.mapbox.navigation.base.model.route.Route
import com.mapbox.navigation.base.model.route.RouteProgress
import java.io.IOException

open class BaseTest {

    companion object {
        @JvmStatic
        val DELTA = 1E-10
        @JvmStatic
        val LARGE_DELTA = 0.1
        @JvmStatic
        val ACCESS_TOKEN = "pk.XXX"
    }

    private val routeBuilder: TestRouteBuilder = TestRouteBuilder()
    private val routeProgressBuilder: TestRouteProgressBuilder = TestRouteProgressBuilder()

    @Throws(IOException::class)
    fun loadJsonFixture(filename: String): String {
        return routeBuilder.loadJsonFixture(filename)
    }

    @Throws(IOException::class)
    fun buildTestDirectionsRoute(): Route {
        return routeBuilder.buildTestDirectionsRoute(null)
    }

    @Throws(IOException::class)
    fun buildTestDirectionsRoute(fixtureName: String?): Route {
        return routeBuilder.buildTestDirectionsRoute(fixtureName)
    }

    @Throws(Exception::class)
    fun buildDefaultTestRouteProgress(): RouteProgress {
        val testRoute = routeBuilder.buildTestDirectionsRoute(null)
        return routeProgressBuilder.buildDefaultTestRouteProgress(testRoute)
    }

    @Throws(Exception::class)
    fun buildTestRouteProgress(
        route: Route,
        stepDistanceRemaining: Double,
        legDistanceRemaining: Double,
        distanceRemaining: Double,
        stepIndex: Int,
        legIndex: Int
    ): RouteProgress {
        return routeProgressBuilder.buildTestRouteProgress(
                route, stepDistanceRemaining, legDistanceRemaining, distanceRemaining, stepIndex, legIndex
        )
    }
}
