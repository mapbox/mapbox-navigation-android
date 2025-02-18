package com.mapbox.navigation.mapgpt.core.textplayer

import com.mapbox.navigation.mapgpt.core.language.Language
import kotlinx.coroutines.flow.StateFlow

/**
 * The VoicePlayer interface defines the contract for a voice playback system.
 * Implementations may use Text-to-Speech (TTS) engines to play back announcements.
 */
interface VoicePlayer {

    /**
     * Provides the available voices for the voice player. This is used to determine which voices
     * are available for the voice player to speak with.
     */
    val availableVoices: StateFlow<Set<Voice>>

    /**
     * Provides the available languages for the voice player. This is used to determine which
     * languages are available for the voice player to speak in.
     */
    val availableLanguages: StateFlow<Set<Language>>

    /**
     * Prefetches the given announcement, preparing it for playback. This method can be used to
     * improve the responsiveness of the [play] method.
     *
     * @param announcement the [Announcement] to be prefetched
     */
    fun prefetch(announcement: Announcement)

    /**
     * Plays the given announcement, reporting progress to the optional VoiceProgress
     * object, and notifying the PlayerCallback when playback completes or encounters an error.
     *
     * @param announcement the [Announcement] to be played.
     * @param progress an optional [VoiceProgress] used to resume playing, emitted by the [PlayerCallback.onStop]
     * @param callback a [PlayerCallback] to be notified of state changes for the playback.
     */
    fun play(
        announcement: Announcement,
        progress: VoiceProgress?,
        callback: PlayerCallback,
    )

    /**
     * Similar to [play] but does not play immediately. Instead, it can play over a short period
     * of time. [fadePlay] is a blocking operation that returns after the play has completed.
     * The [progress] is used to resume playback from a previous [PlayerCallback.onStop] event.
     *
     * @param announcement the [Announcement] to be played.
     * @param progress an optional [VoiceProgress] used to resume playing, emitted by the [PlayerCallback.onStop]
     * @param callback a [PlayerCallback] to be notified of state changes for the playback.
     */
    fun fadePlay(
        announcement: Announcement,
        progress: VoiceProgress?,
        callback: PlayerCallback,
    ) = play(announcement, progress, callback)

    /**
     * Stops the current playback, if any. Calling will emit [PlayerCallback.onStop] to the
     * callback passed to [play]. The [VoiceProgress] can be used to resume playback from the same
     * position.
     */
    fun stop()

    /**
     * Similar to [stop] but does not stop immediately. Instead, it can stop over a short period
     * of time. [fadeStop] is a blocking operation that returns after the stop has completed.
     * Calling will emit [PlayerCallback.onStop] to the callback passed to [play].
     * The [VoiceProgress] represents the playback time at the beginning of the [fadeStop] call.
     */
    suspend fun fadeStop() = stop()

    /**
     * Releases resources held by this [VoicePlayer]. This is an appropriate place to clear caches
     * held from [prefetch]. It is expected that the implementation will be able to allocate new
     * resources through [prefetch] and [play] functions.
     */
    fun release()

    /**
     * Speech volume relative to the current stream type volume used when speaking text.
     * Specified as a float ranging from 0 to 1 where 0 is silence, and 1 is the maximum volume.
     *
     * @param level the volume level, where 1.0 is max volume 0.0 is silence.
     */
    fun volume(level: Float)
}
