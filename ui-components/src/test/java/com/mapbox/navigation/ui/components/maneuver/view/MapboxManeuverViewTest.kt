package com.mapbox.navigation.ui.components.maneuver.view

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.text.SpannableString
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.ManeuverModifier
import com.mapbox.api.directions.v5.models.StepManeuver
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.tripdata.maneuver.model.Component
import com.mapbox.navigation.tripdata.maneuver.model.DelimiterComponentNode
import com.mapbox.navigation.tripdata.maneuver.model.ExitComponentNode
import com.mapbox.navigation.tripdata.maneuver.model.ExitNumberComponentNode
import com.mapbox.navigation.tripdata.maneuver.model.Lane
import com.mapbox.navigation.tripdata.maneuver.model.LaneIndicator
import com.mapbox.navigation.tripdata.maneuver.model.Maneuver
import com.mapbox.navigation.tripdata.maneuver.model.ManeuverError
import com.mapbox.navigation.tripdata.maneuver.model.ManeuverFactory
import com.mapbox.navigation.tripdata.maneuver.model.PrimaryManeuver
import com.mapbox.navigation.tripdata.maneuver.model.RoadShieldComponentNode
import com.mapbox.navigation.tripdata.maneuver.model.SecondaryManeuver
import com.mapbox.navigation.tripdata.maneuver.model.StepDistance
import com.mapbox.navigation.tripdata.maneuver.model.StepDistanceFactory
import com.mapbox.navigation.tripdata.maneuver.model.SubManeuver
import com.mapbox.navigation.tripdata.maneuver.model.TextComponentNode
import com.mapbox.navigation.ui.components.maneuver.model.ManeuverViewOptions
import com.mapbox.navigation.ui.components.test.R
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalMapboxNavigationAPI::class)
@RunWith(RobolectricTestRunner::class)
class MapboxManeuverViewTest {

    private lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `init attribute primary maneuver text color`() {
        val expectedTextColor = ctx.getColor(R.color.mapbox_primary_maneuver_text_color)
        val view = MapboxManeuverView(ctx)

        assertEquals(
            expectedTextColor,
            view.findViewById<TextView>(R.id.primaryManeuverText).currentTextColor,
        )
    }

    @Test
    fun `init attribute secondary maneuver text color`() {
        val expectedTextColor = ctx.getColor(R.color.mapbox_secondary_maneuver_text_color)
        val view = MapboxManeuverView(ctx)

        assertEquals(
            expectedTextColor,
            view.findViewById<TextView>(R.id.secondaryManeuverText).currentTextColor,
        )
    }

    @Test
    fun `init attribute sub maneuver text color`() {
        val expectedTextColor = ctx.getColor(R.color.mapbox_sub_maneuver_text_color)
        val view = MapboxManeuverView(ctx)

        assertEquals(
            expectedTextColor,
            view.findViewById<TextView>(R.id.subManeuverText).currentTextColor,
        )
    }

    @Test
    fun `init attribute main maneuver background color`() {
        val expectedBackgroundColor = ctx.getColor(R.color.mapbox_main_maneuver_background_color)
        val view = MapboxManeuverView(ctx, null)
        val background = view.findViewById<ConstraintLayout>(R.id.mainManeuverLayout).background

        assertEquals(expectedBackgroundColor, (background as ColorDrawable).color)
    }

    @Test
    fun `init attribute sub maneuver background color`() {
        val expectedBackgroundColor = ctx.getColor(R.color.mapbox_sub_maneuver_background_color)
        val view = MapboxManeuverView(ctx, null)
        val background = view.findViewById<ConstraintLayout>(R.id.subManeuverLayout).background

        assertEquals(expectedBackgroundColor, (background as ColorDrawable).color)
    }

    @Test
    fun `init attribute upcoming maneuver background color`() {
        val expectedBackgroundColor =
            ctx.getColor(R.color.mapbox_upcoming_maneuver_background_color)
        val view = MapboxManeuverView(ctx, null)
        val background = view.findViewById<RecyclerView>(R.id.upcomingManeuverRecycler).background

        assertEquals(expectedBackgroundColor, (background as ColorDrawable).color)
    }

