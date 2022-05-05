package com.mapbox.navigation.instrumentation_tests.androidauto

import android.Manifest
import android.graphics.Color
import androidx.test.filters.SmallTest
import androidx.test.rule.GrantPermissionRule
import com.mapbox.androidauto.car.navigation.maneuver.CarManeuverIconOptions
import com.mapbox.androidauto.car.navigation.maneuver.CarManeuverIconRenderer
import com.mapbox.api.directions.v5.models.ManeuverModifier
import com.mapbox.api.directions.v5.models.StepManeuver
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.instrumentation_tests.utils.BitmapTestUtil
import com.mapbox.navigation.ui.maneuver.model.Component
import com.mapbox.navigation.ui.maneuver.model.PrimaryManeuver
import com.mapbox.navigation.ui.maneuver.model.PrimaryManeuverFactory
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName

@SmallTest
class CarManeuverIconRendererTest {

    @Rule
    @JvmField
    val testName = TestName()

    @get:Rule
    val permissionsRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private val bitmapTestUtils = BitmapTestUtil(
        "androidauto/expected_maneuver_icons",
        "test_maneuver_icons"
    )

    private val carLanesImageGenerator = CarManeuverIconRenderer(
        CarManeuverIconOptions.Builder(bitmapTestUtils.carDisplayContext())
            .background(Color.RED)
            .build()
    )

