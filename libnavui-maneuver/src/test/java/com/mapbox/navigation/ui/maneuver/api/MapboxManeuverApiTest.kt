package com.mapbox.navigation.ui.maneuver.api

import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.navigation.base.trip.model.RouteStepProgress
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.base.api.maneuver.ManeuverCallback
import com.mapbox.navigation.ui.base.api.maneuver.StepDistanceRemainingCallback
import com.mapbox.navigation.ui.base.api.maneuver.UpcomingManeuversCallback
import com.mapbox.navigation.ui.base.model.maneuver.Maneuver
import com.mapbox.navigation.ui.base.model.maneuver.ManeuverState
import com.mapbox.navigation.ui.base.model.maneuver.PrimaryManeuver
import com.mapbox.navigation.ui.base.model.maneuver.TotalManeuverDistance
import com.mapbox.navigation.ui.maneuver.ManeuverAction
import com.mapbox.navigation.ui.maneuver.ManeuverProcessor
import com.mapbox.navigation.ui.maneuver.ManeuverResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class MapboxManeuverApiTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()
    private val mapboxManeuverApi = MapboxManeuverApi(mockk())

    @Before
    fun setUp() {
        mockkObject(ManeuverProcessor)
    }

    @After
    fun tearDown() {
        unmockkObject(ManeuverProcessor)
    }

    @Test
    fun `process step distance remaining`() = coroutineRule.runBlockingTest {

        val callback: StepDistanceRemainingCallback = mockk(relaxed = true)
        val mockRouteStepProgress = mockk<RouteStepProgress>()
        val mockResult = mockk<ManeuverResult.StepDistanceRemaining>()
        every { mockResult.distanceRemaining } returns 234.0
        coEvery {
            ManeuverProcessor.process(
                ManeuverAction.FindStepDistanceRemaining(mockRouteStepProgress)
            )
        } returns mockResult
        val expectedDistanceRemaining = 234.0
        val messageSlot = slot<ManeuverState.DistanceRemainingToFinishStep>()

        mapboxManeuverApi.retrieveStepDistanceRemaining(mockRouteStepProgress, callback)

        verify(exactly = 1) { callback.onStepDistanceRemaining(capture(messageSlot)) }
        assertEquals(expectedDistanceRemaining, messageSlot.captured.distanceRemaining, 0.0)
    }

    @Test
    fun `process current maneuver`() = coroutineRule.runBlockingTest {

        val callback: ManeuverCallback = mockk(relaxed = true)
        val expected = Maneuver
            .Builder()
            .primary(
                PrimaryManeuver
                    .Builder()
                    .text("primary")
                    .type(null)
                    .degrees(null)
                    .modifier(null)
                    .drivingSide(null)
                    .componentList(listOf())
                    .build()
            )
            .totalManeuverDistance(TotalManeuverDistance(12.0))
            .secondary(null)
            .sub(null)
            .laneGuidance(null)
            .build()
        val mockBannerInstruction = mockk<BannerInstructions>()
        val mockResult = mockk<ManeuverResult.CurrentManeuver>()
        every { mockResult.currentManeuver } returns expected
        coEvery {
            ManeuverProcessor.process(
                ManeuverAction.ParseCurrentManeuver(mockBannerInstruction)
            )
        } returns mockResult
        val messageSlot = slot<ManeuverState.CurrentManeuver>()

        mapboxManeuverApi.retrieveManeuver(mockBannerInstruction, callback)

        verify(exactly = 1) { callback.onManeuver(capture(messageSlot)) }
        assertEquals(expected, messageSlot.captured.maneuver)
    }

    @Test
    fun `process upcoming maneuvers`() = coroutineRule.runBlockingTest {

        val callback: UpcomingManeuversCallback = mockk(relaxed = true)
        val expected = listOf(
            Maneuver
                .Builder()
                .primary(
                    PrimaryManeuver
                        .Builder()
                        .text("primary")
                        .type(null)
                        .degrees(null)
                        .modifier(null)
                        .drivingSide(null)
                        .componentList(listOf())
                        .build()
                )
                .totalManeuverDistance(TotalManeuverDistance(12.0))
                .secondary(null)
                .sub(null)
                .laneGuidance(null)
                .build()
        )
        val mockRouteLeg = mockk<RouteLeg>()
        val mockSteps = mockk<LegStep>()
        val mockBannerInstruction = mockk<BannerInstructions>()
        every { mockSteps.bannerInstructions() } returns listOf(mockBannerInstruction)
        every { mockRouteLeg.steps() } returns listOf(mockSteps)
        val mockResult = mockk<ManeuverResult.UpcomingManeuvers>()
        every { mockResult.upcomingManeuverList } returns expected
        coEvery {
            ManeuverProcessor.process(
                ManeuverAction.FindAllUpcomingManeuvers(mockRouteLeg)
            )
        } returns mockResult
        val messageSlot = slot<ManeuverState.UpcomingManeuvers.Upcoming>()

        mapboxManeuverApi.retrieveUpcomingManeuvers(mockRouteLeg, callback)

        verify(exactly = 1) { callback.onUpcomingManeuvers(capture(messageSlot)) }
        assertEquals(expected, messageSlot.captured.upcomingManeuverList)
    }
}
