package com.mapbox.navigation.ui.components.maneuver.view

import android.content.Context
import android.text.SpannableString
import androidx.test.core.app.ApplicationProvider
import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.ManeuverModifier
import com.mapbox.api.directions.v5.models.StepManeuver
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.tripdata.maneuver.model.Component
import com.mapbox.navigation.tripdata.maneuver.model.DelimiterComponentNode
import com.mapbox.navigation.tripdata.maneuver.model.ExitComponentNode
import com.mapbox.navigation.tripdata.maneuver.model.ExitNumberComponentNode
import com.mapbox.navigation.tripdata.maneuver.model.PrimaryManeuverFactory
import com.mapbox.navigation.tripdata.maneuver.model.RoadShieldComponentNode
import com.mapbox.navigation.tripdata.maneuver.model.TextComponentNode
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalMapboxNavigationAPI::class)
@RunWith(RobolectricTestRunner::class)
class MapboxPrimaryManeuverTest {

    lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `render primary maneuver text`() {
        val componentList = createComponentList()
        val state = PrimaryManeuverFactory.buildPrimaryManeuver(
            "1234abcd",
            "Exit 23 I-880/Central",
            StepManeuver.TURN,
            null,
            ManeuverModifier.SLIGHT_LEFT,
            null,
            componentList,
        )
        val expected = SpannableString("23 I-880 / Central ")
        val view = MapboxPrimaryManeuver(ctx)

        view.renderManeuver(state, null)

        assertEquals(expected.toString(), view.text.toString())
    }

    private fun createComponentList(): List<Component> {
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
                .text("Central")
                .abbr(null)
                .abbrPriority(null)
                .build(),
        )
        return listOf(
            exitComponent,
            exitNumberComponent,
            roadShieldNumberComponent,
            delimiterComponentNode,
            textComponentNode,
        )
    }
}
