package com.mapbox.navigation.ui.maneuver

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerComponents.DELIMITER
import com.mapbox.api.directions.v5.models.BannerComponents.EXIT
import com.mapbox.api.directions.v5.models.BannerComponents.EXIT_NUMBER
import com.mapbox.api.directions.v5.models.BannerComponents.ICON
import com.mapbox.api.directions.v5.models.BannerComponents.TEXT
import com.mapbox.api.directions.v5.models.BannerComponents.builder
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.BannerText
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.navigation.base.trip.model.RouteStepProgress
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.base.model.maneuver.Component
import com.mapbox.navigation.ui.base.model.maneuver.DelimiterComponentNode
import com.mapbox.navigation.ui.base.model.maneuver.ExitComponentNode
import com.mapbox.navigation.ui.base.model.maneuver.ExitNumberComponentNode
import com.mapbox.navigation.ui.base.model.maneuver.Lane
import com.mapbox.navigation.ui.base.model.maneuver.LaneIndicator
import com.mapbox.navigation.ui.base.model.maneuver.Maneuver
import com.mapbox.navigation.ui.base.model.maneuver.PrimaryManeuver
import com.mapbox.navigation.ui.base.model.maneuver.RoadShieldComponentNode
import com.mapbox.navigation.ui.base.model.maneuver.SecondaryManeuver
import com.mapbox.navigation.ui.base.model.maneuver.SubManeuver
import com.mapbox.navigation.ui.base.model.maneuver.TextComponentNode
import com.mapbox.navigation.ui.base.model.maneuver.TotalManeuverDistance
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class ManeuverProcessorTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Test
    fun `process action step distance remaining result distance`() =
        coroutineRule.runBlockingTest {
            val mockRouteStepProgress = mockk<RouteStepProgress>()
            val mockStepDistanceRemaining = 324f
            every { mockRouteStepProgress.distanceRemaining } returns mockStepDistanceRemaining
            val mockAction = ManeuverAction.FindStepDistanceRemaining(mockRouteStepProgress)
            val expected = ManeuverResult.StepDistanceRemaining(
                mockStepDistanceRemaining.toDouble()
            )

            val actual = ManeuverProcessor.process(mockAction)

            assertEquals(expected, actual)
        }

    @Test
    fun `process action find upcoming maneuvers result no steps empty upcoming maneuvers`() =
        coroutineRule.runBlockingTest {
            val mockRouteLeg = mockk<RouteLeg>()
            every { mockRouteLeg.steps() } returns null
            val mockAction = ManeuverAction.FindAllUpcomingManeuvers(mockRouteLeg)
            val expected = ManeuverResult.UpcomingManeuvers(listOf())

            val actual = ManeuverProcessor.process(mockAction)

            assertEquals(expected, actual)
        }

    @Test
    fun `process action find upcoming maneuvers result no instructions empty upcoming maneuvers`() =
        coroutineRule.runBlockingTest {
            val mockRouteLeg = mockk<RouteLeg>()
            val mockStep = mockk<LegStep>()
            every { mockStep.bannerInstructions() } returns null
            val mockStepList = listOf(mockStep)
            every { mockRouteLeg.steps() } returns mockStepList

            val mockAction = ManeuverAction.FindAllUpcomingManeuvers(mockRouteLeg)
            val expected = ManeuverResult.UpcomingManeuvers(listOf())

            val actual = ManeuverProcessor.process(mockAction)

            assertEquals(expected, actual)
        }

    @Test
    fun `process action find upcoming maneuvers result upcoming maneuvers`() =
        coroutineRule.runBlockingTest {
            val mockRouteLeg = mockk<RouteLeg>()
            val mockPrimaryBannerText = getPrimaryBannerText()
            val mockBannerInstructions = mockk<BannerInstructions>()
            every { mockBannerInstructions.primary() } returns mockPrimaryBannerText
            every { mockBannerInstructions.secondary() } returns null
            every { mockBannerInstructions.sub() } returns null
            every { mockBannerInstructions.distanceAlongGeometry() } returns 23.0
            val mockStep = mockk<LegStep>()
            every { mockStep.bannerInstructions() } returns listOf(mockBannerInstructions)
            val mockStepList = listOf(mockStep)
            every { mockRouteLeg.steps() } returns mockStepList

            val mockAction = ManeuverAction.FindAllUpcomingManeuvers(mockRouteLeg)
            val totalManeuverDistance =
                TotalManeuverDistance(mockBannerInstructions.distanceAlongGeometry())
            val primaryManeuver = createPrimaryManeuver(mockPrimaryBannerText)
            val expected = ManeuverResult.UpcomingManeuvers(
                listOf(
                    Maneuver
                        .Builder()
                        .primary(primaryManeuver)
                        .totalManeuverDistance(totalManeuverDistance)
                        .secondary(null)
                        .sub(null)
                        .laneGuidance(null)
                        .build()
                )
            )

            val actual = ManeuverProcessor.process(mockAction)

            assertEquals(expected, actual)
        }

    @Test
    fun `process action get current maneuver result without secondary, sub and lane`() =
        coroutineRule.runBlockingTest {
            val mockPrimaryBannerText = getPrimaryBannerText()
            val mockBannerInstructions = mockk<BannerInstructions>()
            val mockTotalStepDistance = 23.0
            every { mockBannerInstructions.primary() } returns mockPrimaryBannerText
            every { mockBannerInstructions.secondary() } returns null
            every { mockBannerInstructions.sub() } returns null
            every { mockBannerInstructions.distanceAlongGeometry() } returns mockTotalStepDistance

            val mockAction = ManeuverAction.ParseCurrentManeuver(mockBannerInstructions)
            val primaryManeuver = createPrimaryManeuver(mockPrimaryBannerText)
            val expected = ManeuverResult.CurrentManeuver(
                Maneuver
                    .Builder()
                    .primary(primaryManeuver)
                    .totalManeuverDistance(TotalManeuverDistance(mockTotalStepDistance))
                    .secondary(null)
                    .sub(null)
                    .laneGuidance(null)
                    .build()
            )

            val actual = ManeuverProcessor.process(mockAction)

            assertEquals(expected, actual)
        }

    @Test
    fun `process action get current maneuver result without sub and lane`() =
        coroutineRule.runBlockingTest {
            val mockPrimaryBannerText = getPrimaryBannerText()
            val mockSecondaryBannerText = getSecondaryBannerText()
            val mockBannerInstructions = mockk<BannerInstructions>()
            val mockTotalStepDistance = 23.0
            every { mockBannerInstructions.primary() } returns mockPrimaryBannerText
            every { mockBannerInstructions.secondary() } returns mockSecondaryBannerText
            every { mockBannerInstructions.sub() } returns null
            every { mockBannerInstructions.distanceAlongGeometry() } returns mockTotalStepDistance

            val mockAction = ManeuverAction.ParseCurrentManeuver(mockBannerInstructions)
            val primaryManeuver = createPrimaryManeuver(mockPrimaryBannerText)
            val secondaryManeuver = createSecondaryManeuver(mockSecondaryBannerText)
            val expected = ManeuverResult.CurrentManeuver(
                Maneuver
                    .Builder()
                    .primary(primaryManeuver)
                    .totalManeuverDistance(TotalManeuverDistance(mockTotalStepDistance))
                    .secondary(secondaryManeuver)
                    .sub(null)
                    .laneGuidance(null)
                    .build()
            )

            val actual = ManeuverProcessor.process(mockAction)

            assertEquals(expected, actual)
        }

    @Test
    fun `process action get current maneuver result with sub and without lane`() =
        coroutineRule.runBlockingTest {
            val mockPrimaryBannerText = getPrimaryBannerText()
            val mockSecondaryBannerText = getSecondaryBannerText()
            val mockSubBannerText = getSubBannerText()
            val mockBannerInstructions = mockk<BannerInstructions>()
            val mockTotalStepDistance = 23.0
            every { mockBannerInstructions.primary() } returns mockPrimaryBannerText
            every { mockBannerInstructions.secondary() } returns mockSecondaryBannerText
            every { mockBannerInstructions.sub() } returns mockSubBannerText
            every { mockBannerInstructions.distanceAlongGeometry() } returns mockTotalStepDistance

            val mockAction = ManeuverAction.ParseCurrentManeuver(mockBannerInstructions)
            val primaryManeuver = createPrimaryManeuver(mockPrimaryBannerText)
            val secondaryManeuver = createSecondaryManeuver(mockSecondaryBannerText)
            val subManeuver = createSubManeuver(mockSubBannerText)
            val expected = ManeuverResult.CurrentManeuver(
                Maneuver
                    .Builder()
                    .primary(primaryManeuver)
                    .totalManeuverDistance(TotalManeuverDistance(mockTotalStepDistance))
                    .secondary(secondaryManeuver)
                    .sub(subManeuver)
                    .laneGuidance(null)
                    .build()
            )

            val actual = ManeuverProcessor.process(mockAction)

            assertEquals(expected, actual)
        }

    @Test
    fun `process action get current maneuver result without sub and with lane`() =
        coroutineRule.runBlockingTest {
            val mockPrimaryBannerText = getPrimaryBannerText()
            val mockSecondaryBannerText = getSecondaryBannerText()
            val mockLaneBannerText = getLaneBannerText()
            val mockBannerInstructions = mockk<BannerInstructions>()
            val mockTotalStepDistance = 23.0
            every { mockBannerInstructions.primary() } returns mockPrimaryBannerText
            every { mockBannerInstructions.secondary() } returns mockSecondaryBannerText
            every { mockBannerInstructions.sub() } returns mockLaneBannerText
            every { mockBannerInstructions.distanceAlongGeometry() } returns mockTotalStepDistance

            val mockAction = ManeuverAction.ParseCurrentManeuver(mockBannerInstructions)
            val primaryManeuver = createPrimaryManeuver(mockPrimaryBannerText)
            val secondaryManeuver = createSecondaryManeuver(mockSecondaryBannerText)
            val laneManeuver = Lane
                .Builder()
                .allLanes(createLaneManeuver())
                .activeDirection(mockPrimaryBannerText.modifier())
                .build()
            val expected = ManeuverResult.CurrentManeuver(
                Maneuver
                    .Builder()
                    .primary(primaryManeuver)
                    .totalManeuverDistance(TotalManeuverDistance(mockTotalStepDistance))
                    .secondary(secondaryManeuver)
                    .sub(null)
                    .laneGuidance(laneManeuver)
                    .build()
            )

            val actual = ManeuverProcessor.process(mockAction)

            assertEquals(expected, actual)
        }

    private fun getPrimaryBannerText(): BannerText {
        val primaryBannerComponentList = getPrimaryBannerComponentList()
        val mockPrimaryBannerText = mockk<BannerText>()
        every { mockPrimaryBannerText.type() } returns "merge"
        every { mockPrimaryBannerText.text() } returns "Exit 23 I-880 / Stevenson Boulevard"
        every { mockPrimaryBannerText.degrees() } returns null
        every { mockPrimaryBannerText.drivingSide() } returns null
        every { mockPrimaryBannerText.modifier() } returns "slight left"
        every { mockPrimaryBannerText.components() } returns primaryBannerComponentList
        return mockPrimaryBannerText
    }

    private fun getPrimaryBannerComponentList(): List<BannerComponents> {
        val primaryExitComponent = buildExitComponent("Exit")
        val primaryExitNumberComponent = buildExitNumberComponent("23")
        val primaryRoadShieldComponent = buildRoadShieldComponent("I-880")
        val primaryDelimitedComponent = buildDelimiterComponent("/")
        val primaryTextComponent = buildTextComponent("Stevenson Boulevard")
        return listOf(
            primaryExitComponent,
            primaryExitNumberComponent,
            primaryRoadShieldComponent,
            primaryDelimitedComponent,
            primaryTextComponent
        )
    }

    private fun createPrimaryManeuver(bannerText: BannerText): PrimaryManeuver {
        val componentList = listOf(
            Component(
                EXIT,
                ExitComponentNode
                    .Builder()
                    .text("Exit")
                    .build()
            ),
            Component(
                EXIT_NUMBER,
                ExitNumberComponentNode
                    .Builder()
                    .text("23")
                    .build()
            ),
            Component(
                ICON,
                RoadShieldComponentNode
                    .Builder()
                    .text("I-880")
                    .build()
            ),
            Component(
                DELIMITER,
                DelimiterComponentNode
                    .Builder()
                    .text("/")
                    .build()
            ),
            Component(
                TEXT,
                TextComponentNode
                    .Builder()
                    .text("Stevenson Boulevard")
                    .abbr(null)
                    .abbrPriority(null)
                    .build()
            )
        )
        return PrimaryManeuver
            .Builder()
            .text(bannerText.text())
            .type(bannerText.type())
            .degrees(bannerText.degrees())
            .modifier(bannerText.modifier())
            .drivingSide(bannerText.drivingSide())
            .componentList(componentList)
            .build()
    }

    private fun getSecondaryBannerText(): BannerText {
        val bannerComponentList = getSecondaryBannerComponentList()
        val mockBannerText = mockk<BannerText>()
        every { mockBannerText.type() } returns "fork"
        every { mockBannerText.text() } returns "Mowry Avenue"
        every { mockBannerText.degrees() } returns null
        every { mockBannerText.drivingSide() } returns null
        every { mockBannerText.modifier() } returns "left"
        every { mockBannerText.components() } returns bannerComponentList
        return mockBannerText
    }

    private fun getSecondaryBannerComponentList(): List<BannerComponents> {
        val secondaryTextComponent = buildTextComponent("Mowry Avenue")
        return listOf(
            secondaryTextComponent
        )
    }

    private fun createSecondaryManeuver(bannerText: BannerText): SecondaryManeuver {
        return SecondaryManeuver
            .Builder()
            .text(bannerText.text())
            .type(bannerText.type())
            .degrees(bannerText.degrees())
            .modifier(bannerText.modifier())
            .drivingSide(bannerText.drivingSide())
            .componentList(
                listOf(
                    Component(
                        TEXT,
                        TextComponentNode
                            .Builder()
                            .text("Mowry Avenue")
                            .abbr(null)
                            .abbrPriority(null)
                            .build()
                    )
                )
            )
            .build()
    }

    private fun getSubBannerText(): BannerText {
        val bannerComponentList = getSubBannerComponentList()
        val mockBannerText = mockk<BannerText>()
        every { mockBannerText.type() } returns "turn"
        every { mockBannerText.text() } returns "Central Fremont"
        every { mockBannerText.degrees() } returns null
        every { mockBannerText.drivingSide() } returns null
        every { mockBannerText.modifier() } returns "right"
        every { mockBannerText.components() } returns bannerComponentList
        return mockBannerText
    }

    private fun getLaneBannerText(): BannerText {
        val bannerComponentList = buildLaneComponent()
        val mockBannerText = mockk<BannerText>()
        every { mockBannerText.type() } returns null
        every { mockBannerText.text() } returns ""
        every { mockBannerText.degrees() } returns null
        every { mockBannerText.drivingSide() } returns null
        every { mockBannerText.modifier() } returns null
        every { mockBannerText.components() } returns bannerComponentList
        return mockBannerText
    }

    private fun getSubBannerComponentList(): List<BannerComponents> {
        val subRoadShieldComponent = buildRoadShieldComponent("I-880")
        val subDelimitedComponent = buildDelimiterComponent("/")
        val subTextComponent = buildTextComponent("Central Fremont")
        return listOf(
            subRoadShieldComponent,
            subDelimitedComponent,
            subTextComponent
        )
    }

    private fun createSubManeuver(bannerText: BannerText): SubManeuver {
        val componentList = listOf(
            Component(
                ICON,
                RoadShieldComponentNode
                    .Builder()
                    .text("I-880")
                    .shieldIcon(null)
                    .build()
            ),
            Component(
                DELIMITER,
                DelimiterComponentNode
                    .Builder()
                    .text("/")
                    .build()
            ),
            Component(
                TEXT,
                TextComponentNode
                    .Builder()
                    .text("Central Fremont")
                    .abbr(null)
                    .abbrPriority(null)
                    .build()
            )
        )
        return SubManeuver
            .Builder()
            .text(bannerText.text())
            .type(bannerText.type())
            .degrees(bannerText.degrees())
            .modifier(bannerText.modifier())
            .drivingSide(bannerText.drivingSide())
            .componentList(componentList)
            .build()
    }

    private fun createLaneManeuver(): List<LaneIndicator> {
        return listOf(
            LaneIndicator
                .Builder()
                .isActive(false)
                .directions(listOf("left"))
                .build(),
            LaneIndicator
                .Builder()
                .isActive(false)
                .directions(listOf("right"))
                .build(),
            LaneIndicator
                .Builder()
                .isActive(true)
                .directions(listOf("straight"))
                .build()
        )
    }

    private fun buildExitComponent(text: String): BannerComponents {
        return builder()
            .type(EXIT)
            .text(text)
            .build()
    }

    private fun buildExitNumberComponent(text: String): BannerComponents {
        return builder()
            .type(EXIT_NUMBER)
            .text(text)
            .build()
    }

    private fun buildDelimiterComponent(text: String): BannerComponents {
        return builder()
            .type(DELIMITER)
            .text(text)
            .build()
    }

    private fun buildRoadShieldComponent(text: String): BannerComponents {
        return builder()
            .type(ICON)
            .text(text)
            .build()
    }

    private fun buildTextComponent(text: String): BannerComponents {
        return builder()
            .type(TEXT)
            .text(text)
            .build()
    }

    private fun buildLaneComponent(): List<BannerComponents> {
        return listOf(
            builder()
                .type("lane")
                .text("")
                .active(false)
                .directions(listOf("left"))
                .build(),
            builder()
                .type("lane")
                .text("")
                .active(false)
                .directions(listOf("right"))
                .build(),
            builder()
                .type("lane")
                .text("")
                .active(true)
                .directions(listOf("straight"))
                .build()
        )
    }
}
