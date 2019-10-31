package com.mapbox.navigation.base.model.voice

data class VoiceInstruction(
    val ssmlAnnouncement: String,
    val announcement: String,
    val remainingStepDistance: Float,
    val index: Int
)
