package com.mapbox.navigation.ui.voice.api

import com.mapbox.bindgen.Expected
import com.mapbox.navigation.ui.voice.model.SpeechError
import com.mapbox.navigation.ui.voice.model.SpeechValue
import com.mapbox.navigation.ui.voice.model.TypeAndAnnouncement

internal class VoiceInstructionsCache {

    private val observers =
        mutableMapOf<
            TypeAndAnnouncement,
            MutableList<(Expected<SpeechError, SpeechValue>) -> Unit>
            >()
    private val cachedFiles =
        mutableMapOf<TypeAndAnnouncement, Expected<SpeechError, SpeechValue>>()

    fun get(key: TypeAndAnnouncement): Expected<SpeechError, SpeechValue>? = cachedFiles[key]

    fun remove(key: TypeAndAnnouncement): Expected<SpeechError, SpeechValue>? =
        cachedFiles.remove(key)

    fun put(key: TypeAndAnnouncement, value: Expected<SpeechError, SpeechValue>) {
        cachedFiles[key] = value
        val keyObservers = observers[key]?.toList()
        observers.remove(key)
        keyObservers?.forEach { it(value) }
    }

    fun getEntries(): Map<TypeAndAnnouncement, Expected<SpeechError, SpeechValue>> = cachedFiles

    fun registerOneShotObserver(
        key: TypeAndAnnouncement,
        observer: (Expected<SpeechError, SpeechValue>) -> Unit
    ) {
        val value = cachedFiles[key]
        if (value == null) {
            observers[key] = (observers[key] ?: mutableListOf()).apply { add(observer) }
        } else {
            observer(value)
        }
    }

    fun unregisterOneShotObserver(
        key: TypeAndAnnouncement,
        observer: (Expected<SpeechError, SpeechValue>) -> Unit
    ) {
        observers[key]?.remove(observer)
    }
}
