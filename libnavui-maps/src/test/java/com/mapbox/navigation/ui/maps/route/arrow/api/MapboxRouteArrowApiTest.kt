package com.mapbox.navigation.ui.maps.route.arrow.api

import com.mapbox.geojson.Point
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteStepProgress
import com.mapbox.navigation.ui.base.internal.route.RouteConstants
import io.mockk.every
import io.mockk.mockk
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNull
import org.junit.Test

class MapboxRouteArrowApiTest {

    @Test
    fun addUpcomingManeuverArrowStateWhenArrowPointsFromRouteProgressAreEmpty() {
        val upcomingPoints = listOf(
            Point.fromLngLat(-122.477395, 37.859513),
            Point.fromLngLat(-122.4784726, 37.8587617)
        )
        val routeStepPoints = listOf(Point.fromLngLat(-122.477395, 37.859513))
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
    fun addUpcomingManeuverArrowStateWhenUpComingPointsFromRouteProgressAreEmpty() {
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
            RouteConstants.ARROW_SHAFT_LINE_LAYER_ID,
            result.getVisibilityChanges()[0].first
        )
        assertEquals(Visibility.NONE, result.getVisibilityChanges()[0].second)
        assertEquals(
            RouteConstants.ARROW_SHAFT_CASING_LINE_LAYER_ID,
            result.getVisibilityChanges()[1].first
        )
        assertEquals(Visibility.NONE, result.getVisibilityChanges()[1].second)
        assertEquals(
            RouteConstants.ARROW_HEAD_CASING_LAYER_ID,
            result.getVisibilityChanges()[2].first
        )
        assertEquals(Visibility.NONE, result.getVisibilityChanges()[2].second)
        assertEquals(RouteConstants.ARROW_HEAD_LAYER_ID, result.getVisibilityChanges()[3].first)
        assertEquals(Visibility.NONE, result.getVisibilityChanges()[3].second)
    }

    @Test
    fun showManeuverArrow() {
        val result = MapboxRouteArrowApi().showManeuverArrow()

        assertEquals(4, result.getVisibilityChanges().size)
        assertEquals(
            RouteConstants.ARROW_SHAFT_LINE_LAYER_ID,
            result.getVisibilityChanges()[0].first
        )
        assertEquals(Visibility.VISIBLE, result.getVisibilityChanges()[0].second)
        assertEquals(
            RouteConstants.ARROW_SHAFT_CASING_LINE_LAYER_ID,
            result.getVisibilityChanges()[1].first
        )
        assertEquals(Visibility.VISIBLE, result.getVisibilityChanges()[1].second)
        assertEquals(
            RouteConstants.ARROW_HEAD_CASING_LAYER_ID,
            result.getVisibilityChanges()[2].first
        )
        assertEquals(Visibility.VISIBLE, result.getVisibilityChanges()[2].second)
        assertEquals(RouteConstants.ARROW_HEAD_LAYER_ID, result.getVisibilityChanges()[3].first)
        assertEquals(Visibility.VISIBLE, result.getVisibilityChanges()[3].second)
    }
}
