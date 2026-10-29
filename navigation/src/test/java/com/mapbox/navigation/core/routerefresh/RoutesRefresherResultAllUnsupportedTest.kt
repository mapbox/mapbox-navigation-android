package com.mapbox.navigation.core.routerefresh

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class RoutesRefresherResultAllUnsupportedTest(
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
                arrayOf(RouteRefresherStatus.Unsupported, emptyList<RouteRefresherStatus>(), true),
                arrayOf(
                    RouteRefresherStatus.Success(mockk()),
                    emptyList<RouteRefresherStatus>(),
                    false,
                ),
                arrayOf(RouteRefresherStatus.Failure, emptyList<RouteRefresherStatus>(), false),
                arrayOf(RouteRefresherStatus.Invalidated, emptyList<RouteRefresherStatus>(), false),
                arrayOf(
                    RouteRefresherStatus.Unsupported,
                    listOf(RouteRefresherStatus.Unsupported, RouteRefresherStatus.Unsupported),
                    true,
                ),

                // #5
                arrayOf(
                    RouteRefresherStatus.Unsupported,
                    listOf(RouteRefresherStatus.Unsupported, RouteRefresherStatus.Failure),
                    false,
                ),
                arrayOf(
                    RouteRefresherStatus.Unsupported,
                    listOf(RouteRefresherStatus.Success(mockk())),
                    false,
                ),
                arrayOf(
                    RouteRefresherStatus.Unsupported,
                    listOf(RouteRefresherStatus.Invalidated),
                    false,
                ),
                arrayOf(
                    RouteRefresherStatus.Failure,
                    listOf(RouteRefresherStatus.Unsupported),
                    false,
                ),
                // A route the validator rejects is not one the router can never refresh.
                arrayOf(RouteRefresherStatus.Invalid, emptyList<RouteRefresherStatus>(), false),
            )
        }
    }

    @Test
    fun allUnsupported() {
        val result = RoutesRefresherResult(
            mockk { every { status } returns primaryStatus },
            alternativesStatus.map { mockk { every { status } returns it } },
        )

        assertEquals(expected, result.allUnsupported())
    }
}
