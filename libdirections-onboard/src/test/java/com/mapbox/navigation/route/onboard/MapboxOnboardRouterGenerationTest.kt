package com.mapbox.navigation.route.onboard

import com.mapbox.navigation.navigator.MapboxNativeNavigator
import io.mockk.mockk
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class MapboxOnboardRouterGenerationTest {

    private lateinit var onboardRouter: MapboxOnboardRouter
    private val navigator: MapboxNativeNavigator = mockk()

    @Before
    fun setUp() {
        onboardRouter = MapboxOnboardRouter(navigator)
    }

    @Test
    fun generationSanityTest() {
        Assert.assertNotNull(onboardRouter)
    }
}
