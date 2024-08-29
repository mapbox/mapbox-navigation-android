package com.mapbox.navigation.core.trip.session

import android.os.Looper
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.common.Cancelable
import com.mapbox.common.location.DeviceLocationProvider
import com.mapbox.common.location.DeviceLocationProviderFactory
import com.mapbox.common.location.Location
import com.mapbox.common.location.LocationError
import com.mapbox.common.location.LocationErrorCode
import com.mapbox.common.location.LocationProvider
import com.mapbox.common.location.LocationProviderRequest
import com.mapbox.common.location.LocationService
import com.mapbox.common.location.LocationServiceFactory
import com.mapbox.navigation.base.options.LocationOptions
import com.mapbox.navigation.core.replay.ReplayLocationProvider
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.utils.internal.LoggerFrontend
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class TripSessionLocationEngineTest {

    private val logger = mockk<LoggerFrontend>(relaxed = true)

    @get:Rule
    val loggerRule = LoggingFrontendTestRule(logger)

    private val defaultDeviceLocationProvider = mockk<DeviceLocationProvider>(relaxed = true).also {
        mockLocationProvider(it)
    }
    private val replayLocationProvider = mockk<ReplayLocationProvider>(relaxed = true).also {
        mockLocationProvider(it)
    }
    private val locationOptions = LocationOptions.Builder().build()
    private val locationService = mockk<LocationService>(relaxUnitFun = true) {
        every {
            getDeviceLocationProvider(any<LocationProviderRequest>())
        } returns ExpectedFactory.createValue(defaultDeviceLocationProvider)
    }

    private lateinit var sut: TripSessionLocationEngine

    @Before
    fun setUp() {
        mockkStatic(LocationServiceFactory::class)
        every { LocationServiceFactory.getOrCreate() } returns locationService
    }

    @After
    fun tearDown() {
        unmockkStatic(LocationServiceFactory::class)
    }

    @Test
    fun `should request location updates from navigation options when replay is disabled`() {
        sut = TripSessionLocationEngine(locationOptions) {
            replayLocationProvider
        }

        val onRawLocationUpdate: (Location) -> Unit = mockk()
        sut.startLocationUpdates(false, onRawLocationUpdate)

        verify(exactly = 1) {
            defaultDeviceLocationProvider.addLocationObserver(any(), any())
        }
        verify(exactly = 0) {
            locationService.setUserDefinedDeviceLocationProviderFactory(any())
        }
    }

    @Test
    fun `should not request location updates from navigation options when error is returned`() {
        every { LocationServiceFactory.getOrCreate() } returns mockk {
            every {
                getDeviceLocationProvider(any<LocationProviderRequest>())
            } returns ExpectedFactory.createError(
                LocationError(LocationErrorCode.FAILED_TO_DETECT_LOCATION, "Some error"),
            )
        }
        sut = TripSessionLocationEngine(locationOptions) {
            replayLocationProvider
        }

        val onRawLocationUpdate: (Location) -> Unit = mockk()
        sut.startLocationUpdates(false, onRawLocationUpdate)

        verify(exactly = 0) {
            defaultDeviceLocationProvider.addLocationObserver(any(), any())
        }
        verify(exactly = 1) {
            logger.logW(
                "TripSessionLocationEngine",
                "Location updates are not possible: " +
                    "could not find suitable location provider. " +
                    "Error code: FailedToDetectLocation, " +
                    "message: Some error.",
            )
        }
        verify(exactly = 0) {
            locationService.setUserDefinedDeviceLocationProviderFactory(any())
        }
    }

    @Test
    fun `should request location updates from custom real provider when replay is disabled`() {
        val customLocationProvider = mockk<DeviceLocationProvider>(relaxed = true)
        val customLocationProviderFactory = DeviceLocationProviderFactory {
            ExpectedFactory.createValue(customLocationProvider)
        }
        every {
            locationService.getDeviceLocationProvider(any<LocationProviderRequest>())
        } returns ExpectedFactory.createValue(customLocationProvider)
        val locationOptions = LocationOptions.Builder()
            .locationProviderFactory(
                customLocationProviderFactory,
                LocationOptions.LocationProviderType.REAL,
            )
            .build()
        sut = TripSessionLocationEngine(locationOptions) {
            replayLocationProvider
        }

        val onRawLocationUpdate: (Location) -> Unit = mockk()
        sut.startLocationUpdates(false, onRawLocationUpdate)

        verifyOrder {
            locationService.setUserDefinedDeviceLocationProviderFactory(
                customLocationProviderFactory,
            )
            locationService.getDeviceLocationProvider(any<LocationProviderRequest>())
        }
        verify(exactly = 1) {
            customLocationProvider.addLocationObserver(any(), not(Looper.getMainLooper()))
        }
    }

    @Test
    fun `should request location updates from custom mocked provider when replay is disabled`() {
        val customLocationProvider = mockk<DeviceLocationProvider>(relaxed = true)
        val customLocationProviderFactory = DeviceLocationProviderFactory {
            ExpectedFactory.createValue(customLocationProvider)
        }
        val locationOptions = LocationOptions.Builder()
            .locationProviderFactory(
                customLocationProviderFactory,
                LocationOptions.LocationProviderType.MOCKED,
            )
            .build()
        sut = TripSessionLocationEngine(locationOptions) {
            replayLocationProvider
        }

        val onRawLocationUpdate: (Location) -> Unit = mockk()
        sut.startLocationUpdates(false, onRawLocationUpdate)

        verify(exactly = 0) {
            locationService.setUserDefinedDeviceLocationProviderFactory(any())
            locationService.getDeviceLocationProvider(any<LocationProviderRequest>())
        }
        verify(exactly = 1) {
            customLocationProvider.addLocationObserver(any(), not(Looper.getMainLooper()))
        }
    }

    @Test
    fun `should request location updates from custom mixed provider when replay is disabled`() {
        val customLocationProvider = mockk<DeviceLocationProvider>(relaxed = true)
        val customLocationProviderFactory = DeviceLocationProviderFactory {
            ExpectedFactory.createValue(customLocationProvider)
        }
        val locationOptions = LocationOptions.Builder()
            .locationProviderFactory(
                customLocationProviderFactory,
                LocationOptions.LocationProviderType.MIXED,
            )
            .build()
        sut = TripSessionLocationEngine(locationOptions) {
            replayLocationProvider
        }

        val onRawLocationUpdate: (Location) -> Unit = mockk()
        sut.startLocationUpdates(false, onRawLocationUpdate)

        verify(exactly = 0) {
            locationService.setUserDefinedDeviceLocationProviderFactory(any())
            locationService.getDeviceLocationProvider(any<LocationProviderRequest>())
        }
        verify(exactly = 1) {
            customLocationProvider.addLocationObserver(any(), not(Looper.getMainLooper()))
        }
    }

    @Test
    fun `should stop location updates from navigation options when replay is disabled`() {
        sut = TripSessionLocationEngine(locationOptions) {
            replayLocationProvider
        }

        val onRawLocationUpdate: (Location) -> Unit = mockk()
        sut.startLocationUpdates(false, onRawLocationUpdate)
        sut.stopLocationUpdates()

        verifyOrder {
            defaultDeviceLocationProvider.addLocationObserver(any(), any())
            defaultDeviceLocationProvider.removeLocationObserver(any())
        }
    }

    @Test
    fun `should not request location updates from replay engine when replay is disabled`() {
        sut = TripSessionLocationEngine(locationOptions) {
            replayLocationProvider
        }

        val onRawLocationUpdate: (Location) -> Unit = mockk()
        sut.startLocationUpdates(false, onRawLocationUpdate)

        verify(exactly = 0) {
            replayLocationProvider.addLocationObserver(any(), any())
        }
        verify(exactly = 0) {
            locationService.setUserDefinedDeviceLocationProviderFactory(any())
        }
    }

    @Test
    fun `should request location updates from replay engine when replay is enabled`() {
        sut = TripSessionLocationEngine(locationOptions) {
            replayLocationProvider
        }

        val onRawLocationUpdate: (Location) -> Unit = mockk()
        sut.startLocationUpdates(true, onRawLocationUpdate)

        verify(exactly = 1) {
            replayLocationProvider.addLocationObserver(any(), not(Looper.getMainLooper()))
        }
        verify(exactly = 0) {
            locationService.setUserDefinedDeviceLocationProviderFactory(any())
        }
    }

    @Test
    fun `should stop location updates from replay engine when replay is enabled`() {
        sut = TripSessionLocationEngine(locationOptions) {
            replayLocationProvider
        }

        val onRawLocationUpdate: (Location) -> Unit = mockk()
        sut.startLocationUpdates(true, onRawLocationUpdate)
        sut.stopLocationUpdates()

        verifyOrder {
            replayLocationProvider.addLocationObserver(any(), any())
            replayLocationProvider.removeLocationObserver(any())
        }
    }

    @Test
    fun `should clean up last location from replay engine when replay is enabled`() {
        sut = TripSessionLocationEngine(locationOptions) {
            replayLocationProvider
        }

        val onRawLocationUpdate: (Location) -> Unit = mockk()
        sut.startLocationUpdates(true, onRawLocationUpdate)
        sut.stopLocationUpdates()

        verify { replayLocationProvider.cleanUpLastLocation() }
    }

    @Test
    fun `should not clean up last location from replay engine when replay is disabled`() {
        sut = TripSessionLocationEngine(locationOptions) {
            replayLocationProvider
        }

        val onRawLocationUpdate: (Location) -> Unit = mockk()
        sut.startLocationUpdates(false, onRawLocationUpdate)
        sut.stopLocationUpdates()

        verify(exactly = 0) { replayLocationProvider.cleanUpLastLocation() }
    }

    @Test
    fun `should not request location updates from navigation options when replay is enabled`() {
        sut = TripSessionLocationEngine(locationOptions) {
            replayLocationProvider
        }

        val onRawLocationUpdate: (Location) -> Unit = mockk()
        sut.startLocationUpdates(true, onRawLocationUpdate)

        verify(exactly = 0) {
            defaultDeviceLocationProvider.addLocationObserver(any(), any())
        }
        verify(exactly = 0) {
            locationService.setUserDefinedDeviceLocationProviderFactory(any())
        }
    }

    @Test
    fun `should remove location updates from previous location engine`() {
        sut = TripSessionLocationEngine(locationOptions) {
            replayLocationProvider
        }

        val onRawLocationUpdate: (Location) -> Unit = mockk()
        sut.startLocationUpdates(true, onRawLocationUpdate)

        verify(exactly = 0) {
            defaultDeviceLocationProvider.addLocationObserver(any(), any())
        }
    }

    @Test
    fun `startLocationUpdates should remove updates from previous location engine`() {
        sut = TripSessionLocationEngine(locationOptions) {
            replayLocationProvider
        }

        val onRawLocationUpdate: (Location) -> Unit = mockk()
        sut.startLocationUpdates(true, onRawLocationUpdate)
        sut.startLocationUpdates(false, onRawLocationUpdate)
        sut.startLocationUpdates(true, onRawLocationUpdate)

        verifyOrder {
            replayLocationProvider.addLocationObserver(any(), any())
            replayLocationProvider.removeLocationObserver(any())
            defaultDeviceLocationProvider.addLocationObserver(any(), any())
            defaultDeviceLocationProvider.removeLocationObserver(any())
            replayLocationProvider.addLocationObserver(any(), any())
        }
    }

    @Test
    fun `isReplayEnabled is true after replay is enabled`() {
        sut = TripSessionLocationEngine(locationOptions) {
            replayLocationProvider
        }

        sut.startLocationUpdates(true, mockk())

        assertTrue(sut.isReplayEnabled)
    }

    @Test
    fun `isReplayEnabled is false when replay is disabled for location updates`() {
        sut = TripSessionLocationEngine(locationOptions) {
            replayLocationProvider
        }

        sut.startLocationUpdates(false, mockk())

        assertFalse(sut.isReplayEnabled)
    }

    @Test
    fun `isReplayEnabled is false after stopLocationUpdates`() {
        sut = TripSessionLocationEngine(locationOptions) {
            replayLocationProvider
        }

        sut.startLocationUpdates(true, mockk())
        sut.stopLocationUpdates()

        assertFalse(sut.isReplayEnabled)
    }

    @Test
    fun `should cancel last location task when updates are stopped`() {
        sut = TripSessionLocationEngine(locationOptions) {
            replayLocationProvider
        }

        val firstCallback = mockk<(Location) -> Unit>(relaxed = true)
        val locationTask = mockk<Cancelable>(relaxed = true)
        every { replayLocationProvider.getLastLocation(any()) } returns locationTask
        sut.startLocationUpdates(true, firstCallback)
        verify {
            replayLocationProvider.getLastLocation(any())
        }
        sut.stopLocationUpdates()
        verify { locationTask.cancel() }
    }

    @Test
    fun `should unsets custom location provider factory`() {
        sut = TripSessionLocationEngine(locationOptions) {
            replayLocationProvider
        }
        clearAllMocks(answers = false)

        sut.destroy()

        verify(exactly = 1) {
            locationService.setUserDefinedDeviceLocationProviderFactory(null)
        }
    }

    private fun mockLocationProvider(locationProvider: LocationProvider) {
        val locationObserverSlot = slot<com.mapbox.common.location.LocationObserver>()
        every {
            locationProvider.addLocationObserver(
                capture(locationObserverSlot),
                any(),
            )
        } answers {}
    }
}
