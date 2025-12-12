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
                arrayOf(RouteRefresherStatus.Success(mockk()), emptyList<Boolean>(), false),
                arrayOf(RouteRefresherStatus.Invalid, emptyList<Boolean>(), false),
                arrayOf(RouteRefresherStatus.Invalidated, emptyList<Boolean>(), false),
                arrayOf(RouteRefresherStatus.Failure, emptyList<Boolean>(), true),
                arrayOf(
                    RouteRefresherStatus.Invalid,
                    listOf(RouteRefresherStatus.Success(mockk())),
                    false,
                ),

                // #5
                arrayOf(
                    RouteRefresherStatus.Failure,
                    listOf(RouteRefresherStatus.Invalidated),
                    true,
                ),
                arrayOf(
                    RouteRefresherStatus.Success(mockk()),
                    listOf(RouteRefresherStatus.Failure),
                    true,
                ),
                arrayOf(RouteRefresherStatus.Failure, listOf(RouteRefresherStatus.Failure), true),
                arrayOf(
                    RouteRefresherStatus.Success(mockk()),
                    listOf(RouteRefresherStatus.Invalidated, RouteRefresherStatus.Invalid),
                    false,
                ),
                arrayOf(
                    RouteRefresherStatus.Failure,
                    listOf(RouteRefresherStatus.Success(mockk()), RouteRefresherStatus.Invalidated),
                    true,
                ),

                // #10
                arrayOf(
                    RouteRefresherStatus.Invalidated,
                    listOf(RouteRefresherStatus.Failure, RouteRefresherStatus.Success(mockk())),
                    true,
                ),
                arrayOf(
                    RouteRefresherStatus.Success(mockk()),
                    listOf(RouteRefresherStatus.Invalid, RouteRefresherStatus.Failure),
                    true,
                ),
                arrayOf(
                    RouteRefresherStatus.Failure,
                    listOf(RouteRefresherStatus.Failure, RouteRefresherStatus.Invalidated),
                    true,
                ),
                arrayOf(
                    RouteRefresherStatus.Failure,
                    listOf(RouteRefresherStatus.Success(mockk()), RouteRefresherStatus.Failure),
                    true,
                ),
                arrayOf(
                    RouteRefresherStatus.Failure,
                    listOf(RouteRefresherStatus.Failure, RouteRefresherStatus.Failure),
                    true,
                ),
            )
        }
    }

    @Test
    fun anyRequestFailed() {
        val result = RoutesRefresherResult(
            mockk { every { status } returns primaryStatus },
            alternativesStatus.map { mockk { every { status } returns it } },
        )

        assertEquals(expected, result.anyRequestFailed())
    }
}
