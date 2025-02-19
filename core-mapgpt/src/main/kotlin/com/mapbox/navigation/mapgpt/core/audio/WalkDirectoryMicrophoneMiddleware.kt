package com.mapbox.navigation.mapgpt.core.audio

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import androidx.core.content.ContextCompat
import com.mapbox.navigation.mapgpt.core.MapGptCoreContext
import com.mapbox.navigation.mapgpt.core.common.SharedLog
import com.mapbox.navigation.mapgpt.core.microphone.MicrophoneMiddleware
import com.mapbox.navigation.mapgpt.core.microphone.MicrophoneProvider
import com.mapbox.navigation.mapgpt.core.microphone.PlatformMicrophone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream


/**
 * A [MicrophoneMiddleware] that will walk a directory and stream audio files from it.
 *
 * On your local machine, you can convert MP3 files to WAV PCM 16-bit 44100 Hz with ffmpeg.
 * Additionally, some STT services need silence to detect the final words. Concatenate silence
 * to the audio files for improved results:
 *
 * ```bash
 * # Generate 2 seconds of silence
 * ffmpeg -f lavfi -t 2 -i anullsrc=channel_layout=stereo:sample_rate=44100 silence.wav
 *
 * # Convert MP3 to WAV PCM 16-bit 44100 Hz
 * ffmpeg -i "$file" -acodec pcm_s16le -ar 44100 "${filename}.wav"
 *
 * # Concatenate the original WAV file and the silent audio file
 * ffmpeg -i "${filename}.wav" \
 *        -i silence.wav \
 *        -filter_complex "[0:0][1:0]concat=n=2:v=0:a=1" "${filename}_with_silence.wav"
 *
 * # Clean up temporary files
 * rm silence.wav
 * rm "${filename}.wav"
 * ```
 *
 * Push audio files to the directory on the device:
 *
 * ```bash
 * # Define the source directory containing audio files
 * src_dir=~/Desktop/audio-files/{star}
 *
 * # Define the destination app package name
 * dst_pkg=com.mapbox.dash.app
 *
 * # Define the destination directory on the device
 * android_dst=/storage/emulated/0/Android/data/$dst_pkg/files/mbx_sdk_inject_microphone_audio
 *
 * # Push audio files from the source to the destination directory on the device
 * adb push $src_dir $android_dst
 * ```
 *
 * @param directory The directory to walk. If null, an external files directory will be used.
 *                  You will need to push audio files to the directory with extension "wav".
 */
