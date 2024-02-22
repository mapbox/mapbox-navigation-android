package com.mapbox.navigation.core.telemetry

import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.BuildConfig
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.metrics.MapboxMetricsReporter
import com.mapbox.navigation.metrics.internal.EventsServiceProvider
import com.mapbox.navigation.metrics.internal.TelemetryServiceProvider
import com.mapbox.navigation.metrics.internal.TelemetryUtilsDelegate
import com.mapbox.navigation.utils.internal.logD
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TelemetryWrapperTest {

    private val testAccessToken = "test-access-token"
    private val userAgent: String = "test-user-agent"
    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var navigationOptions: NavigationOptions

    private lateinit var telemetryWrapper: TelemetryWrapper

    @Before
    fun setUp() {
        mockkObject(EventsServiceProvider)
        every {
            EventsServiceProvider.provideEventsService(any())
        } returns mockk(relaxUnitFun = true)
        mockkObject(TelemetryServiceProvider)
        every {
            TelemetryServiceProvider.provideTelemetryService(any())
        } returns mockk(relaxUnitFun = true)
        mockkObject(MapboxNavigationTelemetry)
        every { MapboxNavigationTelemetry.initialize(any(), any(), any(), any()) } just runs
        every { MapboxNavigationTelemetry.destroy(any()) } just runs
        every {
            MapboxNavigationTelemetry.postUserFeedback(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } just runs
        mockkObject(TelemetryUtilsDelegate)
        every { TelemetryUtilsDelegate.getEventsCollectionState() } returns true
        mockkObject(MapboxMetricsReporter)
        mockkStatic("com.mapbox.navigation.utils.internal.LoggerProviderKt")
        every { logD(msg = any(), category = any()) } just Runs

        mapboxNavigation = mockk(relaxed = true)
        navigationOptions = mockk<NavigationOptions>(relaxed = true).apply {
            every { accessToken } returns testAccessToken
        }

        telemetryWrapper = TelemetryWrapper()
    }

    @After
    fun tearDown() {
        unmockkObject(MapboxNavigationTelemetry)
        unmockkObject(TelemetryUtilsDelegate)
        unmockkObject(MapboxMetricsReporter)
        unmockkStatic("com.mapbox.navigation.utils.internal.LoggerProviderKt")
    }

    @Test
    fun `throws exception when initialize is called again`() {
        if (BuildConfig.DEBUG) {
            assertThrows(
                "Already initialized",
                IllegalStateException::class.java
            ) {
                telemetryWrapper.initialize()
                telemetryWrapper.initialize()
            }
        }
    }

    @Test
    fun `throws exception when destroy is called again`() {
        if (BuildConfig.DEBUG) {
            assertThrows(
                "Initialize object first",
                IllegalStateException::class.java
            ) {
                telemetryWrapper.initialize()
                telemetryWrapper.destroy()
                telemetryWrapper.destroy()
            }
        }
    }

    @Test
    fun `throws exception when destroy is called without initialization`() {
        if (BuildConfig.DEBUG) {
            assertThrows(
                "Initialize object first",
                IllegalStateException::class.java
            ) {
                telemetryWrapper.destroy()
            }
        }
    }

    @Test
    fun `initializes MapboxNavigationTelemetry on initialization when telemetry is enabled`() {
        every { TelemetryUtilsDelegate.getEventsCollectionState() } returns true

        telemetryWrapper.initialize()

        verify(exactly = 1) {
            MapboxNavigationTelemetry.initialize(
                mapboxNavigation,
                navigationOptions,
                MapboxMetricsReporter,
                any()
            )
        }
    }

    @Test
    fun `doesn't initialize MapboxNavigationTelemetry on initialization when telemetry is disabled`() {
        every { TelemetryUtilsDelegate.getEventsCollectionState() } returns false

        telemetryWrapper.initialize()

        verify(exactly = 0) {
            MapboxNavigationTelemetry.initialize(
                mapboxNavigation,
                navigationOptions,
                MapboxMetricsReporter,
                any()
            )
        }
    }

    @Test
    fun `destroys MapboxNavigationTelemetry on destroy if MapboxNavigationTelemetry was initialized`() {
        every { TelemetryUtilsDelegate.getEventsCollectionState() } returns true

        telemetryWrapper.initialize()
        telemetryWrapper.destroy()

        verify(exactly = 1) {
            MapboxNavigationTelemetry.destroy(
                mapboxNavigation
            )
        }
    }

    @Test
    fun `doesn't destroy MapboxNavigationTelemetry on destroy if MapboxNavigationTelemetry was not initialized`() {
        every { TelemetryUtilsDelegate.getEventsCollectionState() } returns false

        telemetryWrapper.initialize()
        telemetryWrapper.destroy()

        verify(exactly = 0) {
            MapboxNavigationTelemetry.destroy(
                mapboxNavigation
            )
        }
    }

    private fun TelemetryWrapper.initialize() {
        telemetryWrapper.initialize(
            mapboxNavigation,
            navigationOptions,
            userAgent,
        )
    }
}
