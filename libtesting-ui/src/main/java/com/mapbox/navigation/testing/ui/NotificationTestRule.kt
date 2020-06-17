package com.mapbox.navigation.testing.ui

import androidx.appcompat.app.AppCompatActivity
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until

open class NotificationTestRule<A : AppCompatActivity>(activityClass: Class<A>) :
    BaseTestRule<A>(activityClass) {

    companion object {
        private const val TIMEOUT = 3_000L
        private const val CLEAR_ALL_NOTIFICATIONS_RES_ID = "com.android.systemui:id/dismiss_text"
    }

    protected fun UiDevice.clearAllNotifications() {
        openNotification()
        findObject(By.res(CLEAR_ALL_NOTIFICATIONS_RES_ID)).click()
    }

    protected fun UiDevice.waitForNotification() {
        this.openNotification()
        this.wait(
            Until.hasObject(By.textStartsWith(appName)),
            TIMEOUT
        )
    }
}
