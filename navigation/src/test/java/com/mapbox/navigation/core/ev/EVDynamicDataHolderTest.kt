@file:OptIn(ExperimentalPreviewMapboxNavigationAPI::class)

package com.mapbox.navigation.core.ev

import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import org.junit.Assert.assertEquals
import org.junit.Test

class EVDynamicDataHolderTest {

    private val evDynamicDataHolder = EVDynamicDataHolder()
    private val curve = "0,40;50,120"
    private val freeflowCurve = "0,30;50,100"
    private val charge = "90"
    private val auxConsumption = "70"
    private val preConditioningTime = "10"
    private val unconditionedChargingCurve = "0,30;50,100"
    private val batteryTemperature = "25.0"
    private val initial = mapOf(
        "energy_consumption_curve" to JsonPrimitive(curve),
        "ev_freeflow_consumption_curve" to JsonPrimitive(freeflowCurve),
        "ev_initial_charge" to JsonPrimitive(charge),
        "auxiliary_consumption" to JsonPrimitive(auxConsumption),
        "ev_pre_conditioning_time" to JsonPrimitive(preConditioningTime),
        "ev_unconditioned_charging_curve" to JsonPrimitive(unconditionedChargingCurve),
        "aaa" to JsonPrimitive("bbb"),
    )

    @Test
    fun `currentData() with no updates and empty initial`() {
        assertEquals(0, evDynamicDataHolder.currentData(emptyMap()).size)
    }

    @Test
    fun `currentData() with no updates and EV fallback`() {
        val expected = mapOf(
            "energy_consumption_curve" to curve,
            "ev_freeflow_consumption_curve" to freeflowCurve,
            "ev_initial_charge" to charge,
            "auxiliary_consumption" to auxConsumption,
            "ev_pre_conditioning_time" to preConditioningTime,
            "ev_unconditioned_charging_curve" to unconditionedChargingCurve,
        )
        assertEquals(expected, evDynamicDataHolder.currentData(initial))
        assertEquals(emptyMap<String, String>(), evDynamicDataHolder.updatedRawData().value)
    }

    @Test
    fun `currentData() after one update with empty initial`() {
        val data = mapOf("aaa" to "bbb", "ccc" to "ddd")
        evDynamicDataHolder.updateData(data)

        assertEquals(data, evDynamicDataHolder.currentData(emptyMap()))
        assertEquals(data, evDynamicDataHolder.updatedRawData().value)
    }

    @Test
    fun `currentData() after one update`() {
        val data = mapOf(
            "energy_consumption_curve" to "3,300",
            "auxiliary_consumption" to "80",
        )
        val expected = mapOf(
            "energy_consumption_curve" to "3,300",
            "ev_freeflow_consumption_curve" to freeflowCurve,
            "ev_initial_charge" to charge,
            "auxiliary_consumption" to "80",
            "ev_pre_conditioning_time" to preConditioningTime,
            "ev_unconditioned_charging_curve" to unconditionedChargingCurve,
        )
        evDynamicDataHolder.updateData(data)

        assertEquals(expected, evDynamicDataHolder.currentData(initial))
        assertEquals(data, evDynamicDataHolder.updatedRawData().value)
    }

    @Test
    fun `currentData() after two updates with empty initial`() {
        val data1 = mapOf("aaa" to "bbb", "ccc" to "ddd", "eee" to "fff")
        val data2 = mapOf("ccc" to "zzz", "ggg" to "yyy")
        val expected = mapOf("aaa" to "bbb", "ccc" to "zzz", "ggg" to "yyy", "eee" to "fff")
        evDynamicDataHolder.updateData(data1)
        evDynamicDataHolder.updateData(data2)

        assertEquals(expected, evDynamicDataHolder.currentData(emptyMap()))
        assertEquals(expected, evDynamicDataHolder.updatedRawData().value)
    }

    @Test
    fun `currentData() after two updates`() {
        val data1 = mapOf(
            "energy_consumption_curve" to "3,300",
            "ev_initial_charge" to "78",
            "eee" to "fff",
        )
        val data2 = mapOf(
            "energy_consumption_curve" to "4,400",
            "auxiliary_consumption" to "80",
            "aaa" to "bbb",
        )
        val expected = mapOf(
            "aaa" to "bbb",
            "eee" to "fff",
            "energy_consumption_curve" to "4,400",
            "ev_freeflow_consumption_curve" to freeflowCurve,
            "ev_initial_charge" to "78",
            "auxiliary_consumption" to "80",
            "ev_pre_conditioning_time" to preConditioningTime,
            "ev_unconditioned_charging_curve" to unconditionedChargingCurve,
        )

        evDynamicDataHolder.updateData(data1)
        evDynamicDataHolder.updateData(data2)

        assertEquals(expected, evDynamicDataHolder.currentData(initial))
    }

