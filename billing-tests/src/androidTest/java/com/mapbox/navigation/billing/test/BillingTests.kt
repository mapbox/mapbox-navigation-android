package com.mapbox.navigation.billing.test

import android.location.Location
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import com.mapbox.navigation.testing.ui.http.HttpServiceEvent
import com.mapbox.navigation.testing.ui.http.HttpServiceEventsObserver
import com.mapbox.navigation.testing.ui.http.billingRequests
import com.mapbox.navigation.testing.ui.http.billingRequestsFlow
import com.mapbox.navigation.testing.ui.http.isActiveGuidanceSession
import com.mapbox.navigation.testing.ui.utils.MapboxNavigationRule
import com.mapbox.navigation.testing.ui.utils.coroutines.clearNavigationRoutesAndWaitForUpdate
import com.mapbox.navigation.testing.ui.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.testing.ui.utils.coroutines.requestRoutes
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.ui.utils.coroutines.setNavigationRoutesAndWaitForUpdate
import com.mapbox.navigation.testing.ui.utils.coroutines.startTripSessionAndWaitForActiveGuidanceState
import com.mapbox.navigation.testing.ui.utils.getMapboxAccessTokenFromResources
import com.mapbox.navigation.testing.ui.utils.runOnMainSync
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.net.URI
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit

/**
 * Tests that are scheduled to run on CI. Needed to generate billing events
 * that will later be checked during CI job to verify whether billing events appeared
 * in the billing data base.
 *
 * In this tests we just generate billing events.
 * More test cases and parameters verification can be found in 'TripSessionsBillingTest'.
 */
class BillingTests : BaseCoreNoCleanUpTest() {

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()

    private lateinit var mapboxNavigation: MapboxNavigation

    private lateinit var httpEventsObserver: HttpServiceEventsObserver

    private val source = Point.fromLngLat(-77.031991, 38.894721)
    private val destination = Point.fromLngLat(-77.030923, 38.895433)

    override fun setupMockLocation(): Location = mockLocationUpdatesRule.generateLocationUpdate {
        latitude = source.latitude()
        longitude = source.longitude()
        bearing = 190f
    }

    @Before
    fun setup() {
        httpEventsObserver = HttpServiceEventsObserver.register()

        runOnMainSync {
            assertTrue(httpEventsObserver.billingRequests().isEmpty())

            mapboxNavigation = MapboxNavigationProvider.create(
                NavigationOptions.Builder(context)
                    .accessToken(getMapboxAccessTokenFromResources(context))
                    .routingTilesOptions(
                        RoutingTilesOptions.Builder()
                            .tilesBaseUri(URI(mockWebServerRule.baseUrl))
                            .build()
                    )
                    .build()
            )
        }
    }

    @After
    fun cleanUp() {
        HttpServiceEventsObserver.unregister()
    }

    @Test
    fun test30SecondsLongActiveGuidanceTrip() = sdkTest(TimeUnit.MINUTES.toMillis(1)) {
        val routes = requestRoutes(source, destination)

        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(routes)
        mapboxNavigation.startTripSessionAndWaitForActiveGuidanceState()

        delay(TimeUnit.SECONDS.toMillis(30))

        mapboxNavigation.clearNavigationRoutesAndWaitForUpdate()

        waitForBillingHttpEvents(1) {
            it.isActiveGuidanceSession
        }
    }

    private suspend fun requestRoutes(source: Point, destination: Point): List<NavigationRoute> {
        val routeOptions = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .coordinatesList(listOf(source, destination))
            .alternatives(false)
            .build()

        return mapboxNavigation.requestRoutes(
            routeOptions
        ).getSuccessfulResultOrThrowException().routes
    }

    /**
     * Waiting for http billing events to be sent to the backend.
     */
    private suspend fun waitForBillingHttpEvents(
        count: Int,
        predicate: suspend (HttpServiceEvent) -> Boolean = { true }
    ) {
        try {
            httpEventsObserver
                .billingRequestsFlow()
                .filter(predicate)
                .take(count)
                .toList()
        } catch (e: CancellationException) {
            Assert.fail("Timed out waiting for event(s)")
        }
    }
}
