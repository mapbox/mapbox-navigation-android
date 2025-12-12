package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.core.internal.RouteProgressData
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class RouteRefresherResultTest(
    private val status: RouteRefresherStatus,
    private val expected: Boolean,
) {

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{0} to {1}")
        fun data(): Collection<Array<Any>> {
            val result: Collection<Array<Any>> = listOf(
                arrayOf(RouteRefresherStatus.Failure, false),
                arrayOf(RouteRefresherStatus.Invalidated, false),
                arrayOf(RouteRefresherStatus.Invalid, false),
                arrayOf(RouteRefresherStatus.Success(mockk()), true),
            )
            assertEquals(RouteRefresherStatus::class.sealedSubclasses.size, result.size)
            return result
        }
    }

    @Test
    fun isSuccess() {
        val input = RouteRefresherResult(mockk(), mockk<RouteProgressData>(), status)

        assertEquals(expected, input.isSuccess())
        assertEquals(expected, input.wasRouteUpdated)
    }
}
