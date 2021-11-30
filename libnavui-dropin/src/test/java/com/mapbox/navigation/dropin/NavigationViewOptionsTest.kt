package com.mapbox.navigation.dropin

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.base.formatter.DistanceFormatter
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.ui.maps.NavigationStyles
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.speedlimit.model.SpeedLimitFormatter
import com.mapbox.navigation.ui.tripprogress.model.TripProgressUpdateFormatter
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.IllegalArgumentException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [27])
class NavigationViewOptionsTest {

    lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun mapboxRouteLineOptions() {
        val options = mockk<MapboxRouteLineOptions>()

        val result = NavigationViewOptions.Builder(ctx).mapboxRouteLineOptions(options).build()

        assertEquals(options, result.mapboxRouteLineOptions)
    }

    @Test
    fun routeArrowOptions() {
        val options = mockk<RouteArrowOptions>()

        val result = NavigationViewOptions.Builder(ctx).routeArrowOptions(options).build()

        assertEquals(options, result.routeArrowOptions)
    }

    @Test
    fun distanceFormatterOptions() {
        val options = mockk<DistanceFormatterOptions>()

        val result = NavigationViewOptions.Builder(ctx).distanceFormatterOptions(options).build()

        assertEquals(options, result.distanceFormatterOptions)
    }

    @Test
    fun tripProgressUpdateFormatter() {
        val formatter = mockk<TripProgressUpdateFormatter>()

        val result =
            NavigationViewOptions.Builder(ctx).tripProgressUpdateFormatter(formatter).build()

        assertEquals(formatter, result.tripProgressUpdateFormatter)
    }

    @Test
    fun distanceFormatter() {
        val formatter = mockk<DistanceFormatter>()

        val result = NavigationViewOptions.Builder(ctx).distanceFormatter(formatter).build()

        assertEquals(formatter, result.distanceFormatter)
    }

    @Test
    fun speedLimitFormatter() {
        val formatter = mockk<SpeedLimitFormatter>()

        val result = NavigationViewOptions.Builder(ctx).speedLimitFormatter(formatter).build()

        assertEquals(formatter, result.speedLimitFormatter)
    }

    @Test
    fun mapStyleUrlDarkTheme() {
        val result = NavigationViewOptions.Builder(ctx).mapStyleUrlDarkTheme("theme").build()

        assertEquals("theme", result.mapStyleUrlDarkTheme)
    }

    @Test
    fun mapStyleUrlDarkTheme_default() {
        val result = NavigationViewOptions.Builder(ctx).build()

        assertEquals(NavigationStyles.NAVIGATION_NIGHT_STYLE, result.mapStyleUrlDarkTheme)
    }

    @Test(expected = IllegalArgumentException::class)
    fun mapStyleUrlDarkTheme_emptyInput() {
        NavigationViewOptions.Builder(ctx).mapStyleUrlDarkTheme("").build()
    }

    @Test
    fun mapStyleUrlLightTheme() {
        val result = NavigationViewOptions.Builder(ctx).mapStyleUrlLightTheme("theme").build()

        assertEquals("theme", result.mapStyleUrlLightTheme)
    }

    @Test
    fun mapStyleUrlLightTheme_default() {
        val result = NavigationViewOptions.Builder(ctx).build()

        assertEquals(NavigationStyles.NAVIGATION_DAY_STYLE, result.mapStyleUrlLightTheme)
    }

    @Test(expected = IllegalArgumentException::class)
    fun mapStyleUrlLightTheme_emptyInput() {
        NavigationViewOptions.Builder(ctx).mapStyleUrlLightTheme("").build()
    }

    @Test
    fun darkTheme() {
        val theme = mockk<DropInTheme>()

        val result = NavigationViewOptions.Builder(ctx).darkTheme(theme).build()

        assertEquals(theme, result.darkTheme)
    }

    @Test
    fun darkTheme_default() {
        val result = NavigationViewOptions.Builder(ctx).build()

        assertTrue(result.darkTheme is DropInTheme.DarkTheme)
    }

    @Test
    fun lightTheme() {
        val theme = mockk<DropInTheme>()

        val result = NavigationViewOptions.Builder(ctx).lightTheme(theme).build()

        assertEquals(theme, result.lightTheme)
    }

    @Test
    fun lightTheme_default() {
        val result = NavigationViewOptions.Builder(ctx).build()

        assertTrue(result.lightTheme is DropInTheme.LightTheme)
    }

    @Test
    fun useReplayEngine() {
        val result = NavigationViewOptions.Builder(ctx).useReplayEngine(true).build()

        assertTrue(result.useReplayEngine)
    }

    @Test
    fun useReplayEngine_default() {
        val result = NavigationViewOptions.Builder(ctx).build()

        assertFalse(result.useReplayEngine)
    }

    @Test
    fun defaultRouteLineOptions() {
        val result = NavigationViewOptions.Builder(ctx).build()

        assertEquals("road-label", result.mapboxRouteLineOptions.routeLineBelowLayerId)
    }

    @Test
    fun toBuilder() {
        val mapboxRouteLineOptions = mockk<MapboxRouteLineOptions>()
        val routeArrowOptions = mockk<RouteArrowOptions>()
        val distanceFormatterOptions = mockk<DistanceFormatterOptions>()
        val tripProgressUpdateFormatter = mockk<TripProgressUpdateFormatter>()
        val distanceFormatter = mockk<DistanceFormatter>()
        val speedLimitFormatter = mockk<SpeedLimitFormatter>()
        val darkDropInTheme = mockk<DropInTheme>()
        val lightDropInTheme = mockk<DropInTheme>()

        val defaultOptions = NavigationViewOptions.Builder(ctx).build()
        val updatedOptions = defaultOptions.toBuilder(ctx)
            .mapboxRouteLineOptions(mapboxRouteLineOptions)
            .routeArrowOptions(routeArrowOptions)
            .distanceFormatterOptions(distanceFormatterOptions)
            .tripProgressUpdateFormatter(tripProgressUpdateFormatter)
            .distanceFormatter(distanceFormatter)
            .speedLimitFormatter(speedLimitFormatter)
            .mapStyleUrlDarkTheme("foobarDark")
            .mapStyleUrlLightTheme("foobarLight")
            .darkTheme(darkDropInTheme)
            .lightTheme(lightDropInTheme)
            .useReplayEngine(true)
            .build()

        assertEquals(mapboxRouteLineOptions, updatedOptions.mapboxRouteLineOptions)
        assertEquals(routeArrowOptions, updatedOptions.routeArrowOptions)
        assertEquals(distanceFormatterOptions, updatedOptions.distanceFormatterOptions)
        assertEquals(tripProgressUpdateFormatter, updatedOptions.tripProgressUpdateFormatter)
        assertEquals(distanceFormatter, updatedOptions.distanceFormatter)
        assertEquals(speedLimitFormatter, updatedOptions.speedLimitFormatter)
        assertEquals(darkDropInTheme, updatedOptions.darkTheme)
        assertEquals(lightDropInTheme, updatedOptions.lightTheme)
        assertEquals("foobarDark", updatedOptions.mapStyleUrlDarkTheme)
        assertEquals("foobarLight", updatedOptions.mapStyleUrlLightTheme)
        assertTrue(updatedOptions.useReplayEngine)
    }
}
