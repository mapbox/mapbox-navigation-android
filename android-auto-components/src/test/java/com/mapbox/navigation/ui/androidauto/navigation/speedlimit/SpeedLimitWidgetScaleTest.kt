package com.mapbox.navigation.ui.androidauto.navigation.speedlimit

import com.mapbox.maps.MapboxExperimental
import com.mapbox.navigation.base.speed.model.SpeedLimitSign
import com.mapbox.navigation.ui.androidauto.testing.MapboxRobolectricTestRunner
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(MapboxExperimental::class)
class SpeedLimitWidgetScaleTest : MapboxRobolectricTestRunner() {

    @Test
    fun `scaled widget scales its margins and initial position`() {
        val sut = SpeedLimitWidget.scaled(SpeedLimitSign.MUTCD, scale = 2f)

        assertEquals(28f, sut.marginXPx)
        assertEquals(60f, sut.marginYPx)
        assertEquals(-28f, sut.getPosition().offsetX)
        assertEquals(-60f, sut.getPosition().offsetY)
    }

    @Test
    fun `public constructor keeps the margins in pixels`() {
        val sut = SpeedLimitWidget(SpeedLimitSign.MUTCD)

        assertEquals(-14f, sut.getPosition().offsetX)
        assertEquals(-30f, sut.getPosition().offsetY)
    }
}
