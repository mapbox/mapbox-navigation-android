package com.mapbox.navigation.voice.api

import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.voice.model.SpeechAnnouncement

internal data class PlayCallback(
    val announcement: SpeechAnnouncement,
    val consumer: MapboxNavigationConsumer<SpeechAnnouncement>,
)
