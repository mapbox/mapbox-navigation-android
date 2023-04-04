package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.utils.http.MockDirectionsRequestHandler
import com.mapbox.navigation.instrumentation_tests.utils.readRawFileText
import com.mapbox.navigation.instrumentation_tests.utils.withMapboxNavigation
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import com.mapbox.navigation.testing.ui.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.testing.ui.utils.coroutines.requestRoutes
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.ui.utils.coroutines.setNavigationRoutesAsync
import org.junit.Ignore
import org.junit.Test

class LongRoutesSanityTest : BaseCoreNoCleanUpTest() {

    override fun setupMockLocation(): Location {
        return mockLocationUpdatesRule.generateLocationUpdate { }
    }

    @Test
    @Ignore("Waiting for NN-654 to be fixed")
    fun requestAndSetLongRouteWithoutOnboardTiles() = sdkTest {
        val routeOptions = RouteOptions.builder()
            .baseUrl(mockWebServerRule.baseUrl) // comment to use real Directions API
            .applyDefaultNavigationOptions()
            .coordinates(
                "4.898473756907066,52.37373595766587;5.359980783143584,43.280050656855906" +
                    ";11.571179644010442,48.145540095763664" +
                    ";13.394784408007155,52.51274942160785" +
                    ";-9.143239539655042,38.70880224984026" +
                    ";9.21595128801522,45.4694220491258"
            )
            .alternatives(true)
            .enableRefresh(true)
            .build()
        val handler = MockDirectionsRequestHandler(
            profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
            lazyJsonResponse = { readRawFileText(context, R.raw.long_route_7k) },
            expectedCoordinates = routeOptions.coordinatesList(),
            serverDelayMs = 12_000, // It takes time for Direction API to calculate a long route
        )
        mockWebServerRule.requestHandlers.add(handler)
        withMapboxNavigation { navigation ->
            val routes = navigation
                .requestRoutes(routeOptions)
                .getSuccessfulResultOrThrowException().routes
            navigation.setNavigationRoutesAsync(routes)
        }
    }
}
