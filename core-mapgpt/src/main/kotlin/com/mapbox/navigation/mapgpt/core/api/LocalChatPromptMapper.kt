package com.mapbox.navigation.mapgpt.core.api

object LocalChatPromptMapper {
    fun createPrompt(mapGptRequest: MapGptStreamingRequest): String {
        // TODO add the data context or whatever we need to improve the results
        return mapGptRequest.prompt
    }
}
