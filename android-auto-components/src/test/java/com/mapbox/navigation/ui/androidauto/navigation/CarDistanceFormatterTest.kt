package com.mapbox.navigation.ui.androidauto.navigation

import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class CarDistanceFormatterTest {

    // The formatter is a process-wide object. Clear what an earlier test may have attached, so
    // this test sees the state before any MapboxNavigation is attached.
    private val formatterField = CarDistanceFormatter::class.java
        .getDeclaredField("mapboxDistanceFormatter")
        .apply { isAccessible = true }
    private var previousFormatter: Any? = null

    @Before
    fun setup() {
        previousFormatter = formatterField.get(CarDistanceFormatter)
        formatterField.set(CarDistanceFormatter, null)
    }

    @After
    fun teardown() {
        formatterField.set(CarDistanceFormatter, previousFormatter)
    }

    @Test
    fun `no distance is formatted before navigation is attached`() {
        assertNull(CarDistanceFormatter.formatDistanceOrNull(100.0))
    }
}
