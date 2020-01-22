package com.mapbox.navigation.examples.activity

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.testing.ui.rules.NotificationTestRule
import com.schibsted.spain.barista.assertion.BaristaEnabledAssertions.assertEnabled
import com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.schibsted.spain.barista.interaction.BaristaClickInteractions.clickOn
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TripServiceActivityKtTest :
    NotificationTestRule<TripServiceActivityKt>(TripServiceActivityKt::class.java) {

    @Test
    fun checkStartButtonAccessible() {
        R.id.toggleNotification.let {
            assertDisplayed(it)
            assertEnabled(it)
            assertContains(it, "Start")
        }
    }

    @Test
    fun checkNotificationViewContent() {
        R.id.notifyTextView.let {
            assertContains(it, "")
        }

        R.id.toggleNotification.let {
            clickOn(it)
        }

        R.id.notifyTextView.let {
            assertDisplayed(it)
            assertContains(it, "Time elapsed: + ")
        }
    }

    @Test
    fun checkNotificationContent() {
        R.id.toggleNotification.let {
            clickOn(it)
        }

        R.id.notifyTextView.let {
            assertDisplayed(it)
            assertContains(it, "Time elapsed: + ")
        }

        uiDevice.run {
            openNotification()
            wait(Until.hasObject(By.res("com.mapbox.driver.navigation.examples:id/notificationDistanceText")), 1000)

            val message = findObject(By.res("com.mapbox.driver.navigation.examples:id/notificationDistanceText")).text

            assertEquals("100 m", message)
            pressBack()
        }
    }
}
