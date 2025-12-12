package com.mapbox.navigation.core

import com.mapbox.navigation.core.internal.RouteProgressData
import com.mapbox.navigation.core.internal.utils.initialLegIndex
import com.mapbox.navigation.core.routerefresh.RouteRefresherResult
import com.mapbox.navigation.core.routerefresh.RoutesRefresherResult
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class SetRoutesLegIndexTest(
    private val input: SetRoutes,
    private val expected: Int,
) {

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{1} for {0}")
        fun data(): Collection<Array<Any>> = listOf(
            arrayOf(SetRoutes.CleanUp, 0),
            arrayOf(SetRoutes.NewRoutes(4), 4),
            arrayOf(
                SetRoutes.RefreshRoutes.RefreshControllerRefresh(
                    RoutesRefresherResult(
                        RouteRefresherResult(
                            mockk(),
                            RouteProgressData(1, 2, 3),
                            mockk(),
                        ),
                        listOf(
                            RouteRefresherResult(
                                mockk(),
                                RouteProgressData(2, 2, 3),
                                mockk(),
                            ),
                        ),
                    ),
                ),
                1,
            ),
            arrayOf(
                SetRoutes.RefreshRoutes.ExternalRefresh(
                    legIndex = 2,
                    isManual = false,
                ),
                2,
            ),
            arrayOf(SetRoutes.Reroute(5), 5),
            arrayOf(SetRoutes.Alternatives(6), 6),
            arrayOf(SetRoutes.Reorder(7), 7),
        ).also {
            assertEquals(
                SetRoutes::class.sealedSubclasses.size +
                    SetRoutes.RefreshRoutes::class.sealedSubclasses.size - 1,
                it.size,
            )
        }
    }

    @Test
    fun initialLegIndex() {
        assertEquals(expected, input.initialLegIndex())
    }
}
