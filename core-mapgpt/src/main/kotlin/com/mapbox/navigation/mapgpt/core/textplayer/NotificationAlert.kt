package com.mapbox.navigation.mapgpt.core.textplayer

sealed class NotificationAlert(
    open val soundFile: String,
) : AudioMixer.Clip {

    data class RoadCamera(
        override val soundFile: String = ROAD_CAMERA_DEFAULT_SOUND,
    ) : NotificationAlert(soundFile) {

        override val track: Int = AudioMixer.TRACK_PRIORITY

        override fun toString() = "RoadCamera(${super.toString()}"

        private companion object {
            private const val ROAD_CAMERA_DEFAULT_SOUND = "warning_road_camera.mp3"
        }
    }

    data class Incident(
        override val soundFile: String = INCIDENT_DEFAULT_SOUND,
    ) : NotificationAlert(soundFile) {

        override val track: Int = AudioMixer.TRACK_PRIORITY

        override fun toString() = "Incident(${super.toString()}"

        private companion object {
            private const val INCIDENT_DEFAULT_SOUND = "warning_road_camera.mp3"
        }
    }
}
