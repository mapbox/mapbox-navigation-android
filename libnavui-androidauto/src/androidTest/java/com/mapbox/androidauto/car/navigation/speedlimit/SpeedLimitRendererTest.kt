package com.mapbox.androidauto.car.navigation.speedlimit

import android.Manifest
import androidx.test.filters.SmallTest
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.rule.GrantPermissionRule
import com.mapbox.androidauto.testing.BitmapTestUtil
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
@SmallTest
class SpeedLimitRendererTest {

    @Rule
    @JvmField
    val testName = TestName()

    @get:Rule
    val permissionsRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private val bitmapUtils = BitmapTestUtil(
        "expected_speed_limit_images",
        "test_speed_limit_images"
    )

    private val speedLimitWidget = SpeedLimitWidget()

    @Test
    fun speed_limit_120_speed_150() {
        bitmapUtils.assertBitmapsSimilar(testName, speedLimitWidget.drawSpeedLimitSign(speedLimit = 120, speed = 150))
    }

    @Test
    fun speed_limit_120_speed_90() {
        bitmapUtils.assertBitmapsSimilar(testName, speedLimitWidget.drawSpeedLimitSign(speedLimit = 120, speed = 90))
    }

    @Test
    fun speed_limit_65_speed_90() {
        bitmapUtils.assertBitmapsSimilar(testName, speedLimitWidget.drawSpeedLimitSign(speedLimit = 65, speed = 90))
    }

    @Test
    fun speed_limit_65_speed_30() {
        bitmapUtils.assertBitmapsSimilar(testName, speedLimitWidget.drawSpeedLimitSign(speedLimit = 65, speed = 30))
    }

    @Test
    fun speed_limit_5_speed_30() {
        bitmapUtils.assertBitmapsSimilar(testName, speedLimitWidget.drawSpeedLimitSign(speedLimit = 5, speed = 30))
    }

    @Test
    fun speed_limit_5_speed_0() {
        bitmapUtils.assertBitmapsSimilar(testName, speedLimitWidget.drawSpeedLimitSign(speedLimit = 5, speed = 0))
    }

    @Test
    fun speed_limit_unknown_speed_5() {
        bitmapUtils.assertBitmapsSimilar(testName, speedLimitWidget.drawSpeedLimitSign(speedLimit = null, speed = 5))
    }
}
