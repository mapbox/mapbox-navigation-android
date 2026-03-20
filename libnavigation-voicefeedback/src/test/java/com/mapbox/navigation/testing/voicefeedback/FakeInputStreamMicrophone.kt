package com.mapbox.navigation.testing.voicefeedback

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.voicefeedback.internal.audio.microphone.Microphone
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.InputStream

/**
 * JVM-compatible test double for [com.mapbox.navigation.feedback.voice.internal.audio.microphone.Microphone] that streams audio from an [java.io.InputStream].
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class FakeInputStreamMicrophone(
    private val bufferSizeBytes: Int = DEFAULT_BUFFER_SIZE,
    private val inputStreamProvider: () -> InputStream,
) : Microphone {

    override val config: Microphone.Config = Microphone.Config()
    override val state = MutableStateFlow<Microphone.State>(Microphone.State.Disconnected)

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        // no-op for unit tests
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        // no-op for unit tests
    }

    override suspend fun stream(consumer: (Microphone.State.Streaming) -> Unit) {
        val byteArray = ByteArray(bufferSizeBytes)
        val initialChunk = Microphone.State.Streaming(
            chunkId = 0,
            byteArray = byteArray,
            bytesRead = 0,
        )
        state.value = initialChunk
        consumer(initialChunk)

        inputStreamProvider().use { inputStream ->
            var chunkId = 1
            while (state.value is Microphone.State.Streaming) {
                val bytesRead = inputStream.read(byteArray)
                if (bytesRead < 0) {
                    break
                }
                val chunk = Microphone.State.Streaming(
                    chunkId = chunkId++,
                    byteArray = byteArray,
                    bytesRead = bytesRead,
                )
                state.value = chunk
                consumer(chunk)
                delay(calculateDelayMillis(bytesRead))
            }
        }
        state.value = Microphone.State.Idle
    }

    @Suppress("MagicNumber")
    private fun calculateDelayMillis(bytesRead: Int): Long {
        return (bytesRead * 8 * 1000L) / DEFAULT_BITRATE
    }

    override fun stop() {
        state.value = Microphone.State.Idle
    }

    private companion object {
        private const val DEFAULT_BUFFER_SIZE = 4096
        private const val DEFAULT_BITRATE = 700_000
    }
}
