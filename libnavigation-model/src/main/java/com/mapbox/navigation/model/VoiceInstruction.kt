package com.mapbox.navigation.model

data class VoiceInstruction(
    val ssmlAnnouncement: String,
    val announcement: String,
    val remainingStepDistance: Float,
    val index: Int
)
