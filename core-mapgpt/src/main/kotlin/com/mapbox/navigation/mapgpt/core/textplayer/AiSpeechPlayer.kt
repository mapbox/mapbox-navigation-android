package com.mapbox.navigation.mapgpt.core.textplayer

import kotlinx.coroutines.flow.StateFlow

/**
 * Surfaces the state of the AI speech player.
 */
interface AiSpeechPlayer {
    val isSpeakingConfirmation: StateFlow<Boolean>
}
