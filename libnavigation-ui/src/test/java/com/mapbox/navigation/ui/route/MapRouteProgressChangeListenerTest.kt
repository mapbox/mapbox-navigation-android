package com.mapbox.navigation.ui.route

import android.os.Build
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.mapboxsdk.style.expressions.Expression
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.internal.utils.RouteLineValueAnimator
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ Build.VERSION_CODES.M])
class MapRouteProgressChangeListenerTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private val routeLine: MapRouteLine = mockk(relaxUnitFun = true)
    private val routeArrow: MapRouteArrow = mockk(relaxUnitFun = true)
    private val animator: RouteLineValueAnimator = RouteLineValueAnimator()

    private val drawDirections = mutableListOf<DirectionsRoute>()
    private val addRouteProgress = mutableListOf<RouteProgress>()
    private val routeListSlot = slot<List<DirectionsRoute>>()

    private val progressChangeListener by lazy {
        MapRouteProgressChangeListener(routeLine, routeArrow, null)
    }

    @Before
    fun setup() {
        every { routeLine.retrieveDirectionsRoutes() } returns emptyList()
        every { routeLine.draw(capture(drawDirections)) } returns Unit
        every { routeLine.reinitializeWithRoutes(capture(routeListSlot)) } returns Unit
        every { routeLine.vanishPointOffset } returns 0f
        every { routeArrow.addUpcomingManeuverArrow(capture(addRouteProgress)) } returns Unit
        every { routeArrow.routeArrowIsVisible() } returns true
    }

    @Test
    fun `should not draw route without geometry`() {
        val newRoute: DirectionsRoute = mockk {
            every { geometry() } returns null
        }
        val routeProgress: RouteProgress = mockk {
            every { route } returns newRoute
        }
        every { routeLine.getPrimaryRoute() } returns newRoute

        progressChangeListener.onRouteProgressChanged(routeProgress)

        verify(exactly = 0) { routeLine.draw(any<DirectionsRoute>()) }
        verify(exactly = 0) { routeLine.draw(any<List<DirectionsRoute>>()) }
    }

    @Test
    fun `should not draw route without directions route`() {
        val routeWithNoGeometry = DirectionsRoute.builder().build()
        every { routeLine.getPrimaryRoute() } returns null
        val routeProgress: RouteProgress = mockk {
            every { route } returns routeWithNoGeometry
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
            every { route } returns mockk {
                every { geometry() } returns "y{v|bA{}diiGOuDpBiMhM{k@~Syj@bLuZlEiM"
            }
        }
        val newRoute: DirectionsRoute = mockk {
            every { geometry() } returns null
        }
        every { routeLine.getPrimaryRoute() } returns newRoute

        progressChangeListener.onRouteProgressChanged(routeProgress)

        verify(exactly = 1) { routeLine.reinitializeWithRoutes(any<List<DirectionsRoute>>()) }
    }

    @Test
    fun `should cancel animator when route progress has geometry`() {
        val animator = mockk<RouteLineValueAnimator>(relaxUnitFun = true)
        val routeProgressChangeListener = MapRouteProgressChangeListener(routeLine, routeArrow, animator)

        every { routeLine.retrieveDirectionsRoutes() } returns listOf(
            mockk {
                every { geometry() } returns null
            }
        )
        val routeProgress: RouteProgress = mockk {
            every { route } returns mockk {
                every { geometry() } returns "y{v|bA{}diiGOuDpBiMhM{k@~Syj@bLuZlEiM"
            }
        }
        val newRoute: DirectionsRoute = mockk {
            every { geometry() } returns null
        }
        every { routeLine.getPrimaryRoute() } returns newRoute

        routeProgressChangeListener.onRouteProgressChanged(routeProgress)

        verify(exactly = 1) { animator.cancelAnimationCallbacks() }
    }

    @Test
    fun `should only add maneuver arrow when visible`() {
        val routes = listOf(
            mockk<DirectionsRoute> {
                every { geometry() } returns "y{v|bA{}diiGOuDpBiMhM{k@~Syj@bLuZlEiM"
            }
        )
        every { routeLine.getPrimaryRoute() } returns routes[0]
        every { routeLine.retrieveDirectionsRoutes() } returns routes
        val routeProgress: RouteProgress = mockk {
            every { route } returns routes[0]
        }

        progressChangeListener.onRouteProgressChanged(routeProgress)

        verify(exactly = 1) { routeArrow.addUpcomingManeuverArrow(routeProgress) }
    }

    @Test
    fun `should not add maneuver arrow when not visible`() {
        every { routeArrow.routeArrowIsVisible() } returns false
        val routes = listOf(
            mockk<DirectionsRoute> {
                every { geometry() } returns "y{v|bA{}diiGOuDpBiMhM{k@~Syj@bLuZlEiM"
            }
        )
        every { routeLine.getPrimaryRoute() } returns routes[0]
        every { routeLine.retrieveDirectionsRoutes() } returns routes
        val routeProgress: RouteProgress = mockk {
            every { route } returns mockk {
                every { geometry() } returns "{au|bAqtiiiG|TnI`B\\dEzAl_@hMxGxB"
            }
        }

        progressChangeListener.onRouteProgressChanged(routeProgress)

        verify(exactly = 0) { routeArrow.addUpcomingManeuverArrow(routeProgress) }
    }

    @Test
    fun `should draw new routes`() {
        every { routeLine.retrieveDirectionsRoutes() } returns listOf(
            mockk {
                every { geometry() } returns "y{v|bA{}diiGOuDpBiMhM{k@~Syj@bLuZlEiM"
            }
        )
        val routeProgress: RouteProgress = mockk {
            every { route } returns mockk {
                every { geometry() } returns "{au|bAqtiiiG|TnI`B\\dEzAl_@hMxGxB"
            }
        }
        every { routeLine.getPrimaryRoute() } returns routeLine.retrieveDirectionsRoutes()[0]

        progressChangeListener.onRouteProgressChanged(routeProgress)

        verify(exactly = 1) { routeLine.reinitializeWithRoutes(any<List<DirectionsRoute>>()) }
        assertEquals(1, routeListSlot.captured.size)
        assertEquals("{au|bAqtiiiG|TnI`B\\dEzAl_@hMxGxB", routeListSlot.captured[0].geometry())
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
            every { route } returns mockk {
                every { geometry() } returns "y{v|bA{}diiGOuDpBiMhM{k@~Syj@bLuZlEiM"
                every { distance() } returns 100.0
            }
        }
        every { routeLine.getPrimaryRoute() } returns routeLine.retrieveDirectionsRoutes()[0]

        progressChangeListener.onRouteProgressChanged(routeProgress)

        verify(exactly = 1) { routeLine.reinitializeWithRoutes(any<List<DirectionsRoute>>()) }
        assertEquals(1, routeListSlot.captured.size)
        assertEquals(100.0, routeListSlot.captured[0].distance()!!, 0.001)
    }

    @Test
    fun `does not decorate route line when route progress route does not have geometry`() {
        val expression: Expression = mockk()
        val progressChangeListener = MapRouteProgressChangeListener(routeLine, routeArrow, animator)
        val routes = listOf(
            mockk<DirectionsRoute> {
                every { geometry() } returns "y{v|bA{}diiGOuDpBiMhM{k@~Syj@bLuZlEiM"
            }
        )
        val routeProgress: RouteProgress = mockk {
            every { route } returns mockk {
                every { geometry() } returns ""
                every { distanceRemaining } returns 1000f
                every { distanceTraveled } returns 500f
            }
        }
        every { routeLine.getPrimaryRoute() } returns routeProgress.route
        every { routeLine.retrieveDirectionsRoutes() } returns routes
        every { routeLine.getExpressionAtOffset(any()) } returns expression

        coroutineRule.runBlockingTest {
            progressChangeListener.onRouteProgressChanged(routeProgress)
        }

        verify(exactly = 0) { routeLine.decorateRouteLine(expression) }
    }

    @Test
    fun `hides shield line at offset when route progress route has geometry`() {
        val expression: Expression = mockk()
        val progressChangeListener = MapRouteProgressChangeListener(routeLine, routeArrow, animator)
        val routes = listOf(
            mockk<DirectionsRoute> {
                every { geometry() } returns "y{v|bA{}diiGOuDpBiMhM{k@~Syj@bLuZlEiM"
            }
        )
        val routeProgress: RouteProgress = mockk {
            every { route } returns mockk {
                every { geometry() } returns "{au|bAqtiiiG|TnI`B\\dEzAl_@hMxGxB"
                every { distanceRemaining } returns 1000f
                every { distanceTraveled } returns 500f
            }
        }
        every { routeLine.getPrimaryRoute() } returns routeProgress.route
        every { routeLine.retrieveDirectionsRoutes() } returns routes
        every { routeLine.getExpressionAtOffset(any()) } returns expression

        coroutineRule.runBlockingTest {
            progressChangeListener.onRouteProgressChanged(routeProgress)
        }

        verify { routeLine.hideShieldLineAtOffset(any()) }
    }

    @Test
    fun `does not hide shield line at offset when route progress route does not have geometry`() {
        val expression: Expression = mockk()
        val progressChangeListener = MapRouteProgressChangeListener(routeLine, routeArrow, animator)
        val routes = listOf(
            mockk<DirectionsRoute> {
                every { geometry() } returns "y{v|bA{}diiGOuDpBiMhM{k@~Syj@bLuZlEiM"
            }
        )
        val routeProgress: RouteProgress = mockk {
            every { route } returns mockk {
                every { geometry() } returns ""
                every { distanceRemaining } returns 1000f
                every { distanceTraveled } returns 500f
            }
        }
        every { routeLine.getPrimaryRoute() } returns routeProgress.route
        every { routeLine.retrieveDirectionsRoutes() } returns routes
        every { routeLine.getExpressionAtOffset(any()) } returns expression

        coroutineRule.runBlockingTest {
            progressChangeListener.onRouteProgressChanged(routeProgress)
        }

        verify(exactly = 0) { routeLine.hideShieldLineAtOffset(any()) }
    }

    @Test
    fun `calls addUpcomingManeuverArrow when on progress update`() = coroutineRule.runBlockingTest {
        val expression: Expression = mockk()
        val progressChangeListener = MapRouteProgressChangeListener(routeLine, routeArrow, animator)
        val routes = listOf(
            mockk<DirectionsRoute> {
                every { geometry() } returns "y{v|bA{}diiGOuDpBiMhM{k@~Syj@bLuZlEiM"
            }
        )
        val routeProgress: RouteProgress = mockk {
            every { route } returns mockk {
                every { geometry() } returns "{au|bAqtiiiG|TnI`B\\dEzAl_@hMxGxB"
                every { distanceRemaining } returns 1000f
                every { distanceTraveled } returns 500f
            }
        }
        every { routeLine.getPrimaryRoute() } returns routeProgress.route
        every { routeLine.retrieveDirectionsRoutes() } returns routes
        every { routeLine.getExpressionAtOffset(any()) } returns expression

        progressChangeListener.onRouteProgressChanged(routeProgress)

        verify { routeArrow.addUpcomingManeuverArrow(routeProgress) }
    }

    @Test
    fun `calls reinitializePrimaryRoute function`() {
        val expression: Expression = mockk()
        val progressChangeListener = MapRouteProgressChangeListener(routeLine, routeArrow, animator)
        val routes = listOf(
            mockk<DirectionsRoute> {
                every { geometry() } returns "y{v|bA{}diiGOuDpBiMhM{k@~Syj@bLuZlEiM"
            }
        )
        val routeProgress: RouteProgress = mockk {
            every { route } returns mockk {
                every { geometry() } returns "{au|bAqtiiiG|TnI`B\\dEzAl_@hMxGxB"
                every { distanceRemaining } returns 1000f
                every { distanceTraveled } returns 500f
            }
        }
        every { routeLine.getPrimaryRoute() } returns routes[0] andThen routeProgress.route
        every { routeLine.retrieveDirectionsRoutes() } returns routes
        every { routeLine.getExpressionAtOffset(any()) } returns expression

        progressChangeListener.onRouteProgressChanged(routeProgress)
        coroutineRule.runBlockingTest {
            progressChangeListener.onRouteProgressChanged(routeProgress)
        }

        verify { routeLine.reinitializePrimaryRoute() }
    }
}
