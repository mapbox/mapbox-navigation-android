package com.mapbox.navigation.core

import android.app.NotificationManager
import android.content.Context
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.trip.service.TripService
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.utils.extensions.inferDeviceLocale
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.verify
import java.util.Locale
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

class MapboxNavigationTest {

    private lateinit var mapboxNavigation: MapboxNavigation
    private val accessToken = "pk.1234"
    private val context: Context = mockk(relaxed = true)
    private val applicationContext: Context = mockk(relaxed = true)
    private val locationEngine: LocationEngine = mockk()
    private val locationEngineRequest: LocationEngineRequest = mockk()
    private val directionsSession: DirectionsSession = mockk(relaxUnitFun = true)
    private val tripSession: TripSession = mockk(relaxUnitFun = true)
    private val tripService: TripService = mockk(relaxUnitFun = true)

    companion object {
        @BeforeClass
        @JvmStatic
        fun initialize() {
            mockkStatic("com.mapbox.navigation.utils.extensions.ContextEx")
        }
    }

    @Before
    fun setUp() {
        every { context.inferDeviceLocale() } returns Locale.US
        every { context.applicationContext } returns applicationContext
        val notificationManager = mockk<NotificationManager>()
        every { applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) } returns notificationManager

        mockkObject(NavigationComponentProvider)
        every { NavigationComponentProvider.createDirectionsSession(any()) } returns directionsSession
        every {
            NavigationComponentProvider.createTripService(
                applicationContext,
                any()
            )
        } returns tripService
        every {
            NavigationComponentProvider.createTripSession(
                tripService,
                locationEngine,
                locationEngineRequest,
                any()
            )
        } returns tripSession
        val navigationOptions = mockk<NavigationOptions>(relaxed = true)
        mapboxNavigation =
            MapboxNavigation(
                context,
                accessToken,
                navigationOptions,
                locationEngine,
                locationEngineRequest
            )
    }

    @Test
    fun sanity() {
        assertNotNull(mapboxNavigation)
    }

    @Test
    fun onDestroy_unregisters_DirectionSession_observers() {
        mapboxNavigation.onDestroy()

        verify(exactly = 1) { directionsSession.unregisterAllRoutesObservers() }
    }

    @Test
    fun onDestroy_unregisters_TripSession_location_observers() {
        mapboxNavigation.onDestroy()

        verify(exactly = 1) { tripSession.unregisterAllLocationObservers() }
    }

    @Test
    fun onDestroy_unregisters_TripSession_routeProgress_observers() {
        mapboxNavigation.onDestroy()

        verify(exactly = 1) { tripSession.unregisterAllRouteProgressObservers() }
    }

    @Test
    fun onDestroy_unregisters_TripSession_offRoute_observers() {
        mapboxNavigation.onDestroy()

        verify(exactly = 1) { tripSession.unregisterAllOffRouteObservers() }
    }

    @Test
    fun onDestroy_unregisters_TripSession_state_observers() {
        mapboxNavigation.onDestroy()

        verify(exactly = 1) { tripSession.unregisterAllStateObservers() }
    }

    @Test
    fun unregisterAllBannerInstructionsObservers() {
        mapboxNavigation.onDestroy()

        verify(exactly = 1) { tripSession.unregisterAllBannerInstructionsObservers() }
    }

    @Test
    fun unregisterAllVoiceInstructionsObservers() {
        mapboxNavigation.onDestroy()

        verify(exactly = 1) { tripSession.unregisterAllVoiceInstructionsObservers() }
    }
}
