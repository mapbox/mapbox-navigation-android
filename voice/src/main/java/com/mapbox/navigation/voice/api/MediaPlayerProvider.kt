package com.mapbox.navigation.voice.api

import android.media.MediaPlayer

internal object MediaPlayerProvider {

    fun retrieveMediaPlayer(): MediaPlayer {
        return MediaPlayer()
    }
}
