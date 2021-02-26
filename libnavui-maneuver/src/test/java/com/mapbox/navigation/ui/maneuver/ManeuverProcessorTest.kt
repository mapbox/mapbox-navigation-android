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
import com.mapbox.api.directions.v5.models.ManeuverModifier
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteStepProgress
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.maneuver.model.Component
import com.mapbox.navigation.ui.maneuver.model.DelimiterComponentNode
import com.mapbox.navigation.ui.maneuver.model.ExitComponentNode
import com.mapbox.navigation.ui.maneuver.model.ExitNumberComponentNode
import com.mapbox.navigation.ui.maneuver.model.Lane
import com.mapbox.navigation.ui.maneuver.model.LaneIndicator
import com.mapbox.navigation.ui.maneuver.model.Maneuver
import com.mapbox.navigation.ui.maneuver.model.PrimaryManeuver
import com.mapbox.navigation.ui.maneuver.model.RoadShieldComponentNode
import com.mapbox.navigation.ui.maneuver.model.SecondaryManeuver
import com.mapbox.navigation.ui.maneuver.model.SubManeuver
import com.mapbox.navigation.ui.maneuver.model.TextComponentNode
import com.mapbox.navigation.ui.maneuver.model.TotalManeuverDistance
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class ManeuverProcessorTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Test
    fun `process action step distance remaining result distance`() =
        coroutineRule.runBlockingTest {
            val maneuverProcessor = ManeuverProcessor()
            val mockRouteStepProgress = mockk<RouteStepProgress>()
            val mockStepDistanceRemaining = 324f
            every { mockRouteStepProgress.distanceRemaining } returns mockStepDistanceRemaining
            val mockAction = ManeuverAction.GetStepDistanceRemaining(mockRouteStepProgress)
            val expected = ManeuverResult.GetStepDistanceRemaining(
                mockStepDistanceRemaining.toDouble()
            )

            val actual = maneuverProcessor.process(mockAction)

            assertEquals(expected, actual)
        }

    @Test
    fun `process action get maneuver list result no currentLegProgress empty maneuver list`() =
        coroutineRule.runBlockingTest {
            val maneuverProcessor = ManeuverProcessor()
            val mockRouteProgress = mockk<RouteProgress>()
            every { mockRouteProgress.currentLegProgress } returns null
            val mockAction = ManeuverAction.GetAllBannerInstructions(mockRouteProgress)
            val expected = ManeuverResult.GetAllBannerInstructions(listOf())

            val actual = maneuverProcessor.process(mockAction)

            assertEquals(expected, actual)
        }

    @Test
    fun `process action get maneuver list result no routeLeg empty maneuver list`() =
        coroutineRule.runBlockingTest {
            val maneuverProcessor = ManeuverProcessor()
            val mockLegProgress = mockk<RouteLegProgress>()
            every { mockLegProgress.routeLeg } returns null
            val mockRouteProgress = mockk<RouteProgress>()
            every { mockRouteProgress.currentLegProgress } returns mockLegProgress
            val mockAction = ManeuverAction.GetAllBannerInstructions(mockRouteProgress)
            val expected = ManeuverResult.GetAllBannerInstructions(listOf())

            val actual = maneuverProcessor.process(mockAction)

            assertEquals(expected, actual)
        }

    @Test
    fun `process action get maneuver list result no steps empty maneuver list`() =
        coroutineRule.runBlockingTest {
            val maneuverProcessor = ManeuverProcessor()
            val mockRouteLeg = mockk<RouteLeg>()
            every { mockRouteLeg.steps() } returns null
            val mockLegProgress = mockk<RouteLegProgress>()
            every { mockLegProgress.routeLeg } returns null
            val mockRouteProgress = mockk<RouteProgress>()
            every { mockRouteProgress.currentLegProgress } returns mockLegProgress
            val mockAction = ManeuverAction.GetAllBannerInstructions(mockRouteProgress)
            val expected = ManeuverResult.GetAllBannerInstructions(listOf())

            val actual = maneuverProcessor.process(mockAction)

            assertEquals(expected, actual)
        }

    @Test
    fun `process action find maneuver list result no instructions empty maneuver list`() =
        coroutineRule.runBlockingTest {
            val maneuverProcessor = ManeuverProcessor()
            val mockStep = mockk<LegStep>()
            every { mockStep.bannerInstructions() } returns null
            val mockRouteLeg = mockk<RouteLeg>()
            every { mockRouteLeg.steps() } returns listOf(mockStep)
            val mockLegProgress = mockk<RouteLegProgress>()
            every { mockLegProgress.routeLeg } returns null
            val mockRouteProgress = mockk<RouteProgress>()
            every { mockRouteProgress.currentLegProgress } returns mockLegProgress
            val mockAction = ManeuverAction.GetAllBannerInstructions(mockRouteProgress)
            val expected = ManeuverResult.GetAllBannerInstructions(listOf())

            val actual = maneuverProcessor.process(mockAction)

            assertEquals(expected, actual)
        }

    @Test
    fun `process action filter maneuver after step null step distance geometry`() =
        coroutineRule.runBlockingTest {
            val maneuverProcessor = ManeuverProcessor()
            val mockBannerInstruction = mockk<BannerInstructions>()
            val mockLegProgress = mockk<RouteLegProgress>()
            every { mockLegProgress.currentStepProgress } returns null
            val mockRouteProgress = mockk<RouteProgress>()
            every { mockRouteProgress.currentLegProgress } returns mockLegProgress
            val mockAction = ManeuverAction.GetAllBannerInstructionsAfterStep(
                mockRouteProgress,
                listOf(mockBannerInstruction)
            )
            val expected = ManeuverResult.GetAllBannerInstructionsAfterStep(
                listOf(mockBannerInstruction)
            )

            val actual = maneuverProcessor.process(mockAction)

            assertEquals(expected, actual)
        }

    @Test
    fun `process action filter maneuver after step original list does not contain step banner`() =
        coroutineRule.runBlockingTest {
            val maneuverProcessor = ManeuverProcessor()
            val mockLegStep = mockk<LegStep>()
            every { mockLegStep.bannerInstructions() } returns listOf(
                getMockBannerInstruction({ "primary3" }, { 123.0 })
            )
            val mockStepProgress = mockk<RouteStepProgress>()
            every { mockStepProgress.step } returns mockLegStep
            every { mockStepProgress.distanceRemaining } returns 12f
            val mockLegProgress = mockk<RouteLegProgress>()
            every { mockLegProgress.currentStepProgress } returns mockStepProgress
            val mockRouteProgress = mockk<RouteProgress>()
            every { mockRouteProgress.currentLegProgress } returns mockLegProgress
            val mockBannerInstruction1 = getMockBannerInstruction({ "primary1" }, { 1.0 })
            val mockBannerInstruction2 = getMockBannerInstruction({ "primary2" }, { 2.0 })
            val mockAction = ManeuverAction.GetAllBannerInstructionsAfterStep(
                mockRouteProgress,
                listOf(mockBannerInstruction1, mockBannerInstruction2)
            )
            val expected = ManeuverResult.GetAllBannerInstructionsAfterStep(
                listOf(mockBannerInstruction1, mockBannerInstruction2)
            )

            val actual = maneuverProcessor.process(mockAction)

            assertEquals(expected, actual)
        }

    @Test
    fun `process action filter maneuver after step original list contains step banner`() =
        coroutineRule.runBlockingTest {
            val maneuverProcessor = ManeuverProcessor()
            val mockLegStep = mockk<LegStep>()
            val mockBannerInstruction1 = getMockBannerInstruction({ "primary1" }, { 1.0 })
            val mockBannerInstruction2 = getMockBannerInstruction({ "primary2" }, { 2.0 })
            every { mockLegStep.bannerInstructions() } returns listOf(mockBannerInstruction1)
            val mockStepProgress = mockk<RouteStepProgress>()
            every { mockStepProgress.step } returns mockLegStep
            every { mockStepProgress.distanceRemaining } returns 12f
            val mockLegProgress = mockk<RouteLegProgress>()
            every { mockLegProgress.currentStepProgress } returns mockStepProgress
            val mockRouteProgress = mockk<RouteProgress>()
            every { mockRouteProgress.currentLegProgress } returns mockLegProgress
            val mockAction = ManeuverAction.GetAllBannerInstructionsAfterStep(
                mockRouteProgress,
                listOf(mockBannerInstruction1, mockBannerInstruction2)
            )
            val expected = ManeuverResult.GetAllBannerInstructionsAfterStep(
                listOf(mockBannerInstruction2)
            )

            val actual = maneuverProcessor.process(mockAction)

            assertEquals(expected, actual)
        }

    @Test
    fun `process action maneuver list from filtered banner instruction list`() =
        coroutineRule.runBlockingTest {
            val maneuverProcessor = ManeuverProcessor()
            val mockBannerInstruction = getMockBannerInstruction({ "primary1" }, { 12.0 })
            val mockAction = ManeuverAction.GetAllManeuvers(listOf(mockBannerInstruction))

            val actual = maneuverProcessor.process(mockAction) as ManeuverResult.GetAllManeuvers

            assertEquals(
                mockBannerInstruction.primary().text(), actual.maneuverList[0].primary.text
            )
            assertEquals(
                mockBannerInstruction.distanceAlongGeometry(),
                actual.maneuverList[0].totalManeuverDistance.totalDistance,
                0.0
            )
            assertNull(actual.maneuverList[0].secondary)
            assertNull(actual.maneuverList[0].sub)
            assertNull(actual.maneuverList[0].laneGuidance)
        }

    @Test
    fun `process action maneuver list from new filtered banner instruction list`() =
        coroutineRule.runBlockingTest {
            val maneuverProcessor = ManeuverProcessor()
            val mockBannerInstruction1 = getMockBannerInstruction({ "primary1" }, { 12.0 })
            val mockAction1 = ManeuverAction.GetAllManeuvers(listOf(mockBannerInstruction1))
            maneuverProcessor.process(mockAction1) as ManeuverResult.GetAllManeuvers
            val mockBannerInstruction2 = getMockBannerInstruction({ "primary2" }, { 22.0 })
            val mockAction2 = ManeuverAction.GetAllManeuvers(listOf(mockBannerInstruction2))
            val maneuverList2 =
                maneuverProcessor.process(mockAction2) as ManeuverResult.GetAllManeuvers

            assertEquals(
                mockBannerInstruction2.primary().text(), maneuverList2.maneuverList[0].primary.text
            )
            assertEquals(
                mockBannerInstruction2.distanceAlongGeometry(),
                maneuverList2.maneuverList[0].totalManeuverDistance.totalDistance,
                0.0
            )
            assertNull(maneuverList2.maneuverList[0].secondary)
            assertNull(maneuverList2.maneuverList[0].sub)
            assertNull(maneuverList2.maneuverList[0].laneGuidance)
        }

    @Test
    fun `process action maneuver list return from cache`() =
        coroutineRule.runBlockingTest {
            val maneuverProcessor = ManeuverProcessor()
            val mockBannerInstruction = getMockBannerInstruction({ "primary1" }, { 12.0 })
            val mockAction1 = ManeuverAction.GetAllManeuvers(
                listOf(mockBannerInstruction)
            )
            val maneuverList1 =
                maneuverProcessor.process(mockAction1) as ManeuverResult.GetAllManeuvers
            val mockAction2 = ManeuverAction.GetAllManeuvers(
                listOf(mockBannerInstruction)
            )
            val maneuverList2 =
                maneuverProcessor.process(mockAction2) as ManeuverResult.GetAllManeuvers

            assertEquals(maneuverList1, maneuverList2)
        }

    @Test
    fun `process action get current maneuver result without secondary, sub and lane`() =
        coroutineRule.runBlockingTest {
            val maneuverProcessor = ManeuverProcessor()
            val mockPrimaryBannerText = getPrimaryBannerText()
            val mockBannerInstructions = mockk<BannerInstructions>()
            val mockTotalStepDistance = 23.0
            every { mockBannerInstructions.primary() } returns mockPrimaryBannerText
            every { mockBannerInstructions.secondary() } returns null
            every { mockBannerInstructions.sub() } returns null
            every { mockBannerInstructions.distanceAlongGeometry() } returns mockTotalStepDistance

            val mockAction = ManeuverAction.GetManeuver(mockBannerInstructions)
            val primaryManeuver = createPrimaryManeuver(mockPrimaryBannerText)
            val expected = ManeuverResult.GetManeuver(
                Maneuver(
                    primaryManeuver,
                    TotalManeuverDistance(mockTotalStepDistance),
                    null,
                    null,
                    null
                )
            )

            val actual = maneuverProcessor.process(mockAction) as ManeuverResult.GetManeuver

            assertEquals(expected.maneuver.primary, actual.maneuver.primary)
            assertEquals(
                expected.maneuver.totalManeuverDistance,
                actual.maneuver.totalManeuverDistance
            )
            assertNull(actual.maneuver.sub)
            assertNull(actual.maneuver.secondary)
            assertNull(actual.maneuver.laneGuidance)
        }

    @Test
    fun `process action get current maneuver result without sub and lane`() =
        coroutineRule.runBlockingTest {
            val maneuverProcessor = ManeuverProcessor()
            val mockPrimaryBannerText = getPrimaryBannerText()
            val mockSecondaryBannerText = getSecondaryBannerText()
            val mockBannerInstructions = mockk<BannerInstructions>()
            val mockTotalStepDistance = 23.0
            every { mockBannerInstructions.primary() } returns mockPrimaryBannerText
            every { mockBannerInstructions.secondary() } returns mockSecondaryBannerText
            every { mockBannerInstructions.sub() } returns null
            every { mockBannerInstructions.distanceAlongGeometry() } returns mockTotalStepDistance

            val mockAction = ManeuverAction.GetManeuver(mockBannerInstructions)
            val primaryManeuver = createPrimaryManeuver(mockPrimaryBannerText)
            val secondaryManeuver = createSecondaryManeuver(mockSecondaryBannerText)
            val expected = ManeuverResult.GetManeuver(
                Maneuver(
                    primaryManeuver,
                    TotalManeuverDistance(mockTotalStepDistance),
                    secondaryManeuver,
                    null,
                    null
                )
            )

            val actual = maneuverProcessor.process(mockAction) as ManeuverResult.GetManeuver

            assertEquals(expected.maneuver.primary, actual.maneuver.primary)
            assertEquals(expected.maneuver.secondary, actual.maneuver.secondary)
            assertEquals(
                expected.maneuver.totalManeuverDistance,
                actual.maneuver.totalManeuverDistance
            )
            assertNull(actual.maneuver.sub)
            assertNull(actual.maneuver.laneGuidance)
        }

    @Test
    fun `process action get current maneuver result with sub and without lane`() =
        coroutineRule.runBlockingTest {
            val maneuverProcessor = ManeuverProcessor()
            val mockPrimaryBannerText = getPrimaryBannerText()
            val mockSecondaryBannerText = getSecondaryBannerText()
            val mockSubBannerText = getSubBannerText()
            val mockBannerInstructions = mockk<BannerInstructions>()
            val mockTotalStepDistance = 23.0
            every { mockBannerInstructions.primary() } returns mockPrimaryBannerText
            every { mockBannerInstructions.secondary() } returns mockSecondaryBannerText
            every { mockBannerInstructions.sub() } returns mockSubBannerText
            every { mockBannerInstructions.distanceAlongGeometry() } returns mockTotalStepDistance

            val mockAction = ManeuverAction.GetManeuver(mockBannerInstructions)
            val primaryManeuver = createPrimaryManeuver(mockPrimaryBannerText)
            val secondaryManeuver = createSecondaryManeuver(mockSecondaryBannerText)
            val subManeuver = createSubManeuver(mockSubBannerText)
            val expected = ManeuverResult.GetManeuver(
                Maneuver(
                    primaryManeuver,
                    TotalManeuverDistance(mockTotalStepDistance),
                    secondaryManeuver,
                    subManeuver,
                    null
                )
            )

            val actual = maneuverProcessor.process(mockAction) as ManeuverResult.GetManeuver

            assertEquals(expected.maneuver.primary, actual.maneuver.primary)
            assertEquals(expected.maneuver.secondary, actual.maneuver.secondary)
            assertEquals(
                expected.maneuver.totalManeuverDistance,
                actual.maneuver.totalManeuverDistance
            )
            assertEquals(expected.maneuver.sub, actual.maneuver.sub)
            assertNull(actual.maneuver.laneGuidance)
        }

    @Test
    fun `process action get current maneuver result without sub and with lane`() =
        coroutineRule.runBlockingTest {
            val maneuverProcessor = ManeuverProcessor()
            val mockPrimaryBannerText = getPrimaryBannerText()
            val mockSecondaryBannerText = getSecondaryBannerText()
            val mockLaneBannerText = getLaneBannerText()
            val mockBannerInstructions = mockk<BannerInstructions>()
            val mockTotalStepDistance = 23.0
            every { mockBannerInstructions.primary() } returns mockPrimaryBannerText
            every { mockBannerInstructions.secondary() } returns mockSecondaryBannerText
            every { mockBannerInstructions.sub() } returns mockLaneBannerText
            every { mockBannerInstructions.distanceAlongGeometry() } returns mockTotalStepDistance

            val mockAction = ManeuverAction.GetManeuver(mockBannerInstructions)
            val primaryManeuver = createPrimaryManeuver(mockPrimaryBannerText)
            val secondaryManeuver = createSecondaryManeuver(mockSecondaryBannerText)
            val laneManeuver = Lane
                .Builder()
                .allLanes(createLaneManeuver())
                .activeDirection(mockPrimaryBannerText.modifier())
                .build()
            val expected = ManeuverResult.GetManeuver(
                Maneuver(
                    primaryManeuver,
                    TotalManeuverDistance(mockTotalStepDistance),
                    secondaryManeuver,
                    null,
                    laneManeuver
                )
            )

            val actual = maneuverProcessor.process(mockAction) as ManeuverResult.GetManeuver

            assertEquals(expected.maneuver.primary, actual.maneuver.primary)
            assertEquals(expected.maneuver.secondary, actual.maneuver.secondary)
            assertEquals(
                expected.maneuver.totalManeuverDistance,
                actual.maneuver.totalManeuverDistance
            )
            assertEquals(expected.maneuver.laneGuidance, actual.maneuver.laneGuidance)
            assertNull(actual.maneuver.sub)
        }

    private fun getMockBannerInstruction(
        textPrimary: () -> String,
        distanceAlongGeometry: () -> Double
    ): BannerInstructions {
        val bannerInstructions = mockk<BannerInstructions>()
        every { bannerInstructions.primary() } returns mockBannerText(
            { textPrimary() },
            { listOf(mockBannerComponent({ textPrimary() }, { TEXT })) }
        )
        every { bannerInstructions.secondary() } returns null
        every { bannerInstructions.sub() } returns null
        every { bannerInstructions.distanceAlongGeometry() } returns distanceAlongGeometry()
        return bannerInstructions
    }

    private fun mockBannerText(
        text: () -> String,
        componentList: () -> List<BannerComponents>,
        type: () -> String = { TEXT },
        modifier: () -> String = { ManeuverModifier.RIGHT },
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
