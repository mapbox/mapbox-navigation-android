package com.mapbox.navigation.utils.internal

import com.mapbox.navigation.testing.toDataRef
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DataRefExTest {

    @Test
    fun isNotEmpty_empty() {
        val input = byteArrayOf().toDataRef()

        assertFalse(input.isNotEmpty())
    }

    @Test
    fun isNotEmpty_nonEmpty() {
        val input = byteArrayOf(1, 2, 1, 2).toDataRef()

        assertTrue(input.isNotEmpty())
    }

    @Test
    fun toReader_nonEmpty() {
        val input = byteArrayOf(1, 2, 1, 2)

        val reader = input.toDataRef().toReader()

        val output = reader.readText().toByteArray()

        assertTrue(output.contentEquals(input))
    }
}
