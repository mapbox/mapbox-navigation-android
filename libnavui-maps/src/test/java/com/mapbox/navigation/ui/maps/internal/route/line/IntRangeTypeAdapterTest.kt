package com.mapbox.navigation.ui.maps.internal.route.line

import com.google.gson.GsonBuilder
import org.junit.Assert.assertEquals
import org.junit.Test

internal class IntRangeTypeAdapterTest {

    @Test
    fun serialiseNull() {
        val adapter = IntRangeTypeAdapter()
        val gson = GsonBuilder()
            .registerTypeAdapter(IntRange::class.java, adapter)
            .create()
        val intRange: IntRange? = null
        val holder = Holder(
            1,
            "aaaaa",
            intRange,
            10,
            "bbbb",
        )
        val json = gson.toJson(holder)
        val restored = gson.fromJson(json, Holder::class.java)
        assertEquals(holder, restored)
    }

    @Test
    fun serialiseNonNull() {
        val adapter = IntRangeTypeAdapter()
        val gson = GsonBuilder()
            .registerTypeAdapter(IntRange::class.java, adapter)
            .create()
        val intRange = 23..76
        val holder = Holder(
            1,
            "aaaaa",
            intRange,
            10,
            "bbbb",
        )
        val json = gson.toJson(holder)
        println(json)
        val restored = gson.fromJson(json, Holder::class.java)
        assertEquals(holder, restored)
    }

    private data class Holder(
        val a: Int,
        val b: String,
        val intRange: IntRange?,
        val c: Long,
        val d: String,
    )
}
