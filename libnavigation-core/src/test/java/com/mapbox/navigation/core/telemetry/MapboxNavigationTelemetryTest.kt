package com.mapbox.navigation.core.telemetry

import android.app.AlarmManager
import android.content.Context
import android.content.SharedPreferences
import com.mapbox.android.telemetry.MapboxTelemetryConstants
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.metrics.MapboxMetricsReporter
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.utils.internal.ThreadController
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
class MapboxNavigationTelemetryTest {
    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private val context: Context = mockk(relaxed = true)
    private val applicationContext: Context = mockk(relaxed = true)
    private val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
    private val navigationOptions: NavigationOptions = mockk(relaxed = true)
    private val route: DirectionsRoute = mockk(relaxed = true)
    private val token = "pk.token"

    @Before
    fun setup() {
        every { mapboxNavigation.getRoutes() } answers { listOf(route) }

        mockkObject(ThreadController)

        val alarmManager = mockk<AlarmManager>()
        every { applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager } returns alarmManager
        every { context.applicationContext } returns applicationContext

        val sharedPreferences = mockk<SharedPreferences>(relaxed = true)
        every {
            applicationContext.getSharedPreferences(
                MapboxTelemetryConstants.MAPBOX_SHARED_PREFERENCES,
                Context.MODE_PRIVATE
            )
        } returns sharedPreferences
        every { sharedPreferences.getString("mapboxTelemetryState", "ENABLED"); } returns "DISABLED"

        MapboxMetricsReporter.init(context, token, "userAgent")
    }

    @Test
    fun onInit_registerRouteProgressObserver_called() {
        onInit { verify(exactly = 1) { mapboxNavigation.registerRouteProgressObserver(any()) } }
    }

    @Test
    fun onInit_registerLocationObserver_called() {
        onInit { verify(exactly = 1) { mapboxNavigation.registerLocationObserver(any()) } }
    }

    @Test
    fun onInit_registerRoutesObserver_called() {
        onInit { verify(exactly = 1) { mapboxNavigation.registerRoutesObserver(any()) } }
    }

    @Test
    fun onInit_registerOffRouteObserver_called() {
        onInit { verify(exactly = 1) { mapboxNavigation.registerOffRouteObserver(any()) } }
    }

    @Test
    fun onInit_registerNavigationSessionObserver_called() {
        onInit { verify(exactly = 1) { mapboxNavigation.registerNavigationSessionObserver(any()) } }
    }

    @Test
    fun onInit_getRoutes_called() {
        onInit { verify(exactly = 1) { mapboxNavigation.getRoutes() } }
    }

    @Test
    fun onUnregisterListener_unregisterRouteProgressObserver_called() {
        onUnregister { verify(exactly = 1) { mapboxNavigation.unregisterRouteProgressObserver(any()) } }
    }

    @Test
    fun onUnregisterListener_unregisterLocationObserver_called() {
        onUnregister { verify(exactly = 1) { mapboxNavigation.unregisterLocationObserver(any()) } }
    }

    @Test
    fun onUnregisterListener_unregisterRoutesObserver_called() {
        onUnregister { verify(exactly = 1) { mapboxNavigation.unregisterRoutesObserver(any()) } }
    }

    @Test
    fun onUnregisterListener_unregisterOffRouteObserver_called() {
        onUnregister { verify(exactly = 1) { mapboxNavigation.unregisterOffRouteObserver(any()) } }
    }

    @Test
    fun onUnregisterListener_unregisterNavigationSessionObserver_called() {
        onUnregister { verify(exactly = 1) { mapboxNavigation.unregisterNavigationSessionObserver(any()) } }
    }

    @Test
    fun after_unregister_onInit_registers_all_listeners_again() {
        initTelemetry()
        resetTelemetry()
        initTelemetry()

        verify(exactly = 2) { mapboxNavigation.registerRouteProgressObserver(any()) }
        verify(exactly = 2) { mapboxNavigation.registerLocationObserver(any()) }
        verify(exactly = 2) { mapboxNavigation.registerRoutesObserver(any()) }
        verify(exactly = 2) { mapboxNavigation.registerOffRouteObserver(any()) }
        verify(exactly = 2) { mapboxNavigation.registerNavigationSessionObserver(any()) }
        verify(exactly = 2) { mapboxNavigation.getRoutes() }

        resetTelemetry()
    }

    @Test
    fun onInitTwice_unregisters_all_listeners() {
        initTelemetry()
        initTelemetry()

        verify(exactly = 1) { mapboxNavigation.unregisterRouteProgressObserver(any()) }
        verify(exactly = 1) { mapboxNavigation.unregisterLocationObserver(any()) }
        verify(exactly = 1) { mapboxNavigation.unregisterRoutesObserver(any()) }
        verify(exactly = 1) { mapboxNavigation.unregisterOffRouteObserver(any()) }
        verify(exactly = 1) { mapboxNavigation.unregisterNavigationSessionObserver(any()) }

        resetTelemetry()
    }

    @After
    fun cleanUp() {
        unmockkObject(ThreadController)
    }

    private fun initTelemetry() {
        MapboxNavigationTelemetry.initialize(
            context,
            mapboxNavigation,
            MapboxMetricsReporter,
            "locationEngine",
            ThreadController.getMainScopeAndRootJob(),
            navigationOptions,
            "userAgent"
        )
    }

    private fun resetTelemetry() {
        MapboxNavigationTelemetry.unregisterListeners(mapboxNavigation)
    }

    private fun onInit(block: () -> Unit) {
        initTelemetry()
        block()
        resetTelemetry()
    }

    private fun onUnregister(block: () -> Unit) {
        initTelemetry()
        resetTelemetry()
        block()
    }
}
