package com.mapbox.navigation.voicefeedback.internal

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

@ExperimentalPreviewMapboxNavigationAPI
internal sealed interface AsrData {
    data class Transcript(
        val text: String,
        val isFinal: Boolean,
    ) : AsrData

    data class Result(
        val description: String,
        val type: String,
    ) : AsrData
}
