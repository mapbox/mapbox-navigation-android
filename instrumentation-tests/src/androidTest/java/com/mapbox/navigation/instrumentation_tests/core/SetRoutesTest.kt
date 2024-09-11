package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.RoutesSetError
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.instrumentation_tests.activity.EmptyTestActivity
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.utils.MapboxNavigationRule
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.ui.utils.coroutines.setNavigationRoutesAsync
import com.mapbox.navigation.testing.ui.utils.runOnMainSync
import com.mapbox.navigation.testing.utils.routes.RoutesProvider
import com.mapbox.navigation.testing.utils.routes.requestMockRoutes
import kotlinx.coroutines.delay
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SetRoutesTest : BaseTest<EmptyTestActivity>(EmptyTestActivity::class.java) {

    private val invalidRouteReason = "Route is invalid for navigation"

    override fun setupMockLocation(): Location = mockLocationUpdatesRule.generateLocationUpdate {
        latitude = 38.894721
        longitude = -77.031991
    }

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()
    private lateinit var mapboxNavigation: MapboxNavigation

    @Before
    fun setUp() {
        runOnMainSync {
            mapboxNavigation = MapboxNavigationProvider.create(
                NavigationOptions.Builder(activity)
                    .build(),
            )
        }
    }

    @Test
    fun set_navigation_routes_successfully() = sdkTest {
        val routes = mapboxNavigation.requestMockRoutes(
            mockWebServerRule,
            RoutesProvider.dc_very_short(activity),
        )
        val result = mapboxNavigation.setNavigationRoutesAsync(routes)
        result.run {
            assertTrue(this.isValue)
            assertEquals(emptyMap<String, RoutesSetError>(), this.value!!.ignoredAlternatives)
        }
    }

    @Test
    fun set_navigation_routes_ignore_alternatives() = sdkTest {
        val routes = mapboxNavigation.requestMockRoutes(
            mockWebServerRule,
            RoutesProvider.dc_short_with_invalid_alternatives(activity),
        )

        val result = mapboxNavigation.setNavigationRoutesAsync(routes)
        result.run {
            assertTrue(this.isValue)
            assertEquals(3, this.value!!.ignoredAlternatives.size)
            assertNotNull(this.value!!.ignoredAlternatives[routes[2].id])
            assertNotNull(this.value!!.ignoredAlternatives[routes[3].id])
            assertNotNull(this.value!!.ignoredAlternatives[routes[4].id])
        }
    }

    @Test
    fun set_navigation_routes_failed() = sdkTest {
        val routes = mapboxNavigation.requestMockRoutes(
            mockWebServerRule,
            RoutesProvider.dc_very_short(activity),
        )
        val result = mapboxNavigation.setNavigationRoutesAsync(routes, 6)
        result.run {
            assertTrue(this.isError)
            assertNotNull(this.error!!.message)
        }
    }

    @Test
    fun set_navigation_routes_empty() = sdkTest {
        val result = mapboxNavigation.setNavigationRoutesAsync(emptyList())
        result.run {
            assertTrue(this.isValue)
            assertEquals(emptyMap<String, RoutesSetError>(), this.value!!.ignoredAlternatives)
        }
    }

    @Test
    fun routes_observer_waits_for_routes_to_finish_processing_when_registered() = sdkTest {
        val routes1 = mapboxNavigation.requestMockRoutes(
            mockWebServerRule,
            RoutesProvider.dc_very_short(activity),
        )
        val routes2 = mapboxNavigation.requestMockRoutes(
            mockWebServerRule,
            RoutesProvider.dc_very_short_two_legs(activity),
        )

        mapboxNavigation.setNavigationRoutesAsync(routes1)
        mapboxNavigation.setNavigationRoutes(routes2)

        suspendCoroutine<Unit> { continuation ->
            mapboxNavigation.registerRoutesObserver {
                assertEquals(routes2, it.navigationRoutes)
                assertEquals(RoutesExtra.ROUTES_UPDATE_REASON_NEW, it.reason)
                continuation.resume(Unit)
            }
        }
    }

    @Test
    fun routes_observer_waits_for_multiple_routes_to_finish_processing_when_registered() = sdkTest {
        val routes1 = mapboxNavigation.requestMockRoutes(
            mockWebServerRule,
            RoutesProvider.dc_very_short(activity),
        )
        val routes2 = mapboxNavigation.requestMockRoutes(
            mockWebServerRule,
            RoutesProvider.dc_very_short_two_legs(activity),
        )

        mapboxNavigation.setNavigationRoutes(routes1)
        mapboxNavigation.setNavigationRoutes(routes2)

        suspendCoroutine<Unit> { continuation ->
            mapboxNavigation.registerRoutesObserver {
                assertEquals(routes2, it.navigationRoutes)
                assertEquals(RoutesExtra.ROUTES_UPDATE_REASON_NEW, it.reason)
                continuation.resume(Unit)
            }
        }
    }

    @Test
    fun routes_observer_isnt_notified_on_registration_when_routes_processing_fails() = sdkTest {
        val routes1 = mapboxNavigation.requestMockRoutes(
            mockWebServerRule,
            RoutesProvider.dc_very_short(activity),
        )
        mapboxNavigation.setNavigationRoutes(routes1, initialLegIndex = 6)

        mapboxNavigation.registerRoutesObserver {
            Assert.fail("observer shouldn't be notified")
        }
        delay(TimeUnit.SECONDS.toMillis(3))
    }

    @Test
    fun routes_observer_receives_only_valid_alternatives() = sdkTest {
        val mockRoute = RoutesProvider.dc_short_with_invalid_alternatives(activity)
        val routes = mapboxNavigation.requestMockRoutes(
            mockWebServerRule,
            mockRoute,
        )

        mapboxNavigation.setNavigationRoutes(routes)

        val actual = suspendCoroutine<RoutesUpdatedResult> { continuation ->
            mapboxNavigation.registerRoutesObserver {
                continuation.resume(it)
            }
        }
        assertEquals(routes.take(2), actual.navigationRoutes)
        assertEquals(routes.drop(2), actual.ignoredRoutes.map { it.navigationRoute })
        assertTrue(actual.ignoredRoutes.all { it.reason == invalidRouteReason })
        assertEquals(RoutesExtra.ROUTES_UPDATE_REASON_NEW, actual.reason)
    }
}
