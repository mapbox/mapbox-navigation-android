package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.RouteProgressData
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RouteRefresherExecutorTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()
    private val routeRefresherResult = RouteRefresherResult(
        true,
        emptyList(),
        RouteProgressData(1, 2, 3)
    )
    private val routeRefresher = mockk<RouteRefresher>(relaxed = true) {
        coEvery { refresh(any(), any()) } returns routeRefresherResult
    }
    private val timeout = 100L
    private val sut = RouteRefresherExecutor(routeRefresher, coroutineRule.coroutineScope, timeout)
    private val routes = listOf<NavigationRoute>(mockk(), mockk())
    private val callback = mockk<RouteRefresherProgressCallback>(relaxed = true)

    @Test
    fun postRoutesToRefresh() = coroutineRule.runBlockingTest {
        sut.postRoutesToRefresh(routes, callback)

        coVerifyOrder {
            callback.onStarted()
            callback.onResult(routeRefresherResult)
        }
        coVerify(exactly = 1) {
            routeRefresher.refresh(routes, timeout)
        }
    }

    @Test
    fun twoRequestsAreNotExecutedSimultaneously() = coroutineRule.runBlockingTest {
        val routes2 = listOf<NavigationRoute>(mockk(), mockk(), mockk())
        val callback2 = mockk<RouteRefresherProgressCallback>(relaxed = true)
        val routeRefresherResult2 = RouteRefresherResult(
            false,
            emptyList(),
            RouteProgressData(4, 5, 6)
        )

        coEvery { routeRefresher.refresh(routes, any()) } coAnswers {
            delay(10000)
            routeRefresherResult
        }
        coEvery { routeRefresher.refresh(routes2, any()) } returns routeRefresherResult2

        sut.postRoutesToRefresh(routes, callback)

        coVerify(exactly = 1) { callback.onStarted() }
        coVerify(exactly = 0) { callback.onResult(any()) }
        clearAllMocks(answers = false)

        sut.postRoutesToRefresh(routes2, callback2)

        coVerify(exactly = 0) { callback.onResult(any()) }
        coVerify(exactly = 0) { callback2.onStarted() }
        coVerify(exactly = 0) { callback2.onResult(any()) }

        coroutineRule.testDispatcher.advanceTimeBy(10000)

        coVerify {
            callback.onResult(routeRefresherResult)
        }
        coVerify(exactly = 0) {
            callback2.onStarted()
            callback2.onResult(any())
        }
    }
}
