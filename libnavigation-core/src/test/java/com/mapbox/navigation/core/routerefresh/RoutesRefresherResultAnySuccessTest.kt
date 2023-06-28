package com.mapbox.navigation.core.routerefresh

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class RoutesRefresherResultAnySuccessTest(
    private val primarySuccess: Boolean,
    private val alternativesSuccess: List<Boolean>,
    private val expected: Boolean,
) {

    companion object {

        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<Any>> {
            return listOf(
                // #0
                arrayOf(false, emptyList<Boolean>(), false),
                arrayOf(true, emptyList<Boolean>(), true),
                arrayOf(false, listOf(false), false),
                arrayOf(true, listOf(false), true),
                arrayOf(false, listOf(true), true),

                // #5
                arrayOf(true, listOf(true), true),
                arrayOf(false, listOf(false, false), false),
                arrayOf(true, listOf(false, false), true),
                arrayOf(false, listOf(true, false), true),
                arrayOf(false, listOf(false, true), true),

                // #10
                arrayOf(true, listOf(true, false), true),
                arrayOf(true, listOf(false, true), true),
                arrayOf(true, listOf(true, true), true),
            )
        }
    }

    @Test
    fun anySuccess() {
        val result = RoutesRefresherResult(
            mockk { every { isSuccess() } returns primarySuccess },
            alternativesSuccess.map { mockk { every { isSuccess() } returns it } }
        )

        assertEquals(expected, result.anySuccess())
    }
}
