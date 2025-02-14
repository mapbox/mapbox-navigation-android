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
    private val charge = "90"
    private val auxConsumption = "70"
    private val preConditioningTime = "10"
    private val initial = mapOf(
        "energy_consumption_curve" to JsonPrimitive(curve),
        "ev_initial_charge" to JsonPrimitive(charge),
        "auxiliary_consumption" to JsonPrimitive(auxConsumption),
        "ev_pre_conditioning_time" to JsonPrimitive(preConditioningTime),
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
            "ev_initial_charge" to charge,
            "auxiliary_consumption" to auxConsumption,
            "ev_pre_conditioning_time" to preConditioningTime,
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
            "ev_initial_charge" to charge,
            "auxiliary_consumption" to "80",
            "ev_pre_conditioning_time" to preConditioningTime,
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
            "ev_initial_charge" to "78",
            "auxiliary_consumption" to "80",
            "ev_pre_conditioning_time" to preConditioningTime,
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
            "ev_initial_charge" to JsonPrimitive(charge),
            "auxiliary_consumption" to JsonObject(),
            "ev_pre_conditioning_time" to JsonPrimitive(preConditioningTime),
            "aaa" to JsonPrimitive("bbb"),
        )
        val expected = mapOf(
            "energy_consumption_curve" to curve,
            "ev_initial_charge" to charge,
            "ev_pre_conditioning_time" to preConditioningTime,
        )

        assertEquals(expected, evDynamicDataHolder.currentData(initial))
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
}
