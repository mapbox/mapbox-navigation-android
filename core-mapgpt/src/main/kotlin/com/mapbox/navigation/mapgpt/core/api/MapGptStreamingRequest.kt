package com.mapbox.navigation.mapgpt.core.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data object defining a [MapGptService] request.
 *
 * @param prompt User input.
 * @param context Context required for MapGPT service to process the request.
 * @param profileId Optional unique ID of the profile that the MapGPT should use to respond to the user input.
 * A default profile is selected if left `null`.
 * @param conversationId Optional unique ID of the current conversation.
 */
@Serializable
data class MapGptStreamingRequest(
    val prompt: String,
    val context: MapGptContextDTO,
    @SerialName("profile_id")
    val profileId: String? = null,
    @SerialName("capabilities")
    val capabilities: Set<String>? = null,
    @SerialName("conversation_id")
    val conversationId: String? = null,
)
