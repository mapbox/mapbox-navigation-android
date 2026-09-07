package com.mapbox.navigation.base.internal.performance

import androidx.annotation.RestrictTo
import org.jetbrains.annotations.TestOnly
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource
import kotlin.time.measureTime

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@OptIn(ExperimentalTime::class)
class AsyncSection(
    internal val name: String,
    internal val id: Int,
    internal val startMark: TimeSource.Monotonic.ValueTimeMark,
)

@OptIn(ExperimentalTime::class)
private val emptyAsyncSection = AsyncSection("empty", -1, TimeSource.Monotonic.markNow())

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
object PerformanceTracker {

    internal fun addObserver(observer: PerformanceObserver) {
        performanceObservers.add(observer)
    }

    internal fun removeObserver(observer: PerformanceObserver) {
        performanceObservers.remove(observer)
    }

    @TestOnly
    internal fun resetStateForTests() {
        asyncSectionIdCounter.set(0)
        performanceObservers.clear()
    }

    private val performanceObservers = CopyOnWriteArraySet<PerformanceObserver>()
    private val asyncSectionIdCounter = AtomicInteger(0)

    val trackingIsActive get() = performanceObservers.isNotEmpty()

    @Deprecated("Use trackPerformanceSync instead")
    fun syncSectionStarted(name: String) {
        performanceObservers.forEach {
            it.syncSectionStarted(name)
        }
    }

    @Deprecated("Use trackPerformanceSync instead")
    fun syncSectionCompleted(name: String, duration: Duration?) {
        performanceObservers.forEach {
            it.syncSectionCompleted(name, duration)
        }
    }

    @OptIn(ExperimentalTime::class)
    fun asyncSectionStarted(name: String): AsyncSection {
        if (!trackingIsActive) {
            return emptyAsyncSection
        }
        val id = asyncSectionIdCounter.incrementAndGet()
        val startMark = TimeSource.Monotonic.markNow()
        performanceObservers.forEach {
            it.asyncSectionStarted(name, id)
        }
        return AsyncSection(name, id, startMark)
    }

    @OptIn(ExperimentalTime::class)
    fun asyncSectionCompleted(section: AsyncSection) {
        if (section === emptyAsyncSection) {
            return
        }
        val duration = section.startMark.elapsedNow()
        performanceObservers.forEach {
            it.asyncSectionFinished(section.name, section.id, duration)
        }
    }

    /**
     * Track a synchronous (non-suspending) block.
     *
     * Observers must know if a section is sync vs async to emit correct trace calls
     * (e.g., beginSection/endSection). Use only for code that completes on the same
     * call stack; do not pass a suspending block.
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

    /**
     * Track a suspending/async block.
     *
     * Observers need the async signal to use async trace APIs
     * (e.g., beginAsyncSection/endAsyncSection with an id). Use when work may
     * suspend, hop threads, or outlive the caller.
     */
    @OptIn(ExperimentalTime::class)
    suspend fun <R> trackPerformanceAsync(name: String, block: suspend () -> R): R {
        if (!trackingIsActive) {
            return block()
        }
        val section = asyncSectionStarted(name)
        val result: R
        try {
            result = block()
        } finally {
            asyncSectionCompleted(section)
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

    /**
     * Notifies about new asynchronous section start.
     * @param name the name of the async section
     * @param id unique numeric identifier for the async section
     * @see [asyncSectionFinished]
     */
    fun asyncSectionStarted(name: String, id: Int)

    /**
     * Notifies about asynchronous section end.
     * @param name the name of the async section
     * @param id unique numeric identifier for the async section
     * @param duration the duration of the async section, null if not available
     * @see [asyncSectionStarted]
     */
    fun asyncSectionFinished(name: String, id: Int, duration: Duration?)
}
