package com.mapbox.navigation.ui.maneuver.api

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.BannerText
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteStepProgress
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.base.model.Expected
import com.mapbox.navigation.ui.maneuver.ManeuverAction
import com.mapbox.navigation.ui.maneuver.ManeuverProcessor
import com.mapbox.navigation.ui.maneuver.ManeuverResult
import com.mapbox.navigation.ui.maneuver.model.Maneuver
import com.mapbox.navigation.ui.maneuver.model.PrimaryManeuver
import com.mapbox.navigation.ui.maneuver.model.StepDistance
import com.mapbox.navigation.ui.maneuver.model.TotalManeuverDistance
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class MapboxManeuverApiTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()
    private val mapboxManeuverApi = MapboxManeuverApi(mockk())

    @Test
    fun `process step distance remaining`() = coroutineRule.runBlockingTest {
        val maneuverProcessor = mockk<ManeuverProcessor>()
        val expectedDistanceRemaining = 234.0
        val callback: StepDistanceRemainingCallback = mockk(relaxed = true)
        val mockRouteStepProgress = mockk<RouteStepProgress>().apply {
            every { distanceRemaining } returns 234f
        }
        val mockResult = mockk<ManeuverResult.GetStepDistanceRemaining>()
        every { mockResult.distanceRemaining } returns 234.0
        coEvery {
            maneuverProcessor.process(
                ManeuverAction.GetStepDistanceRemaining(mockRouteStepProgress)
            )
        } returns mockResult
        val messageSlot = slot<Expected.Success<StepDistance>>()

        mapboxManeuverApi.getStepDistanceRemaining(mockRouteStepProgress, callback)

        verify(exactly = 1) { callback.onStepDistanceRemaining(capture(messageSlot)) }
        assertEquals(expectedDistanceRemaining, messageSlot.captured.value.distance, 0.0)
    }

    @Test
    fun `process current maneuver`() = coroutineRule.runBlockingTest {
        val maneuverProcessor = mockk<ManeuverProcessor>()
        val callback: ManeuverCallback = mockk(relaxed = true)
        val primaryManeuver = PrimaryManeuver
            .Builder()
            .text("primary")
            .type(null)
            .degrees(null)
            .modifier(null)
            .drivingSide(null)
            .componentList(listOf())
            .build()
        val expected = Maneuver(
            primaryManeuver,
            TotalManeuverDistance(12.0),
            null,
            null,
            null
        )
        val mockBannerInstruction = mockk<BannerInstructions>()
        every {
            mockBannerInstruction.primary()
        } returns mockBannerText(
            { "primary" },
            { listOf(mockBannerComponent({ "primary" }, { BannerComponents.TEXT })) }
        )
        every { mockBannerInstruction.secondary() } returns null
        every { mockBannerInstruction.sub() } returns null
        every { mockBannerInstruction.distanceAlongGeometry() } returns 12.0
        val mockResult = mockk<ManeuverResult.GetManeuver>()
        coEvery {
            maneuverProcessor.process(
                ManeuverAction.GetManeuver(mockBannerInstruction)
            )
        } returns mockResult
        val slot = slot<Expected.Success<Maneuver>>()

        mapboxManeuverApi.getManeuver(mockBannerInstruction, callback)

        verify(exactly = 1) { callback.onManeuver(capture(slot)) }
        assertEquals(expected.primary.text, slot.captured.value.primary.text)
        assertNull(slot.captured.value.secondary)
        assertNull(slot.captured.value.sub)
        assertEquals(expected.totalManeuverDistance, slot.captured.value.totalManeuverDistance)
    }

    @Test
    fun `process upcoming maneuver list`() = coroutineRule.runBlockingTest {
        val maneuverProcessor = mockk<ManeuverProcessor>()
        val mockAllBannerInstructions = mockk<ManeuverResult.GetAllBannerInstructions>()
        val callback: UpcomingManeuverListCallback = mockk(relaxed = true)
        val mockBannerInstruction1 = mockBannerInstruction({ "primary1" }, { 1.0 })
        val mockBannerInstruction2 = mockBannerInstruction({ "primary2" }, { 2.0 })

        val mockLegStep = mockk<LegStep>()
        every { mockLegStep.bannerInstructions() } returns listOf(
            mockBannerInstruction1,
            mockBannerInstruction2
        )
        val mockStepProgress = mockk<RouteStepProgress>()
        every { mockStepProgress.step } returns mockLegStep
        every { mockStepProgress.distanceRemaining } returns 12f
        val mockRouteLeg = mockk<RouteLeg>()
        every { mockRouteLeg.steps() } returns listOf(mockLegStep)
        val mockLegProgress = mockk<RouteLegProgress>()
        every { mockLegProgress.routeLeg } returns mockRouteLeg
        every { mockLegProgress.currentStepProgress } returns mockStepProgress
        val mockRouteProgress = mockk<RouteProgress>()
        every { mockRouteProgress.currentLegProgress } returns mockLegProgress
        coEvery {
            maneuverProcessor.process(
                ManeuverAction.GetAllBannerInstructions(mockRouteProgress)
            )
        } returns mockAllBannerInstructions
        val slot = slot<Expected.Success<List<Maneuver>>>()

        mapboxManeuverApi.getUpcomingManeuverList(mockRouteProgress, callback)

        verify(exactly = 1) { callback.onUpcomingManeuvers(capture(slot)) }
        assertEquals(mockBannerInstruction2.primary().text(), slot.captured.value[0].primary.text)
        assertNull(slot.captured.value[0].secondary)
        assertNull(slot.captured.value[0].sub)
        assertEquals(
            mockBannerInstruction2.distanceAlongGeometry(),
            slot.captured.value[0].totalManeuverDistance.totalDistance,
            0.0
        )
    }

    private fun mockBannerInstruction(
        textPrimary: () -> String,
        distanceAlongGeometry: () -> Double
    ): BannerInstructions {
        val bannerInstructions = mockk<BannerInstructions>()
        every { bannerInstructions.primary() } returns mockBannerText(
            { textPrimary() },
            { listOf(mockBannerComponent({ textPrimary() }, { BannerComponents.TEXT })) }
        )
        every { bannerInstructions.secondary() } returns null
        every { bannerInstructions.sub() } returns null
        every { bannerInstructions.distanceAlongGeometry() } returns distanceAlongGeometry()
        return bannerInstructions
    }

    private fun mockBannerText(
        text: () -> String,
        componentList: () -> List<BannerComponents>,
        type: () -> String = { MANEUVER_TYPE },
        modifier: () -> String = { MANEUVER_MODIFIER },
        degrees: () -> Double? = { null },
        drivingSide: () -> String? = { null },
    ): BannerText {
        val bannerText = mockk<BannerText>()
        every { bannerText.text() } returns text()
        every { bannerText.type() } returns type()
        every { bannerText.degrees() } returns degrees()
        every { bannerText.modifier() } returns modifier()
        every { bannerText.drivingSide() } returns drivingSide()
        every { bannerText.components() } returns componentList()
        return bannerText
    }

    private fun mockBannerComponent(
        text: () -> String,
        type: () -> String,
        active: () -> Boolean? = { null },
        subType: () -> String? = { null },
        imageUrl: () -> String? = { null },
        directions: () -> List<String>? = { null },
        imageBaseUrl: () -> String? = { null },
        abbreviation: () -> String? = { null },
        abbreviationPriority: () -> Int? = { null },
    ): BannerComponents {
        val bannerComponents = mockk<BannerComponents>()
        every { bannerComponents.text() } returns text()
        every { bannerComponents.type() } returns type()
        every { bannerComponents.active() } returns active()
        every { bannerComponents.subType() } returns subType()
        every { bannerComponents.imageUrl() } returns imageUrl()
        every { bannerComponents.directions() } returns directions()
        every { bannerComponents.imageBaseUrl() } returns imageBaseUrl()
        every { bannerComponents.abbreviation() } returns abbreviation()
        every { bannerComponents.abbreviationPriority() } returns abbreviationPriority()
        return bannerComponents
    }

    private companion object {
        private const val MANEUVER_TYPE = "MANEUVER TYPE"
        private const val MANEUVER_MODIFIER = "MANEUVER MODIFIER"
    }
}
