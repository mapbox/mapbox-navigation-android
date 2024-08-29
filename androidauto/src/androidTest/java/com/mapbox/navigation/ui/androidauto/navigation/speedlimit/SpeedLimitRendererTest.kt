package com.mapbox.navigation.ui.androidauto.navigation.speedlimit

import android.Manifest
import androidx.test.filters.SmallTest
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.rule.GrantPermissionRule
import com.mapbox.navigation.base.speed.model.SpeedLimitSign
import com.mapbox.navigation.ui.androidauto.testing.BitmapTestUtil
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
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
    )

    private val bitmapUtils = BitmapTestUtil(
        "expected_speed_limit_images",
        "test_speed_limit_images",
    )

    @Test
    fun speed_limit_120_speed_150_mutcd() {
        val bitmap = SpeedLimitBitmapRenderer().getBitmap(
            SpeedLimitSign.MUTCD,
            speedLimit = 120,
            speed = 150,
            warn = true,
        )
        bitmapUtils.assertBitmapsSimilar(testName, bitmap)
    }

    @Test
    fun speed_limit_120_speed_90_mutcd() {
        val bitmap = SpeedLimitBitmapRenderer().getBitmap(
            SpeedLimitSign.MUTCD,
            speedLimit = 120,
            speed = 90,
            warn = false,
        )
        bitmapUtils.assertBitmapsSimilar(testName, bitmap)
    }

    @Test
    fun speed_limit_65_speed_30_mutcd() {
        val bitmap = SpeedLimitBitmapRenderer().getBitmap(
            SpeedLimitSign.MUTCD,
            speedLimit = 65,
            speed = 30,
            warn = false,
        )
        bitmapUtils.assertBitmapsSimilar(testName, bitmap)
    }

    @Test
    fun speed_limit_5_speed_30_mutcd() {
        val bitmap = SpeedLimitBitmapRenderer().getBitmap(
            SpeedLimitSign.MUTCD,
            speedLimit = 5,
            speed = 30,
            warn = true,
        )
        bitmapUtils.assertBitmapsSimilar(testName, bitmap)
    }

    @Test
    fun speed_limit_5_speed_0_mutcd() {
        val bitmap = SpeedLimitBitmapRenderer().getBitmap(
            SpeedLimitSign.MUTCD,
            speedLimit = 5,
            speed = 0,
            warn = false,
        )
        bitmapUtils.assertBitmapsSimilar(testName, bitmap)
    }

    @Test
    fun speed_limit_unknown_speed_5_mutcd() {
        val bitmap = SpeedLimitBitmapRenderer().getBitmap(
            SpeedLimitSign.MUTCD,
            speedLimit = null,
            speed = 5,
            warn = false,
        )
        bitmapUtils.assertBitmapsSimilar(testName, bitmap)
    }

    @Test
    fun speed_limit_120_speed_150_vienna() {
        val bitmap = SpeedLimitBitmapRenderer().getBitmap(
            SpeedLimitSign.VIENNA,
            speedLimit = 120,
            speed = 150,
            warn = true,
        )
        bitmapUtils.assertBitmapsSimilar(testName, bitmap)
    }

    @Test
    fun speed_limit_120_speed_90_vienna() {
        val bitmap = SpeedLimitBitmapRenderer().getBitmap(
            SpeedLimitSign.VIENNA,
            speedLimit = 120,
            speed = 90,
            warn = false,
        )
        bitmapUtils.assertBitmapsSimilar(testName, bitmap)
    }

    @Test
    fun speed_limit_65_speed_30_vienna() {
        val bitmap = SpeedLimitBitmapRenderer().getBitmap(
            SpeedLimitSign.VIENNA,
            speedLimit = 65,
            speed = 30,
            warn = false,
        )
        bitmapUtils.assertBitmapsSimilar(testName, bitmap)
    }

    @Test
    fun speed_limit_5_speed_30_vienna() {
        val bitmap = SpeedLimitBitmapRenderer().getBitmap(
            SpeedLimitSign.VIENNA,
            speedLimit = 5,
            speed = 30,
            warn = true,
        )
        bitmapUtils.assertBitmapsSimilar(testName, bitmap)
    }

    @Test
    fun speed_limit_5_speed_0_vienna() {
        val bitmap = SpeedLimitBitmapRenderer().getBitmap(
            SpeedLimitSign.VIENNA,
            speedLimit = 5,
            speed = 0,
            warn = false,
        )
        bitmapUtils.assertBitmapsSimilar(testName, bitmap)
    }

    @Test
    fun speed_limit_unknown_speed_5_vienna() {
        val bitmap = SpeedLimitBitmapRenderer().getBitmap(
            SpeedLimitSign.VIENNA,
            speedLimit = null,
            speed = 5,
            warn = false,
        )
        bitmapUtils.assertBitmapsSimilar(testName, bitmap)
    }
}
