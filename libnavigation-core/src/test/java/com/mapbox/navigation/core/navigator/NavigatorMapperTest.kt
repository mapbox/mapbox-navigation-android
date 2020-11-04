package com.mapbox.navigation.core.navigator

import android.location.Location
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.trip.session.MapMatcherResult
import com.mapbox.navigation.navigator.internal.TripStatus
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class NavigatorMapperTest {

    private val enhancedLocation: Location = mockk(relaxed = true)
    private val keyPoints: List<Location> = mockk(relaxed = true)
    private val routeProgress: RouteProgress = mockk(relaxed = true)
    private val offRoute: Boolean = mockk(relaxed = true)

    @Test
    fun `map matcher result sanity`() {
        val tripStatus = TripStatus(
            enhancedLocation,
            keyPoints,
            routeProgress,
            offRoute,
            mockk {
                every { offRoadProba } returns 0f
                every { map_matcher_output } returns mockk {
                    every { isTeleport } returns false
                    every { matches } returns listOf(
                        mockk {
                            every { proba } returns 1f
                        }
                    )
                }
            }
        )
        val expected = MapMatcherResult(
            enhancedLocation,
            keyPoints,
            isOffRoad = false,
            offRoadProbability = 0f,
            isTeleport = false,
            roadEdgeMatchProbability = 1f
        )

        val result = tripStatus.getMapMatcherResult()

        assertEquals(expected, result)
    }

    @Test
    fun `map matcher result when close to being off road`() {
        val tripStatus = TripStatus(
            enhancedLocation,
            keyPoints,
            routeProgress,
            offRoute,
            mockk {
                every { offRoadProba } returns 0.5f
                every { map_matcher_output } returns mockk {
                    every { isTeleport } returns false
                    every { matches } returns listOf(
                        mockk {
                            every { proba } returns 1f
                        }
                    )
                }
            }
        )
        val expected = MapMatcherResult(
            enhancedLocation,
            keyPoints,
            isOffRoad = false,
            offRoadProbability = 0.5f,
            isTeleport = false,
            roadEdgeMatchProbability = 1f
        )

        val result = tripStatus.getMapMatcherResult()

        assertEquals(expected, result)
    }

    @Test
    fun `map matcher result when off road`() {
        val tripStatus = TripStatus(
            enhancedLocation,
            keyPoints,
            routeProgress,
            offRoute,
            mockk {
                every { offRoadProba } returns 0.500009f
                every { map_matcher_output } returns mockk {
                    every { isTeleport } returns false
                    every { matches } returns listOf(
                        mockk {
                            every { proba } returns 1f
                        }
                    )
                }
            }
        )
        val expected = MapMatcherResult(
            enhancedLocation,
            keyPoints,
            isOffRoad = true,
            offRoadProbability = 0.500009f,
            isTeleport = false,
            roadEdgeMatchProbability = 1f
        )

        val result = tripStatus.getMapMatcherResult()

        assertEquals(expected, result)
    }

    @Test
    fun `map matcher result teleport`() {
        val tripStatus = TripStatus(
            enhancedLocation,
            keyPoints,
            routeProgress,
            offRoute,
            mockk {
                every { offRoadProba } returns 0f
                every { map_matcher_output } returns mockk {
                    every { isTeleport } returns true
                    every { matches } returns listOf(
                        mockk {
                            every { proba } returns 1f
                        }
                    )
                }
            }
        )
        val expected = MapMatcherResult(
            enhancedLocation,
            keyPoints,
            isOffRoad = false,
            offRoadProbability = 0f,
            isTeleport = true,
            roadEdgeMatchProbability = 1f
        )

        val result = tripStatus.getMapMatcherResult()

        assertEquals(expected, result)
    }

    @Test
    fun `map matcher result no edge matches`() {
        val tripStatus = TripStatus(
            enhancedLocation,
            keyPoints,
            routeProgress,
            offRoute,
            mockk {
                every { offRoadProba } returns 1f
                every { map_matcher_output } returns mockk {
                    every { isTeleport } returns false
                    every { matches } returns listOf()
                }
            }
        )
        val expected = MapMatcherResult(
            enhancedLocation,
            keyPoints,
            isOffRoad = true,
            offRoadProbability = 1f,
            isTeleport = false,
            roadEdgeMatchProbability = 0f
        )

        val result = tripStatus.getMapMatcherResult()

        assertEquals(expected, result)
    }
}
