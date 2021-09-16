package com.mapbox.navigation.base.internal.utils

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.RouteLeg
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectionsRouteExTest {

    @Test
    fun isRouteTheSame() {
        val mockkRoute = mockk<DirectionsRoute> {
            every { geometry() } throws AssertionError(
                "we should check reference first"
            )
            every { legs() } throws AssertionError(
                "we should check reference first"
            )
        }
        val comparing = listOf<Triple<DirectionsRoute, DirectionsRoute?, Boolean>>(
            Triple(
                getDirectionRouteBuilder().geometry("geomery").build(),
                getDirectionRouteBuilder().geometry("geomery").build(),
                true
            ),
            Triple(
                getDirectionRouteBuilder().geometry("geomery_1").build(),
                getDirectionRouteBuilder().geometry("geomery").build(),
                false
            ),
            Triple(
                getDirectionRouteBuilder().geometry("geomery").build(),
                null,
                false
            ),
            Triple(
                getDirectionRouteBuilder().legs(
                    listOf(
                        getListOfRouteLegs("one, two"),
                        getListOfRouteLegs("three, four"),
                    )
                ).build(),
                getDirectionRouteBuilder().legs(
                    listOf(
                        getListOfRouteLegs("one, two"),
                        getListOfRouteLegs("three, four"),
                    )
                ).build(),
                true
            ),
            Triple(
                getDirectionRouteBuilder().legs(
                    listOf(
                        getListOfRouteLegs("one, two"),
                        getListOfRouteLegs("three, four"),
                    )
                ).build(),
                getDirectionRouteBuilder().legs(
                    listOf(
                        getListOfRouteLegs("one, two"),
                        getListOfRouteLegs("three, four_NOT"),
                    )
                ).build(),
                false
            ),
            Triple(
                mockkRoute,
                mockkRoute,
                true
            )
        )

        comparing.forEach { (route1, route2, compareResult) ->
            if (compareResult) {
                assertTrue(route1.isSameRoute(route2))
            } else {
                assertFalse(route1.isSameRoute(route2))
            }
        }
    }

    private fun getDirectionRouteBuilder(): DirectionsRoute.Builder =
        DirectionsRoute.builder().duration(1.0).distance(2.0)

    private fun getListOfRouteLegs(vararg stepsNames: String): RouteLeg =
        RouteLeg.builder()
            .also { legBuilder ->
                legBuilder.steps(
                    stepsNames.map {
                        LegStep.builder()
                            .name(it)
                            .distance(3.0)
                            .duration(4.0)
                            .mode("")
                            .maneuver(mockk(relaxUnitFun = true))
                            .weight(5.0)
                            .build()
                    }
                )
            }
            .build()
}
