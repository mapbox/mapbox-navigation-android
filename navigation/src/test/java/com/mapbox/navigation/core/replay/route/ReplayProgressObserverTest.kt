@file:OptIn(ExperimentalMapboxNavigationAPI::class)

package com.mapbox.navigation.core.replay.route

import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.route.update
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.history.ReplayEventBase
import com.mapbox.navigation.core.replay.history.ReplayEventUpdateLocation
import com.mapbox.navigation.testing.factories.createDirectionsRoute
import com.mapbox.navigation.testing.factories.createNavigationRoute
import com.mapbox.navigation.testing.factories.createNavigationRoutes
import com.mapbox.navigation.testing.factories.createRouteLeg
import com.mapbox.navigation.testing.factories.createRouteLegAnnotation
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
            mockValidRouteProgress(mockDistanceTraveled = 0.0f),
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

        val testRoute = createNavigationRoutes()[0]
        replayProgressObserver.onRouteProgressChanged(
            mockValidRouteProgress(testRoute, 0, mockDistanceTraveled = 10.0f),
        )
        replayProgressObserver.onRouteProgressChanged(
            mockValidRouteProgress(testRoute, 0, mockDistanceTraveled = 50.0f),
        )
        val refreshedRoute = testRoute.update(
            directionsRouteBlock = {
                this.toBuilder()
                    .legs(
                        this.legs()?.map {
                            it.toBuilder()
                                .annotation(
                                    createRouteLegAnnotation(
                                        congestionNumeric = listOf(25, 84),
                                    ),
                                )
                                .build()
                        },
                    )
                    .build()
            },
            waypointsBlock = {
                this
            },
        )
        replayProgressObserver.onRouteProgressChanged(
            mockValidRouteProgress(refreshedRoute, 0, mockDistanceTraveled = 55.0f),
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
        val firstRouteLeg: RouteLeg = createRouteLeg()
        val secondRouteLeg: RouteLeg = createRouteLeg()
        val testRoute = createNavigationRoute(
            directionsRoute = createDirectionsRoute(
                legs = listOf(firstRouteLeg, secondRouteLeg),
            ),
        )
        replayProgressObserver.onRouteProgressChanged(
            mockValidRouteProgress(
                route = testRoute,
                currentLegIndex = 0,
                mockDistanceTraveled = 10.0f,
            ),
        )
        replayProgressObserver.onRouteProgressChanged(
            mockValidRouteProgress(
                route = testRoute,
                currentLegIndex = 1,
                mockDistanceTraveled = 50.0f,
            ),
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
            """yg{bgA|cufhFoEiAiA[}i@oNoD_As@QqdAeX""",
        )
        val eventsSlot = slot<List<ReplayEventBase>>()
        val seekToSlot = slot<ReplayEventBase>()
        every { mapboxReplayer.pushEvents(capture(eventsSlot)) } returns mapboxReplayer
        every { mapboxReplayer.seekTo(capture(seekToSlot)) } just Runs

        replayProgressObserver.onRouteProgressChanged(
            mockValidRouteProgress(mockDistanceTraveled = 0.0f),
        )

        // Seek to first location because 0.0 distance traveled
        assertEquals(7, eventsSlot.captured.size)
        assertEquals(0.0, eventsSlot.captured[0].eventTimestamp, 0.0)
        assertEquals(0.0, seekToSlot.captured.eventTimestamp, 0.0)
    }

    @Test
    fun `should seekTo mid route when distanceTraveled is mid route`() {
        every { replayRouteMapper.mapRouteLegGeometry(any()) } returns mockEvents(
            """yg{bgA|cufhFoEiAiA[}i@oNoD_As@QqdAeX""",
        )
        val eventsSlot = slot<List<ReplayEventBase>>()
        val seekToSlot = slot<ReplayEventBase>()
        every { mapboxReplayer.pushEvents(capture(eventsSlot)) } returns mapboxReplayer
        every { mapboxReplayer.seekTo(capture(seekToSlot)) } just Runs

        replayProgressObserver.onRouteProgressChanged(
            mockValidRouteProgress(mockDistanceTraveled = 90.0f),
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
            mockValidRouteProgress(mockDistanceTraveled = 90.0f),
        )

        assertFalse(eventsSlot.isCaptured)
        assertFalse(seekToSlot.isCaptured)
    }

    @Test
    fun `should seekTo alternative route with distanceTraveled`() {
        val firstRouteLeg: RouteLeg = createRouteLeg()
        val secondRouteLeg: RouteLeg = createRouteLeg()
        val testRoute = createNavigationRoute(
            directionsRoute = createDirectionsRoute(
                legs = listOf(firstRouteLeg, secondRouteLeg),
            ),
        )
        every { replayRouteMapper.mapRouteLegGeometry(firstRouteLeg) } returns mockEvents(
            """yg{bgA|cufhFoEiAiA[}i@oNoD_As@QqdAeX""",
        )
        every { replayRouteMapper.mapRouteLegGeometry(secondRouteLeg) } returns mockEvents(
            """yg{bgA|cufhFoEiAiA[}i@oNoD_As@Q""",
        )
        val eventsSlot = mutableListOf<List<ReplayEventBase>>()
        val seekToSlot = mutableListOf<ReplayEventBase>()
        every { mapboxReplayer.pushEvents(capture(eventsSlot)) } returns mapboxReplayer
        every { mapboxReplayer.seekTo(capture(seekToSlot)) } just Runs

        replayProgressObserver.onRouteProgressChanged(
            mockValidRouteProgress(
                route = testRoute,
                currentLegIndex = 0,
                mockDistanceTraveled = 0.0f,
            ),
        )
        replayProgressObserver.onRouteProgressChanged(
            mockValidRouteProgress(
                route = testRoute,
                currentLegIndex = 1,
                mockDistanceTraveled = 90.0f,
            ),
        )

        val alternativeRouteEvents = eventsSlot[1]
        val alternativeRouteSeekTo = seekToSlot[1]
        assertEquals(6, alternativeRouteEvents.size)
        assertEquals(0.0, alternativeRouteEvents[0].eventTimestamp, 0.0)
        assertEquals(3.0, alternativeRouteSeekTo.eventTimestamp, 0.0)
    }

    private fun mockValidRouteProgress(
        route: NavigationRoute = createNavigationRoute(),
        currentLegIndex: Int = 0,
        mockDistanceTraveled: Float = 0.0f,
    ): RouteProgress = mockk {
        every { navigationRoute } returns route
        every { currentLegProgress } returns mockk {
            every { routeLeg } returns route.directionsRoute.legs()!![currentLegIndex]
            every { distanceTraveled } returns mockDistanceTraveled
            every { legIndex } returns currentLegIndex
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
                },
            )
        }
    }
}
