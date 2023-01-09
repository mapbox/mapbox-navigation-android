package com.mapbox.navigation.core.trip

import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.common.LoggingLevel
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.utils.internal.LoggerFrontend
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    private val sut = MapboxTripStarter()

    @Before
    fun setup() {
        mockkStatic(PermissionsManager::class)
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
    fun `refreshLocationPermissions will startTripSession after onAttached`() {
        every { PermissionsManager.areLocationPermissionsGranted(any()) } returns false

        val mapboxNavigation = mockMapboxNavigation()
        sut.onAttached(mapboxNavigation)
        every { PermissionsManager.areLocationPermissionsGranted(any()) } returns true
        sut.refreshLocationPermissions()

        verify(exactly = 1) { mapboxNavigation.startTripSession() }
    }

    @Test
    fun `enableReplayRoute will startReplayTripSession without location permissions`() {
        every { PermissionsManager.areLocationPermissionsGranted(any()) } returns false

        val mapboxNavigation = mockMapboxNavigation()
        sut.enableReplayRoute()
        sut.onAttached(mapboxNavigation)

        verify(exactly = 1) { mapboxNavigation.startReplayTripSession() }
    }

    @Test
    fun `enableReplayRoute will resetTripSession when the options change`() {
        every { PermissionsManager.areLocationPermissionsGranted(any()) } returns false

        val mapboxNavigation = mockMapboxNavigation()
        sut.enableReplayRoute()
        sut.onAttached(mapboxNavigation)
        val nextOptions = sut.getReplayRouteSessionOptions().toBuilder()
            .decodeMinDistance(Double.MAX_VALUE)
            .build()
        sut.enableReplayRoute(nextOptions)

        verifyOrder {
            mapboxNavigation.startReplayTripSession()
            mapboxNavigation.resetTripSession(any())
            mapboxNavigation.startReplayTripSession()
            mapboxNavigation.resetTripSession(any())
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
            mapboxNavigation.startReplayTripSession()
            mapboxNavigation.startTripSession()
            mapboxNavigation.stopTripSession()
        }
    }

    @Test
    fun `setLocationPermissionGranted will not restart startReplayTripSession`() {
        every { PermissionsManager.areLocationPermissionsGranted(any()) } returns false

        val mapboxNavigation = mockMapboxNavigation()
        sut.enableReplayRoute()
        sut.onAttached(mapboxNavigation)
        every { PermissionsManager.areLocationPermissionsGranted(any()) } returns true
        sut.refreshLocationPermissions()

        verify(exactly = 1) { mapboxNavigation.startReplayTripSession() }
    }

    @Test
    fun `update will not stop a trip session that has been started`() {
        val mapboxNavigation = mockMapboxNavigation()
        every { mapboxNavigation.getTripSessionState() } returns TripSessionState.STARTED
        every { mapboxNavigation.isReplayEnabled() } returns false

        sut.onAttached(mapboxNavigation)
        sut.enableReplayRoute()

        verify(exactly = 0) { mapboxNavigation.stopTripSession() }
        verify(exactly = 1) { mapboxNavigation.startReplayTripSession() }
    }

    @Test
    fun `update before onAttached will not startTripSession`() {
        val mapboxNavigation = mockMapboxNavigation()

        sut.enableReplayRoute()
        sut.onAttached(mapboxNavigation)

        verify(exactly = 0) { mapboxNavigation.stopTripSession() }
        verify(exactly = 0) { mapboxNavigation.startTripSession() }
        verify(exactly = 1) { mapboxNavigation.startReplayTripSession() }
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
                mockk { every { navigationRoutes } returns emptyList() }
            )
        }
        return mapboxNavigation
    }
}
