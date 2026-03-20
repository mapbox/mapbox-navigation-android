package com.mapbox.navigation.testing.voicefeedback

import android.media.AudioFormat
import android.media.AudioRecord
import android.util.Log
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.voicefeedback.internal.audio.microphone.Microphone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.io.InputStream

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class InputStreamMicrophone(
    private val inputStreamProvider: () -> InputStream,
) : Microphone {
    override val config: Microphone.Config = Microphone.Config()
    override val state =
        MutableStateFlow<Microphone.State>(Microphone.State.Disconnected)

    private val minBufferSizeBytes = AudioRecord.getMinBufferSize(
        config.sampleRateHz,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
    )

    private val bufferSizeBytes = minBufferSizeBytes * BUFFER_MULTIPLIER

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        // no-op
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        // no-op
    }

    override suspend fun stream(consumer: (Microphone.State.Streaming) -> Unit) {
        Log.d(
            TAG,
            "Streaming audio, " +
                "minBufferSizeBytes: $minBufferSizeBytes, " +
                "bufferSizeBytes: $bufferSizeBytes",
        )
        streamAudioBytes(inputStreamProvider, ByteArray(bufferSizeBytes)) { chunk ->
            state.value = chunk
            consumer(chunk)
        }

        state.value = Microphone.State.Idle
    }

    private suspend fun streamAudioBytes(
        stream: () -> InputStream,
        byteArray: ByteArray,
        consumer: (Microphone.State.Streaming) -> Unit,
    ) = withContext(Dispatchers.IO) {
        consumer.invoke(
            Microphone.State.Streaming(
                chunkId = 0,
                byteArray = byteArray,
                bytesRead = 0,
            ),
        )
        stream().use { inputStream ->
            var chunkId = 0
            while (state.value is Microphone.State.Streaming) {
                val bytesRead = inputStream.read(byteArray)
                if (bytesRead < 0) {
                    Log.d(TAG, "End of stream reached")
                    break
                } else {
                    val chunk = Microphone.State.Streaming(
                        chunkId = chunkId++,
                        byteArray = byteArray,
                        bytesRead = bytesRead,
                    )
                    val delayMillis = calculateDelayMillis(bytesRead, DEFAULT_BITRATE)

                    Log.d(
                        TAG,
                        "Streaming chunk ${chunkId - 1} with size $bytesRead bytes. " +
                            "Delay: $delayMillis millis",
                    )
                    delay(delayMillis)
                    consumer.invoke(chunk)
                }
            }
        }
    }

    @Suppress("MagicNumber")
    private fun calculateDelayMillis(bytesRead: Int, bitrate: Int): Long {
        return (bytesRead * 8 * 1000L) / bitrate
    }

    override fun stop() {
        state.value = Microphone.State.Idle
    }

    private companion object {
        private const val TAG = "InputStreamMicrophone"

        // Make sure to covert the audio files to PCM 16-bit 44100 Hz
        private const val AUDIO_EXTENSION = "wav"

        private const val BUFFER_MULTIPLIER = 4

        private const val DEFAULT_BITRATE = 700_000
    }
}
