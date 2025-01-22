package com.mapbox.navigation.core.trip.service

import androidx.test.espresso.Espresso.onIdle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.mapbox.navigation.core.test.R
import com.mapbox.navigation.core.tests.activity.TripServiceActivity
import com.mapbox.navigation.testing.ui.NotificationTest
import com.mapbox.navigation.testing.ui.utils.loopFor
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

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
        // give notification time to show up for the first time
        // it can take a significant amount of time on Android 12
        // while it's faster for subsequent runs
        R.id.container.loopFor(TimeUnit.SECONDS.toMillis(15))
        uiDevice.run {
            val etaContent =
                By.res("com.mapbox.navigation.core.test:id/etaContent")
            val freeDriveText =
                By.res("com.mapbox.navigation.core.test:id/freeDriveText")

            openNotification()
            searchForMyForegroundNotification(freeDriveText)

            assertFalse(hasObject(etaContent))
            assertTrue(hasObject(freeDriveText))
            pressBack()
        }
    }
}
