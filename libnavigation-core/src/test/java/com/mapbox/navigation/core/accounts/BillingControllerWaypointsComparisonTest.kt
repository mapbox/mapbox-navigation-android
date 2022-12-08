package com.mapbox.navigation.core.accounts

import com.mapbox.common.BillingServiceInterface
import com.mapbox.common.BillingSessionStatus
import com.mapbox.common.SessionSKUIdentifier
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.arrival.ArrivalProgressObserver
import com.mapbox.navigation.core.trip.session.NavigationSession
import com.mapbox.navigation.core.trip.session.NavigationSessionStateObserver
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.factories.createDirectionsRoute
import com.mapbox.navigation.testing.factories.createNavigationRoute
import com.mapbox.navigator.Waypoint
import com.mapbox.navigator.WaypointType
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class BillingControllerWaypointsComparisonTest(
    private val description: String,
    private val remainingWaypoints: Int,
    private val oldWaypoints: List<Waypoint>,
    private val newWaypoints: List<Waypoint>,
    private val shouldStartSession: Boolean,
) {

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(
                    "Route start, non EV + non EV with non-matching size",
                    2,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR)
                    ),
                    listOf(
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    true
                ),
                arrayOf(
                    "Route start, non EV + EV with non-matching size",
                    2,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR)
                    ),
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.EV_CHARGING),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    true
                ),
                arrayOf(
                    "Route start, EV + non EV with non-matching size",
                    2,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR)
                    ),
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    true
                ),
                arrayOf(
                    "Route start, EV + EV with non-matching size",
                    2,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR)
                    ),
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING),
                        waypoint(4.4, 5.5, WaypointType.REGULAR)
                    ),
                    true
                ),
                arrayOf(
                    "Route start, non EV + non EV with matching size and non-matching locations",
                    2,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR)
                    ),
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    true
                ),
                arrayOf(
                    "Route start, non EV + EV with matching size and non-matching locations",
                    2,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR)
                    ),
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.EV_CHARGING),
                        waypoint(5.5, 6.6, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    true
                ),
                arrayOf(
                    "Route start, EV + non EV with matching size and non-matching locations",
                    2,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR)
                    ),
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    true
                ),
                arrayOf(
                    "Route start, EV + EV with matching size and non-matching locations",
                    2,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR)
                    ),
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING),
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    true
                ),
                arrayOf(
                    "Route start, non EV + non EV with matching size and matching locations",
                    2,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR)
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR)
                    ),
                    false
                ),
                arrayOf(
                    "Route start, non EV + EV with matching size and matching locations",
                    2,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR)
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR)
                    ),
                    false
                ),
                arrayOf(
                    "Route start, EV + non EV with matching size and matching locations",
                    2,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR)
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR)
                    ),
                    false
                ),
                arrayOf(
                    "Route start, EV + EV with matching size and matching locations",
                    2,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR)
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR)
                    ),
                    false
                ),

                arrayOf(
                    "Route middle, non EV + non EV with non-matching size",
                    2,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR)
                    ),
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    true
                ),
                arrayOf(
                    "Route middle, non EV + EV with non-matching size",
                    2,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR)
                    ),
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING),
                        waypoint(4.4, 5.5, WaypointType.REGULAR)
                    ),
                    true
                ),
                arrayOf(
                    "Route middle, EV + non EV with non-matching size",
                    2,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR)
                    ),
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    true
                ),
                arrayOf(
                    "Route middle, EV + EV with non-matching size",
                    2,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR)
                    ),
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING),
                        waypoint(4.4, 5.5, WaypointType.REGULAR)
                    ),
                    true
                ),
                arrayOf(
                    "Route middle, non EV + non EV with matching size and non-matching locations",
                    2,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR)
                    ),
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR)
                    ),
                    true
                ),
                arrayOf(
                    "Route middle, non EV + EV with matching size and non-matching locations",
                    2,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR)
                    ),
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING),
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR)
                    ),
                    true
                ),
                arrayOf(
                    "Route middle, EV + non EV with matching size and non-matching locations",
                    2,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR)
                    ),
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR)
                    ),
                    true
                ),
                arrayOf(
                    "Route middle, EV + EV with matching size and non-matching locations",
                    2,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR)
                    ),
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING),
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR)
                    ),
                    true
                ),
                arrayOf(
                    "Route middle, non EV + non EV with matching size and matching locations",
                    2,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR)
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR)
                    ),
                    false
                ),
                arrayOf(
                    "Route middle, non EV + EV with matching size and matching locations",
                    2,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR)
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR)
                    ),
                    false
                ),
                arrayOf(
                    "Route middle, EV + non EV with matching size and matching locations",
                    2,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR)
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR)
                    ),
                    false
                ),
                arrayOf(
                    "Route middle, EV + EV with matching size and matching locations",
                    2,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR)
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR)
                    ),
                    false
                ),
                arrayOf(
                    "Route finished, non EV + non EV with matching size and matching locations",
                    0,
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR)
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR)
                    ),
                    true
                ),
                arrayOf(
                    "Route not started, non EV + non EV with matching size and matching locations",
                    3,
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR)
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR)
                    ),
                    false
                ),
                arrayOf(
                    "Route not started, non EV + non EV with matching size " +
                        "and matching locations but different origin",
                    3,
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR)
                    ),
                    listOf(
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR)
                    ),
                    false
                ),
            )
        }

        private fun waypoint(
            longitude: Double,
            latitude: Double,
            waypointType: WaypointType
        ): Waypoint {
            return mockk(relaxed = true) {
                every { location } returns Point.fromLngLat(longitude, latitude)
                every { type } returns waypointType
            }
        }
    }

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    private val accessToken = "pk.123"
    private lateinit var navigationSession: NavigationSession
    private lateinit var tripSession: TripSession
    private lateinit var arrivalProgressObserver: ArrivalProgressObserver
    private val billingService = mockk<BillingServiceInterface>(relaxed = true)
    private lateinit var routeProgress: RouteProgress
    private lateinit var newRoute: NavigationRoute

    private lateinit var billingController: BillingController

    @Before
    fun setup() {
        mockkObject(BillingServiceProvider)
        every { BillingServiceProvider.getInstance() } returns billingService

        every {
            billingService.getSessionStatus(any())
        } returns BillingSessionStatus.NO_SESSION
        every {
            billingService.getSessionStatus(SessionSKUIdentifier.NAV2_SES_TRIP)
        } returns BillingSessionStatus.SESSION_ACTIVE

        val sessionStateObserverSlot = slot<NavigationSessionStateObserver>()
        navigationSession = mockk(relaxUnitFun = true) {
            every {
                registerNavigationSessionStateObserver(capture(sessionStateObserverSlot))
            } just Runs
        }

        arrivalProgressObserver = mockk(relaxUnitFun = true)
        routeProgress = mockk(relaxed = true) {
            every { navigationRoute } returns createNavigationRoute(
                directionsRoute = createDirectionsRoute(),
                waypoints = oldWaypoints
            )
            every {
                remainingWaypoints
            } returns this@BillingControllerWaypointsComparisonTest.remainingWaypoints
        }
        newRoute = createNavigationRoute(
            directionsRoute = createDirectionsRoute(),
            waypoints = newWaypoints
        )

        tripSession = mockk(relaxed = true) {
            every { getRouteProgress() } returns routeProgress
        }

        billingController = BillingController(
            navigationSession,
            arrivalProgressObserver,
            accessToken,
            tripSession
        )
    }

    @Test
    fun compareWaypoints() {
        billingController.onExternalRouteSet(newRoute)

        verify(exactly = if (shouldStartSession) 1 else 0) {
            billingService.beginBillingSession(any(), any(), any(), any(), any())
        }
    }
}
