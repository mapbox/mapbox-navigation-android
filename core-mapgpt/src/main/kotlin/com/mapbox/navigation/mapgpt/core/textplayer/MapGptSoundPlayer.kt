package com.mapbox.navigation.mapgpt.core.textplayer

import android.content.Context
import android.media.MediaPlayer
import com.mapbox.navigation.mapgpt.core.common.SharedLog
import com.mapbox.navigation.mapgpt.core.common.w
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.resume

internal class MapGptSoundPlayer(
    private val context: Context,
    private val attributes: PlayerAttributes,
) : SoundPlayer {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var mediaPlayer: MediaPlayer? = null

    override suspend fun playSound(sound: Sound) {
        when (sound) {
            is Sound.CustomSound -> {
                playEffect(sound.assetFileName)
            }
            Sound.StartInterruptionSound -> {
                playEffect("ding-separator-start.mp3")
            }
            Sound.StopInterruptionSound -> {
                playEffect("ding-separator-end.mp3")
            }
        }
    }

    private suspend fun playEffect(assetFileName: String) = withContext(scope.coroutineContext) {
        suspendCancellableCoroutine { cont ->
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer().apply {
                    attributes.applyOn(this)
                }
            }
            val sound = context.assets.openFd(assetFileName)
            mediaPlayer?.apply {
                reset()
                try {
                    setDataSource(sound.fileDescriptor, sound.startOffset, sound.length)
                } catch (ise: IllegalStateException) {
                    SharedLog.w(TAG, ise) {
                        "Impossible to set data source cause player is in incorrect state."
                    }
                    cont.resume(Unit)
                    return@apply
                } catch (iae: IllegalArgumentException) {
                    SharedLog.w(TAG, iae) {
                        "Impossible to set data source cause file descriptor is not valid."
                    }
                    cont.resume(Unit)
                    return@apply
                } catch (ioexc: IOException) {
                    SharedLog.w(TAG, ioexc) {
                        "Impossible to set data source cause file descriptor can not be read."
                    }
                    cont.resume(Unit)
                    return@apply
                }
                setOnCompletionListener {
                    cont.resume(Unit)
                }
                setOnPreparedListener { mp ->
                    mp.start()
                }
                prepareAsync()
            }

            cont.invokeOnCancellation { stopInternal() }
        }
    }

    private fun stopInternal() {
        mediaPlayer?.stop()
    }

    override fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private companion object {
        private const val TAG = "MapGptSoundPlayer"
    }
}
