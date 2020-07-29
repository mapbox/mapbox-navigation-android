package com.mapbox.navigation.base.trip.model

import com.mapbox.navigation.testing.BuilderTest
import io.mockk.mockk
import kotlin.reflect.KClass
import org.junit.Test

class RouteLegProgressTest : BuilderTest<RouteLegProgress, RouteLegProgress.Builder>() {
    override fun getImplementationClass(): KClass<RouteLegProgress> = RouteLegProgress::class

    override fun getFilledUpBuilder(): RouteLegProgress.Builder {
        return RouteLegProgress.Builder()
            .currentStepProgress(mockk(relaxed = true))
            .distanceRemaining(123f)
            .distanceTraveled(999f)
            .durationRemaining(456.0)
            .fractionTraveled(789f)
            .routeLeg(mockk(relaxed = true))
            .upcomingStep(mockk(relaxed = true))
            .legIndex(123)
    }

    @Test
    override fun trigger() {
        // only used to trigger JUnit4 to run this class if all test cases come from the parent
    }
}
