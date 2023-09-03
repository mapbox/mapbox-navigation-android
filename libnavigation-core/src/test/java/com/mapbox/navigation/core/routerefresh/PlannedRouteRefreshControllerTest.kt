package com.mapbox.navigation.core.routerefresh

import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.internal.utils.CoroutineUtils
import com.mapbox.navigation.core.utils.Delayer
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.utils.internal.LoggerFrontend
import io.mockk.clearAllMocks
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
class PlannedRouteRefreshControllerTest {

    private val logger = mockk<LoggerFrontend>(relaxed = true)

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @get:Rule
    val loggerRule = LoggingFrontendTestRule(logger)

    private val executor = mockk<RouteRefresherExecutor>(relaxed = true)
    private val stateHolder = mockk<RouteRefreshStateHolder>(relaxed = true)
    private val attemptListener = mockk<RoutesRefreshAttemptListener>(relaxed = true)
    private val listener = mockk<RouteRefresherListener>(relaxed = true)
    private val retryStrategy = mockk<RetryRouteRefreshStrategy>(relaxed = true)
    private val interval = 40000L
    private val childScopeDispatcher = TestCoroutineDispatcher()
    private var childScope: CoroutineScope? = null
    private val parentScope = coroutineRule.createTestScope()
    private val delayer = spyk(Delayer(interval))
    private lateinit var sut: PlannedRouteRefreshController

    @Before
    fun setUp() {
        mockkObject(RouteRefreshValidator)
        mockkObject(CoroutineUtils)
        every { CoroutineUtils.createChildScope(parentScope) } answers {
            TestCoroutineScope(SupervisorJob() + childScopeDispatcher).also { childScope = it }
        }
        sut = PlannedRouteRefreshController(
            executor,
            delayer,
            stateHolder,
            listener,
            attemptListener,
            parentScope,
            retryStrategy
        )
    }

    @After
    fun tearDown() {
        parentScope.cancel()
        unmockkObject(RouteRefreshValidator)
        unmockkObject(CoroutineUtils)
    }

    @Test
    fun startRoutesRefreshing_emptyRoutes() = coroutineRule.runBlockingTest {
        sut.startRoutesRefreshing(emptyList())

        verify(exactly = 1) {
            stateHolder.reset()
            logger.logI("Routes are empty, nothing to refresh", "RouteRefreshController")
        }
        verify(exactly = 0) {
            stateHolder.onFailure(any())
            RouteRefreshValidator.validateRoute(any())
        }
        childScopeDispatcher.advanceTimeBy(interval)
        coVerify(exactly = 0) {
            executor.executeRoutesRefresh(any(), any())
        }
        assertNull(sut.routesToRefresh)
    }

    @Test
    fun startRoutesRefreshing_routesChangedCompletely() = coroutineRule.runBlockingTest {
        every {
            RouteRefreshValidator.validateRoute(any())
        } returns RouteRefreshValidator.RouteValidationResult.Valid
        val routes1 = listOf<NavigationRoute>(
            mockk { every { id } returns "id#0" },
            mockk { every { id } returns "id#1" },
        )
        val routes2 = listOf<NavigationRoute>(
            mockk { every { id } returns "id#2" },
            mockk { every { id } returns "id#3" },
        )
        sut.startRoutesRefreshing(routes1)
        clearAllMocks(answers = false)
        sut.startRoutesRefreshing(routes2)

        verify(exactly = 0) {
            stateHolder.reset()
            stateHolder.onFailure(any())
        }
        childScopeDispatcher.advanceTimeBy(interval)
        coVerify(exactly = 1) {
            stateHolder.onCancel()
            delayer.delay()
            executor.executeRoutesRefresh(routes2, any())
        }
        coVerify(exactly = 0) {
            delayer.resumeDelay()
        }
        assertEquals(routes2, sut.routesToRefresh)
    }

