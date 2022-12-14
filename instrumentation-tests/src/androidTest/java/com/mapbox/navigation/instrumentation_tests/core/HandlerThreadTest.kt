package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.internal.extensions.flowOnFinalDestinationArrival
import com.mapbox.navigation.instrumentation_tests.activity.EmptyTestActivity
import com.mapbox.navigation.instrumentation_tests.utils.MapboxNavigationRule
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.sdkTest
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.setNavigationRoutesAsync
import com.mapbox.navigation.instrumentation_tests.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.instrumentation_tests.utils.routes.RoutesProvider
import com.mapbox.navigation.instrumentation_tests.utils.routes.RoutesProvider.toNavigationRoutes
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.utils.getMapboxAccessTokenFromResources
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class HandlerThreadTest : BaseTest<EmptyTestActivity>(EmptyTestActivity::class.java) {

    override fun setupMockLocation(): Location = mockLocationUpdatesRule.generateLocationUpdate {
        latitude = 38.894721
        longitude = -77.031991
    }

    @get:Rule
    val mockLocationReplayerRule = MockLocationReplayerRule(mockLocationUpdatesRule)

    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var sdkThread: HandlerThread
    private lateinit var handler: Handler
    private lateinit var sdkDispatcher: CoroutineDispatcher

    @Before
    fun setUp() {
        sdkThread = HandlerThread("test sdk thread")
        sdkThread.start()
        handler = Handler(sdkThread.looper)
        sdkDispatcher = handler.asCoroutineDispatcher()

        mapboxNavigation = MapboxNavigationProvider.create(
            NavigationOptions.Builder(activity)
                .accessToken(getMapboxAccessTokenFromResources(activity))
                .looper(sdkThread.looper)
                .build()
        )
    }

    @After
    fun tearDown() {
        mapboxNavigation.onDestroy()
        sdkThread.quitSafely()
    }

    private fun assertSDKThread() {
        assertEquals(sdkThread.looper, Looper.myLooper())
    }

    @Test
    fun set_navigation_routes_successfully() = sdkTest {
        val routes = RoutesProvider.dc_very_short(activity).toNavigationRoutes()
        mapboxNavigation.setNavigationRoutesAsync(routes)
        assertEquals(routes, mapboxNavigation.getNavigationRoutes())
        mapboxNavigation.startTripSession()
        mockLocationReplayerRule.playRoute(routes.first().directionsRoute)
        var routeProgressEventsCount = 0
        mapboxNavigation.registerRouteProgressObserver {
            assertSDKThread()
            routeProgressEventsCount++
            assertEquals(routes, mapboxNavigation.getNavigationRoutes())
        }

        mapboxNavigation.flowOnFinalDestinationArrival().first()
        assertNotEquals(0, routeProgressEventsCount)
    }
}