    @Test
    fun `currentData() with non-string EV value in initial`() {
        val initial = mapOf(
            "engine" to JsonPrimitive("electric"),
            "energy_consumption_curve" to JsonPrimitive(curve),
            "ev_freeflow_consumption_curve" to JsonPrimitive(freeflowCurve),
            "ev_initial_charge" to JsonPrimitive(charge),
            "auxiliary_consumption" to JsonObject(),
            "ev_pre_conditioning_time" to JsonPrimitive(preConditioningTime),
            "ev_unconditioned_charging_curve" to JsonPrimitive(unconditionedChargingCurve),
            "aaa" to JsonPrimitive("bbb"),
        )
        val expected = mapOf(
            "energy_consumption_curve" to curve,
            "ev_freeflow_consumption_curve" to freeflowCurve,
            "ev_initial_charge" to charge,
            "ev_pre_conditioning_time" to preConditioningTime,
            "ev_unconditioned_charging_curve" to unconditionedChargingCurve,
        )

        assertEquals(expected, evDynamicDataHolder.currentData(initial))
    }

    @Test
    fun `currentData() drops directions-only battery conditioning keys from EV fallback`() {
        // A valid battery-model route: ev_battery_temperature is the only battery parameter that may
        // be refreshed, so the other three must be dropped from the data carried on a refresh.
        val batteryModelInitial = mapOf(
            "energy_consumption_curve" to JsonPrimitive(curve),
            "ev_freeflow_consumption_curve" to JsonPrimitive(freeflowCurve),
            "ev_initial_charge" to JsonPrimitive(charge),
            "auxiliary_consumption" to JsonPrimitive(auxConsumption),
            "ev_battery_temperature" to JsonPrimitive(batteryTemperature),
            "ev_battery_charging_power_at_temperature" to JsonPrimitive("-5.0,10.0;0.0,100.0"),
            "ev_battery_charging_power_optimal_temperature" to JsonPrimitive("10.0,-5.0;100.0,0.0"),
            "ev_battery_heating_parameters" to JsonPrimitive("1.0,2.0,3.0"),
            "aaa" to JsonPrimitive("bbb"),
        )

        val result = evDynamicDataHolder.currentData(batteryModelInitial)

        assertEquals(null, result["ev_battery_charging_power_at_temperature"])
        assertEquals(null, result["ev_battery_charging_power_optimal_temperature"])
        assertEquals(null, result["ev_battery_heating_parameters"])
        assertEquals(batteryTemperature, result["ev_battery_temperature"])
    }

    @Test
    fun `route refresh keeps the updated battery temperature and drops directions-only battery keys`() {
        // During guidance the integrator pushes a new battery temperature; the other battery model
        // parameters stay on the initial directions request only and must not survive the refresh.
        val updatedBatteryTemperature = "30.0"
        evDynamicDataHolder.updateData(
            mapOf("ev_battery_temperature" to updatedBatteryTemperature),
        )
        val batteryModelInitial = mapOf(
            "energy_consumption_curve" to JsonPrimitive(curve),
            "ev_initial_charge" to JsonPrimitive(charge),
            "ev_battery_temperature" to JsonPrimitive(batteryTemperature),
            "ev_battery_charging_power_at_temperature" to JsonPrimitive("-5.0,10.0;0.0,100.0"),
            "ev_battery_charging_power_optimal_temperature" to JsonPrimitive("10.0,-5.0;100.0,0.0"),
            "ev_battery_heating_parameters" to JsonPrimitive("1.0,2.0,3.0"),
        )

        val result = evDynamicDataHolder.currentData(batteryModelInitial)

        assertEquals(updatedBatteryTemperature, result["ev_battery_temperature"])
        assertEquals(charge, result["ev_initial_charge"])
        assertEquals(null, result["ev_battery_charging_power_at_temperature"])
        assertEquals(null, result["ev_battery_charging_power_optimal_temperature"])
        assertEquals(null, result["ev_battery_heating_parameters"])
    }