    @Test
    fun startRoutesRefreshing_routesChangedPartially() = coroutineRule.runBlockingTest {
        every {
            RouteRefreshValidator.validateRoute(any())
        } returns RouteRefreshValidator.RouteValidationResult.Valid
        val routes1 = listOf<NavigationRoute>(
            mockk { every { id } returns "id#0" },
            mockk { every { id } returns "id#1" },
        )
        val routes2 = listOf<NavigationRoute>(
            mockk { every { id } returns "id#1" },
            mockk { every { id } returns "id#3" },
        )
        sut.startRoutesRefreshing(routes1)
        clearAllMocks(answers = false)
        sut.startRoutesRefreshing(routes2)

        verify(exactly = 0) {
            stateHolder.reset()
            stateHolder.onFailure(any())
        }
        childScopeDispatcher.advanceTimeBy(interval)
        coVerify(exactly = 1) {
            stateHolder.onCancel()
            delayer.resumeDelay()
            executor.executeRoutesRefresh(routes2, any())
        }
        coVerify(exactly = 0) {
            delayer.delay()
        }
        assertEquals(routes2, sut.routesToRefresh)
    }

    @Test
    fun startRoutesRefreshing_allRoutesAreInvalid() = coroutineRule.runBlockingTest {
        val route1 = mockk<NavigationRoute>(relaxed = true)
        val route2 = mockk<NavigationRoute>(relaxed = true)
        val validation1 = RouteRefreshValidator.RouteValidationResult.Invalid("some reason 1")
        val validation2 = RouteRefreshValidator.RouteValidationResult.Invalid("some reason 2")
        val message = "some message"
        val expectedLogMessage = "No routes which could be refreshed. $message"
        every {
            RouteRefreshValidator.validateRoute(route1)
        } returns validation1
        every {
            RouteRefreshValidator.validateRoute(route2)
        } returns validation2
        every {
            RouteRefreshValidator.joinValidationErrorMessages(
                listOf(validation1 to route1, validation2 to route2)
            )
        } returns message

        sut.startRoutesRefreshing(listOf(route1, route2))

        verify(exactly = 1) {
            logger.logI(expectedLogMessage, "RouteRefreshController")
        }
        verifyOrder {
            stateHolder.onStarted()
            stateHolder.onFailure(expectedLogMessage)
            stateHolder.reset()
        }
        childScopeDispatcher.advanceTimeBy(interval)
        coVerify(exactly = 0) {
            executor.executeRoutesRefresh(any(), any())
        }
        assertNull(sut.routesToRefresh)
    }

    @Test
    fun startRoutesRefreshing_someRoutesAreInvalid() = coroutineRule.runBlockingTest {
        val route1 = mockk<NavigationRoute>(relaxed = true)
        val route2 = mockk<NavigationRoute>(relaxed = true)
        val routes = listOf(route1, route2)
        val validation1 = RouteRefreshValidator.RouteValidationResult.Invalid("some reason 1")
        every {
            RouteRefreshValidator.validateRoute(route1)
        } returns validation1
        every {
            RouteRefreshValidator.validateRoute(route2)
        } returns RouteRefreshValidator.RouteValidationResult.Valid

        sut.startRoutesRefreshing(routes)

        verify(exactly = 1) {
            retryStrategy.reset()
        }
        childScopeDispatcher.advanceTimeBy(interval)
        coVerify(exactly = 1) {
            delayer.delay()
            executor.executeRoutesRefresh(any(), any())
        }
        coVerify(exactly = 0) {
            delayer.resumeDelay()
        }
        assertEquals(routes, sut.routesToRefresh)
    }

    @Test
    fun startRoutesRefreshing_allRoutesAreValid() = coroutineRule.runBlockingTest {
        val route1 = mockk<NavigationRoute>(relaxed = true)
        val route2 = mockk<NavigationRoute>(relaxed = true)
        val routes = listOf(route1, route2)
        every {
            RouteRefreshValidator.validateRoute(any())
        } returns RouteRefreshValidator.RouteValidationResult.Valid

        sut.startRoutesRefreshing(routes)

        verify(exactly = 1) {
            retryStrategy.reset()
        }
        childScopeDispatcher.advanceTimeBy(interval)
        coVerify(exactly = 1) {
            delayer.delay()
            executor.executeRoutesRefresh(any(), any())
        }
        coVerify(exactly = 0) {
            delayer.resumeDelay()
        }
        assertEquals(routes, sut.routesToRefresh)
    }