    @Test
    fun `show maneuver list on click`() {
        val view = MapboxManeuverView(ctx)
        val expected = VISIBLE
        view.findViewById<RecyclerView>(R.id.upcomingManeuverRecycler).visibility = GONE

        view.performClick()
        val actual = view.findViewById<RecyclerView>(R.id.upcomingManeuverRecycler).visibility

        assertEquals(expected, actual)
    }

    @Test
    fun `change maneuver state to expanded on click`() {
        val view = MapboxManeuverView(ctx)
        val expected = MapboxManeuverViewState.EXPANDED
        view.findViewById<RecyclerView>(R.id.upcomingManeuverRecycler).visibility = GONE

        view.performClick()
        val actual = view.maneuverViewState.value

        assertEquals(expected, actual)
    }

    @Test
    fun `hide maneuver list on click`() {
        val view = MapboxManeuverView(ctx)
        val expected = GONE
        view.findViewById<RecyclerView>(R.id.upcomingManeuverRecycler).visibility = VISIBLE

        view.performClick()
        val actual = view.findViewById<RecyclerView>(R.id.upcomingManeuverRecycler).visibility

        assertEquals(expected, actual)
    }

    @Test
    fun `change maneuver state to collapsed on click`() {
        val view = MapboxManeuverView(ctx)
        val expected = MapboxManeuverViewState.COLLAPSED
        view.findViewById<RecyclerView>(R.id.upcomingManeuverRecycler).visibility = VISIBLE

        view.performClick()
        val actual = view.maneuverViewState.value

        assertEquals(expected, actual)
    }

    @Test
    fun `maneuver list on click check sub maneuver visibility`() {
        val view = MapboxManeuverView(ctx)
        val expected = GONE
        view.findViewById<MapboxManeuversList>(R.id.upcomingManeuverRecycler).visibility = VISIBLE
        view.findViewById<ConstraintLayout>(R.id.subManeuverLayout).visibility = GONE

        view.performClick()
        val actual = view.findViewById<ConstraintLayout>(R.id.subManeuverLayout).visibility

        assertEquals(expected, actual)
    }

    @Test
    fun `render step distance remaining`() {
        val view = MapboxManeuverView(ctx)
        val expected = SpannableString("13 mi")
        val totalDistance = 75.0
        val stepDistanceRemaining = 45.0
        val stepDistance = StepDistanceFactory.buildStepDistance(
            mockk {
                every { formatDistance(stepDistanceRemaining) } returns SpannableString("13 mi")
            },
            totalDistance,
            stepDistanceRemaining,
        )

        view.renderDistanceRemaining(stepDistance)
        val actual = view.findViewById<MapboxStepDistance>(R.id.stepDistance).text

        assertEquals(expected.toString(), actual.toString())
    }

    @Test
    fun `render maneuver list`() {
        val view = MapboxManeuverView(ctx)
        val subManeuver = getMockSubManeuver()
        val primary = getMockPrimaryManeuver()
        val secondaryManeuver = getMockSecondaryManeuver()
        val totalDistance = 75.0
        val stepDistanceRemaining = 45.0
        val stepDistance = StepDistanceFactory.buildStepDistance(
            mockk {
                every { formatDistance(stepDistanceRemaining) } returns SpannableString("13 mi")
            },
            totalDistance,
            stepDistanceRemaining,
        )
        val lane = null
        val point = Point.fromLngLat(-122.345234, 37.899765)
        val list: Expected<ManeuverError, List<Maneuver>> = ExpectedFactory.createValue(
            listOf(
                ManeuverFactory.buildManeuver(
                    primary,
                    stepDistance,
                    secondaryManeuver,
                    subManeuver,
                    lane,
                    point,
                ),
                ManeuverFactory.buildManeuver(
                    primary,
                    stepDistance,
                    secondaryManeuver,
                    subManeuver,
                    lane,
                    point,
                ),
            ),
        )
        val expected = list.value?.subList(1, list.value!!.size)!!.size

        view.renderManeuvers(list)
        val actual =
            view.findViewById<MapboxManeuversList>(R.id.upcomingManeuverRecycler).adapter?.itemCount

        assertEquals(expected, actual)
    }

