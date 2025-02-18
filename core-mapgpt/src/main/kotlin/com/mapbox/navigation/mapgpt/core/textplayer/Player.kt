package com.mapbox.navigation.mapgpt.core.textplayer

import kotlinx.coroutines.flow.StateFlow

/**
 * An API that allows you to interact with the speech player.
 */
interface Player {

    /**
     * Exposes current state of the player.
     */
    val state: StateFlow<PlayerState>

    /**
     * Sets the player mute on or off
     *
     * When it's set to true the player immediately stops playing the current [Announcement],
     * the inner queue is flushed and doesn't collect new incoming [Announcement]
     */
    var isMuted: Boolean

    /**
     * If set to `true`, the player will prefer synthesizing the speech using an onboard TTS client,
     * if available.
     */
    var preferLocalTts: Boolean

    /**
     * If set to `true`, the player will only play [Announcement.Priority].
     */
    var playPriorityExclusively: Boolean

    /**
     * The method will try to pre-generate any resources needed to play the given [announcement].
     */
    fun prefetch(announcement: Announcement)

    /**
     * Insert an [announcement] to play based on order of priority
     */
    fun play(announcement: Announcement)

    /**
     * Insert an [notificationAlert] to play based on order of priority
     */
    fun play(notificationAlert: NotificationAlert)

    /**
     * Stops playback and clears any queued announcements.
     */
    fun clear()

    /**
     * Clears all queued [Announcement.Regular]s and stops playback if the currently playing
     * announcement is [Announcement.Regular].
     */
    fun clearRegularQueue()

    /**
     * Clears all queued [Announcement.Priority]s and stops playback if the currently playing
     * announcement is [Announcement.Priority].
     */
    fun clearPriorityQueue()
}
