package com.mapbox.navigation.ui.maps.util

import com.mapbox.navigation.testing.NavSDKRobolectricTestRunner
import com.mapbox.navigation.ui.maps.util.CacheResultUtils.cacheResult
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(NavSDKRobolectricTestRunner::class)
class CacheResultUtilsTest {

    @Test
    fun cacheResult() {
        var counter = 0
        val testFun: (a: Int) -> Int = { a: Int ->
            counter += 1
            a + counter
        }.cacheResult(1)
        testFun(5)
        testFun(5)

        val result = testFun(5)

        assertEquals(6, result)
        assertEquals(1, counter)
    }

    @Test
    fun cacheMaxSize() {
        var counter = 0
        val testFun: (a: Int) -> Int = { a: Int ->
            counter += 1
            a + counter
        }.cacheResult(1)
        testFun(5)
        testFun(5)
        testFun(5)
        testFun(10)

        val result = testFun(5)

        assertEquals(8, result)
        assertEquals(3, counter)
    }
}
