package com.mapbox.navigation.voice.model

import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.base.options.DeviceType
import java.io.File

/**
 * @property announcement normal announcement text retrieved from [VoiceInstructions].
 * @property ssmlAnnouncement SSML announcement text retrieved from [VoiceInstructions].
 * @property file synthesized speech mp3.
 */
class SpeechAnnouncement private constructor(
    val announcement: String,
    val ssmlAnnouncement: String?,
    val file: File?,
) {

    /**
     * @return builder matching the one used to create this instance
     */
    fun toBuilder(): Builder = Builder(announcement).apply {
        ssmlAnnouncement(ssmlAnnouncement)
        file(file)
    }

    /**
     * Build a new [SpeechAnnouncement]
     * @param announcement normal announcement text retrieved from [VoiceInstructions].
     */
    class Builder(private val announcement: String) {
        private var ssmlAnnouncement: String? = null
        private var file: File? = null

        /**
         * SSML announcement text retrieved from [VoiceInstructions]
         */
        fun ssmlAnnouncement(ssmlAnnouncement: String?): Builder = apply {
            this.ssmlAnnouncement = ssmlAnnouncement
        }

        /**
         * Change the [DeviceType]
         */
        fun file(file: File?): Builder = apply { this.file = file }

        /**
         * Build the [SpeechAnnouncement]
         */
        fun build() = SpeechAnnouncement(
            announcement = announcement,
            ssmlAnnouncement = ssmlAnnouncement,
            file = file,
        )
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SpeechAnnouncement

        if (announcement != other.announcement) return false
        if (ssmlAnnouncement != other.ssmlAnnouncement) return false
        if (file != other.file) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = announcement.hashCode()
        result = 31 * result + ssmlAnnouncement.hashCode()
        result = 31 * result + file.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "SpeechAnnouncement(announcement='$announcement', " +
            "ssmlAnnouncement='$ssmlAnnouncement', " +
            "file=$file)"
    }
}
