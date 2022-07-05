package com.mapbox.navigation.core.trip.roadobject.reststop

import com.mapbox.api.directions.v5.models.StepIntersection
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectProvider
import com.mapbox.navigation.base.trip.model.roadobject.UpcomingRoadObject
import com.mapbox.navigation.base.trip.model.roadobject.reststop.RestStop
import com.mapbox.navigation.base.trip.model.roadobject.tunnel.Tunnel
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RestStopFactoryTest {

    @Test
    fun `no rest stop when list of step intersection is null`() {
        val actual = RestStopFactory.createWithStepIntersection(null)

        assertTrue(actual.isEmpty())
    }

    @Test
    fun `no rest stop when list of step intersection is empty`() {
        val actual = RestStopFactory.createWithStepIntersection(emptyList())

        assertTrue(actual.isEmpty())
    }

    @Test
    fun `rest stop when list of step intersection is not empty`() {
        val mockRestStop = mockk<com.mapbox.api.directions.v5.models.RestStop> {
            every { name() } returns "rest stop name"
            every { type() } returns "service_area"
            every { amenities() } returns null
        }
        val mockStepIntersection1 = mockk<StepIntersection> {
            every { location() } returns Point.fromLngLat(11.0, 22.0)
            every { restStop() } returns mockRestStop
        }
        val mockStepIntersection2 = mockk<StepIntersection> {
            every { location() } returns Point.fromLngLat(21.0, 32.0)
            every { restStop() } returns null
        }
        val actual = RestStopFactory.createWithStepIntersection(
            listOf(mockStepIntersection1, mockStepIntersection2)
        )

        assertEquals(1, actual.size)
        assertEquals(mockRestStop.name(), actual.first().name)
        assertEquals(mockRestStop.type(), actual.first().type)
        assertEquals(mockRestStop.amenities(), actual.first().amenities)
    }

    @Test
    fun `no rest stop when list of upcoming road object is empty`() {
        val actual = RestStopFactory.createWithUpcomingRoadObjects(emptyList())

        assertTrue(actual.isEmpty())
    }

    @Test
    fun `rest stop when list of upcoming road object has no rest stop`() {
        val mockUpcomingRoadObject = mockk<UpcomingRoadObject> {
            every { roadObject } returns mockk<Tunnel>()
            every { distanceToStart } returns 123.0
            every { distanceInfo } returns null
        }
        val actual = RestStopFactory.createWithUpcomingRoadObjects(listOf(mockUpcomingRoadObject))

        assertTrue(actual.isEmpty())
    }

    @Test
    fun `rest stop when list of upcoming road object has rest stop`() {
        val mockRestStop = mockk<RestStop> {
            every { id } returns "uuid"
            every { name } returns "rest stop name"
            every { restStopType } returns 1
            every { amenities } returns null
            every { length } returns null
            every { location } returns mockk()
            every { provider } returns RoadObjectProvider.MAPBOX
        }
        val mockUpcomingRoadObject1 = mockk<UpcomingRoadObject> {
            every { roadObject } returns mockk<Tunnel>()
            every { distanceToStart } returns 123.0
            every { distanceInfo } returns null
        }
        val mockUpcomingRoadObject2 = mockk<UpcomingRoadObject> {
            every { roadObject } returns mockRestStop
            every { distanceToStart } returns 123.0
            every { distanceInfo } returns null
        }
        val actual = RestStopFactory.createWithUpcomingRoadObjects(
            listOf(mockUpcomingRoadObject1, mockUpcomingRoadObject2)
        )

        assertEquals(1, actual.size)
        assertEquals(mockRestStop.name, actual.first().restStop.name)
        assertEquals(mockRestStop.restStopType, actual.first().restStop.restStopType)
        assertEquals(mockRestStop.amenities, actual.first().restStop.amenities)
    }
}
