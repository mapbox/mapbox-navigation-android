package com.mapbox.navigation.copilot

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object CopilotTestUtils {

    internal fun retrieveAttachments(metadata: String): List<AttachmentMetadata> {
        val gson = Gson()
        val itemType = object : TypeToken<List<AttachmentMetadata>>() {}.type
        return gson.fromJson(metadata, itemType)
    }
}
