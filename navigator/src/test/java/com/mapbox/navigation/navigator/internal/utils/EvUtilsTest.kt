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
            null,
            null,
            emptyList(),
            hashMapOf(),
        )

        assertEquals(expected, input.toEvStateData())
    }

    @Test
    fun toEvStateDataFilled() {
        val input = mapOf(
            "ev_initial_charge" to "10",
            "energy_consumption_curve" to "1.0,2.0;3.0,4.0",
            "ev_freeflow_consumption_curve" to "9.0,10.0;11.0,12.0",
            "auxiliary_consumption" to "5",
            "ev_pre_conditioning_time" to "7",
            "ev_unconditioned_charging_curve" to "5.0,6.0;7.0,8.0",
            "ev_extra_param_1_key" to "extra_param_1_value",
            "ev_extra_param_2_key" to "extra_param_2_value",
        )
        val expected = EvStateData(
            10,
            listOf(CurveElement(1.0f, 2.0f), CurveElement(3.0f, 4.0f)),
            listOf(CurveElement(9.0f, 10.0f), CurveElement(11.0f, 12.0f)),
            null,
            5,
            7,
            listOf(CurveElement(5.0f, 6.0f), CurveElement(7.0f, 8.0f)),
            hashMapOf(
                "ev_extra_param_1_key" to "extra_param_1_value",
                "ev_extra_param_2_key" to "extra_param_2_value",
            ),
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

    @Test
    fun invalidFreeflowCurveElements() {
        val input = mapOf(
            "ev_freeflow_consumption_curve" to "invalid",
        )

        assertEquals(emptyList<CurveElement>(), input.toEvStateData().evFreeflowConsumptionCurve)
    }

    @Test
    fun invalidSingleFreeflowCurveElements() {
        val input = mapOf(
            "ev_freeflow_consumption_curve" to "1.0,2.0;invalid;3.0,4.0",
        )

        assertEquals(emptyList<CurveElement>(), input.toEvStateData().evFreeflowConsumptionCurve)
    }
}