    @Test
    fun startRoutesRefreshing_resetsRetryStrategy() = coroutineRule.runBlockingTest {
        val route1 = mockk<NavigationRoute>(relaxed = true)
        val route2 = mockk<NavigationRoute>(relaxed = true)
        val routes = listOf(route1, route2)
        every {
            RouteRefreshValidator.validateRoute(any())
        } returns RouteRefreshValidator.RouteValidationResult.Valid

        sut.startRoutesRefreshing(routes)

        verify(exactly = 1) { retryStrategy.reset() }
    }

    @Test
    fun startRoutesRefreshing_postsCancellableTask() = coroutineRule.runBlockingTest {
        coEvery { delayer.delay() } coAnswers { suspendCancellableCoroutine {} }
        val route1 = mockk<NavigationRoute>(relaxed = true)
        val route2 = mockk<NavigationRoute>(relaxed = true)
        val routes = listOf(route1, route2)
        every {
            RouteRefreshValidator.validateRoute(any())
        } returns RouteRefreshValidator.RouteValidationResult.Valid

        sut.startRoutesRefreshing(routes)

        childScope?.cancel()
        verify { stateHolder.onCancel() }
    }

    @Test
    fun startRoutesRefreshing_notifiesOnStart() = coroutineRule.runBlockingTest {
        val route1 = mockk<NavigationRoute>(relaxed = true)
        val route2 = mockk<NavigationRoute>(relaxed = true)
        val routes = listOf(route1, route2)
        every {
            RouteRefreshValidator.validateRoute(any())
        } returns RouteRefreshValidator.RouteValidationResult.Valid

        sut.startRoutesRefreshing(routes)

        startRequest()
        verify(exactly = 1) {
            stateHolder.onStarted()
        }
    }

    @Test
    fun retryIncrementsAttempt() = coroutineRule.runBlockingTest {
        val route1 = mockk<NavigationRoute>(relaxed = true)
        val route2 = mockk<NavigationRoute>(relaxed = true)
        val routes = listOf(route1, route2)
        every {
            RouteRefreshValidator.validateRoute(any())
        } returns RouteRefreshValidator.RouteValidationResult.Valid
        every { retryStrategy.shouldRetry() } returns true

        sut.startRoutesRefreshing(routes)
        finishRequest(
            mockk<RoutesRefresherResult> {
                every { anySuccess() } returns false
                every { anyRequestFailed() } returns true
            }
        )

        startRequest()
        verify(exactly = 1) { retryStrategy.onNextAttempt() }
    }

    @Test
    fun finishRequestSuccessfully() = coroutineRule.runBlockingTest {
        val route1 = mockk<NavigationRoute>(relaxed = true)
        val route2 = mockk<NavigationRoute>(relaxed = true)
        val refreshedRoute1 = mockk<NavigationRoute>(relaxed = true)
        val refreshedRoute2 = mockk<NavigationRoute>(relaxed = true)
        val routes = listOf(route1, route2)
        every {
            RouteRefreshValidator.validateRoute(any())
        } returns RouteRefreshValidator.RouteValidationResult.Valid

        sut.startRoutesRefreshing(routes)
        val result = mockk<RoutesRefresherResult> {
            every { anySuccess() } returns true
            every { primaryRouteRefresherResult } returns mockk {
                every { route } returns refreshedRoute1
            }
            every { alternativesRouteRefresherResults } returns listOf(
                mockk {
                    every { route } returns refreshedRoute2
                }
            )
        }
        finishRequest(result)

        verify(exactly = 1) {
            attemptListener.onRoutesRefreshAttemptFinished(result)
            stateHolder.onSuccess()
            listener.onRoutesRefreshed(result)
        }
        childScopeDispatcher.advanceTimeBy(interval)
        coVerify(exactly = 1) {
            executor.executeRoutesRefresh(listOf(refreshedRoute1, refreshedRoute2), any())
        }
        coVerify(exactly = 0) {
            delayer.resumeDelay()
        }
        assertEquals(listOf(refreshedRoute1, refreshedRoute2), sut.routesToRefresh)
    }

