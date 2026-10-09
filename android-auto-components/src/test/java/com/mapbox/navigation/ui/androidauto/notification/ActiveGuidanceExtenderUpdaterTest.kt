package com.mapbox.navigation.ui.androidauto.notification

import android.content.Context
import android.text.SpannableString
import androidx.car.app.notification.CarAppExtender
import androidx.test.core.app.ApplicationProvider
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.ManeuverModifier
import com.mapbox.api.directions.v5.models.StepManeuver
import com.mapbox.navigation.base.formatter.DistanceFormatter
import com.mapbox.navigation.base.formatter.TimeFormatter
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.ui.androidauto.testing.MapboxRobolectricTestRunner
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveGuidanceExtenderUpdaterTest : MapboxRobolectricTestRunner() {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val distanceFormatter = DistanceFormatter { SpannableString("1 km") }
    private val timeFormatter = TimeFormatter { "10:00" }
    private val sut = ActiveGuidanceExtenderUpdater(context)

    @Test
    fun `title is complete when it is set`() {
        var titleWhenSet: String? = null
        val builder = mockk<CarAppExtender.Builder>(relaxed = true) {
            every { setContentTitle(any()) } answers {
                titleWhenSet = firstArg<CharSequence>().toString()
                self as CarAppExtender.Builder
            }
        }

        sut.update(builder, routeProgress(), distanceFormatter, timeFormatter)

        val title = titleWhenSet!!
        assertTrue(title, title.startsWith("1 km • "))
        assertTrue(title, title.contains("10:00"))
    }

    @Test
    fun `reset forgets everything shown for the previous trip`() {
        val previousTrip = update(routeProgress(instruction = "Turn left"))
        assertEquals("Turn left", previousTrip.contentText.toString())
        assertNotNull(previousTrip.largeIcon)

        sut.reset()
        // The first progress of the next trip has no leg progress and no banner yet.
        val nextTrip = update(
            mockk {
                every { bannerInstructions } returns null
                every { currentLegProgress } returns null
            },
        )

        assertNull(nextTrip.contentText)
        assertNull(nextTrip.largeIcon)
        val title = nextTrip.contentTitle.toString()
        assertFalse(title, title.contains("1 km"))
        assertFalse(title, title.contains("10:00"))
    }

    private fun update(routeProgress: RouteProgress): CarAppExtender {
        val builder = CarAppExtender.Builder()
        sut.update(builder, routeProgress, distanceFormatter, timeFormatter)
        return builder.build()
    }

    private fun routeProgress(instruction: String? = null): RouteProgress = mockk {
        every { bannerInstructions } returns instruction?.let { text ->
            mockk<BannerInstructions> {
                every { primary() } returns mockk(relaxed = true) {
                    every { text() } returns text
                    every { type() } returns StepManeuver.TURN
                    every { modifier() } returns ManeuverModifier.LEFT
                    every { degrees() } returns null
                }
            }
        }
        every { currentLegProgress } returns mockk {
            every { durationRemaining } returns 600.0
            every { currentStepProgress } returns mockk {
                every { distanceRemaining } returns 1000f
                every { step } returns null
            }
        }
    }
}
