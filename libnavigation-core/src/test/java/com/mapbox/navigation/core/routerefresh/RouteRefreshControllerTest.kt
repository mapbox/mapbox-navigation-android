package com.mapbox.navigation.core.routerefresh

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.base.common.logger.Logger
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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

    private val routeRefreshController = RouteRefreshController(
        directionsSession,
        tripSession,
        logger
    )

    @Before
    fun setup() {
        every { tripSession.getRouteProgress() } returns mockk {
            every { currentLegProgress } returns mockk {
                every { legIndex } returns 0
            }
        }
        every { directionsSession.requestRouteRefresh(any(), any(), any()) } returns Unit
    }

    @Test
    fun `should refresh route every 5 minutes`() = coroutineRule.runBlockingTest {
        every { tripSession.route } returns mockk {
            every { routeOptions() } returns mockk {
                every { profile() } returns DirectionsCriteria.PROFILE_DRIVING_TRAFFIC
                every { overview() } returns DirectionsCriteria.OVERVIEW_FULL
                every { annotationsList() } returns listOf(DirectionsCriteria.ANNOTATION_MAXSPEED)
            }
        }

        routeRefreshController.start()
        coroutineRule.testDispatcher.advanceTimeBy(TimeUnit.MINUTES.toMillis(15))
        routeRefreshController.stop()

        verify(exactly = 3) { directionsSession.requestRouteRefresh(any(), any(), any()) }
    }

    @Test
    fun `should refresh route with correct properties`() = coroutineRule.runBlockingTest {
        every { tripSession.route } returns mockk {
            every { routeOptions() } returns mockk {
                every { profile() } returns DirectionsCriteria.PROFILE_DRIVING_TRAFFIC
                every { overview() } returns DirectionsCriteria.OVERVIEW_FULL
                every { annotationsList() } returns listOf(DirectionsCriteria.ANNOTATION_MAXSPEED)
            }
        }

        routeRefreshController.start()
        coroutineRule.testDispatcher.advanceTimeBy(TimeUnit.MINUTES.toMillis(6))
        routeRefreshController.stop()

        verify(exactly = 1) { directionsSession.requestRouteRefresh(any(), any(), any()) }
    }

    @Test
    fun `should not refresh route without maxspeed or congestion annotation`() =
        coroutineRule.runBlockingTest {
            every { tripSession.route } returns mockk {
                every { routeOptions() } returns mockk {
                    every { profile() } returns DirectionsCriteria.PROFILE_DRIVING_TRAFFIC
                    every { overview() } returns DirectionsCriteria.OVERVIEW_FULL
                    every {
                        annotationsList()
                    } returns listOf(DirectionsCriteria.ANNOTATION_DISTANCE)
                }
            }

            routeRefreshController.start()
            coroutineRule.testDispatcher.advanceTimeBy(TimeUnit.MINUTES.toMillis(6))
            routeRefreshController.stop()

            verify(exactly = 0) { directionsSession.requestRouteRefresh(any(), any(), any()) }
        }
}
