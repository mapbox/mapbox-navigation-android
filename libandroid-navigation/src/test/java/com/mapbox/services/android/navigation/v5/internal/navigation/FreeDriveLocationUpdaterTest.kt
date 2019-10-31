package com.mapbox.services.android.navigation.v5.internal.navigation

import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.services.android.navigation.v5.navigation.OfflineNavigator
import com.mapbox.services.android.navigation.v5.navigation.OnOfflineTilesConfiguredCallback
import io.mockk.mockk
import io.mockk.verify
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import org.junit.Test

class FreeDriveLocationUpdaterTest {

    @Test
    fun `checks OfflineNavigator#configure is called when configure`() {
        val mockedOfflineNavigator: OfflineNavigator = mockk<OfflineNavigator>(relaxed = true)
        val theFreeDriveLocationUpdater =
            buildFreeDriveLocationUpdater(offlineNavigator = mockedOfflineNavigator)
        val anyTilePath = "a/tile/path/version"
        val anyOfflineTilesConfiguredCallback = mockk<OnOfflineTilesConfiguredCallback>()

        theFreeDriveLocationUpdater.configure(
            anyTilePath,
            anyOfflineTilesConfiguredCallback
        )

        verify {
            mockedOfflineNavigator.configure(
                tilePath = "a/tile/path/version",
                callback = anyOfflineTilesConfiguredCallback
            )
        }
    }

    @Test
    fun `checks requestLocationUpdates is called when start`() {
        val mockedLocationEngine = mockk<LocationEngine>(relaxed = true)
        val mockedLocationEngineRequest = mockk<LocationEngineRequest>(relaxed = true)
        val theFreeDriveLocationUpdater =
            buildFreeDriveLocationUpdater(
                locationEngine = mockedLocationEngine,
                locationEngineRequest = mockedLocationEngineRequest
            )

        theFreeDriveLocationUpdater.start()

        verify {
            mockedLocationEngine.requestLocationUpdates(
                eq(mockedLocationEngineRequest),
                any<LocationEngineCallback<LocationEngineResult>>(),
                null
            )
        }
    }

    @Test
    fun `checks scheduleAtFixedRate is called when start`() {
        val mockedScheduledExecutorService = mockk<ScheduledExecutorService>(relaxed = true)
        val theFreeDriveLocationUpdater =
            buildFreeDriveLocationUpdater(
                executorService = mockedScheduledExecutorService
            )

        theFreeDriveLocationUpdater.start()

        verify {
            mockedScheduledExecutorService.scheduleAtFixedRate(
                any(),
                eq(1500),
                eq(1000),
                eq(TimeUnit.MILLISECONDS)
            )
        }
    }

    @Test
    fun `checks requestLocationUpdates is called only once if start is called multiple times`() {
        val mockedLocationEngine = mockk<LocationEngine>(relaxed = true)
        val mockedLocationEngineRequest = mockk<LocationEngineRequest>(relaxed = true)
        val theFreeDriveLocationUpdater =
            buildFreeDriveLocationUpdater(
                locationEngine = mockedLocationEngine,
                locationEngineRequest = mockedLocationEngineRequest
            )

        theFreeDriveLocationUpdater.start()
        theFreeDriveLocationUpdater.start()
        theFreeDriveLocationUpdater.start()

        verify(exactly = 1) {
            mockedLocationEngine.requestLocationUpdates(
                eq(mockedLocationEngineRequest),
                any<LocationEngineCallback<LocationEngineResult>>(),
                null
            )
        }
    }

    @Test
    fun `checks scheduleAtFixedRate is called only once if start is called multiple times`() {
        val mockedScheduledExecutorService = mockk<ScheduledExecutorService>(relaxed = true)
        val theFreeDriveLocationUpdater =
            buildFreeDriveLocationUpdater(
                executorService = mockedScheduledExecutorService
            )

        theFreeDriveLocationUpdater.start()
        theFreeDriveLocationUpdater.start()
        theFreeDriveLocationUpdater.start()

        verify(exactly = 1) {
            mockedScheduledExecutorService.scheduleAtFixedRate(
                any(),
                eq(1500),
                eq(1000),
                eq(TimeUnit.MILLISECONDS)
            )
        }
    }

