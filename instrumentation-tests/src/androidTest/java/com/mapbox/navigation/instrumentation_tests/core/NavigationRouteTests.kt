package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.internal.route.createNavigationRoutes
import com.mapbox.navigation.base.internal.utils.internalWaypoints
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.activity.EmptyTestActivity
import com.mapbox.navigation.instrumentation_tests.utils.readRawFileText
import com.mapbox.navigation.testing.ui.BaseTest
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.junit.Test

class NavigationRouteTests : BaseTest<EmptyTestActivity>(EmptyTestActivity::class.java) {

    override fun setupMockLocation(): Location = mockLocationUpdatesRule.generateLocationUpdate {
        latitude = 0.0
        longitude = 0.0
    }

    @Test
    fun mapDirectionsResponseToNavigationRoutes() {
        val jsonResponse = readRawFileText(activity, R.raw.route_response_dc_very_short)

        val navRoutes = NavigationRoute.create(
            DirectionsResponse.fromJson(jsonResponse),
            provideRouteOptions(),
            RouterOrigin.Onboard
        )

        assertTrue(navRoutes.isNotEmpty())
        navRoutes.forEach {
            assertTrue(it.internalWaypoints().isNotEmpty())
        }
    }

    @Test
    fun mapDirectionsRouteToNavigationRoutes() {
        val directionsRoute = DirectionsRoute.fromJson(readRawFileText(activity, R.raw.short_route))

        val navRoutes = createNavigationRoutes(
            listOf(directionsRoute),
            provideRouteOptions(),
            RouterOrigin.Onboard,
        )

        assertTrue(navRoutes.isNotEmpty())
        navRoutes.forEach {
            assertTrue(it.internalWaypoints().isNotEmpty())
        }
    }

    @Test
    fun mapMultipleDirectionsRoutesToNavigationRoutes() {
        val directionsRoutes = DirectionsResponse.fromJson(
            readRawFileText(activity, R.raw.route_response_alternative_start)
        )

        val navRoutes = createNavigationRoutes(
            directionsRoutes.routes(),
            directionsRoutes.routes()[0].routeOptions()!!,
            RouterOrigin.Onboard,
        )

        assertTrue(navRoutes.isNotEmpty())
        navRoutes.forEachIndexed { index, navigationRoute ->
            assertEquals(directionsRoutes.routes()[index], navigationRoute.directionsRoute)
        }
        navRoutes.forEach {
            assertTrue(it.internalWaypoints().isNotEmpty())
        }
    }

    private fun provideRouteOptions(): RouteOptions =
        RouteOptions.builder().applyDefaultNavigationOptions()
            .coordinatesList(
                listOf(
                    Point.fromLngLat(1.1, 2.2),
                    Point.fromLngLat(3.3, 4.4),
                )
            )
            .build()
}
