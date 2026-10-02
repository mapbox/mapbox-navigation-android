package com.mapbox.navigation.ui.androidauto.ui.maneuver.view

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mapbox.api.directions.v5.models.ManeuverModifier
import com.mapbox.navigation.tripdata.maneuver.model.ExitNumberComponentNode
import com.mapbox.navigation.ui.androidauto.ui.maneuver.model.MapboxExitProperties
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MapboxExitTextTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val exit = ExitNumberComponentNode.Builder().text("12A").build()

    @Test
    fun `every left modifier draws the arrow on the left`() {
        listOf(
            ManeuverModifier.LEFT,
            ManeuverModifier.SLIGHT_LEFT,
            ManeuverModifier.SHARP_LEFT,
        ).forEach { modifier ->
            val exitText = mutcdExitText()

            exitText.setExit(modifier, exit)

            val (start, _, end, _) = exitText.compoundDrawables
            assertNotNull(modifier, start)
            assertNull(modifier, end)
            assertEquals("12A", exitText.text.toString())
        }
    }

    @Test
    fun `every right modifier draws the arrow on the right`() {
        listOf(
            ManeuverModifier.RIGHT,
            ManeuverModifier.SLIGHT_RIGHT,
            ManeuverModifier.SHARP_RIGHT,
        ).forEach { modifier ->
            val exitText = mutcdExitText()

            exitText.setExit(modifier, exit)

            val (start, _, end, _) = exitText.compoundDrawables
            assertNull(modifier, start)
            assertNotNull(modifier, end)
            assertEquals("12A", exitText.text.toString())
        }
    }

    @Test
    fun `fallback text prefixes the exit number`() {
        val exitText = MapboxExitText(context).apply {
            updateExitProperties(
                MapboxExitProperties.PropertiesMutcd(
                    shouldFallbackWithText = true,
                    shouldFallbackWithDrawable = false,
                ),
            )
        }

        exitText.setExit(ManeuverModifier.STRAIGHT, exit)

        assertEquals("Exit 12A", exitText.text.toString())
    }

    private fun mutcdExitText() = MapboxExitText(context).apply {
        updateExitProperties(MapboxExitProperties.PropertiesMutcd())
    }
}
