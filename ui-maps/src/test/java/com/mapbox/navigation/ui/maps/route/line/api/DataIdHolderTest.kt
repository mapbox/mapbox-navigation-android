package com.mapbox.navigation.ui.maps.route.line.api

import org.junit.Assert.assertEquals
import org.junit.Test

class DataIdHolderTest {

    private val holder = DataIdHolder()

    @Test
    fun incrementDataId() {
        val actual1 = holder.incrementDataId("source1")

        assertEquals(1, actual1)

        val actual2 = holder.incrementDataId("source1")

        assertEquals(2, actual2)

        val actual3 = holder.incrementDataId("source2")

        assertEquals(1, actual3)

        val actual4 = holder.incrementDataId("source2")

        assertEquals(2, actual4)
    }
}
