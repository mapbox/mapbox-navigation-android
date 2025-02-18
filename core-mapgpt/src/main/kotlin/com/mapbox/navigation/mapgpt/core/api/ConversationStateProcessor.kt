package com.mapbox.navigation.mapgpt.core.api

import com.mapbox.navigation.mapgpt.core.common.Log
import com.mapbox.navigation.mapgpt.core.common.SharedLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration

internal interface ConversationStateProcessor {
    val conversationStatus: SharedFlow<ConversationState>
    val conversationState: StateFlow<ConversationState>
    val interactionHistory: StateFlow<List<InteractionHistoryElement>>
    var onTimeout: (suspend (ConversationTimeoutException) -> Unit)?

    fun onNewInput(input: String)
    fun updateConversationBuffers(eventBody: SessionFrame.SendEvent.Body)
    fun cancel()
    fun clear()
}

/**
 * Processor that accepts [SessionFrame.SendEvent.Body] frames and produces new [BufferedConversation]s and related state.
 *
 * A conversation can either be created as a response to user query, or on-demand where an event with [SessionFrame.SendEvent.Body.isSupplement]
 * flag is sent by the backend.
 *
 * A non-supplement event is only accepted if the processor is in the [ConversationState.ProcessingInput] state, otherwise, the event is dropped.
 *
 * A supplement event can either be delivered as [ConversationState.Responding] immediately if there's no other conversation or user query active,
 * or be queued in a priority queue based on [SessionFrame.SendEvent.Body.chunkPrefix].
 * When the active conversation finishes, the first element from the queue is popped and delivered immediately as a new [ConversationState.Responding] state,
 * without [ConversationState.Idle] in-between.
 *
 * The processor also handles non-conversation events (entities/actions/etc) that are delivered out-of-order or without an active conversation.
 * Due to the async nature of the backend, these events can be delivered even before utterances that refer to them,
 * so they are cached and extracted when related conversation appears.
 */