    @Test
    fun `render maneuver list empty list`() {
        val view = MapboxManeuverView(ctx)
        val subManeuver = getMockSubManeuver()
        val primary = getMockPrimaryManeuver()
        val secondaryManeuver = getMockSecondaryManeuver()
        val totalDistance = 75.0
        val stepDistanceRemaining = 45.0
        val stepDistance = StepDistanceFactory.buildStepDistance(
            mockk {
                every { formatDistance(stepDistanceRemaining) } returns SpannableString("13 mi")
            },
            totalDistance,
            stepDistanceRemaining,
        )
        val laneGuidance = null
        val point = Point.fromLngLat(-122.345234, 37.899765)
        val list: Expected<ManeuverError, List<Maneuver>> = ExpectedFactory.createValue(
            listOf(
                ManeuverFactory.buildManeuver(
                    primary,
                    stepDistance,
                    secondaryManeuver,
                    subManeuver,
                    laneGuidance,
                    point,
                ),
            ),
        )
        val expected = list.value?.subList(1, list.value!!.size)!!.size

        view.renderManeuvers(list)
        val actual =
            view.findViewById<MapboxManeuversList>(R.id.upcomingManeuverRecycler).adapter?.itemCount

        assertEquals(expected, actual)
    }

    @Test
    fun `maneuver list visible query visibility`() {
        val view = MapboxManeuverView(ctx)
        val mockVisibility = VISIBLE
        val expected = VISIBLE

        view.updateUpcomingManeuversVisibility(mockVisibility)
        val actual = view.findViewById<RecyclerView>(R.id.upcomingManeuverRecycler).visibility

        assertEquals(expected, actual)
    }

    @Test
    fun `maneuver list gone query visibility`() {
        val view = MapboxManeuverView(ctx)
        val mockVisibility = GONE
        val expected = GONE

        view.updateUpcomingManeuversVisibility(mockVisibility)
        val actual = view.findViewById<RecyclerView>(R.id.upcomingManeuverRecycler).visibility

        assertEquals(expected, actual)
    }

    @Test
    fun `render add lanes`() {
        val lane = getMockLane()
        val view = MapboxManeuverView(ctx)
        val expected = lane.allLanes.size

        view.renderAddLanes(lane)
        val actual = view.findViewById<RecyclerView>(R.id.laneGuidanceRecycler).adapter?.itemCount

        assertEquals(expected, actual)
    }

    @Test
    fun `render remove lanes`() {
        val lane = getMockLane()
        val view = MapboxManeuverView(ctx)
        val expected = 0

        view.renderAddLanes(lane)
        view.renderRemoveLanes()
        val actual = view.findViewById<RecyclerView>(R.id.laneGuidanceRecycler).adapter?.itemCount

        assertEquals(expected, actual)
    }

    @Test
    fun `render primary maneuver text empty list`() {
        val primaryManeuver = mockk<PrimaryManeuver> {
            every { text } returns "Central Fremont"
            every { type } returns StepManeuver.TURN
            every { degrees } returns null
            every { modifier } returns ManeuverModifier.RIGHT
            every { drivingSide } returns null
            every { componentList } returns listOf()
        }
        val view = MapboxManeuverView(ctx)
        val expected = ""

        view.renderPrimary(primaryManeuver, null)
        val actual = view.findViewById<MapboxPrimaryManeuver>(R.id.primaryManeuverText).text

        assertEquals(expected, actual)
    }

    @Test
    fun `render primary maneuver text`() {
        val primaryManeuver = getMockPrimaryManeuver()
        val view = MapboxManeuverView(ctx)
        val expected = "Central Fremont "

        view.renderPrimary(primaryManeuver, null)
        val actual = view.findViewById<MapboxPrimaryManeuver>(R.id.primaryManeuverText).text

        assertEquals(expected, actual.toString())
    }

    @Test
    fun `render primary maneuver gone query visibility`() {
        val view = MapboxManeuverView(ctx)
        val expected = GONE

        view.updatePrimaryManeuverTextVisibility(GONE)
        val actual =
            view.findViewById<MapboxPrimaryManeuver>(R.id.primaryManeuverText).visibility

        assertEquals(expected, actual)
    }

