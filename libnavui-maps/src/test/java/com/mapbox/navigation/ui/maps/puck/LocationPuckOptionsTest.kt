package com.mapbox.navigation.ui.maps.puck

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.mapbox.navigation.testing.BuilderTest
import com.mapbox.navigation.ui.utils.internal.extensions.withBlurEffect
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Before
import kotlin.reflect.KClass

class LocationPuckOptionsTest :
    BuilderTest<LocationPuckOptions, LocationPuckOptions.Builder>() {

    private val context: Context = mockk(relaxed = true)

    @Before
    fun setUp() {
        mockkStatic(Drawable::withBlurEffect)
        mockkStatic(ContextCompat::getDrawable)
        every { ContextCompat.getDrawable(any(), any()) } returns mockk {
            every { withBlurEffect(any(), any()) } returns mockk()
        }
    }

    override fun getImplementationClass(): KClass<LocationPuckOptions> = LocationPuckOptions::class

    override fun getFilledUpBuilder(): LocationPuckOptions.Builder {
        return LocationPuckOptions
            .Builder(context)
            .defaultPuck(mockk())
            .freeDrivePuck(mockk())
            .destinationPreviewPuck(mockk())
            .routePreviewPuck(mockk())
            .activeNavigationPuck(mockk())
            .arrivalPuck(mockk())
            .idlePuck(mockk())
    }

    override fun trigger() {
        // only used to trigger JUnit4 to run this class if all test cases come from the parent
    }
}
