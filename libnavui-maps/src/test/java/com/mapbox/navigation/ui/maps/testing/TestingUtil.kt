package com.mapbox.navigation.ui.maps.testing

import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.DirectionsWaypoint
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.navigation.base.internal.route.createNavigationRoutes
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.testing.FileUtils
import com.mapbox.navigation.testing.factories.createNavigationRoute
import com.mapbox.navigation.testing.factories.createWaypoint
import io.mockk.every
import io.mockk.mockk

object TestingUtil {

    fun loadNavigationRoute(
        routeFileName: String,
        uuid: String? = null,
        waypoints: List<DirectionsWaypoint> = listOf(createWaypoint(), createWaypoint()),
    ): NavigationRoute {
        val routeAsJson = FileUtils.loadJsonFixture(routeFileName)
        return createNavigationRoute(
            directionsRoute = DirectionsRoute.fromJson(
                routeAsJson,
            ).toBuilder()
                .requestUuid(uuid)
                .build(),
            responseWaypoints = waypoints,
        )
    }
}

internal data class TestRoute(
    val fileName: String,
    val uuid: String? = null,
) {
    val navigationRoute = TestingUtil.loadNavigationRoute(fileName, uuid = uuid)
    private val points = navigationRoute.getPoints()

    fun mockRouteProgress(
        stepIndexValue: Int = 0,
        legIndexValue: Int = 0,
    ): RouteProgress {
        // selects the first segment/point of the requested step
        var currentSegmentIndexInRoute = 0
        for (i in 0 until legIndexValue) {
            currentSegmentIndexInRoute += points[i].sumOf { it.size - 1 }
        }
        currentSegmentIndexInRoute += points[legIndexValue].take(stepIndexValue)
            .sumOf { it.size - 1 }
        val route = navigationRoute
        val legStep = route.directionsRoute.legs()!![legIndexValue].steps()!![stepIndexValue]
        return mockk {
            every { currentLegProgress } returns mockk {
                every { legIndex } returns legIndexValue
                every { currentStepProgress } returns mockk {
                    every { stepPoints } returns PolylineUtils.decode(
                        legStep.geometry()!!,
                        6,
                    )
                    every { distanceTraveled } returns 0f
                    every { step } returns mockk {
                        every { distance() } returns
                            legStep.distance()
                    }
                    every { stepIndex } returns stepIndexValue
                }
            }
            every { currentState } returns RouteProgressState.TRACKING
            every { navigationRoute } returns route
            every { currentRouteGeometryIndex } returns currentSegmentIndexInRoute
        }
    }
}

internal data class TestResponse(
    val fileName: String,
    val routeOptions: RouteOptions,
) {
    val navigationRoutes: List<NavigationRoute> by lazy {
        val responseAsJson = FileUtils.loadJsonFixture(fileName)
        createNavigationRoutes(
            DirectionsResponse.fromJson(responseAsJson).toJson(),
            routeOptions.toUrl("***").toString(),
            RouterOrigin.ONLINE,
        )
    }
}

internal fun NavigationRoute.getPoints(): List<List<List<Point>>> =
    directionsRoute.legs()?.map { leg ->
        leg.steps()?.map { step ->
            PolylineUtils.decode(step.geometry()!!, 6)
        }.orEmpty()
    }.orEmpty()