    @Test
    fun `render primary maneuver visible query visibility`() {
        val view = MapboxManeuverView(ctx)
        val expected = VISIBLE

        view.updatePrimaryManeuverTextVisibility(VISIBLE)
        val actual =
            view.findViewById<MapboxPrimaryManeuver>(R.id.primaryManeuverText).visibility

        assertEquals(expected, actual)
    }

    @Test
    fun `render secondary maneuver text empty list`() {
        val secondaryManeuver = mockk<SecondaryManeuver> {
            every { text } returns "Stivers Street"
            every { type } returns StepManeuver.TURN
            every { degrees } returns null
            every { modifier } returns ManeuverModifier.RIGHT
            every { drivingSide } returns null
            every { componentList } returns listOf()
        }
        val view = MapboxManeuverView(ctx)
        val expected = ""

        view.renderSecondary(secondaryManeuver, null)
        val actual = view.findViewById<MapboxSecondaryManeuver>(R.id.secondaryManeuverText).text

        assertEquals(expected, actual)
    }

    @Test
    fun `render secondary maneuver text`() {
        val secondaryManeuver = getMockSecondaryManeuver()
        val view = MapboxManeuverView(ctx)
        val expected = "I-880 / Stivers Street "

        view.renderSecondary(secondaryManeuver, null)
        val actual = view.findViewById<MapboxSecondaryManeuver>(R.id.secondaryManeuverText).text

        assertEquals(expected, actual.toString())
    }

    @Test
    fun `render secondary maneuver gone query visibility`() {
        val view = MapboxManeuverView(ctx)
        val expected = GONE

        view.updateSecondaryManeuverVisibility(GONE)
        val actual =
            view.findViewById<MapboxSecondaryManeuver>(R.id.secondaryManeuverText).visibility

        assertEquals(expected, actual)
    }

    @Test
    fun `render secondary maneuver gone update primary maneuver constraints`() {
        val view = MapboxManeuverView(ctx)
        val expectedTopToTop = ConstraintLayout.LayoutParams.PARENT_ID
        val expectedBottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID

        view.updateSecondaryManeuverVisibility(GONE)
        val primaryManeuverView = view.findViewById<MapboxPrimaryManeuver>(R.id.primaryManeuverText)
        val params = primaryManeuverView.layoutParams as ConstraintLayout.LayoutParams
        val actualTopToTop = params.topToTop
        val actualBottomToBottom = params.bottomToBottom

        assertEquals(expectedTopToTop, actualTopToTop)
        assertEquals(expectedBottomToBottom, actualBottomToBottom)
    }

    @Test
    fun `render secondary maneuver invisible query visibility`() {
        val view = MapboxManeuverView(ctx)
        val expected = INVISIBLE

        view.updateSecondaryManeuverVisibility(INVISIBLE)
        val actual =
            view.findViewById<MapboxSecondaryManeuver>(R.id.secondaryManeuverText).visibility

        assertEquals(expected, actual)
    }

    @Test
    fun `render secondary maneuver invisible update primary maneuver constraints`() {
        val view = MapboxManeuverView(ctx)
        val expectedTopToTop = ConstraintLayout.LayoutParams.PARENT_ID
        val expectedBottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID

        view.updateSecondaryManeuverVisibility(INVISIBLE)
        val primaryManeuverView = view.findViewById<MapboxPrimaryManeuver>(R.id.primaryManeuverText)
        val params = primaryManeuverView.layoutParams as ConstraintLayout.LayoutParams
        val actualTopToTop = params.topToTop
        val actualBottomToBottom = params.bottomToBottom

        assertEquals(expectedTopToTop, actualTopToTop)
        assertEquals(expectedBottomToBottom, actualBottomToBottom)
    }

    @Test
    fun `render secondary maneuver visible query visibility`() {
        val view = MapboxManeuverView(ctx)
        val expected = VISIBLE

        view.updateSecondaryManeuverVisibility(VISIBLE)
        val actual =
            view.findViewById<MapboxSecondaryManeuver>(R.id.secondaryManeuverText).visibility

        assertEquals(expected, actual)
    }

