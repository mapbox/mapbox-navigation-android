package com.mapbox.navigation.mapgpt.core.textplayer

/**
 * Exposes current state of the [Player].
 */
sealed class PlayerState {

    override fun toString(): String = this::class.simpleName ?: ""

    /**
     * State representing a player that doesn't play or buffer any announcements.
     */
    object Idle : PlayerState()

    /**
     * State representing a player that doesn't play any media but is in a process of preparing a message.
     *
     * @param utteranceId unique ID of an utterance
     */
    data class Preparing(
        val utteranceId: String,
    ) : PlayerState()

    /**
     * State representing an announcement that started being played. Called for each announcement in the queue.
     *
     * @param utteranceId unique ID of an utterance
     */
    data class Speaking(
        val text: String? = null,
        val utteranceId: String,
    ) : PlayerState()

    /**
     * State representing an announcement that stopped playing.
     *
     * @param utteranceId unique ID of an utterance
     */
    data class Stopped(
        val progress: VoiceProgress?,
        val text: String?,
        val utteranceId: String,
    ) : PlayerState()

    /**
     * State representing an error. Set whenever the playback fails.
     */
    data class Error(
        val utteranceId: String?,
        val reason: String,
    ) : PlayerState()

    /**
     * State representing an announcement that finished playing. Called for each announcement in the queue.
     *
     * @param utteranceId unique ID of an utterance
     */
    data class Done(
        val text: String? = null,
        val utteranceId: String
    ) : PlayerState()
}
