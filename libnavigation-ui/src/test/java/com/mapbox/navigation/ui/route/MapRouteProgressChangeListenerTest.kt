package com.mapbox.navigation.ui.route

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.Assert.assertEquals
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
        every { routeLine.retrievePrimaryRouteIndex() } returns 0
        every { routeLine.draw(capture(drawDirections)) } returns Unit
        every { routeArrow.addUpcomingManeuverArrow(capture(addRouteProgress)) } returns Unit
    }

    @Test
    fun `should do nothing when invisible`() {
        val routeProgress: RouteProgress = mockk()

        progressChangeListener.updateVisibility(false)
        progressChangeListener.onRouteProgressChanged(routeProgress)

        verify { routeLine wasNot Called }
        verify { routeArrow wasNot Called }
    }

    @Test
    fun `should not draw route without directions`() {
        val newRoute: DirectionsRoute = mockk {
            every { geometry() } returns null
        }
        val routeProgress: RouteProgress = mockk {
            every { route() } returns newRoute
        }

        progressChangeListener.updateVisibility(true)
        progressChangeListener.onRouteProgressChanged(routeProgress)

        verify(exactly = 0) { routeLine.draw(any<DirectionsRoute>()) }
        verify(exactly = 0) { routeLine.draw(any<List<DirectionsRoute>>()) }
    }

    @Test
    fun `should update maneuver arrow when visible`() {
        val newRoute: DirectionsRoute = mockk {
            every { geometry() } returns null
        }
        val routeProgress: RouteProgress = mockk {
            every { route() } returns newRoute
        }

        progressChangeListener.onRouteProgressChanged(routeProgress)

        verify(exactly = 1) { routeArrow.addUpcomingManeuverArrow(routeProgress) }
    }

    @Test
    fun `should only draw routes with geometry`() {
        every { routeLine.retrievePrimaryRouteIndex() } returns 0
        every { routeLine.retrieveDirectionsRoutes() } returns listOf(
            mockk {
                every { geometry() } returns "y{v|bA{}diiGOuDpBiMhM{k@~Syj@bLuZlEiM"
            }
        )
        val routeProgress: RouteProgress = mockk {
            every { route() } returns mockk {
                every { geometry() } returns null
            }
        }
        progressChangeListener.onRouteProgressChanged(routeProgress)

        verify(exactly = 0) { routeLine.draw(any<DirectionsRoute>()) }
        verify(exactly = 0) { routeLine.draw(any<List<DirectionsRoute>>()) }
    }

    @Test
    fun `should draw new routes`() {
        every { routeLine.retrievePrimaryRouteIndex() } returns 0
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

        progressChangeListener.onRouteProgressChanged(routeProgress)

        verify(exactly = 1) { routeLine.draw(any<DirectionsRoute>()) }
        assertEquals(drawDirections.size, 1)
        assertEquals(drawDirections[0].geometry(), "{au|bAqtiiiG|TnI`B\\dEzAl_@hMxGxB")
    }
}