    @Test
    fun finishRequestUnsuccessfullyHasFailedRequestShouldRetry() = coroutineRule.runBlockingTest {
        val route1 = mockk<NavigationRoute>(relaxed = true)
        val route2 = mockk<NavigationRoute>(relaxed = true)
        val routes = listOf(route1, route2)
        every {
            RouteRefreshValidator.validateRoute(any())
        } returns RouteRefreshValidator.RouteValidationResult.Valid
        every { retryStrategy.shouldRetry() } returns true

        sut.startRoutesRefreshing(routes)
        clearAllMocks(answers = false)
        val result = mockk<RoutesRefresherResult> {
            every { anySuccess() } returns false
            every { anyRequestFailed() } returns true
        }
        finishRequest(result)
        verify(exactly = 1) {
            attemptListener.onRoutesRefreshAttemptFinished(result)
        }

        childScopeDispatcher.advanceTimeBy(interval)
        coVerify(exactly = 1) {
            executor.executeRoutesRefresh(any(), any())
        }
        coVerify(exactly = 0) {
            delayer.resumeDelay()
        }
        verify(exactly = 0) {
            stateHolder.onFailure(any())
            listener.onRoutesRefreshed(any())
            retryStrategy.reset()
        }
    }

    @Test
    fun finishRequestUnsuccessfullyNoFailedRequestsShouldRetry() = coroutineRule.runBlockingTest {
        val route1 = mockk<NavigationRoute>(relaxed = true)
        val route2 = mockk<NavigationRoute>(relaxed = true)
        val routes = listOf(route1, route2)
        every {
            RouteRefreshValidator.validateRoute(any())
        } returns RouteRefreshValidator.RouteValidationResult.Valid
        every { retryStrategy.shouldRetry() } returns true

        sut.startRoutesRefreshing(routes)
        clearAllMocks(answers = false)
        val result = mockk<RoutesRefresherResult> {
            every { anySuccess() } returns false
            every { anyRequestFailed() } returns false
        }
        finishRequest(result)
        verifyOrder {
            stateHolder.onFailure(null)
            retryStrategy.reset()
        }
        verify(exactly = 1) {
            attemptListener.onRoutesRefreshAttemptFinished(result)
            listener.onRoutesRefreshed(any())
        }

        childScopeDispatcher.advanceTimeBy(interval)
        coVerify(exactly = 1) {
            executor.executeRoutesRefresh(any(), any())
        }
        coVerify(exactly = 0) {
            delayer.resumeDelay()
        }
    }

    @Test
    fun finishRequestUnsuccessfullyShouldRetryDoesNotNotifyOnStart() =
        coroutineRule.runBlockingTest {
            val route1 = mockk<NavigationRoute>(relaxed = true)
            val route2 = mockk<NavigationRoute>(relaxed = true)
            val routes = listOf(route1, route2)
            every {
                RouteRefreshValidator.validateRoute(any())
            } returns RouteRefreshValidator.RouteValidationResult.Valid
            every { retryStrategy.shouldRetry() } returns true

            sut.startRoutesRefreshing(routes)
            val result = mockk<RoutesRefresherResult> {
                every { anySuccess() } returns false
                every { anyRequestFailed() } returns true
            }
            finishRequest(result)
            startRequest()

            verify(exactly = 0) { stateHolder.onStarted() }
        }

    @Test
    fun finishRequestUnsuccessfullyShouldNotRetry() = coroutineRule.runBlockingTest {
        val route1 = mockk<NavigationRoute>(relaxed = true)
        val route2 = mockk<NavigationRoute>(relaxed = true)
        val routes = listOf(route1, route2)
        every {
            RouteRefreshValidator.validateRoute(any())
        } returns RouteRefreshValidator.RouteValidationResult.Valid
        every { retryStrategy.shouldRetry() } returns false

        sut.startRoutesRefreshing(routes)
        clearAllMocks(answers = false)
        val result = mockk<RoutesRefresherResult> {
            every { anySuccess() } returns false
            every { anyRequestFailed() } returns true
        }
        finishRequest(result)

        verifyOrder {
            stateHolder.onFailure(null)
            retryStrategy.reset()
        }
        verify(exactly = 1) {
            attemptListener.onRoutesRefreshAttemptFinished(result)
            listener.onRoutesRefreshed(any())
        }
        childScopeDispatcher.advanceTimeBy(interval)
        coVerify(exactly = 1) {
            executor.executeRoutesRefresh(any(), any())
        }
        coVerify(exactly = 0) {
            delayer.resumeDelay()
        }
    }

