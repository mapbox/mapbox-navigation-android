package com.mapbox.navigation.ui.base.model.tripprogress

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PercentDistanceTraveledFormatterTest {

    @Test
    fun format() {
        val update = TripProgressUpdate(
            System.currentTimeMillis(),
            19312.1,
            50.0,
            100.0,
            21.0,
            111
        )

        val result = PercentDistanceTraveledFormatter().format(update)

        assertEquals("21", result.toString())
    }
}
