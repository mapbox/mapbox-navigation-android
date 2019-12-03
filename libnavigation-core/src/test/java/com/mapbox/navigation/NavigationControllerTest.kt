package com.mapbox.navigation

import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.navigation.base.route.DirectionsSession
import com.mapbox.navigation.navigator.MapboxNativeNavigator
import com.mapbox.navigation.route.offboard.router.NavigationRoute
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Assert
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

class NavigationControllerTest {

    private lateinit var navigationController: NavigationController
    private val routeBuilderProvider: NavigationRoute.Builder = mockk()
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
        navigationController =
            NavigationController(
                routeBuilderProvider,
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
