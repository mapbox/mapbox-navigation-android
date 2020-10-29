package com.mapbox.navigation.core.trip.service

import androidx.test.espresso.Espresso.onIdle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import com.mapbox.navigation.core.test.R
import com.mapbox.navigation.core.tests.activity.TripServiceActivity
import com.mapbox.navigation.testing.ui.NotificationTest
import com.schibsted.spain.barista.interaction.BaristaClickInteractions.clickOn
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TripServiceTest :
    NotificationTest<TripServiceActivity>(TripServiceActivity::class.java) {

    @Before
    fun setup() {
        onIdle()
    }

    @Test
    fun checkNotificationContent() {
        R.id.btnStart.let {
            clickOn(it)
        }
        uiDevice.run {
            mockLocationUpdatesRule.pushLocationUpdate()
            val etaContent =
                By.res("com.mapbox.navigation.core.test:id/etaContent")
            val freeDriveText =
                By.res("com.mapbox.navigation.core.test:id/freeDriveText")
            waitForMyForegroundNotification(freeDriveText)

            assertFalse(hasObject(etaContent))
            assertTrue(hasObject(freeDriveText))
            pressBack()
        }
    }
}
