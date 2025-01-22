package com.mapbox.navigation.utils.internal

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class CollectionsTest(private val testData: TestData) {

    @Test
    fun test() {
        with(testData) {
            assertEquals(expectedResult, list.takeEvenly(n))
        }
    }

    data class TestData(
        val list: List<Int>,
        val n: Int,
        val expectedResult: List<Int>,
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data() = listOf(
            TestData(emptyList(), 0, emptyList()),
            TestData(emptyList(), 10, emptyList()),

            TestData(listOf(0, 1, 2), 0, listOf()),
            TestData(listOf(0, 1, 2), 1, listOf(0)),
            TestData(listOf(0, 1, 2), 2, listOf(0, 2)),
            TestData(listOf(0, 1, 2), 10, listOf(0, 1, 2)),

            TestData(listOf(0, 1, 2, 3, 4, 5), 0, listOf()),
            TestData(listOf(0, 1, 2, 3, 4, 5), 1, listOf(0)),
            TestData(listOf(0, 1, 2, 3, 4, 5), 2, listOf(0, 5)),
            TestData(listOf(0, 1, 2, 3, 4, 5), 3, listOf(0, 3, 5)),

            TestData(listOf(0, 1, 2, 3, 4, 5, 6), 3, listOf(0, 3, 6)),
        )
    }
}
