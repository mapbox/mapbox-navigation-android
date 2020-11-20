package com.mapbox.navigation.ui.maps.internal.route.arrow

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteStepProgress
import com.mapbox.navigation.ui.base.internal.route.RouteConstants
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [Build.VERSION_CODES.O_MR1])
@RunWith(RobolectricTestRunner::class)
class RouteArrowUtilsTest {

    lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun obtainArrowPointsFromWhenCurrentArrowLessThan2Points() {
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

        val result = RouteArrowUtils.obtainArrowPointsFrom(routeProgress)

        assertTrue(result.isEmpty())
    }

    @Test
    fun obtainArrowPointsFromWhenUpComingArrowLineLessThan2Points() {
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

        val result = RouteArrowUtils.obtainArrowPointsFrom(routeProgress)

        assertTrue(result.isEmpty())
    }

    @Test
    fun obtainArrowPointsFrom() {
        val upcomingPoints = listOf(
            Point.fromLngLat(-122.477395, 37.859513),
            Point.fromLngLat(-122.4784726, 37.8587617)
        )
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

        val result = RouteArrowUtils.obtainArrowPointsFrom(routeProgress)

        assertEquals(4, result.size)
    }

    @Test
    fun layersAreInitialized() {
        val style = mockk<Style> {
            every { fullyLoaded } returns true
            every { styleSourceExists(RouteConstants.ARROW_SHAFT_SOURCE_ID) } returns true
            every { styleSourceExists(RouteConstants.ARROW_HEAD_SOURCE_ID) } returns true
            every { styleLayerExists(RouteConstants.ARROW_SHAFT_CASING_LINE_LAYER_ID) } returns true
            every { styleLayerExists(RouteConstants.ARROW_HEAD_CASING_LAYER_ID) } returns true
            every { styleLayerExists(RouteConstants.ARROW_SHAFT_LINE_LAYER_ID) } returns true
            every { styleLayerExists(RouteConstants.ARROW_HEAD_LAYER_ID) } returns true
        }

        val result = RouteArrowUtils.layersAreInitialized(style)

        assertTrue(result)
        verify { style.fullyLoaded }
        verify { style.styleSourceExists(RouteConstants.ARROW_SHAFT_SOURCE_ID) }
        verify { style.styleSourceExists(RouteConstants.ARROW_HEAD_SOURCE_ID) }
        verify { style.styleLayerExists(RouteConstants.ARROW_SHAFT_CASING_LINE_LAYER_ID) }
        verify { style.styleLayerExists(RouteConstants.ARROW_HEAD_CASING_LAYER_ID) }
        verify { style.styleLayerExists(RouteConstants.ARROW_SHAFT_LINE_LAYER_ID) }
        verify { style.styleLayerExists(RouteConstants.ARROW_HEAD_LAYER_ID) }
    }

    @Test
    fun initializeLayers_whenStyleNotLoaded() {
        val options = RouteArrowOptions.Builder(ctx).build()
        val style = mockk<Style> {
            every { fullyLoaded } returns false
        }

        RouteArrowUtils.initializeLayers(style, options)

        verify(exactly = 0) { style.styleSourceExists(any()) }
    }

    @Test
    fun initializeLayers_whenLayersAreInitialized() {
        val options = RouteArrowOptions.Builder(ctx).build()
        val style = mockk<Style> {
            every { styleLayers } returns listOf()
            every { fullyLoaded } returns true
            every { styleSourceExists(RouteConstants.ARROW_SHAFT_SOURCE_ID) } returns true
            every { styleSourceExists(RouteConstants.ARROW_HEAD_SOURCE_ID) } returns true
            every { styleLayerExists(RouteConstants.ARROW_SHAFT_CASING_LINE_LAYER_ID) } returns true
            every { styleLayerExists(RouteConstants.ARROW_HEAD_CASING_LAYER_ID) } returns true
            every { styleLayerExists(RouteConstants.ARROW_SHAFT_LINE_LAYER_ID) } returns true
            every { styleLayerExists(RouteConstants.ARROW_HEAD_LAYER_ID) } returns true
        }

        RouteArrowUtils.initializeLayers(style, options)

        verify(exactly = 0) { style.styleLayers }
        verify(exactly = 0) { style.addStyleSource(any(), any()) }
    }
}
