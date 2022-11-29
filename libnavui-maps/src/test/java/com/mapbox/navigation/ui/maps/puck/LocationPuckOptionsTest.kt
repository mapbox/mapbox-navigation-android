package com.mapbox.navigation.ui.maps.puck

import android.content.Context
import com.mapbox.navigation.testing.BuilderTest
import io.mockk.mockk
import kotlin.reflect.KClass

class LocationPuckOptionsTest :
    BuilderTest<LocationPuckOptions, LocationPuckOptions.Builder>() {

    private val context: Context = mockk(relaxed = true)

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
