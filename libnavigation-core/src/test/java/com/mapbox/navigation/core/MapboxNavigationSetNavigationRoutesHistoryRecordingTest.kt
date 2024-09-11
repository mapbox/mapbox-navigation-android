package com.mapbox.navigation.core

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.trip.session.NativeSetRouteError
import com.mapbox.navigation.core.trip.session.NativeSetRouteValue
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@ExperimentalPreviewMapboxNavigationAPI
@Config(shadows = [ShadowReachabilityFactory::class])
@InternalCoroutinesApi
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
internal class MapboxNavigationSetNavigationRoutesHistoryRecordingTest :
    MapboxNavigationBaseTest() {

    private val passedPrimaryRoute = mockk<NavigationRoute>(relaxed = true)
    private val initialLegIndex = 2

    @Test
    fun `calls only setRoutes for successfully set routes`() = coroutineRule.runBlockingTest {
        createMapboxNavigation()
        val routes = listOf(passedPrimaryRoute)
        coEvery {
            tripSession.setRoutes(
                routes,
                SetRoutes.NewRoutes(initialLegIndex),
            )
        } returns NativeSetRouteValue(routes, emptyList())

        mapboxNavigation.setNavigationRoutes(routes, initialLegIndex)

        verify(exactly = 1) { historyRecordingStateHandler.setRoutes(routes) }
        verify(exactly = 0) { historyRecordingStateHandler.lastSetRoutesFailed() }
    }

    @Test
    fun `calls setRoutes and setRoutesFailed for failed set routes`() = coroutineRule.runBlockingTest {
        createMapboxNavigation()
        val routes = listOf(passedPrimaryRoute)
        coEvery {
            tripSession.setRoutes(
                routes,
                SetRoutes.NewRoutes(initialLegIndex),
            )
        } returns NativeSetRouteError("error")

        mapboxNavigation.setNavigationRoutes(routes, initialLegIndex)

        verify(exactly = 1) { historyRecordingStateHandler.setRoutes(routes) }
        verify(exactly = 1) { historyRecordingStateHandler.lastSetRoutesFailed() }
    }

    @Test
    fun `calls only setRoutes for successfully set empty routes`() = coroutineRule.runBlockingTest {
        createMapboxNavigation()
        val routes = emptyList<NavigationRoute>()
        coEvery {
            tripSession.setRoutes(
                routes,
                SetRoutes.NewRoutes(initialLegIndex),
            )
        } returns NativeSetRouteValue(routes, emptyList())

        mapboxNavigation.setNavigationRoutes(routes, initialLegIndex)

        verify(exactly = 1) { historyRecordingStateHandler.setRoutes(routes) }
        verify(exactly = 0) { historyRecordingStateHandler.lastSetRoutesFailed() }
    }
}
