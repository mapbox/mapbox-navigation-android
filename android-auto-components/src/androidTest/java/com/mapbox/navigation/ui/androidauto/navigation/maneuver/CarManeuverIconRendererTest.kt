package com.mapbox.navigation.ui.androidauto.navigation.maneuver

import android.Manifest
import android.graphics.Color
import androidx.test.filters.SmallTest
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.rule.GrantPermissionRule
import com.mapbox.api.directions.v5.models.ManeuverModifier
import com.mapbox.api.directions.v5.models.StepManeuver
import com.mapbox.navigation.tripdata.maneuver.model.PrimaryManeuver
import com.mapbox.navigation.ui.androidauto.testing.BitmapTestUtil
import io.mockk.every
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
@SmallTest
class CarManeuverIconRendererTest {

    @Rule
    @JvmField
    val testName = TestName()

    @get:Rule
    val permissionsRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
    )

    private val bitmapTestUtils = BitmapTestUtil(
        "expected_maneuver_icons",
        "test_maneuver_icons",
    )

    private val carLanesImageGenerator = CarManeuverIconRenderer(
        CarManeuverIconOptions.Builder(bitmapTestUtils.carDisplayContext())
            .background(Color.RED)
            .build(),
    )

    @Test
    fun turn_straight() {
        val carLanesImage = carLanesImageGenerator.renderManeuverIcon(
            maneuver = mockk<PrimaryManeuver>(relaxed = true) {
                every { type } returns StepManeuver.TURN
                every { modifier } returns ManeuverModifier.STRAIGHT
            },
        )

        val actual = carLanesImage!!.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun driving_side_right_u_turn() {
        val carLanesImage = carLanesImageGenerator.renderManeuverIcon(
            maneuver = mockk<PrimaryManeuver>(relaxed = true) {
                every { type } returns StepManeuver.TURN
                every { modifier } returns ManeuverModifier.UTURN
                every { drivingSide } returns "right"
            },
        )

        val actual = carLanesImage!!.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun driving_side_left_u_turn() {
        val carLanesImage = carLanesImageGenerator.renderManeuverIcon(
            maneuver = mockk<PrimaryManeuver>(relaxed = true) {
                every { type } returns StepManeuver.TURN
                every { modifier } returns ManeuverModifier.UTURN
                every { drivingSide } returns "left"
            },
        )

        val actual = carLanesImage!!.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun turn_left() {
        val carLanesImage = carLanesImageGenerator.renderManeuverIcon(
            maneuver = mockk<PrimaryManeuver>(relaxed = true) {
                every { type } returns StepManeuver.TURN
                every { modifier } returns ManeuverModifier.SHARP_LEFT
            },
        )

        val actual = carLanesImage!!.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun turn_right() {
        val carLanesImage = carLanesImageGenerator.renderManeuverIcon(
            maneuver = mockk<PrimaryManeuver>(relaxed = true) {
                every { type } returns StepManeuver.TURN
                every { modifier } returns ManeuverModifier.SHARP_RIGHT
            },
        )

        val actual = carLanesImage!!.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun turn_slight_left() {
        val carLanesImage = carLanesImageGenerator.renderManeuverIcon(
            maneuver = mockk<PrimaryManeuver>(relaxed = true) {
                every { type } returns StepManeuver.TURN
                every { modifier } returns ManeuverModifier.SLIGHT_LEFT
            },
        )

        val actual = carLanesImage!!.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun turn_slight_right() {
        val carLanesImage = carLanesImageGenerator.renderManeuverIcon(
            maneuver = mockk<PrimaryManeuver>(relaxed = true) {
                every { type } returns StepManeuver.TURN
                every { modifier } returns ManeuverModifier.SLIGHT_RIGHT
            },
        )

        val actual = carLanesImage!!.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun turn_sharp_left() {
        val carLanesImage = carLanesImageGenerator.renderManeuverIcon(
            maneuver = mockk<PrimaryManeuver>(relaxed = true) {
                every { type } returns StepManeuver.TURN
                every { modifier } returns ManeuverModifier.SHARP_LEFT
            },
        )

        val actual = carLanesImage!!.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun turn_sharp_right() {
        val carLanesImage = carLanesImageGenerator.renderManeuverIcon(
            maneuver = mockk<PrimaryManeuver>(relaxed = true) {
                every { type } returns StepManeuver.TURN
                every { modifier } returns ManeuverModifier.SHARP_RIGHT
            },
        )

        val actual = carLanesImage!!.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun driving_side_right_roundabout_degrees_45() {
        val carLanesImage = carLanesImageGenerator.renderManeuverIcon(
            maneuver = mockk<PrimaryManeuver>(relaxed = true) {
                every { type } returns StepManeuver.ROUNDABOUT
                every { degrees } returns 45.0
                every { drivingSide } returns "right"
            },
        )

        val actual = carLanesImage!!.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun driving_side_right_roundabout_degrees_90() {
        val carLanesImage = carLanesImageGenerator.renderManeuverIcon(
            maneuver = mockk<PrimaryManeuver>(relaxed = true) {
                every { type } returns StepManeuver.ROUNDABOUT
                every { degrees } returns 90.0
                every { drivingSide } returns "right"
            },
        )

        val actual = carLanesImage!!.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun driving_side_right_roundabout_degrees_135() {
        val carLanesImage = carLanesImageGenerator.renderManeuverIcon(
            maneuver = mockk<PrimaryManeuver>(relaxed = true) {
                every { type } returns StepManeuver.ROUNDABOUT
                every { degrees } returns 135.0
                every { drivingSide } returns "right"
            },
        )

        val actual = carLanesImage!!.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun driving_side_right_roundabout_degrees_180() {
        val carLanesImage = carLanesImageGenerator.renderManeuverIcon(
            maneuver = mockk<PrimaryManeuver>(relaxed = true) {
                every { type } returns StepManeuver.ROUNDABOUT
                every { degrees } returns 180.0
                every { drivingSide } returns "right"
            },
        )

        val actual = carLanesImage!!.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun driving_side_right_roundabout_degrees_225() {
        val carLanesImage = carLanesImageGenerator.renderManeuverIcon(
            maneuver = mockk<PrimaryManeuver>(relaxed = true) {
                every { type } returns StepManeuver.ROUNDABOUT
                every { degrees } returns 225.0
                every { drivingSide } returns "right"
            },
        )

        val actual = carLanesImage!!.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun driving_side_right_roundabout_degrees_270() {
        val carLanesImage = carLanesImageGenerator.renderManeuverIcon(
            maneuver = mockk<PrimaryManeuver>(relaxed = true) {
                every { type } returns StepManeuver.ROUNDABOUT
                every { degrees } returns 270.0
                every { drivingSide } returns "right"
            },
        )

        val actual = carLanesImage!!.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun driving_side_right_roundabout_degrees_315() {
        val carLanesImage = carLanesImageGenerator.renderManeuverIcon(
            maneuver = mockk<PrimaryManeuver>(relaxed = true) {
                every { type } returns StepManeuver.ROUNDABOUT
                every { degrees } returns 315.0
                every { drivingSide } returns "right"
            },
        )

        val actual = carLanesImage!!.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun driving_side_left_roundabout_degrees_45() {
        val carLanesImage = carLanesImageGenerator.renderManeuverIcon(
            maneuver = mockk<PrimaryManeuver>(relaxed = true) {
                every { type } returns StepManeuver.ROUNDABOUT
                every { degrees } returns 45.0
                every { drivingSide } returns "left"
            },
        )

        val actual = carLanesImage!!.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun driving_side_left_roundabout_degrees_90() {
        val carLanesImage = carLanesImageGenerator.renderManeuverIcon(
            maneuver = mockk<PrimaryManeuver>(relaxed = true) {
                every { type } returns StepManeuver.ROUNDABOUT
                every { degrees } returns 90.0
                every { drivingSide } returns "left"
            },
        )

        val actual = carLanesImage!!.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun driving_side_left_roundabout_degrees_135() {
        val carLanesImage = carLanesImageGenerator.renderManeuverIcon(
            maneuver = mockk<PrimaryManeuver>(relaxed = true) {
                every { type } returns StepManeuver.ROUNDABOUT
                every { degrees } returns 135.0
                every { drivingSide } returns "left"
            },
        )

        val actual = carLanesImage!!.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun driving_side_left_roundabout_degrees_180() {
        val carLanesImage = carLanesImageGenerator.renderManeuverIcon(
            maneuver = mockk<PrimaryManeuver>(relaxed = true) {
                every { type } returns StepManeuver.ROUNDABOUT
                every { degrees } returns 180.0
                every { drivingSide } returns "left"
            },
        )

        val actual = carLanesImage!!.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun driving_side_left_roundabout_degrees_225() {
        val carLanesImage = carLanesImageGenerator.renderManeuverIcon(
            maneuver = mockk<PrimaryManeuver>(relaxed = true) {
                every { type } returns StepManeuver.ROUNDABOUT
                every { degrees } returns 225.0
                every { drivingSide } returns "left"
            },
        )

        val actual = carLanesImage!!.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun driving_side_left_roundabout_degrees_270() {
        val carLanesImage = carLanesImageGenerator.renderManeuverIcon(
            maneuver = mockk<PrimaryManeuver>(relaxed = true) {
                every { type } returns StepManeuver.ROUNDABOUT
                every { degrees } returns 270.0
                every { drivingSide } returns "left"
            },
        )

        val actual = carLanesImage!!.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun driving_side_left_roundabout_degrees_315() {
        val carLanesImage = carLanesImageGenerator.renderManeuverIcon(
            maneuver = mockk<PrimaryManeuver>(relaxed = true) {
                every { type } returns StepManeuver.ROUNDABOUT
                every { degrees } returns 315.0
                every { drivingSide } returns "left"
            },
        )

        val actual = carLanesImage!!.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }
}
