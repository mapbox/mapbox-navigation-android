package com.mapbox.navigation.ui.maneuver.view

import android.content.Context
import android.text.SpannableString
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.ManeuverModifier
import com.mapbox.api.directions.v5.models.StepManeuver
import com.mapbox.navigation.ui.base.model.maneuver.Component
import com.mapbox.navigation.ui.base.model.maneuver.DelimiterComponentNode
import com.mapbox.navigation.ui.base.model.maneuver.ExitComponentNode
import com.mapbox.navigation.ui.base.model.maneuver.ExitNumberComponentNode
import com.mapbox.navigation.ui.base.model.maneuver.Lane
import com.mapbox.navigation.ui.base.model.maneuver.LaneIndicator
import com.mapbox.navigation.ui.base.model.maneuver.Maneuver
import com.mapbox.navigation.ui.base.model.maneuver.ManeuverState
import com.mapbox.navigation.ui.base.model.maneuver.PrimaryManeuver
import com.mapbox.navigation.ui.base.model.maneuver.RoadShieldComponentNode
import com.mapbox.navigation.ui.base.model.maneuver.SecondaryManeuver
import com.mapbox.navigation.ui.base.model.maneuver.SubManeuver
import com.mapbox.navigation.ui.base.model.maneuver.TextComponentNode
import com.mapbox.navigation.ui.base.model.maneuver.TotalManeuverDistance
import com.mapbox.navigation.ui.maneuver.R
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MapboxManeuverViewTest {

    private lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `init attribute primary maneuver text color`() {
        val expectedTextColor = ctx.getColor(R.color.primaryManeuverTextColor)
        val view = MapboxManeuverView(ctx)

        assertEquals(
            expectedTextColor,
            view.findViewById<TextView>(R.id.primaryManeuverText).currentTextColor
        )
    }

    @Test
    fun `init attribute secondary maneuver text color`() {
        val expectedTextColor = ctx.getColor(R.color.secondaryManeuverTextColor)
        val view = MapboxManeuverView(ctx)

        assertEquals(
            expectedTextColor,
            view.findViewById<TextView>(R.id.secondaryManeuverText).currentTextColor
        )
    }

    @Test
    fun `init attribute sub maneuver text color`() {
        val expectedTextColor = ctx.getColor(R.color.subManeuverTextColor)
        val view = MapboxManeuverView(ctx)

        assertEquals(
            expectedTextColor,
            view.findViewById<TextView>(R.id.subManeuverText).currentTextColor
        )
    }

    @Test
    fun `init attribute main maneuver card background color`() {
        val expectedBackgroundColor = ctx.getColor(R.color.mainManeuverCardBackgroundColor)
        val view = MapboxManeuverView(ctx)

        assertEquals(
            expectedBackgroundColor,
            view.findViewById<CardView>(R.id.mainManeuverView).cardBackgroundColor.defaultColor
        )
    }

    @Test
    fun `init attribute sub maneuver card background color`() {
        val expectedBackgroundColor = ctx.getColor(R.color.subManeuverBackgroundColor)
        val view = MapboxManeuverView(ctx)

        assertEquals(
            expectedBackgroundColor,
            view.findViewById<CardView>(R.id.subManeuverView).cardBackgroundColor.defaultColor
        )
    }

    @Test
    fun `init attribute lane guidance card background color`() {
        val expectedBackgroundColor = ctx.getColor(R.color.laneGuidanceBackgroundColor)
        val view = MapboxManeuverView(ctx)

        assertEquals(
            expectedBackgroundColor,
            view.findViewById<CardView>(R.id.laneGuidanceCard).cardBackgroundColor.defaultColor
        )
    }

    @Test
    fun `show upcoming maneuver list visibility on click`() {
        val view = MapboxManeuverView(ctx)
        val expected = VISIBLE
        view.findViewById<RecyclerView>(R.id.upcomingManeuverRecycler).visibility = GONE

        view.performClick()

        val actual = view.findViewById<RecyclerView>(R.id.upcomingManeuverRecycler).visibility

        assertEquals(expected, actual)
    }

    @Test
    fun `hide upcoming maneuver list visibility on click`() {
        val view = MapboxManeuverView(ctx)
        val expected = GONE
        view.findViewById<RecyclerView>(R.id.upcomingManeuverRecycler).visibility = VISIBLE

        view.performClick()

        val actual = view.findViewById<RecyclerView>(R.id.upcomingManeuverRecycler).visibility

        assertEquals(expected, actual)
    }

    @Test
    fun `render upcoming maneuver list visibility show`() {
        val view = MapboxManeuverView(ctx)
        val mockState = ManeuverState.UpcomingManeuvers.Show
        val expected = VISIBLE

        view.render(mockState)
        val actual = view.findViewById<RecyclerView>(R.id.upcomingManeuverRecycler).visibility

        assertEquals(expected, actual)
    }

    @Test
    fun `render upcoming maneuver list visibility hide`() {
        val view = MapboxManeuverView(ctx)
        val mockState = ManeuverState.UpcomingManeuvers.Hide
        val expected = GONE

        view.render(mockState)
        val actual = view.findViewById<RecyclerView>(R.id.upcomingManeuverRecycler).visibility

        assertEquals(expected, actual)
    }

    @Test
    fun `render step distance remaining`() {
        val view = MapboxManeuverView(ctx)
        val stepDistanceRemaining = 45.0
        val totalStepDistance = ManeuverState.DistanceRemainingToFinishStep(
            mockk {
                every { formatDistance(stepDistanceRemaining) } returns SpannableString("13 mi")
            },
            stepDistanceRemaining
        )
        val expected = SpannableString("13 mi")

        view.render(totalStepDistance)
        val actual = view.findViewById<MapboxStepDistance>(R.id.stepDistance).text

        assertEquals(expected.toString(), actual.toString())
    }

    @Test
    fun `render primary maneuver text empty list`() {
        val totalManeuverDistance = mockk<TotalManeuverDistance>()
        val primaryManeuver = mockk<PrimaryManeuver> {
            every { text } returns "Central Fremont"
            every { type } returns StepManeuver.TURN
            every { degrees } returns null
            every { modifier } returns ManeuverModifier.RIGHT
            every { drivingSide } returns null
            every { componentList } returns listOf()
        }
        val maneuver = Maneuver
            .Builder()
            .primary(primaryManeuver)
            .totalManeuverDistance(totalManeuverDistance)
            .secondary(null)
            .sub(null)
            .laneGuidance(null)
            .build()
        val view = MapboxManeuverView(ctx)
        val expected = ""

        view.render(ManeuverState.CurrentManeuver(maneuver))
        val actual = view.findViewById<MapboxPrimaryManeuver>(R.id.primaryManeuverText).text

        assertEquals(expected, actual)
    }

    @Test
    fun `render primary maneuver text`() {
        val totalManeuverDistance = mockk<TotalManeuverDistance>()
        val primaryManeuver = getMockPrimaryManeuver()
        val maneuver = Maneuver
            .Builder()
            .primary(primaryManeuver)
            .totalManeuverDistance(totalManeuverDistance)
            .secondary(null)
            .sub(null)
            .laneGuidance(null)
            .build()
        val view = MapboxManeuverView(ctx)
        val expected = "Central Fremont "

        view.render(ManeuverState.ManeuverPrimary.Instruction(maneuver.primary))
        val actual = view.findViewById<MapboxPrimaryManeuver>(R.id.primaryManeuverText).text

        assertEquals(expected, actual.toString())
    }

    @Test
    fun `render secondary maneuver text empty list`() {
        val totalManeuverDistance = mockk<TotalManeuverDistance>()
        val primaryManeuver = getMockPrimaryManeuver()
        val secondaryManeuver = mockk<SecondaryManeuver> {
            every { text } returns "Stivers Street"
            every { type } returns StepManeuver.TURN
            every { degrees } returns null
            every { modifier } returns ManeuverModifier.RIGHT
            every { drivingSide } returns null
            every { componentList } returns listOf()
        }
        val maneuver = Maneuver
            .Builder()
            .primary(primaryManeuver)
            .totalManeuverDistance(totalManeuverDistance)
            .secondary(secondaryManeuver)
            .sub(null)
            .laneGuidance(null)
            .build()
        val view = MapboxManeuverView(ctx)
        val expected = ""

        view.render(ManeuverState.CurrentManeuver(maneuver))
        val actual = view.findViewById<MapboxSecondaryManeuver>(R.id.secondaryManeuverText).text

        assertEquals(expected, actual)
    }

    @Test
    fun `render secondary maneuver text empty and query primary max lines`() {
        val view = MapboxManeuverView(ctx)
        val expected = 2

        view.render(ManeuverState.ManeuverSecondary.Hide)
        val actual = view.findViewById<MapboxPrimaryManeuver>(R.id.primaryManeuverText).maxLines

        assertEquals(expected, actual)
    }

    @Test
    fun `render secondary maneuver visibility hide`() {
        val view = MapboxManeuverView(ctx)
        val expected = GONE

        view.render(ManeuverState.ManeuverSecondary.Hide)
        val actual =
            view.findViewById<MapboxSecondaryManeuver>(R.id.secondaryManeuverText).visibility

        assertEquals(expected, actual)
    }

    @Test
    fun `render secondary maneuver visibility show`() {
        val view = MapboxManeuverView(ctx)
        val expected = VISIBLE

        view.render(ManeuverState.ManeuverSecondary.Show)
        val actual =
            view.findViewById<MapboxSecondaryManeuver>(R.id.secondaryManeuverText).visibility

        assertEquals(expected, actual)
    }

    @Test
    fun `render secondary maneuver hide update primary maneuver constraints`() {
        val view = MapboxManeuverView(ctx)
        val expectedTopToTop = ConstraintLayout.LayoutParams.PARENT_ID
        val expectedBottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID

        view.render(ManeuverState.ManeuverSecondary.Hide)
        val primaryManeuverView = view.findViewById<MapboxPrimaryManeuver>(R.id.primaryManeuverText)
        val params = primaryManeuverView.layoutParams as ConstraintLayout.LayoutParams
        val actualTopToTop = params.topToTop
        val actualBottomToBottom = params.bottomToBottom

        assertEquals(expectedTopToTop, actualTopToTop)
        assertEquals(expectedBottomToBottom, actualBottomToBottom)
    }

    @Test
    fun `render secondary maneuver show update primary maneuver constraints`() {
        val view = MapboxManeuverView(ctx)
        val expectedTopToTop = ConstraintLayout.LayoutParams.UNSET
        val expectedBottomToTop =
            view.findViewById<MapboxSecondaryManeuver>(R.id.secondaryManeuverText).id
        val expectedBottomToBottom = ConstraintLayout.LayoutParams.UNSET

        view.render(ManeuverState.ManeuverSecondary.Show)
        val primaryManeuverView = view.findViewById<MapboxPrimaryManeuver>(R.id.primaryManeuverText)
        val params = primaryManeuverView.layoutParams as ConstraintLayout.LayoutParams
        val actualTopToTop = params.topToTop
        val actualBottomToTop = params.bottomToTop
        val actualBottomToBottom = params.bottomToBottom

        assertEquals(expectedTopToTop, actualTopToTop)
        assertEquals(expectedBottomToTop, actualBottomToTop)
        assertEquals(expectedBottomToBottom, actualBottomToBottom)
    }

    @Test
    fun `render secondary maneuver text`() {
        val totalManeuverDistance = mockk<TotalManeuverDistance>()
        val primaryManeuver = getMockPrimaryManeuver()
        val secondaryManeuver = getMockSecondaryManeuver()
        val maneuver = Maneuver
            .Builder()
            .primary(primaryManeuver)
            .totalManeuverDistance(totalManeuverDistance)
            .secondary(secondaryManeuver)
            .sub(null)
            .laneGuidance(null)
            .build()
        val view = MapboxManeuverView(ctx)
        val expected = "I-880 / Stivers Street "

        view.render(ManeuverState.ManeuverSecondary.Instruction(maneuver.secondary))
        val actual = view.findViewById<MapboxSecondaryManeuver>(R.id.secondaryManeuverText).text

        assertEquals(expected, actual.toString())
    }

    @Test
    fun `render secondary maneuver text and query primary max lines`() {
        val view = MapboxManeuverView(ctx)
        val expected = 1

        view.render(ManeuverState.ManeuverSecondary.Show)
        val actual = view.findViewById<MapboxPrimaryManeuver>(R.id.primaryManeuverText).maxLines

        assertEquals(expected, actual)
    }

    @Test
    fun `render sub maneuver text empty list`() {
        val totalManeuverDistance = mockk<TotalManeuverDistance>()
        val primaryManeuver = getMockPrimaryManeuver()
        val subManeuver = mockk<SubManeuver> {
            every { text } returns "Stivers Street"
            every { type } returns StepManeuver.TURN
            every { degrees } returns null
            every { modifier } returns ManeuverModifier.RIGHT
            every { drivingSide } returns null
            every { componentList } returns listOf()
        }
        val maneuver = Maneuver
            .Builder()
            .primary(primaryManeuver)
            .totalManeuverDistance(totalManeuverDistance)
            .secondary(null)
            .sub(subManeuver)
            .laneGuidance(null)
            .build()
        val view = MapboxManeuverView(ctx)
        val expected = ""

        view.render(ManeuverState.ManeuverSub.Instruction(maneuver.sub))
        val actual = view.findViewById<MapboxSubManeuver>(R.id.subManeuverText).text

        assertEquals(expected, actual)
    }

    @Test
    fun `render sub maneuver text`() {
        val totalManeuverDistance = mockk<TotalManeuverDistance>()
        val primaryManeuver = getMockPrimaryManeuver()
        val subManeuver = getMockSubManeuver()
        val maneuver = Maneuver
            .Builder()
            .primary(primaryManeuver)
            .totalManeuverDistance(totalManeuverDistance)
            .secondary(null)
            .sub(subManeuver)
            .laneGuidance(null)
            .build()
        val view = MapboxManeuverView(ctx)
        val expected = "23 I-880 / Stivers Street "

        view.render(ManeuverState.ManeuverSub.Instruction(maneuver.sub))
        val actual = view.findViewById<MapboxSubManeuver>(R.id.subManeuverText).text

        assertEquals(expected, actual.toString())
    }

    @Test
    fun `render sub maneuver visibility hide`() {
        val view = MapboxManeuverView(ctx)
        val expected = GONE

        view.render(ManeuverState.ManeuverSub.Hide)
        val actual =
            view.findViewById<CardView>(R.id.subManeuverView).visibility

        assertEquals(expected, actual)
    }

    @Test
    fun `render sub maneuver visibility show`() {
        val view = MapboxManeuverView(ctx)
        val expected = VISIBLE

        view.render(ManeuverState.ManeuverSub.Show)
        val actual =
            view.findViewById<CardView>(R.id.subManeuverView).visibility

        assertEquals(expected, actual)
    }

    @Test
    fun `render lane guidance hide`() {
        val view = MapboxManeuverView(ctx)
        val expected = GONE

        view.render(ManeuverState.LaneGuidanceManeuver.Hide)
        val actual = view.findViewById<CardView>(R.id.laneGuidanceCard).visibility

        assertEquals(expected, actual)
    }

    @Test
    fun `render lane guidance show`() {
        val view = MapboxManeuverView(ctx)
        val expected = VISIBLE

        view.render(ManeuverState.LaneGuidanceManeuver.Show)
        val actual = view.findViewById<CardView>(R.id.laneGuidanceCard).visibility

        assertEquals(expected, actual)
    }

    @Test
    fun `render lane guidance query recycler item count`() {
        val totalManeuverDistance = mockk<TotalManeuverDistance>()
        val primaryManeuver = getMockPrimaryManeuver()
        val lane = getMockLane()
        val maneuver = Maneuver
            .Builder()
            .primary(primaryManeuver)
            .totalManeuverDistance(totalManeuverDistance)
            .secondary(null)
            .sub(null)
            .laneGuidance(lane)
            .build()
        val view = MapboxManeuverView(ctx)
        val expected = lane.allLanes.size

        view.render(ManeuverState.LaneGuidanceManeuver.AddLanes(maneuver.laneGuidance!!))
        val actual = view.findViewById<RecyclerView>(R.id.laneGuidanceRecycler).adapter?.itemCount

        assertEquals(expected, actual)
    }

    @Test
    fun `render lane guidance remove lane guidance query item count`() {
        val totalManeuverDistance = mockk<TotalManeuverDistance>()
        val primaryManeuver = getMockPrimaryManeuver()
        val lane = getMockLane()
        val maneuver = Maneuver
            .Builder()
            .primary(primaryManeuver)
            .totalManeuverDistance(totalManeuverDistance)
            .secondary(null)
            .sub(null)
            .laneGuidance(lane)
            .build()
        val view = MapboxManeuverView(ctx)
        val expected = 0

        view.render(ManeuverState.LaneGuidanceManeuver.AddLanes(maneuver.laneGuidance!!))
        view.render(ManeuverState.LaneGuidanceManeuver.RemoveLanes)
        val actual = view.findViewById<RecyclerView>(R.id.laneGuidanceRecycler).adapter?.itemCount

        assertEquals(expected, actual)
    }

    @Test
    fun `render upcoming maneuvers`() {
        val view = MapboxManeuverView(ctx)
        val upcomingManeuverState = ManeuverState.UpcomingManeuvers.Upcoming(getUpcomingManeuver())
        val expected = getUpcomingManeuver().size

        view.render(upcomingManeuverState)

        val actual =
            view.findViewById<RecyclerView>(R.id.upcomingManeuverRecycler).adapter?.itemCount

        assertEquals(expected, actual)
    }

    @Test
    fun `render upcoming maneuvers empty list`() {
        val view = MapboxManeuverView(ctx)
        val upcomingManeuverState = ManeuverState.UpcomingManeuvers.Upcoming(listOf())
        val expected = 0

        view.render(upcomingManeuverState)

        val actual =
            view.findViewById<RecyclerView>(R.id.upcomingManeuverRecycler).adapter?.itemCount

        assertEquals(expected, actual)
    }

    private fun getMockPrimaryManeuver(): PrimaryManeuver {
        val textComponentNode = Component(
            BannerComponents.TEXT,
            TextComponentNode
                .Builder()
                .text("Central Fremont")
                .abbr(null)
                .abbrPriority(null)
                .build()
        )
        return mockk {
            every { text } returns "Central Fremont"
            every { type } returns StepManeuver.TURN
            every { degrees } returns null
            every { modifier } returns ManeuverModifier.RIGHT
            every { drivingSide } returns null
            every { componentList } returns listOf(textComponentNode)
        }
    }

    private fun getMockSecondaryManeuver(): SecondaryManeuver {
        val roadShieldNumberComponent = Component(
            BannerComponents.ICON,
            RoadShieldComponentNode
                .Builder()
                .text("I-880")
                .shieldIcon(null)
                .build()
        )
        val delimiterComponentNode = Component(
            BannerComponents.DELIMITER,
            DelimiterComponentNode
                .Builder()
                .text("/")
                .build()
        )
        val textComponentNode = Component(
            BannerComponents.TEXT,
            TextComponentNode
                .Builder()
                .text("Stivers Street")
                .abbr(null)
                .abbrPriority(null)
                .build()
        )
        return mockk {
            every { text } returns "I-880/Stivers Street"
            every { type } returns StepManeuver.TURN
            every { degrees } returns null
            every { modifier } returns ManeuverModifier.RIGHT
            every { drivingSide } returns null
            every { componentList } returns listOf(
                roadShieldNumberComponent, delimiterComponentNode, textComponentNode
            )
        }
    }

    private fun getMockSubManeuver(): SubManeuver {
        val exitComponent = Component(
            BannerComponents.EXIT,
            ExitComponentNode
                .Builder()
                .text("Exit")
                .build()
        )
        val exitNumberComponent = Component(
            BannerComponents.EXIT_NUMBER,
            ExitNumberComponentNode
                .Builder()
                .text("23")
                .build()
        )
        val roadShieldNumberComponent = Component(
            BannerComponents.ICON,
            RoadShieldComponentNode
                .Builder()
                .text("I-880")
                .build()
        )
        val delimiterComponentNode = Component(
            BannerComponents.DELIMITER,
            DelimiterComponentNode
                .Builder()
                .text("/")
                .build()
        )
        val textComponentNode = Component(
            BannerComponents.TEXT,
            TextComponentNode
                .Builder()
                .text("Stivers Street")
                .abbr(null)
                .abbrPriority(null)
                .build()
        )
        return mockk {
            every { text } returns "Exit 23 I-880/Stivers Street"
            every { type } returns StepManeuver.TURN
            every { degrees } returns null
            every { modifier } returns ManeuverModifier.RIGHT
            every { drivingSide } returns null
            every { componentList } returns listOf(
                exitComponent,
                exitNumberComponent,
                roadShieldNumberComponent,
                delimiterComponentNode,
                textComponentNode
            )
        }
    }

    private fun getMockLane(): Lane {
        val laneIndicator1 = mockk<LaneIndicator> {
            every { isActive } returns true
            every { directions } returns listOf("left")
        }
        val laneIndicator2 = mockk<LaneIndicator> {
            every { isActive } returns false
            every { directions } returns listOf("right")
        }
        return mockk {
            every { activeDirection } returns ManeuverModifier.LEFT
            every { allLanes } returns listOf(laneIndicator1, laneIndicator2)
        }
    }

    private fun getUpcomingManeuver(): List<Maneuver> {
        val totalStepDistance1 = mockk<TotalManeuverDistance>()
        val primaryManeuver1 = mockk<PrimaryManeuver> {
            every { text } returns "Central Fremont"
            every { type } returns StepManeuver.TURN
            every { degrees } returns null
            every { modifier } returns ManeuverModifier.RIGHT
            every { drivingSide } returns null
            every { componentList } returns listOf(
                Component(
                    BannerComponents.TEXT,
                    TextComponentNode
                        .Builder()
                        .text("Central Fremont")
                        .abbr(null)
                        .abbrPriority(null)
                        .build()
                )
            )
        }
        val maneuver1 = Maneuver
            .Builder()
            .primary(primaryManeuver1)
            .totalManeuverDistance(totalStepDistance1)
            .secondary(null)
            .sub(null)
            .laneGuidance(null)
            .build()
        val totalStepDistance2 = mockk<TotalManeuverDistance>()
        val primaryManeuver2 = mockk<PrimaryManeuver> {
            every { text } returns "Besco Drive"
            every { type } returns StepManeuver.TURN
            every { degrees } returns null
            every { modifier } returns ManeuverModifier.LEFT
            every { drivingSide } returns null
            every { componentList } returns listOf(
                Component(
                    BannerComponents.TEXT,
                    TextComponentNode
                        .Builder()
                        .text("Besco Drive")
                        .abbr(null)
                        .abbrPriority(null)
                        .build()
                )
            )
        }
        val maneuver2 = Maneuver
            .Builder()
            .primary(primaryManeuver2)
            .totalManeuverDistance(totalStepDistance2)
            .secondary(null)
            .sub(null)
            .laneGuidance(null)
            .build()
        return listOf(maneuver1, maneuver2)
    }
}
