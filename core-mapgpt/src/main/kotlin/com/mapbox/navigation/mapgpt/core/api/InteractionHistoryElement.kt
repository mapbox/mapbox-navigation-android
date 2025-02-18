package com.mapbox.navigation.mapgpt.core.api

/**
 * Holds user input and AI output of interactions with MapGPT service.
 */
sealed class InteractionHistoryElement {

    /**
     * User input sent to the MapGPT Service.
     */
    data class Input(val text: String) : InteractionHistoryElement()

    /**
     * Output produced by MapGPT Service.
     */
    data class Output(val historyOutput: HistoryOutput) : InteractionHistoryElement()
}

/**
 * Holds AI output that can either contain verbal confirmation or not.
 *
 * @param bufferedOutput is [Response] in case there is a verbal confirmation associated with the event,
 * [NoResponse] otherwise.
 */
sealed class HistoryOutput(val bufferedOutput: BufferedOutput) {
    /**
     * Verbal confirmation produced by MapGPT Service.
     *
     * @param bufferedConversation chunked, streamed response from the backend
     */
    data class Response(val bufferedConversation: BufferedConversation): HistoryOutput(bufferedConversation)

    /**
     * Non verbal confirmation produced by MapGPT Service.
     *
     * @param bufferedNoResponse events associated with this response.
     */
    data class NoResponse(val bufferedNoResponse: BufferedNoResponse): HistoryOutput(bufferedNoResponse)
}
