package com.mapbox.navigation.core.routerefresh

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.base.common.logger.Logger
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.navigation.base.extensions.supportsRouteRefresh
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit

class RouteRefreshControllerTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val directionsSession: DirectionsSession = mockk()
    private val tripSession: TripSession = mockk()
    private val logger: Logger = mockk()
    private val routeOptions: RouteOptions = mockk {
        every { profile() } returns DirectionsCriteria.PROFILE_DRIVING_TRAFFIC
        every { overview() } returns DirectionsCriteria.OVERVIEW_FULL
        every { annotationsList() } returns listOf(DirectionsCriteria.ANNOTATION_MAXSPEED)
    }
    private val validRoute: DirectionsRoute = mockk {
        every { routeOptions() } returns routeOptions
    }

    private val routeRefreshController = RouteRefreshController(
        directionsSession,
        tripSession,
        logger
    )

    @Before
    fun setup() {
        mockkStatic("com.mapbox.navigation.base.extensions.DirectionsRefreshEx")
        every { tripSession.getRouteProgress() } returns mockk {
            every { currentLegProgress } returns mockk {
                every { legIndex } returns 0
            }
        }
        every { tripSession.route } returns validRoute
        every { directionsSession.requestRouteRefresh(any(), any(), any()) } returns Unit
    }

    @Test
    fun `should refresh route every 5 minutes`() = coroutineRule.runBlockingTest {
        every { routeOptions.supportsRouteRefresh() } returns true

        routeRefreshController.start()
        coroutineRule.testDispatcher.advanceTimeBy(TimeUnit.MINUTES.toMillis(15))
        routeRefreshController.stop()

        verify(exactly = 3) { directionsSession.requestRouteRefresh(any(), any(), any()) }
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
            logger.w(
                Tag("RouteRefreshController"),
                Message(
                    """
                       The route is not qualified for route refresh feature.
                       See RouteOptions?.supportsRouteRefresh() extension for details.
                    """.trimIndent()
                )
            )
        }
    }

    @After
    fun tearDown() {
        unmockkStatic("com.mapbox.navigation.base.extensions.DirectionsRefreshEx")
    }
}
