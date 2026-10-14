package com.mapbox.navigation.ui.androidauto.navigation.lanes

import android.content.Context
import androidx.car.app.navigation.model.Step
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.tripdata.maneuver.model.Lane
import com.mapbox.navigation.ui.androidauto.testing.MapboxRobolectricTestRunner
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CarLanesImageRendererTest : MapboxRobolectricTestRunner() {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val sut = CarLanesImageRenderer(context)

    // Lanes without indications are dropped from the guidance, which can leave no lanes.
    private val laneGuidanceWithoutLanes: Lane = mockk {
        every { allLanes } returns emptyList()
    }

    @Test
    fun `lane guidance without lanes renders no lanes image`() {
        assertNull(sut.renderLanesImage(laneGuidanceWithoutLanes))
    }

    @Test
    fun `a step with lane guidance without lanes is built without a lanes image`() {
        val step = Step.Builder("Turn left")
            .useMapboxLaneGuidance(sut, laneGuidanceWithoutLanes)
            .build()

        assertNull(step.lanesImage)
        assertTrue(step.lanes.isEmpty())
    }
}
