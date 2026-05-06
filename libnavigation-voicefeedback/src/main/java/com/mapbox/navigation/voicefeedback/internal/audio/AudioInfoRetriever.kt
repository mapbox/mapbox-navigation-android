package com.mapbox.navigation.voicefeedback.internal.audio

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.media.AudioRecord
import android.media.AudioRecordingConfiguration
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.media.MicrophoneInfo
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.utils.internal.logD
import com.mapbox.navigation.utils.internal.logW
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * This object provides utility functions for retrieving audio information from the Android
 * platform. This includes information about the audio devices, audio recording configurations,
 * audio playback configurations, and audio metadata.
 *
 * Last updated for Api 33.
 */
@ExperimentalPreviewMapboxNavigationAPI
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
internal object AudioInfoRetriever {

    private const val TAG = "AudioInfoRetriever"

    fun getAudioManager(applicationContext: Context): AudioManager? {
        return applicationContext.getSystemService(
            Context.AUDIO_SERVICE,
        ) as? AudioManager
    }

    fun logAudioDevices(audioManager: AudioManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            logW(TAG) { "AudioDevice info unavailable for api ${Build.VERSION.SDK_INT}" }
            return
        }
        logD(TAG) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
            val deviceProperties = devices.joinToString { it.toLogString() }
            val microphoneProperties = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                audioManager.microphones.joinToString { it.toLogString() }
            } else {
                "VERSION.SDK_INT < P"
            }
            "AudioManager(" +
                "AudioDeviceInfo=[$deviceProperties], " +
                "MicrophoneInfo=[$microphoneProperties]" +
                ")"
        }
    }

    fun stateString(audioRecord: AudioRecord): String {
        val initializedState = when (val state = audioRecord.state) {
            AudioRecord.STATE_INITIALIZED -> "STATE_INITIALIZED"
            AudioRecord.STATE_UNINITIALIZED -> "STATE_UNINITIALIZED"
            else -> "Other state: $state"
        }
        val recordingState = when (val state = audioRecord.recordingState) {
            AudioRecord.RECORDSTATE_RECORDING -> "RECORDSTATE_RECORDING"
            AudioRecord.RECORDSTATE_STOPPED -> "RECORDSTATE_STOPPED"
            else -> "Other recording state: $state"
        }
        val channelCount = when (val count = audioRecord.channelCount) {
            1 -> "mono"
            2 -> "stereo"
            else -> "Other channel count: $count"
        }
        val audioFormat = when (val format = audioRecord.audioFormat) {
            AudioFormat.ENCODING_PCM_16BIT -> "ENCODING_PCM_16BIT"
            AudioFormat.ENCODING_PCM_8BIT -> "ENCODING_PCM_8BIT"
            else -> "Other audio format: $format"
        }
        val superString = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioRecord.format.toString()
        } else {
            "VERSION.SDK_INT < M"
        }
        return "AudioRecord(" +
            "initializedState=$initializedState, " +
            "recordingState=$recordingState, " +
            "channelCount=$channelCount, " +
            "audioFormat=$audioFormat, " +
            "sampleRate=${audioRecord.sampleRate}, " +
            "super=$superString)"
    }

    fun observeRecordingConfigurationChanges(audioManager: AudioManager): Flow<String> =
        callbackFlow {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val activeRecordingConfigurations = audioManager.activeRecordingConfigurations
                var propertiesString =
                    activeRecordingConfigurations.joinToString { it.toLogString() }
                trySend("activeRecordingConfigurations[$propertiesString]")
                val callback = object : AudioManager.AudioRecordingCallback() {
                    override fun onRecordingConfigChanged(
                        configs: MutableList<AudioRecordingConfiguration>?,
                    ) {
                        propertiesString = configs?.joinToString { it.toLogString() } ?: ""
                        trySend("onRecordingConfigChanged[$propertiesString]")
                    }
                }
                audioManager.registerAudioRecordingCallback(callback, null)
                awaitClose { audioManager.unregisterAudioRecordingCallback(callback) }
            } else {
                trySend("AudioRecordingConfiguration unavailable for api ${Build.VERSION.SDK_INT}")
                close()
            }
        }

    fun observeAudioDeviceChanges(audioManager: AudioManager): Flow<String> = callbackFlow {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val callback = object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                    super.onAudioDevicesAdded(addedDevices)
                    val propertiesString = addedDevices?.joinToString { it.toLogString() } ?: ""
                    trySend("onAudioDevicesAdded[$propertiesString]")
                }

                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                    super.onAudioDevicesRemoved(removedDevices)
                    val propertiesString = removedDevices?.joinToString { it.toLogString() } ?: ""
                    trySend("onAudioDevicesRemoved[$propertiesString]")
                }
            }
            audioManager.registerAudioDeviceCallback(callback, null)
            awaitClose { audioManager.unregisterAudioDeviceCallback(callback) }
        } else {
            trySend("AudioDeviceCallback unavailable for api ${Build.VERSION.SDK_INT}")
            close()
        }
    }

    fun observePlaybackConfigurationChanges(audioManager: AudioManager): Flow<String> =
        callbackFlow {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val callback = object : AudioManager.AudioPlaybackCallback() {
                    override fun onPlaybackConfigChanged(
                        configs: MutableList<AudioPlaybackConfiguration>?,
                    ) {
                        val propertiesString = configs?.joinToString { it.toLogString() } ?: ""
                        trySend("onPlaybackConfigChanged[$propertiesString]")
                    }
                }
                audioManager.registerAudioPlaybackCallback(callback, null)
                awaitClose { audioManager.unregisterAudioPlaybackCallback(callback) }
            } else {
                trySend("AudioDeviceCallback unavailable for api ${Build.VERSION.SDK_INT}")
                close()
            }
        }

    /**
     * This is a helper function to print out the properties of an [AudioDeviceInfo] object.
     */
    private fun AudioDeviceInfo.toLogString(): String {
        val properties = mutableMapOf<String, String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            properties["id"] = id.toString()
            properties["type"] = audioDeviceInfoTypeString(type)
            properties["isSource"] = isSource.toString()
            properties["isSink"] = isSink.toString()
            productName?.let { properties["productName"] = it.toString() }
            properties["sampleRates"] = sampleRates.joinToString()
            properties["channelCounts"] = channelCounts.joinToString()
            properties["channelMasks"] = channelMasks.joinToString()
            properties["channelIndexMasks"] = channelIndexMasks.joinToString()
            properties["encodings"] = encodings.joinToString()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            properties["address"] = address
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            properties["encapsulationModes"] = encapsulationModes.joinToString()
            properties["encapsulationMetadataTypes"] = encapsulationMetadataTypes.joinToString()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            properties["audioProfiles"] = audioProfiles.joinToString()
            properties["audioDescriptors"] = audioDescriptors.joinToString()
        }
        return properties.removeEmptyValues().toString()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun audioDeviceInfoTypeString(type: Int): String = when (type) {
        AudioDeviceInfo.TYPE_AUX_LINE -> "aux_line"
        AudioDeviceInfo.TYPE_BLE_BROADCAST -> "ble_broadcast"
        AudioDeviceInfo.TYPE_BLE_HEADSET -> "ble_headset"
        AudioDeviceInfo.TYPE_BLE_SPEAKER -> "ble_speaker"
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "bluetooth_a2dp"
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "bluetooth_sco"
        AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "builtin_earpiece"
        AudioDeviceInfo.TYPE_BUILTIN_MIC -> "builtin_mic"
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "builtin_speaker"
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE -> "builtin_speaker_safe"
        AudioDeviceInfo.TYPE_BUS -> "bus"
        AudioDeviceInfo.TYPE_DOCK -> "dock"
        AudioDeviceInfo.TYPE_FM -> "fm"
        AudioDeviceInfo.TYPE_FM_TUNER -> "fm_tuner"
        AudioDeviceInfo.TYPE_HDMI -> "hdmi"
        AudioDeviceInfo.TYPE_HDMI_ARC -> "hdmi_arc"
        AudioDeviceInfo.TYPE_HDMI_EARC -> "hdmi_earc"
        AudioDeviceInfo.TYPE_HEARING_AID -> "hearing_aid"
        AudioDeviceInfo.TYPE_IP -> "ip"
        AudioDeviceInfo.TYPE_LINE_ANALOG -> "line_analog"
        AudioDeviceInfo.TYPE_LINE_DIGITAL -> "line_digital"
        AudioDeviceInfo.TYPE_REMOTE_SUBMIX -> "remote_submix"
        AudioDeviceInfo.TYPE_TELEPHONY -> "telephony"
        AudioDeviceInfo.TYPE_TV_TUNER -> "tv_tuner"
        AudioDeviceInfo.TYPE_UNKNOWN -> "unknown"
        AudioDeviceInfo.TYPE_USB_ACCESSORY -> "usb_accessory"
        AudioDeviceInfo.TYPE_USB_DEVICE -> "usb_device"
        AudioDeviceInfo.TYPE_USB_HEADSET -> "usb_headset"
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "wired_headphones"
        AudioDeviceInfo.TYPE_WIRED_HEADSET -> "wired_headset"
        else -> "unmapped_$type"
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun AudioPlaybackConfiguration.toLogString(): String {
        val properties = mutableMapOf<String, String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            @Suppress("DEPRECATION")
            audioDeviceInfo?.let { properties["AudioDeviceInfo"] = it.toLogString() }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            properties["audioAttributes"] = audioAttributes.toString()
        }
        // This.toString includes "AudioRecordingConfiguration" internal values
        return "$this $properties"
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun AudioRecordingConfiguration.toLogString(): String {
        val properties = mutableMapOf<String, String>()
        audioDevice?.let { properties["AudioDeviceInfo"] = it.toLogString() }
        properties["audioSource"] = when (val value = clientAudioSource) {
            MediaRecorder.AudioSource.DEFAULT -> "DEFAULT"
            MediaRecorder.AudioSource.MIC -> "MIC"
            MediaRecorder.AudioSource.VOICE_UPLINK -> "VOICE_UPLINK"
            MediaRecorder.AudioSource.VOICE_DOWNLINK -> "VOICE_DOWNLINK"
            MediaRecorder.AudioSource.VOICE_CALL -> "VOICE_CALL"
            MediaRecorder.AudioSource.CAMCORDER -> "CAMCORDER"
            MediaRecorder.AudioSource.VOICE_RECOGNITION -> "VOICE_RECOGNITION"
            MediaRecorder.AudioSource.VOICE_COMMUNICATION -> "VOICE_COMMUNICATION"
            MediaRecorder.AudioSource.UNPROCESSED -> "UNPROCESSED"
            MediaRecorder.AudioSource.VOICE_PERFORMANCE -> "VOICE_PERFORMANCE"
            else -> "unmapped_$value"
        }
        properties["audioSessionId"] = clientAudioSessionId.toString()
        properties["hardwareFormat"] = format.toString()
        properties["applicationFormat"] = clientFormat.toString()
        // Check the difference because sampling can impact audio performance.
        properties["appHardwareSampleRateDiff"] =
            (format.sampleRate - clientFormat.sampleRate).toString()
        return properties.removeEmptyValues().toString()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun MicrophoneInfo.toLogString(): String {
        val properties = mutableMapOf<String, String>()
        properties["id"] = id.toString()
        properties["description"] = description
        properties["type"] = audioDeviceInfoTypeString(type)
        properties["address"] = address
        properties["deviceLocation"] = when (val value = location) {
            MicrophoneInfo.LOCATION_UNKNOWN -> "unknown"
            MicrophoneInfo.LOCATION_MAINBODY -> "mainbody"
            MicrophoneInfo.LOCATION_MAINBODY_MOVABLE -> "mainbody_movable"
            MicrophoneInfo.LOCATION_PERIPHERAL -> "peripheral"
            else -> "unmapped_$value"
        }
        properties["groupId"] = group.toString()
        properties["groupIndex"] = indexInTheGroup.toString()
        properties["position"] = with(position) { "(x=$x,y=$y,z=$z)" }
        properties["orientation"] = with(orientation) { "(x=$x,y=$y,z=$z)" }
        properties["hasFrequencyResponse"] = frequencyResponse.isNotEmpty().toString()
        properties["channelMapping"] = channelMapping.joinToString {
            val value = when (it.second) {
                MicrophoneInfo.CHANNEL_MAPPING_PROCESSED -> "processed"
                MicrophoneInfo.CHANNEL_MAPPING_DIRECT -> "direct"
                else -> "unmapped_${it.second}"
            }
            "(index=${it.first},value=$value)"
        }
        properties["sensitivity"] = sensitivity.let {
            if (it == MicrophoneInfo.SENSITIVITY_UNKNOWN) "unknown" else it.toString()
        }
        properties["soundPressureLevel"] = run {
            val minSplString = minSpl.let {
                if (it == MicrophoneInfo.SPL_UNKNOWN) "unknown" else it.toString()
            }
            val maxSplString = maxSpl.let {
                if (it == MicrophoneInfo.SPL_UNKNOWN) "unknown" else it.toString()
            }
            "[$minSplString,$maxSplString]"
        }
        properties["directionality"] = when (val value = directionality) {
            MicrophoneInfo.DIRECTIONALITY_UNKNOWN -> "unknown"
            MicrophoneInfo.DIRECTIONALITY_OMNI -> "omni"
            MicrophoneInfo.DIRECTIONALITY_BI_DIRECTIONAL -> "bi_directional"
            MicrophoneInfo.DIRECTIONALITY_CARDIOID -> "cardioid"
            MicrophoneInfo.DIRECTIONALITY_HYPER_CARDIOID -> "hyper_cardioid"
            MicrophoneInfo.DIRECTIONALITY_SUPER_CARDIOID -> "super_cardioid"
            else -> "unmapped_$value"
        }
        properties["indexInGroup"] = directionality.toString()

        return properties.removeEmptyValues().toString()
    }

    private fun MutableMap<String, String>.putMetadata(
        retriever: MediaMetadataRetriever,
        nameKey: String,
        metadataKey: Int,
    ) {
        retriever.extractMetadata(metadataKey)?.let { value ->
            put(nameKey, value)
        }
    }

    private fun MutableMap<String, String>.removeEmptyValues() = apply {
        entries.removeAll { it.value.isEmpty() }
    }
}
