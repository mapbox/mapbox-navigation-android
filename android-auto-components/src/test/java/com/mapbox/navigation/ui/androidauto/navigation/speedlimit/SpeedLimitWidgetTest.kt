package com.mapbox.navigation.ui.androidauto.navigation.speedlimit

import com.mapbox.maps.MapboxExperimental
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(MapboxExperimental::class)
class SpeedLimitWidgetTest {

    @Test
    fun `does not warn at the speed limit`() {
        assertFalse(SpeedLimitWidget.shouldWarn(speedLimit = 50, speed = 50, threshold = 0))
    }

    @Test
    fun `warns above the speed limit without a threshold`() {
        assertTrue(SpeedLimitWidget.shouldWarn(speedLimit = 50, speed = 51, threshold = 0))
    }

    @Test
    fun `does not warn at the speed limit plus the threshold`() {
        assertFalse(SpeedLimitWidget.shouldWarn(speedLimit = 50, speed = 55, threshold = 5))
    }

    @Test
    fun `warns above the speed limit plus the threshold`() {
        assertTrue(SpeedLimitWidget.shouldWarn(speedLimit = 50, speed = 56, threshold = 5))
    }

    @Test
    fun `does not warn without a speed limit`() {
        assertFalse(SpeedLimitWidget.shouldWarn(speedLimit = null, speed = 200, threshold = 0))
    }
}