    @Test
    fun `checks removeLocationUpdates is called when stop if previously started`() {
        val mockedLocationEngine = mockk<LocationEngine>(relaxed = true)
        val theFreeDriveLocationUpdater =
            buildFreeDriveLocationUpdater(
                locationEngine = mockedLocationEngine
            )

        theFreeDriveLocationUpdater.start()
        theFreeDriveLocationUpdater.stop()

        verify {
            mockedLocationEngine.removeLocationUpdates(
                any<LocationEngineCallback<LocationEngineResult>>()
            )
        }
    }

    @Test
    fun `checks removeLocationUpdates is not called when stop if not previously started`() {
        val mockedLocationEngine = mockk<LocationEngine>(relaxed = true)
        val theFreeDriveLocationUpdater =
            buildFreeDriveLocationUpdater(
                locationEngine = mockedLocationEngine
            )

        theFreeDriveLocationUpdater.stop()

        verify(exactly = 0) {
            mockedLocationEngine.removeLocationUpdates(
                any<LocationEngineCallback<LocationEngineResult>>()
            )
        }
    }

    @Test
    fun `checks removeLocationUpdates is called when kill if previously started`() {
        val mockedLocationEngine = mockk<LocationEngine>(relaxed = true)
        val theFreeDriveLocationUpdater =
            buildFreeDriveLocationUpdater(
                locationEngine = mockedLocationEngine
            )

        theFreeDriveLocationUpdater.start()
        theFreeDriveLocationUpdater.kill()

        verify {
            mockedLocationEngine.removeLocationUpdates(
                any<LocationEngineCallback<LocationEngineResult>>()
            )
        }
    }

    @Test
    fun `checks removeLocationUpdates is not called when kill if not previously started`() {
        val mockedLocationEngine = mockk<LocationEngine>(relaxed = true)
        val theFreeDriveLocationUpdater =
            buildFreeDriveLocationUpdater(
                locationEngine = mockedLocationEngine
            )

        theFreeDriveLocationUpdater.kill()

        verify(exactly = 0) {
            mockedLocationEngine.removeLocationUpdates(
                any<LocationEngineCallback<LocationEngineResult>>()
            )
        }
    }

    @Test
    fun `checks shutdown is called when kill`() {
        val mockedScheduledExecutorService = mockk<ScheduledExecutorService>(relaxed = true)
        val theFreeDriveLocationUpdater =
            buildFreeDriveLocationUpdater(
                executorService = mockedScheduledExecutorService
            )

        theFreeDriveLocationUpdater.kill()

        verify {
            mockedScheduledExecutorService.shutdown()
        }
    }

    @Test
    fun `checks stop and start are called when updateLocationEngine if previously started`() {
        val aLocationEngine = mockk<LocationEngine>(relaxed = true)
        val mockedLocationEngineRequest = mockk<LocationEngineRequest>(relaxed = true)
        val theFreeDriveLocationUpdater =
            buildFreeDriveLocationUpdater(
                locationEngine = aLocationEngine,
                locationEngineRequest = mockedLocationEngineRequest
            )
        val anotherLocationEngine = mockk<LocationEngine>(relaxed = true)

        theFreeDriveLocationUpdater.start()
        theFreeDriveLocationUpdater.updateLocationEngine(anotherLocationEngine)

        verify {
            aLocationEngine.requestLocationUpdates(
                eq(mockedLocationEngineRequest),
                any<LocationEngineCallback<LocationEngineResult>>(),
                null
            )
        }
        verify {
            aLocationEngine.removeLocationUpdates(
                any<LocationEngineCallback<LocationEngineResult>>()
            )
        }
        verify {
            anotherLocationEngine.requestLocationUpdates(
                eq(mockedLocationEngineRequest),
                any<LocationEngineCallback<LocationEngineResult>>(),
                null
            )
        }
    }

