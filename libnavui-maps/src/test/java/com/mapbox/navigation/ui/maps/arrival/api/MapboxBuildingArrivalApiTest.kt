package com.mapbox.navigation.ui.maps.arrival.api

import com.mapbox.common.Logger
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.arrival.ArrivalObserver
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class MapboxBuildingArrivalApiTest {

    private val mapboxNavigation = mockk<MapboxNavigation> {
        every { unregisterArrivalObserver(any()) } returns Unit
        every { registerArrivalObserver(any()) } returns Unit
    }

    private val buildingHighlightApi = mockk<MapboxBuildingHighlightApi> {
        every { highlightBuilding(any()) } returns Unit
    }
    private val buildingArrivalApi = MapboxBuildingArrivalApi()

    @Before
    fun setup() {
        mockkStatic(Logger::class)
        every { Logger.w(any(), any()) } returns Unit
    }

    @After
    fun teardown() {
        unmockkStatic(Logger::class)
    }

    @Test
    fun `enable should register arrivalObserver`() {
        buildingArrivalApi.enable(mapboxNavigation)

        verify { mapboxNavigation.registerArrivalObserver(any()) }
    }

    @Test
    fun `disable should unregister arrivalObserver`() {
        buildingArrivalApi.enable(mapboxNavigation)
        buildingArrivalApi.disable()

        verify { mapboxNavigation.unregisterArrivalObserver(any()) }
    }

    @Test
    fun `no-op when BuildingHighlightApi has not been set`() {
        val arrivalSlot = CapturingSlot<ArrivalObserver>()
        every { mapboxNavigation.registerArrivalObserver(capture(arrivalSlot)) } returns Unit

        buildingArrivalApi.enable(mapboxNavigation)

        arrivalSlot.captured.onFinalDestinationArrival(mockFinalDestinationRouteProgress())
    }

    @Test
    fun `highlight building on final destination arrival`() {
        val arrivalSlot = CapturingSlot<ArrivalObserver>()
        every { mapboxNavigation.registerArrivalObserver(capture(arrivalSlot)) } returns Unit

        buildingArrivalApi.buildingHighlightApi(buildingHighlightApi)
        buildingArrivalApi.enable(mapboxNavigation)
        arrivalSlot.captured.onFinalDestinationArrival(mockFinalDestinationRouteProgress())

        verify { buildingHighlightApi.highlightBuilding(any()) }
    }

    @Test
    fun `highlight building on waypoint arrival`() {
        val arrivalSlot = CapturingSlot<ArrivalObserver>()
        every { mapboxNavigation.registerArrivalObserver(capture(arrivalSlot)) } returns Unit

        buildingArrivalApi.buildingHighlightApi(buildingHighlightApi)
        buildingArrivalApi.enable(mapboxNavigation)
        arrivalSlot.captured.onWaypointArrival(mockWaypointRouteProgress())

        verify { buildingHighlightApi.highlightBuilding(any()) }
    }

    @Test
    fun `clear building highlight on next route leg start`() {
        val arrivalSlot = CapturingSlot<ArrivalObserver>()
        every { mapboxNavigation.registerArrivalObserver(capture(arrivalSlot)) } returns Unit

        buildingArrivalApi.buildingHighlightApi(buildingHighlightApi)
        buildingArrivalApi.enable(mapboxNavigation)
        arrivalSlot.captured.onWaypointArrival(mockWaypointRouteProgress())
        arrivalSlot.captured.onNextRouteLegStart(mockk())

        verify { buildingHighlightApi.highlightBuilding(null) }
    }

    private fun mockFinalDestinationRouteProgress() = mockk<RouteProgress> {
        every { route } returns mockk {
            every { routeOptions() } returns mockk {
                every { coordinates() } returns listOf(
                    Point.fromLngLat(-122.431969, 37.777663)
                )
            }
        }
    }

    private fun mockWaypointRouteProgress() = mockk<RouteProgress> {
        every { route } returns mockk {
            every { routeOptions() } returns mockk {
                every { coordinates() } returns listOf(
                    Point.fromLngLat(-122.431969, 37.777663),
                    Point.fromLngLat(-122.431423, 37.776434)
                )
            }
            every { currentLegProgress } returns mockk {
                every { legIndex } returns 0
            }
        }
    }
}
