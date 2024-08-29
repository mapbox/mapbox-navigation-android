package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RouteRefresherExecutorTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()
    private val routesRefresherResult = mockk<RoutesRefresherResult> {
        every { anySuccess() } returns true
    }
    private val routeRefresher = mockk<RouteRefresher>(relaxed = true) {
        coEvery { refresh(any(), any()) } returns routesRefresherResult
    }
    private val timeout = 100L
    private val sut = RouteRefresherExecutor(routeRefresher, timeout, coroutineRule.coroutineScope)
    private val routes = listOf<NavigationRoute>(mockk(), mockk())
    private val startCallback = mockk<() -> Unit>(relaxed = true)

    @Test
    fun executeRoutesRefresh() = coroutineRule.runBlockingTest {
        val actual = sut.executeRoutesRefresh(routes, startCallback)

        coVerify(exactly = 1) {
            startCallback.invoke()
            routeRefresher.refresh(routes, timeout)
        }
        assertEquals(
            routesRefresherResult,
            (actual as RoutesRefresherExecutorResult.Finished).value,
        )
    }

    @Test
    fun intermediateRequestsAreRemovedFromQueue() = coroutineRule.runBlockingTest {
        val routes2 = listOf<NavigationRoute>(mockk(), mockk(), mockk())
        val routes3 = listOf<NavigationRoute>(mockk(), mockk(), mockk())
        val routes4 = listOf<NavigationRoute>(mockk(), mockk(), mockk())
        val startCallback2 = mockk<() -> Unit>(relaxed = true)
        val startCallback3 = mockk<() -> Unit>(relaxed = true)
        val startCallback4 = mockk<() -> Unit>(relaxed = true)
        val routesRefresherResult2 = mockk<RoutesRefresherResult> {
            every { anySuccess() } returns false
        }
        val routesRefresherResult3 = mockk<RoutesRefresherResult> {
            every { anySuccess() } returns false
        }
        val routesRefresherResult4 = mockk<RoutesRefresherResult> {
            every { anySuccess() } returns false
        }

        coEvery { routeRefresher.refresh(routes, any()) } coAnswers {
            delay(10000)
            routesRefresherResult
        }
        coEvery { routeRefresher.refresh(routes2, any()) } returns routesRefresherResult2
        coEvery { routeRefresher.refresh(routes3, any()) } returns routesRefresherResult3
        coEvery { routeRefresher.refresh(routes4, any()) } returns routesRefresherResult4

        val result1 = async {
            sut.executeRoutesRefresh(routes, startCallback)
        }

        coVerify(exactly = 1) { startCallback() }
        clearAllMocks(answers = false)

        val result2 = async {
            sut.executeRoutesRefresh(routes2, startCallback2)
        }
        val result3 = async {
            sut.executeRoutesRefresh(routes3, startCallback3)
        }
        val result4 = async {
            sut.executeRoutesRefresh(routes4, startCallback4)
        }

        assertEquals(
            routesRefresherResult,
            (result1.await() as RoutesRefresherExecutorResult.Finished).value,
        )
        assertTrue(result2.await() is RoutesRefresherExecutorResult.ReplacedByNewer)
        assertTrue(result3.await() is RoutesRefresherExecutorResult.ReplacedByNewer)
        assertEquals(
            routesRefresherResult4,
            (result4.await() as RoutesRefresherExecutorResult.Finished).value,
        )
    }

    @Test
    fun secondRequestIsExecutedWhenTheFirstOneIsCancelled() = coroutineRule.runBlockingTest {
        coEvery { routeRefresher.refresh(routes, any()) } coAnswers {
            delay(1000)
            routesRefresherResult
        }
        val job1 = launch {
            sut.executeRoutesRefresh(routes, startCallback)
        }
        job1.cancel()

        val actual = sut.executeRoutesRefresh(routes, startCallback)

        assertEquals(
            routesRefresherResult,
            (actual as RoutesRefresherExecutorResult.Finished).value,
        )
    }
}
