package com.mapbox.navigation.core.routerefresh

import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.utils.internal.LoggerFrontend
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
class ImmediateRouteRefreshControllerTest {

    private val logger = mockk<LoggerFrontend>(relaxed = true)

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @get:Rule
    val loggerRule = LoggingFrontendTestRule(logger)

    private val routeRefresherExecutor = mockk<RouteRefresherExecutor>(relaxed = true)
    private val stateHolder = mockk<RouteRefreshStateHolder>(relaxed = true)
    private val attemptListener = mockk<RoutesRefreshAttemptListener>(relaxed = true)
    private val listener = mockk<RouteRefresherListener>(relaxed = true)
    private val clientCallback =
        mockk<(Expected<String, RoutesRefresherResult>) -> Unit>(relaxed = true)
    private val routes = listOf<NavigationRoute>(mockk())

    private val sut = ImmediateRouteRefreshController(
        routeRefresherExecutor,
        stateHolder,
        coroutineRule.coroutineScope,
        listener,
        attemptListener
    )

    @Test(expected = IllegalArgumentException::class)
    fun requestRoutesRefreshWithEmptyRoutes() = coroutineRule.runBlockingTest {
        sut.requestRoutesRefresh(emptyList(), clientCallback)
    }

    @Test
    fun requestRoutesRefreshPostsRefreshRequest() = coroutineRule.runBlockingTest {
        sut.requestRoutesRefresh(routes, clientCallback)

        coVerify(exactly = 1) { routeRefresherExecutor.executeRoutesRefresh(routes, any()) }
    }

    @Test
    fun routesRefreshStarted() = coroutineRule.runBlockingTest {
        sut.requestRoutesRefresh(routes, clientCallback)
        val startCallback = interceptStartCallback()

        startCallback()

        verify(exactly = 1) { stateHolder.onStarted() }
    }

    @Test
    fun routesRefreshFinishedSuccessfully() = coroutineRule.runBlockingTest {
        val result = mockk<RoutesRefresherResult> { every { anySuccess() } returns true }
        coEvery {
            routeRefresherExecutor.executeRoutesRefresh(any(), any())
        } returns ExpectedFactory.createValue(result)

        sut.requestRoutesRefresh(routes, clientCallback)

        verify(exactly = 1) { attemptListener.onRoutesRefreshAttemptFinished(result) }
        verify(exactly = 1) { stateHolder.onSuccess() }
        verify(exactly = 1) { listener.onRoutesRefreshed(result) }
        verify(exactly = 1) { clientCallback(match { it.value == result }) }
    }

    @Test
    fun routesRefreshFinishedWithFailure() = coroutineRule.runBlockingTest {
        val result = mockk<RoutesRefresherResult> {
            every { anySuccess() } returns false
            every { anyRequestFailed() } returns true
        }
        coEvery {
            routeRefresherExecutor.executeRoutesRefresh(any(), any())
        } returns ExpectedFactory.createValue(result)

        sut.requestRoutesRefresh(routes, clientCallback)

        verify(exactly = 1) { attemptListener.onRoutesRefreshAttemptFinished(result) }
        verify(exactly = 1) { stateHolder.onFailure(null) }
        verify(exactly = 1) { clientCallback(match { it.value == result }) }
        verify(exactly = 1) { listener.onRoutesRefreshed(result) }
    }

    @Test
    fun routesRefreshFinishedWithError() = coroutineRule.runBlockingTest {
        val error: Expected<String, RoutesRefresherResult> =
            ExpectedFactory.createError("Some error")
        coEvery {
            routeRefresherExecutor.executeRoutesRefresh(any(), any())
        } returns error

        sut.requestRoutesRefresh(routes, clientCallback)

        verify(exactly = 0) {
            attemptListener.onRoutesRefreshAttemptFinished(any())
            stateHolder.onFailure(any())
            stateHolder.onSuccess()
            listener.onRoutesRefreshed(any())
        }
        verify(exactly = 1) { clientCallback.invoke(error) }
        verify(exactly = 1) {
            logger.logW(
                "Route refresh on-demand error: Some error",
                "RouteRefreshController"
            )
        }
    }

    private fun interceptStartCallback(): () -> Unit {
        val callbacks = mutableListOf<() -> Unit>()
        coVerify { routeRefresherExecutor.executeRoutesRefresh(any(), capture(callbacks)) }
        return callbacks.last()
    }
}
