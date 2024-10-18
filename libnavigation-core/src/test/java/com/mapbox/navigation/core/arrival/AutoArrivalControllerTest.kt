package com.mapbox.navigation.core.arrival

import io.mockk.mockk
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoArrivalControllerTest {

    private val arrivalController = AutoArrivalController()

    @Test
    fun `navigateNextRouteLeg fire true`() {
        val result = arrivalController.navigateNextRouteLeg(mockk())

        assertTrue(result)
    }
}
