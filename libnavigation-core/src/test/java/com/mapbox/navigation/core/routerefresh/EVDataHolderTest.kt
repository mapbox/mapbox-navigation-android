package com.mapbox.navigation.core.routerefresh

import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class EVDataHolderTest {

    private val evDataHolder = EVDataHolder()
    private val curve = "0,40;50,120"
    private val charge = "90"
    private val auxConsumption = "70"
    private val preConditioningTime = "10"
    private val electricInitial = mapOf(
        "engine" to JsonPrimitive("electric"),
        "energy_consumption_curve" to JsonPrimitive(curve),
        "ev_initial_charge" to JsonPrimitive(charge),
        "auxiliary_consumption" to JsonPrimitive(auxConsumption),
        "ev_pre_conditioning_time" to JsonPrimitive(preConditioningTime),
        "aaa" to JsonPrimitive("bbb"),
    )
    private val nonElectricInitial = mapOf(
        "engine" to JsonPrimitive("non-electric"),
        "energy_consumption_curve" to JsonPrimitive(curve),
        "ev_initial_charge" to JsonPrimitive(charge),
        "auxiliary_consumption" to JsonPrimitive(auxConsumption),
        "ev_pre_conditioning_time" to JsonPrimitive(preConditioningTime),
        "aaa" to JsonPrimitive("bbb"),
    )

    @Test
    fun `currentData() with no updates and empty initial`() {
        assertEquals(0, evDataHolder.currentData(emptyMap()).size)
    }

    @Test
    fun `currentData() with no updates and non-EV initial`() {
        assertEquals(0, evDataHolder.currentData(nonElectricInitial).size)
    }

    @Test
    fun `currentData() with no updates and EV fallback`() {
        val expected = mapOf(
            "energy_consumption_curve" to curve,
            "ev_initial_charge" to charge,
            "auxiliary_consumption" to auxConsumption,
            "ev_pre_conditioning_time" to preConditioningTime,
        )
        assertEquals(expected, evDataHolder.currentData(electricInitial))
    }

    @Test
    fun `currentData() after one update with empty initial`() {
        val data = mapOf("aaa" to "bbb", "ccc" to "ddd")
        evDataHolder.updateData(data)

        assertEquals(data, evDataHolder.currentData(emptyMap()))
    }

    @Test
    fun `currentData() after one update with non-electric initial`() {
        val data = mapOf("aaa" to "bbb", "ccc" to "ddd")
        evDataHolder.updateData(data)

        assertEquals(data, evDataHolder.currentData(nonElectricInitial))
    }

    @Test
    fun `currentData() after one update with electric initial`() {
        val data = mapOf(
            "energy_consumption_curve" to "3,300",
            "auxiliary_consumption" to "80"
        )
        val expected = mapOf(
            "energy_consumption_curve" to "3,300",
            "ev_initial_charge" to charge,
            "auxiliary_consumption" to "80",
            "ev_pre_conditioning_time" to preConditioningTime,
        )
        evDataHolder.updateData(data)

        assertEquals(expected, evDataHolder.currentData(electricInitial))
    }

    @Test
    fun `currentData() after two updates with empty initial`() {
        val data1 = mapOf("aaa" to "bbb", "ccc" to "ddd", "eee" to "fff")
        val data2 = mapOf("ccc" to "zzz", "ggg" to "yyy")
        val expected = mapOf("aaa" to "bbb", "ccc" to "zzz", "ggg" to "yyy", "eee" to "fff")
        evDataHolder.updateData(data1)
        evDataHolder.updateData(data2)

        assertEquals(expected, evDataHolder.currentData(emptyMap()))
    }

    @Test
    fun `currentData() after two updates with non-electric initial`() {
        val data1 = mapOf("aaa" to "bbb", "ccc" to "ddd", "eee" to "fff")
        val data2 = mapOf("ccc" to "zzz", "ggg" to "yyy")
        val expected = mapOf("aaa" to "bbb", "ccc" to "zzz", "ggg" to "yyy", "eee" to "fff")
        evDataHolder.updateData(data1)
        evDataHolder.updateData(data2)

        assertEquals(expected, evDataHolder.currentData(nonElectricInitial))
    }

    @Test
    fun `currentData() after two updates with electric initial`() {
        val data1 = mapOf(
            "energy_consumption_curve" to "3,300",
            "ev_initial_charge" to "78",
            "eee" to "fff"
        )
        val data2 = mapOf(
            "energy_consumption_curve" to "4,400",
            "auxiliary_consumption" to "80",
            "aaa" to "bbb"
        )
        val expected = mapOf(
            "aaa" to "bbb",
            "eee" to "fff",
            "energy_consumption_curve" to "4,400",
            "ev_initial_charge" to "78",
            "auxiliary_consumption" to "80",
            "ev_pre_conditioning_time" to preConditioningTime,
        )

        evDataHolder.updateData(data1)
        evDataHolder.updateData(data2)

        assertEquals(expected, evDataHolder.currentData(electricInitial))
    }

    @Test
    fun `currentData() with non-string engine in initial`() {
        val initial = mapOf(
            "engine" to JsonObject(),
            "energy_consumption_curve" to JsonPrimitive(curve),
            "ev_initial_charge" to JsonPrimitive(charge),
            "auxiliary_consumption" to JsonPrimitive(auxConsumption),
            "ev_pre_conditioning_time" to JsonPrimitive(preConditioningTime),
            "aaa" to JsonPrimitive("bbb"),
        )

        assertEquals(0, evDataHolder.currentData(initial).size)
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

        assertEquals(expected, evDataHolder.currentData(initial))
    }
}
