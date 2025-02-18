package com.mapbox.navigation.mapgpt.core.performance

import com.mapbox.navigation.mapgpt.core.common.SharedLog
import com.mapbox.navigation.mapgpt.core.performance.SharedPerformance.complete
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Entry point for creating performance metrics. This object can be used to measure or trace the
 * time between events from different parts of the sdk. Be sure to [complete] the instances when you
 * use this.
 *
 * ## When to use [DashMeasure] vs [DashTrace]:
 * [DashMeasure] is for debug logging and can be used as an enhanced local logger.
 *  - This does not have indexing limitations so you can use it to log unique values or identifiers.
 *  - Useful for debug logging that can be helpful for understanding the performance.
 * [DashTrace] is used for uploading performance metrics to service dashboards like Firebase.
 *  - Do not use this for logging unique values or identifiers as it has indexing limitations.
 *  - Useful for storing performance metrics in a database.
 */
object SharedPerformance {

    private val prefixes = MutableStateFlow<Set<String>>(emptySet())
    private val measures = MutableStateFlow<Map<String, DashMeasure>>(emptyMap())
    private val dashTraceFactory = MutableStateFlow<DashTraceFactory?>(null)
    private val traces = MutableStateFlow<Map<TraceName, DashTrace>>(emptyMap())

    /**
     * Set the factory to use to create a [DashTrace] instance. This allows you to use your own
     * implementation of [DashTrace]. For example, you can implement FirebaseTrace to track the
     * performance of your app in Firebase. Setting to null will disable tracing.
     */
    fun setTraceFactory(factory: DashTraceFactory?) {
        dashTraceFactory.value = factory
    }

    /**
     * Add a prefix to enable performance tracking for. [DashMeasure] or [DashTrace] created
     * through this object will be operational when the name starts with any of the prefixes.
     */
    fun enable(prefixes: String) = apply {
        SharedPerformance.prefixes.update { it + prefixes }
    }

    /**
     * Adds prefixes to enable performance tracking for. [DashMeasure] or [DashTrace] created
     * through this object will be operational when the name starts with any of the prefixes.
     */
    fun enableAll(prefixes: Set<String>) = apply {
        SharedPerformance.prefixes.update { it + prefixes }
    }

    /**
     * Remove a prefix to disable performance tracking for. [DashMeasure] or [DashTrace] created
     * through this object will be disabled when the name starts with the prefix.
     */
    fun disable(prefix: String) = apply {
        prefixes.update { it - prefix }
        measures.value.keys.filter { it.startsWith(prefix) }.forEach {
            complete(measure(it))
        }
        traces.value.keys.filter { it.snakeCase.startsWith(prefix) }.forEach {
            complete(start(it))
        }
    }

    /**
     * Run a block of code only if the prefix is enabled.
     * Returns the result of the block if the prefix is enabled, otherwise null.
     */
    fun <R> runWhenEnabled(name: String, block: () -> R): R? {
        return if (prefixes.value.any { name.startsWith(it) }) {
            block()
        } else {
            SharedLog.i(TAG) { "Prefix for $name is not enabled" }
            null
        }
    }

    /**
     * Returns a sharable instance of [DashMeasure].
     * Must be enabled by a prefix in order to run real measurements.
     * Caller must call [complete] when done with the instance.
     */
    fun measure(name: String): DashMeasure {
        return runWhenEnabled(name) {
            measures.value[name] ?: run {
                val newMeasure = createMeasure(name)
                measures.update { it + (newMeasure.name to newMeasure) }
                newMeasure
            }
        } ?: NoDashMeasure
    }

    /**
     * Returns a sharable instance of [DashTrace].
     * Must be enabled by a prefix in order to run real measurements.
     * Caller must call [complete] when done with the instance.
     */
    fun start(name: TraceName): DashTrace {
        return runWhenEnabled(name.snakeCase) {
            traces.value[name] ?: run {
                val newTrace = createTrace(name)
                traces.update { it + (newTrace.name to newTrace) }
                newTrace.start()
            }
        } ?: NoDashTrace
    }

