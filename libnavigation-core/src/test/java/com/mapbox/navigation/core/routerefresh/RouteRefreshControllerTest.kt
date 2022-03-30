package com.mapbox.navigation.core.routerefresh

import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.common.Logger
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterRefreshCallback
import com.mapbox.navigation.base.route.RouteRefreshOptions
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.testing.MockLoggerRule
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigation.utils.internal.logI
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private const val routeDiff1 = "Updated distance, duration, speed, congestion at leg 1"
private const val routeDiff2 = "Updated duration, speed, maxSpeed, congestion at leg 3"

@ExperimentalCoroutinesApi
class RouteRefreshControllerTest {

    @get:Rule
    val mockLoggerTestRule = MockLoggerRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val routeRefreshCallbackSlot = slot<NavigationRouterRefreshCallback>()
    private val directionsSession: DirectionsSession = mockk(relaxUnitFun = true) {
        every { routes } returns listOf(mockk())
    }
    private val tripSession: TripSession = mockk()
    private val routeDiffProvider = mockk<DirectionsRouteDiffProvider>()
    private val routeOptions: RouteOptions = mockk {
        every {
            coordinatesList()
        } returns listOf(Point.fromLngLat(0.0, 0.0), Point.fromLngLat(1.1, 1.1))
    }
    private val validRoute: NavigationRoute = mockk {
        every { routeOptions } returns this@RouteRefreshControllerTest.routeOptions
        every { directionsResponse.uuid() } returns "test_uuid"
        every { directionsRoute.legs() } returns null
    }

    private val routeRefreshOptions = RouteRefreshOptions.Builder().build()
    private val threadController = ThreadController()

    private val routeRefreshController = RouteRefreshController(
        routeRefreshOptions,
        directionsSession,
        tripSession,
        threadController,
        routeDiffProvider,
    )

    private val requestId = 1L

    @Before
    fun setup() {
        mockkStatic("com.mapbox.navigation.base.extensions.RouteOptionsExtensions")
        every { tripSession.getRouteProgress() } returns mockk {
            every { currentLegProgress } returns mockk {
                every { legIndex } returns 0
            }
        }
        every {
            directionsSession.requestRouteRefresh(any(), any(), capture(routeRefreshCallbackSlot))
        } returns requestId
    }

    @Test
    fun `should refresh route every 5 minutes by default`() = coroutineRule.runBlockingTest {
        every { routeOptions.enableRefresh() } returns true

        routeRefreshController.restart(validRoute) {}
        coroutineRule.testDispatcher.advanceTimeBy(TimeUnit.MINUTES.toMillis(15))
        routeRefreshController.stop()

        verify(exactly = 3) { directionsSession.requestRouteRefresh(any(), any(), any()) }
    }

    @Test
    fun `should refresh route according to options`() = coroutineRule.runBlockingTest {
        val routeRefreshController = RouteRefreshController(
            RouteRefreshOptions.Builder()
                .intervalMillis(TimeUnit.MINUTES.toMillis(1))
                .build(),
            directionsSession,
            tripSession,
            threadController,
        )
        every { routeOptions.enableRefresh() } returns true

        routeRefreshController.restart(validRoute) {}
        coroutineRule.testDispatcher.advanceTimeBy(TimeUnit.MINUTES.toMillis(15))
        routeRefreshController.stop()

        verify(exactly = 15) { directionsSession.requestRouteRefresh(any(), any(), any()) }
    }

    @Test
    fun `should refresh route with correct properties`() = coroutineRule.runBlockingTest {
        every { routeOptions.enableRefresh() } returns true

        routeRefreshController.restart(validRoute) {}
        coroutineRule.testDispatcher.advanceTimeBy(TimeUnit.MINUTES.toMillis(6))
        routeRefreshController.stop()

        verify(exactly = 1) { directionsSession.requestRouteRefresh(any(), any(), any()) }
    }

    @Test
    fun `should refresh route with any annotation`() =
        coroutineRule.runBlockingTest {
            every { routeOptions.enableRefresh() } returns true

            routeRefreshController.restart(validRoute) {}
            coroutineRule.testDispatcher.advanceTimeBy(TimeUnit.MINUTES.toMillis(6))
            routeRefreshController.stop()

            verify(exactly = 1) { directionsSession.requestRouteRefresh(any(), any(), any()) }
        }

    @Test
    fun `should log warning when route is not supported`() = coroutineRule.runBlockingTest {
        every { routeOptions.enableRefresh() } returns true
        every { validRoute.directionsResponse.uuid() } returns null

        routeRefreshController.restart(validRoute) {}
        coroutineRule.testDispatcher.advanceTimeBy(TimeUnit.MINUTES.toMillis(6))
        routeRefreshController.stop()

        verify(exactly = 0) { directionsSession.requestRouteRefresh(any(), any(), any()) }
        verify(exactly = 1) {
            Logger.w(any(), any())
        }
    }

    @Test
    fun `cancel request when stopped (nothing started)`() {
        routeRefreshController.stop()

        verify(exactly = 0) { directionsSession.cancelRouteRefreshRequest(any()) }
    }

    @Test
    fun `cancel request when stopped`() = coroutineRule.runBlockingTest {
        every { routeOptions.enableRefresh() } returns true

        routeRefreshController.restart(validRoute) {}
        coroutineRule.testDispatcher.advanceTimeBy(TimeUnit.MINUTES.toMillis(6))
        routeRefreshController.stop()

        verify(exactly = 1) { directionsSession.cancelRouteRefreshRequest(requestId) }
    }

