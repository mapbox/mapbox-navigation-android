package com.mapbox.navigation.testing

import java.util.concurrent.CountDownLatch

class BlockingSamCallback<T : Any> : (T) -> Unit {

    private var countDownLatch = CountDownLatch(1)
    private lateinit var result: T

    override fun invoke(result: T) {
        this.result = result
        countDownLatch.countDown()
    }

    fun getResultBlocking(): T {
        countDownLatch.await()
        return result
    }
}
