package com.mapbox.navigation.ui.base.model.voice

import java.io.File

/**
 * @property announcement normal announcement text retrieved from [VoiceInstructions].
 * @property ssmlAnnouncement SSML announcement text retrieved from [VoiceInstructions].
 * @property file synthesized speech mp3.
 */
data class Announcement(
    val announcement: String,
    val ssmlAnnouncement: String?,
    val file: File?
)
