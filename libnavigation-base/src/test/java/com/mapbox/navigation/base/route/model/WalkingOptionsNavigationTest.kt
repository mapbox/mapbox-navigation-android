package com.mapbox.navigation.base.route.model

import org.junit.Assert.assertEquals
import org.junit.Test

class WalkingOptionsNavigationTest {

    @Test
    fun alleyBias_walkingOptionSet() {
        val options = WalkingOptionsNavigation.builder().alleyBias(0.7).build()

        assertEquals(0.7, options.alleyBias)
    }

    @Test
    fun walkwayBias_walkingOptionSet() {
        val options = WalkingOptionsNavigation.builder().walkwayBias(0.8).build()

        assertEquals(0.8, options.walkwayBias)
    }

    @Test
    fun walkingSpeed_walkingOptionSet() {
        val options = WalkingOptionsNavigation.builder().walkingSpeed(2.0).build()

        assertEquals(2.0, options.walkingSpeed)
    }
}
