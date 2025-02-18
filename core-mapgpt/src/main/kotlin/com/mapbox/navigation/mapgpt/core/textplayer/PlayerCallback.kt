package com.mapbox.navigation.mapgpt.core.textplayer

/**
 * The PlayerCallback interface defines the callbacks that are triggered during the
 * playback of an announcement by a [VoicePlayer].
 */
interface PlayerCallback {

    /**
     * Called when playback of an announcement starts.
     *
     * @param utteranceId the identifier of the announcement being played
     */
    @Throws(Throwable::class)
    fun onStartPlaying(text: String?, utteranceId: String)

    /**
     * Called when playback of an announcement completes.
     *
     * @param utteranceId the identifier of the announcement that was played
     */
    @Throws(Throwable::class)
    fun onComplete(text: String?, utteranceId: String)

    /**
     * Called when playback of an announcement is stopped.
     *
     * @param utteranceId the identifier of the announcement that was stopped
     * @param progress the [VoiceProgress] indicating the position at which playback was stopped
     */
    @Throws(Throwable::class)
    fun onStop(utteranceId: String, progress: VoiceProgress)

    /**
     * Called when an error occurs during playback.
     *
     * @param utteranceId the identifier of the announcement during which the error occurred
     * @param reason description of the error (optional)
     */
    @Throws(Throwable::class)
    fun onError(utteranceId: String, reason: String?)
}
