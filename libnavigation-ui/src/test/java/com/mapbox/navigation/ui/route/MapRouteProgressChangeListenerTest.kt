package com.mapbox.navigation.ui.route

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MapRouteProgressChangeListenerTest {

    private val routeLine: MapRouteLine = mockk()
    private val routeArrow: MapRouteArrow = mockk()

    private val drawDirections = mutableListOf<DirectionsRoute>()
    private val addRouteProgress = mutableListOf<RouteProgress>()

    private val progressChangeListener = MapRouteProgressChangeListener(routeLine, routeArrow)

    @Before
    fun setup() {
        every { routeLine.retrieveDirectionsRoutes() } returns emptyList()
        every { routeLine.draw(capture(drawDirections)) } returns Unit
        every { routeArrow.addUpcomingManeuverArrow(capture(addRouteProgress)) } returns Unit
    }

    @Test
    fun `should not draw route without geometry`() {
        val newRoute: DirectionsRoute = mockk {
            every { geometry() } returns null
        }
        val routeProgress: RouteProgress = mockk {
            every { route() } returns newRoute
        }
        every { routeLine.getPrimaryRoute() } returns newRoute

        progressChangeListener.onRouteProgressChanged(routeProgress)

        verify(exactly = 0) { routeLine.draw(any<DirectionsRoute>()) }
        verify(exactly = 0) { routeLine.draw(any<List<DirectionsRoute>>()) }
    }

    @Test
    fun `should not draw route without directions route`() {
        every { routeLine.getPrimaryRoute() } returns null
        val routeProgress: RouteProgress = mockk {
            every { route() } returns null
        }

        progressChangeListener.onRouteProgressChanged(routeProgress)

        verify(exactly = 0) { routeLine.draw(any<DirectionsRoute>()) }
        verify(exactly = 0) { routeLine.draw(any<List<DirectionsRoute>>()) }
    }

    @Test
    fun `should draw routes when route progress has geometry`() {
        every { routeLine.retrieveDirectionsRoutes() } returns listOf(
            mockk {
                every { geometry() } returns null
            }
        )
        val routeProgress: RouteProgress = mockk {
            every { route() } returns mockk {
                every { geometry() } returns "y{v|bA{}diiGOuDpBiMhM{k@~Syj@bLuZlEiM"
            }
        }
        val newRoute: DirectionsRoute = mockk {
            every { geometry() } returns null
        }

        every { routeLine.getPrimaryRoute() } returns newRoute

        progressChangeListener.onRouteProgressChanged(routeProgress)

        verify(exactly = 1) { routeLine.draw(any<DirectionsRoute>()) }
    }

    @Test
    fun `should only add maneuver arrow with geometry`() {
        val routes = listOf(
            mockk<DirectionsRoute> {
                every { geometry() } returns "y{v|bA{}diiGOuDpBiMhM{k@~Syj@bLuZlEiM"
            }
        )
        every { routeLine.getPrimaryRoute() } returns routes[0]
        every { routeLine.retrieveDirectionsRoutes() } returns routes
        val routeProgress: RouteProgress = mockk {
            every { route() } returns mockk {
                every { geometry() } returns "{au|bAqtiiiG|TnI`B\\dEzAl_@hMxGxB"
            }
        }

        progressChangeListener.onRouteProgressChanged(routeProgress)

        verify(exactly = 1) { routeArrow.addUpcomingManeuverArrow(routeProgress) }
    }

    @Test
    fun `should draw new routes`() {
        every { routeLine.retrieveDirectionsRoutes() } returns listOf(
            mockk {
                every { geometry() } returns "y{v|bA{}diiGOuDpBiMhM{k@~Syj@bLuZlEiM"
            }
        )
        val routeProgress: RouteProgress = mockk {
            every { route() } returns mockk {
                every { geometry() } returns "{au|bAqtiiiG|TnI`B\\dEzAl_@hMxGxB"
            }
        }
        every { routeLine.getPrimaryRoute() } returns routeLine.retrieveDirectionsRoutes()[0]

        progressChangeListener.onRouteProgressChanged(routeProgress)

        verify(exactly = 1) { routeLine.draw(any<DirectionsRoute>()) }
        assertEquals(1, drawDirections.size)
        assertEquals("{au|bAqtiiiG|TnI`B\\dEzAl_@hMxGxB", drawDirections[0].geometry())
    }

    @Test
    fun `should draw new routes when other values change`() {
        every { routeLine.retrieveDirectionsRoutes() } returns listOf(
            mockk {
                every { geometry() } returns "y{v|bA{}diiGOuDpBiMhM{k@~Syj@bLuZlEiM"
                every { distance() } returns 110.0
            }
        )
        val routeProgress: RouteProgress = mockk {
            every { route() } returns mockk {
                every { geometry() } returns "y{v|bA{}diiGOuDpBiMhM{k@~Syj@bLuZlEiM"
                every { distance() } returns 100.0
            }
        }
        every { routeLine.getPrimaryRoute() } returns routeLine.retrieveDirectionsRoutes()[0]

        progressChangeListener.onRouteProgressChanged(routeProgress)

        verify(exactly = 1) { routeLine.draw(any<DirectionsRoute>()) }
        assertEquals(1, drawDirections.size)
        assertEquals(100.0, drawDirections[0].distance()!!, 0.001)
    }
}
