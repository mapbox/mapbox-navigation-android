package com.mapbox.navigation.instrumentation_tests.utils.coroutines

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout

interface ListFlow<T> : StateFlow<List<T>> {

    suspend fun nextElements(n: Int): List<T>

    suspend fun nextElement(): T

    fun hasNextElement(): Boolean
}

private const val WAIT_FOR_NEXT_ELEMENT_TIMEOUT = 5000L

class MutableListFlow<T> : ListFlow<T>, MutableStateFlow<List<T>> {

    private var processedElements = 0

    private val underlyingFlow = MutableStateFlow<List<T>>(value = listOf())

    override suspend fun nextElements(n: Int): List<T> {
        return withTimeout(WAIT_FOR_NEXT_ELEMENT_TIMEOUT) {
            underlyingFlow.filter {
                it.size >= n + processedElements
            }
                .first()
                .subList(processedElements, processedElements + n)
                .also {
                    processedElements += n
                }
        }
    }

    override suspend fun nextElement(): T = nextElements(1)[0]

    override fun hasNextElement(): Boolean {
        return value.size > processedElements
    }

    override val subscriptionCount: StateFlow<Int>
        get() = underlyingFlow.subscriptionCount

    override suspend fun emit(value: List<T>) {
        underlyingFlow.emit(value)
    }

    @ExperimentalCoroutinesApi
    override fun resetReplayCache() {
        underlyingFlow.resetReplayCache()
    }

    override fun tryEmit(value: List<T>): Boolean {
        return underlyingFlow.tryEmit(value)
    }

    override fun compareAndSet(expect: List<T>, update: List<T>): Boolean {
        return underlyingFlow.compareAndSet(expect, update)
    }

    @InternalCoroutinesApi
    override suspend fun collect(collector: FlowCollector<List<T>>) {
        underlyingFlow.collect(collector)
    }

    fun asListFlow(): ListFlow<T> = this

    override var value: List<T>
        get() = underlyingFlow.value
        set(value) { underlyingFlow.value = value }
    override val replayCache: List<List<T>>
        get() = underlyingFlow.replayCache
}