    @Test
    fun finishRequestUnsuccessfullyShouldNotRetryShouldNotifyOnStart() =
        coroutineRule.runBlockingTest {
            val route1 = mockk<NavigationRoute>(relaxed = true)
            val route2 = mockk<NavigationRoute>(relaxed = true)
            val routes = listOf(route1, route2)
            every {
                RouteRefreshValidator.validateRoute(any())
            } returns RouteRefreshValidator.RouteValidationResult.Valid
            every { retryStrategy.shouldRetry() } returns false

            sut.startRoutesRefreshing(routes)
            val result = mockk<RoutesRefresherResult> {
                every { anySuccess() } returns false
                every { anyRequestFailed() } returns true
            }
            finishRequest(result)
            startRequest()

            verify(exactly = 1) { stateHolder.onStarted() }
        }

    @Test
    fun finishRequestWithErrorIsIgnored() = coroutineRule.runBlockingTest {
        val route1 = mockk<NavigationRoute>(relaxed = true)
        val route2 = mockk<NavigationRoute>(relaxed = true)
        val routes = listOf(route1, route2)
        every {
            RouteRefreshValidator.validateRoute(any())
        } returns RouteRefreshValidator.RouteValidationResult.Valid
        every { retryStrategy.shouldRetry() } returns false

        sut.startRoutesRefreshing(routes)
        clearMocks(retryStrategy, answers = false)
        finishRequest(ExpectedFactory.createError("Some error"))

        verify(exactly = 0) {
            attemptListener.onRoutesRefreshAttemptFinished(any())
            stateHolder.onSuccess()
            stateHolder.onFailure(any())
            listener.onRoutesRefreshed(any())
            retryStrategy.shouldRetry()
            retryStrategy.reset()
        }
        childScopeDispatcher.advanceTimeBy(interval)
        coVerify(exactly = 0) {
            executor.executeRoutesRefresh(any(), any())
        }
        verify(exactly = 1) {
            logger.logW("Planned route refresh error: Some error", "RouteRefreshController")
        }
    }

    private suspend fun startRequest() {
        childScopeDispatcher.advanceTimeBy(interval)
        val startCallbacks = mutableListOf<() -> Unit>()
        coVerify(exactly = 1) { executor.executeRoutesRefresh(any(), capture(startCallbacks)) }
        startCallbacks.last().invoke()
    }

    private suspend fun finishRequest(result: RoutesRefresherResult) {
        finishRequest(ExpectedFactory.createValue(result))
    }

    private suspend fun finishRequest(result: Expected<String, RoutesRefresherResult>) {
        coEvery {
            executor.executeRoutesRefresh(any(), any())
        } returns result
        childScopeDispatcher.advanceTimeBy(interval)
        coVerify(exactly = 1) {
            executor.executeRoutesRefresh(any(), any())
        }
        clearMocks(executor, answers = false)
    }

    @Test
    fun pauseNotPaused() = coroutineRule.runBlockingTest {
        every {
            RouteRefreshValidator.validateRoute(any())
        } returns RouteRefreshValidator.RouteValidationResult.Valid
        sut.startRoutesRefreshing(listOf(mockk(relaxed = true)))

        sut.pause()

        verify(exactly = 1) { stateHolder.onCancel() }
    }

    @Test
    fun pausePaused() = coroutineRule.runBlockingTest {
        every {
            RouteRefreshValidator.validateRoute(any())
        } returns RouteRefreshValidator.RouteValidationResult.Valid
        every { retryStrategy.shouldRetry() } returns true
        sut.startRoutesRefreshing(listOf(mockk(relaxed = true)))
        sut.pause()
        clearAllMocks(answers = false)

        sut.pause()

        verify(exactly = 0) { stateHolder.onCancel() }
    }

