package com.mapbox.navigation.instrumentation_tests.androidauto

import android.Manifest
import android.graphics.Color
import androidx.test.filters.SmallTest
import androidx.test.rule.GrantPermissionRule
import com.mapbox.androidauto.car.navigation.lanes.CarLanesImageRenderer
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.instrumentation_tests.utils.BitmapTestUtil
import com.mapbox.navigation.ui.maneuver.model.LaneFactory
import com.mapbox.navigation.ui.maneuver.model.LaneIndicator
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName

@OptIn(ExperimentalMapboxNavigationAPI::class)
@SmallTest
class CarLanesImageRendererTest {

    @Rule
    @JvmField
    var testName = TestName()

    @get:Rule
    val permissionsRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private val bitmapTestUtils = BitmapTestUtil(
        "androidauto/expected_lanes_images",
        "test_lanes_images"
    )

    private val carLanesImageGenerator = CarLanesImageRenderer(
        context = bitmapTestUtils.carDisplayContext(),
        background = Color.RED
    )

    @Test
    fun one_lane_uturn() {
        val carLanesImage = carLanesImageGenerator.renderLanesImage(
            lane = LaneFactory.buildLane(
                listOf(
                    LaneIndicator.Builder()
                        .drivingSide("right")
                        .activeDirection("uturn")
                        .isActive(true)
                        .directions(listOf("uturn"))
                        .build()
                )
            )
        )

        val actual = carLanesImage!!.carIcon.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun two_lanes_straight_sharp_left_straight() {
        val carLanesImage = carLanesImageGenerator.renderLanesImage(
            lane = LaneFactory.buildLane(
                listOf(
                    LaneIndicator.Builder()
                        .drivingSide("right")
                        .activeDirection("sharp left")
                        .isActive(true)
                        .directions(listOf("sharp left"))
                        .build(),
                    LaneIndicator.Builder()
                        .drivingSide("right")
                        .activeDirection("sharp left")
                        .isActive(false)
                        .directions(listOf("straight"))
                        .build()
                )
            )
        )

        val actual = carLanesImage!!.carIcon.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun three_lanes_straight_left_straight_right() {
        val carLanesImage = carLanesImageGenerator.renderLanesImage(
            lane = LaneFactory.buildLane(
                listOf(
                    LaneIndicator.Builder()
                        .drivingSide("right")
                        .activeDirection("straight")
                        .isActive(true)
                        .directions(listOf("straight", "left"))
                        .build(),
                    LaneIndicator.Builder()
                        .drivingSide("right")
                        .activeDirection("straight")
                        .isActive(true)
                        .directions(listOf("straight"))
                        .build(),
                    LaneIndicator.Builder()
                        .drivingSide("right")
                        .activeDirection("straight")
                        .isActive(false)
                        .directions(listOf("right"))
                        .build()
                )
            )
        )

        val actual = carLanesImage!!.carIcon.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun four_lanes_right_left_straight_right() {
        val carLanesImage = carLanesImageGenerator.renderLanesImage(
            lane = LaneFactory.buildLane(
                listOf(
                    LaneIndicator.Builder()
                        .drivingSide("right")
                        .activeDirection("right")
                        .isActive(false)
                        .directions(listOf("straight", "left"))
                        .build(),
                    LaneIndicator.Builder()
                        .drivingSide("right")
                        .activeDirection("right")
                        .isActive(false)
                        .directions(listOf("straight"))
                        .build(),
                    LaneIndicator.Builder()
                        .drivingSide("right")
                        .activeDirection("right")
                        .isActive(true)
                        .directions(listOf("straight", "right"))
                        .build(),
                    LaneIndicator.Builder()
                        .drivingSide("right")
                        .activeDirection("right")
                        .isActive(true)
                        .directions(listOf("right"))
                        .build()
                )
            )
        )

        val actual = carLanesImage!!.carIcon.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun five_lanes_straight_various() {
        val carLanesImage = carLanesImageGenerator.renderLanesImage(
            lane = LaneFactory.buildLane(
                listOf(
                    LaneIndicator.Builder()
                        .drivingSide("right")
                        .activeDirection("straight")
                        .isActive(true)
                        .directions(listOf("straight", "left"))
                        .build(),
                    LaneIndicator.Builder()
                        .drivingSide("right")
                        .activeDirection("straight")
                        .isActive(true)
                        .directions(listOf("straight", "slight left"))
                        .build(),
                    LaneIndicator.Builder()
                        .drivingSide("right")
                        .activeDirection("straight")
                        .isActive(true)
                        .directions(listOf("straight", "slight right"))
                        .build(),
                    LaneIndicator.Builder()
                        .drivingSide("right")
                        .activeDirection("straight")
                        .isActive(false)
                        .directions(listOf("right"))
                        .build(),
                    LaneIndicator.Builder()
                        .drivingSide("right")
                        .activeDirection("straight")
                        .isActive(false)
                        .directions(listOf("sharp right"))
                        .build()
                )
            )
        )

        val actual = carLanesImage!!.carIcon.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun six_lanes_left_various() {
        val carLanesImage = carLanesImageGenerator.renderLanesImage(
            lane = LaneFactory.buildLane(
                listOf(
                    LaneIndicator.Builder()
                        .drivingSide("right")
                        .activeDirection(null)
                        .isActive(false)
                        .directions(listOf("uturn"))
                        .build(),
                    LaneIndicator.Builder()
                        .drivingSide("right")
                        .activeDirection("left")
                        .isActive(false)
                        .directions(listOf("straight", "left"))
                        .build(),
                    LaneIndicator.Builder()
                        .drivingSide("right")
                        .activeDirection("left")
                        .isActive(false)
                        .directions(listOf("straight", "slight left"))
                        .build(),
                    LaneIndicator.Builder()
                        .drivingSide("right")
                        .activeDirection("left")
                        .isActive(false)
                        .directions(listOf("straight", "slight right"))
                        .build(),
                    LaneIndicator.Builder()
                        .drivingSide("right")
                        .activeDirection("left")
                        .isActive(true)
                        .directions(listOf("right"))
                        .build(),
                    LaneIndicator.Builder()
                        .drivingSide("right")
                        .activeDirection("left")
                        .isActive(false)
                        .directions(listOf("sharp right"))
                        .build()
                )
            )
        )

        val actual = carLanesImage!!.carIcon.icon!!.bitmap!!
        bitmapTestUtils.assertBitmapsSimilar(testName, actual)
    }

    @Test
    fun seven_lanes_left_various() {
        val carLanesImage = carLanesImageGenerator.renderLanesImage(
            lane = LaneFactory.buildLane(
                listOf(
                    LaneIndicator.Builder()
                        .drivingSide("right")
                        .activeDirection("left")
                        .isActive(false)
                        .directions(listOf("straight"))
                        .build(),
                    LaneIndicator.Builder()
                        .drivingSide("right")
                        .activeDirection("left")
                        .isActive(false)
                        .directions(listOf("straight"))
                        .build(),
                    LaneIndicator.Builder()
                        .drivingSide("right")
                        .activeDirection("left")
                        .isActive(false)
                        .directions(listOf("straight"))
                        .build(),
                    LaneIndicator.Builder()
                        .drivingSide("right")
                        .activeDirection("left")
                        .isActive(false)
                        .directions(listOf("straight", "slight left"))
                        .build(),
                    LaneIndicator.Builder()
                        .drivingSide("right")
                        .activeDirection("left")
                        .isActive(true)
                        .directions(listOf("slight right"))
                        .build(),
                    LaneIndicator.Builder()
                        .drivingSide("right")
                        .activeDirection("left")
                        .isActive(true)
                        .directions(listOf("right"))
                        .build(),
                    LaneIndicator.Builder()
                        .drivingSide("right")
                        .activeDirection("left")
                        .isActive(true)
                        .directions(listOf("sharp right"))
                        .build()
                )
            )
        )

        // Fail successfully for now. Handle more images with scaling
//        writeFileSample(carLanesImage!!)
        assertNotNull(carLanesImage)
    }
}
