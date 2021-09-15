package com.mapbox.navigation.core

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.common.BillingServiceError
import com.mapbox.common.BillingServiceErrorCode
import com.mapbox.common.BillingSessionStatus
import com.mapbox.common.OnBillingServiceError
import com.mapbox.common.SKUIdentifier
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.accounts.BillingController
import com.mapbox.navigation.core.accounts.BillingServiceWrapper
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.core.arrival.ArrivalProgressObserver
import com.mapbox.navigation.core.trip.session.NavigationSession
import com.mapbox.navigation.core.trip.session.NavigationSessionState
import com.mapbox.navigation.core.trip.session.NavigationSessionStateObserver
import com.mapbox.navigation.core.trip.session.TripSession
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import io.mockk.verifyOrder
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
    private lateinit var arrivalProgressObserver: ArrivalProgressObserver
    private lateinit var arrivalObserver: ArrivalObserver
    private val triggerBillingServiceErrorCallback = slot<OnBillingServiceError>()
    private val beginBillingServiceErrorCallback = slot<OnBillingServiceError>()
    private val resumeBillingServiceErrorCallback = slot<OnBillingServiceError>()

    private lateinit var billingController: BillingController

    @Before
    fun setup() {
        mockkObject(BillingServiceWrapper)

        val stopSkuIdSlot = slot<SKUIdentifier>()
        every { BillingServiceWrapper.stopBillingSession(capture(stopSkuIdSlot)) } answers {
            every {
                BillingServiceWrapper.getSessionStatus(stopSkuIdSlot.captured)
            } returns BillingSessionStatus.NO_SESSION
        }

        every {
            BillingServiceWrapper.triggerBillingEvent(
                any(),
                any(),
                any(),
                capture(triggerBillingServiceErrorCallback)
            )
        } just Runs

        val beginSkuIdSlot = slot<SKUIdentifier>()
        every {
            BillingServiceWrapper.beginBillingSession(
                any(),
                any(),
                capture(beginSkuIdSlot),
                capture(beginBillingServiceErrorCallback),
                any()
            )
        } answers {
            every {
                BillingServiceWrapper.getSessionStatus(beginSkuIdSlot.captured)
            } returns BillingSessionStatus.SESSION_ACTIVE
        }

        val pauseSkuIdSlot = slot<SKUIdentifier>()
        every { BillingServiceWrapper.pauseBillingSession(capture(pauseSkuIdSlot)) } answers {
            every {
                BillingServiceWrapper.getSessionStatus(pauseSkuIdSlot.captured)
            } returns BillingSessionStatus.SESSION_PAUSED
        }

        val resumeSkuIdSlot = slot<SKUIdentifier>()
        every {
            BillingServiceWrapper.resumeBillingSession(
                capture(resumeSkuIdSlot),
                capture(resumeBillingServiceErrorCallback)
            )
        } answers {
            every {
                BillingServiceWrapper.getSessionStatus(resumeSkuIdSlot.captured)
            } returns BillingSessionStatus.SESSION_ACTIVE
        }

        every {
            BillingServiceWrapper.getSessionStatus(any())
        } returns BillingSessionStatus.NO_SESSION

        val sessionStateObserverSlot = slot<NavigationSessionStateObserver>()
        navigationSession = mockk(relaxUnitFun = true) {
            every {
                registerNavigationSessionStateObserver(capture(sessionStateObserverSlot))
            } just Runs
        }

        val arrivalObserverSlot = slot<ArrivalObserver>()
        arrivalProgressObserver = mockk(relaxUnitFun = true) {
            every {
                registerObserver(capture(arrivalObserverSlot))
            } just Runs
        }

        tripSession = mockk()

        billingController = BillingController(
            navigationSession,
            arrivalProgressObserver,
            accessToken,
            tripSession
        )
        sessionStateObserver = sessionStateObserverSlot.captured
        arrivalObserver = arrivalObserverSlot.captured
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
    fun `free drive cycle`() {
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.Idle)
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.FreeDrive("1"))
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.Idle)

        verifyOrder {
            BillingServiceWrapper.triggerBillingEvent(
                accessToken,
                "",
                SKUIdentifier.NAV2_SES_MAU,
                any()
            )
            BillingServiceWrapper.beginBillingSession(
                accessToken,
                "",
                SKUIdentifier.NAV2_SES_FDTRIP,
                any(),
                TimeUnit.HOURS.toMillis(1)
            )
            BillingServiceWrapper.pauseBillingSession(SKUIdentifier.NAV2_SES_FDTRIP)
        }
    }

    @Test
    fun `active guidance cycle`() {
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.Idle)
        sessionStateObserver.onNavigationSessionStateChanged(
            NavigationSessionState.ActiveGuidance("2")
        )
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.Idle)

        verifyOrder {
            BillingServiceWrapper.triggerBillingEvent(
                accessToken,
                "",
                SKUIdentifier.NAV2_SES_MAU,
                any()
            )
            BillingServiceWrapper.beginBillingSession(
                accessToken,
                "",
                SKUIdentifier.NAV2_SES_TRIP,
                any(),
                0
            )
            BillingServiceWrapper.pauseBillingSession(SKUIdentifier.NAV2_SES_TRIP)
        }
    }

    @Test
    fun `when active guidance to free drive, start billing session`() {
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.Idle)
        sessionStateObserver.onNavigationSessionStateChanged(
            NavigationSessionState.ActiveGuidance("2")
        )
        sessionStateObserver.onNavigationSessionStateChanged(
            NavigationSessionState.FreeDrive("1")
        )

        verifyOrder {
            BillingServiceWrapper.beginBillingSession(
                accessToken,
                "",
                SKUIdentifier.NAV2_SES_TRIP,
                any(),
                0
            )
            BillingServiceWrapper.stopBillingSession(SKUIdentifier.NAV2_SES_TRIP)
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
    fun `when free drive to active guidance, start billing session`() {
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.Idle)
        sessionStateObserver.onNavigationSessionStateChanged(
            NavigationSessionState.FreeDrive("1")
        )
        sessionStateObserver.onNavigationSessionStateChanged(
            NavigationSessionState.ActiveGuidance("2")
        )

        verifyOrder {
            BillingServiceWrapper.beginBillingSession(
                accessToken,
                "",
                SKUIdentifier.NAV2_SES_FDTRIP,
                any(),
                TimeUnit.HOURS.toMillis(1)
            )
            BillingServiceWrapper.stopBillingSession(SKUIdentifier.NAV2_SES_FDTRIP)
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
    fun `when free drive session resumed, native APIs called`() {
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.Idle)
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.FreeDrive("1"))
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.Idle)
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.FreeDrive("1"))

        verifyOrder {
            BillingServiceWrapper.beginBillingSession(
                accessToken,
                "",
                SKUIdentifier.NAV2_SES_FDTRIP,
                any(),
                TimeUnit.HOURS.toMillis(1)
            )
            BillingServiceWrapper.pauseBillingSession(SKUIdentifier.NAV2_SES_FDTRIP)
            BillingServiceWrapper.resumeBillingSession(SKUIdentifier.NAV2_SES_FDTRIP, any())
        }
    }

    @Test
    fun `when active guidance session resumed, native APIs called`() {
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.Idle)
        sessionStateObserver.onNavigationSessionStateChanged(
            NavigationSessionState.ActiveGuidance("2")
        )
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.Idle)
        sessionStateObserver.onNavigationSessionStateChanged(
            NavigationSessionState.ActiveGuidance("2")
        )

        verifyOrder {
            BillingServiceWrapper.beginBillingSession(
                accessToken,
                "",
                SKUIdentifier.NAV2_SES_TRIP,
                any(),
                0
            )
            BillingServiceWrapper.pauseBillingSession(SKUIdentifier.NAV2_SES_TRIP)
            BillingServiceWrapper.resumeBillingSession(SKUIdentifier.NAV2_SES_TRIP, any())
        }
    }

    @Test
    fun `when free drive paused and active guidance started, stop free drive`() {
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.Idle)
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.FreeDrive("1"))
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.Idle)
        sessionStateObserver.onNavigationSessionStateChanged(
            NavigationSessionState.ActiveGuidance("2")
        )

        verifyOrder {
            BillingServiceWrapper.beginBillingSession(
                accessToken,
                "",
                SKUIdentifier.NAV2_SES_FDTRIP,
                any(),
                TimeUnit.HOURS.toMillis(1)
            )
            BillingServiceWrapper.pauseBillingSession(SKUIdentifier.NAV2_SES_FDTRIP)
            BillingServiceWrapper.stopBillingSession(SKUIdentifier.NAV2_SES_FDTRIP)
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
    fun `when active guidance paused and free drive started, stop active guidance`() {
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.Idle)
        sessionStateObserver.onNavigationSessionStateChanged(
            NavigationSessionState.ActiveGuidance("2")
        )
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.Idle)
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.FreeDrive("1"))

        verifyOrder {
            BillingServiceWrapper.beginBillingSession(
                accessToken,
                "",
                SKUIdentifier.NAV2_SES_TRIP,
                any(),
                0
            )
            BillingServiceWrapper.pauseBillingSession(SKUIdentifier.NAV2_SES_TRIP)
            BillingServiceWrapper.stopBillingSession(SKUIdentifier.NAV2_SES_TRIP)
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
    fun `when resumption fails, restart the session`() {
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.Idle)
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.FreeDrive("1"))
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.Idle)
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.FreeDrive("1"))
        resumeBillingServiceErrorCallback.captured.run(
            BillingServiceError(BillingServiceErrorCode.RESUME_FAILED, "failure")
        )
        verifyOrder {
            BillingServiceWrapper.beginBillingSession(
                accessToken,
                "",
                SKUIdentifier.NAV2_SES_FDTRIP,
                any(),
                TimeUnit.HOURS.toMillis(1)
            )
            BillingServiceWrapper.pauseBillingSession(SKUIdentifier.NAV2_SES_FDTRIP)
            BillingServiceWrapper.resumeBillingSession(SKUIdentifier.NAV2_SES_FDTRIP, any())
            BillingServiceWrapper.stopBillingSession(SKUIdentifier.NAV2_SES_FDTRIP)
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
    fun `when idle and external route set, do nothing`() {
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.Idle)
        every { navigationSession.state } returns NavigationSessionState.Idle
        billingController.onExternalRouteSet(mockk())
        verify(exactly = 0) {
            BillingServiceWrapper.beginBillingSession(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `when free drive and external route set, do nothing`() {
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.FreeDrive("1"))
        every { navigationSession.state } returns NavigationSessionState.FreeDrive("1")
        billingController.onExternalRouteSet(mockk())
        verify(exactly = 0) {
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
    fun `when active guidance and new external route set, start new billing session`() {
        sessionStateObserver.onNavigationSessionStateChanged(
            NavigationSessionState.ActiveGuidance("2")
        )

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

        verifyOrder {
            BillingServiceWrapper.beginBillingSession(
                accessToken,
                "",
                SKUIdentifier.NAV2_SES_TRIP,
                any(),
                0
            )
            BillingServiceWrapper.stopBillingSession(SKUIdentifier.NAV2_SES_TRIP)
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
        sessionStateObserver.onNavigationSessionStateChanged(
            NavigationSessionState.ActiveGuidance("2")
        )

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

        verify(exactly = 1) {
            BillingServiceWrapper.beginBillingSession(
                accessToken,
                "",
                SKUIdentifier.NAV2_SES_TRIP,
                any(),
                0
            )
        }
        verify(exactly = 0) {
            BillingServiceWrapper.stopBillingSession(any())
        }
    }

    @Test
    fun `when active guidance and external route set with silent waypoints, do nothing`() {
        sessionStateObserver.onNavigationSessionStateChanged(
            NavigationSessionState.ActiveGuidance("2")
        )

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

        verify(exactly = 1) {
            BillingServiceWrapper.beginBillingSession(
                accessToken,
                "",
                SKUIdentifier.NAV2_SES_TRIP,
                any(),
                0
            )
        }
        verify(exactly = 0) {
            BillingServiceWrapper.stopBillingSession(any())
        }
    }

    @Test
    fun `when active guidance and external route set with original silent waypoints, do nothing`() {
        sessionStateObserver.onNavigationSessionStateChanged(
            NavigationSessionState.ActiveGuidance("2")
        )

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

        verify(exactly = 1) {
            BillingServiceWrapper.beginBillingSession(
                accessToken,
                "",
                SKUIdentifier.NAV2_SES_TRIP,
                any(),
                0
            )
        }
        verify(exactly = 0) {
            BillingServiceWrapper.stopBillingSession(any())
        }
    }

    @Test
    fun `when active guidance and new external multi-leg route set, start new billing session`() {
        sessionStateObserver.onNavigationSessionStateChanged(
            NavigationSessionState.ActiveGuidance("2")
        )

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

        verifyOrder {
            BillingServiceWrapper.beginBillingSession(
                accessToken,
                "",
                SKUIdentifier.NAV2_SES_TRIP,
                any(),
                0
            )
            BillingServiceWrapper.stopBillingSession(SKUIdentifier.NAV2_SES_TRIP)
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
        sessionStateObserver.onNavigationSessionStateChanged(
            NavigationSessionState.ActiveGuidance("2")
        )

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

        verifyOrder {
            BillingServiceWrapper.beginBillingSession(
                accessToken,
                "",
                SKUIdentifier.NAV2_SES_TRIP,
                any(),
                0
            )
            BillingServiceWrapper.stopBillingSession(SKUIdentifier.NAV2_SES_TRIP)
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
        sessionStateObserver.onNavigationSessionStateChanged(
            NavigationSessionState.ActiveGuidance("2")
        )

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

        verify(exactly = 1) {
            BillingServiceWrapper.beginBillingSession(
                accessToken,
                "",
                SKUIdentifier.NAV2_SES_TRIP,
                any(),
                0
            )
        }
        verify(exactly = 0) {
            BillingServiceWrapper.stopBillingSession(any())
        }
    }

    @Test
    fun `when active guidance completed and external route set, start new billing session`() {
        sessionStateObserver.onNavigationSessionStateChanged(
            NavigationSessionState.ActiveGuidance("2")
        )

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

        verifyOrder {
            BillingServiceWrapper.beginBillingSession(
                accessToken,
                "",
                SKUIdentifier.NAV2_SES_TRIP,
                any(),
                0
            )
            BillingServiceWrapper.stopBillingSession(SKUIdentifier.NAV2_SES_TRIP)
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
        sessionStateObserver.onNavigationSessionStateChanged(
            NavigationSessionState.ActiveGuidance("2")
        )

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

        verify(exactly = 1) {
            BillingServiceWrapper.beginBillingSession(
                accessToken,
                "",
                SKUIdentifier.NAV2_SES_TRIP,
                any(),
                0
            )
        }
        verify(exactly = 0) {
            BillingServiceWrapper.stopBillingSession(any())
        }
    }

    @Test
    fun `active guidance paused and new external route set, start and pause new billing session`() {
        sessionStateObserver.onNavigationSessionStateChanged(
            NavigationSessionState.ActiveGuidance("2")
        )
        sessionStateObserver.onNavigationSessionStateChanged(
            NavigationSessionState.Idle
        )
        every { navigationSession.state } returns NavigationSessionState.Idle
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

        verifyOrder {
            BillingServiceWrapper.beginBillingSession(
                accessToken,
                "",
                SKUIdentifier.NAV2_SES_TRIP,
                any(),
                0
            )
            BillingServiceWrapper.pauseBillingSession(SKUIdentifier.NAV2_SES_TRIP)
            BillingServiceWrapper.stopBillingSession(SKUIdentifier.NAV2_SES_TRIP)
            BillingServiceWrapper.beginBillingSession(
                accessToken,
                "",
                SKUIdentifier.NAV2_SES_TRIP,
                any(),
                0
            )
            BillingServiceWrapper.pauseBillingSession(SKUIdentifier.NAV2_SES_TRIP)
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `exception thrown when new route leg started while a session is not running`() {
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.Idle)
        arrivalObserver.onNextRouteLegStart(mockk())
    }

    @Test(expected = IllegalStateException::class)
    fun `exception thrown when new route leg started while a free drive session is running`() {
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.Idle)
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.FreeDrive("1"))
        arrivalObserver.onNextRouteLegStart(mockk())
    }

    @Test
    fun `new session is started for each route leg`() {
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.Idle)
        sessionStateObserver.onNavigationSessionStateChanged(
            NavigationSessionState.ActiveGuidance("2")
        )
        arrivalObserver.onNextRouteLegStart(mockk())
        arrivalObserver.onNextRouteLegStart(mockk())

        verify(exactly = 3) {
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
    fun `on destroy stops running free drive session`() {
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.Idle)
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.FreeDrive("1"))
        billingController.onDestroy()

        verify {
            BillingServiceWrapper.stopBillingSession(SKUIdentifier.NAV2_SES_FDTRIP)
        }
    }

    @Test
    fun `on destroy stops running active guidance session`() {
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.Idle)
        sessionStateObserver.onNavigationSessionStateChanged(
            NavigationSessionState.ActiveGuidance("2")
        )
        billingController.onDestroy()

        verify {
            BillingServiceWrapper.stopBillingSession(SKUIdentifier.NAV2_SES_TRIP)
        }
    }

    @Test
    fun `on destroy unregisters observers`() {
        billingController.onDestroy()
        verify { navigationSession.unregisterNavigationSessionStateObserver(sessionStateObserver) }
        verify { arrivalProgressObserver.unregisterObserver(arrivalObserver) }
    }

    @After
    fun cleanup() {
        unmockkObject(BillingServiceWrapper)
    }
}
