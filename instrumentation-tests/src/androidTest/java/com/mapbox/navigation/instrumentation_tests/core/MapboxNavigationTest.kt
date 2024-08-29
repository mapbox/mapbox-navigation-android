package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import androidx.test.espresso.Espresso
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.activity.EmptyTestActivity
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.utils.MapboxNavigationRule
import com.mapbox.navigation.testing.ui.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.testing.ui.utils.coroutines.navigateNextRouteLeg
import com.mapbox.navigation.testing.ui.utils.coroutines.requestRoutes
import com.mapbox.navigation.testing.ui.utils.coroutines.resetTripSessionAndWaitForResult
import com.mapbox.navigation.testing.ui.utils.coroutines.routeProgressUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.routesUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.ui.utils.runOnMainSync
import com.mapbox.navigation.testing.utils.http.MockDirectionsRequestHandler
import com.mapbox.navigation.testing.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.testing.utils.readRawFileText
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.net.URI

class MapboxNavigationTest : BaseTest<EmptyTestActivity>(EmptyTestActivity::class.java) {

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()

    @get:Rule
    val mockLocationReplayerRule = MockLocationReplayerRule(mockLocationUpdatesRule)

    private val coordinates = listOf(
        Point.fromLngLat(-77.1576396, 38.7830304),
        Point.fromLngLat(-77.1670888, 38.7756155),
        Point.fromLngLat(-77.1534183, 38.7708948),
    )

    private lateinit var mapboxNavigation: MapboxNavigation

    override fun setupMockLocation(): Location = mockLocationUpdatesRule.generateLocationUpdate {
        latitude = coordinates.first().latitude()
        longitude = coordinates.first().longitude()
    }

    @Before
    fun setup() {
        Espresso.onIdle()

        runOnMainSync {
            mapboxNavigation = MapboxNavigationProvider.create(
                NavigationOptions.Builder(activity)
                    .routingTilesOptions(
                        RoutingTilesOptions.Builder()
                            .tilesBaseUri(URI(mockWebServerRule.baseUrl))
                            .build(),
                    )
                    .build(),
            )
        }
    }

    @Test
    fun trip_session_resets_successfully() = sdkTest {
        mapboxNavigation.resetTripSessionAndWaitForResult()
    }

    @Test
    fun current_leg_index() = sdkTest {
        mockWebServerRule.requestHandlers.clear()
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
                readRawFileText(activity, R.raw.multileg_route),
                coordinates,
            ),
        )

        mapboxNavigation.startTripSession()

        assertEquals(0, mapboxNavigation.currentLegIndex())

        val routes = mapboxNavigation.requestRoutes(
            RouteOptions.builder()
                .baseUrl(mockWebServerRule.baseUrl)
                .applyDefaultNavigationOptions(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
                .coordinatesList(coordinates)
                .build(),
        ).getSuccessfulResultOrThrowException().routes
        mapboxNavigation.setNavigationRoutes(routes)

        mapboxNavigation.routesUpdates().filter { it.navigationRoutes.isNotEmpty() }.first()

        assertEquals(0, mapboxNavigation.currentLegIndex())

        stayOnPosition(coordinates[1])

        mapboxNavigation.routeProgressUpdates()
            .filter { it.currentState == RouteProgressState.TRACKING }
            .first()

        assertEquals(0, mapboxNavigation.currentLegIndex())

        mapboxNavigation.navigateNextRouteLeg()

        mapboxNavigation.routeProgressUpdates()
            .filter { it.currentLegProgress?.legIndex == 1 }
            .first()

        assertEquals(1, mapboxNavigation.currentLegIndex())
    }

    @Test
    fun current_leg_index_with_initial_leg_index_1() = sdkTest {
        mockWebServerRule.requestHandlers.clear()
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
                readRawFileText(activity, R.raw.multileg_route),
                coordinates,
            ),
        )

        mapboxNavigation.startTripSession()

        assertEquals(0, mapboxNavigation.currentLegIndex())

        val routes = mapboxNavigation.requestRoutes(
            RouteOptions.builder()
                .applyDefaultNavigationOptions(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
                .coordinatesList(coordinates)
                .baseUrl(mockWebServerRule.baseUrl)
                .build(),
        ).getSuccessfulResultOrThrowException().routes
        stayOnPosition(coordinates[1])
        mapboxNavigation.setNavigationRoutes(routes, initialLegIndex = 1)

        mapboxNavigation.routesUpdates().filter { it.navigationRoutes.isNotEmpty() }.first()

        assertEquals(1, mapboxNavigation.currentLegIndex())

        mapboxNavigation.routeProgressUpdates()
            .filter { it.currentState == RouteProgressState.TRACKING }
            .first()

        assertEquals(1, mapboxNavigation.currentLegIndex())
    }

    private fun stayOnPosition(position: Point) {
        mockLocationReplayerRule.loopUpdate(
            mockLocationUpdatesRule.generateLocationUpdate {
                latitude = position.latitude()
                longitude = position.longitude()
            },
            times = 120,
        )
    }
}
