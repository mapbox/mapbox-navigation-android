package com.mapbox.navigation.ui.androidauto.testing

import androidx.car.app.OnDoneCallback
import androidx.car.app.serialization.Bundleable
import org.junit.Assert.assertTrue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Used to help test interactions with the car screen templates.
 */
class TestOnDoneCallback : OnDoneCallback {
    private val countDownLatch = CountDownLatch(1)
    private var success: Boolean = false
    private var failure: Boolean = false

    override fun onSuccess(response: Bundleable?) {
        success = true
        countDownLatch.countDown()
    }

    override fun onFailure(response: Bundleable) {
        failure = true
        countDownLatch.countDown()
    }

    fun assertSuccess() {
        countDownLatch.await(1, TimeUnit.SECONDS)
        assertTrue(success)
    }
}