class WalkDirectoryMicrophoneMiddleware(
    private val directory: File? = null,
) : MicrophoneMiddleware() {

    override val provider: MicrophoneProvider = MicrophoneProvider("walk_directory")
    override val config: PlatformMicrophone.Config = defaultConfig
    private val _state = MutableStateFlow<PlatformMicrophone.State>(PlatformMicrophone.State.Disconnected)
    override val state: StateFlow<PlatformMicrophone.State> get() = _state

    private var audioDirectory: File? = null
    private var audioFile: File? = null

    override fun onAttached(middlewareContext: MapGptCoreContext) {
        super.onAttached(middlewareContext)
        SharedLog.d(TAG) { "onAttached" }
        val androidContext = middlewareContext.platformContext.applicationContext
        val audioDir = directory ?: androidContext.getExternalFilesDir(DEFAULT_DIRECTORY_NAME)
        if (audioDir == null || !audioDir.exists()) {
            SharedLog.e(TAG) { "Audio directory does not exist: ${audioDir?.absolutePath}" }
            _state.value = PlatformMicrophone.State.Disconnected
            return
        }
        audioDirectory = audioDir
        audioFile = nextAudioFile()
        SharedLog.d(TAG) {
            "Audio directory set for microphone streaming: ${audioDirectory?.absolutePath}"
        }
    }

    override fun onDetached(middlewareContext: MapGptCoreContext) {
        super.onDetached(middlewareContext)
        SharedLog.d(TAG) { "onDetached" }
    }

    override fun hasPermission(): Boolean {
        if (directory != null) return true
        val middlewareContext = middlewareContext ?: return false
        val androidContext = middlewareContext.platformContext.applicationContext
        return ContextCompat.checkSelfPermission(
            androidContext,
            READ_EXTERNAL_STORAGE,
        ) == PackageManager.PERMISSION_GRANTED
    }

    override suspend fun stream(consumer: (PlatformMicrophone.State.Streaming) -> Unit) {
        val file = audioFile?.takeIf { it.isFile } ?: run {
            SharedLog.d(TAG) { "Reached end of directory. Retry to start it again." }
            audioFile = nextAudioFile()
            _state.value = PlatformMicrophone.State.Idle
            return
        }
        audioFile = nextAudioFile()
        SharedLog.d(TAG) {
            "Streaming audio file: ${file.absolutePath}, " +
                "minBufferSizeBytes: $minBufferSizeBytes, " +
                "bufferSizeBytes: $bufferSizeBytes"
        }
        streamAudioBytes(file, ByteArray(bufferSizeBytes)) { chunk ->
            _state.value = chunk
            consumer(chunk)
        }

        _state.value = PlatformMicrophone.State.Idle
    }

    private suspend fun streamAudioBytes(
        file: File,
        byteArray: ByteArray,
        consumer: (PlatformMicrophone.State.Streaming) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val metadata = AudioInfoRetriever.audioMetadataMap(file)
        val bitrate = metadata["bitrate"]?.toIntOrNull() ?: DEFAULT_BITRATE
        SharedLog.d(TAG) { "Audio metadata: $metadata" }
        consumer.invoke(
            PlatformMicrophone.State.Streaming(
                chunkId = 0,
                byteArray = byteArray,
                bytesRead = 0,
            ),
        )
        FileInputStream(file).use { inputStream ->
            var chunkId = 0
            while (state.value is PlatformMicrophone.State.Streaming) {
                val bytesRead = inputStream.read(byteArray)
                if (bytesRead < 0) {
                    SharedLog.d(TAG) { "End of file reached" }
                    break
                } else {
                    val chunk = PlatformMicrophone.State.Streaming(
                        chunkId = chunkId++,
                        byteArray = byteArray,
                        bytesRead = bytesRead,
                    )
                    val delayMillis = calculateDelayMillis(bytesRead, bitrate)
                    SharedLog.d(TAG) {
                        "Streaming chunk ${chunkId - 1} with size $bytesRead bytes. " +
                            "Delay: $delayMillis millis"
                    }
                    delay(delayMillis)
                    consumer.invoke(chunk)
                }
            }
            SharedLog.d(TAG) { "Finished sending ${file.absolutePath}" }
        }
    }

    override fun stop() {
        _state.value = PlatformMicrophone.State.Idle
    }

    /**
     * We could cache the list of audio files in the directory and keep track of the current index.
     * But this also allows for the directory to change between playbacks.
     * Considering the use case is functional, it is not optimized for speed.
     */
    private fun nextAudioFile(): File? {
        // Check if the directory is set and exists
        val audioDir = audioDirectory
        if (audioDir == null || !audioDir.exists()) {
            val reason = "Audio directory is not set or does not exist."
            SharedLog.e(TAG) { reason }
            _state.value = PlatformMicrophone.State.Error(reason)
            return null
        }

        // Filter the directory tree to include only audio files with the specified extensions
        val directoryTree = audioDir.walkTopDown()
            .filter { it.isFile && it.extension == AUDIO_EXTENSION }
            .toList()

        // Check if the directory tree is empty
        return if (directoryTree.isEmpty()) {
            val reason = "No audio files found in directory: ${audioDir.absolutePath}"
            SharedLog.e(TAG) { reason }
            _state.value = PlatformMicrophone.State.Error(reason)
            null
        } else {
            // Find the next supported audio file. Null when reaching the end of the list.
            val currentIndex = directoryTree.indexOf(audioFile) + 1
            SharedLog.d(TAG) { "Next index: $currentIndex" }
            directoryTree.getOrNull(currentIndex)
        }
    }

    @Suppress("MagicNumber")
    private fun calculateDelayMillis(bytesRead: Int, bitrate: Int): Long {
        return (bytesRead * 8 * 1000L) / bitrate
    }

    private companion object {
        private const val TAG = "WalkDirectoryMicrophoneMiddleware"

        private val defaultConfig = PlatformMicrophone.Config()
        private const val DEFAULT_DIRECTORY_NAME = "/mbx_sdk_inject_microphone_audio/"

        // Make sure to covert the audio files to PCM 16-bit 44100 Hz
        private const val AUDIO_EXTENSION = "wav"

        // Use a larger buffer size for better performance (e.g., 4 times the minimum size)
        private const val BUFFER_MULTIPLIER = 4

        // Default bitrate in bits per second, but it should come from the audio file.
        private const val DEFAULT_BITRATE = 700_000

        private val minBufferSizeBytes = AudioRecord.getMinBufferSize(
            defaultConfig.sampleRateHz,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        private val bufferSizeBytes = minBufferSizeBytes * BUFFER_MULTIPLIER
    }
}
