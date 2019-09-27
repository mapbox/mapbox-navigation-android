package com.mapbox.services.android.navigation.v5.navigation.metrics.audio

import android.content.Context

internal class UnknownAudioType : AudioTypeResolver {
    private val UNKNOWN = "unknown"

    override fun nextChain(chain: AudioTypeResolver) {}

    override fun obtainAudioType(context: Context): String {
        return UNKNOWN
    }

}
