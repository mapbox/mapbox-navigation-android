package com.mapbox.navigation.core.routerefresh

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.base.common.logger.Logger
import com.mapbox.navigation.base.extensions.supportsRouteRefresh
import com.mapbox.navigation.base.route.RouteRefreshOptions
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit

@ExperimentalCoroutinesApi
class RouteRefreshControllerTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val directionsSession: DirectionsSession = mockk(relaxUnitFun = true)
    private val tripSession: TripSession = mockk()
    private val logger: Logger = mockk {
        every { w(any(), any()) } just Runs
    }
    private val routeOptions: RouteOptions = mockk {
        every { profile() } returns DirectionsCriteria.PROFILE_DRIVING_TRAFFIC
        every { overview() } returns DirectionsCriteria.OVERVIEW_FULL
        every { annotationsList() } returns listOf(DirectionsCriteria.ANNOTATION_MAXSPEED)
        every { requestUuid() } returns "sadsad1212c"
    }
    private val validRoute: DirectionsRoute = mockk {
        every { routeOptions() } returns routeOptions
    }

    private val routeRefreshController = RouteRefreshController(
        RouteRefreshOptions.Builder().build(),
        directionsSession,
        tripSession,
        logger
    )

    private val requestId = 1L

    @Before
    fun setup() {
        mockkStatic("com.mapbox.navigation.base.extensions.RouteOptionsEx")
        every { tripSession.getRouteProgress() } returns mockk {
            every { currentLegProgress } returns mockk {
                every { legIndex } returns 0
            }
        }
        every { tripSession.route } returns validRoute
        every { directionsSession.requestRouteRefresh(any(), any(), any()) } returns requestId
    }

    @Test
    fun `should refresh route every 5 minutes by default`() = coroutineRule.runBlockingTest {
        every { routeOptions.supportsRouteRefresh() } returns true

        routeRefreshController.start()
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
            logger
        )
        every { routeOptions.supportsRouteRefresh() } returns true

        routeRefreshController.start()
        coroutineRule.testDispatcher.advanceTimeBy(TimeUnit.MINUTES.toMillis(15))
        routeRefreshController.stop()

        verify(exactly = 15) { directionsSession.requestRouteRefresh(any(), any(), any()) }
    }

    @Test
    fun `should refresh route with correct properties`() = coroutineRule.runBlockingTest {
        every { routeOptions.supportsRouteRefresh() } returns true

        routeRefreshController.start()
        coroutineRule.testDispatcher.advanceTimeBy(TimeUnit.MINUTES.toMillis(6))
        routeRefreshController.stop()

        verify(exactly = 1) { directionsSession.requestRouteRefresh(any(), any(), any()) }
    }

    @Test
    fun `should refresh route with any annotation`() =
        coroutineRule.runBlockingTest {
            every { routeOptions.supportsRouteRefresh() } returns true

            routeRefreshController.start()
            coroutineRule.testDispatcher.advanceTimeBy(TimeUnit.MINUTES.toMillis(6))
            routeRefreshController.stop()

            verify(exactly = 1) { directionsSession.requestRouteRefresh(any(), any(), any()) }
        }

    @Test
    fun `should log warning when route is not supported`() = coroutineRule.runBlockingTest {
        every { routeOptions.supportsRouteRefresh() } returns false

        routeRefreshController.start()
        coroutineRule.testDispatcher.advanceTimeBy(TimeUnit.MINUTES.toMillis(6))
        routeRefreshController.stop()

        verify(exactly = 0) { directionsSession.requestRouteRefresh(any(), any(), any()) }
        verify(exactly = 1) {
            logger.w(RouteRefreshController.TAG, any())
        }
    }

    @Test
    fun `cancel request when stopped (nothing started)`() {
        routeRefreshController.stop()

        verify(exactly = 0) { directionsSession.cancelRouteRefreshRequest(any()) }
    }

    @Test
    fun `cancel request when stopped`() = coroutineRule.runBlockingTest {
        every { routeOptions.supportsRouteRefresh() } returns true

        routeRefreshController.start()
        coroutineRule.testDispatcher.advanceTimeBy(TimeUnit.MINUTES.toMillis(6))
        routeRefreshController.stop()

        verify(exactly = 1) { directionsSession.cancelRouteRefreshRequest(requestId) }
    }

    @After
    fun tearDown() {
        unmockkStatic("com.mapbox.navigation.base.extensions.RouteOptionsEx")
    }
}
