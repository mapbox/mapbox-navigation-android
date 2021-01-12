package com.mapbox.navigation.ui.maneuver.view

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.ManeuverModifier
import com.mapbox.api.directions.v5.models.StepManeuver
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.core.internal.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.ui.base.model.maneuver.Component
import com.mapbox.navigation.ui.base.model.maneuver.Maneuver
import com.mapbox.navigation.ui.base.model.maneuver.PrimaryManeuver
import com.mapbox.navigation.ui.base.model.maneuver.SecondaryManeuver
import com.mapbox.navigation.ui.base.model.maneuver.TextComponentNode
import com.mapbox.navigation.ui.base.model.maneuver.TotalManeuverDistance
import io.mockk.every
import io.mockk.mockk
import kotlinx.android.synthetic.main.mapbox_layout_main_maneuver_view.view.*
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

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
        val upcomingManeuverList = getUpcomingManeuver()
        val adapter = MapboxUpcomingManeuverAdapter(ctx)
        val expected = upcomingManeuverList.size

        adapter.addUpcomingManeuvers(upcomingManeuverList)
        val actual = adapter.itemCount

        assertEquals(expected, actual)
    }

    @Test
    fun `add lanes query primary maneuver text`() {
        val upcomingManeuverList = getUpcomingManeuver()
        val adapter = MapboxUpcomingManeuverAdapter(ctx)
        val rvParent = RecyclerView(ctx)
        rvParent.layoutManager = LinearLayoutManager(ctx)
        val viewHolder: MapboxUpcomingManeuverAdapter.MapboxUpcomingManeuverViewHolder =
            adapter.onCreateViewHolder(rvParent, 0)
        val expected = upcomingManeuverList[0].primary.text.plus(" ")

        adapter.addUpcomingManeuvers(upcomingManeuverList)
        adapter.onBindViewHolder(viewHolder, 0)
        val actual = viewHolder.view.primaryManeuverText.text

        assertEquals(expected, actual.toString())
    }

    @Test
    fun `add lanes query secondary maneuver text`() {
        val upcomingManeuverList = getUpcomingManeuver()
        val adapter = MapboxUpcomingManeuverAdapter(ctx)
        val rvParent = RecyclerView(ctx)
        rvParent.layoutManager = LinearLayoutManager(ctx)
        val viewHolder: MapboxUpcomingManeuverAdapter.MapboxUpcomingManeuverViewHolder =
            adapter.onCreateViewHolder(rvParent, 0)
        val expected = upcomingManeuverList[0].secondary?.text.plus(" ")

        adapter.addUpcomingManeuvers(upcomingManeuverList)
        adapter.onBindViewHolder(viewHolder, 0)
        val actual = viewHolder.view.secondaryManeuverText.text

        assertEquals(expected, actual.toString())
    }

    @Test
    fun `add lanes query total step distance`() {
        val distanceFormatter = MapboxDistanceFormatter(
            DistanceFormatterOptions.Builder(ctx).build()
        )
        val upcomingManeuverList = getUpcomingManeuver()
        val adapter = MapboxUpcomingManeuverAdapter(ctx)
        val rvParent = RecyclerView(ctx)
        rvParent.layoutManager = LinearLayoutManager(ctx)
        val viewHolder: MapboxUpcomingManeuverAdapter.MapboxUpcomingManeuverViewHolder =
            adapter.onCreateViewHolder(rvParent, 0)
        val expected = distanceFormatter.formatDistance(
            upcomingManeuverList[0].totalManeuverDistance.totalDistance
        )

        adapter.addUpcomingManeuvers(upcomingManeuverList)
        adapter.onBindViewHolder(viewHolder, 0)
        val actual = viewHolder.view.stepDistance.text

        assertEquals(expected.toString(), actual.toString())
    }

    private fun getUpcomingManeuver(): List<Maneuver> {
        val totalStepDistance = mockk<TotalManeuverDistance>() {
            every { totalDistance } returns 32.0
        }
        val primaryManeuver = mockk<PrimaryManeuver> {
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
        val secondaryManeuver = mockk<SecondaryManeuver> {
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
                        .build()
                )
            )
        }
        val maneuver = Maneuver
            .Builder()
            .primary(primaryManeuver)
            .totalManeuverDistance(totalStepDistance)
            .secondary(secondaryManeuver)
            .sub(null)
            .laneGuidance(null)
            .build()
        return listOf(maneuver)
    }
}
