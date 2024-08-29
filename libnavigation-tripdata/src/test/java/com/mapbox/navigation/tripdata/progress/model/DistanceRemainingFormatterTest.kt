package com.mapbox.navigation.tripdata.progress.model

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.formatter.UnitType
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class DistanceRemainingFormatterTest {

    private val ctx: Context = ApplicationProvider.getApplicationContext()

    @Config(qualifiers = "en")
    @Test
    fun formatDistanceLargeDistanceImperialWithDefaultLocale() {
        val formatter = DistanceRemainingFormatter(
            DistanceFormatterOptions.Builder(ctx)
                .unitType(UnitType.IMPERIAL)
                .build(),
        )

        val result = formatter.format(19312.1)

        assertEquals("12 mi", result.toString())
    }

    @Config(qualifiers = "en")
    @Test
    fun formatDistanceLargeDistanceUnitTypeDefault() {
        val formatter = DistanceRemainingFormatter(
            DistanceFormatterOptions.Builder(ctx)
                .build(),
        )

        val result = formatter.format(19312.1)

        assertEquals("19 km", result.toString())
    }

    @Test
    fun formatDistanceJapaneseLocale() {
        val formatter = DistanceRemainingFormatter(
            DistanceFormatterOptions.Builder(ctx)
                .locale(Locale.JAPAN)
                .unitType(UnitType.IMPERIAL)
                .build(),
        )

        val result = formatter.format(55.3)

        assertEquals("150 フィート", result.toString())
    }
}
