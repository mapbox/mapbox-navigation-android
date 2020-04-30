package com.mapbox.navigation.examples.activity

import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.examples.core.TripServiceActivityKt
import com.mapbox.navigation.examples.util.OnMapReadyIdlingResource
import com.mapbox.navigation.testing.ui.NotificationTestRule
import com.schibsted.spain.barista.assertion.BaristaEnabledAssertions.assertEnabled
import com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.schibsted.spain.barista.interaction.BaristaClickInteractions.clickOn
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TripServiceActivityKtTest :
    NotificationTestRule<TripServiceActivityKt>(TripServiceActivityKt::class.java) {

    lateinit var mapIdlingResource: IdlingResource

    @Before
    fun setup() {
        mapIdlingResource = OnMapReadyIdlingResource(activity)
        IdlingRegistry.getInstance().register(mapIdlingResource)
        Espresso.onIdle()
    }

    @After
    fun shutdown() {
        IdlingRegistry.getInstance().unregister(mapIdlingResource)
    }

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
            val etaContent = By.res("com.mapbox.navigation.examples:id/etaContent")
            val freeDriveText = By.res("com.mapbox.navigation.examples:id/freeDriveText")
            wait(Until.hasObject(etaContent), 1000)
            wait(Until.hasObject(freeDriveText), 1000)

            assertFalse(hasObject(etaContent))
            assertTrue(hasObject(freeDriveText))
            pressBack()
        }
    }
}
