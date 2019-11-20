package com.mapbox.navigation

import android.app.Application
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.navigation.navigator.MapboxNativeNavigator
import io.mockk.mockk
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class NavigationControllerTest {

    private lateinit var navigationController: NavigationController
    private val application: Application = mockk()
    private val token = "pk_*"
    private val navigator: MapboxNativeNavigator = mockk()
    private val locationEngine: LocationEngine = mockk()
    private val locationEngineRequest: LocationEngineRequest = mockk()

    @Before
    fun setUp() {
        navigationController =
            NavigationController(
                application,
                token,
                navigator,
                locationEngine,
                locationEngineRequest
            )
    }

    @Test
    fun sanity() {
        Assert.assertNotNull(navigationController)
    }
}
