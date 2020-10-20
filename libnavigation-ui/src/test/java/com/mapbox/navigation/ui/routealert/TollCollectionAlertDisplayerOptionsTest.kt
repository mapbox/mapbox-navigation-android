package com.mapbox.navigation.ui.routealert

import com.mapbox.navigation.testing.BuilderTest
import io.mockk.mockk
import org.junit.Test

class TollCollectionAlertDisplayerOptionsTest :
    BuilderTest<TollCollectionAlertDisplayerOptions,
        TollCollectionAlertDisplayerOptions.Builder>() {
    override fun getImplementationClass() = TollCollectionAlertDisplayerOptions::class

    override fun getFilledUpBuilder() = TollCollectionAlertDisplayerOptions.Builder(
        mockk(relaxed = true)
    ).apply {
        drawable(mockk())
        properties(arrayOf(mockk()))
    }

    @Test
    override fun trigger() {
        // see docs
    }
}
