package com.mapbox.navigation.base.internal.performance

import androidx.annotation.RestrictTo
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
object PerformanceTracker {

    private val sectionNumberCounter = AtomicInteger(0)

    internal fun addObserver(observer: PerformanceObserver) {
        performanceObservers.add(observer)
    }

    internal fun removeObserver(observer: PerformanceObserver) {
        performanceObservers.remove(observer)
    }

    private val performanceObservers = CopyOnWriteArraySet<PerformanceObserver>()

    val trackingIsActive get() = performanceObservers.isNotEmpty()

    fun sectionStarted(name: String): Int {
        val id = sectionNumberCounter.incrementAndGet()
        performanceObservers.forEach {
            it.sectionStarted(name, id)
        }
        return id
    }

    fun sectionCompleted(name: String, id: Int, duration: Duration?) {
        performanceObservers.forEach {
            it.sectionCompleted(name, id, duration)
        }
    }

    @OptIn(ExperimentalTime::class)
    inline fun <R> trackPerformance(name: String, block: () -> R): R {
        if (!trackingIsActive) {
            return block()
        }
        val id = sectionStarted(name)
        val result: R
        var executionTime: Duration? = null
        try {
            executionTime = measureTime {
                result = block()
            }
        } finally {
            sectionCompleted(name, id, executionTime)
        }
        return result
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
internal interface PerformanceObserver {
    fun sectionStarted(name: String, id: Int)
    fun sectionCompleted(name: String, id: Int, duration: Duration?)
}
