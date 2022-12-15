package com.mapbox.androidauto.car.navigation.speedlimit

import android.Manifest
import androidx.test.filters.SmallTest
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.rule.GrantPermissionRule
import com.mapbox.androidauto.navigation.speedlimit.SpeedLimitWidget
import com.mapbox.androidauto.testing.BitmapTestUtil
import com.mapbox.androidauto.testing.MemoryTestRule
import com.mapbox.maps.MapboxExperimental
import com.mapbox.navigation.base.speed.model.SpeedLimitSign
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@OptIn(MapboxExperimental::class)
@RunWith(AndroidJUnit4ClassRunner::class)
@SmallTest
class SpeedLimitRendererTest {

    @Rule
    @JvmField
    val testName = TestName()

    @Rule
    @JvmField
    val memoryTestRule = MemoryTestRule()

    @get:Rule
    val permissionsRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private val bitmapUtils = BitmapTestUtil(
        "expected_speed_limit_images",
        "test_speed_limit_images"
    )

    @After
    fun teardown() {
        println("memory used %.2f MB".format(memoryTestRule.memoryUsedMB))
    }

    @Test
    fun speed_limit_120_speed_150_mutcd() {
        val sut = SpeedLimitWidget()
        sut.update(
            speedLimit = 120,
            speed = 150,
            signFormat = SpeedLimitSign.MUTCD,
            threshold = 0
        )

        bitmapUtils.assertBitmapsSimilar(testName, sut.bitmap!!)
    }

    @Test
    fun speed_limit_120_speed_90_mutcd() {
        val sut = SpeedLimitWidget()
        sut.update(
            speedLimit = 120,
            speed = 90,
            signFormat = SpeedLimitSign.MUTCD,
            threshold = 50
        )

        bitmapUtils.assertBitmapsSimilar(testName, sut.bitmap!!)
    }

    @Test
    fun speed_limit_65_speed_30_mutcd() {
        val bitmap =
            SpeedLimitWidget.drawMutcdSpeedLimitSign(speedLimit = 65, speed = 30, warn = false)
        bitmapUtils.assertBitmapsSimilar(testName, bitmap)
    }

    @Test
    fun speed_limit_5_speed_30_mutcd() {
        val bitmap =
            SpeedLimitWidget.drawMutcdSpeedLimitSign(speedLimit = 5, speed = 30, warn = true)
        bitmapUtils.assertBitmapsSimilar(testName, bitmap)
    }

    @Test
    fun speed_limit_5_speed_0_mutcd() {
        val bitmap =
            SpeedLimitWidget.drawMutcdSpeedLimitSign(speedLimit = 5, speed = 0, warn = false)
        bitmapUtils.assertBitmapsSimilar(testName, bitmap)
    }

    @Test
    fun speed_limit_unknown_speed_5_mutcd() {
        val bitmap =
            SpeedLimitWidget.drawMutcdSpeedLimitSign(speedLimit = null, speed = 5, warn = false)
        bitmapUtils.assertBitmapsSimilar(testName, bitmap)
    }

    @Test
    fun speed_limit_120_speed_150_vienna() {
        val bitmap =
            SpeedLimitWidget.drawViennaSpeedLimitSign(speedLimit = 120, speed = 150, warn = true)
        bitmapUtils.assertBitmapsSimilar(testName, bitmap)
    }

    @Test
    fun speed_limit_120_speed_90_vienna() {
        val bitmap =
            SpeedLimitWidget.drawViennaSpeedLimitSign(speedLimit = 120, speed = 90, warn = false)
        bitmapUtils.assertBitmapsSimilar(testName, bitmap)
    }

    @Test
    fun speed_limit_65_speed_30_vienna() {
        val bitmap =
            SpeedLimitWidget.drawViennaSpeedLimitSign(speedLimit = 65, speed = 30, warn = false)
        bitmapUtils.assertBitmapsSimilar(testName, bitmap)
    }

    @Test
    fun speed_limit_5_speed_30_vienna() {
        val bitmap =
            SpeedLimitWidget.drawViennaSpeedLimitSign(speedLimit = 5, speed = 30, warn = true)
        bitmapUtils.assertBitmapsSimilar(testName, bitmap)
    }

    @Test
    fun speed_limit_5_speed_0_vienna() {
        val bitmap =
            SpeedLimitWidget.drawViennaSpeedLimitSign(speedLimit = 5, speed = 0, warn = false)
        bitmapUtils.assertBitmapsSimilar(testName, bitmap)
    }

    @Test
    fun speed_limit_unknown_speed_5_vienna() {
        val bitmap =
            SpeedLimitWidget.drawViennaSpeedLimitSign(speedLimit = null, speed = 5, warn = false)
        bitmapUtils.assertBitmapsSimilar(testName, bitmap)
    }

    @Test
    fun multiple_calls_() {
        val bitmap =
            SpeedLimitWidget.drawViennaSpeedLimitSign(speedLimit = null, speed = 5, warn = false)
        bitmapUtils.assertBitmapsSimilar(testName, bitmap)
    }
}
