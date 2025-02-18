package com.mapbox.navigation.mapgpt.core.audiofocus

import android.content.Context
import com.mapbox.navigation.mapgpt.core.audiofocus.AudioFocusDelegateProvider.defaultAudioFocusDelegate
import com.mapbox.navigation.mapgpt.core.common.SharedLog
import com.mapbox.navigation.mapgpt.core.textplayer.options.PlayerOptions

/**
 * An Api that allows you to interact with the audio focus in an asynchronous way.
 */
class MapGptAudioFocusManager(
    context: Context,
    playerOptions: PlayerOptions,
) : AudioFocusManager {

    private val audioFocusDelegate: AudioFocusManager =
        defaultAudioFocusDelegate(context, playerOptions)

    /**
     * Request audio focus. Send a request to obtain the audio focus
     * @param owner specifies the owner for request
     * @param callback invoked when the delegate processed the audio request
     */
    override fun request(
        owner: AudioFocusOwner,
        callback: AudioFocusRequestCallback,
    ) {
        SharedLog.d(TAG) {
            "request owner = ${owner::class.simpleName}"
        }
        audioFocusDelegate.request(owner, callback)
    }

    /**
     * Abandon audio focus. Causes the previous focus owner, if any, to receive the focus.
     * @param owner specifies the owner for request
     * @param callback invoked when the delegate processed the audio request
     */
    override fun abandon(owner: AudioFocusOwner, callback: AudioFocusRequestCallback) {
        SharedLog.d(TAG) {
            "abandon owner = ${owner::class.simpleName}"
        }
        audioFocusDelegate.abandon(owner, callback)
    }

    private companion object {

        private const val TAG = "MapGptAudioFocusManager"
    }
}
