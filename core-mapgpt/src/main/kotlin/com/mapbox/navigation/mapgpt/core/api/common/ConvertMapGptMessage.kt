package com.mapbox.navigation.mapgpt.core.api.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
class ConvertMapGptMessage(
    val action: String,
    val body: Body,
) {
    @Serializable
    class Body(
        val id: Long,
        val timestamp: Long,
        @SerialName("is_supplement")
        val isSupplement: Boolean,
        @SerialName("chunk_id")
        val chunkId: String,
        val type: String,
        val data: JsonElement,
    )
}
