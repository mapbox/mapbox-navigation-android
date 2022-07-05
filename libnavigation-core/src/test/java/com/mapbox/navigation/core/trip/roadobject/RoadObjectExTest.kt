package com.mapbox.navigation.core.trip.roadobject

import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.api.directions.v5.models.StepIntersection
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.roadobject.UpcomingRoadObject
import com.mapbox.navigation.core.trip.roadobject.reststop.RestStopFactory
import com.mapbox.navigation.core.trip.roadobject.reststop.RestStopFromIntersection
import com.mapbox.navigation.core.trip.roadobject.reststop.RestStopFromRoadObject
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RoadObjectExTest {

    @Before
    fun setUp() {
        mockkObject(RestStopFactory)
    }

    @After
    fun tearDown() {
        unmockkObject(RestStopFactory)
    }

    @Test
    fun `get all rest stops using road object`() {
        val upcomingRoadObjects = listOf<UpcomingRoadObject>(mockk(), mockk())
        val mockRestStops = listOf<RestStopFromRoadObject>(mockk())
        every { RestStopFactory.createWithUpcomingRoadObjects(any()) } returns mockRestStops

        val actual = upcomingRoadObjects.getAllRestStops()

        assertEquals(mockRestStops, actual)
    }

    @Test
    fun `get first upcoming rest stop`() {
        val upcomingRoadObjects = listOf<UpcomingRoadObject>(mockk(), mockk())
        val mockRestStops = listOf<RestStopFromRoadObject>(
            mockk {
                every { distanceToStart } returns 123.0
            },
            mockk()
        )

        every { RestStopFactory.createWithUpcomingRoadObjects(any()) } returns mockRestStops

        val actual = upcomingRoadObjects.getFirstUpcomingRestStop()

        assertEquals(actual, mockRestStops.first())
    }

    @Test
    fun `get first upcoming rest stop when distanceToStart is null`() {
        val upcomingRoadObjects = listOf<UpcomingRoadObject>(mockk(), mockk())
        val mockRestStops = listOf<RestStopFromRoadObject>(
            mockk {
                every { distanceToStart } returns null
            },
            mockk()
        )

        every { RestStopFactory.createWithUpcomingRoadObjects(any()) } returns mockRestStops

        val actual = upcomingRoadObjects.getFirstUpcomingRestStop()

        assertNull(actual)
    }

    @Test
    fun `get first upcoming rest stop when distanceToStart is zero`() {
        val upcomingRoadObjects = listOf<UpcomingRoadObject>(mockk(), mockk())
        val mockRestStops = listOf<RestStopFromRoadObject>(
            mockk {
                every { distanceToStart } returns 0.0
            },
            mockk()
        )

        every { RestStopFactory.createWithUpcomingRoadObjects(any()) } returns mockRestStops

        val actual = upcomingRoadObjects.getFirstUpcomingRestStop()

        assertNull(actual)
    }

    @Test
    fun `get first upcoming rest stop when distanceToStart is negative`() {
        val upcomingRoadObjects = listOf<UpcomingRoadObject>(mockk(), mockk())
        val mockRestStops = listOf<RestStopFromRoadObject>(
            mockk {
                every { distanceToStart } returns -1.0
            },
            mockk()
        )

        every { RestStopFactory.createWithUpcomingRoadObjects(any()) } returns mockRestStops

        val actual = upcomingRoadObjects.getFirstUpcomingRestStop()

        assertNull(actual)
    }

    @Test
    fun `get first upcoming rest stop when empty`() {
        val upcomingRoadObjects = listOf<UpcomingRoadObject>(mockk(), mockk())
        val mockRestStops = emptyList<RestStopFromRoadObject>()

        every { RestStopFactory.createWithUpcomingRoadObjects(any()) } returns mockRestStops

        val actual = upcomingRoadObjects.getFirstUpcomingRestStop()

        assertNull(actual)
    }

    @Test
    fun `no rest stops returned when route leg is null`() {
        val navigationRoute = mockk<NavigationRoute> {
            every { id } returns "uuid"
            every { directionsRoute } returns mockk {
                every { legs() } returns null
            }
        }

        val actual = navigationRoute.getAllRestStops()

        assertTrue(actual.legIndexToRoadObject.isEmpty())
        assertEquals(actual.routeId, navigationRoute.id)
    }

    @Test
    fun `no rest stops returned when route leg step is null`() {
        val routeLeg = mockk<RouteLeg> {
            every { steps() } returns null
        }
        val navigationRoute = mockk<NavigationRoute> {
            every { id } returns "uuid"
            every { directionsRoute } returns mockk {
                every { legs() } returns listOf(routeLeg)
            }
        }

        val actual = navigationRoute.getAllRestStops()

        assertTrue(actual.legIndexToRoadObject[0]!!.isEmpty())
        assertEquals(actual.routeId, navigationRoute.id)
    }

    @Test
    fun `rest stops available`() {
        val stepIntersection: List<StepIntersection> = listOf(mockk())
        val legStep = mockk<LegStep> {
            every { intersections() } returns stepIntersection
        }
        val routeLeg = mockk<RouteLeg> {
            every { steps() } returns listOf(legStep)
        }
        val navigationRoute = mockk<NavigationRoute> {
            every { id } returns "uuid"
            every { directionsRoute } returns mockk {
                every { legs() } returns listOf(routeLeg)
            }
        }
        val mockRestStops = listOf<RestStopFromIntersection>(mockk())
        every { RestStopFactory.createWithStepIntersection(stepIntersection) } returns mockRestStops

        val actual = navigationRoute.getAllRestStops()

        assertEquals(actual.legIndexToRoadObject[0], mockRestStops)
        assertEquals(actual.routeId, navigationRoute.id)
    }
}
