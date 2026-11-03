package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
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
        staleDataTimeout,
    )

    @Test
    fun `onRoutesRefreshed success notifies observer`() {
        val result = mockk<RoutesRefresherResult> {
            every { anySuccess() } returns true
            every { anyFullSuccess() } returns true
        }

        sut.onRoutesRefreshed(result)

        verify(exactly = 1) { observersManager.onRoutesRefreshed(result) }
    }

    @Test
    fun `onRoutesRefreshed success does not change state`() {
        val result = mockk<RoutesRefresherResult> {
            every { anySuccess() } returns true
            every { anyFullSuccess() } returns true
        }

        sut.onRoutesRefreshed(result)

        verify(exactly = 0) { stateHolder.onClearedExpired() }
    }

    @Test
    fun `onRoutesRefreshed success updates last refresh time`() {
        val result = mockk<RoutesRefresherResult> {
            every { anySuccess() } returns true
            every { anyFullSuccess() } returns true
        }

        sut.onRoutesRefreshed(result)

        clearAllMocks(answers = false)
        val newResult = mockk<RoutesRefresherResult> {
            every { anySuccess() } returns false
            every { anyFullSuccess() } returns false
        }
        sut.onRoutesRefreshed(newResult)
        verify(exactly = 0) { observersManager.onRoutesRefreshed(any()) }
    }

    @Test
    fun `onRoutesRefreshed failure timeout did not pass does not notify observer`() {
        val result = mockk<RoutesRefresherResult> {
            every { anySuccess() } returns false
            every { anyFullSuccess() } returns false
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
            every { anyFullSuccess() } returns false
        }
        setUpForTimeoutToNotPass()

        sut.onRoutesRefreshed(result)

        clearAllMocks(answers = false)
        val newResult = mockk<RoutesRefresherResult> {
            every { anySuccess() } returns false
            every { anyFullSuccess() } returns false
        }
        every { timeProvider.millis() } returns initialTime + staleDataTimeout
        sut.onRoutesRefreshed(newResult)
        verify(exactly = 1) { observersManager.onRoutesRefreshed(any()) }
    }

    @Test
    fun `onRoutesRefreshed failure timeout did not pass does not change state`() {
        val result = mockk<RoutesRefresherResult> {
            every { anySuccess() } returns false
            every { anyFullSuccess() } returns false
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
            every { anyFullSuccess() } returns false
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
            every { anyFullSuccess() } returns false
        }
        every {
            expiringDataRemover.removeExpiringDataFromRoutesProgressData(result)
        } returns expectedResult
        setUpForTimeoutToPass()

        sut.onRoutesRefreshed(result)

        clearAllMocks(answers = false)
        val newResult = mockk<RoutesRefresherResult> {
            every { anySuccess() } returns false
            every { anyFullSuccess() } returns false
        }
        sut.onRoutesRefreshed(newResult)
        verify(exactly = 0) { observersManager.onRoutesRefreshed(any()) }
    }

    @Test
    fun `onRoutesRefreshed failure timeout passed routes changed changes state`() {
        val expectedResult = mockk<RoutesRefresherResult>()
        val result = mockk<RoutesRefresherResult> {
            every { anySuccess() } returns false
            every { anyFullSuccess() } returns false
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
            every { anyFullSuccess() } returns false
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
            every { anyFullSuccess() } returns false
        }
        every {
            expiringDataRemover.removeExpiringDataFromRoutesProgressData(result)
        } returns result
        setUpForTimeoutToPass()

        sut.onRoutesRefreshed(result)

        clearAllMocks(answers = false)
        val newResult = mockk<RoutesRefresherResult> {
            every { anySuccess() } returns false
            every { anyFullSuccess() } returns false
        }
        sut.onRoutesRefreshed(newResult)
        verify(exactly = 0) { observersManager.onRoutesRefreshed(any()) }
    }

    @Test
    fun `onRoutesRefreshed failure timeout passed routes did not change changes state`() {
        val result = mockk<RoutesRefresherResult> {
            every { anySuccess() } returns false
            every { anyFullSuccess() } returns false
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
            every { anyFullSuccess() } returns false
        }
        sut.onRoutesRefreshed(newResult)
        verify(exactly = 0) { observersManager.onRoutesRefreshed(any()) }
    }

    @Test
    fun `charge only success timeout did not pass notifies observer with the result`() {
        val result = chargeOnlyResult()
        setUpForTimeoutToNotPass()

        sut.onRoutesRefreshed(result)

        verify(exactly = 0) { expiringDataRemover.removeExpiringDataFromRoutesProgressData(any()) }
        verify(exactly = 0) { stateHolder.onClearedExpired() }
        verify(exactly = 1) { observersManager.onRoutesRefreshed(result) }
    }

    @Test
    fun `charge only success does not update last refresh time`() {
        setUpForTimeoutToNotPass()
        sut.onRoutesRefreshed(chargeOnlyResult())

        clearAllMocks(answers = false)
        val result = chargeOnlyResult()
        val clearedResult = mockk<RoutesRefresherResult>()
        every {
            expiringDataRemover.removeExpiringDataFromRoutesProgressData(result)
        } returns clearedResult
        every { timeProvider.millis() } returns initialTime + staleDataTimeout

        sut.onRoutesRefreshed(result)

        verify(exactly = 1) { stateHolder.onClearedExpired() }
        verify(exactly = 1) { observersManager.onRoutesRefreshed(clearedResult) }
    }

    @Test
    fun `charge only success after expiring data removal keeps removing it`() {
        setUpForTimeoutToPass()
        sut.onRoutesRefreshed(chargeOnlyResult())

        clearAllMocks(answers = false)
        val result = chargeOnlyResult()
        val clearedResult = mockk<RoutesRefresherResult>()
        every {
            expiringDataRemover.removeExpiringDataFromRoutesProgressData(result)
        } returns clearedResult
        every { timeProvider.millis() } returns initialTime + staleDataTimeout + 1

        sut.onRoutesRefreshed(result)

        verify(exactly = 0) { stateHolder.onClearedExpired() }
        verify(exactly = 1) { observersManager.onRoutesRefreshed(clearedResult) }
    }

    @Test
    fun `full success after expiring data removal stops removing it`() {
        setUpForTimeoutToPass()
        sut.onRoutesRefreshed(chargeOnlyResult())
        val fullResult = mockk<RoutesRefresherResult> {
            every { anySuccess() } returns true
            every { anyFullSuccess() } returns true
        }
        sut.onRoutesRefreshed(fullResult)

        clearAllMocks(answers = false)
        val result = chargeOnlyResult()
        sut.onRoutesRefreshed(result)

        verify(exactly = 0) { expiringDataRemover.removeExpiringDataFromRoutesProgressData(any()) }
        verify(exactly = 1) { observersManager.onRoutesRefreshed(result) }
    }

    @Test
    fun `charge only success with a failed alternative is cleared as a whole after timeout`() {
        val result = RoutesRefresherResult(
            RouteRefresherResult(
                onlineRoute(),
                mockk(),
                RouteRefresherStatus.Success(mockk(), chargeOnly = true),
            ),
            listOf(RouteRefresherResult(onlineRoute(), mockk(), RouteRefresherStatus.Failure)),
        )
        val clearedResult = mockk<RoutesRefresherResult>()
        every {
            expiringDataRemover.removeExpiringDataFromRoutesProgressData(result)
        } returns clearedResult
        setUpForTimeoutToPass()

        sut.onRoutesRefreshed(result)

        verify(exactly = 1) { stateHolder.onClearedExpired() }
        verify(exactly = 1) { observersManager.onRoutesRefreshed(clearedResult) }
    }

    @Test
    fun `an offline alternative refreshed on the device doesn't keep the online route fresh`() {
        val result = RoutesRefresherResult(
            RouteRefresherResult(
                onlineRoute(),
                mockk(),
                RouteRefresherStatus.Success(mockk(), chargeOnly = true),
            ),
            listOf(
                RouteRefresherResult(
                    route(RouterOrigin.OFFLINE),
                    mockk(),
                    RouteRefresherStatus.Success(mockk()),
                ),
            ),
        )
        val clearedResult = mockk<RoutesRefresherResult>()
        every {
            expiringDataRemover.removeExpiringDataFromRoutesProgressData(result)
        } returns clearedResult
        setUpForTimeoutToPass()

        sut.onRoutesRefreshed(result)

        verify(exactly = 1) { stateHolder.onClearedExpired() }
        verify(exactly = 1) { observersManager.onRoutesRefreshed(clearedResult) }
    }

    @Test
    fun `an offline route refreshed next to an unsupported online route keeps the timer fresh`() {
        val result = RoutesRefresherResult(
            RouteRefresherResult(onlineRoute(), mockk(), RouteRefresherStatus.Unsupported),
            listOf(
                RouteRefresherResult(
                    route(RouterOrigin.OFFLINE),
                    mockk(),
                    RouteRefresherStatus.Success(mockk()),
                ),
            ),
        )
        setUpForTimeoutToPass()

        sut.onRoutesRefreshed(result)

        verify(exactly = 0) { stateHolder.onClearedExpired() }
        verify(exactly = 0) { expiringDataRemover.removeExpiringDataFromRoutesProgressData(any()) }
        verify(exactly = 1) { observersManager.onRoutesRefreshed(result) }
    }

    private fun route(origin: String) = mockk<NavigationRoute> {
        every { this@mockk.origin } returns origin
    }

    private fun onlineRoute() = mockk<NavigationRoute> {
        every { origin } returns RouterOrigin.ONLINE
    }

    private fun chargeOnlyResult() = mockk<RoutesRefresherResult> {
        every { anySuccess() } returns true
        every { anyFullSuccess() } returns false
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
