package com.mapbox.navigation.base.internal.performance

import androidx.annotation.RestrictTo
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
object PerformanceTracker {

    internal fun addObserver(observer: PerformanceObserver) {
        performanceObservers.add(observer)
    }

    internal fun removeObserver(observer: PerformanceObserver) {
        performanceObservers.remove(observer)
    }

    private val performanceObservers = CopyOnWriteArraySet<PerformanceObserver>()

    val trackingIsActive get() = performanceObservers.isNotEmpty()

    fun sectionStarted(name: String) {
        performanceObservers.forEach {
            it.sectionStarted(name)
        }
    }

    fun sectionCompleted(name: String, duration: Duration?) {
        performanceObservers.forEach {
            it.sectionCompleted(name, duration)
        }
    }

    @OptIn(ExperimentalTime::class)
    inline fun <R> trackPerformance(name: String, block: () -> R): R {
        if (!trackingIsActive) {
            return block()
        }
        sectionStarted(name)
        val result: R
        var executionTime: Duration? = null
        try {
            executionTime = measureTime {
                result = block()
            }
        } finally {
            sectionCompleted(name, executionTime)
        }
        return result
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
internal interface PerformanceObserver {
    fun sectionStarted(name: String)
    fun sectionCompleted(name: String, duration: Duration?)
}
