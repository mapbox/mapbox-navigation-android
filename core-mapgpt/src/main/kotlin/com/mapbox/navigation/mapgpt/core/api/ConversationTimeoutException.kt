package com.mapbox.navigation.mapgpt.core.api

/**
 * Exception used when there's no conversation data received for a timeout duration provided in
 * [MapGptService.postPromptsForStreaming].
 */
class ConversationTimeoutException : Exception(
    "Failed to receive conversation updates within required time",
)
