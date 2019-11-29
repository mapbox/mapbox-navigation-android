package com.mapbox.navigation

import android.app.Application
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.navigation.base.route.DirectionsSession
import com.mapbox.navigation.navigator.MapboxNativeNavigator
import com.mapbox.navigation.utils.extensions.inferDeviceLocale
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import java.util.Locale
import org.junit.Assert
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

class NavigationControllerTest {

    private lateinit var navigationController: NavigationController
    private val application: Application = mockk()
    private val token = "pk_*"
    private val navigator: MapboxNativeNavigator = mockk()
    private val locationEngine: LocationEngine = mockk()
    private val locationEngineRequest: LocationEngineRequest = mockk()
    private val routeObserver: DirectionsSession.RouteObserver = mockk()

    companion object {
        @BeforeClass
        @JvmStatic
        fun initialize() {
            mockkStatic("com.mapbox.navigation.utils.extensions.ContextEx")
        }
    }

    @Before
    fun setUp() {
        every { application.inferDeviceLocale() } returns Locale.US

        navigationController =
            NavigationController(
                application,
                token,
                navigator,
                locationEngine,
                locationEngineRequest,
                routeObserver
            )
    }

    @Test
    fun sanity() {
        Assert.assertNotNull(navigationController)
    }
}
