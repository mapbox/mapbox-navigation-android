package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.utils.routeRefresh.RouteRefreshUtils
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.utils.internal.LoggerFrontend
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
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
        mockk<(RoutesRefresherExecutorResult) -> Unit>(relaxed = true)
    private val routes = listOf<NavigationRoute>(mockk())
    private val routeRefreshUtils by lazy { RouteRefreshUtils() }
    private var currentPrimaryRouteId: String? = null
    private val currentPrimaryRouteIdProvider: () -> String? = { currentPrimaryRouteId }

    private val sut = ImmediateRouteRefreshController(
        routeRefresherExecutor,
        stateHolder,
        coroutineRule.createTestScope(),
        listener,
        attemptListener,
        routeRefreshUtils,
        currentPrimaryRouteIdProvider,
    )

    @Test(expected = IllegalArgumentException::class)
    fun requestRoutesRefreshWithEmptyRoutes() = coroutineRule.runBlockingTest {
        sut.requestRoutesRefresh(emptyList(), clientCallback)
    }

    @Test
    fun requestRoutesRefreshPostsRefreshRequest() = coroutineRule.runBlockingTest {
        sut.requestRoutesRefresh(routes, clientCallback)

        coVerify(exactly = 1) { routeRefresherExecutor.executeRoutesRefresh(routes, any()) }
        verify(exactly = 0) { stateHolder.onCancel() }
    }

    @Test
    fun routesRefreshStarted() = coroutineRule.runBlockingTest {
        sut.requestRoutesRefresh(routes, clientCallback)
        val startCallback = interceptStartCallback()

        startCallback()

        verify(exactly = 1) { stateHolder.onStarted() }
        verify(exactly = 0) { stateHolder.onCancel() }
    }

    @Test
    fun routesRefreshFinishedSuccessfully() = coroutineRule.runBlockingTest {
        val refreshedRoute = mockk<NavigationRoute>(relaxed = true)
        val result = mockk<RoutesRefresherResult> {
            every { anySuccess() } returns true
            every { primaryRouteRefresherResult } returns mockk {
                every { route } returns refreshedRoute
            }
        }
        coEvery {
            routeRefresherExecutor.executeRoutesRefresh(any(), any())
        } returns RoutesRefresherExecutorResult.Finished(result)

        sut.requestRoutesRefresh(routes, clientCallback)

        verify(exactly = 1) { attemptListener.onRoutesRefreshAttemptFinished(result) }
        verify(exactly = 1) { stateHolder.onSuccess() }
        verify(exactly = 1) { listener.onRoutesRefreshed(result) }
        verify(exactly = 1) {
            clientCallback(
                match { (it as RoutesRefresherExecutorResult.Finished).value == result },
            )
        }
        verify(exactly = 0) { stateHolder.onCancel() }
    }

    @Test
    fun routesRefreshFinishedWithFailure() = coroutineRule.runBlockingTest {
        val result = mockk<RoutesRefresherResult> {
            every { anySuccess() } returns false
            every { anyRequestFailed() } returns true
        }
        coEvery {
            routeRefresherExecutor.executeRoutesRefresh(any(), any())
        } returns RoutesRefresherExecutorResult.Finished(result)

        sut.requestRoutesRefresh(routes, clientCallback)

        verify(exactly = 1) { attemptListener.onRoutesRefreshAttemptFinished(result) }
        verify(exactly = 1) { stateHolder.onFailure(null) }
        verify(exactly = 1) {
            clientCallback(
                match { (it as RoutesRefresherExecutorResult.Finished).value == result },
            )
        }
        verify(exactly = 1) { listener.onRoutesRefreshed(result) }
        verify(exactly = 0) { stateHolder.onCancel() }
    }

    @Test
    fun routesRefreshFinishedButStalePrimaryRouteSuppressesPublish() =
        coroutineRule.runBlockingTest {
            // Simulate a route swap that happened during the refresh, SDK now has a
            // different primary route than the one carried by the refresh result.
            currentPrimaryRouteId = "id#new"
            val staleRoute = mockk<NavigationRoute>(relaxed = true) {
                every { id } returns "id#stale"
            }
            val result = mockk<RoutesRefresherResult> {
                every { anySuccess() } returns true
                every { primaryRouteRefresherResult } returns mockk {
                    every { route } returns staleRoute
                }
            }
            coEvery {
                routeRefresherExecutor.executeRoutesRefresh(any(), any())
            } returns RoutesRefresherExecutorResult.Finished(result)

            sut.requestRoutesRefresh(routes, clientCallback)

            // attempt and state still happen
            verify(exactly = 1) { attemptListener.onRoutesRefreshAttemptFinished(result) }
            verify(exactly = 1) { stateHolder.onSuccess() }
            // client callback still fires with Finished so the public API resolves
            verify(exactly = 1) {
                clientCallback(
                    match { (it as RoutesRefresherExecutorResult.Finished).value == result },
                )
            }
            // but the publish to observers is suppressed
            verify(exactly = 0) { listener.onRoutesRefreshed(any()) }
        }

    @Test
    fun routesRefreshFinishedWithError() = coroutineRule.runBlockingTest {
        val error = RoutesRefresherExecutorResult.ReplacedByNewer
        coEvery {
            routeRefresherExecutor.executeRoutesRefresh(any(), any())
        } returns error

        sut.requestRoutesRefresh(routes, clientCallback)

        verify(exactly = 0) {
            attemptListener.onRoutesRefreshAttemptFinished(any())
            stateHolder.onFailure(any())
            stateHolder.onSuccess()
            stateHolder.onCancel()
            listener.onRoutesRefreshed(any())
        }
        verify(exactly = 1) { clientCallback.invoke(error) }
        verify(exactly = 1) {
            logger.logW(
                "Route refresh on-demand error: request is skipped as a newer one is available",
                "RouteRefreshController",
            )
        }
    }

    @Test
    fun routesRefreshFinishedWithCancellation() = coroutineRule.runBlockingTest {
        coEvery {
            routeRefresherExecutor.executeRoutesRefresh(any(), any())
        } coAnswers {
            suspendCancellableCoroutine {}
        }

        sut.requestRoutesRefresh(routes, clientCallback)

        sut.cancel()

        verify(exactly = 0) {
            attemptListener.onRoutesRefreshAttemptFinished(any())
            stateHolder.onFailure(any())
            stateHolder.onSuccess()
            listener.onRoutesRefreshed(any())
            clientCallback.invoke(any())
        }
        verify(exactly = 1) {
            stateHolder.onCancel()
        }
    }

    @Test
    fun runJobAfterCancel() = coroutineRule.runBlockingTest {
        coEvery {
            routeRefresherExecutor.executeRoutesRefresh(any(), any())
        } coAnswers {
            suspendCancellableCoroutine {}
        }

        sut.requestRoutesRefresh(routes, clientCallback)

        sut.cancel()
        clearAllMocks(answers = false)

        coEvery {
            routeRefresherExecutor.executeRoutesRefresh(any(), any())
        } returns RoutesRefresherExecutorResult.ReplacedByNewer
        sut.requestRoutesRefresh(routes, clientCallback)

        coVerify(exactly = 1) {
            routeRefresherExecutor.executeRoutesRefresh(routes, any())
        }
    }

    @Test
    fun cancelWithNoActiveJobs() = coroutineRule.runBlockingTest {
        sut.cancel()

        verify(exactly = 0) { stateHolder.onCancel() }
    }

    private fun interceptStartCallback(): () -> Unit {
        val callbacks = mutableListOf<() -> Unit>()
        coVerify { routeRefresherExecutor.executeRoutesRefresh(any(), capture(callbacks)) }
        return callbacks.last()
    }
}
