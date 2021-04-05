package com.mapbox.navigation.base.formatter

import android.content.Context
import com.mapbox.navigation.base.formatter.Rounding.INCREMENT_ONE_HUNDRED
import com.mapbox.navigation.testing.BuilderTest
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class DistanceFormatterOptionsTest :
    BuilderTest<DistanceFormatterOptions, DistanceFormatterOptions.Builder>() {

    private val context: Context = mockk(relaxed = true)
    private val applicationContext: Context = mockk(relaxed = true)

    override fun getImplementationClass() = DistanceFormatterOptions::class

    override fun getFilledUpBuilder(): DistanceFormatterOptions.Builder {
        every { context.applicationContext } returns applicationContext
        every { applicationContext.applicationContext } returns applicationContext

        return DistanceFormatterOptions.Builder(context)
            .locale(Locale.JAPAN)
            .roundingIncrement(INCREMENT_ONE_HUNDRED)
            .unitType(UnitType.IMPERIAL)
    }

    @Test
    override fun trigger() {
        // only used to trigger JUnit4 to run this class if all test cases come from the parent
    }
}
