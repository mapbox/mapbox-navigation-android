package com.mapbox.navigation.base.route

import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationWalkingOptionsTest {

    @Test
    fun alleyBias_walkingOptionSet() {
        val options = NavigationWalkingOptions.builder().alleyBias(0.7).build()

        assertEquals(java.lang.Double.valueOf(0.7), options.walkingOptions.alleyBias())
    }

    @Test
    fun walkwayBias_walkingOptionSet() {
        val options = NavigationWalkingOptions.builder().walkwayBias(0.8).build()

        assertEquals(java.lang.Double.valueOf(0.8), options.walkingOptions.walkwayBias())
    }

    @Test
    fun walkingSpeed_walkingOptionSet() {
        val options = NavigationWalkingOptions.builder().walkingSpeed(2.0).build()

        assertEquals(java.lang.Double.valueOf(2.0), options.walkingOptions.walkingSpeed())
    }
}
