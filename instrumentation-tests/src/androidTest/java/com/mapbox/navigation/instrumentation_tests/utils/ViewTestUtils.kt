package com.mapbox.navigation.instrumentation_tests.utils

import android.view.View
import androidx.test.espresso.Espresso
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.Matcher

/**
 * Runs the code block on the app's main thread and blocks until the block returns.
 */
fun runOnMainSync(runnable: Runnable) =
    InstrumentationRegistry.getInstrumentation().runOnMainSync(runnable)

/**
 * Runs the code block on the app's main thread and blocks until the block returns.
 */
fun runOnMainSync(fn: () -> Unit) =
    InstrumentationRegistry.getInstrumentation().runOnMainSync(fn)

fun Int.loopFor(millis: Long) {
    Espresso.onView(ViewMatchers.withId(this)).perform(
        object : ViewAction {
            override fun getDescription(): String = "waiting for $millis"

            override fun getConstraints(): Matcher<View> = ViewMatchers.isEnabled()

            override fun perform(uiController: UiController?, view: View?) {
                uiController?.loopMainThreadForAtLeast(millis)
            }
        }
    )
}
