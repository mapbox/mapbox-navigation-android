package com.mapbox.navigation.voicefeedback.internal.audio.microphone

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import androidx.annotation.WorkerThread
import androidx.core.app.ActivityCompat
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.utils.internal.logD
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigation.utils.internal.logI
import com.mapbox.navigation.utils.internal.logW
import com.mapbox.navigation.voicefeedback.Microphone
import com.mapbox.navigation.voicefeedback.internal.audio.AudioInfoRetriever
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * A [com.mapbox.navigation.voicefeedback.Microphone] that uses the Android [AudioRecord] API to stream audio from the
 * device's microphone. This implementation is suitable for real-time audio streaming.
 *
 * Requires the `Manifest.permission.RECORD_AUDIO` permission.
 */
@ExperimentalPreviewMapboxNavigationAPI
internal class AudioRecordMicrophone : Microphone {

    private lateinit var context: Context

    private var _config: Microphone.Config = Microphone.Config()

    /**
     * Current microphone configuration including sample rate and other audio parameters.
     */
    override val config: Microphone.Config get() = _config

    private val _state = MutableStateFlow<Microphone.State>(
        Microphone.State.Disconnected,
    )

    /**
     * Current state of the microphone (Disconnected, Idle, Streaming, or Error).
     */
    override val state: StateFlow<Microphone.State> = _state

    private var audioRecordBuffer: Pair<AudioRecord, ByteArray>? = null

    private var audioManager: AudioManager? = null

