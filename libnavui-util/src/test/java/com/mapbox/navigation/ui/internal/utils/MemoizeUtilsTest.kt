package com.mapbox.navigation.ui.internal.utils

import com.mapbox.navigation.utils.internal.MemoizeUtils.memoize
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MemoizeUtilsTest {

    @Test
    fun memoizeKey1Test() {
        var counter = 0
        val sumFun: (a: Int) -> Int = { a: Int ->
            counter += 1
            a + 1
        }.memoize(5)

        val firstResult = sumFun(1)
        val secondResult = sumFun(1)

        assertEquals(firstResult, secondResult)
        assertEquals(1, counter)
    }

    @Test
    fun memoizeKey2Test() {
        var counter = 0
        val sumFun: (a: Int, b: Int) -> Int = { a: Int, b: Int ->
            counter += 1
            a + b
        }.memoize(5)

        val firstResult = sumFun(1, 2)
        val secondResult = sumFun(1, 2)

        assertEquals(firstResult, secondResult)
        assertEquals(1, counter)
    }
}
