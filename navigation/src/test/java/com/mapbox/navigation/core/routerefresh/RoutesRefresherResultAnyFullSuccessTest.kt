package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.internal.RouteProgressData
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutesRefresherResultAnyFullSuccessTest {

    @Test
    fun `charge only success is a success but not a full one`() {
        val result = routeResult(RouteRefresherStatus.Success(mockk(), chargeOnly = true))

        assertTrue(result.isSuccess())
        assertFalse(result.isFullSuccess())
    }

    @Test
    fun `failure is not a full success`() {
        assertFalse(routeResult(RouteRefresherStatus.Failure).isFullSuccess())
    }

    @Test
    fun anyFullSuccess() {
        val full = RouteRefresherStatus.Success(mockk())
        val chargeOnly = RouteRefresherStatus.Success(mockk(), chargeOnly = true)
        val failure = RouteRefresherStatus.Failure

        assertEquals(true, routesResult(full, chargeOnly).anyFullSuccess())
        assertEquals(true, routesResult(chargeOnly, full).anyFullSuccess())
        assertEquals(false, routesResult(chargeOnly, chargeOnly).anyFullSuccess())
        assertEquals(false, routesResult(chargeOnly, failure).anyFullSuccess())
        assertEquals(true, routesResult(chargeOnly, failure).anySuccess())
    }

    private fun routeResult(status: RouteRefresherStatus) =
        RouteRefresherResult(mockk(), mockk<RouteProgressData>(), status)

    @Test
    fun `a device refresh of an offline route doesn't count next to a server-built route`() {
        val full = RouteRefresherStatus.Success(mockk())
        val chargeOnly = RouteRefresherStatus.Success(mockk(), chargeOnly = true)

        // The offline alternative has no server traffic to keep the online primary's fresh.
        assertEquals(
            false,
            routesResult(chargeOnly, full, alternativeOrigin = RouterOrigin.OFFLINE)
                .anyFullSuccess(),
        )
        assertEquals(
            true,
            routesResult(full, full, alternativeOrigin = RouterOrigin.OFFLINE).anyFullSuccess(),
        )
        // Offline routes only: their refresh on the device is all there is.
        assertEquals(
            true,
            routesResult(
                RouteRefresherStatus.Failure,
                full,
                primaryOrigin = RouterOrigin.OFFLINE,
                alternativeOrigin = RouterOrigin.OFFLINE,
            ).anyFullSuccess(),
        )
    }

    @Test
    fun `a server-built route that is never refreshed leaves the offline routes counting`() {
        val full = RouteRefresherStatus.Success(mockk())
        for (status in listOf(RouteRefresherStatus.Invalid, RouteRefresherStatus.Unsupported)) {
            assertEquals(
                "$status",
                true,
                routesResult(status, full, alternativeOrigin = RouterOrigin.OFFLINE)
                    .anyFullSuccess(),
            )
        }
        // A failed or invalidated server-built route has stale server traffic, and still counts.
        for (status in listOf(RouteRefresherStatus.Failure, RouteRefresherStatus.Invalidated)) {
            assertEquals(
                "$status",
                false,
                routesResult(status, full, alternativeOrigin = RouterOrigin.OFFLINE)
                    .anyFullSuccess(),
            )
        }
    }

    @Test
    fun `a primary route without alternatives`() {
        for (origin in listOf(RouterOrigin.ONLINE, RouterOrigin.OFFLINE)) {
            for ((status, expected) in listOf(
                RouteRefresherStatus.Success(mockk()) to true,
                RouteRefresherStatus.Success(mockk(), chargeOnly = true) to false,
                RouteRefresherStatus.Failure to false,
            )) {
                val result = RoutesRefresherResult(
                    RouteRefresherResult(route(origin), mockk(), status),
                    emptyList(),
                )
                assertEquals("$origin $status", expected, result.anyFullSuccess())
            }
        }
    }

    private fun routesResult(
        primary: RouteRefresherStatus,
        alternative: RouteRefresherStatus,
        primaryOrigin: String = RouterOrigin.ONLINE,
        alternativeOrigin: String = RouterOrigin.ONLINE,
    ) = RoutesRefresherResult(
        RouteRefresherResult(route(primaryOrigin), mockk(), primary),
        listOf(RouteRefresherResult(route(alternativeOrigin), mockk(), alternative)),
    )

    private fun route(origin: String): NavigationRoute = mockk {
        every { this@mockk.origin } returns origin
    }
}
