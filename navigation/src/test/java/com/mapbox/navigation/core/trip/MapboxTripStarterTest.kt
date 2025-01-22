package com.mapbox.navigation.core.trip

import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.common.LoggingLevel
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.replay.history.ReplayHistorySession
import com.mapbox.navigation.core.replay.history.ReplayHistorySessionOptions
import com.mapbox.navigation.core.replay.route.ReplayRouteSession
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.utils.internal.LoggerFrontend
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
class MapboxTripStarterTest {

    private val infoLogSlot = slot<String>()
    private val logger: LoggerFrontend = mockk {
        every { getLogLevel() } returns LoggingLevel.INFO
        every { logI(capture(infoLogSlot), any()) } just runs
    }

    @get:Rule
    val logRule = LoggingFrontendTestRule(logger)

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val replayRouteSession = mockk<ReplayRouteSession>(relaxed = true)
    private var historyOptions = MutableStateFlow(ReplayHistorySessionOptions.Builder().build())
    private val replayHistorySession = mockk<ReplayHistorySession>(relaxed = true) {
        every { getOptions() } returns historyOptions
        every { setOptions(any()) } answers {
            historyOptions.value = firstArg()
        }
    }

    private val sut = MapboxTripStarter(
        mockk {
            every { getReplayRouteSession() } returns replayRouteSession
            every { getReplayHistorySession() } returns replayHistorySession
        },
    )

