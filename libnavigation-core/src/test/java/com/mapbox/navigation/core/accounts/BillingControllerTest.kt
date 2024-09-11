package com.mapbox.navigation.core.accounts

import com.mapbox.common.BillingServiceError
import com.mapbox.common.BillingServiceErrorCode
import com.mapbox.common.BillingSessionStatus
import com.mapbox.common.OnBillingServiceError
import com.mapbox.common.SdkInformation
import com.mapbox.common.SessionSKUIdentifier
import com.mapbox.common.UserSKUIdentifier
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.internal.accounts.SkuIdProvider
import com.mapbox.navigation.base.internal.accounts.SkuIdProviderImpl
import com.mapbox.navigation.base.internal.route.Waypoint
import com.mapbox.navigation.base.internal.utils.internalWaypoints
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.core.arrival.ArrivalProgressObserver
import com.mapbox.navigation.core.trip.session.NavigationSession
import com.mapbox.navigation.core.trip.session.NavigationSessionState
import com.mapbox.navigation.core.trip.session.NavigationSessionStateObserver
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.testing.LoggingFrontendTestRule
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
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit

class BillingControllerTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    private lateinit var navigationSession: NavigationSession
    private lateinit var tripSession: TripSession
    private lateinit var sessionStateObserver: NavigationSessionStateObserver
    private lateinit var arrivalProgressObserver: ArrivalProgressObserver
    private lateinit var arrivalObserver: ArrivalObserver
    private lateinit var skuIdProvider: SkuIdProvider
    private lateinit var sdkInformation: SdkInformation
    private val triggerBillingServiceErrorCallback = slot<OnBillingServiceError>()
    private val beginBillingServiceErrorCallback = slot<OnBillingServiceError>()
    private val resumeBillingServiceErrorCallback = slot<OnBillingServiceError>()
    private val billingService = mockk<BillingServiceProxy>()

    private lateinit var billingController: BillingController

    @Before
    fun setup() {
        mockkObject(BillingServiceProvider)
        every { BillingServiceProvider.getInstance() } returns billingService

        val stopSkuIdSlot = slot<SessionSKUIdentifier>()
        every { billingService.stopBillingSession(capture(stopSkuIdSlot)) } answers {
            every {
                billingService.getSessionStatus(stopSkuIdSlot.captured)
            } returns BillingSessionStatus.NO_SESSION
        }

        every {
            billingService.triggerUserBillingEvent(
                any(),
                any(),
                capture(triggerBillingServiceErrorCallback),
            )
        } just Runs

        val beginSkuIdSlot = slot<SessionSKUIdentifier>()
        every {
            billingService.beginBillingSession(
                any(),
                capture(beginSkuIdSlot),
                capture(beginBillingServiceErrorCallback),
                any(),
            )
        } answers {
            every {
                billingService.getSessionStatus(beginSkuIdSlot.captured)
            } returns BillingSessionStatus.SESSION_ACTIVE
        }

        val pauseSkuIdSlot = slot<SessionSKUIdentifier>()
        every { billingService.pauseBillingSession(capture(pauseSkuIdSlot)) } answers {
            every {
                billingService.getSessionStatus(pauseSkuIdSlot.captured)
            } returns BillingSessionStatus.SESSION_PAUSED
        }

        val resumeSkuIdSlot = slot<SessionSKUIdentifier>()
        every {
            billingService.resumeBillingSession(
                capture(resumeSkuIdSlot),
                capture(resumeBillingServiceErrorCallback),
            )
        } answers {
            every {
                billingService.getSessionStatus(resumeSkuIdSlot.captured)
            } returns BillingSessionStatus.SESSION_ACTIVE
        }

        every {
            billingService.getSessionStatus(any())
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

        skuIdProvider = SkuIdProviderImpl()

        sdkInformation = SdkInformation("test-name", "test-version", "test-package")

        billingController = BillingController(
            navigationSession,
            arrivalProgressObserver,
            tripSession,
            skuIdProvider,
            sdkInformation,
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

        verify(exactly = 0) { billingService.stopBillingSession(any()) }
        verify(exactly = 0) {
            billingService.triggerUserBillingEvent(any(), any(), any())
        }
        verify(exactly = 0) {
            billingService.beginBillingSession(any(), any(), any(), any())
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `when both free drive and active guidance are active, throw an exception`() {
        every {
            billingService.getSessionStatus(SessionSKUIdentifier.NAV3_SES_CORE_AGTRIP)
        } returns BillingSessionStatus.SESSION_ACTIVE
        every {
            billingService.getSessionStatus(SessionSKUIdentifier.NAV3_SES_CORE_FDTRIP)
        } returns BillingSessionStatus.SESSION_ACTIVE
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.Idle)
    }

    @Test
    fun `free drive cycle`() {
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.Idle)
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.FreeDrive("1"))
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.Idle)

        verifyOrder {
            billingService.triggerUserBillingEvent(
                sdkInformation,
                UserSKUIdentifier.NAV3_CORE_MAU,
                any(),
            )
            billingService.beginBillingSession(
                sdkInformation,
                SessionSKUIdentifier.NAV3_SES_CORE_FDTRIP,
                any(),
                TimeUnit.HOURS.toMillis(1),
            )
            billingService.pauseBillingSession(SessionSKUIdentifier.NAV3_SES_CORE_FDTRIP)
        }
    }

    @Test
    fun `active guidance cycle`() {
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.Idle)
        sessionStateObserver.onNavigationSessionStateChanged(
            NavigationSessionState.ActiveGuidance("2"),
        )
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.Idle)

        verifyOrder {
            billingService.triggerUserBillingEvent(
                sdkInformation,
                UserSKUIdentifier.NAV3_CORE_MAU,
                any(),
            )
            billingService.beginBillingSession(
                sdkInformation,
                SessionSKUIdentifier.NAV3_SES_CORE_AGTRIP,
                any(),
                0,
            )
            billingService.pauseBillingSession(SessionSKUIdentifier.NAV3_SES_CORE_AGTRIP)
        }
    }

    @Test
    fun `when active guidance to free drive, start billing session`() {
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.Idle)
        sessionStateObserver.onNavigationSessionStateChanged(
            NavigationSessionState.ActiveGuidance("2"),
        )
        sessionStateObserver.onNavigationSessionStateChanged(
            NavigationSessionState.FreeDrive("1"),
        )

        verifyOrder {
            billingService.beginBillingSession(
                sdkInformation,
                SessionSKUIdentifier.NAV3_SES_CORE_AGTRIP,
                any(),
                0,
            )
            billingService.stopBillingSession(SessionSKUIdentifier.NAV3_SES_CORE_AGTRIP)
            billingService.beginBillingSession(
                sdkInformation,
                SessionSKUIdentifier.NAV3_SES_CORE_FDTRIP,
                any(),
                TimeUnit.HOURS.toMillis(1),
            )
        }
    }

    @Test
    fun `when free drive to active guidance, start billing session`() {
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.Idle)
        sessionStateObserver.onNavigationSessionStateChanged(
            NavigationSessionState.FreeDrive("1"),
        )
        sessionStateObserver.onNavigationSessionStateChanged(
            NavigationSessionState.ActiveGuidance("2"),
        )

        verifyOrder {
            billingService.beginBillingSession(
                sdkInformation,
                SessionSKUIdentifier.NAV3_SES_CORE_FDTRIP,
                any(),
                TimeUnit.HOURS.toMillis(1),
            )
            billingService.stopBillingSession(SessionSKUIdentifier.NAV3_SES_CORE_FDTRIP)
            billingService.beginBillingSession(
                sdkInformation,
                SessionSKUIdentifier.NAV3_SES_CORE_AGTRIP,
                any(),
                0,
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
            billingService.beginBillingSession(
                sdkInformation,
                SessionSKUIdentifier.NAV3_SES_CORE_FDTRIP,
                any(),
                TimeUnit.HOURS.toMillis(1),
            )
            billingService.pauseBillingSession(SessionSKUIdentifier.NAV3_SES_CORE_FDTRIP)
            billingService.resumeBillingSession(SessionSKUIdentifier.NAV3_SES_CORE_FDTRIP, any())
        }
    }

    @Test
    fun `when active guidance session resumed, native APIs called`() {
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.Idle)
        sessionStateObserver.onNavigationSessionStateChanged(
            NavigationSessionState.ActiveGuidance("2"),
        )
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.Idle)
        sessionStateObserver.onNavigationSessionStateChanged(
            NavigationSessionState.ActiveGuidance("2"),
        )

        verifyOrder {
            billingService.beginBillingSession(
                sdkInformation,
                SessionSKUIdentifier.NAV3_SES_CORE_AGTRIP,
                any(),
                0,
            )
            billingService.pauseBillingSession(SessionSKUIdentifier.NAV3_SES_CORE_AGTRIP)
            billingService.resumeBillingSession(SessionSKUIdentifier.NAV3_SES_CORE_AGTRIP, any())
        }
    }

    @Test
    fun `when free drive paused and active guidance started, stop free drive`() {
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.Idle)
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.FreeDrive("1"))
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.Idle)
        sessionStateObserver.onNavigationSessionStateChanged(
            NavigationSessionState.ActiveGuidance("2"),
        )

        verifyOrder {
            billingService.beginBillingSession(
                sdkInformation,
                SessionSKUIdentifier.NAV3_SES_CORE_FDTRIP,
                any(),
                TimeUnit.HOURS.toMillis(1),
            )
            billingService.pauseBillingSession(SessionSKUIdentifier.NAV3_SES_CORE_FDTRIP)
            billingService.stopBillingSession(SessionSKUIdentifier.NAV3_SES_CORE_FDTRIP)
            billingService.beginBillingSession(
                sdkInformation,
                SessionSKUIdentifier.NAV3_SES_CORE_AGTRIP,
                any(),
                0,
            )
        }
    }

    @Test
    fun `when active guidance paused and free drive started, stop active guidance`() {
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.Idle)
        sessionStateObserver.onNavigationSessionStateChanged(
            NavigationSessionState.ActiveGuidance("2"),
        )
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.Idle)
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.FreeDrive("1"))

        verifyOrder {
            billingService.beginBillingSession(
                sdkInformation,
                SessionSKUIdentifier.NAV3_SES_CORE_AGTRIP,
                any(),
                0,
            )
            billingService.pauseBillingSession(SessionSKUIdentifier.NAV3_SES_CORE_AGTRIP)
            billingService.stopBillingSession(SessionSKUIdentifier.NAV3_SES_CORE_AGTRIP)
            billingService.beginBillingSession(
                sdkInformation,
                SessionSKUIdentifier.NAV3_SES_CORE_FDTRIP,
                any(),
                TimeUnit.HOURS.toMillis(1),
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
            BillingServiceError(BillingServiceErrorCode.RESUME_FAILED, "failure"),
        )
        verifyOrder {
            billingService.beginBillingSession(
                sdkInformation,
                SessionSKUIdentifier.NAV3_SES_CORE_FDTRIP,
                any(),
                TimeUnit.HOURS.toMillis(1),
            )
            billingService.pauseBillingSession(SessionSKUIdentifier.NAV3_SES_CORE_FDTRIP)
            billingService.resumeBillingSession(SessionSKUIdentifier.NAV3_SES_CORE_FDTRIP, any())
            billingService.stopBillingSession(SessionSKUIdentifier.NAV3_SES_CORE_FDTRIP)
            billingService.beginBillingSession(
                sdkInformation,
                SessionSKUIdentifier.NAV3_SES_CORE_FDTRIP,
                any(),
                TimeUnit.HOURS.toMillis(1),
            )
        }
    }

    @Test
    fun `when idle and external route set, do nothing`() {
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.Idle)
        every { navigationSession.state } returns NavigationSessionState.Idle
        billingController.onExternalRouteSet(mockk(), 0)
        verify(exactly = 0) {
            billingService.beginBillingSession(any(), any(), any(), any())
        }
    }

    @Test
    fun `when free drive and external route set, do nothing`() {
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.FreeDrive("1"))
        every { navigationSession.state } returns NavigationSessionState.FreeDrive("1")
        billingController.onExternalRouteSet(mockk(), 0)
        verify(exactly = 0) {
            billingService.beginBillingSession(
                sdkInformation,
                SessionSKUIdentifier.NAV3_SES_CORE_AGTRIP,
                any(),
                0,
            )
        }
    }

    @Test
    fun `when active guidance and new external route set, start new billing session`() {
        sessionStateObserver.onNavigationSessionStateChanged(
            NavigationSessionState.ActiveGuidance("2"),
        )

        val originalWaypoints = mutableListOf(
            mockk<Waypoint>(relaxed = true) {
                every { location } returns Point.fromLngLat(0.0, 0.0)
            },
            mockk(relaxed = true) {
                every { location } returns Point.fromLngLat(1.0, 1.0)
            },
        )
        val originalRoute = mockk<NavigationRoute> {
            every { internalWaypoints() } returns originalWaypoints
        }
        val routeProgress = mockk<RouteProgress> {
            every { navigationRoute } returns originalRoute
            every { remainingWaypoints } returns 1
        }
        every { tripSession.getRouteProgress() } returns routeProgress

        val newWaypoints = mutableListOf(
            mockk<Waypoint>(relaxed = true) {
                every { location } returns Point.fromLngLat(0.1, 0.1)
            },
            mockk(relaxed = true) {
                every { location } returns Point.fromLngLat(2.0, 2.0)
            },
        )
        val newRoute = mockk<NavigationRoute> {
            every { internalWaypoints() } returns newWaypoints
        }

        billingController.onExternalRouteSet(newRoute, 0)

        verifyOrder {
            billingService.beginBillingSession(
                sdkInformation,
                SessionSKUIdentifier.NAV3_SES_CORE_AGTRIP,
                any(),
                0,
            )
            billingService.stopBillingSession(SessionSKUIdentifier.NAV3_SES_CORE_AGTRIP)
            billingService.beginBillingSession(
                sdkInformation,
                SessionSKUIdentifier.NAV3_SES_CORE_AGTRIP,
                any(),
                0,
            )
        }
    }

    @Test
    fun `when active guidance and same external route set, do nothing`() {
        sessionStateObserver.onNavigationSessionStateChanged(
            NavigationSessionState.ActiveGuidance("2"),
        )

        val originalWaypoints = mutableListOf(
            mockk<Waypoint>(relaxed = true) {
                every { location } returns Point.fromLngLat(0.0, 0.0)
            },
            mockk(relaxed = true) {
                every { location } returns Point.fromLngLat(1.0, 1.0)
            },
        )
        val originalRoute = mockk<NavigationRoute> {
            every { internalWaypoints() } returns originalWaypoints
        }
        val routeProgress = mockk<RouteProgress> {
            every { navigationRoute } returns originalRoute
            every { remainingWaypoints } returns 1
        }
        every { tripSession.getRouteProgress() } returns routeProgress

        val newWaypoints = mutableListOf(
            mockk<Waypoint>(relaxed = true) {
                every { location } returns Point.fromLngLat(0.1, 0.1)
            },
            // less than 100 meters from original
            mockk(relaxed = true) {
                every { location } returns Point.fromLngLat(1.0005, 1.0005)
            },
        )
        val newRoute = mockk<NavigationRoute> {
            every { internalWaypoints() } returns newWaypoints
        }

        billingController.onExternalRouteSet(newRoute, 0)

        verify(exactly = 1) {
            billingService.beginBillingSession(
                sdkInformation,
                SessionSKUIdentifier.NAV3_SES_CORE_AGTRIP,
                any(),
                0,
            )
        }
        verify(exactly = 0) {
            billingService.stopBillingSession(any())
        }
    }

    @Test
    fun `when active guidance and new external multi-leg route set, start new billing session`() {
        sessionStateObserver.onNavigationSessionStateChanged(
            NavigationSessionState.ActiveGuidance("2"),
        )

        val originalWaypoints = mutableListOf(
            mockk<Waypoint>(relaxed = true) {
                every { location } returns Point.fromLngLat(0.0, 0.0)
            },
            mockk(relaxed = true) {
                every { location } returns Point.fromLngLat(1.0, 1.0)
            },
        )
        val originalRoute = mockk<NavigationRoute> {
            every { internalWaypoints() } returns originalWaypoints
        }
        val routeProgress = mockk<RouteProgress> {
            every { navigationRoute } returns originalRoute
            every { remainingWaypoints } returns 1
        }
        every { tripSession.getRouteProgress() } returns routeProgress

        val newWaypoints = mutableListOf(
            mockk<Waypoint>(relaxed = true) {
                every { location } returns Point.fromLngLat(0.1, 0.1)
            },
            mockk(relaxed = true) {
                every { location } returns Point.fromLngLat(1.1, 1.1)
            },
            mockk(relaxed = true) {
                every { location } returns Point.fromLngLat(1.0, 1.0)
            },
        )
        val newRoute = mockk<NavigationRoute> {
            every { internalWaypoints() } returns newWaypoints
        }

        billingController.onExternalRouteSet(newRoute, 0)

        verifyOrder {
            billingService.beginBillingSession(
                sdkInformation,
                SessionSKUIdentifier.NAV3_SES_CORE_AGTRIP,
                any(),
                0,
            )
            billingService.stopBillingSession(SessionSKUIdentifier.NAV3_SES_CORE_AGTRIP)
            billingService.beginBillingSession(
                sdkInformation,
                SessionSKUIdentifier.NAV3_SES_CORE_AGTRIP,
                any(),
                0,
            )
        }
    }

    @Test
    fun `when active guidance and route set with multi-leg destination not matching, restart`() {
        sessionStateObserver.onNavigationSessionStateChanged(
            NavigationSessionState.ActiveGuidance("2"),
        )

        val originalWaypoints = mutableListOf(
            mockk<Waypoint>(relaxed = true) {
                every { location } returns Point.fromLngLat(0.0, 0.0)
            },
            mockk(relaxed = true) {
                every { location } returns Point.fromLngLat(0.5, 0.5)
            },
            mockk(relaxed = true) {
                every { location } returns Point.fromLngLat(1.0, 1.0)
            },
        )
        val originalRoute = mockk<NavigationRoute> {
            every { internalWaypoints() } returns originalWaypoints
        }
        val routeProgress = mockk<RouteProgress> {
            every { navigationRoute } returns originalRoute
            every { remainingWaypoints } returns 2
        }
        every { tripSession.getRouteProgress() } returns routeProgress

        val newWaypoints = mutableListOf(
            mockk<Waypoint>(relaxed = true) {
                every { location } returns Point.fromLngLat(0.7, 0.7)
            },
            // less than 100 meters from original
            mockk(relaxed = true) {
                every { location } returns Point.fromLngLat(1.0005, 1.0005)
            },
        )
        val newRoute = mockk<NavigationRoute> {
            every { internalWaypoints() } returns newWaypoints
        }

        billingController.onExternalRouteSet(newRoute, 0)

        verifyOrder {
            billingService.beginBillingSession(
                sdkInformation,
                SessionSKUIdentifier.NAV3_SES_CORE_AGTRIP,
                any(),
                0,
            )
            billingService.stopBillingSession(SessionSKUIdentifier.NAV3_SES_CORE_AGTRIP)
            billingService.beginBillingSession(
                sdkInformation,
                SessionSKUIdentifier.NAV3_SES_CORE_AGTRIP,
                any(),
                0,
            )
        }
    }

    @Test
    fun `when active guidance and route set with multi-leg destination matching, do nothing`() {
        sessionStateObserver.onNavigationSessionStateChanged(
            NavigationSessionState.ActiveGuidance("2"),
        )

        val originalWaypoints = mutableListOf(
            mockk<Waypoint>(relaxed = true) {
                every { location } returns Point.fromLngLat(0.0, 0.0)
            },
            mockk(relaxed = true) {
                every { location } returns Point.fromLngLat(0.5, 0.5)
            },
            mockk(relaxed = true) {
                every { location } returns Point.fromLngLat(1.0, 1.0)
            },
        )
        val originalRoute = mockk<NavigationRoute> {
            every { internalWaypoints() } returns originalWaypoints
        }
        val routeProgress = mockk<RouteProgress> {
            every { navigationRoute } returns originalRoute
            every { remainingWaypoints } returns 1
        }
        every { tripSession.getRouteProgress() } returns routeProgress

        val newWaypoints = mutableListOf(
            mockk<Waypoint>(relaxed = true) {
                every { location } returns Point.fromLngLat(0.7, 0.7)
            },
            // less than 100 meters from original
            mockk(relaxed = true) {
                every { location } returns Point.fromLngLat(1.0005, 1.0005)
            },
        )
        val newRoute = mockk<NavigationRoute> {
            every { internalWaypoints() } returns newWaypoints
        }

        billingController.onExternalRouteSet(newRoute, 0)

        verify(exactly = 1) {
            billingService.beginBillingSession(
                sdkInformation,
                SessionSKUIdentifier.NAV3_SES_CORE_AGTRIP,
                any(),
                0,
            )
        }
        verify(exactly = 0) {
            billingService.stopBillingSession(any())
        }
    }

    @Test
    fun `when active guidance completed and external route set, start new billing session`() {
        sessionStateObserver.onNavigationSessionStateChanged(
            NavigationSessionState.ActiveGuidance("2"),
        )

        val originalWaypoints = mutableListOf(
            mockk<Waypoint>(relaxed = true) {
                every { location } returns Point.fromLngLat(0.0, 0.0)
            },
            mockk(relaxed = true) {
                every { location } returns Point.fromLngLat(1.0, 1.0)
            },
        )
        val originalRoute = mockk<NavigationRoute> {
            every { internalWaypoints() } returns originalWaypoints
        }
        val routeProgress = mockk<RouteProgress> {
            every { navigationRoute } returns originalRoute
            every { remainingWaypoints } returns 0
        }
        every { tripSession.getRouteProgress() } returns routeProgress

        val newWaypoints = mutableListOf(
            mockk<Waypoint>(relaxed = true) {
                every { location } returns Point.fromLngLat(0.1, 0.1)
            },
            mockk(relaxed = true) {
                every { location } returns Point.fromLngLat(2.0, 2.0)
            },
        )
        val newRoute = mockk<NavigationRoute> {
            every { internalWaypoints() } returns newWaypoints
        }

        billingController.onExternalRouteSet(newRoute, 0)

        verifyOrder {
            billingService.beginBillingSession(
                sdkInformation,
                SessionSKUIdentifier.NAV3_SES_CORE_AGTRIP,
                any(),
                0,
            )
            billingService.stopBillingSession(SessionSKUIdentifier.NAV3_SES_CORE_AGTRIP)
            billingService.beginBillingSession(
                sdkInformation,
                SessionSKUIdentifier.NAV3_SES_CORE_AGTRIP,
                any(),
                0,
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
            NavigationSessionState.ActiveGuidance("2"),
        )

        val originalWaypoints = mutableListOf(
            mockk<Waypoint>(relaxed = true) {
                every { location } returns Point.fromLngLat(0.0, 0.0)
            },
            mockk(relaxed = true) {
                every { location } returns Point.fromLngLat(1.0, 1.0)
            },
        )
        val originalRoute = mockk<NavigationRoute> {
            every { internalWaypoints() } returns originalWaypoints
        }
        val routeProgress = mockk<RouteProgress> {
            every { navigationRoute } returns originalRoute
            every { remainingWaypoints } returns 2
        }
        every { tripSession.getRouteProgress() } returns routeProgress

        val newWaypoints = mutableListOf(
            mockk<Waypoint>(relaxed = true) {
                every { location } returns Point.fromLngLat(0.1, 0.1)
            },
            mockk(relaxed = true) {
                every { location } returns Point.fromLngLat(1.0005, 1.0005)
            },
        )
        val newRoute = mockk<NavigationRoute> {
            every { internalWaypoints() } returns newWaypoints
        }

        billingController.onExternalRouteSet(newRoute, 0)

        verify(exactly = 1) {
            billingService.beginBillingSession(
                sdkInformation,
                SessionSKUIdentifier.NAV3_SES_CORE_AGTRIP,
                any(),
                0,
            )
        }
        verify(exactly = 0) {
            billingService.stopBillingSession(any())
        }
    }

    @Test
    fun `active guidance paused and new external route set, start and pause new billing session`() {
        sessionStateObserver.onNavigationSessionStateChanged(
            NavigationSessionState.ActiveGuidance("2"),
        )
        sessionStateObserver.onNavigationSessionStateChanged(
            NavigationSessionState.Idle,
        )
        every { navigationSession.state } returns NavigationSessionState.Idle

        val originalWaypoints = mutableListOf(
            mockk<Waypoint>(relaxed = true) {
                every { location } returns Point.fromLngLat(0.0, 0.0)
            },
            mockk(relaxed = true) {
                every { location } returns Point.fromLngLat(1.0, 1.0)
            },
        )
        val originalRoute = mockk<NavigationRoute> {
            every { internalWaypoints() } returns originalWaypoints
        }
        val routeProgress = mockk<RouteProgress> {
            every { navigationRoute } returns originalRoute
            every { remainingWaypoints } returns 1
        }
        every { tripSession.getRouteProgress() } returns routeProgress

        val newWaypoints = mutableListOf(
            mockk<Waypoint>(relaxed = true) {
                every { location } returns Point.fromLngLat(0.1, 0.1)
            },
            mockk(relaxed = true) {
                every { location } returns Point.fromLngLat(2.0, 2.0)
            },
        )
        val newRoute = mockk<NavigationRoute> {
            every { internalWaypoints() } returns newWaypoints
        }

        billingController.onExternalRouteSet(newRoute, 0)

        verifyOrder {
            billingService.beginBillingSession(
                sdkInformation,
                SessionSKUIdentifier.NAV3_SES_CORE_AGTRIP,
                any(),
                0,
            )
            billingService.pauseBillingSession(SessionSKUIdentifier.NAV3_SES_CORE_AGTRIP)
            billingService.stopBillingSession(SessionSKUIdentifier.NAV3_SES_CORE_AGTRIP)
            billingService.beginBillingSession(
                sdkInformation,
                SessionSKUIdentifier.NAV3_SES_CORE_AGTRIP,
                any(),
                0,
            )
            billingService.pauseBillingSession(SessionSKUIdentifier.NAV3_SES_CORE_AGTRIP)
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
            NavigationSessionState.ActiveGuidance("2"),
        )
        arrivalObserver.onNextRouteLegStart(mockk())
        arrivalObserver.onNextRouteLegStart(mockk())

        verify(exactly = 3) {
            billingService.beginBillingSession(
                sdkInformation,
                SessionSKUIdentifier.NAV3_SES_CORE_AGTRIP,
                any(),
                0,
            )
        }
    }

    @Test
    fun `on destroy stops running free drive session`() {
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.Idle)
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.FreeDrive("1"))
        billingController.onDestroy()

        verify {
            billingService.stopBillingSession(SessionSKUIdentifier.NAV3_SES_CORE_FDTRIP)
        }
    }

    @Test
    fun `on destroy stops running active guidance session`() {
        sessionStateObserver.onNavigationSessionStateChanged(NavigationSessionState.Idle)
        sessionStateObserver.onNavigationSessionStateChanged(
            NavigationSessionState.ActiveGuidance("2"),
        )
        billingController.onDestroy()

        verify {
            billingService.stopBillingSession(SessionSKUIdentifier.NAV3_SES_CORE_AGTRIP)
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
        unmockkObject(BillingServiceProvider)
    }
}
