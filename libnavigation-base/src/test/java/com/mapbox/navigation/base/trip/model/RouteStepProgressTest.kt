package com.mapbox.navigation.base.trip.model

import com.mapbox.navigation.testing.BuilderTest
import io.mockk.mockk
import org.junit.Test
import kotlin.reflect.KClass

class RouteStepProgressTest : BuilderTest<RouteStepProgress, RouteStepProgress.Builder>() {
    override fun getImplementationClass(): KClass<RouteStepProgress> = RouteStepProgress::class

    override fun getFilledUpBuilder(): RouteStepProgress.Builder {
        return RouteStepProgress.Builder()
            .stepIndex(123)
            .intersectionIndex(987)
            .instructionIndex(101112)
            .step(mockk(relaxed = true))
            .stepPoints(mockk(relaxed = true))
            .distanceRemaining(456f)
            .distanceTraveled(789f)
            .fractionTraveled(101112f)
            .durationRemaining(131415.0)
    }

    @Test
    override fun trigger() {
        // only used to trigger JUnit4 to run this class if all test cases come from the parent
    }
}
