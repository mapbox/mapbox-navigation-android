package com.mapbox.navigation.examples.activity

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.examples.core.TripServiceActivityKt
import com.mapbox.navigation.testing.ui.NotificationTestRule
import com.schibsted.spain.barista.assertion.BaristaEnabledAssertions.assertEnabled
import com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.schibsted.spain.barista.interaction.BaristaClickInteractions.clickOn
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TripServiceActivityKtTest :
    NotificationTestRule<TripServiceActivityKt>(TripServiceActivityKt::class.java) {

    companion object {
        /**
         * The target app package.
         */
        private val TARGET_PACKAGE: String =
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().context.packageName
    }

    @Test
    fun checkStartButtonAccessible() {
        awaitForView("toggleNotification")
        R.id.toggleNotification.let {
            assertDisplayed(it)
            assertEnabled(it)
            assertContains(it, "Start")
        }
    }

    @Test
    fun checkNotificationViewContent() {
        awaitForView("notifyTextView")
        R.id.notifyTextView.let {
            assertContains(it, "")
        }

        awaitForView("toggleNotification")
        R.id.toggleNotification.let {
            clickOn(it)
        }

        awaitForView("notifyTextView")
        R.id.notifyTextView.let {
            assertDisplayed(it)
            assertContains(it, "Time elapsed: + ")
        }
    }

    @Test
    fun checkNotificationContent() {
        awaitForView("toggleNotification")
        R.id.toggleNotification.let {
            clickOn(it)
        }

        awaitForView("notifyTextView")
        R.id.notifyTextView.let {
            assertDisplayed(it)
            assertContains(it, "Time elapsed: + ")
        }

        uiDevice.run {
            openNotification()
            awaitForView("etaContent")
            awaitForView("freeDriveText")
            val etaContent = By.res("com.mapbox.navigation.examples:id/etaContent")
            val freeDriveText = By.res("com.mapbox.navigation.examples:id/freeDriveText")
            wait(Until.hasObject(etaContent), 1000)
            wait(Until.hasObject(freeDriveText), 1000)

            assertFalse(hasObject(etaContent))
            assertTrue(hasObject(freeDriveText))
            pressBack()
        }
    }

    private inline fun awaitForView(resName: String, timeout: Long = 1000) {
        uiDevice.wait(Until.hasObject(By.res(TARGET_PACKAGE, resName)), timeout)
    }
}
