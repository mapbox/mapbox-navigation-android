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
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants
import com.mapbox.navigation.ui.maps.route.arrow.model.InvalidPointError
import com.mapbox.navigation.ui.maps.route.arrow.model.ManeuverArrow
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
            Point.fromLngLat(-122.4784726, 37.8587617),
        )
        val route = getRoute()
        val routeProgress = mockk<RouteProgress> {
            every { currentLegProgress } returns mockk {
                every { legIndex } returns 0
                every { currentStepProgress } returns mockk {
                    every { stepPoints } returns PolylineUtils.decode(
                        route.legs()!![0].steps()!![2].geometry()!!,
                        6,
                    )
                    every { distanceTraveled } returns 0f
                    every { step } returns mockk {
                        every { distance() } returns route.legs()!![0].steps()!![2].distance()
                    }
                    every { stepIndex } returns 2
                }
            }
            every { currentState } returns RouteProgressState.TRACKING
            every { upcomingStepPoints } returns upcomingPoints
        }

        val result =
            MapboxRouteArrowApi().addUpcomingManeuverArrow(routeProgress)

        assertEquals(4, result.value!!.layerVisibilityModifications.size)
        assertEquals(expectedShaftFeature, result.value!!.arrowShaftFeature!!.toJson())
        assertEquals(expectedHeadFeature, result.value!!.arrowHeadFeature!!.toJson())
    }

    @Test
    fun updateUpcomingManeuverArrow_whenUpComingPointsFromRouteProgressAreEmpty() {
        val upcomingPoints = listOf(Point.fromLngLat(-122.477395, 37.859513))
        val routeStepPoints = listOf(
            Point.fromLngLat(-122.477395, 37.859513),
            Point.fromLngLat(-122.4784726, 37.8587617),
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

        val result =
            MapboxRouteArrowApi().addUpcomingManeuverArrow(routeProgress)

        assertTrue(result.error is InvalidPointError)
    }

    @Test
    fun hideManeuverArrow() {
        val result = MapboxRouteArrowApi().hideManeuverArrow()

        assertEquals(4, result.layerVisibilityModifications.size)
        assertEquals(
            RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID,
            result.layerVisibilityModifications[0].first,
        )
        assertEquals(Visibility.NONE, result.layerVisibilityModifications[0].second)
        assertEquals(
            RouteLayerConstants.ARROW_SHAFT_CASING_LINE_LAYER_ID,
            result.layerVisibilityModifications[1].first,
        )
        assertEquals(Visibility.NONE, result.layerVisibilityModifications[1].second)
        assertEquals(
            RouteLayerConstants.ARROW_HEAD_CASING_LAYER_ID,
            result.layerVisibilityModifications[2].first,
        )
        assertEquals(Visibility.NONE, result.layerVisibilityModifications[2].second)
        assertEquals(
            RouteLayerConstants.ARROW_HEAD_LAYER_ID,
            result.layerVisibilityModifications[3].first,
        )
        assertEquals(Visibility.NONE, result.layerVisibilityModifications[3].second)
    }

    @Test
    fun showManeuverArrow() {
        val result = MapboxRouteArrowApi().showManeuverArrow()

        assertEquals(4, result.layerVisibilityModifications.size)
        assertEquals(
            RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID,
            result.layerVisibilityModifications[0].first,
        )
        assertEquals(Visibility.VISIBLE, result.layerVisibilityModifications[0].second)
        assertEquals(
            RouteLayerConstants.ARROW_SHAFT_CASING_LINE_LAYER_ID,
            result.layerVisibilityModifications[1].first,
        )
        assertEquals(Visibility.VISIBLE, result.layerVisibilityModifications[1].second)
        assertEquals(
            RouteLayerConstants.ARROW_HEAD_CASING_LAYER_ID,
            result.layerVisibilityModifications[2].first,
        )
        assertEquals(Visibility.VISIBLE, result.layerVisibilityModifications[2].second)
        assertEquals(
            RouteLayerConstants.ARROW_HEAD_LAYER_ID,
            result.layerVisibilityModifications[3].first,
        )
        assertEquals(Visibility.VISIBLE, result.layerVisibilityModifications[3].second)
    }

    @Test
    fun addArrow_doesNotPreventAddDuplicates() {
        val firstPoints = listOf(
            Point.fromLngLat(-122.528540, 37.971168),
            Point.fromLngLat(-122.528637, 37.970187),
            Point.fromLngLat(-122.528076, 37.969760),
        )
        val firstManeuverArrow = ManeuverArrow(firstPoints)

        val secondPoints = listOf(
            Point.fromLngLat(-122.528076, 37.969760),
            Point.fromLngLat(-122.527418, 37.969325),
            Point.fromLngLat(-122.526409, 37.968767),
        )
        val secondManeuverArrow = ManeuverArrow(secondPoints)

        val arrows = MapboxRouteArrowApi().also {
            it.addArrow(firstManeuverArrow)
        }
        arrows.addArrow(secondManeuverArrow)

        val redrawState = arrows.redraw()

        assertEquals(2, arrows.getArrows().size)
        assertEquals(2, redrawState.arrowHeadFeatureCollection.features()!!.size)
        assertEquals(2, redrawState.arrowShaftFeatureCollection.features()!!.size)
    }

    @Test
    fun addArrow() {
        val firstPoints = listOf(
            Point.fromLngLat(-122.528540, 37.971168),
            Point.fromLngLat(-122.528637, 37.970187),
            Point.fromLngLat(-122.528076, 37.969760),
        )
        val firstManeuverArrow = ManeuverArrow(firstPoints)

        val state = MapboxRouteArrowApi().addArrow(firstManeuverArrow)

        assertEquals(1, state.value!!.arrowHeadFeatureCollection.features()!!.size)
        assertEquals(1, state.value!!.arrowShaftFeatureCollection.features()!!.size)
    }

    @Test
    fun removeArrow() {
        val firstPoints = listOf(
            Point.fromLngLat(-122.528540, 37.971168),
            Point.fromLngLat(-122.528637, 37.970187),
            Point.fromLngLat(-122.528076, 37.969760),
        )
        val firstManeuverArrow = ManeuverArrow(firstPoints)
        val secondPoints = listOf(
            Point.fromLngLat(-122.527418, 37.969325),
            Point.fromLngLat(-122.526409, 37.968767),
        )
        val secondManeuverArrow = ManeuverArrow(secondPoints)

        val arrows = MapboxRouteArrowApi().also {
            it.addArrow(firstManeuverArrow)
            it.addArrow(secondManeuverArrow)
        }
        assertEquals(2, arrows.getArrows().size)

        val state = arrows.removeArrow(secondManeuverArrow)

        assertEquals(1, arrows.getArrows().size)
        assertEquals(1, state.arrowHeadFeatureCollection.features()!!.size)
        assertEquals(1, state.arrowShaftFeatureCollection.features()!!.size)
    }

    @Test
    fun clearArrows() {
        val firstPoints = listOf(
            Point.fromLngLat(-122.528540, 37.971168),
            Point.fromLngLat(-122.528637, 37.970187),
            Point.fromLngLat(-122.528076, 37.969760),
        )
        val firstManeuverArrow = ManeuverArrow(firstPoints)
        val secondPoints = listOf(
            Point.fromLngLat(-122.527418, 37.969325),
            Point.fromLngLat(-122.526409, 37.968767),
        )
        val secondManeuverArrow = ManeuverArrow(secondPoints)
        val arrows = MapboxRouteArrowApi().also {
            it.addArrow(firstManeuverArrow)
            it.addArrow(secondManeuverArrow)
        }
        assertEquals(2, arrows.getArrows().size)

        val state = arrows.clearArrows()

        assertEquals(0, arrows.getArrows().size)
        assertEquals(0, state.arrowHeadFeatureCollection.features()!!.size)
        assertEquals(0, state.arrowShaftFeatureCollection.features()!!.size)
    }

    @Test
    fun getArrows() {
        val points = listOf(
            Point.fromLngLat(-122.528540, 37.971168),
            Point.fromLngLat(-122.528637, 37.970187),
            Point.fromLngLat(-122.528076, 37.969760),
        )
        val firstManeuverArrow = ManeuverArrow(points)

        val arrows = MapboxRouteArrowApi().also {
            it.addArrow(firstManeuverArrow)
        }

        assertEquals(1, arrows.getArrows().size)
    }

    @Test
    fun getArrows_includesManeuverArrow() {
        val points = listOf(
            Point.fromLngLat(-122.528540, 37.971168),
            Point.fromLngLat(-122.528637, 37.970187),
            Point.fromLngLat(-122.528076, 37.969760),
        )
        val firstManeuverArrow = ManeuverArrow(points)
        val route = getDirectionsRoute()
        val routeProgress = mockk<RouteProgress> {
            every { currentLegProgress } returns mockk {
                every { legIndex } returns 0
                every { currentStepProgress } returns mockk {
                    every { stepPoints } returns PolylineUtils.decode(
                        route.legs()!![0].steps()!![2].geometry()!!,
                        6,
                    )
                    every { distanceTraveled } returns 0f
                    every { step } returns mockk {
                        every { distance() } returns route.legs()!![0].steps()!![2].distance()
                    }
                    every { stepIndex } returns 2
                }
            }
            every { upcomingStepPoints } returns PolylineUtils.decode(
                route.legs()!![0].steps()!![2].geometry()!!,
                6,
            )
        }

        val arrows = MapboxRouteArrowApi().also {
            it.addArrow(firstManeuverArrow)
            it.addUpcomingManeuverArrow(routeProgress)
        }

        assertEquals(2, arrows.getArrows().size)
    }

    @Test
    fun redraw() {
        val firstPoints = listOf(
            Point.fromLngLat(-122.528540, 37.971168),
            Point.fromLngLat(-122.528637, 37.970187),
            Point.fromLngLat(-122.528076, 37.969760),
        )
        val firstManeuverArrow = ManeuverArrow(firstPoints)
        val secondPoints = listOf(
            Point.fromLngLat(-122.527418, 37.969325),
            Point.fromLngLat(-122.526409, 37.968767),
        )
        val secondManeuverArrow = ManeuverArrow(secondPoints)
        val arrows = MapboxRouteArrowApi().also {
            it.addArrow(firstManeuverArrow)
            it.addArrow(secondManeuverArrow)
        }

        val state = arrows.redraw()

        assertEquals(2, state.arrowHeadFeatureCollection.features()!!.size)
        assertEquals(2, state.arrowShaftFeatureCollection.features()!!.size)
    }

    private fun getDirectionsRoute(): DirectionsRoute {
        val tokenHere = "someToken"
        val directionsRouteAsJson = FileUtils.loadJsonFixture("vanish_point_test.txt")
            .replace("tokenHere", tokenHere)

        return DirectionsRoute.fromJson(directionsRouteAsJson)
    }

    @Test
    fun redraw_whenManeuverPointsEmpty() {
        val result = MapboxRouteArrowApi().redraw()

        assertTrue(result.arrowHeadFeatureCollection.features().isNullOrEmpty())
        assertTrue(result.arrowShaftFeatureCollection.features().isNullOrEmpty())
    }

    @Test
    fun redraw_whenManeuverPointsNotEmpty() {
        val expectedShaftFeature = "{\"type\":\"FeatureCollection\",\"features\":" +
            "[{\"type\":\"Feature\",\"geometry\":{\"type\":\"LineString\",\"coordinates\":" +
            "[[-122.523117,37.975107],[-122.523131,37.975067],[-122.477395,37.859513]," +
            "[-122.4776511,37.8593345]]},\"properties\":{}}]}"
        val expectedHeadFeature = "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":" +
            "\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":" +
            "[-122.4776511,37.8593345]},\"properties\":{\"mapbox-navigation-arrow-bearing\":" +
            "228.55380580181654}}]}"
        val upcomingPoints = listOf(
            Point.fromLngLat(-122.477395, 37.859513),
            Point.fromLngLat(-122.4784726, 37.8587617),
        )
        val route = getRoute()
        val routeProgress = mockk<RouteProgress> {
            every { currentLegProgress } returns mockk {
                every { legIndex } returns 0
                every { currentStepProgress } returns mockk {
                    every { stepPoints } returns PolylineUtils.decode(
                        route.legs()!![0].steps()!![2].geometry()!!,
                        6,
                    )
                    every { distanceTraveled } returns 0f
                    every { step } returns mockk {
                        every { distance() } returns route.legs()!![0].steps()!![2].distance()
                    }
                    every { stepIndex } returns 2
                }
            }
            every { currentState } returns RouteProgressState.TRACKING
            every { upcomingStepPoints } returns upcomingPoints
        }
        val api = MapboxRouteArrowApi()
        api.addUpcomingManeuverArrow(routeProgress)

        val result = api.redraw()

        assertEquals(expectedShaftFeature, result.arrowShaftFeatureCollection.toJson())
        assertEquals(expectedHeadFeature, result.arrowHeadFeatureCollection.toJson())
    }

    private fun getRoute(): DirectionsRoute {
        val routeAsJson = FileUtils.loadJsonFixture("short_route.json")
        return DirectionsRoute.fromJson(routeAsJson)
    }
}
