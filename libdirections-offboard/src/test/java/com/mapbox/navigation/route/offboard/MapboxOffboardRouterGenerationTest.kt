package com.mapbox.navigation.route.offboard

import org.junit.Assert
import org.junit.Before
import org.junit.Test

class MapboxOffboardRouterGenerationTest {

    private lateinit var offboardRouter: MapboxOffboardRouter

    @Before
    fun setUp() {
        offboardRouter = MapboxOffboardRouter()
    }

    @Test
    fun generationSanityTest() {
        Assert.assertNotNull(offboardRouter)
    }
}
