package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.RouteProgressData
import com.mapbox.navigation.utils.internal.Time
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class RouteRefresherResultProcessorTest {

    private val observersManager = mockk<RefreshObserversManager>(relaxed = true)
    private val expiringDataRemover = mockk<ExpiringDataRemover>(relaxed = true)
    private val timeProvider = mockk<Time>(relaxed = true)
    private val initialTime = 50L
    private val staleDataTimeout = 100L
    private val route1 = mockk<NavigationRoute>(relaxed = true)
    private val route2 = mockk<NavigationRoute>(relaxed = true)
    private val routeProgressData = RouteProgressData(1, 2, 3)
    private val sut = RouteRefresherResultProcessor(
        observersManager,
        expiringDataRemover,
        timeProvider,
        staleDataTimeout
    )

    @Test
    fun `onRoutesRefreshed success notifies observer`() {
        val result = RouteRefresherResult(true, listOf(route1, route2), routeProgressData)

        sut.onRoutesRefreshed(result)

        verify(exactly = 1) { observersManager.onRoutesRefreshed(result) }
    }

    @Test
    fun `onRoutesRefreshed success updates last refresh time`() {
        val result = RouteRefresherResult(true, listOf(route1, route2), routeProgressData)

        sut.onRoutesRefreshed(result)

        clearAllMocks(answers = false)
        val newResult = RouteRefresherResult(false, listOf(route1), routeProgressData)
        sut.onRoutesRefreshed(newResult)
        verify(exactly = 0) { observersManager.onRoutesRefreshed(any()) }
    }

    @Test
    fun `onRoutesRefreshed failure timeout did not pass does not notify observer`() {
        val result = RouteRefresherResult(false, listOf(route1, route2), routeProgressData)
        setUpForTimeoutToNotPass()

        sut.onRoutesRefreshed(result)

        verify(exactly = 0) { expiringDataRemover.removeExpiringDataFromRoutes(any(), any()) }
        verify(exactly = 0) { observersManager.onRoutesRefreshed(any()) }
    }

    @Test
    fun `onRoutesRefreshed failure timeout did not pass does not update last refresh time`() {
        val result = RouteRefresherResult(false, listOf(route1, route2), routeProgressData)
        setUpForTimeoutToNotPass()

        sut.onRoutesRefreshed(result)

        clearAllMocks(answers = false)
        val newResult = RouteRefresherResult(false, listOf(route1), routeProgressData)
        every { timeProvider.millis() } returns initialTime + staleDataTimeout
        sut.onRoutesRefreshed(newResult)
        verify(exactly = 1) { observersManager.onRoutesRefreshed(any()) }
    }

    @Test
    fun `onRoutesRefreshed failure timeout passed routes changed notifies observer`() {
        val expectedRoute1 = mockk<NavigationRoute>(relaxed = true)
        val expectedRoute2 = mockk<NavigationRoute>(relaxed = true)
        val result = RouteRefresherResult(false, listOf(route1, route2), routeProgressData)
        val expected = RouteRefresherResult(
            false,
            listOf(expectedRoute1, expectedRoute2),
            routeProgressData
        )
        every {
            expiringDataRemover.removeExpiringDataFromRoutes(listOf(route1, route2), 1)
        } returns listOf(expectedRoute1, expectedRoute2)
        setUpForTimeoutToPass()

        sut.onRoutesRefreshed(result)

        verify(exactly = 1) { observersManager.onRoutesRefreshed(expected) }
    }

    @Test
    fun `onRoutesRefreshed failure timeout passed routes changed updates last refresh time`() {
        val expectedRoute1 = mockk<NavigationRoute>(relaxed = true)
        val expectedRoute2 = mockk<NavigationRoute>(relaxed = true)
        val result = RouteRefresherResult(false, listOf(route1, route2), routeProgressData)
        every {
            expiringDataRemover.removeExpiringDataFromRoutes(listOf(route1, route2), 1)
        } returns listOf(expectedRoute1, expectedRoute2)
        setUpForTimeoutToPass()

        sut.onRoutesRefreshed(result)

        clearAllMocks(answers = false)
        val newResult = RouteRefresherResult(false, listOf(route1), routeProgressData)
        sut.onRoutesRefreshed(newResult)
        verify(exactly = 0) { observersManager.onRoutesRefreshed(any()) }
    }

    @Test
    fun `onRoutesRefreshed failure timeout passed routes did not change does not notify observer`() {
        val result = RouteRefresherResult(false, listOf(route1, route2), routeProgressData)
        every {
            expiringDataRemover.removeExpiringDataFromRoutes(listOf(route1, route2), 1)
        } returns listOf(route1, route2)
        setUpForTimeoutToPass()

        sut.onRoutesRefreshed(result)

        verify(exactly = 0) { observersManager.onRoutesRefreshed(any()) }
    }

    @Test
    fun `onRoutesRefreshed failure timeout passed routes did not change updates last refresh time`() {
        val result = RouteRefresherResult(false, listOf(route1, route2), routeProgressData)
        every {
            expiringDataRemover.removeExpiringDataFromRoutes(listOf(route1, route2), 1)
        } returns listOf(route1, route2)
        setUpForTimeoutToPass()

        sut.onRoutesRefreshed(result)

        clearAllMocks(answers = false)
        val newResult = RouteRefresherResult(false, listOf(route1), routeProgressData)
        sut.onRoutesRefreshed(newResult)
        verify(exactly = 0) { observersManager.onRoutesRefreshed(any()) }
    }

    @Test
    fun `reset updates last refresh time`() {
        sut.reset()

        clearAllMocks(answers = false)
        val newResult = RouteRefresherResult(false, listOf(route1), routeProgressData)
        sut.onRoutesRefreshed(newResult)
        verify(exactly = 0) { observersManager.onRoutesRefreshed(any()) }
    }

    private fun setUpForTimeoutToNotPass() {
        every { timeProvider.millis() } returns initialTime
        sut.reset()
        every { timeProvider.millis() } returns initialTime + staleDataTimeout - 1
    }

    private fun setUpForTimeoutToPass() {
        every { timeProvider.millis() } returns initialTime
        sut.reset()
        every { timeProvider.millis() } returns initialTime + staleDataTimeout
    }
}
