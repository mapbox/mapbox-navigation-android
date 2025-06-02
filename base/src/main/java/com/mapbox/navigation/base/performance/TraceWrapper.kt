package com.mapbox.navigation.base.performance

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.utils.internal.logD
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Simplifies interactions with [SharedPerformance] the [Trace]. This wrapper allows developers
 * to aggressively call [Trace.stop] without worrying about if the trace was started.
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class TraceWrapper(
    private val trace: Trace,
) : Trace {

    override val name: TraceName = trace.name

    private val _state = MutableStateFlow<State>(State.Initialized)
    val state: StateFlow<State> = _state

    override fun start() = apply {
        when (val state = state.value) {
            State.Initialized -> {
                trace.start()
                _state.value = State.Started
            }

            else -> logD(TAG) { "Start for ${name.snakeCase} ignored for state $state" }
        }
    }

    override fun counter(key: TraceKey, block: (Long) -> Long) = apply {
        when (val state = state.value) {
            !is State.Stopped -> trace.counter(key, block)
            else -> logD(TAG) {
                "Counter ${name.snakeCase}:${key.snakeCase} ignored for state $state"
            }
        }
    }

    override fun attribute(key: TraceKey, block: (Traceable?) -> TraceValue?) = apply {
        when (val state = state.value) {
            !is State.Stopped -> trace.attribute(key, block)
            else -> logD(TAG) {
                "Attribute ${name.snakeCase}:${key.snakeCase} ignored for state $state"
            }
        }
    }

    override fun attributeCustom(key: TraceKey, block: (Traceable?) -> Traceable?) = apply {
        when (val state = state.value) {
            !is State.Stopped -> trace.attributeCustom(key, block)
            else -> logD(TAG) {
                "Attribute ${name.snakeCase}:${key.snakeCase} ignored for state $state"
            }
        }
    }

    override fun stop() {
        when (val state = state.value) {
            State.Started -> {
                trace.stop()
                _state.value = State.Stopped
            }

            State.Initialized,
            State.Stopped,
            -> logD(TAG) { "Stop ${name.snakeCase} ignored for state: $state" }
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

        private const val TAG = "TraceWrapper"
    }
}
