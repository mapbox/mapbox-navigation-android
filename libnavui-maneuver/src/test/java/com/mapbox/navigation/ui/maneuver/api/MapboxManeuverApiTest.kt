package com.mapbox.navigation.ui.maneuver.api

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.bindgen.Expected
import com.mapbox.navigation.base.formatter.DistanceFormatter
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.maneuver.ManeuverAction
import com.mapbox.navigation.ui.maneuver.ManeuverProcessor
import com.mapbox.navigation.ui.maneuver.ManeuverResult
import com.mapbox.navigation.ui.maneuver.ManeuverState
import com.mapbox.navigation.ui.maneuver.RoadShieldContentManager
import com.mapbox.navigation.ui.maneuver.model.Maneuver
import com.mapbox.navigation.ui.maneuver.model.ManeuverError
import com.mapbox.navigation.ui.maneuver.model.RoadShield
import com.mapbox.navigation.ui.maneuver.model.RoadShieldError
import com.mapbox.navigation.ui.maneuver.model.RoadShieldResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class MapboxManeuverApiTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()
    private val distanceFormatter = mockk<DistanceFormatter>()
    private val mapboxManeuverApi = MapboxManeuverApi(distanceFormatter)

    @Before
    fun setup() {
        mockkObject(ManeuverProcessor)
    }

    @After
    fun tearDown() {
        unmockkObject(ManeuverProcessor)
    }

    @Test
    fun `when get maneuver invoked and processor returns failure`() {
        val route = mockk<DirectionsRoute>()
        val maneuverState = ManeuverState()
        val action = ManeuverAction.GetManeuverListWithRoute(
            route,
            null,
            maneuverState,
            distanceFormatter
        )
        every { ManeuverProcessor.process(action) } returns
            ManeuverResult.GetManeuverList.Failure("whatever")
        val callback: ManeuverCallback = mockk(relaxed = true)
        val slot = slot<Expected<ManeuverError, List<Maneuver>>>()
        mapboxManeuverApi.getManeuvers(route, callback)

        verify(exactly = 1) { callback.onManeuvers(capture(slot)) }
    }

    @Test
    fun `when get maneuver invoked and processor returns inappropriate result`() {
        mockkObject(ManeuverProcessor)
        val route = mockk<DirectionsRoute>()
        val maneuverState = ManeuverState()
        val action = ManeuverAction.GetManeuverListWithRoute(
            route,
            null,
            maneuverState,
            distanceFormatter
        )
        every { ManeuverProcessor.process(action) } returns
            ManeuverResult.GetManeuverListWithProgress.Failure("whatever")
        val callback: ManeuverCallback = mockk(relaxed = true)
        val slot = slot<Expected<ManeuverError, List<Maneuver>>>()
        mapboxManeuverApi.getManeuvers(route, callback)

        verify(exactly = 1) { callback.onManeuvers(capture(slot)) }
    }

    @Test
    fun `when get maneuver invoked and processor returns success`() {
        mockkObject(ManeuverProcessor)
        val route = mockk<DirectionsRoute>()
        val maneuverState = ManeuverState()
        val action = ManeuverAction.GetManeuverListWithRoute(
            route,
            null,
            maneuverState,
            distanceFormatter
        )
        every { ManeuverProcessor.process(action) } returns
            ManeuverResult.GetManeuverList.Success(listOf())
        val callback: ManeuverCallback = mockk(relaxed = true)
        val slot = slot<Expected<ManeuverError, List<Maneuver>>>()
        mapboxManeuverApi.getManeuvers(route, callback)

        verify(exactly = 1) { callback.onManeuvers(capture(slot)) }
    }

    @Test
    fun `when get maneuver with progress invoked and processor returns failure`() {
        mockkObject(ManeuverProcessor)
        val routeProgress = mockk<RouteProgress>()
        val maneuverState = ManeuverState()
        val action = ManeuverAction.GetManeuverList(
            routeProgress,
            maneuverState,
            distanceFormatter
        )
        every { ManeuverProcessor.process(action) } returns
            ManeuverResult.GetManeuverListWithProgress.Failure("whatever")
        val callback: ManeuverCallback = mockk(relaxed = true)
        val slot = slot<Expected<ManeuverError, List<Maneuver>>>()
        mapboxManeuverApi.getManeuvers(routeProgress, callback)

        verify(exactly = 1) { callback.onManeuvers(capture(slot)) }
    }

    @Test
    fun `when get maneuver with progress invoked and processor returns inappropriate result`() {
        mockkObject(ManeuverProcessor)
        val routeProgress = mockk<RouteProgress>()
        val maneuverState = ManeuverState()
        val action = ManeuverAction.GetManeuverList(
            routeProgress,
            maneuverState,
            distanceFormatter
        )
        every { ManeuverProcessor.process(action) } returns
            ManeuverResult.GetManeuverList.Failure("whatever")
        val callback: ManeuverCallback = mockk(relaxed = true)
        val slot = slot<Expected<ManeuverError, List<Maneuver>>>()
        mapboxManeuverApi.getManeuvers(routeProgress, callback)

        verify(exactly = 1) { callback.onManeuvers(capture(slot)) }
    }

    @Test
    fun `when get maneuver with progress invoked and processor returns success`() {
        mockkObject(ManeuverProcessor)
        val routeProgress = mockk<RouteProgress>()
        val maneuverState = ManeuverState()
        val action = ManeuverAction.GetManeuverList(
            routeProgress,
            maneuverState,
            distanceFormatter
        )
        every { ManeuverProcessor.process(action) } returns
            ManeuverResult.GetManeuverListWithProgress.Success(listOf())
        val callback: ManeuverCallback = mockk(relaxed = true)
        val slot = slot<Expected<ManeuverError, List<Maneuver>>>()
        mapboxManeuverApi.getManeuvers(routeProgress, callback)

        verify(exactly = 1) { callback.onManeuvers(capture(slot)) }
    }

    @Test
    fun `when roadShields is invoked returns success`() = coroutineRule.runBlockingTest {
        val roadShieldContentManager = mockk<RoadShieldContentManager>()
        val maneuver: Maneuver = mockk(relaxed = true)
        val maneuverList = listOf(maneuver)
        val roadShieldError = RoadShieldError("https://mapbox.com", "whatever")
        coEvery {
            roadShieldContentManager.getShields(maneuverList)
        } returns RoadShieldResult(mapOf(), mapOf("1234abcd" to roadShieldError))
        val callback: RoadShieldCallback = mockk(relaxed = true)
        val maneuverSlot = slot<List<Maneuver>>()
        val shieldSlot = slot<Map<String, RoadShield?>>()
        val errorSlot = slot<Map<String, RoadShieldError>>()

        mapboxManeuverApi.getRoadShields(maneuverList, callback)

        verify(exactly = 1) {
            callback.onRoadShields(capture(maneuverSlot), capture(shieldSlot), capture(errorSlot))
        }
    }
}
