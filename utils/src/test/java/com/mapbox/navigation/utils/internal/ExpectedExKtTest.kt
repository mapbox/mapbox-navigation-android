package com.mapbox.navigation.utils.internal

import com.mapbox.bindgen.ExpectedFactory
import org.junit.Assert.assertEquals
import org.junit.Test

class ExpectedExKtTest {

    @Test
    fun `fold value`() {
        val expected = ExpectedFactory.createValue<Int, Int>(5)
        val result = expected.foldInline(
            errorHandler = { "error:$it" },
            valueHandler = { "value:$it" },
        )
        assertEquals(
            "value:5",
            result,
        )
    }

    @Test
    fun `fold error`() {
        val expected = ExpectedFactory.createError<Int, Int>(8)
        val result = expected.foldInline(
            errorHandler = { "error:$it" },
            valueHandler = { "value:$it" },
        )
        assertEquals(
            "error:8",
            result,
        )
    }
}
