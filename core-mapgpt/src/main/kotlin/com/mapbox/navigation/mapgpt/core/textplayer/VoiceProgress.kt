package com.mapbox.navigation.mapgpt.core.textplayer

interface VoiceProgress {
    object None : VoiceProgress {
        override fun toString() = "None"
    }
    data class Index(val position: Int) : VoiceProgress
    data class Time(val milliseconds: Int) : VoiceProgress
}
