@file:OptIn(ExperimentalPreviewMapboxNavigationAPI::class)

package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.internal.extensions.flowRouteProgress
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.utils.assumeNotNROBecauseOfAlternativesDropDuringSerialization
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import com.mapbox.navigation.testing.ui.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.testing.ui.utils.coroutines.requestRoutes
import com.mapbox.navigation.testing.ui.utils.coroutines.routeProgressUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.routesPreviewUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.routesUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.ui.utils.coroutines.setNavigationRoutesAsync
import com.mapbox.navigation.testing.utils.DelayedResponseModifier
import com.mapbox.navigation.testing.utils.http.MockDirectionsRequestHandler
import com.mapbox.navigation.testing.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.testing.utils.location.moveAlongTheRouteUntilTracking
import com.mapbox.navigation.testing.utils.location.stayOnPosition
import com.mapbox.navigation.testing.utils.readRawFileText
import com.mapbox.navigation.testing.utils.routes.RoutesProvider
import com.mapbox.navigation.testing.utils.withMapboxNavigation
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

// old devices parse long routes very slowly
private const val EXTENDED_TIMEOUT_FOR_SLOW_PARSING = 200_000L

@OptIn(ExperimentalMapboxNavigationAPI::class)
class LongRoutesSanityTest : BaseCoreNoCleanUpTest() {

    @get:Rule
    val mockLocationReplayerRule = MockLocationReplayerRule(mockLocationUpdatesRule)

    override fun setupMockLocation(): Location {
        return mockLocationUpdatesRule.generateLocationUpdate {
            longitude = 4.898473756907066
            latitude = 52.37373595766587
        }
    }

    @Test
    fun requestAndSetLongRouteWithoutOnboardTiles() = sdkTest(timeout = 120_000) {
        val routeOptions = longRouteOptions()
        val handler = MockDirectionsRequestHandler(
            profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
            lazyJsonResponse = { readRawFileText(context, R.raw.long_route_7k) },
            expectedCoordinates = routeOptions.coordinatesList(),
        ).apply {
            // It takes time for Direction API to calculate a long route
            jsonResponseModifier = DelayedResponseModifier(12_000)
        }
        mockWebServerRule.requestHandlers.add(handler)
        withMapboxNavigation { navigation ->
            val routes = navigation
                .requestRoutes(routeOptions)
                .getSuccessfulResultOrThrowException().routes
            navigation.setNavigationRoutesAsync(routes)
        }
    }

    @Test
    fun requestNewRoutesWhileLongRoutesAreSet() = sdkTest(EXTENDED_TIMEOUT_FOR_SLOW_PARSING) {
        assumeNotNROBecauseOfAlternativesDropDuringSerialization()
        val longRoutesOptions = setupLongRoutes()
        val shortRoutesOptions = setupShortRoutes()
        withMapboxNavigation { navigation ->
            navigation.setNavigationRoutesAsync(
                navigation
                    .requestRoutes(longRoutesOptions)
                    .getSuccessfulResultOrThrowException().routes,
                1,
            )
            assertEquals(2, navigation.getNavigationRoutes().size)

            val shortRoutes = navigation.requestRoutes(shortRoutesOptions)
            assertEquals(2, navigation.getNavigationRoutes().size)

            val secondWaypoint = longRoutesOptions.coordinatesList()[1]
            stayOnPosition(
                longitude = secondWaypoint.longitude(),
                latitude = secondWaypoint.latitude(),
                bearing = 190.0f,
            ) {
                navigation.startTripSession()
                navigation.routeProgressUpdates().first()

                val newRoutes = navigation.requestRoutes(longRoutesOptions)
                    .getSuccessfulResultOrThrowException()
                assertEquals(1, navigation.getNavigationRoutes().size)
                assertEquals(1, navigation.currentLegIndex())
            }
        }
    }

    @Test
    fun requestNewRoutesWhileLongRoutesArePreviewed() = sdkTest(EXTENDED_TIMEOUT_FOR_SLOW_PARSING) {
        assumeNotNROBecauseOfAlternativesDropDuringSerialization()
        val longRoutesOptions = setupLongRoutes()
        val shortRoutesOptions = setupShortRoutes()
        withMapboxNavigation { navigation ->
            navigation.setRoutesPreview(
                navigation
                    .requestRoutes(longRoutesOptions)
                    .getSuccessfulResultOrThrowException().routes,
            )

            assertEquals(
                2,
                navigation.routesPreviewUpdates()
                    .map { it.routesPreview }
                    .filterNotNull()
                    .first()
                    .routesList.size,
            )

            val shortRoutes = navigation.requestRoutes(shortRoutesOptions)
            assertEquals(2, navigation.getRoutesPreview()?.routesList?.size)

            val newLongRoutes = navigation.requestRoutes(longRoutesOptions)
                .getSuccessfulResultOrThrowException()
            assertEquals(1, navigation.getRoutesPreview()?.routesList?.size)
        }
    }