    @Test
    fun `do not send a request when uuid is empty`() {
        every { routeOptions.enableRefresh() } returns true
        every { validRoute.directionsResponse.uuid() } returns ""

        routeRefreshController.restart(validRoute) {}
        coroutineRule.testDispatcher.advanceTimeBy(routeRefreshOptions.intervalMillis * 2)
        routeRefreshController.stop()

        verify(exactly = 0) { directionsSession.requestRouteRefresh(any(), any(), any()) }
    }

    @Test
    fun `should cancel the previous request before starting a new one`() {
        every { routeOptions.enableRefresh() } returns true

        // Create 2 requests.
        //   Let the first one create a requestId equal to 1.
        //   Cancel during the second call, and then we expect the first request to be canceled.
        val countDownLatch = CountDownLatch(2)
        every { directionsSession.requestRouteRefresh(any(), any(), any()) } answers {
            if (countDownLatch.count == 1L) {
                routeRefreshController.restart(validRoute) {}
            }
            countDownLatch.countDown()
            countDownLatch.count
        }

        routeRefreshController.restart(validRoute) {}
        coroutineRule.testDispatcher.advanceTimeBy(routeRefreshOptions.intervalMillis * 2)
        countDownLatch.await()
        routeRefreshController.stop()

        verify(exactly = 2) { directionsSession.requestRouteRefresh(any(), any(), any()) }
        verify { directionsSession.cancelRouteRefreshRequest(1) }
    }

    @Test
    fun `restart should cancel outgoing requests and restart the refresh interval`() {
        every { routeOptions.enableRefresh() } returns true

        // We're expecting 2 requests in this test. The interruptions will
        // happen in between the requests.
        val countDownLatch = CountDownLatch(2)
        every { directionsSession.requestRouteRefresh(any(), any(), any()) } answers {
            countDownLatch.countDown()
            countDownLatch.count
        }

        routeRefreshController.restart(validRoute) {}
        coroutineRule.testDispatcher.advanceTimeBy(
            (routeRefreshOptions.intervalMillis * 1.9).toLong()
        )
        routeRefreshController.restart(validRoute) {}
        coroutineRule.testDispatcher.advanceTimeBy(
            (routeRefreshOptions.intervalMillis * 1.9).toLong()
        )
        countDownLatch.await()

        // If the timer did not restart, the count would be 3.
        verify(exactly = 2) { directionsSession.requestRouteRefresh(any(), any(), any()) }
        verify { directionsSession.cancelRouteRefreshRequest(1) }

        // Clean up coroutines
        routeRefreshController.stop()
    }

    @Test
    fun `clear the request when there is a successful response`() {
        every { routeOptions.enableRefresh() } returns true
        every { routeDiffProvider.buildRouteDiffs(validRoute, any(), 0) } returns emptyList()

        val callback = mockk<(List<NavigationRoute>) -> Unit>(relaxed = true)
        routeRefreshController.restart(validRoute, callback)
        coroutineRule.testDispatcher.advanceTimeBy(routeRefreshOptions.intervalMillis)
        val navRoute = mockk<NavigationRoute> {
            every { directionsRoute } returns mockk {
                every { legs() } returns null
            }
        }
        routeRefreshCallbackSlot.captured.onRefreshReady(navRoute)
        routeRefreshController.stop()

        verify(exactly = 1) { callback(listOf(navRoute)) }
        verify(exactly = 1) { directionsSession.requestRouteRefresh(any(), any(), any()) }
        verify(exactly = 0) { directionsSession.cancelRouteRefreshRequest(any()) }
    }

    @Test
    fun `clear the request when there is a failure response`() {
        every { routeOptions.enableRefresh() } returns true

        val callback = mockk<(List<NavigationRoute>) -> Unit>()
        routeRefreshController.restart(validRoute, callback)
        coroutineRule.testDispatcher.advanceTimeBy(routeRefreshOptions.intervalMillis)
        routeRefreshCallbackSlot.captured.onFailure(
            mockk {
                every { message } returns "test error"
                every { throwable } returns mockk()
            }
        )
        routeRefreshController.stop()

        verify(exactly = 0) { callback(any()) }
        verify(exactly = 1) { directionsSession.requestRouteRefresh(any(), any(), any()) }
        verify(exactly = 0) { directionsSession.cancelRouteRefreshRequest(any()) }
    }

    @Test
    fun `should log route diffs when there is a successful response`() {
        val newRoute = mockk<NavigationRoute>()
        val routeDiffs = listOf(routeDiff1, routeDiff2)

        every { routeOptions.enableRefresh() } returns true
        every { routeDiffProvider.buildRouteDiffs(validRoute, newRoute, 0) } returns routeDiffs

        routeRefreshController.restart(validRoute) {}
        coroutineRule.testDispatcher.advanceTimeBy(routeRefreshOptions.intervalMillis)
        routeRefreshCallbackSlot.captured.onRefreshReady(newRoute)
        routeRefreshController.stop()

        verify(exactly = 1) {
            logI(routeDiff1, RouteRefreshController.LOG_CATEGORY)
            logI(routeDiff2, RouteRefreshController.LOG_CATEGORY)
        }
    }

    @Test
    fun `should log message when there is a successful response without route diffs`() {
        val newRoute = mockk<NavigationRoute>()

        every { routeOptions.enableRefresh() } returns true
        every { routeDiffProvider.buildRouteDiffs(validRoute, newRoute, 0) } returns emptyList()

        routeRefreshController.restart(validRoute) {}
        coroutineRule.testDispatcher.advanceTimeBy(routeRefreshOptions.intervalMillis)
        routeRefreshCallbackSlot.captured.onRefreshReady(newRoute)
        routeRefreshController.stop()

        verify(exactly = 1) {
            logI("No changes to route annotations", RouteRefreshController.LOG_CATEGORY)
        }
    }

    @After
    fun tearDown() {
        unmockkStatic("com.mapbox.navigation.base.extensions.RouteOptionsExtensions")
    }
}
