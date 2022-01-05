package com.mapbox.navigation.ui.maneuver.view

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.mapbox.api.directions.v5.models.ManeuverModifier
import com.mapbox.navigation.ui.maneuver.R
import com.mapbox.navigation.ui.maneuver.model.ExitNumberComponentNode
import com.mapbox.navigation.ui.maneuver.model.MapboxExitProperties
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MapboxExitTextTest {

    lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `exit text with left modifier and use properties`() {
        val view = MapboxExitText(ctx)
        val properties = MapboxExitProperties.PropertiesMutcd(
            shouldFallbackWithText = false,
            shouldFallbackWithDrawable = true
        )
        val leftDrawable = ContextCompat.getDrawable(view.context, properties.exitLeftDrawable)
        view.updateExitProperties(properties)
        val exitNumberComponent = ExitNumberComponentNode
            .Builder()
            .text("23")
            .build()
        val expectedText = exitNumberComponent.text

        view.setExit(ManeuverModifier.LEFT, exitNumberComponent)

        assertEquals(expectedText, view.text)
        assertNotNull(view.compoundDrawables[0])
    }

    @Test
    fun `exit text with right modifier and use properties`() {
        val view = MapboxExitText(ctx)
        val properties = MapboxExitProperties.PropertiesMutcd(
            shouldFallbackWithText = false,
            shouldFallbackWithDrawable = true
        )
        val rightDrawable = ContextCompat.getDrawable(view.context, properties.exitRightDrawable)
        view.updateExitProperties(properties)
        val exitNumberComponent = ExitNumberComponentNode
            .Builder()
            .text("23")
            .build()
        val expectedText = exitNumberComponent.text

        view.setExit(ManeuverModifier.RIGHT, exitNumberComponent)

        assertEquals(expectedText, view.text)
        assertNotNull(view.compoundDrawables[2])
    }

    @Test
    fun `exit text with straight modifier drawable fallback use properties`() {
        val view = MapboxExitText(ctx)
        val properties = MapboxExitProperties.PropertiesMutcd(
            shouldFallbackWithText = false,
            shouldFallbackWithDrawable = true
        )
        val fallbackDrawable = ContextCompat.getDrawable(view.context, properties.fallbackDrawable)
        view.updateExitProperties(properties)
        val exitNumberComponent = ExitNumberComponentNode
            .Builder()
            .text("23")
            .build()
        val expectedText = exitNumberComponent.text

        view.setExit(ManeuverModifier.STRAIGHT, exitNumberComponent)

        assertEquals(expectedText, view.text)
        assertNotNull(view.compoundDrawables[2])
    }

    @Test
    fun `exit text with straight modifier text fallback use properties`() {
        val view = MapboxExitText(ctx)
        val properties = MapboxExitProperties.PropertiesMutcd(
            shouldFallbackWithText = true,
            shouldFallbackWithDrawable = false
        )
        view.updateExitProperties(properties)
        val exitNumberComponent = ExitNumberComponentNode
            .Builder()
            .text("23")
            .build()
        val expectedText = "Exit ".plus(exitNumberComponent.text)

        view.setExit(ManeuverModifier.STRAIGHT, exitNumberComponent)

        assertEquals(expectedText, view.text)
    }

    @Test
    fun `exit text with straight modifier no fallback use properties`() {
        val view = MapboxExitText(ctx)
        val properties = MapboxExitProperties.PropertiesMutcd(
            shouldFallbackWithText = false,
            shouldFallbackWithDrawable = false
        )
        view.updateExitProperties(properties)
        val exitNumberComponent = ExitNumberComponentNode
            .Builder()
            .text("23")
            .build()
        val expectedText = exitNumberComponent.text

        view.setExit(ManeuverModifier.STRAIGHT, exitNumberComponent)

        assertEquals(expectedText, view.text)
    }

    @Test
    fun `exit text with left modifier and use legacy`() {
        val view = MapboxExitText(ctx)
        val background = ContextCompat.getDrawable(
            view.context,
            R.drawable.mapbox_exit_board_background
        )
        val left = ContextCompat.getDrawable(
            view.context,
            R.drawable.mapbox_ic_exit_arrow_left_mutcd
        )
        val right = ContextCompat.getDrawable(
            view.context,
            R.drawable.mapbox_ic_exit_arrow_right_mutcd
        )
        view.setExitStyle(background, left, right)
        val exitNumberComponent = ExitNumberComponentNode
            .Builder()
            .text("23")
            .build()
        val expectedText = exitNumberComponent.text

        view.setExit(ManeuverModifier.LEFT, exitNumberComponent)

        assertEquals(expectedText, view.text)
        assertNotNull(view.compoundDrawables[0])
    }

    @Test
    fun `exit text with right modifier and use legacy`() {
        val view = MapboxExitText(ctx)
        val background = ContextCompat.getDrawable(
            view.context,
            R.drawable.mapbox_exit_board_background
        )
        val left = ContextCompat.getDrawable(
            view.context,
            R.drawable.mapbox_ic_exit_arrow_left_mutcd
        )
        val right = ContextCompat.getDrawable(
            view.context,
            R.drawable.mapbox_ic_exit_arrow_right_mutcd
        )
        view.setExitStyle(background, left, right)
        val exitNumberComponent = ExitNumberComponentNode
            .Builder()
            .text("23")
            .build()
        val expectedText = exitNumberComponent.text

        view.setExit(ManeuverModifier.RIGHT, exitNumberComponent)

        assertEquals(expectedText, view.text)
        assertNotNull(view.compoundDrawables[2])
    }

    @Test
    fun `exit text with straight modifier and use legacy`() {
        val view = MapboxExitText(ctx)
        val background = ContextCompat.getDrawable(
            view.context,
            R.drawable.mapbox_exit_board_background
        )
        val left = ContextCompat.getDrawable(
            view.context,
            R.drawable.mapbox_ic_exit_arrow_left_mutcd
        )
        val right = ContextCompat.getDrawable(
            view.context,
            R.drawable.mapbox_ic_exit_arrow_right_mutcd
        )
        view.setExitStyle(background, left, right)
        val exitNumberComponent = ExitNumberComponentNode
            .Builder()
            .text("23")
            .build()
        val expectedText = exitNumberComponent.text

        view.setExit(ManeuverModifier.RIGHT, exitNumberComponent)

        assertEquals(expectedText, view.text)
        assertNotNull(view.compoundDrawables[2])
    }
}
