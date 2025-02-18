
package com.mapbox.navigation.mapgpt.core.textplayer.middleware

import com.mapbox.navigation.mapgpt.core.MiddlewareContext
import com.mapbox.navigation.mapgpt.core.PlatformContext
import com.mapbox.navigation.mapgpt.core.audiofocus.AudioFocusManager
import com.mapbox.navigation.mapgpt.core.language.Language
import com.mapbox.navigation.mapgpt.core.textplayer.SoundPlayer
import com.mapbox.navigation.mapgpt.core.textplayer.Voice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Provides context needed for the middleware to operate.
 *
 * @param platformContext contains platform context specific to Android, iOS, or another.
 * @param language the currently selected language.
 * @param voice the currently selected voice.
 * @param audioFocusManager allows the middleware to request audio focus before playing.
 * @param soundPlayer allows the middleware to play sound effects.
 */
class VoicePlayerMiddlewareContext(
    val platformContext: PlatformContext,
    val language: StateFlow<Language>,
    val voice: StateFlow<Voice?>,
    val audioFocusManager: AudioFocusManager,
    val soundPlayer: SoundPlayer,
) : MiddlewareContext {

    private val _preferLocalTts = MutableStateFlow(false)
    val preferLocalTts: StateFlow<Boolean> = _preferLocalTts.asStateFlow()
    internal fun setPreferLocalTts(newValue: Boolean) {
        _preferLocalTts.value = newValue
    }

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()
    internal fun setIsMuted(newValue: Boolean) {
        _isMuted.value = newValue
    }

    private val _playPriorityExclusively = MutableStateFlow(false)
    val playPriorityExclusively: StateFlow<Boolean> = _playPriorityExclusively.asStateFlow()
    internal fun setPlayPriorityExclusively(newValue: Boolean) {
        _playPriorityExclusively.value = newValue
    }
}