    @Before
    fun setup() {
        mockkStatic(PermissionsManager::class)
        mockkObject(PermissionsManager)
        every { PermissionsManager.areLocationPermissionsGranted(any()) } returns true
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `onAttached will startTripSession when location permissions are granted`() {
        every { PermissionsManager.areLocationPermissionsGranted(any()) } returns true

        val mapboxNavigation = mockMapboxNavigation()
        sut.onAttached(mapboxNavigation)

        verify(exactly = 1) { mapboxNavigation.startTripSession() }
    }

    @Test
    fun `onAttached will not startTripSession when location permissions are disabled`() {
        every { PermissionsManager.areLocationPermissionsGranted(any()) } returns false

        val mapboxNavigation = mockMapboxNavigation()
        sut.onAttached(mapboxNavigation)

        verify(exactly = 0) { mapboxNavigation.startTripSession() }
    }

    @Test
    fun `onAttached will not emit log when location permissions are granted`() {
        every { PermissionsManager.areLocationPermissionsGranted(any()) } returns true

        val mapboxNavigation = mockMapboxNavigation()
        sut.onAttached(mapboxNavigation)

        verify(exactly = 1) { mapboxNavigation.startTripSession() }
        verify(exactly = 0) { logger.logI(any(), any()) }
    }

    @Test
    fun `onAttached will emit log when location permissions are not granted`() {
        every { PermissionsManager.areLocationPermissionsGranted(any()) } returns false

        val mapboxNavigation = mockMapboxNavigation()
        sut.onAttached(mapboxNavigation)

        verify(exactly = 0) { mapboxNavigation.startTripSession() }
        assertTrue(infoLogSlot.captured.contains("startTripSession was not called"))
    }

    @Test
    fun `enableMapMatching will emit log when location permissions are not granted`() {
        every { PermissionsManager.areLocationPermissionsGranted(any()) } returns false

        val mapboxNavigation = mockMapboxNavigation()
        sut.enableReplayRoute()
        sut.onAttached(mapboxNavigation)
        sut.enableMapMatching()

        verify(exactly = 0) { mapboxNavigation.startTripSession() }
        assertTrue(infoLogSlot.captured.contains("startTripSession was not called"))
    }

    @Test
    fun `enableMapMatching after replay route will resetTripSession`() {
        every { PermissionsManager.areLocationPermissionsGranted(any()) } returns true

        val mapboxNavigation = mockMapboxNavigation()
        sut.enableReplayRoute()
        sut.onAttached(mapboxNavigation)
        sut.enableMapMatching()

        verifyOrder {
            replayRouteSession.onAttached(mapboxNavigation)
            replayRouteSession.onDetached(mapboxNavigation)
            mapboxNavigation.startTripSession()
            mapboxNavigation.resetTripSession(any())
        }
        verify(exactly = 0) { mapboxNavigation.stopTripSession() }
    }

    @Test
    fun `enableMapMatching after replay history will resetTripSession`() {
        every { PermissionsManager.areLocationPermissionsGranted(any()) } returns true

        val mapboxNavigation = mockMapboxNavigation()
        sut.enableReplayHistory()
        sut.onAttached(mapboxNavigation)
        sut.enableMapMatching()

        verifyOrder {
            replayHistorySession.onAttached(mapboxNavigation)
            replayHistorySession.onDetached(mapboxNavigation)
            mapboxNavigation.startTripSession()
            mapboxNavigation.resetTripSession(any())
        }
        verify(exactly = 0) { mapboxNavigation.stopTripSession() }
    }

    @Test
    fun `refreshLocationPermissions will startTripSession after onAttached`() {
        every { PermissionsManager.areLocationPermissionsGranted(any()) } returns false

        val mapboxNavigation = mockMapboxNavigation()
        sut.onAttached(mapboxNavigation)
        every { PermissionsManager.areLocationPermissionsGranted(any()) } returns true
        sut.refreshLocationPermissions()

        verify(exactly = 1) { mapboxNavigation.startTripSession() }
    }

    @Test
    fun `enableReplayRoute will attach ReplayRouteSession without location permissions`() {
        every { PermissionsManager.areLocationPermissionsGranted(any()) } returns false

        val mapboxNavigation = mockMapboxNavigation()
        sut.enableReplayRoute()
        sut.onAttached(mapboxNavigation)

        verify(exactly = 1) { replayRouteSession.onAttached(mapboxNavigation) }
        verify(exactly = 0) { replayRouteSession.onDetached(mapboxNavigation) }
    }

    @Test
    fun `enableReplayRoute will set options before onAttached`() {
        every { PermissionsManager.areLocationPermissionsGranted(any()) } returns false

        val mapboxNavigation = mockMapboxNavigation()
        sut.enableReplayRoute()
        sut.onAttached(mapboxNavigation)
        val nextOptions = sut.getReplayRouteSessionOptions().toBuilder()
            .decodeMinDistance(Double.MAX_VALUE)
            .build()
        sut.enableReplayRoute(nextOptions)

        verifyOrder {
            replayRouteSession.setOptions(any())
            replayRouteSession.onAttached(mapboxNavigation)
            replayRouteSession.onDetached(mapboxNavigation)
            replayRouteSession.setOptions(nextOptions)
            replayRouteSession.onAttached(mapboxNavigation)
        }
        verify(exactly = 0) { mapboxNavigation.stopTripSession() }
    }

    @Test
    fun `enableMapMatching can be used to switch to regular trip session`() {
        every { PermissionsManager.areLocationPermissionsGranted(any()) } returns true

        val mapboxNavigation = mockMapboxNavigation()
        sut.enableReplayRoute()
        sut.onAttached(mapboxNavigation)
        sut.enableMapMatching()
        sut.onDetached(mapboxNavigation)

        verifyOrder {
            replayHistorySession.onDetached(mapboxNavigation)
            replayRouteSession.onAttached(mapboxNavigation)
            replayRouteSession.onDetached(mapboxNavigation)
            mapboxNavigation.startTripSession()
            mapboxNavigation.stopTripSession()
        }
    }

    @Test
    fun `setLocationPermissionGranted will not restart ReplayRouteSession`() {
        every { PermissionsManager.areLocationPermissionsGranted(any()) } returns false

        val mapboxNavigation = mockMapboxNavigation()
        sut.enableReplayRoute()
        sut.onAttached(mapboxNavigation)
        every { PermissionsManager.areLocationPermissionsGranted(any()) } returns true
        sut.refreshLocationPermissions()

        verify(exactly = 1) { replayRouteSession.onAttached(mapboxNavigation) }
    }

    @Test
    fun `enableReplayRoute will not stop a trip session that has been started`() {
        val mapboxNavigation = mockMapboxNavigation()
        every { mapboxNavigation.getTripSessionState() } returns TripSessionState.STARTED
        every { mapboxNavigation.isReplayEnabled() } returns false

        sut.onAttached(mapboxNavigation)
        sut.enableReplayRoute()

        verify(exactly = 0) { mapboxNavigation.stopTripSession() }
        verify(exactly = 1) { replayRouteSession.onAttached(mapboxNavigation) }
    }

    @Test
    fun `enableReplayRoute before onAttached will not startTripSession`() {
        val mapboxNavigation = mockMapboxNavigation()

        sut.enableReplayRoute()
        sut.onAttached(mapboxNavigation)

        verify(exactly = 0) { mapboxNavigation.stopTripSession() }
        verify(exactly = 0) { mapboxNavigation.startTripSession() }
        verify(exactly = 1) { replayRouteSession.onAttached(mapboxNavigation) }
    }

    @Test
    fun `enableReplayHistory before onAttached will not startTripSession`() {
        val mapboxNavigation = mockMapboxNavigation()

        sut.enableReplayHistory()
        sut.onAttached(mapboxNavigation)

        verify(exactly = 0) { mapboxNavigation.stopTripSession() }
        verify(exactly = 0) { mapboxNavigation.startTripSession() }
        verify(exactly = 1) { replayHistorySession.onAttached(mapboxNavigation) }
    }

    @Test
    fun `enableReplayHistory will set options before onAttached`() {
        every { PermissionsManager.areLocationPermissionsGranted(any()) } returns false

        val mapboxNavigation = mockMapboxNavigation()
        sut.enableReplayHistory()
        sut.onAttached(mapboxNavigation)
        val nextOptions = sut.getReplayHistorySessionOptions().toBuilder()
            .enableSetRoute(false)
            .build()
        sut.enableReplayHistory(nextOptions)

        verifyOrder {
            replayHistorySession.setOptions(any())
            replayHistorySession.onAttached(mapboxNavigation)
            replayHistorySession.setOptions(nextOptions)
        }
        verify(exactly = 0) {
            mapboxNavigation.stopTripSession()
            replayHistorySession.onDetached(mapboxNavigation)
        }
    }

    private fun mockMapboxNavigation(): MapboxNavigation {
        val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        every { mapboxNavigation.getTripSessionState() } returns TripSessionState.STOPPED
        every { mapboxNavigation.startReplayTripSession() } answers {
            every { mapboxNavigation.isReplayEnabled() } returns true
            every { mapboxNavigation.getTripSessionState() } returns TripSessionState.STARTED
        }
        every { mapboxNavigation.startTripSession() } answers {
            every { mapboxNavigation.isReplayEnabled() } returns false
            every { mapboxNavigation.getTripSessionState() } returns TripSessionState.STARTED
        }
        every { mapboxNavigation.stopTripSession() } answers {
            every { mapboxNavigation.isReplayEnabled() } returns false
            every { mapboxNavigation.getTripSessionState() } returns TripSessionState.STOPPED
        }
        every { mapboxNavigation.registerRoutesObserver(any()) } answers {
            firstArg<RoutesObserver>().onRoutesChanged(
                mockk { every { navigationRoutes } returns emptyList() },
            )
        }
        return mapboxNavigation
    }
}
