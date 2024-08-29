package com.mapbox.navigation.ui.components.tripprogress.model

import com.mapbox.navigation.testing.BuilderTest
import io.mockk.mockk
import org.junit.Test
import kotlin.reflect.KClass

class TripProgressViewOptionsTest :
    BuilderTest<TripProgressViewOptions, TripProgressViewOptions.Builder>() {

    override fun getImplementationClass(): KClass<TripProgressViewOptions> =
        TripProgressViewOptions::class

    override fun getFilledUpBuilder(): TripProgressViewOptions.Builder {
        return TripProgressViewOptions.Builder()
            .backgroundColor(1)
            .distanceRemainingIcon(1)
            .estimatedArrivalTimeIcon(1)
            .timeRemainingTextAppearance(1)
            .distanceRemainingTextAppearance(1)
            .estimatedArrivalTimeTextAppearance(1)
            .distanceRemainingIconTint(mockk())
            .estimatedArrivalTimeIconTint(mockk())
    }

    @Test
    override fun trigger() {
        // see comments
    }
}
