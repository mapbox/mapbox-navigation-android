package com.mapbox.navigation.mapgpt.core.audio

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.WorkerThread
import androidx.core.app.ActivityCompat
import com.mapbox.navigation.mapgpt.core.microphone.MicrophoneMiddleware
import com.mapbox.navigation.mapgpt.core.microphone.MicrophoneProvider
import com.mapbox.navigation.mapgpt.core.microphone.PlatformMicrophone
import com.mapbox.navigation.mapgpt.core.MapGptCoreContext
import com.mapbox.navigation.mapgpt.core.common.SharedLog
import com.mapbox.navigation.mapgpt.core.common.SharedLogLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * A [MicrophoneMiddleware] that uses the Android [AudioRecord] API to stream audio from the
 * device's microphone. This implementation is suitable for real-time audio streaming.
 *
 * Requires the `Manifest.permission.RECORD_AUDIO` permission.
 */
class AudioRecordMicrophoneMiddleware : MicrophoneMiddleware() {
    override val provider: MicrophoneProvider = Companion.provider

    private var _config: PlatformMicrophone.Config = PlatformMicrophone.Config()
    override val config: PlatformMicrophone.Config get() = _config

    private val _state = MutableStateFlow<PlatformMicrophone.State>(
        PlatformMicrophone.State.Disconnected,
    )
    override val state: StateFlow<PlatformMicrophone.State> = _state

    private var audioRecordBuffer: Pair<AudioRecord, ByteArray>? = null

    /**
     * Called when the middleware is attached to the platform. This is a good place to initialize
     * resources that are needed for the microphone. There is no guarantee that microphone
     * permissions have been granted at this point.
     */
    override fun onAttached(middlewareContext: MapGptCoreContext) {
        SharedLog.d(TAG) { "onAttached" }
        super.onAttached(middlewareContext)
        launchAudioDeviceLogging(middlewareContext)
        _state.value = PlatformMicrophone.State.Idle
    }

    override fun onDetached(middlewareContext: MapGptCoreContext) {
        SharedLog.d(TAG) { "onDetached" }
        super.onDetached(middlewareContext)
        _state.value = PlatformMicrophone.State.Disconnected
        audioRecordBuffer?.first?.let { audioRecord ->
            audioRecordBuffer = null
            SharedLog.d(TAG) { "Calling audioRecord.release()" }
            audioRecord.release()
        }
    }

    /**
     * The [AudioRecord] API requires the `Manifest.permission.RECORD_AUDIO` permission.
     * This method ensures [AudioRecord] is initialized and the permission is granted.
     */
    override fun hasPermission(): Boolean {
        val middlewareContext = middlewareContext
            ?: return false
        val androidContext = middlewareContext.platformContext.applicationContext
        val isGranted = ActivityCompat.checkSelfPermission(
            androidContext,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        return if (isGranted) {
            val audioRecord = audioRecordBuffer ?: createAudioRecordBuffer()
            audioRecord?.first?.state == AudioRecord.STATE_INITIALIZED
        } else {
            false
        }
    }

    override suspend fun stream(consumer: (PlatformMicrophone.State.Streaming) -> Unit) {
        val audioRecordBuffer = audioRecordBuffer ?: run {
            onError("Cannot stream when AudioRecord is unavailable.")
            return
        }
        SharedLog.i(TAG) { "stream $config" }
        streamAudioBytes(
            audioRecord = audioRecordBuffer.first,
            byteArray = audioRecordBuffer.second,
        ) { chunk ->
            _state.value = chunk
            consumer.invoke(chunk)
        }
    }

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
                SharedLog.w(TAG) {
                    "Requested sample rate ${config.sampleRateHz} does not match " +
                        "AudioRecord sample rate ${audioRecord.sampleRate}." +
                        "Updating config.sampleRateHz to ${audioRecord.sampleRate}"
                }
                _config = config.copy(sampleRateHz = audioRecord.sampleRate)
            }
            SharedLog.d(TAG) {
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
        wrappedConsumer: (PlatformMicrophone.State.Streaming) -> Unit,
    ) = suspendCancellableCoroutine<Unit> { cont ->
        wrappedConsumer.invoke(
            PlatformMicrophone.State.Streaming(
                chunkId = 0,
                byteArray = byteArray,
                bytesRead = 0,
            ),
        )
        SharedLog.d(TAG) { "Before startRecording ${AudioInfoRetriever.stateString(audioRecord)}" }
        audioRecord.startRecording()
        SharedLog.d(TAG) { "After startRecording: ${AudioInfoRetriever.stateString(audioRecord)}" }
        cont.invokeOnCancellation {
            SharedLog.d(TAG) { "stream cancelled, calling audioRecord.stop()" }
            audioRecord.stop()
        }
        while (
            cont.isActive &&
            audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING
        ) {
            val previousState = state.value as? PlatformMicrophone.State.Streaming ?: break
            audioRecord.read(byteArray, 0, byteArray.size).let { bytesRead ->
                if (bytesRead < 0) {
                    onError("AudioRecord read failed $bytesRead")
                } else if (bytesRead > 0) {
                    wrappedConsumer.invoke(
                        PlatformMicrophone.State.Streaming(
                            chunkId = previousState.chunkId + 1,
                            byteArray = byteArray,
                            bytesRead = bytesRead,
                        )
                    )
                }
            }
        }
        _state.value = PlatformMicrophone.State.Idle
        SharedLog.d(TAG) { "Done streaming" }
    }

    private fun onError(reason: String) {
        SharedLog.i(TAG) { "onError $reason" }
        _state.value = PlatformMicrophone.State.Error(reason)
        stop()
    }

    override fun stop() {
        SharedLog.i(TAG) { "stop" }
        audioRecordBuffer?.first?.let { audioRecord ->
            SharedLog.d(TAG) { "Calling audioRecord.stop()" }
            audioRecord.stop()
        }
        _state.value = PlatformMicrophone.State.Idle
    }

    private fun launchAudioDeviceLogging(middlewareContext: MapGptCoreContext) {
        val androidContext = middlewareContext.platformContext.applicationContext
        val audioManager = AudioInfoRetriever.getAudioManager(androidContext) ?: run {
            SharedLog.e(TAG) { "AudioManager is null, cannot log audio devices" }
            return
        }
        if (SharedLog.isEnabled(SharedLogLevel.Debug, TAG)) {
            AudioInfoRetriever.logAudioDevices(audioManager)
            AudioInfoRetriever.observeRecordingConfigurationChanges(audioManager)
                .onEach { logString -> SharedLog.d(TAG) { logString } }
                .launchIn(ioScope)
            AudioInfoRetriever.observeAudioDeviceChanges(audioManager)
                .onEach { logString -> SharedLog.d(TAG) { logString } }
                .launchIn(ioScope)
            AudioInfoRetriever.observePlaybackConfigurationChanges(audioManager)
                .onEach { logString -> SharedLog.d(TAG) { logString } }
                .launchIn(ioScope)
        } else {
            SharedLog.i(TAG) { "Debug logging is disabled, skipping audio device logging" }
        }
    }

    companion object {
        val provider = MicrophoneProvider("android_audiorecord")

        private const val TAG = "AudioRecordMicrophoneMiddleware"

        private const val AUDIO_SOURCE: Int = MediaRecorder.AudioSource.VOICE_RECOGNITION
        private const val CHANNEL_CONFIG: Int = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT: Int = AudioFormat.ENCODING_PCM_16BIT

        // Use a larger buffer size for better performance (e.g., 4 times the minimum size)
        private const val BUFFER_MULTIPLIER = 4
    }
}
