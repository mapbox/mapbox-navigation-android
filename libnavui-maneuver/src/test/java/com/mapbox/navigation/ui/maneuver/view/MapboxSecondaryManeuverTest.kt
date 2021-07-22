package com.mapbox.navigation.ui.maneuver.view

import android.content.Context
import android.text.SpannableString
import androidx.test.core.app.ApplicationProvider
import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.ManeuverModifier
import com.mapbox.api.directions.v5.models.StepManeuver
import com.mapbox.navigation.ui.maneuver.model.Component
import com.mapbox.navigation.ui.maneuver.model.DelimiterComponentNode
import com.mapbox.navigation.ui.maneuver.model.ExitComponentNode
import com.mapbox.navigation.ui.maneuver.model.ExitNumberComponentNode
import com.mapbox.navigation.ui.maneuver.model.RoadShieldComponentNode
import com.mapbox.navigation.ui.maneuver.model.SecondaryManeuver
import com.mapbox.navigation.ui.maneuver.model.TextComponentNode
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MapboxSecondaryManeuverTest {

    lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `render secondary maneuver text`() {
        val componentList = createComponentList()
        val state = SecondaryManeuver(
            "1234abcd",
            "Exit 23 I-880/Central",
            StepManeuver.TURN,
            null,
            ManeuverModifier.SLIGHT_LEFT,
            null,
            componentList
        )
        val expected = SpannableString("23 I-880 / Central ")
        val view = MapboxSecondaryManeuver(ctx)

        view.render(state)

        assertEquals(expected.toString(), view.text.toString())
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
