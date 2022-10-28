package com.mapbox.navigation.core.directions

import com.mapbox.navigation.base.internal.RouteRefreshRequestData
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouter
import com.mapbox.navigation.base.route.NavigationRouterRefreshCallback
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class LegacyNavigationRouterAdapterTest {

    private val legacyRouter = mockk<NavigationRouter>()
    private val adapter = LegacyNavigationRouterAdapter(legacyRouter)

    @Test
    fun getRouteRefresh() {
        val route = mockk<NavigationRoute>()
        val callback = mockk<NavigationRouterRefreshCallback>()
        val result = 77L
        every { legacyRouter.getRouteRefresh(route, 5, callback) } returns result
        assertEquals(
            result,
            adapter.getRouteRefresh(
                route,
                RouteRefreshRequestData(5, 6, 7, emptyMap()),
                callback
            )
        )
    }
}
