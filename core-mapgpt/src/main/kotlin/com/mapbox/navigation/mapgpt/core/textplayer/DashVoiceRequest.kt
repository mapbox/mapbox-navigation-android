package com.mapbox.navigation.mapgpt.core.textplayer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class DashVoiceRequest(
    @SerialName("text")
    val text: String,
    @SerialName("model_id")
    val modelId: String,
    @SerialName("language")
    val language: String,
)