    @Test
    fun pauseResumed() = coroutineRule.runBlockingTest {
        every {
            RouteRefreshValidator.validateRoute(any())
        } returns RouteRefreshValidator.RouteValidationResult.Valid
        every { retryStrategy.shouldRetry() } returns true
        sut.startRoutesRefreshing(listOf(mockk(relaxed = true)))
        sut.pause()
        sut.resume()
        clearAllMocks(answers = false)

        sut.pause()

        verify(exactly = 1) { stateHolder.onCancel() }
    }

    @Test
    fun resumePausedNullRoutes() = coroutineRule.runBlockingTest {
        sut.pause()
        clearAllMocks(answers = false)

        sut.resume()

        verify(exactly = 0) {
            retryStrategy.shouldRetry()
        }
        childScopeDispatcher.advanceTimeBy(interval)
        coVerify(exactly = 0) {
            executor.executeRoutesRefresh(any(), any())
        }
    }

    @Test
    fun resumePausedHasRoutesShouldNotRetry() = coroutineRule.runBlockingTest {
        val route1 = mockk<NavigationRoute>(relaxed = true)
        val route2 = mockk<NavigationRoute>(relaxed = true)
        val routes = listOf(route1, route2)
        every {
            RouteRefreshValidator.validateRoute(any())
        } returns RouteRefreshValidator.RouteValidationResult.Valid
        sut.startRoutesRefreshing(routes)
        sut.pause()
        clearAllMocks(answers = false)
        every { retryStrategy.shouldRetry() } returns false

        sut.resume()

        childScopeDispatcher.advanceTimeBy(interval)
        coVerify(exactly = 0) {
            executor.executeRoutesRefresh(any(), any())
        }
    }

    @Test
    fun resumePausedHasRoutesShouldRetry() = coroutineRule.runBlockingTest {
        val route1 = mockk<NavigationRoute>(relaxed = true)
        val route2 = mockk<NavigationRoute>(relaxed = true)
        val routes = listOf(route1, route2)
        every {
            RouteRefreshValidator.validateRoute(any())
        } returns RouteRefreshValidator.RouteValidationResult.Valid
        sut.startRoutesRefreshing(routes)
        sut.pause()
        clearAllMocks(answers = false)
        every { retryStrategy.shouldRetry() } returns true

        sut.resume()

        childScopeDispatcher.advanceTimeBy(interval)
        coVerify(exactly = 1) {
            executor.executeRoutesRefresh(any(), any())
        }
    }

    @Test
    fun resumeNotPausedHasRoutesShouldRetry() = coroutineRule.runBlockingTest {
        val route1 = mockk<NavigationRoute>(relaxed = true)
        val route2 = mockk<NavigationRoute>(relaxed = true)
        val routes = listOf(route1, route2)
        every {
            RouteRefreshValidator.validateRoute(any())
        } returns RouteRefreshValidator.RouteValidationResult.Valid
        sut.startRoutesRefreshing(routes)
        clearAllMocks(answers = false)
        every { retryStrategy.shouldRetry() } returns true

        sut.resume()

        verify(exactly = 0) { retryStrategy.shouldRetry() }
    }

    @Test
    fun resumeResumedHasRoutesShouldRetry() = coroutineRule.runBlockingTest {
        val route1 = mockk<NavigationRoute>(relaxed = true)
        val route2 = mockk<NavigationRoute>(relaxed = true)
        val routes = listOf(route1, route2)
        every {
            RouteRefreshValidator.validateRoute(any())
        } returns RouteRefreshValidator.RouteValidationResult.Valid
        sut.startRoutesRefreshing(routes)
        sut.pause()
        sut.resume()
        clearAllMocks(answers = false)
        every { retryStrategy.shouldRetry() } returns true

        sut.resume()

        verify(exactly = 0) { retryStrategy.shouldRetry() }
    }

