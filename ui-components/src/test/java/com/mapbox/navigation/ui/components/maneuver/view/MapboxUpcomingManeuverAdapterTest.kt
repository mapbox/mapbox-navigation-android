package com.mapbox.navigation.ui.components.maneuver.view

import android.content.Context
import android.text.SpannableString
import androidx.appcompat.view.ContextThemeWrapper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.ManeuverModifier
import com.mapbox.api.directions.v5.models.StepManeuver
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.core.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.tripdata.maneuver.model.Component
import com.mapbox.navigation.tripdata.maneuver.model.Maneuver
import com.mapbox.navigation.tripdata.maneuver.model.ManeuverFactory
import com.mapbox.navigation.tripdata.maneuver.model.PrimaryManeuver
import com.mapbox.navigation.tripdata.maneuver.model.SecondaryManeuver
import com.mapbox.navigation.tripdata.maneuver.model.StepDistance
import com.mapbox.navigation.tripdata.maneuver.model.TextComponentNode
import com.mapbox.navigation.ui.components.maneuver.model.ManeuverExitOptions
import com.mapbox.navigation.ui.components.maneuver.model.ManeuverPrimaryOptions
import com.mapbox.navigation.ui.components.maneuver.model.ManeuverSecondaryOptions
import com.mapbox.navigation.ui.components.maneuver.model.ManeuverViewOptions
import com.mapbox.navigation.ui.components.test.R
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@OptIn(ExperimentalMapboxNavigationAPI::class)
@RunWith(RobolectricTestRunner::class)
class MapboxUpcomingManeuverAdapterTest {

    lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `add upcoming maneuvers empty query item count`() {
        val adapter = MapboxUpcomingManeuverAdapter(ctx)

        adapter.addUpcomingManeuvers(listOf())

        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `add upcoming maneuvers query item count`() {
        val upcomingManeuverList = getUpcomingManeuver("Besco Drive")
        val adapter = MapboxUpcomingManeuverAdapter(ctx)
        val expected = upcomingManeuverList.size

        adapter.addUpcomingManeuvers(upcomingManeuverList)
        val actual = adapter.itemCount

        assertEquals(expected, actual)
    }

    @Test
    fun `add lanes query primary maneuver text`() {
        val upcomingManeuverList = getUpcomingManeuver("Besco Drive")
        val adapter = MapboxUpcomingManeuverAdapter(ctx)
        val rvParent = RecyclerView(ctx)
        rvParent.layoutManager = LinearLayoutManager(ctx)
        val viewHolder: MapboxUpcomingManeuverAdapter.MapboxUpcomingManeuverViewHolder =
            adapter.onCreateViewHolder(rvParent, 0)
        val expected = upcomingManeuverList[0].primary.text.plus(" ")

        adapter.addUpcomingManeuvers(upcomingManeuverList)
        adapter.onBindViewHolder(viewHolder, 0)
        val actual = viewHolder.viewBinding.primaryManeuverText.text

        assertEquals(expected, actual.toString())
    }

    @Test
    fun `add lanes query secondary maneuver text`() {
        val upcomingManeuverList = getUpcomingManeuver("Besco Drive")
        val adapter = MapboxUpcomingManeuverAdapter(ctx)
        val rvParent = RecyclerView(ctx)
        rvParent.layoutManager = LinearLayoutManager(ctx)
        val viewHolder: MapboxUpcomingManeuverAdapter.MapboxUpcomingManeuverViewHolder =
            adapter.onCreateViewHolder(rvParent, 0)
        val expected = upcomingManeuverList[0].secondary?.text.plus(" ")

        adapter.addUpcomingManeuvers(upcomingManeuverList)
        adapter.onBindViewHolder(viewHolder, 0)
        val actual = viewHolder.viewBinding.secondaryManeuverText.text

        assertEquals(expected, actual.toString())
    }

    @Test
    fun `add lanes query total step distance`() {
        val distanceFormatter = MapboxDistanceFormatter(
            DistanceFormatterOptions.Builder(ctx).build(),
        )
        val upcomingManeuverList = getUpcomingManeuver("Besco Drive")
        val adapter = MapboxUpcomingManeuverAdapter(ctx)
        val rvParent = RecyclerView(ctx)
        rvParent.layoutManager = LinearLayoutManager(ctx)
        val viewHolder: MapboxUpcomingManeuverAdapter.MapboxUpcomingManeuverViewHolder =
            adapter.onCreateViewHolder(rvParent, 0)
        val expected = distanceFormatter.formatDistance(
            upcomingManeuverList[0].stepDistance.totalDistance,
        )

        adapter.addUpcomingManeuvers(upcomingManeuverList)
        adapter.onBindViewHolder(viewHolder, 0)
        val actual = viewHolder.viewBinding.stepDistance.text

        assertEquals(expected.toString(), actual.toString())
    }

    @Test
    fun `update turn icon style`() {
        val style = ContextThemeWrapper(ctx, R.style.MapboxTestStyleTurnIconManeuver)
        val adapter = MapboxUpcomingManeuverAdapter(ctx)
        adapter.updateUpcomingManeuverIconStyle(style)
        val rvParent = RecyclerView(ctx)
        rvParent.layoutManager = LinearLayoutManager(ctx)
        val viewHolder: MapboxUpcomingManeuverAdapter.MapboxUpcomingManeuverViewHolder =
            adapter.onCreateViewHolder(rvParent, 0)

        viewHolder.bindUpcomingManeuver(getUpcomingManeuver("My maneuver").first())

        assertEquals(style, viewHolder.viewBinding.maneuverIcon.getTurnIconTheme())
    }

    @Test
    fun updateOptions() {
        val primaryManeuverOptions = ManeuverPrimaryOptions.Builder()
            .textAppearance(R.style.CustomPrimaryTextAppearance)
            .exitOptions(
                ManeuverExitOptions.Builder()
                    .textAppearance(R.style.CustomPrimaryExitTextAppearance)
                    .build(),
            )
            .build()
        val secondaryManeuverOptions = ManeuverSecondaryOptions.Builder()
            .textAppearance(R.style.CustomSecondaryTextAppearance)
            .exitOptions(
                ManeuverExitOptions.Builder()
                    .textAppearance(R.style.CustomSecondaryExitTextAppearance)
                    .build(),
            )
            .build()
        val options = ManeuverViewOptions.Builder()
            .primaryManeuverOptions(primaryManeuverOptions)
            .secondaryManeuverOptions(secondaryManeuverOptions)
            .stepDistanceTextAppearance(R.style.CustomStepDistanceTextAppearance)
            .build()
        val adapter = MapboxUpcomingManeuverAdapter(ctx)
        adapter.updateManeuverViewOptions(options)
        val rvParent = RecyclerView(ctx)
        rvParent.layoutManager = LinearLayoutManager(ctx)
        val viewHolder: MapboxUpcomingManeuverAdapter.MapboxUpcomingManeuverViewHolder =
            adapter.onCreateViewHolder(rvParent, 0)

        viewHolder.bindUpcomingManeuver(getUpcomingManeuver("My maneuver").first())

        assertEquals(
            primaryManeuverOptions,
            viewHolder.viewBinding.primaryManeuverText.getOptions(),
        )
        assertEquals(
            secondaryManeuverOptions,
            viewHolder.viewBinding.secondaryManeuverText.getOptions(),
        )
        // check text appearance
        assertEquals(
            R.style.CustomPrimaryTextAppearance,
            Shadows.shadowOf(viewHolder.viewBinding.primaryManeuverText).textAppearanceId,
        )
        assertEquals(
            R.style.CustomSecondaryTextAppearance,
            Shadows.shadowOf(viewHolder.viewBinding.secondaryManeuverText).textAppearanceId,
        )
        assertEquals(
            R.style.CustomStepDistanceTextAppearance,
            Shadows.shadowOf(viewHolder.viewBinding.stepDistance).textAppearanceId,
        )
    }

    private fun getUpcomingManeuver(primaryText: String): List<Maneuver> {
        val stepDistance = mockk<StepDistance> {
            every { totalDistance } returns 75.0
            every { distanceRemaining } returns 45.0
            every { distanceFormatter.formatDistance(any()) } returns SpannableString("200 ft")
        }
        val primaryManeuver = mockk<PrimaryManeuver> {
            every { id } returns "1234abcd"
            every { text } returns primaryText
            every { type } returns StepManeuver.TURN
            every { degrees } returns null
            every { modifier } returns ManeuverModifier.LEFT
            every { drivingSide } returns null
            every { componentList } returns listOf(
                Component(
                    BannerComponents.TEXT,
                    TextComponentNode
                        .Builder()
                        .text(primaryText)
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
        val maneuver = ManeuverFactory.buildManeuver(
            primaryManeuver,
            stepDistance,
            secondaryManeuver,
            null,
            null,
            Point.fromLngLat(-122.345234, 37.899765),
        )
        return listOf(maneuver)
    }
}