    private lateinit var coroutineScope: CoroutineScope

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        this.context = mapboxNavigation.navigationOptions.applicationContext
        audioManager = AudioInfoRetriever.getAudioManager(context)
        _state.value = Microphone.State.Idle
        launchAudioDeviceLogging(audioManager)
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        _state.value = Microphone.State.Disconnected
        audioRecordBuffer?.first?.let { audioRecord ->
            audioRecordBuffer = null
            logD(TAG) { "Calling audioRecord.release()" }
            audioRecord.release()
        }
        coroutineScope.cancel()
    }

    private fun launchAudioDeviceLogging(audioManager: AudioManager?) {
        if (audioManager == null) {
            logE(TAG) { "AudioManager is null, cannot log audio devices" }
            return
        }

        coroutineScope.launch(Dispatchers.IO) { AudioInfoRetriever.logAudioDevices(audioManager) }
        coroutineScope.launch(Dispatchers.IO) {
            AudioInfoRetriever.observeRecordingConfigurationChanges(audioManager)
                .collect { logString -> logD(TAG) { logString } }
        }
        coroutineScope.launch(Dispatchers.IO) {
            AudioInfoRetriever.observeAudioDeviceChanges(audioManager)
                .collect { logString -> logD(TAG) { logString } }
        }
        coroutineScope.launch(Dispatchers.IO) {
            AudioInfoRetriever.observePlaybackConfigurationChanges(audioManager)
                .collect { logString -> logD(TAG) { logString } }
        }
    }

    /**
     * Streams audio data from the microphone to the provided consumer function.
     * Requires [AudioRecord] to be properly initialized and permission to be granted.
     *
     * @param consumer Function that receives streaming audio data chunks
     */
    override suspend fun stream(consumer: (Microphone.State.Streaming) -> Unit) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            onError("Cannot stream when RECORD_AUDIO permission is not granted.")
            return
        }
        val audioRecordBuffer = audioRecordBuffer ?: createAudioRecordBuffer() ?: run {
            onError("Cannot stream when AudioRecord is unavailable.")
            return
        }
        logI(TAG) { "stream $config" }
        streamAudioBytes(
            audioRecord = audioRecordBuffer.first,
            byteArray = audioRecordBuffer.second,
        ) { chunk ->
            _state.value = chunk
            consumer.invoke(chunk)
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun createAudioRecordBuffer(): Pair<AudioRecord, ByteArray>? {
        val minBufferSizeBytes = AudioRecord.getMinBufferSize(
            config.sampleRateHz,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
        )
        val bufferSizeBytes = minBufferSizeBytes * BUFFER_MULTIPLIER
        val audioRecord = AudioRecord(
            AUDIO_SOURCE,
            config.sampleRateHz,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSizeBytes,
        )
        if (audioRecord.state == AudioRecord.STATE_INITIALIZED) {
            audioRecordBuffer = audioRecord to ByteArray(bufferSizeBytes)
            if (config.sampleRateHz != audioRecord.sampleRate) {
                logW(TAG) {
                    "Requested sample rate ${config.sampleRateHz} does not match " +
                        "AudioRecord sample rate ${audioRecord.sampleRate}." +
                        "Updating config.sampleRateHz to ${audioRecord.sampleRate}"
                }
                _config = config.copy(sampleRateHz = audioRecord.sampleRate)
            }
            logD(TAG) {
                "AudioRecord initialized minBufferSizeBytes: $minBufferSizeBytes, " +
                    "bufferSizeBytes: $bufferSizeBytes, " +
                    AudioInfoRetriever.stateString(audioRecord)
            }
        } else {
            onError("AudioRecord initialization failed, releasing AudioRecord.")
            audioRecordBuffer = null
            audioRecord.release()
        }
        return audioRecordBuffer
    }

    @WorkerThread
    private suspend fun streamAudioBytes(
        audioRecord: AudioRecord,
        byteArray: ByteArray,
        wrappedConsumer: (Microphone.State.Streaming) -> Unit,
    ) = suspendCancellableCoroutine<Unit> { cont ->
        wrappedConsumer.invoke(
            Microphone.State.Streaming(
                chunkId = 0,
                byteArray = byteArray,
                bytesRead = 0,
            ),
        )
        logD(TAG) { "Before startRecording ${AudioInfoRetriever.stateString(audioRecord)}" }
        audioRecord.startRecording()
        logD(TAG) { "After startRecording: ${AudioInfoRetriever.stateString(audioRecord)}" }
        cont.invokeOnCancellation {
            logD(TAG) { "stream cancelled, calling audioRecord.stop()" }
            audioRecord.stop()
        }
        while (
            cont.isActive &&
            audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING
        ) {
            val previousState = state.value as? Microphone.State.Streaming ?: break
            audioRecord.read(byteArray, 0, byteArray.size).let { bytesRead ->
                if (bytesRead < 0) {
                    onError("AudioRecord read failed $bytesRead")
                } else if (bytesRead > 0) {
                    wrappedConsumer.invoke(
                        Microphone.State.Streaming(
                            chunkId = previousState.chunkId + 1,
                            byteArray = byteArray,
                            bytesRead = bytesRead,
                        ),
                    )
                }
            }
        }
        _state.value = Microphone.State.Idle
        logD(TAG) { "Done streaming" }
    }

    private fun onError(reason: String) {
        logI(TAG) { "onError $reason" }
        _state.value = Microphone.State.Error(reason)
        stop()
    }

    /**
     * Stops the current audio recording session and transitions state to [Microphone.State.Idle].
     */
    override fun stop() {
        logI(TAG) { "stop" }
        audioRecordBuffer?.first?.let { audioRecord ->
            logD(TAG) { "Calling audioRecord.stop()" }
            audioRecord.stop()
        }
        _state.value = Microphone.State.Idle
    }

    companion object {
        private const val TAG = "AudioRecordMicrophone"

        private const val AUDIO_SOURCE: Int = MediaRecorder.AudioSource.VOICE_RECOGNITION
        private const val CHANNEL_CONFIG: Int = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT: Int = AudioFormat.ENCODING_PCM_16BIT

        // Use a larger buffer size for better performance (e.g., 4 times the minimum size)
        private const val BUFFER_MULTIPLIER = 4
    }
}
