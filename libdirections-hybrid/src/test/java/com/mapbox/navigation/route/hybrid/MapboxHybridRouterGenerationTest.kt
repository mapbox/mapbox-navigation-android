package com.mapbox.navigation.route.hybrid

import com.mapbox.navigation.route.offboard.MapboxOffboardRouter
import com.mapbox.navigation.route.onboard.MapboxOnboardRouter
import io.mockk.mockk
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class MapboxHybridRouterGenerationTest {

    private lateinit var hybridRouter: MapboxHybridRouter

    private val onboardRouter: MapboxOnboardRouter = mockk()
    private val offboardRouter: MapboxOffboardRouter = mockk()

    @Before
    fun setUp() {
        hybridRouter = MapboxHybridRouter(onboardRouter, offboardRouter)
    }

    @Test
    fun generationSanityTest() {
        Assert.assertNotNull(hybridRouter)
    }
}
