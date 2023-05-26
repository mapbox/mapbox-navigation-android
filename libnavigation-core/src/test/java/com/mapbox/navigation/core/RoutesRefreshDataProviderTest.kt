package com.mapbox.navigation.core

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.internal.RouteProgressData
import com.mapbox.navigation.core.internal.RoutesProgressData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RoutesRefreshDataProviderTest {

    private val routesProgressDataProvider = mockk<RoutesProgressDataProvider>(relaxed = true)
    private val sut = RoutesRefreshDataProvider(routesProgressDataProvider)

    @Test(expected = IllegalArgumentException::class)
    fun `getRoutesProgressData for empty routes`() = runBlockingTest {
        sut.getRoutesRefreshData(emptyList())
    }

    @Test
    fun `getRoutesProgressData for primary route only`() = runBlockingTest {
        val primaryRouteProgressData = RouteProgressData(4, 5, 6)
        val routesProgressData = RoutesProgressData(
            primaryRouteProgressData,
            mapOf("id#1" to RouteProgressData(7, 8, 9))
        )
        val primaryRoute = mockk<NavigationRoute>(relaxed = true)
        coEvery {
            routesProgressDataProvider.getRouteRefreshRequestDataOrWait()
        } returns routesProgressData
        val expected = RoutesRefreshData(primaryRoute, primaryRouteProgressData, emptyList())

        val actual = sut.getRoutesRefreshData(listOf(primaryRoute))

        assertEquals(expected, actual)
    }

    @Test
    fun `getRoutesProgressData for valid alternatives`() = runBlockingTest {
        val primaryRouteProgressData = RouteProgressData(4, 5, 6)
        val alternativeRoute1ProgressData = RouteProgressData(7, 8, 9)
        val alternativeRoute2ProgressData = RouteProgressData(10, 11, 12)
        val primaryRoute = mockk<NavigationRoute>(relaxed = true)
        val alternativeRoute1 = mockk<NavigationRoute>(relaxed = true) {
            every { id } returns "id#1"
        }
        val alternativeRoute2 = mockk<NavigationRoute>(relaxed = true) {
            every { id } returns "id#2"
        }
        val routesProgressData = RoutesProgressData(
            primaryRouteProgressData,
            mapOf(
                "id#1" to alternativeRoute1ProgressData,
                "id#2" to alternativeRoute2ProgressData,
            )
        )
        coEvery {
            routesProgressDataProvider.getRouteRefreshRequestDataOrWait()
        } returns routesProgressData
        val expected = RoutesRefreshData(
            primaryRoute,
            primaryRouteProgressData,
            listOf(
                alternativeRoute1 to alternativeRoute1ProgressData,
                alternativeRoute2 to alternativeRoute2ProgressData
            )
        )

        val actual = sut.getRoutesRefreshData(
            listOf(primaryRoute, alternativeRoute1, alternativeRoute2)
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `getRoutesProgressData for one invalid alternative`() = runBlockingTest {
        val primaryRouteProgressData = RouteProgressData(4, 5, 6)
        val alternativeRoute2ProgressData = RouteProgressData(10, 11, 12)
        val primaryRoute = mockk<NavigationRoute>(relaxed = true)
        val alternativeRoute1 = mockk<NavigationRoute>(relaxed = true) {
            every { id } returns "id#1"
        }
        val alternativeRoute2 = mockk<NavigationRoute>(relaxed = true) {
            every { id } returns "id#2"
        }
        val routesProgressData = RoutesProgressData(
            primaryRouteProgressData,
            mapOf(
                "id#2" to alternativeRoute2ProgressData,
                "id#3" to RouteProgressData(3, 5, 7),
            )
        )
        coEvery {
            routesProgressDataProvider.getRouteRefreshRequestDataOrWait()
        } returns routesProgressData
        val expected = RoutesRefreshData(
            primaryRoute,
            primaryRouteProgressData,
            listOf(alternativeRoute1 to null, alternativeRoute2 to alternativeRoute2ProgressData)
        )

        val actual = sut.getRoutesRefreshData(
            listOf(primaryRoute, alternativeRoute1, alternativeRoute2)
        )

        assertEquals(expected, actual)
    }
}
