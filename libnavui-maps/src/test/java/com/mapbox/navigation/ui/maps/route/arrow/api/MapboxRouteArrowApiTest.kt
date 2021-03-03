package com.mapbox.navigation.ui.maps.route.arrow.api

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.base.trip.model.RouteStepProgress
import com.mapbox.navigation.testing.FileUtils
import com.mapbox.navigation.ui.base.model.route.RouteLayerConstants
import io.mockk.every
import io.mockk.mockk
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNull
import junit.framework.Assert.assertTrue
import org.junit.Test

class MapboxRouteArrowApiTest {

    @Test
    fun updateUpcomingManeuverArrow() {
        val expectedShaftFeature = "{\"type\":\"Feature\",\"geometry\":{\"type\":\"LineString\"," +
            "\"coordinates\":[[-122.523117,37.975107],[-122.523131,37.975067]," +
            "[-122.477395,37.859513],[-122.4776511,37.8593345]]}}"
        val expectedHeadFeature = "{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\"," +
            "\"coordinates\":[-122.4776511,37.8593345]},\"properties\":" +
            "{\"mapbox-navigation-arrow-bearing\":228.55380580181654}}"
        val upcomingPoints = listOf(
            Point.fromLngLat(-122.477395, 37.859513),
            Point.fromLngLat(-122.4784726, 37.8587617)
        )
        val route = getRoute()
        val routeProgress = mockk<RouteProgress> {
            every { currentLegProgress } returns mockk {
                every { legIndex } returns 0
                every { currentStepProgress } returns mockk {
                    every { stepPoints } returns PolylineUtils.decode(
                        route.legs()!![0].steps()!![2].geometry()!!,
                        6
                    )
                    every { distanceTraveled } returns 0f
                    every { step } returns mockk {
                        every { distance() } returns route.legs()!![0].steps()!![2].distance()
                    }
                    every { stepIndex } returns 2
                }
            }
            every { currentState } returns RouteProgressState.LOCATION_TRACKING
            every { upcomingStepPoints } returns upcomingPoints
        }

        val result = MapboxRouteArrowApi().updateUpcomingManeuverArrow(routeProgress)

        assertEquals(4, result.getVisibilityChanges().size)
        assertEquals(expectedShaftFeature, result.getArrowShaftFeature()!!.toJson())
        assertEquals(expectedHeadFeature, result.getArrowHeadFeature()!!.toJson())
    }

    @Test
    fun updateUpcomingManeuverArrow_whenUpComingPointsFromRouteProgressAreEmpty() {
        val upcomingPoints = listOf(Point.fromLngLat(-122.477395, 37.859513))
        val routeStepPoints = listOf(
            Point.fromLngLat(-122.477395, 37.859513),
            Point.fromLngLat(-122.4784726, 37.8587617)
        )
        val stepProgress = mockk<RouteStepProgress> {
            every { stepPoints } returns routeStepPoints
        }
        val routeLegProgress = mockk<RouteLegProgress> {
            every { currentStepProgress } returns stepProgress
        }
        val routeProgress = mockk<RouteProgress> {
            every { currentLegProgress } returns routeLegProgress
            every { upcomingStepPoints } returns upcomingPoints
        }

        val result = MapboxRouteArrowApi().updateUpcomingManeuverArrow(routeProgress)

        assertNull(result.getArrowHeadFeature())
        assertNull(result.getArrowShaftFeature())
        assertEquals(4, result.getVisibilityChanges().size)
    }

    @Test
    fun hideManeuverArrow() {
        val result = MapboxRouteArrowApi().hideManeuverArrow()

        assertEquals(4, result.getVisibilityChanges().size)
        assertEquals(
            RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID,
            result.getVisibilityChanges()[0].first
        )
        assertEquals(Visibility.NONE, result.getVisibilityChanges()[0].second)
        assertEquals(
            RouteLayerConstants.ARROW_SHAFT_CASING_LINE_LAYER_ID,
            result.getVisibilityChanges()[1].first
        )
        assertEquals(Visibility.NONE, result.getVisibilityChanges()[1].second)
        assertEquals(
            RouteLayerConstants.ARROW_HEAD_CASING_LAYER_ID,
            result.getVisibilityChanges()[2].first
        )
        assertEquals(Visibility.NONE, result.getVisibilityChanges()[2].second)
        assertEquals(
            RouteLayerConstants.ARROW_HEAD_LAYER_ID,
            result.getVisibilityChanges()[3].first
        )
        assertEquals(Visibility.NONE, result.getVisibilityChanges()[3].second)
    }

