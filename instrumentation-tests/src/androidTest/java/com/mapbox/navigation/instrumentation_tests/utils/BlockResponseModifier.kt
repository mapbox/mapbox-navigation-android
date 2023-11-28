package com.mapbox.navigation.instrumentation_tests.utils

import java.util.concurrent.CountDownLatch

class BlockResponseModifier : (String) -> String {

    private val countDownLatch = CountDownLatch(1)

    fun release() {
        countDownLatch.countDown()
    }

    override fun invoke(p1: String): String {
        countDownLatch.await()
        return p1
    }
}