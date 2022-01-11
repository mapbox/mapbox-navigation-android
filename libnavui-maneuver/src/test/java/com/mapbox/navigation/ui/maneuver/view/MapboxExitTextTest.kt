package com.mapbox.navigation.ui.maneuver.view

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.mapbox.api.directions.v5.models.ManeuverModifier
import com.mapbox.navigation.ui.maneuver.R
import com.mapbox.navigation.ui.maneuver.model.ExitNumberComponentNode
import com.mapbox.navigation.ui.maneuver.model.MapboxExitProperties
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun `init font padding`() {
        val view = MapboxExitText(ctx)

        assertFalse(view.includeFontPadding)
    }

    @Test
    fun `init text color`() {
        val expectedTextColor = ctx.getColor(R.color.mapbox_exit_text_color)

        val view = MapboxExitText(ctx)

        assertEquals(expectedTextColor, view.currentTextColor)
    }

    @Test
    fun `init text size`() {
        val expectedTextSize = 10 * ctx.resources.displayMetrics.scaledDensity

        val view = MapboxExitText(ctx)

        assertEquals(expectedTextSize, view.textSize)
    }

    @Test
    fun `init text typeface`() {
        val expectedTypeface = Typeface.BOLD

        val view = MapboxExitText(ctx)

        assertEquals(expectedTypeface, view.typeface.style)
    }

    @Test
    fun `exit text with left modifier and use properties`() {
        val view = MapboxExitText(ctx)
        val properties = MapboxExitProperties.PropertiesMutcd(
            shouldFallbackWithText = false,
            shouldFallbackWithDrawable = true
        )
        val leftDrawable = ContextCompat.getDrawable(view.context, properties.exitLeftDrawable)
        view.setExitProperties(properties)
        val exitNumberComponent = ExitNumberComponentNode
            .Builder()
            .text("23")
            .build()
        val expectedText = exitNumberComponent.text

        view.setExit(ManeuverModifier.LEFT, exitNumberComponent)

        assertEquals(expectedText, view.text)
        assertNotNull(view.compoundDrawables[0])
        assertEquals(view.compoundDrawables[0].constantState, leftDrawable?.constantState)
    }

    @Test
    fun `exit text with right modifier and use properties`() {
        val view = MapboxExitText(ctx)
        val properties = MapboxExitProperties.PropertiesMutcd(
            shouldFallbackWithText = false,
            shouldFallbackWithDrawable = true
        )
        val rightDrawable = ContextCompat.getDrawable(view.context, properties.exitRightDrawable)
        view.setExitProperties(properties)
        val exitNumberComponent = ExitNumberComponentNode
            .Builder()
            .text("23")
            .build()
        val expectedText = exitNumberComponent.text

        view.setExit(ManeuverModifier.RIGHT, exitNumberComponent)

        assertEquals(expectedText, view.text)
        assertNotNull(view.compoundDrawables[2])
        assertEquals(view.compoundDrawables[2].constantState, rightDrawable?.constantState)
    }

    @Test
    fun `exit text with straight modifier drawable fallback use properties`() {
        val view = MapboxExitText(ctx)
        val properties = MapboxExitProperties.PropertiesMutcd(
            shouldFallbackWithText = false,
            shouldFallbackWithDrawable = true
        )
        val fallbackDrawable = ContextCompat.getDrawable(view.context, properties.fallbackDrawable)
        view.setExitProperties(properties)
        val exitNumberComponent = ExitNumberComponentNode
            .Builder()
            .text("23")
            .build()
        val expectedText = exitNumberComponent.text

        view.setExit(ManeuverModifier.STRAIGHT, exitNumberComponent)

        assertEquals(expectedText, view.text)
        assertNotNull(view.compoundDrawables[2])
        assertEquals(view.compoundDrawables[2].constantState, fallbackDrawable?.constantState)
    }

    @Test
    fun `exit text with straight modifier text fallback use properties`() {
        val view = MapboxExitText(ctx)
        val properties = MapboxExitProperties.PropertiesMutcd(
            shouldFallbackWithText = true,
            shouldFallbackWithDrawable = false
        )
        view.setExitProperties(properties)
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
        view.setExitProperties(properties)
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
            R.drawable.mapbox_ic_exit_arrow_left
        )
        val right = ContextCompat.getDrawable(
            view.context,
            R.drawable.mapbox_ic_exit_arrow_right
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
        assertEquals(view.compoundDrawables[0].constantState, left?.constantState)
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
            R.drawable.mapbox_ic_exit_arrow_left
        )
        val right = ContextCompat.getDrawable(
            view.context,
            R.drawable.mapbox_ic_exit_arrow_right
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
        assertEquals(view.compoundDrawables[2].constantState, right?.constantState)
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
            R.drawable.mapbox_ic_exit_arrow_left
        )
        val right = ContextCompat.getDrawable(
            view.context,
            R.drawable.mapbox_ic_exit_arrow_right
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
        assertEquals(view.compoundDrawables[2].constantState, right?.constantState)
    }
}
