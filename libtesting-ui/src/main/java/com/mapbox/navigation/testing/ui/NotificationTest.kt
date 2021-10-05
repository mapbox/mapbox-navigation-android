package com.mapbox.navigation.testing.ui

import android.graphics.Point
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until

open class NotificationTest<A : AppCompatActivity>(activityClass: Class<A>) :
    BaseTest<A>(activityClass) {

    companion object {
        private const val TIMEOUT = 1_000L
        private const val NOTIFICATION_CHECK_MAX_RETRIES = 3
    }

    /**
     * Waits for a notification to be available that meets all of the conditions via the
     * [Until.hasObject].
     * If notification is not visible, the automation scrolls the notification tab down up to
     * three times to find it.
     */
    protected fun UiDevice.waitForMyForegroundNotification(vararg selectors: BySelector) {
        this.openNotification()
        var notificationChecksCount = 0
        while (!notificationAvailable(*selectors).all { it }
            && notificationChecksCount < NOTIFICATION_CHECK_MAX_RETRIES) {
            scrollUp()
            notificationChecksCount++
        }
    }

    /**
     * Check if all of the conditions are met.
     */
    private fun UiDevice.notificationAvailable(vararg selectors: BySelector) = selectors.map {
        this.wait(
            Until.hasObject(it),
            TIMEOUT
        )
    }

    /**
     * Scrolls up by a quarter of the screen up, starting from center.
     */
    private fun UiDevice.scrollUp() {
        val screenSize = Point()
        activityRule.activity.windowManager.defaultDisplay.getRealSize(screenSize)
        this.drag(
            screenSize.x / 2,
            screenSize.y / 2,
            screenSize.x / 2,
            screenSize.y / 4,
            100
        )
    }

    override fun setupMockLocation(): Location = mockLocationUpdatesRule.generateLocationUpdate()
}
