package com.mapbox.services.android.navigation.v5.route

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.location.Location
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.route.offboard.NavigationRoute
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import com.mapbox.services.android.navigation.v5.utils.RouteUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.ArrayList
import java.util.Locale
import junit.framework.TestCase.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RouteFetcherTest {

    @Test
    fun cancelRouteCall_cancelsWithNonNullNavigationRoute() {
        val context = mockk<Context>(relaxed = true)
        val navigationRoute = mockk<com.mapbox.navigation.route.offboard.NavigationRoute>(relaxed = true)
        val routeUtils = mockk<RouteUtils>(relaxed = true)
        val routeFetcher = RouteFetcher(context, "pk.xx", routeUtils, navigationRoute)

        routeFetcher.cancelRouteCall()

        verify { navigationRoute.cancelCall() }
    }

    @Test
    fun buildRequestFrom_returnsValidBuilder() {
        val context = buildMockContext()
        val location = buildMockLocation()
        val remainingCoordinates = buildCoordinateList()
        val routeProgress = buildMockProgress(remainingCoordinates)
        val routeUtils = mockk<RouteUtils>(relaxed = true)
        every { routeUtils.calculateRemainingWaypoints(routeProgress) } returns remainingCoordinates
        val routeFetcher = RouteFetcher(context, "pk.xx")

        val builder = routeFetcher.buildRequestFrom(location, routeProgress)

        assertNotNull(builder)
    }

    @Test
    fun findRouteWith_callNavigationRoute() {
        val context = mockk<Context>(relaxed = true)
        val navigationRoute = mockk<com.mapbox.navigation.route.offboard.NavigationRoute>(relaxed = true)
        val builder = mockk<com.mapbox.navigation.route.offboard.NavigationRoute.Builder>(relaxed = true)
        every { builder.build() } returns navigationRoute
        val routeUtils = mockk<RouteUtils>(relaxed = true)
        val routeFetcher = RouteFetcher(context, "pk.xx", routeUtils, navigationRoute)

        routeFetcher.findRouteWith(builder)

        verify { navigationRoute.getRoute(any()) }
    }

    private fun buildMockContext(): Context {
        val context = mockk<Context>()
        val resources = mockk<Resources>()
        val configuration = Configuration()
        configuration.setLocale(Locale.US)
        every { resources.configuration } returns configuration
        every { context.resources } returns resources
        return context
    }

    private fun buildMockLocation(): Location {
        val location = mockk<Location>(relaxed = true)
        every { location.longitude } returns 1.23
        every { location.latitude } returns 2.34
        return location
    }

    private fun buildMockProgress(remainingCoordinates: List<Point>): RouteProgress {
        val route = mockk<DirectionsRoute>(relaxed = true)
        val routeOptions = mockk<RouteOptions>(relaxed = true)
        every { routeOptions.coordinates() } returns remainingCoordinates
        every { routeOptions.waypointIndices() } returns "0;2;3"
        every { route.routeOptions() } returns routeOptions
        val routeProgress = mockk<RouteProgress>(relaxed = true)
        every { routeProgress.remainingWaypoints() } returns 2
        every { routeProgress.directionsRoute() } returns route
        return routeProgress
    }

    private fun buildCoordinateList(): List<Point> {
        val coordinates = ArrayList<Point>()
        coordinates.add(Point.fromLngLat(1.234, 5.678))
        coordinates.add(Point.fromLngLat(9.012, 3.456))
        coordinates.add(Point.fromLngLat(7.890, 1.234))
        coordinates.add(Point.fromLngLat(5.678, 9.012))
        return coordinates
    }
}
