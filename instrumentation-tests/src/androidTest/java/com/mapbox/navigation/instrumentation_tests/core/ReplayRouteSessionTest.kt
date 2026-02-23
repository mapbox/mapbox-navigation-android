package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.RouteRefreshOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.replay.route.ReplayRouteSession
import com.mapbox.navigation.core.replay.route.ReplayRouteSessionOptions
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import com.mapbox.navigation.testing.ui.utils.MapboxNavigationRule
import com.mapbox.navigation.testing.ui.utils.coroutines.routeProgressUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.routesUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.ui.utils.coroutines.setNavigationRoutesAndWaitForUpdate
import com.mapbox.navigation.testing.ui.utils.runOnMainSync
import com.mapbox.navigation.testing.utils.nro.assumeNotNROBecauseOfClientSideUpdate
import com.mapbox.navigation.testing.utils.routes.RoutesProvider
import com.mapbox.navigation.testing.utils.routes.requestMockRoutes
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class ReplayRouteSessionTest : BaseCoreNoCleanUpTest() {

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()
    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var replayRouteSession: ReplayRouteSession

    override fun setupMockLocation(): Location {
        return mockLocationUpdatesRule.generateLocationUpdate {
            this.latitude = 0.0
            this.longitude = 0.0
        }
    }

    @Before
    fun setUp() {
        runOnMainSync {
            replayRouteSession = ReplayRouteSession()
            replayRouteSession.setOptions(
                ReplayRouteSessionOptions.Builder().locationResetEnabled(false).build(),
            )
        }
    }

    @After
    fun tearDown() {
        runOnMainSync {
            if (::mapboxNavigation.isInitialized) {
                replayRouteSession.onDetached(mapboxNavigation)
            }
        }
    }

    @Test
    fun routeIsPlayedIfNoLocationUpdatesHappenedBefore() = sdkTest {
        mapboxNavigation = MapboxNavigationProvider.create(
            NavigationOptions.Builder(context)
                .build(),
        )
        val routes = mapboxNavigation.requestMockRoutes(
            mockWebServerRule,
            RoutesProvider.dc_very_short(context),
        )

        replayRouteSession.onAttached(mapboxNavigation)
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(routes)

        val routeProgressUpdates = mapboxNavigation.routeProgressUpdates().take(5).toList()
        val lastUpdate = routeProgressUpdates.last()
        val firstUpdate = routeProgressUpdates.first()
        assertTrue(
            lastUpdate.distanceTraveled > firstUpdate.distanceTraveled,
        )
    }

    @Test
    fun routeIsPlayedFromCurrentPositionAfterRefresh() = sdkTest {
        // it's hard to emulate server side refresh,
        // ignoring that test for NRO,
        // while java relies on client side update
        assumeNotNROBecauseOfClientSideUpdate()
        mapboxNavigation = MapboxNavigationProvider.create(
            NavigationOptions.Builder(context)
                .routeRefreshOptions(routeRefreshOptions(3000))
                .build(),
        )
        val routes = mapboxNavigation.requestMockRoutes(
            mockWebServerRule,
            RoutesProvider.dc_short_with_alternative_reroute(context),
        )

        replayRouteSession.onAttached(mapboxNavigation)
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(routes)
        mapboxNavigation.routeProgressUpdates()
            .filter { it.currentRouteGeometryIndex >= 4 }
            .first()

        mapboxNavigation.routeProgressUpdates().filter {
            it.currentRouteGeometryIndex == 4
        }.first()

        mapboxNavigation.routesUpdates()
            .filter { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REFRESH }
            .first()

        val routeProgressUpdates = mapboxNavigation.routeProgressUpdates().take(2).toList()
        assertTrue(routeProgressUpdates.all { it.currentRouteGeometryIndex >= 4 })
    }

    private fun routeRefreshOptions(intervalMillis: Long): RouteRefreshOptions {
        val routeRefreshOptions = RouteRefreshOptions.Builder()
            .intervalMillis(TimeUnit.SECONDS.toMillis(30))
            .build()
        RouteRefreshOptions::class.java.getDeclaredField("intervalMillis").apply {
            isAccessible = true
            set(routeRefreshOptions, intervalMillis)
        }
        return routeRefreshOptions
    }
}
