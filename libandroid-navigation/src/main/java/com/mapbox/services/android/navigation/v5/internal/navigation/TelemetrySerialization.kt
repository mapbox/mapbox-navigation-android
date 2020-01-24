package com.mapbox.services.android.navigation.v5.internal.navigation

import com.mapbox.navigation.utils.thread.ifChannelException
import com.mapbox.services.android.navigation.v5.utils.extensions.ifNonNull
import java.lang.Exception
import java.util.ArrayList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

internal enum class queueAction {
    UPDATE_LAST,
    ADD_ITEM,
    GENERATE_ITEM,
    REMOVE_IF,
    FIND_IF
}

internal data class TelemetryEventDescriptor<T>(val action: queueAction, var event: T?, val updatePredicate: (T) -> T, val findPredicate: (T) -> Boolean)

internal class TelemetrySerialization<T>(scope: CoroutineScope) : TelemetrySerializationInterface<T> {

    private val channel = Channel<TelemetryEventDescriptor<T>>(Channel.UNLIMITED)
    private val eventsQueue = ArrayList<T>()
    private val resultChannel = Channel<T?>(1)

    init {
        scope.launch {
            try {

                while (isActive && select {
                            channel.onReceive { item ->
                                when (item.action) {
                                    queueAction.ADD_ITEM -> {
                                        ifNonNull(item.event) { event ->
                                            eventsQueue.add(event)
                                        }
                                    }
                                    queueAction.GENERATE_ITEM -> {
                                        eventsQueue.forEach { event ->
                                            item.updatePredicate(event)
                                        }
                                    }
                                    queueAction.UPDATE_LAST -> {
                                        if (eventsQueue.isNotEmpty()) {
                                            eventsQueue.last().apply {
                                                item.updatePredicate(this)
                                            }
                                        }
                                    }
                                    queueAction.REMOVE_IF -> {
                                        val iterator = eventsQueue.listIterator()
                                        while (iterator.hasNext()) {
                                            val event = iterator.next()
                                            if (item.findPredicate(event)) {
                                                iterator.remove()
                                            }
                                        }
                                    }
                                    queueAction.FIND_IF -> {
                                        resultChannel.send(eventsQueue.find { target ->
                                            item.findPredicate(target)
                                        })
                                    }
                                }
                                true
                            }
                        }) {
                }
            } catch (e: Exception) {
                e.ifChannelException {
                    // Do nothing. This channel is closed.
                }
            }
        }
    }

    override fun addEvent(routeEvent: T) {
        channel.offer(TelemetryEventDescriptor<T>(queueAction.ADD_ITEM, routeEvent, { rerouteEvent -> rerouteEvent }, { false }))
    }

    override fun updateLastEvent(predicate: (T) -> T) {
        channel.offer(TelemetryEventDescriptor(queueAction.UPDATE_LAST, null, predicate, { false }))
    }

    override fun applyToEach(predicate: (T) -> Boolean) {
        channel.offer(TelemetryEventDescriptor(queueAction.GENERATE_ITEM, null, { arg: T -> arg }, predicate))
    }

    override fun removeEventIf(predicate: (T) -> Boolean) {
        channel.offer(TelemetryEventDescriptor(queueAction.REMOVE_IF, null, { arg: T -> arg }, predicate))
    }

    override suspend fun findIf(predicate: (T) -> Boolean): T? {
        channel.offer(TelemetryEventDescriptor(queueAction.REMOVE_IF, null, { arg: T -> arg }, predicate))
        return resultChannel.receive()
    }
}
