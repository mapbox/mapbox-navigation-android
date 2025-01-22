package com.mapbox.navigation.voice.api

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener

internal object TextToSpeechProvider {

    fun getTextToSpeech(context: Context, onInitListener: OnInitListener): TextToSpeech =
        TextToSpeech(context, onInitListener)
}
