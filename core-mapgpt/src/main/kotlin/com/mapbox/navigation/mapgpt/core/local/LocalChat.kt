package com.mapbox.navigation.mapgpt.core.local

import com.mapbox.navigation.mapgpt.core.api.LocalChatPromptMapper
import com.mapbox.navigation.mapgpt.core.api.MapGptStreamingRequest

/**
 * Interface that defines interactions with the Local LLM directly.
 */
interface LocalChat {

    /**
     * Posts a prompt to the local LLM.
     *
     * Use shared utilities like [LocalChatPromptMapper] to keep consistency between platforms.
     *
     * @return flowable that may throw an exception if something is wrong
     */
    fun postPrompt(mapGptRequest: MapGptStreamingRequest)
}
