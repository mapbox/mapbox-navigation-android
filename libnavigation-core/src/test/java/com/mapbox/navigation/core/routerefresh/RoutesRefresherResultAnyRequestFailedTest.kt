package com.mapbox.navigation.core.routerefresh

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class RoutesRefresherResultAnyRequestFailedTest(
    private val primaryStatus: RouteRefresherStatus,
    private val alternativesStatus: List<RouteRefresherStatus>,
    private val expected: Boolean,
) {

    companion object {

        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<Any>> {
            return listOf(
                // #0
                arrayOf(RouteRefresherStatus.SUCCESS, emptyList<Boolean>(), false),
                arrayOf(RouteRefresherStatus.INVALID, emptyList<Boolean>(), false),
                arrayOf(RouteRefresherStatus.INVALIDATED, emptyList<Boolean>(), false),
                arrayOf(RouteRefresherStatus.FAILURE, emptyList<Boolean>(), true),
                arrayOf(RouteRefresherStatus.INVALID, listOf(RouteRefresherStatus.SUCCESS), false),

                // #5
                arrayOf(
                    RouteRefresherStatus.FAILURE,
                    listOf(RouteRefresherStatus.INVALIDATED),
                    true
                ),
                arrayOf(RouteRefresherStatus.SUCCESS, listOf(RouteRefresherStatus.FAILURE), true),
                arrayOf(RouteRefresherStatus.FAILURE, listOf(RouteRefresherStatus.FAILURE), true),
                arrayOf(
                    RouteRefresherStatus.SUCCESS,
                    listOf(RouteRefresherStatus.INVALIDATED, RouteRefresherStatus.INVALID),
                    false
                ),
                arrayOf(
                    RouteRefresherStatus.FAILURE,
                    listOf(RouteRefresherStatus.SUCCESS, RouteRefresherStatus.INVALIDATED),
                    true
                ),

                // #10
                arrayOf(
                    RouteRefresherStatus.INVALIDATED,
                    listOf(RouteRefresherStatus.FAILURE, RouteRefresherStatus.SUCCESS),
                    true
                ),
                arrayOf(
                    RouteRefresherStatus.SUCCESS,
                    listOf(RouteRefresherStatus.INVALID, RouteRefresherStatus.FAILURE),
                    true
                ),
                arrayOf(
                    RouteRefresherStatus.FAILURE,
                    listOf(RouteRefresherStatus.FAILURE, RouteRefresherStatus.INVALIDATED),
                    true
                ),
                arrayOf(
                    RouteRefresherStatus.FAILURE,
                    listOf(RouteRefresherStatus.SUCCESS, RouteRefresherStatus.FAILURE),
                    true
                ),
                arrayOf(
                    RouteRefresherStatus.FAILURE,
                    listOf(RouteRefresherStatus.FAILURE, RouteRefresherStatus.FAILURE),
                    true
                ),
            )
        }
    }

    @Test
    fun anyRequestFailed() {
        val result = RoutesRefresherResult(
            mockk { every { status } returns primaryStatus },
            alternativesStatus.map { mockk { every { status } returns it } }
        )

        assertEquals(expected, result.anyRequestFailed())
    }
}