    @Test
    fun `checks stop and start are not called when updateLocationEngine if not previously started`() {
        val aLocationEngine = mockk<LocationEngine>(relaxed = true)
        val mockedLocationEngineRequest = mockk<LocationEngineRequest>(relaxed = true)
        val theFreeDriveLocationUpdater =
            buildFreeDriveLocationUpdater(
                locationEngine = aLocationEngine,
                locationEngineRequest = mockedLocationEngineRequest
            )
        val anotherLocationEngine = mockk<LocationEngine>(relaxed = true)

        theFreeDriveLocationUpdater.updateLocationEngine(anotherLocationEngine)

        verify(exactly = 0) {
            aLocationEngine.requestLocationUpdates(
                eq(mockedLocationEngineRequest),
                any<LocationEngineCallback<LocationEngineResult>>(),
                null
            )
        }
        verify(exactly = 0) {
            aLocationEngine.removeLocationUpdates(
                any<LocationEngineCallback<LocationEngineResult>>()
            )
        }
        verify(exactly = 0) {
            anotherLocationEngine.requestLocationUpdates(
                eq(mockedLocationEngineRequest),
                any<LocationEngineCallback<LocationEngineResult>>(),
                null
            )
        }
    }

    @Test
    fun `checks stop and start are called when updateLocationEngineRequest if previously started`() {
        val mockedLocationEngine = mockk<LocationEngine>(relaxed = true)
        val aLocationEngineRequest = mockk<LocationEngineRequest>(relaxed = true)
        val theFreeDriveLocationUpdater =
            buildFreeDriveLocationUpdater(
                locationEngine = mockedLocationEngine,
                locationEngineRequest = aLocationEngineRequest
            )
        val anotherLocationEngineRequest = mockk<LocationEngineRequest>(relaxed = true)

        theFreeDriveLocationUpdater.start()
        theFreeDriveLocationUpdater.updateLocationEngineRequest(anotherLocationEngineRequest)

        verify {
            mockedLocationEngine.requestLocationUpdates(
                eq(aLocationEngineRequest),
                any<LocationEngineCallback<LocationEngineResult>>(),
                null
            )
        }
        verify {
            mockedLocationEngine.removeLocationUpdates(
                any<LocationEngineCallback<LocationEngineResult>>()
            )
        }
        verify {
            mockedLocationEngine.requestLocationUpdates(
                eq(anotherLocationEngineRequest),
                any<LocationEngineCallback<LocationEngineResult>>(),
                null
            )
        }
    }

    @Test
    fun `checks stop and start are not called when updateLocationEngineRequest if not previously started`() {
        val mockedLocationEngine = mockk<LocationEngine>(relaxed = true)
        val aLocationEngineRequest = mockk<LocationEngineRequest>(relaxed = true)
        val theFreeDriveLocationUpdater =
            buildFreeDriveLocationUpdater(
                locationEngine = mockedLocationEngine,
                locationEngineRequest = aLocationEngineRequest
            )
        val anotherLocationEngineRequest = mockk<LocationEngineRequest>(relaxed = true)

        theFreeDriveLocationUpdater.updateLocationEngineRequest(anotherLocationEngineRequest)

        verify(exactly = 0) {
            mockedLocationEngine.requestLocationUpdates(
                eq(aLocationEngineRequest),
                any<LocationEngineCallback<LocationEngineResult>>(),
                null
            )
        }
        verify(exactly = 0) {
            mockedLocationEngine.removeLocationUpdates(
                any<LocationEngineCallback<LocationEngineResult>>()
            )
        }
        verify(exactly = 0) {
            mockedLocationEngine.requestLocationUpdates(
                eq(anotherLocationEngineRequest),
                any<LocationEngineCallback<LocationEngineResult>>(),
                null
            )
        }
    }

    private fun buildFreeDriveLocationUpdater(
        locationEngine: LocationEngine = mockk<LocationEngine>(relaxed = true),
        locationEngineRequest: LocationEngineRequest = mockk<LocationEngineRequest>(relaxed = true),
        navigationEventDispatcher: NavigationEventDispatcher = mockk<NavigationEventDispatcher>(
            relaxed = true
        ),
        mapboxNavigator: MapboxNavigator = mockk<MapboxNavigator>(relaxed = true),
        offlineNavigator: OfflineNavigator = mockk<OfflineNavigator>(relaxed = true),
        executorService: ScheduledExecutorService = mockk<ScheduledExecutorService>(relaxed = true)
    ): FreeDriveLocationUpdater {
        return FreeDriveLocationUpdater(
            locationEngine,
            locationEngineRequest,
            navigationEventDispatcher,
            mapboxNavigator,
            offlineNavigator,
            executorService
        )
    }
}
