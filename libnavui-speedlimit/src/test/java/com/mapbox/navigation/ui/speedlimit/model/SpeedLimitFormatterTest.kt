package com.mapbox.navigation.ui.speedlimit.model

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.base.speed.model.SpeedLimitSign
import com.mapbox.navigation.base.speed.model.SpeedLimitUnit
import com.mapbox.navigation.testing.NavSDKRobolectricTestRunner
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@Config(sdk = [Build.VERSION_CODES.O_MR1])
@RunWith(NavSDKRobolectricTestRunner::class)
class SpeedLimitFormatterTest {

    private lateinit var ctx: Context

    @Before
    fun setup() {
        ctx = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun format_MUTCD() {
        val formatter = SpeedLimitFormatter(ctx)

        val result = formatter.format(
            UpdateSpeedLimitValue(
                35,
                SpeedLimitUnit.KILOMETRES_PER_HOUR,
                SpeedLimitSign.MUTCD,
                formatter
            )
        )

        assertEquals("MAX\n35", result)
    }

    @Test
    fun format_MUTCD_MPH() {
        val formatter = SpeedLimitFormatter(ctx)

        val result = formatter.format(
            UpdateSpeedLimitValue(
                35,
                SpeedLimitUnit.MILES_PER_HOUR,
                SpeedLimitSign.MUTCD,
                formatter
            )
        )

        assertEquals("MAX\n20", result)
    }

    @Test
    fun format_VIENNA() {
        val formatter = SpeedLimitFormatter(ctx)

        val result = formatter.format(
            UpdateSpeedLimitValue(
                35,
                SpeedLimitUnit.KILOMETRES_PER_HOUR,
                SpeedLimitSign.VIENNA,
                formatter
            )
        )

        assertEquals("35", result)
    }

    @Test
    fun format_VIENNA_MPH() {
        val formatter = SpeedLimitFormatter(ctx)

        val result = formatter.format(
            UpdateSpeedLimitValue(
                35,
                SpeedLimitUnit.MILES_PER_HOUR,
                SpeedLimitSign.VIENNA,
                formatter
            )
        )

        assertEquals("20", result)
    }
}
