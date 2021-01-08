package com.mapbox.navigation.ui.base.model.tripprogress

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.base.internal.VoiceUnit
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class DistanceRemainingFormatterTest {

    lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
    }

    @Config(qualifiers = "en")
    @Test
    fun formatDistanceLargeDistanceImperialWithDefaultLocale() {
        val update = TripProgressUpdate(
            System.currentTimeMillis(),
            19312.1,
            50.0,
            100.0,
            21.0,
            111
        )
        val formatter = DistanceRemainingFormatter(ctx, VoiceUnit.IMPERIAL)

        val result = formatter.format(update)

        assertEquals("12 mi", result.toString())
    }

    @Config(qualifiers = "en")
    @Test
    fun formatDistanceLargeDistanceUnitTypeDefault() {
        val update = TripProgressUpdate(
            System.currentTimeMillis(),
            19312.1,
            50.0,
            100.0,
            21.0,
            111
        )
        val formatter = DistanceRemainingFormatter(ctx)

        val result = formatter.format(update)

        assertEquals("19 km", result.toString())
    }

    @Test
    fun formatDistanceJapaneseLocale() {
        val locale = Locale.JAPAN
        val update = TripProgressUpdate(
            System.currentTimeMillis(),
            55.3,
            50.0,
            100.0,
            21.0,
            111
        )

        val result = DistanceRemainingFormatter(ctx, VoiceUnit.IMPERIAL, locale).format(update)

        assertEquals("150 フィート", result.toString())
    }
}