    @Test
    fun resumePausedHasRoutesShouldRetryNotifiesOnStart() = coroutineRule.runBlockingTest {
        val route1 = mockk<NavigationRoute>(relaxed = true)
        val route2 = mockk<NavigationRoute>(relaxed = true)
        val routes = listOf(route1, route2)
        every {
            RouteRefreshValidator.validateRoute(any())
        } returns RouteRefreshValidator.RouteValidationResult.Valid
        sut.startRoutesRefreshing(routes)
        sut.pause()
        clearAllMocks(answers = false)
        every { retryStrategy.shouldRetry() } returns true

        sut.resume()
        startRequest()

        verify(exactly = 1) { stateHolder.onStarted() }
    }

    @Test
    fun emptyRoutesAreNotRemembered() = coroutineRule.runBlockingTest {
        sut.startRoutesRefreshing(emptyList())
        sut.pause()
        clearAllMocks(answers = false)

        sut.resume()

        verify(exactly = 0) {
            retryStrategy.shouldRetry()
        }
        childScopeDispatcher.advanceTimeBy(interval)
        coVerify(exactly = 0) {
            executor.executeRoutesRefresh(any(), any())
        }
    }

    @Test
    fun invalidRoutesAreNotRemembered() = coroutineRule.runBlockingTest {
        val route1 = mockk<NavigationRoute>(relaxed = true)
        val route2 = mockk<NavigationRoute>(relaxed = true)
        val validation1 = RouteRefreshValidator.RouteValidationResult.Invalid("some reason 1")
        val validation2 = RouteRefreshValidator.RouteValidationResult.Invalid("some reason 2")
        val message = "some message"
        every {
            RouteRefreshValidator.validateRoute(route1)
        } returns validation1
        every {
            RouteRefreshValidator.validateRoute(route2)
        } returns validation2
        every {
            RouteRefreshValidator.joinValidationErrorMessages(
                listOf(validation1 to route1, validation2 to route2)
            )
        } returns message
        sut.startRoutesRefreshing(listOf(route1, route2))
        sut.pause()
        clearAllMocks(answers = false)

        sut.resume()

        verify(exactly = 0) {
            retryStrategy.shouldRetry()
        }
        childScopeDispatcher.advanceTimeBy(interval)
        coVerify(exactly = 0) {
            executor.executeRoutesRefresh(any(), any())
        }
    }

    @Test
    fun partiallyInvalidRoutesAreRemembered() = coroutineRule.runBlockingTest {
        val route1 = mockk<NavigationRoute>(relaxed = true)
        val route2 = mockk<NavigationRoute>(relaxed = true)
        val routes = listOf(route1, route2)
        val validation1 = RouteRefreshValidator.RouteValidationResult.Invalid("some reason 1")
        every {
            RouteRefreshValidator.validateRoute(route1)
        } returns validation1
        every {
            RouteRefreshValidator.validateRoute(route2)
        } returns RouteRefreshValidator.RouteValidationResult.Valid
        sut.startRoutesRefreshing(routes)
        sut.pause()
        clearAllMocks(answers = false)
        every { retryStrategy.shouldRetry() } returns true

        sut.resume()

        childScopeDispatcher.advanceTimeBy(interval)
        coVerify(exactly = 1) { executor.executeRoutesRefresh(routes, any()) }
    }

    @Test
    fun validRoutesAreRemembered() = coroutineRule.runBlockingTest {
        val route1 = mockk<NavigationRoute>(relaxed = true)
        val route2 = mockk<NavigationRoute>(relaxed = true)
        val routes = listOf(route1, route2)
        every {
            RouteRefreshValidator.validateRoute(any())
        } returns RouteRefreshValidator.RouteValidationResult.Valid
        sut.startRoutesRefreshing(routes)
        sut.pause()
        clearAllMocks(answers = false)
        every { retryStrategy.shouldRetry() } returns true

        sut.resume()

        childScopeDispatcher.advanceTimeBy(interval)
        coVerify(exactly = 1) { executor.executeRoutesRefresh(routes, any()) }
    }

