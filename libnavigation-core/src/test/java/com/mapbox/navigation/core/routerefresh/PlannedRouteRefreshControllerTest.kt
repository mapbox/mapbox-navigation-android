package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouteRefreshOptions
import com.mapbox.navigation.core.RouteProgressData
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.utils.internal.LoggerFrontend
import io.mockk.Called
import io.mockk.clearAllMocks
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class PlannedRouteRefreshControllerTest {

    private val logger = mockk<LoggerFrontend>(relaxed = true)

    @get:Rule
    val loggerRule = LoggingFrontendTestRule(logger)

    private val executor = mockk<RouteRefresherExecutor>(relaxed = true)
    private val stateHolder = mockk<RouteRefreshStateHolder>(relaxed = true)
    private val listener = mockk<RouteRefresherListener>(relaxed = true)
    private val cancellableHandler = mockk<CancellableHandler>(relaxed = true)
    private val retryStrategy = mockk<RetryRouteRefreshStrategy>(relaxed = true)
    private val interval = 40000L
    private val routeRefreshOptions = RouteRefreshOptions.Builder().intervalMillis(interval).build()
    private val sut = PlannedRouteRefreshController(
        executor,
        routeRefreshOptions,
        stateHolder,
        listener,
        cancellableHandler,
        retryStrategy
    )

    @Before
    fun setUp() {
        mockkObject(RouteRefreshValidator)
    }

    @After
    fun tearDown() {
        unmockkObject(RouteRefreshValidator)
    }

    @Test
    fun startRoutesRefreshing_emptyRoutes() {
        sut.startRoutesRefreshing(emptyList())

        verify(exactly = 1) {
            cancellableHandler.cancelAll()
            stateHolder.reset()
            logger.logI("Routes are empty", "RouteRefreshController")
        }
        verify(exactly = 0) {
            stateHolder.onFailure(any())
            RouteRefreshValidator.validateRoute(any())
            cancellableHandler.postDelayed(any(), any(), any())
        }
    }

    @Test
    fun startRoutesRefreshing_allRoutesAreInvalid() {
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
            stateHolder.onFailure(expectedLogMessage)
            stateHolder.reset()
        }
        verify(exactly = 0) { cancellableHandler.postDelayed(any(), any(), any()) }
    }

    @Test
    fun startRoutesRefreshing_someRoutesAreInvalid() {
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
            cancellableHandler.postDelayed(interval, any(), any())
        }
    }

    @Test
    fun startRoutesRefreshing_allRoutesAreValid() {
        val route1 = mockk<NavigationRoute>(relaxed = true)
        val route2 = mockk<NavigationRoute>(relaxed = true)
        val routes = listOf(route1, route2)
        every {
            RouteRefreshValidator.validateRoute(any())
        } returns RouteRefreshValidator.RouteValidationResult.Valid

        sut.startRoutesRefreshing(routes)

        verify(exactly = 1) {
            retryStrategy.reset()
            cancellableHandler.postDelayed(interval, any(), any())
        }
    }

    @Test
    fun startRoutesRefreshing_resetsRetryStrategy() {
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
    fun startRoutesRefreshing_postsCancellableTask() {
        val route1 = mockk<NavigationRoute>(relaxed = true)
        val route2 = mockk<NavigationRoute>(relaxed = true)
        val routes = listOf(route1, route2)
        every {
            RouteRefreshValidator.validateRoute(any())
        } returns RouteRefreshValidator.RouteValidationResult.Valid

        sut.startRoutesRefreshing(routes)

        val attemptBlocks = mutableListOf<Runnable>()
        val cancellableBlocks = mutableListOf<() -> Unit>()
        verify(exactly = 1) {
            cancellableHandler.postDelayed(
                interval,
                capture(attemptBlocks),
                capture(cancellableBlocks)
            )
        }
        cancellableBlocks.last().invoke()
        verify { stateHolder.onCancel() }
        attemptBlocks.last().run()
        verify(exactly = 1) { executor.postRoutesToRefresh(routes, any()) }
    }

    @Test
    fun startRoutesRefreshing_notifiesOnStart() {
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
    fun finishRequestIncrementsAttempt() {
        val route1 = mockk<NavigationRoute>(relaxed = true)
        val route2 = mockk<NavigationRoute>(relaxed = true)
        val routes = listOf(route1, route2)
        every {
            RouteRefreshValidator.validateRoute(any())
        } returns RouteRefreshValidator.RouteValidationResult.Valid

        sut.startRoutesRefreshing(routes)
        finishRequest(
            RouteRefresherResult(
                false,
                listOf(route1, route2),
                RouteProgressData(1, 2, 3)
            )
        )

        verify(exactly = 1) { retryStrategy.onNextAttempt() }
    }

    @Test
    fun finishRequestSuccessfully() {
        val route1 = mockk<NavigationRoute>(relaxed = true)
        val route2 = mockk<NavigationRoute>(relaxed = true)
        val routes = listOf(route1, route2)
        every {
            RouteRefreshValidator.validateRoute(any())
        } returns RouteRefreshValidator.RouteValidationResult.Valid

        sut.startRoutesRefreshing(routes)
        val result = RouteRefresherResult(true, listOf(route1, route2), RouteProgressData(1, 2, 3))
        finishRequest(result)

        verify(exactly = 1) {
            stateHolder.onSuccess()
            listener.onRoutesRefreshed(result)
        }
        verify { cancellableHandler wasNot Called }
    }

    @Test
    fun finishRequestUnsuccessfullyShouldRetry() {
        val route1 = mockk<NavigationRoute>(relaxed = true)
        val route2 = mockk<NavigationRoute>(relaxed = true)
        val routes = listOf(route1, route2)
        every {
            RouteRefreshValidator.validateRoute(any())
        } returns RouteRefreshValidator.RouteValidationResult.Valid
        every { retryStrategy.shouldRetry() } returns true

        sut.startRoutesRefreshing(routes)
        clearMocks(retryStrategy, answers = false)
        val result = RouteRefresherResult(false, listOf(route1, route2), RouteProgressData(1, 2, 3))
        finishRequest(result)

        verify(exactly = 1) {
            cancellableHandler.postDelayed(interval, any(), any())
        }
        verify(exactly = 0) {
            stateHolder.onFailure(any())
            listener.onRoutesRefreshed(any())
            retryStrategy.reset()
        }
    }

    @Test
    fun finishRequestUnsuccessfullyShouldRetryDoesNotNotifyOnStart() {
        val route1 = mockk<NavigationRoute>(relaxed = true)
        val route2 = mockk<NavigationRoute>(relaxed = true)
        val routes = listOf(route1, route2)
        every {
            RouteRefreshValidator.validateRoute(any())
        } returns RouteRefreshValidator.RouteValidationResult.Valid
        every { retryStrategy.shouldRetry() } returns true

        sut.startRoutesRefreshing(routes)
        val result = RouteRefresherResult(false, listOf(route1, route2), RouteProgressData(1, 2, 3))
        finishRequest(result)
        startRequest()

        verify(exactly = 0) { stateHolder.onStarted() }
    }

    @Test
    fun finishRequestUnsuccessfullyShouldNotRetryRoutesDidNotChange() {
        val route1 = mockk<NavigationRoute>(relaxed = true)
        val route2 = mockk<NavigationRoute>(relaxed = true)
        val routes = listOf(route1, route2)
        every {
            RouteRefreshValidator.validateRoute(any())
        } returns RouteRefreshValidator.RouteValidationResult.Valid
        every { retryStrategy.shouldRetry() } returns false

        sut.startRoutesRefreshing(routes)
        clearMocks(retryStrategy, answers = false)
        val result = RouteRefresherResult(false, listOf(route1, route2), RouteProgressData(1, 2, 3))
        finishRequest(result)

        verifyOrder {
            stateHolder.onFailure(null)
            retryStrategy.reset()
            cancellableHandler.postDelayed(interval, any(), any())
        }
        verify(exactly = 0) {
            listener.onRoutesRefreshed(any())
        }
    }

    @Test
    fun finishRequestUnsuccessfullyShouldNotRetryRoutesDidNotChangeShouldNotifyOnStart() {
        val route1 = mockk<NavigationRoute>(relaxed = true)
        val route2 = mockk<NavigationRoute>(relaxed = true)
        val routes = listOf(route1, route2)
        every {
            RouteRefreshValidator.validateRoute(any())
        } returns RouteRefreshValidator.RouteValidationResult.Valid
        every { retryStrategy.shouldRetry() } returns false

        sut.startRoutesRefreshing(routes)
        val result = RouteRefresherResult(false, listOf(route1, route2), RouteProgressData(1, 2, 3))
        finishRequest(result)
        startRequest()

        verify(exactly = 1) { stateHolder.onStarted() }
    }

    @Test
    fun finishRequestUnsuccessfullyShouldNotRetryRoutesChanged() {
        val route1 = mockk<NavigationRoute>(relaxed = true)
        val route2 = mockk<NavigationRoute>(relaxed = true)
        val newRoute1 = mockk<NavigationRoute>(relaxed = true)
        val newRoute2 = mockk<NavigationRoute>(relaxed = true)
        val routes = listOf(route1, route2)
        every {
            RouteRefreshValidator.validateRoute(any())
        } returns RouteRefreshValidator.RouteValidationResult.Valid
        every { retryStrategy.shouldRetry() } returns false

        sut.startRoutesRefreshing(routes)
        val result = RouteRefresherResult(
            false,
            listOf(newRoute1, newRoute2),
            RouteProgressData(1, 2, 3)
        )
        finishRequest(result)

        verify(exactly = 1) {
            stateHolder.onFailure(null)
            listener.onRoutesRefreshed(result)
        }
        verify(exactly = 0) {
            cancellableHandler.postDelayed(any(), any(), any())
        }
    }

    private fun startRequest() {
        val attemptBlocks = mutableListOf<Runnable>()
        verify(exactly = 1) {
            cancellableHandler.postDelayed(
                any(),
                capture(attemptBlocks),
                any()
            )
        }
        attemptBlocks.last().run()
        val progressCallbacks = mutableListOf<RouteRefresherProgressCallback>()
        verify(exactly = 1) { executor.postRoutesToRefresh(any(), capture(progressCallbacks)) }
        progressCallbacks.last().onStarted()
    }

    private fun finishRequest(result: RouteRefresherResult) {
        val attemptBlocks = mutableListOf<Runnable>()
        verify(exactly = 1) {
            cancellableHandler.postDelayed(
                any(),
                capture(attemptBlocks),
                any()
            )
        }
        clearMocks(cancellableHandler, answers = false)
        attemptBlocks.last().run()
        val progressCallbacks = mutableListOf<RouteRefresherProgressCallback>()
        verify(exactly = 1) { executor.postRoutesToRefresh(any(), capture(progressCallbacks)) }
        clearMocks(executor, answers = false)
        progressCallbacks.last().onResult(result)
    }

    @Test
    fun pauseNotPaused() {
        sut.pause()

        verify(exactly = 1) { cancellableHandler.cancelAll() }
    }

    @Test
    fun pausePaused() {
        sut.pause()
        clearAllMocks(answers = false)

        sut.pause()

        verify(exactly = 0) { cancellableHandler.cancelAll() }
    }

    @Test
    fun pauseResumed() {
        sut.pause()
        sut.resume()
        clearAllMocks(answers = false)

        sut.pause()

        verify(exactly = 1) { cancellableHandler.cancelAll() }
    }

    @Test
    fun resumePausedNoRoutes() {
        sut.pause()
        clearAllMocks(answers = false)

        sut.resume()

        verify(exactly = 0) {
            retryStrategy.shouldRetry()
            cancellableHandler.postDelayed(any(), any(), any())
        }
    }

    @Test
    fun resumePausedHasRoutesShouldNotRetry() {
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

        verify(exactly = 0) { cancellableHandler.postDelayed(any(), any(), any()) }
    }

    @Test
    fun resumePausedHasRoutesShouldRetry() {
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

        verify(exactly = 1) { cancellableHandler.postDelayed(interval, any(), any()) }
    }

    @Test
    fun resumeNotPausedHasRoutesShouldRetry() {
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

        verify(exactly = 0) { cancellableHandler.postDelayed(any(), any(), any()) }
    }

    @Test
    fun resumeResumedHasRoutesShouldRetry() {
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

        verify(exactly = 0) { cancellableHandler.postDelayed(any(), any(), any()) }
    }

    @Test
    fun resumePausedHasRoutesShouldRetryNotifiesOnStart() {
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
    fun emptyRoutesAreNotRemembered() {
        sut.startRoutesRefreshing(emptyList())
        sut.pause()
        clearAllMocks(answers = false)

        sut.resume()

        verify(exactly = 0) {
            retryStrategy.shouldRetry()
            cancellableHandler.postDelayed(any(), any(), any())
        }
    }

    @Test
    fun invalidRoutesAreNotRemembered() {
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
            cancellableHandler.postDelayed(any(), any(), any())
        }
    }

    @Test
    fun partiallyInvalidRoutesAreRemembered() {
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

        val attemptBlocks = mutableListOf<Runnable>()
        verify(exactly = 1) {
            cancellableHandler.postDelayed(interval, capture(attemptBlocks), any())
        }
        attemptBlocks.last().run()
        verify(exactly = 1) { executor.postRoutesToRefresh(routes, any()) }
    }

    @Test
    fun validRoutesAreRemembered() {
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

        val attemptBlocks = mutableListOf<Runnable>()
        verify(exactly = 1) {
            cancellableHandler.postDelayed(interval, capture(attemptBlocks), any())
        }
        attemptBlocks.last().run()
        verify(exactly = 1) { executor.postRoutesToRefresh(routes, any()) }
    }

    @Test
    fun emptyRoutesResetOldValidRoutes() {
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
            cancellableHandler.postDelayed(any(), any(), any())
        }
    }

    @Test
    fun invalidRoutesResetOldValidRoutes() {
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
            cancellableHandler.postDelayed(any(), any(), any())
        }
    }

    @Test
    fun partiallyValidRoutesResetOldValidRoutes() {
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

        val attemptBlocks = mutableListOf<Runnable>()
        verify(exactly = 1) {
            cancellableHandler.postDelayed(interval, capture(attemptBlocks), any())
        }
        attemptBlocks.last().run()
        verify(exactly = 1) { executor.postRoutesToRefresh(listOf(route3, route4), any()) }
    }

    @Test
    fun validRoutesResetOldValidRoutes() {
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

        val attemptBlocks = mutableListOf<Runnable>()
        verify(exactly = 1) {
            cancellableHandler.postDelayed(interval, capture(attemptBlocks), any())
        }
        attemptBlocks.last().run()
        verify(exactly = 1) { executor.postRoutesToRefresh(listOf(route3, route4), any()) }
    }
}
