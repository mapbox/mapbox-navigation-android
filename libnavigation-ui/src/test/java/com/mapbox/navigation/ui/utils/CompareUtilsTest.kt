package com.mapbox.navigation.ui.utils

import com.mapbox.navigation.ui.internal.utils.CompareUtils
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompareUtilsTest {

    @Test
    fun areEqualContentsIgnoreOrderWhenContentsAreSame() {
        val listA = listOf("a", "b", "c")
        val listB = listOf("b", "a", "c")

        val result = CompareUtils.areEqualContentsIgnoreOrder(listA, listB)

        assertTrue(result)
    }

    @Test
    fun areEqualContentsIgnoreOrderWhenContentsAreNotSame() {
        val listA = listOf("a", "b", "c")
        val listB = listOf("b", "a", "x")

        val result = CompareUtils.areEqualContentsIgnoreOrder(listA, listB)

        assertFalse(result)
    }
}
