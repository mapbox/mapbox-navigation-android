package com.mapbox.navigation.mapgpt.core.api

/**
 * Provides a conversation state. The state starts as [ConversationState.Idle],
 * moves on to [ConversationState.ProcessingInput] when [MapGptService.postPromptsForStreaming] is called,
 * and continues as [ConversationState.Responding] when first conversation message is available.
 *
 * There are some prompts that do not warrant a conversational response but only action based events. For
 * such user prompts, the state starts as [ConversationState.Idle], moves on to
 * [ConversationState.ProcessingInput] when [MapGptService.postPromptsForStreaming] is called,
 * and continues as [ConversationState.NoResponse]
 *
 * You can access [ConversationState.Responding.bufferedConversation] to continue observe
 * the messages as they become available, in the correct order suited for presentation purposes.
 *
 * There can only be one conversation active at a time. Calling [MapGptService.postPromptsForStreaming]
 * while a conversation is not finished will interrupt and finish it.
 *
 * Once the active conversation finishes, the state will go back to [ConversationState.Idle]. All finished conversations are available in the [MapGptService.interactionHistory].
 *
 * The service has an ability to push supplemental conversation, unprompted by the user and initiated by the service itself.
 * This would cause the [ConversationState.Idle] move directly to [ConversationState.Responding].
 * If there's an already active conversation, the supplemental one will be queued and delivered as new [ConversationState.Responding] states,
 * without [ConversationState.Idle] in-between. Starting a new conversation with [MapGptService.postPromptsForStreaming] will flush the queue and move the conversations directly to the history.
 */
sealed class ConversationState {

    /**
     * Default [ConversationState] when no conversation is active.
     */
    object Idle : ConversationState() {
        override fun toString(): String = "Idle"
    }

    data class NoResponse(val bufferedNoResponse: BufferedNoResponse) : ConversationState()

    /**
     * Represents user input when [MapGptService.postPromptsForStreaming] is called
     *
     * @param input user query
     */
    data class ProcessingInput(
        val input: String,
    ) : ConversationState()

    /**
     * Represents AI response when first response to user query is available
     *
     * @param bufferedConversation state of a chunked, streamed response from the backend.
     */
    data class Responding(
        val bufferedConversation: BufferedConversation,
    ) : ConversationState()
}
