package com.mapbox.navigation.ui.voice.api

import android.media.MediaPlayer

internal object MediaPlayerProvider {

    fun retrieveMediaPlayer(): MediaPlayer {
        return MediaPlayer()
    }
}
