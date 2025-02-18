package com.mapbox.navigation.mapgpt.core.music

interface MusicPlayerState {

    /**
     * The monotonic clock value in milliseconds when the state was updated.
     */
    val elapsedRealtime: Long

    /**
     * Word that describes the state. See [PlaybackState] for possible states.
     */
    val playbackState: PlaybackState

    /**
     * Returns the name of the track that is currently playing.
     */
    val trackName: String?

    /**
     * Returns the name of the track that is currently playing.
     */
    val artistName: String?

    /**
     * Link to the album art of the track that is currently playing.
     */
    val albumArtUri: String?

    /**
     * Returns the duration of the track in milliseconds.
     */
    val duration: Long

    /**
     * Returns the position of the track in milliseconds where 0 is the starting position.
     */
    val position: Long

    /**
     * The current playback rate where 1.0 is normal, 0 is paused or stopped, negative is reversed.
     */
    val playbackSpeed: Double

    /**
     * Current track shuffle mode, if known. See [ShuffleMode] for possible modes.
     */
    val shuffleMode: ShuffleMode?

    /**
     * Current track repeat mode, if known. See [RepeatMode] for possible modes.
     */
    val repeatMode: RepeatMode?

    /**
     * Describes current playback state.
     */
    enum class PlaybackState {
        Unknown, Playing, Paused, Stopped
    }

    /**
     * The mode which controls the playback queue ordering.
     */
    enum class ShuffleMode {
        None, Shuffle
    }

    /**
     * The mode which determines if(how) playlist or tracks are repeated for playback.
     */
    enum class RepeatMode {
        None, RepeatOne, RepeatAll
    }
}
