package com.mapbox.navigation.mapgpt.core.textplayer

import com.mapbox.navigation.mapgpt.core.performance.DashMeasure
import com.mapbox.navigation.mapgpt.core.performance.SharedPerformance
import com.mapbox.navigation.mapgpt.core.performance.TraceName
import com.mapbox.navigation.mapgpt.core.textplayer.SpeechPlayerPerformance.MEASURE_TTS_DOWNLOAD

object SpeechPlayerPerformance {
    const val MEASURE_TTS_DOWNLOAD = "text_to_speech_download"

    fun enableAll() {
        val prefixes = setOf(
            MEASURE_TTS_DOWNLOAD,
            TraceName.TEXT_TO_SPEECH_STARTED.snakeCase,
        )
        SharedPerformance.enableAll(prefixes)
    }

    fun remove(announcement: Announcement) = with(SharedPerformance) {
        complete("${MEASURE_TTS_DOWNLOAD}_${announcement.mediaCacheId}")
    }
}

fun Announcement.measureDownload(): DashMeasure {
    return SharedPerformance.measure("${MEASURE_TTS_DOWNLOAD}_$mediaCacheId")
}