    @Test
    fun showManeuverArrow() {
        val result = MapboxRouteArrowApi().showManeuverArrow()

        assertEquals(4, result.getVisibilityChanges().size)
        assertEquals(
            RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID,
            result.getVisibilityChanges()[0].first
        )
        assertEquals(Visibility.VISIBLE, result.getVisibilityChanges()[0].second)
        assertEquals(
            RouteLayerConstants.ARROW_SHAFT_CASING_LINE_LAYER_ID,
            result.getVisibilityChanges()[1].first
        )
        assertEquals(Visibility.VISIBLE, result.getVisibilityChanges()[1].second)
        assertEquals(
            RouteLayerConstants.ARROW_HEAD_CASING_LAYER_ID,
            result.getVisibilityChanges()[2].first
        )
        assertEquals(Visibility.VISIBLE, result.getVisibilityChanges()[2].second)
        assertEquals(
            RouteLayerConstants.ARROW_HEAD_LAYER_ID,
            result.getVisibilityChanges()[3].first
        )
        assertEquals(Visibility.VISIBLE, result.getVisibilityChanges()[3].second)
    }

    @Test
    fun redraw_whenManeuverPointsEmpty() {
        val result = MapboxRouteArrowApi().redraw()

        assertTrue(result.getVisibilityChanges().isEmpty())
        assertNull(result.getArrowHeadFeature())
        assertNull(result.getArrowShaftFeature())
    }

    @Test
    fun redraw_whenManeuverPointsNotEmpty() {
        val expectedShaftFeature = "{\"type\":\"Feature\",\"geometry\":{\"type\":\"LineString\"," +
            "\"coordinates\":[[-122.523117,37.975107],[-122.523131,37.975067]," +
            "[-122.477395,37.859513],[-122.4776511,37.8593345]]}}"
        val expectedHeadFeature = "{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\"," +
            "\"coordinates\":[-122.4776511,37.8593345]},\"properties\":" +
            "{\"mapbox-navigation-arrow-bearing\":228.55380580181654}}"
        val upcomingPoints = listOf(
            Point.fromLngLat(-122.477395, 37.859513),
            Point.fromLngLat(-122.4784726, 37.8587617)
        )
        val route = getRoute()
        val routeProgress = mockk<RouteProgress> {
            every { currentLegProgress } returns mockk {
                every { legIndex } returns 0
                every { currentStepProgress } returns mockk {
                    every { stepPoints } returns PolylineUtils.decode(
                        route.legs()!![0].steps()!![2].geometry()!!,
                        6
                    )
                    every { distanceTraveled } returns 0f
                    every { step } returns mockk {
                        every { distance() } returns route.legs()!![0].steps()!![2].distance()
                    }
                    every { stepIndex } returns 2
                }
            }
            every { currentState } returns RouteProgressState.LOCATION_TRACKING
            every { upcomingStepPoints } returns upcomingPoints
        }
        val api = MapboxRouteArrowApi()
        api.updateUpcomingManeuverArrow(routeProgress)

        val result = api.redraw()

        assertTrue(result.getVisibilityChanges().isEmpty())
        assertEquals(expectedShaftFeature, result.getArrowShaftFeature()!!.toJson())
        assertEquals(expectedHeadFeature, result.getArrowHeadFeature()!!.toJson())
    }

    private fun getRoute(): DirectionsRoute {
        val routeAsJson = FileUtils.loadJsonFixture("short_route.json")
        return DirectionsRoute.fromJson(routeAsJson)
    }
}
