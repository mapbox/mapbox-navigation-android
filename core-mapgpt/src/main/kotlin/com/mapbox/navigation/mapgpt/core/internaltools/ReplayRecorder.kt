package com.mapbox.navigation.mapgpt.core.internaltools

import com.mapbox.navigation.mapgpt.core.api.MapGptStreamingRequest

interface ReplayRecorder {

    fun recordRequest(
        apiHost: String,
        request: MapGptStreamingRequest,
    )
    fun recordRawResponse(text: String)
}
