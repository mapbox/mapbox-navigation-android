package com.mapbox.navigation.ui.maneuver.view

import android.content.Context
import android.text.SpannableString
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.ManeuverModifier
import com.mapbox.api.directions.v5.models.StepManeuver
import com.mapbox.navigation.ui.base.model.maneuver.Component
import com.mapbox.navigation.ui.base.model.maneuver.DelimiterComponentNode
import com.mapbox.navigation.ui.base.model.maneuver.ExitComponentNode
import com.mapbox.navigation.ui.base.model.maneuver.ExitNumberComponentNode
import com.mapbox.navigation.ui.base.model.maneuver.ManeuverState
import com.mapbox.navigation.ui.base.model.maneuver.RoadShieldComponentNode
import com.mapbox.navigation.ui.base.model.maneuver.SubManeuver
import com.mapbox.navigation.ui.base.model.maneuver.TextComponentNode
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MapboxSubManeuverTest {

    lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `render sub maneuver text`() {
        val componentList = createComponentList()
        val state = ManeuverState.ManeuverSub.Instruction(
            SubManeuver
                .Builder()
                .text("Exit 23 I-880/Central")
                .type(StepManeuver.TURN)
                .degrees(null)
                .modifier(ManeuverModifier.SLIGHT_LEFT)
                .drivingSide(null)
                .componentList(componentList)
                .build()
        )
        val expected = SpannableString("23 I-880 / Central ")
        val view = MapboxSubManeuver(ctx)

        view.render(state)

        assertEquals(expected.toString(), view.text.toString())
    }

    @Test
    fun `render sub view hide`() {
        val view = MapboxSubManeuver(ctx)
        val state = ManeuverState.ManeuverSub.Hide
        val expected = View.GONE

        view.render(state)

        assertEquals(expected, view.visibility)
    }

    @Test
    fun `render sub view show`() {
        val view = MapboxSubManeuver(ctx)
        val state = ManeuverState.ManeuverSub.Show
        val expected = View.VISIBLE

        view.render(state)

        assertEquals(expected, view.visibility)
    }

    private fun createComponentList(): List<Component> {
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
                .text("Central")
                .abbr(null)
                .abbrPriority(null)
                .build()
        )
        return listOf(
            exitComponent,
            exitNumberComponent,
            roadShieldNumberComponent,
            delimiterComponentNode,
            textComponentNode
        )
    }
}