    @Test
    fun `render secondary maneuver visible update primary maneuver constraints`() {
        val view = MapboxManeuverView(ctx)
        val expectedTopToTop = ConstraintLayout.LayoutParams.UNSET
        val expectedBottomToTop =
            view.findViewById<MapboxSecondaryManeuver>(R.id.secondaryManeuverText).id
        val expectedBottomToBottom = ConstraintLayout.LayoutParams.UNSET

        view.updateSecondaryManeuverVisibility(VISIBLE)
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
    fun `render sub maneuver text empty list`() {
        val subManeuver = mockk<SubManeuver> {
            every { text } returns "Stivers Street"
            every { type } returns StepManeuver.TURN
            every { degrees } returns null
            every { modifier } returns ManeuverModifier.RIGHT
            every { drivingSide } returns null
            every { componentList } returns listOf()
        }
        val view = MapboxManeuverView(ctx)
        val expected = ""

        view.renderSub(subManeuver, null)
        val actual = view.findViewById<MapboxSubManeuver>(R.id.subManeuverText).text

        assertEquals(expected, actual.toString())
    }

    @Test
    fun `render sub maneuver text`() {
        val subManeuver = getMockSubManeuver()
        val view = MapboxManeuverView(ctx)
        val expected = "23 I-880 / Stivers Street "

        view.renderSub(subManeuver, null)
        val actual = view.findViewById<MapboxSubManeuver>(R.id.subManeuverText).text

        assertEquals(expected, actual.toString())
    }

    @Test
    fun `render sub maneuver gone query visibility`() {
        val view = MapboxManeuverView(ctx)
        val expected = GONE

        view.updateSubManeuverViewVisibility(GONE)
        val actual =
            view.findViewById<ConstraintLayout>(R.id.subManeuverLayout).visibility

        assertEquals(expected, actual)
    }

    @Test
    fun `render sub maneuver visible query visibility`() {
        val view = MapboxManeuverView(ctx)
        val expected = VISIBLE

        view.updateSubManeuverViewVisibility(VISIBLE)
        val actual =
            view.findViewById<ConstraintLayout>(R.id.subManeuverLayout).visibility

        assertEquals(expected, actual)
    }

    @Test
    fun `render maneuver with primary`() {
        val view = MapboxManeuverView(ctx)
        val subManeuver = getMockSubManeuver()
        val primary = getMockPrimaryManeuver()
        val secondaryManeuver = getMockSecondaryManeuver()
        val totalDistance = 75.0
        val stepDistanceRemaining = 45.0
        val stepDistance = StepDistanceFactory.buildStepDistance(
            mockk {
                every { formatDistance(stepDistanceRemaining) } returns SpannableString("13 mi")
            },
            totalDistance,
            stepDistanceRemaining,
        )
        val laneGuidance = null

        val point = Point.fromLngLat(-122.345234, 37.899765)
        val mockExpected: Expected<ManeuverError, List<Maneuver>> = ExpectedFactory.createValue(
            listOf(
                ManeuverFactory.buildManeuver(
                    primary,
                    stepDistance,
                    secondaryManeuver,
                    subManeuver,
                    laneGuidance,
                    point,
                ),
            ),
        )

        view.renderManeuvers(mockExpected)

        assertEquals(
            primary.text.plus(" "),
            view.findViewById<MapboxPrimaryManeuver>(R.id.primaryManeuverText).text.toString(),
        )
    }

    @Test
    fun `update maneuver view options updates turn icon style for upcoming maneuvers`() {
        val turnIconManeuver = R.style.MapboxTestStyleTurnIconManeuver
        val laneGuidanceTurnIconManeuver = R.style.MapboxStylePrimaryManeuver
        val options = ManeuverViewOptions.Builder()
            .turnIconManeuver(turnIconManeuver)
            .laneGuidanceTurnIconManeuver(laneGuidanceTurnIconManeuver)
            .build()
        val view = MapboxManeuverView(ctx)
        val rvParent = RecyclerView(ctx)
        rvParent.layoutManager = LinearLayoutManager(ctx)
        val viewHolder: MapboxUpcomingManeuverAdapter.MapboxUpcomingManeuverViewHolder =
            view.getUpcomingManeuverAdapter().onCreateViewHolder(rvParent, 0)

        view.updateManeuverViewOptions(options)

        viewHolder.bindUpcomingManeuver(getMockUpcomingManeuver())
        assertEquals(
            turnIconManeuver,
            viewHolder.viewBinding.maneuverIcon.getTurnIconTheme().themeResId,
        )
    }

    private fun getMockPrimaryManeuver(): PrimaryManeuver {
        val textComponentNode = Component(
            BannerComponents.TEXT,
            TextComponentNode
                .Builder()
                .text("Central Fremont")
                .abbr(null)
                .abbrPriority(null)
                .build(),
        )
        return mockk {
            every { id } returns "1234abcd"
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
                .build(),
        )
        val delimiterComponentNode = Component(
            BannerComponents.DELIMITER,
            DelimiterComponentNode
                .Builder()
                .text("/")
                .build(),
        )
        val textComponentNode = Component(
            BannerComponents.TEXT,
            TextComponentNode
                .Builder()
                .text("Stivers Street")
                .abbr(null)
                .abbrPriority(null)
                .build(),
        )
        return mockk {
            every { id } returns "1a2b3c4d"
            every { text } returns "I-880/Stivers Street"
            every { type } returns StepManeuver.TURN
            every { degrees } returns null
            every { modifier } returns ManeuverModifier.RIGHT
            every { drivingSide } returns null
            every { componentList } returns listOf(
                roadShieldNumberComponent,
                delimiterComponentNode,
                textComponentNode,
            )
        }
    }

    private fun getMockSubManeuver(): SubManeuver {
        val exitComponent = Component(
            BannerComponents.EXIT,
            ExitComponentNode
                .Builder()
                .text("Exit")
                .build(),
        )
        val exitNumberComponent = Component(
            BannerComponents.EXIT_NUMBER,
            ExitNumberComponentNode
                .Builder()
                .text("23")
                .build(),
        )
        val roadShieldNumberComponent = Component(
            BannerComponents.ICON,
            RoadShieldComponentNode
                .Builder()
                .text("I-880")
                .build(),
        )
        val delimiterComponentNode = Component(
            BannerComponents.DELIMITER,
            DelimiterComponentNode
                .Builder()
                .text("/")
                .build(),
        )
        val textComponentNode = Component(
            BannerComponents.TEXT,
            TextComponentNode
                .Builder()
                .text("Stivers Street")
                .abbr(null)
                .abbrPriority(null)
                .build(),
        )
        return mockk {
            every { id } returns "abcd1234"
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
                textComponentNode,
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
            every { allLanes } returns listOf(laneIndicator1, laneIndicator2)
        }
    }

    private fun getMockUpcomingManeuver(): Maneuver {
        val stepDistance = mockk<StepDistance> {
            every { totalDistance } returns 75.0
            every { distanceRemaining } returns 45.0
            every { distanceFormatter.formatDistance(any()) } returns SpannableString("200 ft")
        }
        val primaryManeuver = mockk<PrimaryManeuver> {
            every { id } returns "1234abcd"
            every { text } returns "Upcoming maneuver"
            every { type } returns StepManeuver.TURN
            every { degrees } returns null
            every { modifier } returns ManeuverModifier.LEFT
            every { drivingSide } returns null
            every { componentList } returns listOf(
                Component(
                    BannerComponents.TEXT,
                    TextComponentNode
                        .Builder()
                        .text("Upcoming maneuver")
                        .abbr(null)
                        .abbrPriority(null)
                        .build(),
                ),
            )
        }
        val secondaryManeuver = mockk<SecondaryManeuver> {
            every { id } returns "abcd1234"
            every { text } returns "Davis Street"
            every { type } returns StepManeuver.TURN
            every { degrees } returns null
            every { modifier } returns ManeuverModifier.SLIGHT_LEFT
            every { drivingSide } returns null
            every { componentList } returns listOf(
                Component(
                    BannerComponents.TEXT,
                    TextComponentNode
                        .Builder()
                        .text("Davis Street")
                        .abbr(null)
                        .abbrPriority(null)
                        .build(),
                ),
            )
        }
        return ManeuverFactory.buildManeuver(
            primaryManeuver,
            stepDistance,
            secondaryManeuver,
            null,
            null,
            Point.fromLngLat(-122.345234, 37.899765),
        )
    }
}