    @Test
    fun turn_straight() {
        val carLanesImage = carLanesImageGenerator.renderManeuverIcon(
            maneuver = buildPrimaryManeuver(
                type = StepManeuver.TURN,
                modifier = ManeuverModifier.STRAIGHT,
            )
        )

        val actual = carLanesImage!!.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun driving_side_right_u_turn() {
        val carLanesImage = carLanesImageGenerator.renderManeuverIcon(
            maneuver = buildPrimaryManeuver(
                type = StepManeuver.TURN,
                modifier = ManeuverModifier.UTURN,
                drivingSide = "right",
            )
        )

        val actual = carLanesImage!!.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun driving_side_left_u_turn() {
        val carLanesImage = carLanesImageGenerator.renderManeuverIcon(
            maneuver = buildPrimaryManeuver(
                type = StepManeuver.TURN,
                modifier = ManeuverModifier.UTURN,
                drivingSide = "left",
            )
        )

        val actual = carLanesImage!!.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun turn_left() {
        val carLanesImage = carLanesImageGenerator.renderManeuverIcon(
            maneuver = buildPrimaryManeuver(
                type = StepManeuver.TURN,
                modifier = ManeuverModifier.SHARP_LEFT,
            )
        )

        val actual = carLanesImage!!.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun turn_right() {
        val carLanesImage = carLanesImageGenerator.renderManeuverIcon(
            maneuver = buildPrimaryManeuver(
                type = StepManeuver.TURN,
                modifier = ManeuverModifier.SHARP_RIGHT,
            )
        )

        val actual = carLanesImage!!.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun turn_slight_left() {
        val carLanesImage = carLanesImageGenerator.renderManeuverIcon(
            maneuver = buildPrimaryManeuver(
                type = StepManeuver.TURN,
                modifier = ManeuverModifier.SLIGHT_LEFT,
            )
        )

        val actual = carLanesImage!!.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun turn_slight_right() {
        val carLanesImage = carLanesImageGenerator.renderManeuverIcon(
            maneuver = buildPrimaryManeuver(
                type = StepManeuver.TURN,
                modifier = ManeuverModifier.SLIGHT_RIGHT,
            )
        )

        val actual = carLanesImage!!.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun turn_sharp_left() {
        val carLanesImage = carLanesImageGenerator.renderManeuverIcon(
            maneuver = buildPrimaryManeuver(
                type = StepManeuver.TURN,
                modifier = ManeuverModifier.SHARP_LEFT,
            )
        )

        val actual = carLanesImage!!.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun turn_sharp_right() {
        val carLanesImage = carLanesImageGenerator.renderManeuverIcon(
            maneuver = buildPrimaryManeuver(
                type = StepManeuver.TURN,
                modifier = ManeuverModifier.SHARP_RIGHT,
            )
        )

        val actual = carLanesImage!!.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun driving_side_right_roundabout_degrees_45() {
        val carLanesImage = carLanesImageGenerator.renderManeuverIcon(
            maneuver = buildPrimaryManeuver(
                type = StepManeuver.ROUNDABOUT,
                degrees = 45.0,
                drivingSide = "right",
            )
        )

        val actual = carLanesImage!!.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun driving_side_right_roundabout_degrees_90() {
        val carLanesImage = carLanesImageGenerator.renderManeuverIcon(
            maneuver = buildPrimaryManeuver(
                type = StepManeuver.ROUNDABOUT,
                degrees = 90.0,
                drivingSide = "right",
            )
        )

        val actual = carLanesImage!!.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun driving_side_right_roundabout_degrees_135() {
        val carLanesImage = carLanesImageGenerator.renderManeuverIcon(
            maneuver = buildPrimaryManeuver(
                type = StepManeuver.ROUNDABOUT,
                degrees = 135.0,
                drivingSide = "right",
            )
        )

        val actual = carLanesImage!!.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun driving_side_right_roundabout_degrees_180() {
        val carLanesImage = carLanesImageGenerator.renderManeuverIcon(
            maneuver = buildPrimaryManeuver(
                type = StepManeuver.ROUNDABOUT,
                degrees = 180.0,
                drivingSide = "right",
            )
        )

        val actual = carLanesImage!!.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun driving_side_right_roundabout_degrees_225() {
        val carLanesImage = carLanesImageGenerator.renderManeuverIcon(
            maneuver = buildPrimaryManeuver(
                type = StepManeuver.ROUNDABOUT,
                degrees = 225.0,
                drivingSide = "right",
            )
        )

        val actual = carLanesImage!!.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun driving_side_right_roundabout_degrees_270() {
        val carLanesImage = carLanesImageGenerator.renderManeuverIcon(
            maneuver = buildPrimaryManeuver(
                type = StepManeuver.ROUNDABOUT,
                degrees = 270.0,
                drivingSide = "right",
            )
        )

        val actual = carLanesImage!!.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun driving_side_right_roundabout_degrees_315() {
        val carLanesImage = carLanesImageGenerator.renderManeuverIcon(
            maneuver = buildPrimaryManeuver(
                type = StepManeuver.ROUNDABOUT,
                degrees = 315.0,
                drivingSide = "right",
            )
        )

        val actual = carLanesImage!!.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun driving_side_left_roundabout_degrees_45() {
        val carLanesImage = carLanesImageGenerator.renderManeuverIcon(
            maneuver = buildPrimaryManeuver(
                type = StepManeuver.ROUNDABOUT,
                degrees = 45.0,
                drivingSide = "left",
            )
        )

        val actual = carLanesImage!!.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun driving_side_left_roundabout_degrees_90() {
        val carLanesImage = carLanesImageGenerator.renderManeuverIcon(
            maneuver = buildPrimaryManeuver(
                type = StepManeuver.ROUNDABOUT,
                degrees = 90.0,
                drivingSide = "left",
            )
        )

        val actual = carLanesImage!!.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun driving_side_left_roundabout_degrees_135() {
        val carLanesImage = carLanesImageGenerator.renderManeuverIcon(
            maneuver = buildPrimaryManeuver(
                type = StepManeuver.ROUNDABOUT,
                degrees = 135.0,
                drivingSide = "left",
            )
        )

        val actual = carLanesImage!!.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun driving_side_left_roundabout_degrees_180() {
        val carLanesImage = carLanesImageGenerator.renderManeuverIcon(
            maneuver = buildPrimaryManeuver(
                type = StepManeuver.ROUNDABOUT,
                degrees = 180.0,
                drivingSide = "left",
            )
        )

        val actual = carLanesImage!!.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun driving_side_left_roundabout_degrees_225() {
        val carLanesImage = carLanesImageGenerator.renderManeuverIcon(
            maneuver = buildPrimaryManeuver(
                type = StepManeuver.ROUNDABOUT,
                degrees = 225.0,
                drivingSide = "left",
            )
        )

        val actual = carLanesImage!!.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun driving_side_left_roundabout_degrees_270() {
        val carLanesImage = carLanesImageGenerator.renderManeuverIcon(
            maneuver = buildPrimaryManeuver(
                type = StepManeuver.ROUNDABOUT,
                degrees = 270.0,
                drivingSide = "left",
            )
        )

        val actual = carLanesImage!!.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun driving_side_left_roundabout_degrees_315() {
        val carLanesImage = carLanesImageGenerator.renderManeuverIcon(
            maneuver = buildPrimaryManeuver(
                type = StepManeuver.ROUNDABOUT,
                degrees = 315.0,
                drivingSide = "left",
            )
        )

        val actual = carLanesImage!!.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @OptIn(ExperimentalMapboxNavigationAPI::class)
    private fun buildPrimaryManeuver(
        id: String = "",
        text: String = "",
        type: String = "",
        degrees: Double = 0.0,
        modifier: String = "",
        drivingSide: String = "",
        componentList: List<Component> = emptyList()
    ): PrimaryManeuver {
        return PrimaryManeuverFactory.buildPrimaryManeuver(
            id,
            text,
            type,
            degrees,
            modifier,
            drivingSide,
            componentList,
        )
    }
}
