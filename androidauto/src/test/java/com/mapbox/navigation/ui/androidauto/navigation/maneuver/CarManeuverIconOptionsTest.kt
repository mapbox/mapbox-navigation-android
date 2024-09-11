package com.mapbox.navigation.ui.androidauto.navigation.maneuver

import android.content.Context
import android.graphics.Color
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.testing.BuilderTest
import com.mapbox.navigation.ui.androidauto.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.M])
class CarManeuverIconOptionsTest :
    BuilderTest<CarManeuverIconOptions, CarManeuverIconOptions.Builder>() {

    private val context: Context = ApplicationProvider.getApplicationContext()

    override fun getImplementationClass() = CarManeuverIconOptions::class

    override fun getFilledUpBuilder(): CarManeuverIconOptions.Builder {
        return CarManeuverIconOptions.Builder(context)
            .background(Color.BLUE)
            .styleRes(R.style.CarAppTheme)
    }

    @Test
    override fun trigger() {
        // only used to trigger JUnit4 to run this class if all test cases come from the parent
    }
}
