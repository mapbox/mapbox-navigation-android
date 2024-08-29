package com.mapbox.navigation.ui.maps.puck

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.mapbox.maps.ImageHolder
import com.mapbox.navigation.testing.BuilderTest
import com.mapbox.navigation.ui.utils.internal.extensions.getBitmap
import com.mapbox.navigation.ui.utils.internal.extensions.withBlurEffect
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Before
import kotlin.reflect.KClass

class LocationPuckOptionsTest :
    BuilderTest<LocationPuckOptions, LocationPuckOptions.Builder>() {

    private val context: Context = mockk(relaxed = true)

    @Before
    fun setUp() {
        mockkObject(ImageHolder)
        mockkStatic(Drawable::withBlurEffect)
        mockkStatic(ContextCompat::getDrawable)
        mockkStatic(Drawable::getBitmap)
        every { ContextCompat.getDrawable(any(), any()) } returns mockk(relaxed = true) {
            every { withBlurEffect(any(), any()) } returns mockk(relaxed = true) {
                every { getBitmap() } returns mockk(relaxed = true)
            }
        }
        every { ImageHolder.from(any<Bitmap>()) } returns mockk()
    }

    @After
    fun tearDown() {
        unmockkStatic(Drawable::withBlurEffect)
        unmockkObject(ImageHolder)
        unmockkStatic(Drawable::getBitmap)
        unmockkStatic(ContextCompat::getDrawable)
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
