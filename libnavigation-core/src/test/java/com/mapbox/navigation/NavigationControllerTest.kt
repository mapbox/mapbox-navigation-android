package com.mapbox.navigation

import android.content.Context
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineRequest
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
    private val context: Context = mockk()
    private val navigator: MapboxNativeNavigator = mockk()
    private val locationEngine: LocationEngine = mockk()
    private val locationEngineRequest: LocationEngineRequest = mockk()

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
        navigationController =
            NavigationController(
                context,
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
