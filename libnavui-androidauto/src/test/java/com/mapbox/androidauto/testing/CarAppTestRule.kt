package com.mapbox.androidauto.testing

import com.mapbox.androidauto.AndroidAutoLog
import io.mockk.every
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Top level test rule for the android auto library.
 */
class CarAppTestRule : TestWatcher() {
    override fun starting(description: Description?) {
        mockkStatic(AndroidAutoLog::class)
        mockkObject(AndroidAutoLog)
        every { AndroidAutoLog.logAndroidAuto(any()) } just runs
        every { AndroidAutoLog.logAndroidAutoFailure(any(), any()) } just runs
    }

    override fun finished(description: Description?) {
        mockkStatic(AndroidAutoLog::class)
        mockkObject(AndroidAutoLog)
    }
}
