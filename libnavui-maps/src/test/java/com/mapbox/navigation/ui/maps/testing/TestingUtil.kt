package com.mapbox.navigation.ui.maps.testing

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.navigation.base.internal.route.toTestNavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.testing.FileUtils
import io.mockk.every
import io.mockk.mockk
import org.json.JSONObject

object TestingUtil {
    fun loadRoute(routeFileName: String, uuid: String? = null): DirectionsRoute {
        val routeAsJson = FileUtils.loadJsonFixture(routeFileName)
        val options = JSONObject(routeAsJson).let {
            if (it.has("routeOptions")) {
                RouteOptions.fromJson(it.getJSONObject("routeOptions").toString())
            } else {
                null
            }
        }
        return DirectionsRoute.fromJson(routeAsJson, options, uuid)
    }

    fun loadNavigationRoute(routeFileName: String, uuid: String? = null) =
        loadRoute(routeFileName, uuid).toTestNavigationRoute(RouterOrigin.Offboard)
}

internal data class TestRoute(
    val fileName: String,
    val uuid: String? = null
) {
    val navigationRoute = TestingUtil.loadNavigationRoute(fileName, uuid = uuid)
    val points = navigationRoute.directionsRoute.legs()?.map { leg ->
        leg.steps()?.map { step ->
            PolylineUtils.decode(step.geometry()!!, 6)
        }.orEmpty()
    }.orEmpty()

    fun mockRouteProgress(
        stepIndexValue: Int = 0,
        legIndexValue: Int = 0
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
                        6
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
