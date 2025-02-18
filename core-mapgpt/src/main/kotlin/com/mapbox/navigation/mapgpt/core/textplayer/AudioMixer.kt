package com.mapbox.navigation.mapgpt.core.textplayer

import com.mapbox.navigation.mapgpt.core.common.Log
import com.mapbox.navigation.mapgpt.core.common.SharedLog
import com.mapbox.navigation.mapgpt.core.textplayer.AudioMixer.Clip
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * [AudioMixer] manages audio clips in prioritized tracks, ensuring that clips
 * are played in the order of insertion while allowing higher-priority clips
 * to interrupt lower-priority clips.
 *
 * The tracks in the AudioMixer are arrays of audio clips that are played in order. They are used
 * to organize the clips by priority. This way, the caller can add clips to the mixer and the mixer
 * will surface the clips in the order they were added, but will prioritize clips in the higher
 * tracks over clips in the lower tracks.
 *
 * Logic is added to this class so the operations can be unit tested in isolation.
 */
internal interface AudioMixer {
    /**
     * [Clip] represents an audio clip with an associated track index.
     */
    sealed interface Clip {
        /**
         * The track index used to prioritize clips and surfaced through the [current] flow.
         */
        val track: Int
    }

    /**
     * Represents the beginning of an interruption. Once an interruption occurs, the [current]
     * clip is replaced with an instance of [InterruptionStart].
     *
     * [deferred] the interrupted clip that was [current].
     * [priority] the priority clip that is interrupting the [current].
     */
    data class InterruptionStart(
        val deferred: Clip,
        val priority: Clip,
    ) : Clip by priority

    /**
     * Represents the end of an interruption. Once the interruption ends, the [Clip] that was
     * deferred will resume when the tracks with higher priority are empty.
     *
     * [deferred] the clip that was deferred.
     */
    data class InterruptionEnd(
        val deferred: Clip,
    ) : Clip by deferred

    /**
     * Represents the front of the queue.
     * Updates upon insertion or removal of clips.
     */
    val current: StateFlow<Clip?>

    /**
     * The total number of clips in all tracks.
     */
    val size: Int

    /**
     * Updates the [current] clip based on the current state of the queue.
     */
    fun updateCurrent()

    /**
     * Inserts a clip into the queue.
     * The clip is added to the back of the [Clip.track].
     *
     * @param clip the clip to insert
     */
    fun insert(clip: Clip, interrupts: Boolean)

    /**
     * Removes a clip from the queue.
     *
     * @param clip the clip to remove
     * @return the removed clip or null if the clip was not found
     */
    fun removeClip(clip: Clip): Clip?

    /**
     * Removes all clips from the queue with the same [Clip.track].
     *
     * @param track the [Clip.track] to remove
     * @return the removed clips
     */
    fun removeTrack(track: Int): List<Clip>

    /**
     * Removes all clips from the queue.
     *
     * @return the removed clips
     */
    fun removeAll(): List<Clip>

    /**
     * Returns the size of each track.
     * The key is the track index and the value is the number of clips on the track.
     */
    fun tracksSize(): Map<Int, Int>

    /**
     * Returns the list of clips on a track.
     *
     * @param track the track index
     * @return the clips or empty if the track is empty
     */
    fun clips(track: Int): List<Clip>

    companion object {
        // Numbers for these tracks are allowing for developers to insert clips in between
        // the regular and priority tracks. These numbers values can change without issue, but the
        // order should be maintained.
        const val TRACK_REGULAR = 10
        const val TRACK_PRIORITY = 20
    }
}

internal class AudioMixerImpl(
    private val log: Log = SharedLog,
) : AudioMixer {
    private val tracks = mutableMapOf<Int, MutableList<Clip>>()

    private val _current = MutableStateFlow<Clip?>(null)
    override val current = _current.asStateFlow()

    override var size: Int = 0
        private set

    override fun updateCurrent() {
        _current.value = tracks.keys.maxOrNull()?.let { maxTrack ->
            tracks[maxTrack]?.firstOrNull()
        }
    }

    override fun insert(
        clip: Clip,
        interrupts: Boolean,
    ) {
        val current = _current.value
        if (interrupts && current != null && current.track < clip.track) {
            insertInterruption(current, clip)
        } else {
            insertInternal(clip)
        }
    }

    override fun removeClip(clip: Clip): Clip? {
        return tracks[clip.track]?.let { clips ->
            val removed = clips.remove(clip)
            if (clips.isEmpty()) {
                tracks.remove(clip.track)
            }
            if (removed) {
                size--
                log.d(TAG) { "removed clip tracksSize=${tracksSize()}, clip=$clip" }
                clip
            } else {
                null
            }
        }
    }

    override fun removeTrack(track: Int): List<Clip> {
        return tracks.remove(track)?.let { clips ->
            size -= clips.size
            clips.toList().also {
                log.d(TAG) { "removed track $track tracksSize=${tracksSize()}, clips=${it.joinToString()}" }
            }
        } ?: emptyList()
    }

    override fun removeAll(): List<Clip> {
        val all = tracks.values.flatten()
        tracks.clear()
        size = 0
        log.w(TAG) { "removed all tracksSize=${tracksSize()}, clips=${all.joinToString()}" }
        return all
    }

    override fun tracksSize(): Map<Int, Int> {
        return tracks.mapValues { it.value.size }
    }

    override fun clips(track: Int): List<Clip> {
        return tracks[track]?.toList() ?: emptyList()
    }

    private fun insertInternal(clip: Clip) {
        val clips = tracks[clip.track]
        if (clips == null) {
            tracks[clip.track] = mutableListOf(clip)
        } else {
            clips.add(clip)
        }
        size++
        log.d(TAG) { "inserted clip tracksSize=${tracksSize()}, clip=$clip" }
    }

    private fun insertInterruption(deferred: Clip, priority: Clip) {
        val deferredTrack = tracks[deferred.track] ?: run {
            log.w(TAG) { "insertInterruption deferred track not found, resumed with priority clip" }
            insertInternal(priority)
            return
        }
        val deferredIndex = deferredTrack.indexOf(deferred)
        deferredTrack[deferredIndex] = AudioMixer.InterruptionEnd(deferred)
        insertInternal(AudioMixer.InterruptionStart(deferred, priority))
    }

    private companion object {
        private const val TAG = "AudioMixer"
    }
}
