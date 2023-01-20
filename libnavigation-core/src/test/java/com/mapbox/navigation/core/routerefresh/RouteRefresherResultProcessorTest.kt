package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.RouteProgressData
import com.mapbox.navigation.core.RoutesProgressData
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
    private val routesProgressData1 = RoutesProgressData(
        mockk(relaxed = true),
        RouteProgressData(1, 2, 3),
        listOf(mockk<NavigationRoute>(relaxed = true) to RouteProgressData(4, 5, 6))
    )
    private val routesProgressData2 = RoutesProgressData(
        mockk(relaxed = true),
        RouteProgressData(3, 4, 5),
        listOf(mockk<NavigationRoute>(relaxed = true) to RouteProgressData(4, 5, 6))
    )
    private val sut = RouteRefresherResultProcessor(
        observersManager,
        expiringDataRemover,
        timeProvider,
        staleDataTimeout
    )

    @Test
    fun `onRoutesRefreshed success notifies observer`() {
        val result = RouteRefresherResult(true, mockk())

        sut.onRoutesRefreshed(result)

        verify(exactly = 1) { observersManager.onRoutesRefreshed(result) }
    }

    @Test
    fun `onRoutesRefreshed success updates last refresh time`() {
        val result = RouteRefresherResult(true, mockk())

        sut.onRoutesRefreshed(result)

        clearAllMocks(answers = false)
        val newResult = RouteRefresherResult(false, mockk())
        sut.onRoutesRefreshed(newResult)
        verify(exactly = 0) { observersManager.onRoutesRefreshed(any()) }
    }

    @Test
    fun `onRoutesRefreshed failure timeout did not pass does not notify observer`() {
        val result = RouteRefresherResult(false, mockk())
        setUpForTimeoutToNotPass()

        sut.onRoutesRefreshed(result)

        verify(exactly = 0) { expiringDataRemover.removeExpiringDataFromRoutesProgressData(any()) }
        verify(exactly = 0) { observersManager.onRoutesRefreshed(any()) }
    }

    @Test
    fun `onRoutesRefreshed failure timeout did not pass does not update last refresh time`() {
        val result = RouteRefresherResult(false, mockk())
        setUpForTimeoutToNotPass()

        sut.onRoutesRefreshed(result)

        clearAllMocks(answers = false)
        val newResult = RouteRefresherResult(false, mockk())
        every { timeProvider.millis() } returns initialTime + staleDataTimeout
        sut.onRoutesRefreshed(newResult)
        verify(exactly = 1) { observersManager.onRoutesRefreshed(any()) }
    }

    @Test
    fun `onRoutesRefreshed failure timeout passed routes changed notifies observer`() {
        val result = RouteRefresherResult(false, routesProgressData1)
        val expected = RouteRefresherResult(false, routesProgressData2)
        every {
            expiringDataRemover.removeExpiringDataFromRoutesProgressData(routesProgressData1)
        } returns routesProgressData2
        setUpForTimeoutToPass()

        sut.onRoutesRefreshed(result)

        verify(exactly = 1) { observersManager.onRoutesRefreshed(expected) }
    }

    @Test
    fun `onRoutesRefreshed failure timeout passed routes changed updates last refresh time`() {
        val result = RouteRefresherResult(false, routesProgressData1)
        every {
            expiringDataRemover.removeExpiringDataFromRoutesProgressData(routesProgressData1)
        } returns routesProgressData2
        setUpForTimeoutToPass()

        sut.onRoutesRefreshed(result)

        clearAllMocks(answers = false)
        val newResult = RouteRefresherResult(false, mockk())
        sut.onRoutesRefreshed(newResult)
        verify(exactly = 0) { observersManager.onRoutesRefreshed(any()) }
    }

    @Test
    fun `onRoutesRefreshed failure timeout passed routes did not change does not notify observer`() {
        val result = RouteRefresherResult(false, routesProgressData1)
        every {
            expiringDataRemover.removeExpiringDataFromRoutesProgressData(routesProgressData1)
        } returns routesProgressData1
        setUpForTimeoutToPass()

        sut.onRoutesRefreshed(result)

        verify(exactly = 0) { observersManager.onRoutesRefreshed(any()) }
    }

    @Test
    fun `onRoutesRefreshed failure timeout passed routes did not change updates last refresh time`() {
        val result = RouteRefresherResult(false, routesProgressData1)
        every {
            expiringDataRemover.removeExpiringDataFromRoutesProgressData(routesProgressData1)
        } returns routesProgressData1
        setUpForTimeoutToPass()

        sut.onRoutesRefreshed(result)

        clearAllMocks(answers = false)
        val newResult = RouteRefresherResult(false, mockk())
        sut.onRoutesRefreshed(newResult)
        verify(exactly = 0) { observersManager.onRoutesRefreshed(any()) }
    }

    @Test
    fun `reset updates last refresh time`() {
        sut.reset()

        clearAllMocks(answers = false)
        val newResult = RouteRefresherResult(false, mockk())
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
