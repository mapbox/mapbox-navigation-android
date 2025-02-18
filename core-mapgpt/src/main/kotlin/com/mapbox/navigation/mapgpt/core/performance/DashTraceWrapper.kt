package com.mapbox.navigation.mapgpt.core.performance

import com.mapbox.navigation.mapgpt.core.common.SharedLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Simplifies interactions with [SharedPerformance] the [DashTrace]. This wrapper allows developers
 * to aggressively call [DashTrace.stop] without worrying about if the trace was started.
 */
internal class DashTraceWrapper(
    private val trace: DashTrace
): DashTrace {
    override val name: TraceName = trace.name

    private val _state = MutableStateFlow<State>(State.Initialized)
    val state: StateFlow<State> = _state

    override fun start() = apply {
        when (val state = state.value) {
            State.Initialized -> {
                trace.start()
                _state.value = State.Started
            }
            else -> SharedLog.d(TAG) { "Start for ${name.snakeCase} ignored for state $state" }
        }
    }

    override fun counter(key: TraceKey, block: (Long) -> Long) = apply {
        when (val state = state.value) {
            !is State.Stopped -> trace.counter(key, block)
            else -> SharedLog.d(TAG) { "Counter ${name.snakeCase}:${key.snakeCase} ignored for state $state" }
        }
    }

    override fun attribute(key: TraceKey, block: (Traceable?) -> TraceValue?) = apply {
        when (val state = state.value) {
            !is State.Stopped -> trace.attribute(key, block)
            else -> SharedLog.d(TAG) { "Attribute ${name.snakeCase}:${key.snakeCase} ignored for state $state" }
        }
    }

    override fun attributeCustom(key: TraceKey, block: (Traceable?) -> Traceable?) = apply {
        when (val state = state.value) {
            !is State.Stopped -> trace.attributeCustom(key, block)
            else -> SharedLog.d(TAG) { "Attribute ${name.snakeCase}:${key.snakeCase} ignored for state $state" }
        }
    }

    override fun stop() {
        when (val state = state.value) {
            State.Started -> {
                trace.stop()
                _state.value = State.Stopped
            }
            State.Initialized,
            State.Stopped -> SharedLog.d(TAG) { "Stop ${name.snakeCase} ignored for state: $state" }
        }
    }

    internal sealed interface State {
        object Initialized : State {
            override fun toString(): String = "Initialized"
        }
        object Started : State {
            override fun toString(): String = "Started"
        }
        object Stopped : State {
            override fun toString(): String = "Stopped"
        }
    }

    private companion object {
        private const val TAG = "DashTraceWrapper"
    }
}
