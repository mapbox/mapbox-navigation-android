package com.mapbox.navigation.instrumentation_tests.ui.navigation_view

import androidx.test.espresso.matcher.ViewMatchers
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.utils.gestures.GesturesUiTestUtils
import com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions
import com.schibsted.spain.barista.interaction.BaristaClickInteractions
import com.schibsted.spain.barista.internal.performActionOnView
import org.junit.Test

class RecenterButtonTest : SimpleNavigationViewTest() {

    @Test
    fun button_visibility() {
        BaristaVisibilityAssertions.assertNotDisplayed(R.id.recenterBtn)
        performActionOnView(
            ViewMatchers.withId(R.id.navigationView),
            GesturesUiTestUtils.move(500f, 500f)
        )
        BaristaVisibilityAssertions.assertDisplayed(R.id.recenterBtn)
        BaristaClickInteractions.clickOn(R.id.recenterBtn)
        BaristaVisibilityAssertions.assertNotDisplayed(R.id.recenterBtn)
    }
}
