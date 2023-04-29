package com.mapbox.navigation.core.reroute

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Rule
import org.junit.Test

class InternalRerouteControllerAdapterTest {

    @get:Rule
    val noLoggingRule = LoggingFrontendTestRule()

    private val originalController = mockk<NavigationRerouteController>(relaxed = true)
    private val sut = InternalRerouteControllerAdapter(originalController)

    @Test
    fun reroute() {
        val callback = mockk<InternalRerouteController.RoutesCallback>(relaxed = true)
        val slot = slot<NavigationRerouteController.RoutesCallback>()
        val routes = listOf(mockk<NavigationRoute>(), mockk())
        val origin = mockk<RouterOrigin>()

        sut.reroute(callback)

        verify {
            originalController.reroute(capture(slot))
        }
        slot.captured.onNewRoutes(routes, origin)

        verify { callback.onNewRoutes(RerouteResult(routes, 0, origin)) }
    }
}