internal class ConversationStateProcessorImpl(
    private val coroutineScope: CoroutineScope,
    private val timeout: Duration,
    private val conversationTimeoutMonitor: ConversationTimeoutMonitor =
        ConversationTimeoutMonitor(coroutineScope = coroutineScope),
    private val logger: Log = SharedLog,
) : ConversationStateProcessor {
    private val mutex = Mutex()

    override var onTimeout: (suspend (ConversationTimeoutException) -> Unit)? = null

    private val _conversationStatus = MutableSharedFlow<ConversationState>()
    override val conversationStatus = _conversationStatus.asSharedFlow()
    override val conversationState: StateFlow<ConversationState> = conversationStatus.stateIn(
        scope = coroutineScope,
        started = SharingStarted.Eagerly,
        initialValue = ConversationState.Idle,
    )

    private val _interactionHistory = MutableStateFlow<List<InteractionHistoryElement>>(emptyList())
    override val interactionHistory = _interactionHistory.asStateFlow()

    private val conversationQueue = PriorityQueue(logger)
    private val eventCache = mutableMapOf<String, MutableList<SessionFrame.SendEvent.Body>>()

    override fun onNewInput(input: String) {
        cancel()
        coroutineScope.launch {
            mutex.withLock {
                _conversationStatus.emit(ConversationState.ProcessingInput(input))
                _interactionHistory.update { it + InteractionHistoryElement.Input(input) }
            }
        }
        conversationTimeoutMonitor.onNewConversationStarted(timeout) {
            coroutineScope.launch {
                cancelSync()
                onTimeout?.invoke(ConversationTimeoutException())
            }
        }
    }

    override fun updateConversationBuffers(
        eventBody: SessionFrame.SendEvent.Body,
    ) {
        coroutineScope.launch {
            mutex.withLock {
                fun createNewConversationAndReadCache(
                    body: SessionFrame.SendEvent.Body.Conversation,
                ): BufferedConversation {
                    val bufferedConversation = BufferedConversation(
                        body,
                    )
                    eventCache.remove(bufferedConversation.chunkPrefix)
                        ?.let { events ->
                            events.forEach {
                                bufferedConversation.onNewEvent(it)
                            }
                        }
                    return bufferedConversation
                }

                fun createNewNoResponseAndReadCache(
                    body: SessionFrame.SendEvent.Body.NoResponse
                ): BufferedNoResponse {
                    val bufferedNoResponseEvent = BufferedNoResponse(body)
                    eventCache.remove(bufferedNoResponseEvent.chunkPrefix)
                        ?.let { events ->
                            events.forEach {
                                bufferedNoResponseEvent.onNewEvent(it)
                            }
                        }
                    return bufferedNoResponseEvent
                }

                val consumedByCurrentConversation = when (val convState = conversationState.value) {
                    ConversationState.Idle -> {
                        false
                    }

                    is ConversationState.ProcessingInput -> {
                        if (eventBody is SessionFrame.SendEvent.Body.Conversation &&
                            !eventBody.isSupplement
                        ) {
                            val conversation = createNewConversationAndReadCache(eventBody)
                            val chunkIsForFinishedConversation = interactionHistory.value
                                .filterIsInstance<InteractionHistoryElement.Output>()
                                .filterIsInstance<HistoryOutput.Response>()
                                .any {
                                    it.bufferedConversation.uniqueId == conversation.uniqueId
                                }
                            if (!chunkIsForFinishedConversation) {
                                conversationTimeoutMonitor.onNewEventReceived()
                                _conversationStatus.emit(
                                    ConversationState.Responding(
                                        bufferedConversation = conversation,
                                    ),
                                )
                                conversation.checkIfFinishedAndContinue()
                                true
                            } else {
                                false
                            }
                        } else if (eventBody is SessionFrame.SendEvent.Body.NoResponse) {
                            val bufferedEvent = createNewNoResponseAndReadCache(eventBody)
                            _conversationStatus.emit(ConversationState.NoResponse(bufferedEvent))
                            _interactionHistory.update {
                                it + InteractionHistoryElement.Output(HistoryOutput.NoResponse(bufferedEvent))
                            }
                            continueIfNeeded()
                            true
                        } else {
                            false
                        }
                    }

                    is ConversationState.Responding -> {
                        val consumed = convState.bufferedConversation.onNewEvent(eventBody)
                        if (consumed) {
                            conversationTimeoutMonitor.onNewEventReceived()
                        }
                        convState.bufferedConversation.checkIfFinishedAndContinue()
                        consumed
                    }

                    is ConversationState.NoResponse -> {
                        val consumed = convState.bufferedNoResponse.onNewEvent(eventBody)
                        if (consumed) {
                            _interactionHistory.update {
                                it + InteractionHistoryElement.Output(HistoryOutput.NoResponse(convState.bufferedNoResponse))
                            }
                            continueIfNeeded()
                            return@launch
                        } else {
                            consumed
                        }
                    }
                }
                if (!consumedByCurrentConversation) {
                    val consumedByPastConversations = interactionHistory.value
                        .filterIsInstance<InteractionHistoryElement.Output>()
                        .map { it.historyOutput.bufferedOutput }
                        .onNewEvent(eventBody)
                    if (!consumedByPastConversations) {
                        fun insertEventIntoCache(body: SessionFrame.SendEvent.Body) {
                            val events = eventCache[body.chunkPrefix]
                                ?: mutableListOf<SessionFrame.SendEvent.Body>().also { list ->
                                    eventCache[body.chunkPrefix] = list
                                }
                            events.add(body)
                        }
                        if (eventBody.isSupplement) {
                            val consumedByQueuedConversations =
                                conversationQueue.onNewEvent(eventBody)
                            if (!consumedByQueuedConversations) {
                                if (eventBody is SessionFrame.SendEvent.Body.Conversation) {
                                    val bufferedConversation =
                                        createNewConversationAndReadCache(eventBody)
                                    if (conversationState.value is ConversationState.Idle) {
                                        startNewConversationTimeout()
                                        _conversationStatus.emit(
                                            ConversationState.Responding(
                                                bufferedConversation = bufferedConversation,
                                            ),
                                        )
                                        bufferedConversation.checkIfFinishedAndContinue()
                                    } else {
                                        conversationQueue.insert(bufferedConversation)
                                    }
                                } else {
                                    insertEventIntoCache(eventBody)
                                }
                            }
                        } else if (eventBody !is SessionFrame.SendEvent.Body.Conversation) {
                            insertEventIntoCache(eventBody)
                        } else {
                            logger.w(TAG) {
                                "event '${eventBody.id}' ignored, it was a conversation " +
                                    "neither for the current input nor a supplement"
                            }
                        }
                    }
                }
            }
        }
    }

    private fun startNewConversationTimeout() {
        conversationTimeoutMonitor.onNewConversationStarted(timeout) {
            coroutineScope.launch {
                cancelSync()
                onTimeout?.invoke(ConversationTimeoutException())
            }
        }
    }

    private suspend fun BufferedConversation.checkIfFinishedAndContinue() {
        if (isFinished) {
            _interactionHistory.update {
                it + InteractionHistoryElement.Output(HistoryOutput.Response(this))
            }
            continueIfNeeded()
        }
    }

    private suspend fun continueIfNeeded() {
        conversationTimeoutMonitor.cancel()
        val queuedConversation = conversationQueue.pop()
        if (queuedConversation == null) {
            _conversationStatus.emit(ConversationState.Idle)
        }
        if (queuedConversation != null) {
            startNewConversationTimeout()
            _conversationStatus.emit(
                ConversationState.Responding(
                    bufferedConversation = queuedConversation,
                ),
            )
            queuedConversation.checkIfFinishedAndContinue()
        }
    }

    override fun cancel() {
        coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            cancelSync()
        }
    }

    private suspend fun cancelSync() {
        mutex.withLock {
            interruptOngoingConversation()
        }
    }

    override fun clear() {
        coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            mutex.withLock {
                interruptOngoingConversation()
                _interactionHistory.value = emptyList()
            }
        }
    }

    private suspend fun interruptOngoingConversation() {
        val ongoing = conversationState.value
        if (ongoing is ConversationState.Responding) {
            conversationTimeoutMonitor.cancel()
            ongoing.bufferedConversation.interrupt()
        }
        val queuedConversation = conversationQueue.clear()
        _conversationStatus.emit(ConversationState.Idle)
        if (ongoing is ConversationState.Responding) {
            _interactionHistory.update {
                it + InteractionHistoryElement.Output(HistoryOutput.Response(ongoing.bufferedConversation))
            }
        }
        queuedConversation.forEach { conversation ->
            _interactionHistory.update {
                it + InteractionHistoryElement.Output(HistoryOutput.Response(conversation))
            }
        }
    }

    /**
     * Priority queue for [BufferedConversation].
     *
     * Inserted elements are sorted increasingly based on the [BufferedConversation.chunkPrefix] string (date).
     */
    private class PriorityQueue(private val logger: Log) {
        private val mutex = Mutex()
        private val queue = mutableListOf<BufferedConversation>()

        suspend fun insert(element: BufferedConversation) {
            mutex.withLock {
                val invertedInsertionPoint = queue.binarySearch {
                    String.CASE_INSENSITIVE_ORDER.compare(it.chunkPrefix, element.chunkPrefix)
                }
                if (invertedInsertionPoint < 0) {
                    queue.add(-(invertedInsertionPoint + 1), element)
                } else {
                    logger.e(TAG) {
                        "received duplicate interaction with `${element.chunkPrefix}` chunk prefix"
                    }
                }
            }
        }

        suspend fun pop(): BufferedConversation? {
            mutex.withLock {
                return queue.removeFirstOrNull()
            }
        }

        suspend fun onNewEvent(eventBody: SessionFrame.SendEvent.Body): Boolean {
            mutex.withLock {
                return queue.onNewEvent(eventBody)
            }
        }

        suspend fun clear(): List<BufferedConversation> {
            mutex.withLock {
                queue.forEach {
                    it.interrupt()
                }
                val queueCopy = queue.toList()
                queue.clear()
                return queueCopy
            }
        }
    }

    private companion object {
        private const val TAG = "ConversationStateProcessor"
    }
}

/**
 * Returns `true` if any [BufferedOutput] in the collection consumed the event, `false` otherwise.
 */
private fun List<BufferedOutput>.onNewEvent(eventBody: SessionFrame.SendEvent.Body): Boolean {
    var index = lastIndex
    var consumed = false
    while (index >= 0 && !consumed) {
        consumed = get(index).onNewEvent(eventBody)
        index--
    }
    return consumed
}
