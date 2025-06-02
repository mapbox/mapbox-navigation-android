package com.mapbox.navigation.base.performance

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.performance.SharedPerformance.complete
import com.mapbox.navigation.utils.internal.logI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Entry point for creating performance metrics. This object can be used to measure or trace the
 * time between events from different parts of the sdk. Be sure to [complete] the instances when you
 * use this.
 *
 * ## When to use [Measure] vs [Trace]:
 * [Measure] is for debug logging and can be used as an enhanced local logger.
 *  - This does not have indexing limitations so you can use it to log unique values or identifiers.
 *  - Useful for debug logging that can be helpful for understanding the performance.
 * [Trace] is used for uploading performance metrics to service dashboards like Firebase.
 *  - Do not use this for logging unique values or identifiers as it has indexing limitations.
 *  - Useful for storing performance metrics in a database.
 */
@ExperimentalPreviewMapboxNavigationAPI
object SharedPerformance {

    private val prefixes = MutableStateFlow<Set<String>>(emptySet())
    private val measures = MutableStateFlow<Map<String, Measure>>(emptyMap())
    private val traceFactory = MutableStateFlow<TraceFactory?>(null)
    private val traces = MutableStateFlow<Map<TraceName, Trace>>(emptyMap())

    /**
     * Set the factory to use to create a [Trace] instance. This allows you to use your own
     * implementation of [Trace]. For example, you can implement FirebaseTrace to track the
     * performance of your app in Firebase. Setting to null will disable tracing.
     */
    fun setTraceFactory(factory: TraceFactory?) {
        traceFactory.value = factory
    }

    /**
     * Add a prefix to enable performance tracking for. [Measure] or [Trace] created
     * through this object will be operational when the name starts with any of the prefixes.
     */
    fun enable(prefixes: String) = apply {
        SharedPerformance.prefixes.update { it + prefixes }
    }

    /**
     * Adds prefixes to enable performance tracking for. [Measure] or [Trace] created
     * through this object will be operational when the name starts with any of the prefixes.
     */
    fun enableAll(prefixes: Set<String>) = apply {
        SharedPerformance.prefixes.update { it + prefixes }
    }

    /**
     * Remove a prefix to disable performance tracking for. [Measure] or [Trace] created
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
            logI(TAG) { "Prefix for $name is not enabled" }
            null
        }
    }

    /**
     * Returns a sharable instance of [Measure].
     * Must be enabled by a prefix in order to run real measurements.
     * Caller must call [complete] when done with the instance.
     */
    fun measure(name: String): Measure {
        return runWhenEnabled(name) {
            measures.value[name] ?: run {
                val newMeasure = createMeasure(name)
                measures.update { it + (newMeasure.name to newMeasure) }
                newMeasure
            }
        } ?: NoMeasure
    }

    /**
     * Returns a sharable instance of [Trace].
     * Must be enabled by a prefix in order to run real measurements.
     * Caller must call [complete] when done with the instance.
     */
    fun start(name: TraceName): Trace {
        return runWhenEnabled(name.snakeCase) {
            traces.value[name] ?: run {
                val newTrace = createTrace(name)
                traces.update { it + (newTrace.name to newTrace) }
                newTrace.start()
            }
        } ?: NoTrace
    }

    /**
     * Removes the [Measure] instance from the shared performance tracking.
     *
     * @param block optional block to log before removing the measure.
     */
    fun complete(measure: Measure, block: (() -> String)? = null) {
        block?.let { measure.log(block) } ?: measure.log { "stop" }
        measures.update { it - measure.name }
    }

    /**
     * Removes and tracks the completion of the [Trace] instance.
     * Removes and tracks the completion of the [Measure] instance.
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
     * Removes and tracks the completion of the [Trace] instance.
     */
    fun complete(trace: Trace) {
        trace.stop()
        traces.update { it - trace.name }
    }

    /**
     * Removes and tracks the completion of the [TraceName] instance.
     *
     * @param traceName trace name key
     */
    fun complete(traceName: TraceName) {
        traces.value[traceName]?.let { complete(it) }
    }

    /**
     * Removes and does not track the completion of the [Measure] instance.
     * Removes and does not track the completion of the [Trace] instance.
     */
    fun discard(prefix: String) {
        val discardTraces = traces.value.filter { it.value.name.snakeCase.startsWith(prefix) }
        traces.update { it - discardTraces.keys }
        val discardMeasures = measures.value.filter { it.key.startsWith(prefix) }
        measures.update { it - discardMeasures.keys }
    }

    /**
     * Caller manages the instance of the [Trace].
     * Must be enabled by a prefix in order to run real measurements.
     * Instance is not held by the [SharedPerformance] object.
     */
    fun newTrace(name: TraceName): Trace {
        return runWhenEnabled(name.snakeCase) { createTrace(name) } ?: NoTrace
    }

    /**
     * Caller manages the instance of the [Measure].
     * Must be enabled by a prefix in order to run real measurements.
     * Instance is not held by the [SharedPerformance] object.
     */
    fun newMeasure(name: String): Measure {
        return runWhenEnabled(name) { createMeasure(name) } ?: NoMeasure
    }

    /**
     * Run a single block of code with the [Measure].
     * Must be enabled by a prefix in order to run real measurements.
     * Instance is not held by the [SharedPerformance] object.
     */
    fun <R> runMeasure(name: String, block: (Measure) -> R): R {
        val measure = newMeasure(name)
        measure.start { "start" }
        return try {
            block(measure)
        } finally {
            measure.log { "stop" }
        }
    }

    /**
     * Run a single block of code with the [Trace].
     * Must be enabled by a prefix in order to run real measurements.
     * Instance is not held by the [SharedPerformance] object.
     */
    fun <R> runTrace(name: TraceName, block: (Trace) -> R): R {
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
    private fun createMeasure(name: String): Measure {
        return MeasureImpl(name)
    }

    /**
     * Does not require a prefix to be enabled.
     * Instance is not held by the [SharedPerformance] object.
     */
    private fun createTrace(name: TraceName): Trace {
        return traceFactory.value?.create(name)?.let { TraceWrapper(it) } ?: NoTrace
    }

    private const val TAG = "SharedPerformance"
}
