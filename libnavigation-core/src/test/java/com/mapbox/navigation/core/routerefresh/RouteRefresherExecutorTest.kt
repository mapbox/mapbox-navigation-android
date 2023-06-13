package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RouteRefresherExecutorTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()
    private val routeRefresherResult = RouteRefresherResult(true, mockk())
    private val routeRefresher = mockk<RouteRefresher>(relaxed = true) {
        coEvery { refresh(any(), any()) } returns routeRefresherResult
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
        assertEquals(routeRefresherResult, actual.value)
    }

    @Test
    fun intermediateRequestsAreRemovedFromQueue() = coroutineRule.runBlockingTest {
        val routes2 = listOf<NavigationRoute>(mockk(), mockk(), mockk())
        val routes3 = listOf<NavigationRoute>(mockk(), mockk(), mockk())
        val routes4 = listOf<NavigationRoute>(mockk(), mockk(), mockk())
        val startCallback2 = mockk<() -> Unit>(relaxed = true)
        val startCallback3 = mockk<() -> Unit>(relaxed = true)
        val startCallback4 = mockk<() -> Unit>(relaxed = true)
        val routeRefresherResult2 = RouteRefresherResult(false, mockk())
        val routeRefresherResult3 = RouteRefresherResult(false, mockk())
        val routeRefresherResult4 = RouteRefresherResult(false, mockk())

        coEvery { routeRefresher.refresh(routes, any()) } coAnswers {
            delay(10000)
            routeRefresherResult
        }
        coEvery { routeRefresher.refresh(routes2, any()) } returns routeRefresherResult2
        coEvery { routeRefresher.refresh(routes3, any()) } returns routeRefresherResult3
        coEvery { routeRefresher.refresh(routes4, any()) } returns routeRefresherResult4

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

        assertEquals(routeRefresherResult, result1.await().value)
        assertEquals("Skipping request as a newer one is queued.", result2.await().error)
        assertEquals("Skipping request as a newer one is queued.", result3.await().error)
        assertEquals(routeRefresherResult4, result4.await().value)
    }

    @Test
    fun secondRequestIsExecutedWhenTheFirstOneIsCancelled() = coroutineRule.runBlockingTest {
        coEvery { routeRefresher.refresh(routes, any()) } coAnswers {
            delay(1000)
            routeRefresherResult
        }
        val job1 = launch {
            sut.executeRoutesRefresh(routes, startCallback)
        }
        job1.cancel()

        val actual = sut.executeRoutesRefresh(routes, startCallback)

        assertEquals(routeRefresherResult, actual.value)
    }
}
