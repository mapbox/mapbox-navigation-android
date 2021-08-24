package com.mapbox.navigation.core

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.common.BillingSessionStatus
import com.mapbox.common.OnBillingServiceError
import com.mapbox.common.SKUIdentifier
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.accounts.BillingController
import com.mapbox.navigation.core.accounts.BillingServiceWrapper
import com.mapbox.navigation.core.trip.session.NavigationSession
import com.mapbox.navigation.core.trip.session.NavigationSessionState
import com.mapbox.navigation.core.trip.session.NavigationSessionStateObserver
import com.mapbox.navigation.core.trip.session.TripSession
import io.mockk.Call
import io.mockk.MockKAnswerScope
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class BillingControllerTest {

    private val accessToken = "pk.123"
    private lateinit var navigationSession: NavigationSession
    private lateinit var tripSession: TripSession
    private lateinit var sessionStateObserver: NavigationSessionStateObserver
    private lateinit var billingServiceErrorCallback: OnBillingServiceError

    private lateinit var billingController: BillingController

    @Before
    fun setup() {
        mockkObject(BillingServiceWrapper)
        val billingServiceErrorCallbackSlot = slot<OnBillingServiceError>()
        val billingServiceErrorCallbackAnswer: MockKAnswerScope<Unit, Unit>.(Call) -> Unit = {
            billingServiceErrorCallback = billingServiceErrorCallbackSlot.captured
        }
        every { BillingServiceWrapper.stopBillingSession(any()) } just Runs
        every {
            BillingServiceWrapper.triggerBillingEvent(
                any(),
                any(),
                any(),
                capture(billingServiceErrorCallbackSlot)
            )
        } answers billingServiceErrorCallbackAnswer
        every {
            BillingServiceWrapper.beginBillingSession(
                any(),
                any(),
                any(),
                capture(billingServiceErrorCallbackSlot),
                any()
            )
        } answers billingServiceErrorCallbackAnswer
        every { BillingServiceWrapper.pauseBillingSession(any()) } just Runs
        every {
            BillingServiceWrapper.resumeBillingSession(
                any(),
                capture(billingServiceErrorCallbackSlot)
            )
        } answers billingServiceErrorCallbackAnswer
        every {
            BillingServiceWrapper.getSessionStatus(any())
        } returns BillingSessionStatus.NO_SESSION

        val sessionStateObserverSlot = slot<NavigationSessionStateObserver>()
        navigationSession = mockk {
            every {
                registerNavigationSessionStateObserver(capture(sessionStateObserverSlot))
            } just Runs
        }
        tripSession = mockk()

        billingController = BillingController(accessToken, navigationSession, tripSession)
        sessionStateObserver = sessionStateObserverSlot.captured
    }

    @Test
    fun sanity() {
        assertNotNull(billingController)
    }

    @Test
    fun `when idle, do nothing`() {
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.Idle)

        verify(exactly = 0) { BillingServiceWrapper.stopBillingSession(any()) }
        verify(exactly = 0) {
            BillingServiceWrapper.triggerBillingEvent(any(), any(), any(), any())
        }
        verify(exactly = 0) {
            BillingServiceWrapper.beginBillingSession(any(), any(), any(), any(), any())
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `when both free drive and active guidance are active, throw an exception`() {
        every {
            BillingServiceWrapper.getSessionStatus(SKUIdentifier.NAV2_SES_TRIP)
        } returns BillingSessionStatus.SESSION_ACTIVE
        every {
            BillingServiceWrapper.getSessionStatus(SKUIdentifier.NAV2_SES_FDTRIP)
        } returns BillingSessionStatus.SESSION_ACTIVE
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.Idle)
    }

    @Test
    fun `when active free drive to idle, stop billing session`() {
        every {
            BillingServiceWrapper.getSessionStatus(SKUIdentifier.NAV2_SES_FDTRIP)
        } returns BillingSessionStatus.SESSION_ACTIVE
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.Idle)

        verify(exactly = 1) {
            BillingServiceWrapper.stopBillingSession(SKUIdentifier.NAV2_SES_FDTRIP)
        }
        verify(exactly = 0) {
            BillingServiceWrapper.triggerBillingEvent(any(), any(), any(), any())
        }
        verify(exactly = 0) {
            BillingServiceWrapper.beginBillingSession(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `when paused free drive to idle, stop billing session`() {
        every {
            BillingServiceWrapper.getSessionStatus(SKUIdentifier.NAV2_SES_FDTRIP)
        } returns BillingSessionStatus.SESSION_PAUSED
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.Idle)

        verify(exactly = 1) {
            BillingServiceWrapper.stopBillingSession(SKUIdentifier.NAV2_SES_FDTRIP)
        }
        verify(exactly = 0) {
            BillingServiceWrapper.triggerBillingEvent(any(), any(), any(), any())
        }
        verify(exactly = 0) {
            BillingServiceWrapper.beginBillingSession(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `when active active guidance to idle, stop billing session`() {
        every {
            BillingServiceWrapper.getSessionStatus(SKUIdentifier.NAV2_SES_TRIP)
        } returns BillingSessionStatus.SESSION_ACTIVE
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.Idle)

        verify(exactly = 1) {
            BillingServiceWrapper.stopBillingSession(SKUIdentifier.NAV2_SES_TRIP)
        }
        verify(exactly = 0) {
            BillingServiceWrapper.triggerBillingEvent(any(), any(), any(), any())
        }
        verify(exactly = 0) {
            BillingServiceWrapper.beginBillingSession(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `when paused active guidance to idle, stop billing session`() {
        every {
            BillingServiceWrapper.getSessionStatus(SKUIdentifier.NAV2_SES_TRIP)
        } returns BillingSessionStatus.SESSION_PAUSED
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.Idle)

        verify(exactly = 1) {
            BillingServiceWrapper.stopBillingSession(SKUIdentifier.NAV2_SES_TRIP)
        }
        verify(exactly = 0) {
            BillingServiceWrapper.triggerBillingEvent(any(), any(), any(), any())
        }
        verify(exactly = 0) {
            BillingServiceWrapper.beginBillingSession(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `when not idle, trigger MAU event`() {
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.FreeDrive("1"))
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.ActiveGuidance("2"))

        verify(exactly = 2) {
            BillingServiceWrapper.triggerBillingEvent(
                accessToken,
                "",
                SKUIdentifier.NAV2_SES_MAU,
                any()
            )
        }
    }

    @Test
    fun `when idle to free drive, start billing session`() {
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.FreeDrive("1"))

        verify(exactly = 1) {
            BillingServiceWrapper.beginBillingSession(
                accessToken,
                "",
                SKUIdentifier.NAV2_SES_FDTRIP,
                any(),
                TimeUnit.HOURS.toMillis(1)
            )
        }
    }

    @Test
    fun `when idle to active guidance, start billing session`() {
        sessionStateObserver.onNavigationSessionStateChanged(
            NavigationSessionState.ActiveGuidance("2")
        )

        verify(exactly = 1) {
            BillingServiceWrapper.beginBillingSession(
                accessToken,
                "",
                SKUIdentifier.NAV2_SES_TRIP,
                any(),
                0
            )
        }
    }

    @Test
    fun `when active guidance to free drive, start billing session`() {
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.ActiveGuidance("1"))
        every {
            BillingServiceWrapper.getSessionStatus(SKUIdentifier.NAV2_SES_TRIP)
        } returns BillingSessionStatus.SESSION_ACTIVE
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.FreeDrive("1"))

        verify(exactly = 1) {
            BillingServiceWrapper.beginBillingSession(
                accessToken,
                "",
                SKUIdentifier.NAV2_SES_FDTRIP,
                any(),
                TimeUnit.HOURS.toMillis(1)
            )
        }
        verify(exactly = 1) {
            BillingServiceWrapper.stopBillingSession(SKUIdentifier.NAV2_SES_TRIP)
        }
    }

    @Test
    fun `when free drive to active guidance, start billing session`() {
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.FreeDrive("1"))
        every {
            BillingServiceWrapper.getSessionStatus(SKUIdentifier.NAV2_SES_FDTRIP)
        } returns BillingSessionStatus.SESSION_ACTIVE
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.ActiveGuidance("1"))

        verify(exactly = 1) {
            BillingServiceWrapper.beginBillingSession(
                accessToken,
                "",
                SKUIdentifier.NAV2_SES_TRIP,
                any(),
                0
            )
        }
        verify(exactly = 1) {
            BillingServiceWrapper.stopBillingSession(SKUIdentifier.NAV2_SES_FDTRIP)
        }
    }

    @Test
    fun `when pause free drive session, native APIs called`() {
        every {
            BillingServiceWrapper.getSessionStatus(SKUIdentifier.NAV2_SES_FDTRIP)
        } returns BillingSessionStatus.SESSION_ACTIVE
        billingController.pauseSession()
        verify(exactly = 1) {
            BillingServiceWrapper.pauseBillingSession(SKUIdentifier.NAV2_SES_FDTRIP)
        }
    }

    @Test
    fun `when pause active guidance session, native APIs called`() {
        every {
            BillingServiceWrapper.getSessionStatus(SKUIdentifier.NAV2_SES_TRIP)
        } returns BillingSessionStatus.SESSION_ACTIVE
        billingController.pauseSession()
        verify(exactly = 1) {
            BillingServiceWrapper.pauseBillingSession(SKUIdentifier.NAV2_SES_TRIP)
        }
    }

    @Test
    fun `when resume free drive session, native APIs called`() {
        every {
            BillingServiceWrapper.getSessionStatus(SKUIdentifier.NAV2_SES_FDTRIP)
        } returns BillingSessionStatus.SESSION_PAUSED
        billingController.resumeSession()
        verify(exactly = 1) {
            BillingServiceWrapper.resumeBillingSession(SKUIdentifier.NAV2_SES_FDTRIP, any())
        }
    }

    @Test
    fun `when resume active session, native APIs called`() {
        every {
            BillingServiceWrapper.getSessionStatus(SKUIdentifier.NAV2_SES_TRIP)
        } returns BillingSessionStatus.SESSION_PAUSED
        billingController.resumeSession()
        verify(exactly = 1) {
            BillingServiceWrapper.resumeBillingSession(SKUIdentifier.NAV2_SES_TRIP, any())
        }
    }

    @Test
    fun `when idle and external route set, do nothing`() {
        every { navigationSession.state } returns NavigationSessionState.Idle
        billingController.onExternalRouteSet(mockk())
        verify(exactly = 0) {
            BillingServiceWrapper.beginBillingSession(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `when free drive and external route set, do nothing`() {
        every { navigationSession.state } returns NavigationSessionState.FreeDrive("1")
        every {
            BillingServiceWrapper.getSessionStatus(SKUIdentifier.NAV2_SES_FDTRIP)
        } returns BillingSessionStatus.SESSION_ACTIVE
        billingController.onExternalRouteSet(mockk())
        verify(exactly = 0) {
            BillingServiceWrapper.beginBillingSession(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `when active guidance and new external route set, start new billing session`() {
        every { navigationSession.state } returns NavigationSessionState.ActiveGuidance("2")
        every {
            BillingServiceWrapper.getSessionStatus(SKUIdentifier.NAV2_SES_TRIP)
        } returns BillingSessionStatus.SESSION_ACTIVE
        val originalRouteOptions = mockk<RouteOptions> {
            every { coordinatesList() } returns listOf(
                Point.fromLngLat(0.0, 0.0),
                Point.fromLngLat(1.0, 1.0)
            )
            every { waypointIndicesList() } returns null
        }
        val originalRoute = mockk<DirectionsRoute> {
            every { routeOptions() } returns originalRouteOptions
        }
        val routeProgress = mockk<RouteProgress> {
            every { route } returns originalRoute
            every { remainingWaypoints } returns 1
        }
        every { tripSession.getRouteProgress() } returns routeProgress

        val newRouteOptions = mockk<RouteOptions> {
            every { coordinatesList() } returns listOf(
                Point.fromLngLat(0.1, 0.1),
                Point.fromLngLat(2.0, 2.0)
            )
            every { waypointIndicesList() } returns null
        }
        val newRoute = mockk<DirectionsRoute> {
            every { routeOptions() } returns newRouteOptions
        }

        billingController.onExternalRouteSet(newRoute)

        verify(exactly = 1) {
            BillingServiceWrapper.stopBillingSession(SKUIdentifier.NAV2_SES_TRIP)
        }
        verify(exactly = 1) {
            BillingServiceWrapper.beginBillingSession(
                accessToken,
                "",
                SKUIdentifier.NAV2_SES_TRIP,
                any(),
                0
            )
        }
    }

    @Test
    fun `when active guidance and same external route set, do nothing`() {
        every { navigationSession.state } returns NavigationSessionState.ActiveGuidance("2")
        every {
            BillingServiceWrapper.getSessionStatus(SKUIdentifier.NAV2_SES_TRIP)
        } returns BillingSessionStatus.SESSION_ACTIVE
        val originalRouteOptions = mockk<RouteOptions> {
            every { coordinatesList() } returns listOf(
                Point.fromLngLat(0.0, 0.0),
                Point.fromLngLat(1.0, 1.0)
            )
            every { waypointIndicesList() } returns null
        }
        val originalRoute = mockk<DirectionsRoute> {
            every { routeOptions() } returns originalRouteOptions
        }
        val routeProgress = mockk<RouteProgress> {
            every { route } returns originalRoute
            every { remainingWaypoints } returns 1
        }
        every { tripSession.getRouteProgress() } returns routeProgress

        val newRouteOptions = mockk<RouteOptions> {
            every { coordinatesList() } returns listOf(
                Point.fromLngLat(0.1, 0.1),
                // less than 100 meters from original
                Point.fromLngLat(1.0005, 1.0005)
            )
            every { waypointIndicesList() } returns null
        }
        val newRoute = mockk<DirectionsRoute> {
            every { routeOptions() } returns newRouteOptions
        }

        billingController.onExternalRouteSet(newRoute)

        verify(exactly = 0) {
            BillingServiceWrapper.beginBillingSession(any(), any(), any(), any(), any())
        }
        verify(exactly = 0) {
            BillingServiceWrapper.stopBillingSession(any())
        }
    }

    @Test
    fun `when active guidance and external route set with silent waypoints, do nothing`() {
        every { navigationSession.state } returns NavigationSessionState.ActiveGuidance("2")
        every {
            BillingServiceWrapper.getSessionStatus(SKUIdentifier.NAV2_SES_TRIP)
        } returns BillingSessionStatus.SESSION_ACTIVE
        val originalRouteOptions = mockk<RouteOptions> {
            every { coordinatesList() } returns listOf(
                Point.fromLngLat(0.0, 0.0),
                Point.fromLngLat(1.0, 1.0)
            )
            every { waypointIndicesList() } returns null
        }
        val originalRoute = mockk<DirectionsRoute> {
            every { routeOptions() } returns originalRouteOptions
        }
        val routeProgress = mockk<RouteProgress> {
            every { route } returns originalRoute
            every { remainingWaypoints } returns 1
        }
        every { tripSession.getRouteProgress() } returns routeProgress

        val newRouteOptions = mockk<RouteOptions> {
            every { coordinatesList() } returns listOf(
                Point.fromLngLat(0.1, 0.1),
                Point.fromLngLat(0.3, 0.3),
                // less than 100 meters from original
                Point.fromLngLat(1.0005, 1.0005)
            )
            every { waypointIndicesList() } returns listOf(0, 2)
        }
        val newRoute = mockk<DirectionsRoute> {
            every { routeOptions() } returns newRouteOptions
        }

        billingController.onExternalRouteSet(newRoute)

        verify(exactly = 0) {
            BillingServiceWrapper.beginBillingSession(any(), any(), any(), any(), any())
        }
        verify(exactly = 0) {
            BillingServiceWrapper.stopBillingSession(any())
        }
    }

    @Test
    fun `when active guidance and external route set with original silent waypoints, do nothing`() {
        every { navigationSession.state } returns NavigationSessionState.ActiveGuidance("2")
        every {
            BillingServiceWrapper.getSessionStatus(SKUIdentifier.NAV2_SES_TRIP)
        } returns BillingSessionStatus.SESSION_ACTIVE
        val originalRouteOptions = mockk<RouteOptions> {
            every { coordinatesList() } returns listOf(
                Point.fromLngLat(0.0, 0.0),
                Point.fromLngLat(0.3, 0.3),
                Point.fromLngLat(1.0, 1.0)
            )
            every { waypointIndicesList() } returns listOf(0, 2)
        }
        val originalRoute = mockk<DirectionsRoute> {
            every { routeOptions() } returns originalRouteOptions
        }
        val routeProgress = mockk<RouteProgress> {
            every { route } returns originalRoute
            every { remainingWaypoints } returns 1
        }
        every { tripSession.getRouteProgress() } returns routeProgress

        val newRouteOptions = mockk<RouteOptions> {
            every { coordinatesList() } returns listOf(
                Point.fromLngLat(0.1, 0.1),
                // less than 100 meters from original
                Point.fromLngLat(1.0005, 1.0005)
            )
            every { waypointIndicesList() } returns null
        }
        val newRoute = mockk<DirectionsRoute> {
            every { routeOptions() } returns newRouteOptions
        }

        billingController.onExternalRouteSet(newRoute)

        verify(exactly = 0) {
            BillingServiceWrapper.beginBillingSession(any(), any(), any(), any(), any())
        }
        verify(exactly = 0) {
            BillingServiceWrapper.stopBillingSession(any())
        }
    }

    @Test
    fun `when active guidance and new external multi-leg route set, start new billing session`() {
        every { navigationSession.state } returns NavigationSessionState.ActiveGuidance("2")
        every {
            BillingServiceWrapper.getSessionStatus(SKUIdentifier.NAV2_SES_TRIP)
        } returns BillingSessionStatus.SESSION_ACTIVE
        val originalRouteOptions = mockk<RouteOptions> {
            every { coordinatesList() } returns listOf(
                Point.fromLngLat(0.0, 0.0),
                Point.fromLngLat(1.0, 1.0)
            )
            every { waypointIndicesList() } returns null
        }
        val originalRoute = mockk<DirectionsRoute> {
            every { routeOptions() } returns originalRouteOptions
        }
        val routeProgress = mockk<RouteProgress> {
            every { route } returns originalRoute
            every { remainingWaypoints } returns 1
        }
        every { tripSession.getRouteProgress() } returns routeProgress

        val newRouteOptions = mockk<RouteOptions> {
            every { coordinatesList() } returns listOf(
                Point.fromLngLat(0.1, 0.1),
                Point.fromLngLat(1.1, 1.1),
                Point.fromLngLat(1.0, 1.0)
            )
            every { waypointIndicesList() } returns null
        }
        val newRoute = mockk<DirectionsRoute> {
            every { routeOptions() } returns newRouteOptions
        }

        billingController.onExternalRouteSet(newRoute)

        verify(exactly = 1) {
            BillingServiceWrapper.stopBillingSession(SKUIdentifier.NAV2_SES_TRIP)
        }
        verify(exactly = 1) {
            BillingServiceWrapper.beginBillingSession(
                accessToken,
                "",
                SKUIdentifier.NAV2_SES_TRIP,
                any(),
                0
            )
        }
    }

    @Test
    fun `when active guidance and route set with multi-leg destination not matching, restart`() {
        every { navigationSession.state } returns NavigationSessionState.ActiveGuidance("2")
        every {
            BillingServiceWrapper.getSessionStatus(SKUIdentifier.NAV2_SES_TRIP)
        } returns BillingSessionStatus.SESSION_ACTIVE
        val originalRouteOptions = mockk<RouteOptions> {
            every { coordinatesList() } returns listOf(
                Point.fromLngLat(0.0, 0.0),
                Point.fromLngLat(0.5, 0.5),
                Point.fromLngLat(1.0, 1.0)
            )
            every { waypointIndicesList() } returns null
        }
        val originalRoute = mockk<DirectionsRoute> {
            every { routeOptions() } returns originalRouteOptions
        }
        val routeProgress = mockk<RouteProgress> {
            every { route } returns originalRoute
            every { remainingWaypoints } returns 2
        }
        every { tripSession.getRouteProgress() } returns routeProgress

        val newRouteOptions = mockk<RouteOptions> {
            every { coordinatesList() } returns listOf(
                Point.fromLngLat(0.7, 0.7),
                Point.fromLngLat(1.0005, 1.0005)
            )
            every { waypointIndicesList() } returns null
        }
        val newRoute = mockk<DirectionsRoute> {
            every { routeOptions() } returns newRouteOptions
        }

        billingController.onExternalRouteSet(newRoute)

        verify(exactly = 1) {
            BillingServiceWrapper.stopBillingSession(SKUIdentifier.NAV2_SES_TRIP)
        }
        verify(exactly = 1) {
            BillingServiceWrapper.beginBillingSession(
                accessToken,
                "",
                SKUIdentifier.NAV2_SES_TRIP,
                any(),
                0
            )
        }
    }

    @Test
    fun `when active guidance and route set with multi-leg destination matching, do nothing`() {
        every { navigationSession.state } returns NavigationSessionState.ActiveGuidance("2")
        val originalRouteOptions = mockk<RouteOptions> {
            every { coordinatesList() } returns listOf(
                Point.fromLngLat(0.0, 0.0),
                Point.fromLngLat(0.5, 0.5),
                Point.fromLngLat(1.0, 1.0)
            )
            every { waypointIndicesList() } returns null
        }
        val originalRoute = mockk<DirectionsRoute> {
            every { routeOptions() } returns originalRouteOptions
        }
        val routeProgress = mockk<RouteProgress> {
            every { route } returns originalRoute
            every { remainingWaypoints } returns 1
        }
        every { tripSession.getRouteProgress() } returns routeProgress

        val newRouteOptions = mockk<RouteOptions> {
            every { coordinatesList() } returns listOf(
                Point.fromLngLat(0.7, 0.7),
                // less than 100 meters from original
                Point.fromLngLat(1.0005, 1.0005)
            )
            every { waypointIndicesList() } returns null
        }
        val newRoute = mockk<DirectionsRoute> {
            every { routeOptions() } returns newRouteOptions
        }

        billingController.onExternalRouteSet(newRoute)

        verify(exactly = 0) {
            BillingServiceWrapper.beginBillingSession(any(), any(), any(), any(), any())
        }
        verify(exactly = 0) {
            BillingServiceWrapper.stopBillingSession(any())
        }
    }

    @Test
    fun `when active guidance completed and external route set, start new billing session`() {
        every { navigationSession.state } returns NavigationSessionState.ActiveGuidance("2")
        every {
            BillingServiceWrapper.getSessionStatus(SKUIdentifier.NAV2_SES_TRIP)
        } returns BillingSessionStatus.SESSION_ACTIVE
        val originalRouteOptions = mockk<RouteOptions> {
            every { coordinatesList() } returns listOf(
                Point.fromLngLat(0.0, 0.0),
                Point.fromLngLat(1.0, 1.0)
            )
            every { waypointIndicesList() } returns null
        }
        val originalRoute = mockk<DirectionsRoute> {
            every { routeOptions() } returns originalRouteOptions
        }
        val routeProgress = mockk<RouteProgress> {
            every { route } returns originalRoute
            every { remainingWaypoints } returns 0
        }
        every { tripSession.getRouteProgress() } returns routeProgress

        val newRouteOptions = mockk<RouteOptions> {
            every { coordinatesList() } returns listOf(
                Point.fromLngLat(0.1, 0.1),
                Point.fromLngLat(2.0, 2.0)
            )
            every { waypointIndicesList() } returns null
        }
        val newRoute = mockk<DirectionsRoute> {
            every { routeOptions() } returns newRouteOptions
        }

        billingController.onExternalRouteSet(newRoute)

        verify(exactly = 1) {
            BillingServiceWrapper.stopBillingSession(SKUIdentifier.NAV2_SES_TRIP)
        }
        verify(exactly = 1) {
            BillingServiceWrapper.beginBillingSession(
                accessToken,
                "",
                SKUIdentifier.NAV2_SES_TRIP,
                any(),
                0
            )
        }
    }

    /**
     * This is an edge case where we are not on the route (due to map-matching or anything else)
     * and remaining waypoints count is equal to all waypoints count.
     */
    @Test
    fun `when active guidance not started and external route set, do nothing`() {
        every { navigationSession.state } returns NavigationSessionState.ActiveGuidance("2")
        val originalRouteOptions = mockk<RouteOptions> {
            every { coordinatesList() } returns listOf(
                Point.fromLngLat(0.0, 0.0),
                Point.fromLngLat(1.0, 1.0)
            )
            every { waypointIndicesList() } returns null
        }
        val originalRoute = mockk<DirectionsRoute> {
            every { routeOptions() } returns originalRouteOptions
        }
        val routeProgress = mockk<RouteProgress> {
            every { route } returns originalRoute
            every { remainingWaypoints } returns 2
        }
        every { tripSession.getRouteProgress() } returns routeProgress

        val newRouteOptions = mockk<RouteOptions> {
            every { coordinatesList() } returns listOf(
                Point.fromLngLat(0.1, 0.1),
                // less than 100 meters from original
                Point.fromLngLat(1.0005, 1.0005)
            )
            every { waypointIndicesList() } returns null
        }
        val newRoute = mockk<DirectionsRoute> {
            every { routeOptions() } returns newRouteOptions
        }

        billingController.onExternalRouteSet(newRoute)

        verify(exactly = 0) {
            BillingServiceWrapper.beginBillingSession(any(), any(), any(), any(), any())
        }
        verify(exactly = 0) {
            BillingServiceWrapper.stopBillingSession(any())
        }
    }

    @After
    fun cleanup() {
        unmockkObject(BillingServiceWrapper)
    }
}
