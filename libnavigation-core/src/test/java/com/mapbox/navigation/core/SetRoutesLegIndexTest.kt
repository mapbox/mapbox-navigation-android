package com.mapbox.navigation.core

import com.mapbox.navigation.core.internal.RouteProgressData
import com.mapbox.navigation.core.internal.utils.initialLegIndex
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
            arrayOf(SetRoutes.RefreshRoutes(RouteProgressData(1, 2, 3)), 1),
            arrayOf(SetRoutes.Reroute(5), 5),
            arrayOf(SetRoutes.Alternatives(6), 6),
        ).also {
            assertEquals(SetRoutes::class.sealedSubclasses.size, it.size)
        }
    }

    @Test
    fun initialLegIndex() {
        assertEquals(expected, input.initialLegIndex())
    }
}
