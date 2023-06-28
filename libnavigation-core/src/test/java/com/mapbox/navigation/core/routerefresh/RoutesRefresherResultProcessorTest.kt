package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.utils.internal.Time
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class RoutesRefresherResultProcessorTest {

    private val stateHolder = mockk<RouteRefreshStateHolder>(relaxed = true)
    private val observersManager = mockk<RefreshObserversManager>(relaxed = true)
    private val expiringDataRemover = mockk<ExpiringDataRemover>(relaxed = true)
    private val timeProvider = mockk<Time>(relaxed = true)
    private val initialTime = 50L
    private val staleDataTimeout = 100L
    private val sut = RouteRefresherResultProcessor(
        stateHolder,
        observersManager,
        expiringDataRemover,
        timeProvider,
        staleDataTimeout
    )

    @Test
    fun `onRoutesRefreshed success notifies observer`() {
        val result = mockk<RoutesRefresherResult> {
            every { anySuccess() } returns true
        }

        sut.onRoutesRefreshed(result)

        verify(exactly = 1) { observersManager.onRoutesRefreshed(result) }
    }

    @Test
    fun `onRoutesRefreshed success does not change state`() {
        val result = mockk<RoutesRefresherResult> {
            every { anySuccess() } returns true
        }

        sut.onRoutesRefreshed(result)

        verify(exactly = 0) { stateHolder.onClearedExpired() }
    }

    @Test
    fun `onRoutesRefreshed success updates last refresh time`() {
        val result = mockk<RoutesRefresherResult> {
            every { anySuccess() } returns true
        }

        sut.onRoutesRefreshed(result)

        clearAllMocks(answers = false)
        val newResult = mockk<RoutesRefresherResult> {
            every { anySuccess() } returns false
        }
        sut.onRoutesRefreshed(newResult)
        verify(exactly = 0) { observersManager.onRoutesRefreshed(any()) }
    }

    @Test
    fun `onRoutesRefreshed failure timeout did not pass does not notify observer`() {
        val result = mockk<RoutesRefresherResult> {
            every { anySuccess() } returns false
        }
        setUpForTimeoutToNotPass()

        sut.onRoutesRefreshed(result)

        verify(exactly = 0) { expiringDataRemover.removeExpiringDataFromRoutesProgressData(any()) }
        verify(exactly = 0) { observersManager.onRoutesRefreshed(any()) }
    }

    @Test
    fun `onRoutesRefreshed failure timeout did not pass does not update last refresh time`() {
        val result = mockk<RoutesRefresherResult> {
            every { anySuccess() } returns false
        }
        setUpForTimeoutToNotPass()

        sut.onRoutesRefreshed(result)

        clearAllMocks(answers = false)
        val newResult = mockk<RoutesRefresherResult> {
            every { anySuccess() } returns false
        }
        every { timeProvider.millis() } returns initialTime + staleDataTimeout
        sut.onRoutesRefreshed(newResult)
        verify(exactly = 1) { observersManager.onRoutesRefreshed(any()) }
    }

    @Test
    fun `onRoutesRefreshed failure timeout did not pass does not change state`() {
        val result = mockk<RoutesRefresherResult> {
            every { anySuccess() } returns false
        }
        setUpForTimeoutToNotPass()

        sut.onRoutesRefreshed(result)

        verify(exactly = 0) { stateHolder.onClearedExpired() }
    }

    @Test
    fun `onRoutesRefreshed failure timeout passed routes changed notifies observer`() {
        val expectedResult = mockk<RoutesRefresherResult>()
        val result = mockk<RoutesRefresherResult> {
            every { anySuccess() } returns false
        }
        every {
            expiringDataRemover.removeExpiringDataFromRoutesProgressData(result)
        } returns expectedResult
        setUpForTimeoutToPass()

        sut.onRoutesRefreshed(result)

        verify(exactly = 1) { observersManager.onRoutesRefreshed(expectedResult) }
    }

    @Test
    fun `onRoutesRefreshed failure timeout passed routes changed updates last refresh time`() {
        val expectedResult = mockk<RoutesRefresherResult>()
        val result = mockk<RoutesRefresherResult> {
            every { anySuccess() } returns false
        }
        every {
            expiringDataRemover.removeExpiringDataFromRoutesProgressData(result)
        } returns expectedResult
        setUpForTimeoutToPass()

        sut.onRoutesRefreshed(result)

        clearAllMocks(answers = false)
        val newResult = mockk<RoutesRefresherResult> {
            every { anySuccess() } returns false
        }
        sut.onRoutesRefreshed(newResult)
        verify(exactly = 0) { observersManager.onRoutesRefreshed(any()) }
    }

    @Test
    fun `onRoutesRefreshed failure timeout passed routes changed changes state`() {
        val expectedResult = mockk<RoutesRefresherResult>()
        val result = mockk<RoutesRefresherResult> {
            every { anySuccess() } returns false
        }
        every {
            expiringDataRemover.removeExpiringDataFromRoutesProgressData(result)
        } returns expectedResult
        setUpForTimeoutToPass()

        sut.onRoutesRefreshed(result)

        verify(exactly = 1) { stateHolder.onClearedExpired() }
    }

    @Test
    fun `onRoutesRefreshed failure timeout passed routes did not change does not notify observer`() {
        val result = mockk<RoutesRefresherResult> {
            every { anySuccess() } returns false
        }
        every {
            expiringDataRemover.removeExpiringDataFromRoutesProgressData(result)
        } returns result
        setUpForTimeoutToPass()

        sut.onRoutesRefreshed(result)

        verify(exactly = 0) { observersManager.onRoutesRefreshed(any()) }
    }

    @Test
    fun `onRoutesRefreshed failure timeout passed routes did not change updates last refresh time`() {
        val result = mockk<RoutesRefresherResult> {
            every { anySuccess() } returns false
        }
        every {
            expiringDataRemover.removeExpiringDataFromRoutesProgressData(result)
        } returns result
        setUpForTimeoutToPass()

        sut.onRoutesRefreshed(result)

        clearAllMocks(answers = false)
        val newResult = mockk<RoutesRefresherResult> {
            every { anySuccess() } returns false
        }
        sut.onRoutesRefreshed(newResult)
        verify(exactly = 0) { observersManager.onRoutesRefreshed(any()) }
    }

    @Test
    fun `onRoutesRefreshed failure timeout passed routes did not change changes state`() {
        val result = mockk<RoutesRefresherResult> {
            every { anySuccess() } returns false
        }
        every {
            expiringDataRemover.removeExpiringDataFromRoutesProgressData(result)
        } returns result
        setUpForTimeoutToPass()

        sut.onRoutesRefreshed(result)

        verify(exactly = 1) { stateHolder.onClearedExpired() }
    }

    @Test
    fun `reset updates last refresh time`() {
        sut.reset()

        clearAllMocks(answers = false)
        val newResult = mockk<RoutesRefresherResult> {
            every { anySuccess() } returns false
        }
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
