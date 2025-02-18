package com.mapbox.navigation.mapgpt.core.api

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Represents a state of a chunked, streamed response from the backend.
 *
 * Exposed [bufferedText] and [chunks] will be called for all available chunks in the correct order.
 *
 * For example, if chunk 2 becomes available from the backend but we don't have chunk 1 yet,
 * the implementation will wait for the missing chunk and deliver both 1 and 2 in the correct order as soon as available.
 */
class BufferedConversation internal constructor(
    conversation: SessionFrame.SendEvent.Body.Conversation,
): BufferedOutput {

    /**
     * ID of this buffered conversation that might be used as a unique key for presentation, for example, in a chat layout.
     */
    val uniqueId = conversation.generateUniqueId()

    /**
     * Represents the prefix of the conversation.
     */
    val chunkPrefix = conversation.chunkPrefix

    /**
     * Flag that indicates if this conversation received and exposed all available chunks.
     *
     * It is set either when the backend notifies the client that all AI responses have been sent
     * or if the interaction was interrupted.
     *
     * @see ConversationState.Idle
     */
    var isFinished = false
        private set

    /**
     * Flag that indicates that this conversation has hit the profile's response token limit.
     *
     * Instructing the service to "continue" (via user input) should respond with the remaining
     * portion of the conversation.
     */
    var isTokenMaxed = false
        private set

    /**
     * Flag that indicates if the conversation has been interrupted. This can only be `true` if [isFinished] is also `true`.
     */
    var wasInterrupted = false
        private set

    private val indexedConversationFrames =
        mutableMapOf<Int, SessionFrame.SendEvent.Body.Conversation>()

    private val indexedEvents = mutableMapOf<Int, List<SessionFrame.SendEvent.Body>>()

    private var offsetToRead = -1

    private val _bufferedText = MutableStateFlow("")

    /**
     * A state that provides the entirety of the available conversation message received from the AI backend.
     *
     * It will keep appending new data to the state until [isFinished] is set.
     */
    val bufferedText = _bufferedText.asStateFlow()

    private val _chunks = MutableSharedFlow<SessionFrame.SendEvent.Body.Conversation>(
        replay = Int.MAX_VALUE,
    )

    /**
     * A flow that pushes a chunk of the conversation message received from the AI backend as soon as it's available.
     *
     * The flow will keep publishing new chunks until [isFinished] is set.
     *
     * This is a [SharedFlow] with infinite replay buffer, so all available chunks will be replayed one-by-one on subscription.
     */
    val chunks = _chunks.asSharedFlow()

    private val _events = MutableStateFlow<List<SessionFrame.SendEvent.Body>>(emptyList())

    /**
     * Holds all events associated with this conversation, ordered according to appearance in the conversation chunks.
     *
     * An event will be available only when the associated conversation chunk is available, in correct order.
     * This in practice means that events won't become available until text that refers to them is available as well.
     *
     * Events can be populated even after [isFinished] is set but will never receive updates if [wasInterrupted] is set as well.
     */
    override val events = _events.asStateFlow()

    init {
        onNewEvent(conversation)
    }

    /**
     * @return true if the event was consumed, false otherwise
     */
    override fun onNewEvent(
        eventBody: SessionFrame.SendEvent.Body,
    ): Boolean {
        when (eventBody) {
            is SessionFrame.SendEvent.Body.Conversation -> {
                if (eventBody.generateUniqueId() == uniqueId) {
                    if (!isFinished) {
                        if (eventBody.data.initial) {
                            offsetToRead = eventBody.chunkOffset
                        }
                        indexedConversationFrames[eventBody.chunkOffset] = eventBody
                        readNextChunkIfAvailable()
                    }
                    return true
                }
            }

            else -> {
                if (eventBody.chunkPrefix == chunkPrefix) {
                    if (!wasInterrupted) {
                        val values = indexedEvents[eventBody.chunkOffset] ?: mutableListOf()
                        val update = values.plus(eventBody)
                        indexedEvents[eventBody.chunkOffset] = update
                        if (eventBody.chunkOffset <= offsetToRead) {
                            _events.value = indexedEvents.stateUpToOffset(offsetToRead)
                        }
                    }
                    return true
                }
            }
        }
        return false
    }

    internal fun interrupt() {
        wasInterrupted = true
        isFinished = true
    }

    private fun readNextChunkIfAvailable() {
        if (offsetToRead == -1) {
            // Wait for the chunk with 'initial' flag to come in to set the starting offset.
            // Conversation chunks may start from non-zero offset,
            // if there were any extractions done on user input string.
            return
        }
        indexedConversationFrames[offsetToRead]?.let { conversationEvent ->
            _chunks.tryEmit(conversationEvent)
            _bufferedText.value.let {
                val update = if (it.isNotEmpty()) {
                    // TODO a space as a divider might not always be correct,
                    //  for example, if the next chunk starts with a comma
                    it.plus(" ")
                } else {
                    it
                }
                _bufferedText.value = update.plus(conversationEvent.data.content)
            }
            _events.value = indexedEvents.stateUpToOffset(offsetToRead)
            offsetToRead++
            if (conversationEvent.data.final) {
                isTokenMaxed = conversationEvent.data.maxTokens
                isFinished = true
            } else {
                readNextChunkIfAvailable()
            }
        }
    }

    private fun SessionFrame.SendEvent.Body.Conversation.generateUniqueId(): String =
        "${data.conversationId}@$chunkPrefix"

    private fun Map<Int, List<SessionFrame.SendEvent.Body>>.stateUpToOffset(
        chunkOffset: Int,
    ): List<SessionFrame.SendEvent.Body> {
        val state = mutableListOf<SessionFrame.SendEvent.Body>()
        // we're iterating from zero
        // because extracted events can happen also for user input
        // which has indices ahead of AI-generated response chunks
        for (i in 0..chunkOffset) {
            get(i)?.let {
                state.addAll(it)
            }
        }
        return state
    }

    /**
     * Checks if both objects have the same value or structure.
     *
     * @param other the other object to compare to
     * @return true if both are equal, false otherwise
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BufferedConversation) return false

        if (uniqueId != other.uniqueId) return false

        return true
    }

    /**
     * Returns a hash code value for the object or zero if the object is null.
     */
    override fun hashCode(): Int {
        return uniqueId.hashCode()
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "BufferedConversation(uniqueId='$uniqueId')"
    }
}