    @Test
    fun rerouteOnLongRoute() = sdkTest(EXTENDED_TIMEOUT_FOR_SLOW_PARSING) {
        assumeNotNROBecauseOfAlternativesDropDuringSerialization()
        val longRoutesOptions = setupLongRoutes()
        val longRoutesRerouteOptions = setupLongRoutesReroute()
        withMapboxNavigation { navigation ->
            val routes = navigation
                .requestRoutes(longRoutesOptions)
                .getSuccessfulResultOrThrowException().routes
            navigation.setNavigationRoutesAsync(routes)
            assertEquals(2, navigation.getNavigationRoutes().size)

            navigation.startTripSession()
            navigation.moveAlongTheRouteUntilTracking(
                routes.first(),
                mockLocationReplayerRule,
            )

            val rerouteOrigin = longRoutesRerouteOptions.coordinatesList().first()
            stayOnPosition(
                latitude = rerouteOrigin.latitude(),
                longitude = rerouteOrigin.longitude(),
                bearing = 190.0f,
            ) {
                navigation.flowRouteProgress().first()
                val droppingAlternatives = navigation.routesUpdates().first {
                    it.reason == RoutesExtra.ROUTES_UPDATE_REASON_ALTERNATIVE
                }
                assertEquals(1, droppingAlternatives.navigationRoutes.size)
                val reroute = navigation.routesUpdates().first {
                    it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REROUTE
                }
                assertEquals(2, reroute.navigationRoutes.size)
            }
        }
    }

    private fun setupLongRoutes(): RouteOptions {
        val routeOptions = longRouteOptions()
        val handler = MockDirectionsRequestHandler(
            profile = routeOptions.profile(),
            lazyJsonResponse = { readRawFileText(context, R.raw.long_route_7k) },
            expectedCoordinates = routeOptions.coordinatesList(),
        )
        mockWebServerRule.requestHandlers.add(handler)
        return routeOptions
    }

    private fun longRouteOptions(): RouteOptions {
        val routeOptions = RouteOptions.builder()
            .baseUrl(mockWebServerRule.baseUrl) // comment to use real Directions API
            .applyDefaultNavigationOptions()
            .coordinates(
                "4.898473756907066,52.37373595766587" +
                    ";5.359980783143584,43.280050656855906" +
                    ";11.571179644010442,48.145540095763664" +
                    ";13.394784408007155,52.51274942160785" +
                    ";-9.143239539655042,38.70880224984026" +
                    ";9.21595128801522,45.4694220491258",
            )
            .alternatives(true)
            .enableRefresh(true)
            .build()
        return routeOptions
    }

    private fun setupLongRoutesReroute(): RouteOptions {
        val routeOptions = longRouteRerouteOptions()
        val handler = MockDirectionsRequestHandler(
            profile = routeOptions.profile(),
            lazyJsonResponse = { readRawFileText(context, R.raw.long_route_7k_reroute) },
            expectedCoordinates = routeOptions.coordinatesList(),
        )
        mockWebServerRule.requestHandlers.add(handler)
        return routeOptions
    }

    private fun longRouteRerouteOptions(): RouteOptions {
        val routeOptions = RouteOptions.builder()
            .baseUrl(mockWebServerRule.baseUrl) // comment to use real Directions API
            .applyDefaultNavigationOptions()
            .coordinates(
                "4.899038,52.373577" +
                    ";5.359980783143584,43.280050656855906" +
                    ";11.571179644010442,48.145540095763664" +
                    ";13.394784408007155,52.51274942160785" +
                    ";-9.143239539655042,38.70880224984026" +
                    ";9.21595128801522,45.4694220491258",
            )
            .alternatives(true)
            .enableRefresh(true)
            .build()
        return routeOptions
    }

    private fun setupShortRoutes(): RouteOptions {
        val shortRoute = RoutesProvider.dc_very_short(context)
        mockWebServerRule.requestHandlers.addAll(shortRoute.mockRequestHandlers)
        return RouteOptions.builder().applyDefaultNavigationOptions()
            .coordinatesList(shortRoute.routeWaypoints)
            .build()
    }
}