    @Test
    fun `currentData() with ev_freeflow_consumption_curve absent from initial but present in update`() {
        val updatedFreeflowCurve = "0,25;50,90"
        evDynamicDataHolder.updateData(
            mapOf("ev_freeflow_consumption_curve" to updatedFreeflowCurve),
        )

        val result = evDynamicDataHolder.currentData(emptyMap())

        assertEquals(updatedFreeflowCurve, result["ev_freeflow_consumption_curve"])
    }

    @Test
    fun `internal values are available only in updatedData()`() {
        val data = mapOf(
            "aaa" to "bbb",
            EV_EFFICIENCY_KEY to "33",
        )
        evDynamicDataHolder.updateData(data)

        assertEquals(
            mapOf("aaa" to "bbb"),
            evDynamicDataHolder.currentData(emptyMap()),
        )
        assertEquals(
            data,
            evDynamicDataHolder.updatedRawData().value,
        )
    }

    @Test
    fun `battery conditioning update drops the stale legacy preconditioning pair`() {
        evDynamicDataHolder.updateData(
            mapOf(
                "ev_pre_conditioning_time" to preConditioningTime,
                "ev_unconditioned_charging_curve" to unconditionedChargingCurve,
            ),
        )
        evDynamicDataHolder.updateData(
            mapOf(
                "ev_battery_temperature" to batteryTemperature,
                "ev_battery_heating_parameters" to "1.0,2.0,3.0",
            ),
        )

        val expected = mapOf(
            "ev_battery_temperature" to batteryTemperature,
            "ev_battery_heating_parameters" to "1.0,2.0,3.0",
        )
        assertEquals(expected, evDynamicDataHolder.updatedRawData().value)
        assertEquals(expected, evDynamicDataHolder.currentData(emptyMap()))
    }

    @Test
    fun `legacy preconditioning update drops the stale battery conditioning model`() {
        evDynamicDataHolder.updateData(
            mapOf(
                "ev_battery_temperature" to batteryTemperature,
                "ev_battery_heating_parameters" to "1.0,2.0,3.0",
            ),
        )
        evDynamicDataHolder.updateData(
            mapOf(
                "ev_pre_conditioning_time" to preConditioningTime,
                "ev_unconditioned_charging_curve" to unconditionedChargingCurve,
            ),
        )

        // ev_battery_temperature is part of the battery model, so it is dropped together with the
        // rest of it: it cannot be sent alongside the legacy pair.
        val expected = mapOf(
            "ev_pre_conditioning_time" to preConditioningTime,
            "ev_unconditioned_charging_curve" to unconditionedChargingCurve,
        )
        assertEquals(expected, evDynamicDataHolder.updatedRawData().value)
    }

    @Test
    fun `single update with both preconditioning groups keeps battery model and drops legacy`() {
        evDynamicDataHolder.updateData(
            mapOf(
                "ev_battery_temperature" to batteryTemperature,
                "ev_battery_heating_parameters" to "1.0,2.0,3.0",
                "ev_pre_conditioning_time" to preConditioningTime,
                "ev_unconditioned_charging_curve" to unconditionedChargingCurve,
            ),
        )

        val expected = mapOf(
            "ev_battery_temperature" to batteryTemperature,
            "ev_battery_heating_parameters" to "1.0,2.0,3.0",
        )
        assertEquals(expected, evDynamicDataHolder.updatedRawData().value)
        assertEquals(expected, evDynamicDataHolder.currentData(emptyMap()))
    }

    @Test
    fun `currentData() does not restore legacy preconditioning from initial when battery model is active`() {
        evDynamicDataHolder.updateData(
            mapOf(
                "ev_battery_temperature" to batteryTemperature,
                "ev_battery_heating_parameters" to "1.0,2.0,3.0",
            ),
        )
        val initialWithLegacy = mapOf(
            "ev_pre_conditioning_time" to JsonPrimitive(preConditioningTime),
            "ev_unconditioned_charging_curve" to JsonPrimitive(unconditionedChargingCurve),
        )

        val result = evDynamicDataHolder.currentData(initialWithLegacy)

        assertEquals(null, result["ev_pre_conditioning_time"])
        assertEquals(null, result["ev_unconditioned_charging_curve"])
    }
}
