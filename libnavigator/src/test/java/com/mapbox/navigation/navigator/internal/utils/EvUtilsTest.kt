package com.mapbox.navigation.navigator.internal.utils

import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigator.CurveElement
import com.mapbox.navigator.EvStateData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class EvUtilsTest {

    @get:Rule
    val noLoggerRule = LoggingFrontendTestRule()

    @Test
    fun toEvStateDataMinimalValid() {
        val input = mapOf(
            "ev_initial_charge" to "10",
            "energy_consumption_curve" to "1.0,2.0;3.0,4.0",
        )
        val expected = EvStateData(
            10,
            listOf(CurveElement(1.0f, 2.0f), CurveElement(3.0f, 4.0f)),
            null,
            null,
        )

        assertEquals(expected, input.toEvStateData())
    }

    @Test
    fun toEvStateDataFilled() {
        val input = mapOf(
            "ev_initial_charge" to "10",
            "energy_consumption_curve" to "1.0,2.0;3.0,4.0",
            "auxiliary_consumption" to "5",
            "ev_pre_conditioning_time" to "7",
        )
        val expected = EvStateData(
            10,
            listOf(CurveElement(1.0f, 2.0f), CurveElement(3.0f, 4.0f)),
            5,
            7,
        )

        assertEquals(expected, input.toEvStateData())
    }

    @Test
    fun emptyInitialCharge() {
        val input = mapOf(
            "energy_consumption_curve" to "1.0,2.0;3.0,4.0",
        )

        assertEquals(0, input.toEvStateData().evInitialCharge)
    }

    @Test
    fun invalidInitialCharge() {
        val input = mapOf(
            "ev_initial_charge" to "string",
            "energy_consumption_curve" to "1.0,2.0;3.0,4.0",
        )

        assertEquals(0, input.toEvStateData().evInitialCharge)
    }

    @Test
    fun invalidPreConditioningTime() {
        val input = mapOf(
            "energy_consumption_curve" to "1.0,2.0;3.0,4.0",
            "ev_pre_conditioning_time" to "string",
        )

        assertNull(input.toEvStateData().evPreConditioningTime)
    }

    @Test
    fun invalidAuxConsumption() {
        val input = mapOf(
            "energy_consumption_curve" to "1.0,2.0;3.0,4.0",
            "auxiliary_consumption" to "string",
        )

        assertNull(input.toEvStateData().auxiliaryConsumption)
    }

    @Test
    fun invalidCurveElements() {
        val input = mapOf(
            "energy_consumption_curve" to "invalid",
        )

        assertEquals(emptyList<CurveElement>(), input.toEvStateData().energyConsumptionCurve)
    }

    @Test
    fun invalidSingleCurveElements() {
        val input = mapOf(
            "energy_consumption_curve" to "1.0,2.0;invalid;3.0,4.0",
        )

        assertEquals(emptyList<CurveElement>(), input.toEvStateData().energyConsumptionCurve)
    }
}