    /**
     * Removes the [DashMeasure] instance from the shared performance tracking.
     *
     * @param block optional block to log before removing the measure.
     */
    fun complete(dashMeasure: DashMeasure, block: (() -> String)? = null) {
        block?.let { dashMeasure.log(block) } ?: dashMeasure.log { "stop" }
        measures.update { it - dashMeasure.name }
    }

    /**
     * Removes and tracks the completion of the [DashTrace] instance.
     * Removes and tracks the completion of the [DashMeasure] instance.
     *
     * @param name associated to the instance to remove.
     * @param block optional block to log before removing the trace.
     */
    fun complete(name: String, block: (() -> String)? = null) {
        val removeTraces = traces.value.filter { it.value.name.snakeCase.startsWith(name) }
        traces.update { it - removeTraces.keys }
        removeTraces.values.forEach { it.stop() }
        measures.value[name]?.let { complete(it, block) }
    }

    /**
     * Removes and tracks the completion of the [DashTrace] instance.
     */
    fun complete(dashTrace: DashTrace) {
        dashTrace.stop()
        traces.update { it - dashTrace.name }
    }

    fun complete(traceName: TraceName) {
        traces.value[traceName]?.let { complete(it) }
    }

    /**
     * Removes and does not track the completion of the [DashMeasure] instance.
     * Removes and does not track the completion of the [DashTrace] instance.
     */
    fun discard(prefix: String) {
        val discardTraces = traces.value.filter { it.value.name.snakeCase.startsWith(prefix) }
        traces.update { it - discardTraces.keys }
        val discardMeasures = measures.value.filter { it.key.startsWith(prefix) }
        measures.update { it - discardMeasures.keys }
    }

    /**
     * Caller manages the instance of the [DashTrace].
     * Must be enabled by a prefix in order to run real measurements.
     * Instance is not held by the [SharedPerformance] object.
     */
    fun newTrace(name: TraceName): DashTrace {
        return runWhenEnabled(name.snakeCase) { createTrace(name) } ?: NoDashTrace
    }

    /**
     * Caller manages the instance of the [DashMeasure].
     * Must be enabled by a prefix in order to run real measurements.
     * Instance is not held by the [SharedPerformance] object.
     */
    fun newMeasure(name: String): DashMeasure {
        return runWhenEnabled(name) { createMeasure(name) } ?: NoDashMeasure
    }

    /**
     * Run a single block of code with the [DashMeasure].
     * Must be enabled by a prefix in order to run real measurements.
     * Instance is not held by the [SharedPerformance] object.
     */
    fun <R> runMeasure(name: String, block: (DashMeasure) -> R): R {
        val measure = newMeasure(name)
        measure.start { "start" }
        return try {
            block(measure)
        } finally {
            measure.log { "stop" }
        }
    }

    /**
     * Run a single block of code with the [DashTrace].
     * Must be enabled by a prefix in order to run real measurements.
     * Instance is not held by the [SharedPerformance] object.
     */
    fun <R> runTrace(name: TraceName, block: (DashTrace) -> R): R {
        val trace = newTrace(name)
        trace.start()
        return try {
            block(trace)
        } finally {
            trace.stop()
        }
    }

    /**
     * Does not require a prefix to be enabled.
     * Instance is not held by the [SharedPerformance] object.
     */
    private fun createMeasure(name: String): DashMeasure {
        return DashMeasureImpl(name)
    }

    /**
     * Does not require a prefix to be enabled.
     * Instance is not held by the [SharedPerformance] object.
     */
    private fun createTrace(name: TraceName): DashTrace {
        return dashTraceFactory.value?.create(name)?.let { DashTraceWrapper(it) } ?: NoDashTrace
    }

    private const val TAG = "SharedPerformance"
}
