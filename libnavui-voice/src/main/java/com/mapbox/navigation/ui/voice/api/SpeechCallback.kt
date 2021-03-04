package com.mapbox.navigation.ui.voice.api

import com.mapbox.navigation.ui.base.model.Expected
import com.mapbox.navigation.ui.voice.model.SpeechError
import com.mapbox.navigation.ui.voice.model.SpeechValue

/**
 * Interface definition for a callback to be invoked when a voice instruction is retrieved.
 */
interface SpeechCallback {

    /**
     * Invoked as a result of [MapboxSpeechApi.generate].
     * @param state is a [SpeechValue] including the announcement to be played when the
     * announcement is ready or a [SpeechError] including the error information and a fallback
     * with the raw announcement (without file) that can be played with a text-to-speech engine.
     */
    fun onSpeech(state: Expected<SpeechValue, SpeechError>)
}
