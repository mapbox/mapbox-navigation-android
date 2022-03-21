package com.mapbox.navigation.core.replay.route

import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.history.ReplayEventBase
import com.mapbox.navigation.core.replay.history.ReplayEventUpdateLocation
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ReplayProgressObserverTest {

    private val mapboxReplayer = mockk<MapboxReplayer>(relaxed = true)
    private val replayRouteMapper = mockk<ReplayRouteMapper>(relaxed = true)
    private val replayProgressObserver = ReplayProgressObserver(mapboxReplayer, replayRouteMapper)

    @Test
    fun `should map progress and push to replayer`() {
        every { replayRouteMapper.mapRouteLegGeometry(any()) } returns mockEventsForShortRoute()

        replayProgressObserver.onRouteProgressChanged(
            mockValidRouteProgress(mockk(), mockDistanceTraveled = 0.0f)
        )

        verifyOrder {
            replayRouteMapper.mapRouteLegGeometry(any())
            mapboxReplayer.pushEvents(any())
            mapboxReplayer.seekTo(any<ReplayEventBase>())
        }
    }

    @Test
    fun `should push events once per route leg`() {
        every { replayRouteMapper.mapRouteLegGeometry(any()) } returns mockEventsForShortRoute()

        val mockRouteLeg = mockk<RouteLeg>()
        replayProgressObserver.onRouteProgressChanged(
            mockValidRouteProgress(mockRouteLeg, mockDistanceTraveled = 10.0f)
        )
        replayProgressObserver.onRouteProgressChanged(
            mockValidRouteProgress(mockRouteLeg, mockDistanceTraveled = 50.0f)
        )

        verify(exactly = 1) {
            replayRouteMapper.mapRouteLegGeometry(any())
            mapboxReplayer.pushEvents(any())
            mapboxReplayer.seekTo(any<ReplayEventBase>())
        }
    }

    @Test
    fun `should push new events for new route leg`() {
        every { replayRouteMapper.mapRouteLegGeometry(any()) } returns mockEventsForShortRoute()

        replayProgressObserver.onRouteProgressChanged(
            mockValidRouteProgress(mockk(), mockDistanceTraveled = 10.0f)
        )
        replayProgressObserver.onRouteProgressChanged(
            mockValidRouteProgress(mockk(), mockDistanceTraveled = 50.0f)
        )

        verify(exactly = 2) {
            replayRouteMapper.mapRouteLegGeometry(any())
            mapboxReplayer.pushEvents(any())
            mapboxReplayer.seekTo(any<ReplayEventBase>())
        }
    }

    @Test
    fun `should seekTo first event when distanceTraveled is zero`() {
        every { replayRouteMapper.mapRouteLegGeometry(any()) } returns mockEvents(
            """yg{bgA|cufhFoEiAiA[}i@oNoD_As@QqdAeX"""
        )
        val eventsSlot = slot<List<ReplayEventBase>>()
        val seekToSlot = slot<ReplayEventBase>()
        every { mapboxReplayer.pushEvents(capture(eventsSlot)) } returns mapboxReplayer
        every { mapboxReplayer.seekTo(capture(seekToSlot)) } just Runs

        replayProgressObserver.onRouteProgressChanged(
            mockValidRouteProgress(mockk(), 0.0f)
        )

        // Seek to first location because 0.0 distance traveled
        assertEquals(7, eventsSlot.captured.size)
        assertEquals(0.0, eventsSlot.captured[0].eventTimestamp, 0.0)
        assertEquals(0.0, seekToSlot.captured.eventTimestamp, 0.0)
    }

    @Test
    fun `should seekTo mid route when distanceTraveled is mid route`() {
        every { replayRouteMapper.mapRouteLegGeometry(any()) } returns mockEvents(
            """yg{bgA|cufhFoEiAiA[}i@oNoD_As@QqdAeX"""
        )
        val eventsSlot = slot<List<ReplayEventBase>>()
        val seekToSlot = slot<ReplayEventBase>()
        every { mapboxReplayer.pushEvents(capture(eventsSlot)) } returns mapboxReplayer
        every { mapboxReplayer.seekTo(capture(seekToSlot)) } just Runs

        replayProgressObserver.onRouteProgressChanged(
            mockValidRouteProgress(mockk(), 90.0f)
        )

        // Seek to the 3rd event timestamp because 90 meters has been traveled
        assertEquals(7, eventsSlot.captured.size)
        assertEquals(0.0, eventsSlot.captured[0].eventTimestamp, 0.0)
        assertEquals(3.0, seekToSlot.captured.eventTimestamp, 0.0)
    }

    @Test
    fun `should not push events when route is empty`() {
        every { replayRouteMapper.mapRouteLegGeometry(any()) } returns mockEvents("")
        val eventsSlot = slot<List<ReplayEventBase>>()
        val seekToSlot = slot<ReplayEventBase>()
        every { mapboxReplayer.pushEvents(capture(eventsSlot)) } returns mapboxReplayer
        every { mapboxReplayer.seekTo(capture(seekToSlot)) } just Runs

        replayProgressObserver.onRouteProgressChanged(
            mockValidRouteProgress(mockk(), 90.0f)
        )

        assertFalse(eventsSlot.isCaptured)
        assertFalse(seekToSlot.isCaptured)
    }

    @Test
    fun `should seekTo alternative route with distanceTraveled`() {
        val firstRouteLeg: RouteLeg = mockk()
        val secondRouteLeg: RouteLeg = mockk()
        every { replayRouteMapper.mapRouteLegGeometry(firstRouteLeg) } returns mockEvents(
            """yg{bgA|cufhFoEiAiA[}i@oNoD_As@QqdAeX"""
        )
        every { replayRouteMapper.mapRouteLegGeometry(secondRouteLeg) } returns mockEvents(
            """yg{bgA|cufhFoEiAiA[}i@oNoD_As@Q"""
        )
        val eventsSlot = mutableListOf<List<ReplayEventBase>>()
        val seekToSlot = mutableListOf<ReplayEventBase>()
        every { mapboxReplayer.pushEvents(capture(eventsSlot)) } returns mapboxReplayer
        every { mapboxReplayer.seekTo(capture(seekToSlot)) } just Runs

        replayProgressObserver.onRouteProgressChanged(
            mockValidRouteProgress(firstRouteLeg, 0.0f)
        )
        replayProgressObserver.onRouteProgressChanged(
            mockValidRouteProgress(secondRouteLeg, 90.0f)
        )

        val alternativeRouteEvents = eventsSlot[1]
        val alternativeRouteSeekTo = seekToSlot[1]
        assertEquals(6, alternativeRouteEvents.size)
        assertEquals(0.0, alternativeRouteEvents[0].eventTimestamp, 0.0)
        assertEquals(3.0, alternativeRouteSeekTo.eventTimestamp, 0.0)
    }

    private fun mockValidRouteProgress(
        mockRouteLeg: RouteLeg,
        mockDistanceTraveled: Float
    ): RouteProgress = mockk {
        every { currentLegProgress } returns mockk {
            every { routeLeg } returns mockRouteLeg
            every { distanceTraveled } returns mockDistanceTraveled
        }
    }

    private fun mockEventsForShortRoute(): List<ReplayEventBase> {
        return mockEvents("""wt}ohAj||tfFoD`Sm_@iMcKgD""")
    }

    private fun mockEvents(encodedPolyline: String): List<ReplayEventBase> {
        return PolylineUtils.decode(encodedPolyline, 6).mapIndexed { index, value ->
            ReplayEventUpdateLocation(
                index.toDouble(),
                mockk(relaxed = true) {
                    every { lon } returns value.longitude()
                    every { lat } returns value.latitude()
                }
            )
        }
    }
}
