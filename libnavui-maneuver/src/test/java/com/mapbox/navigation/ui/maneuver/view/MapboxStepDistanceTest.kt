package com.mapbox.navigation.ui.maneuver.view

import android.content.Context
import android.text.SpannableString
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.base.formatter.DistanceFormatter
import com.mapbox.navigation.ui.maneuver.model.StepDistance
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MapboxStepDistanceTest {

    lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `render step distance`() {
        val view = MapboxStepDistance(ctx)
        val expected = SpannableString("13 mi")
        val mockStepDistanceRemaining = 45.0
        val mockState = StepDistance(
            mockk {
                every { formatDistance(mockStepDistanceRemaining) } returns SpannableString("13 mi")
            },
            75.0,
            mockStepDistanceRemaining
        )

        view.renderDistanceRemaining(mockState)
        val actual = view.text

        assertEquals(expected, actual)
    }

    @Test
    fun equalsTest() {
        val mockDistanceFormatter = mockk<DistanceFormatter>()
        val stepDistance1 = StepDistance(mockDistanceFormatter, 5.5, 6.6)
        val stepDistance2 = StepDistance(mockDistanceFormatter, 5.5, 6.6)

        val areEqual = stepDistance1 == stepDistance2

        assertTrue(areEqual)
    }

    @Test
    fun equals_whenTotalDistanceNotEqual() {
        val mockDistanceFormatter = mockk<DistanceFormatter>()
        val stepDistance1 = StepDistance(mockDistanceFormatter, 5.5, 6.6)
        val stepDistance2 = StepDistance(mockDistanceFormatter, 4.4, 6.6)

        val areEqual = stepDistance1 != stepDistance2

        assertTrue(areEqual)
    }

    @Test
    fun equals_whenDistanceRemainingNotEqual() {
        val mockDistanceFormatter = mockk<DistanceFormatter>()
        val stepDistance1 = StepDistance(mockDistanceFormatter, 5.5, 6.6)
        val stepDistance2 = StepDistance(mockDistanceFormatter, 5.5, 3.3)

        val areEqual = stepDistance1 != stepDistance2

        assertTrue(areEqual)
    }
}