    @Test
    fun emptyRoutesResetOldValidRoutes() = coroutineRule.runBlockingTest {
        val route1 = mockk<NavigationRoute>(relaxed = true)
        val route2 = mockk<NavigationRoute>(relaxed = true)
        val routes = listOf(route1, route2)
        every {
            RouteRefreshValidator.validateRoute(any())
        } returns RouteRefreshValidator.RouteValidationResult.Valid
        sut.startRoutesRefreshing(routes)
        sut.startRoutesRefreshing(emptyList())
        sut.pause()
        clearAllMocks(answers = false)

        sut.resume()

        verify(exactly = 0) {
            retryStrategy.shouldRetry()
        }
        childScopeDispatcher.advanceTimeBy(interval)
        coVerify(exactly = 0) {
            executor.executeRoutesRefresh(any(), any())
        }
        assertNull(sut.routesToRefresh)
    }

    @Test
    fun invalidRoutesResetOldValidRoutes() = coroutineRule.runBlockingTest {
        val route1 = mockk<NavigationRoute>(relaxed = true)
        val route2 = mockk<NavigationRoute>(relaxed = true)
        every {
            RouteRefreshValidator.validateRoute(route1)
        } returns RouteRefreshValidator.RouteValidationResult.Valid
        every {
            RouteRefreshValidator.validateRoute(route2)
        } returns RouteRefreshValidator.RouteValidationResult.Invalid("")
        sut.startRoutesRefreshing(listOf(route1))
        sut.startRoutesRefreshing(listOf(route2))
        sut.pause()
        clearAllMocks(answers = false)

        sut.resume()

        verify(exactly = 0) {
            retryStrategy.shouldRetry()
        }
        childScopeDispatcher.advanceTimeBy(interval)
        coVerify(exactly = 0) {
            executor.executeRoutesRefresh(any(), any())
        }
        assertNull(sut.routesToRefresh)
    }

    @Test
    fun partiallyValidRoutesResetOldValidRoutes() = coroutineRule.runBlockingTest {
        val route1 = mockk<NavigationRoute>(relaxed = true)
        val route2 = mockk<NavigationRoute>(relaxed = true)
        val route3 = mockk<NavigationRoute>(relaxed = true)
        val route4 = mockk<NavigationRoute>(relaxed = true)
        every {
            RouteRefreshValidator.validateRoute(route1)
        } returns RouteRefreshValidator.RouteValidationResult.Valid
        every {
            RouteRefreshValidator.validateRoute(route2)
        } returns RouteRefreshValidator.RouteValidationResult.Valid
        every {
            RouteRefreshValidator.validateRoute(route3)
        } returns RouteRefreshValidator.RouteValidationResult.Invalid("")
        every {
            RouteRefreshValidator.validateRoute(route4)
        } returns RouteRefreshValidator.RouteValidationResult.Valid
        sut.startRoutesRefreshing(listOf(route1, route2))
        sut.startRoutesRefreshing(listOf(route3, route4))
        sut.pause()
        clearAllMocks(answers = false)
        every { retryStrategy.shouldRetry() } returns true

        sut.resume()

        childScopeDispatcher.advanceTimeBy(interval)
        coVerify(exactly = 1) { executor.executeRoutesRefresh(listOf(route3, route4), any()) }
        assertEquals(listOf(route3, route4), sut.routesToRefresh)
    }

    @Test
    fun validRoutesResetOldValidRoutes() = coroutineRule.runBlockingTest {
        val route1 = mockk<NavigationRoute>(relaxed = true)
        val route2 = mockk<NavigationRoute>(relaxed = true)
        val route3 = mockk<NavigationRoute>(relaxed = true)
        val route4 = mockk<NavigationRoute>(relaxed = true)
        every {
            RouteRefreshValidator.validateRoute(any())
        } returns RouteRefreshValidator.RouteValidationResult.Valid
        sut.startRoutesRefreshing(listOf(route1, route2))
        sut.startRoutesRefreshing(listOf(route3, route4))
        sut.pause()
        clearAllMocks(answers = false)
        every { retryStrategy.shouldRetry() } returns true

        sut.resume()

        childScopeDispatcher.advanceTimeBy(interval)
        coVerify(exactly = 1) { executor.executeRoutesRefresh(listOf(route3, route4), any()) }
        assertEquals(listOf(route3, route4), sut.routesToRefresh)
    }
}
