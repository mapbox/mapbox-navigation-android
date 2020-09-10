package com.mapbox.navigation.ui.feedback

import com.mapbox.navigation.testing.BuilderTest
import kotlin.reflect.KClass

class NavigationFeedbackOptionsTest :
    BuilderTest<NavigationFeedbackOptions, NavigationFeedbackOptions.Builder>() {
    override fun getImplementationClass(): KClass<NavigationFeedbackOptions> {
        return NavigationFeedbackOptions::class
    }

    override fun getFilledUpBuilder(): NavigationFeedbackOptions.Builder {
        return NavigationFeedbackOptions.Builder()
            .enableArrivalExperienceFeedback(true)
            .enableDetailedFeedbackAfterNavigation(true)
    }

    override fun trigger() {
        // only used to trigger JUnit4 to run this class if all test cases come from the parent
    }
}
