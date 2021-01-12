package com.mapbox.navigation.ui.maneuver.view

import android.content.Context
import android.graphics.Typeface
import androidx.test.core.app.ApplicationProvider
import com.mapbox.api.directions.v5.models.ManeuverModifier
import com.mapbox.navigation.ui.base.model.maneuver.ExitNumberComponentNode
import com.mapbox.navigation.ui.maneuver.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        val expectedTextColor = ctx.getColor(R.color.maneuverExitTextColor)

        val view = MapboxExitText(ctx)

        assertEquals(
            expectedTextColor,
            view.currentTextColor
        )
    }

    @Test
    fun `init text size`() {
        val expectedTextSize = 10 * ctx.resources.displayMetrics.scaledDensity

        val view = MapboxExitText(ctx)

        assertEquals(
            expectedTextSize,
            view.textSize
        )
    }

    @Test
    fun `init text typeface`() {
        val expectedTypeface = Typeface.BOLD

        val view = MapboxExitText(ctx)

        assertEquals(
            expectedTypeface,
            view.typeface.style
        )
    }

    @Test
    fun `exit text`() {
        val view = MapboxExitText(ctx)
        val exitNumberComponent = ExitNumberComponentNode
            .Builder()
            .text("23")
            .build()
        val expectedText = exitNumberComponent.text

        view.setExit(ManeuverModifier.LEFT, exitNumberComponent)

        assertEquals(
            expectedText,
            view.text
        )
    }
}
