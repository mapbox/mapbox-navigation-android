package com.mapbox.navigation.route.onboard

import com.mapbox.navigation.base.options.MapboxOnboardRouterConfig
import com.mapbox.navigation.navigator.MapboxNativeNavigator
import io.mockk.mockk
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class MapboxOnboardRouterTest {
    private lateinit var onboardRouter: MapboxOnboardRouter
    private val navigator: MapboxNativeNavigator = mockk()
    private val tilePath = "tiles"

    @Before
    fun setUp() {
        onboardRouter = MapboxOnboardRouter(navigator, MapboxOnboardRouterConfig(tilePath))
    }

    @Test
    fun generationSanityTest() {
        Assert.assertNotNull(onboardRouter)
    }
}
