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

    fun syncSectionStarted(name: String) {
        performanceObservers.forEach {
            it.syncSectionStarted(name)
        }
    }

    fun syncSectionCompleted(name: String, duration: Duration?) {
        performanceObservers.forEach {
            it.syncSectionCompleted(name, duration)
        }
    }

    /**
     * Tracks performance of a synchronous block of code.
     * @param block should be a synchronous function. Suspend function might break internal logic.
     */
    @OptIn(ExperimentalTime::class)
    inline fun <R> trackPerformanceSync(name: String, block: () -> R): R {
        if (!trackingIsActive) {
            return block()
        }
        syncSectionStarted(name)
        val result: R
        var executionTime: Duration? = null
        try {
            executionTime = measureTime {
                result = block()
            }
        } finally {
            syncSectionCompleted(name, executionTime)
        }
        return result
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
internal interface PerformanceObserver {
    /**
     * Notifies about new synchronous section start.
     * @see [syncSectionCompleted]
     */
    fun syncSectionStarted(name: String)

    /**
     * Notifies about synchronous section end.
     * @param name always matches last started synchronous section
     * @see [syncSectionStarted]
     */
    fun syncSectionCompleted(name: String, duration: Duration?)
}
