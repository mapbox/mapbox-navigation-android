package com.mapbox.navigation.ui.maps.internal.camera

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.ui.maps.testing.TestingUtil.loadNavigationRoute
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class RoutesIndicesConverterTest(
    private val routes: List<NavigationRoute>,
    private val inputId: String,
    private val inputLegIndex: Int,
    private val inputStepIndex: Int,
    private val inputLegGeometryIndex: Int,
    private val expectedOutput: Int?,
) {

    companion object {

        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<Any?>> {
            val route1 = loadNavigationRoute("short_route.json")
            val route2 = loadNavigationRoute("multileg_route.json")
            return listOf(
                // #0
                arrayOf(emptyList<NavigationRoute>(), "id", 0, 0, 0, null),
                arrayOf(listOf(route1), "id", 0, 0, 0, null),
                arrayOf(listOf(route1), route1.id, 0, 0, 0, 0),
                arrayOf(listOf(route1), route1.id, 0, 0, 1, 1),
                arrayOf(listOf(route1), route1.id, 0, 0, 2, null),
                // #5
                arrayOf(listOf(route1), route1.id, 0, 1, 0, null),
                arrayOf(listOf(route1), route1.id, 0, 1, 1, 0),
                arrayOf(listOf(route1), route1.id, 0, 1, 2, 1),
                arrayOf(listOf(route1), route1.id, 0, 1, 3, 2),
                arrayOf(listOf(route1), route1.id, 0, 1, 4, null),
                // #10
                arrayOf(listOf(route1), route1.id, 0, 2, 2, null),
                arrayOf(listOf(route1), route1.id, 0, 2, 3, 0),
                arrayOf(listOf(route1), route1.id, 0, 2, 4, 1),
                arrayOf(listOf(route1), route1.id, 0, 2, 5, null),
                arrayOf(listOf(route1), route1.id, 0, 3, 3, null),
                // #15
                arrayOf(listOf(route1), route1.id, 0, 3, 4, 0),
                arrayOf(listOf(route1), route1.id, 0, 3, 5, null),
                arrayOf(listOf(route1), route1.id, 1, 0, 0, null),
                arrayOf(listOf(route1, route2), route1.id, 0, 2, 4, 1),
                arrayOf(listOf(route1, route2), route2.id, 1, 2, 12, 2),
            )
        }
    }

    private val converter = RoutesIndicesConverter()

    @Test
    fun convert() {
        converter.onRoutesChanged(routes)
        assertEquals(
            expectedOutput,
            converter.convert(inputId, inputLegIndex, inputStepIndex, inputLegGeometryIndex),
        )
    }
}
