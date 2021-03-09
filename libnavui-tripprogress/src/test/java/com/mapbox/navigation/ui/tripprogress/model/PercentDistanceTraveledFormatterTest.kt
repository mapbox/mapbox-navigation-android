package com.mapbox.navigation.ui.tripprogress.model

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PercentDistanceTraveledFormatterTest {

    @Test
    fun format() {
        val result = PercentDistanceTraveledFormatter().format(21.0)

        assertEquals("21", result.toString())
    }
}
