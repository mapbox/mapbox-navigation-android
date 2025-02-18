package com.mapbox.navigation.mapgpt.core.textplayer

sealed class VoiceAnnouncement(
    open val utteranceId: String,
    open val mediaCacheId: String,
) {

    data class Local constructor(
        override val utteranceId: String,
        override val mediaCacheId: String,
        val text: String,
        val progress: VoiceProgress.Index?,
    ) : VoiceAnnouncement(utteranceId, mediaCacheId)

    data class Remote constructor(
        override val utteranceId: String,
        override val mediaCacheId: String,
        val text: String,
        val filePath: String,
        val progress: VoiceProgress.Time?,
    ) : VoiceAnnouncement(utteranceId, mediaCacheId)
}
