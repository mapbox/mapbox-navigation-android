package com.mapbox.navigation.core.fasterroute

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteComparatorTest {

    private val routeComparator = RouteComparator()

    @Test
    fun `route with different steps is new`() {
        val routeProgress: RouteProgress = mockk {
            every { currentLegProgress } returns mockk {
                every { legIndex } returns 0
                every { currentStepProgress } returns mockk {
                    every { stepIndex } returns 0
                }
                every { routeLeg } returns mockk {
                    every { steps() } returns listOf(
                        mockk { every { name() } returns "Pennsylvania Avenue" },
                        mockk { every { name() } returns "19th Street" },
                        mockk { every { name() } returns "Arkansas Street" }
                    )
                }
            }
        }
        val directionsRoute: DirectionsRoute = mockk {
            every { legs() } returns listOf(
                mockk {
                    every { steps() } returns listOf(
                        mockk { every { name() } returns "Pennsylvania Avenue" },
                        mockk { every { name() } returns "20th Street" },
                        mockk { every { name() } returns "Arkansas Street" }
                    )
                }
            )
        }

        val isNewRoute = routeComparator.isRouteDescriptionDifferent(routeProgress, directionsRoute)

        assertTrue(isNewRoute)
    }

    @Test
    fun `route with same steps is not new`() {
        val routeProgress: RouteProgress = mockk {
            every { currentLegProgress } returns mockk {
                every { legIndex } returns 0
                every { currentStepProgress } returns mockk {
                    every { stepIndex } returns 0
                }
                every { routeLeg } returns mockk {
                    every { steps() } returns listOf(
                        mockk { every { name() } returns "Pennsylvania Avenue" },
                        mockk { every { name() } returns "20th Street" },
                        mockk { every { name() } returns "De Haro Street" }
                    )
                }
            }
        }
        val directionsRoute: DirectionsRoute = mockk {
            every { legs() } returns listOf(
                mockk {
                    every { steps() } returns listOf(
                        mockk { every { name() } returns "Pennsylvania Avenue" },
                        mockk { every { name() } returns "20th Street" },
                        mockk { every { name() } returns "De Haro Street" }
                    )
                }
            )
        }

        val isNewRoute = routeComparator.isRouteDescriptionDifferent(routeProgress, directionsRoute)

        assertFalse(isNewRoute)
    }

    @Test
    fun `stepIndex should clip route progress`() {
        val routeProgress: RouteProgress = mockk {
            every { currentLegProgress } returns mockk {
                every { legIndex } returns 0
                every { currentStepProgress } returns mockk {
                    every { stepIndex } returns 4
                }
                every { routeLeg } returns mockk {
                    every { steps() } returns listOf(
                        mockk { every { name() } returns "Pennsylvania Avenue" },
                        mockk { every { name() } returns "Cesar Chavez Street" },
                        mockk { every { name() } returns "Bryant Street" },
                        mockk { every { name() } returns "Precita Avenue" },
                        mockk { every { name() } returns "Alabama Street" },
                        mockk { every { name() } returns "Bradford Street" },
                        mockk { every { name() } returns "Nevada Street" }
                    )
                }
            }
        }
        val directionsRoute: DirectionsRoute = mockk {
            every { legs() } returns listOf(
                mockk {
                    every { steps() } returns listOf(
                        mockk { every { name() } returns "Alabama Street" },
                        mockk { every { name() } returns "Bradford Street" },
                        mockk { every { name() } returns "Nevada Street" }
                    )
                }
            )
        }

        val isNewRoute = routeComparator.isRouteDescriptionDifferent(routeProgress, directionsRoute)

        assertFalse(isNewRoute)
    }
}
