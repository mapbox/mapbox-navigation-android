package com.mapbox.navigation.ui.voice.api

import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement

internal data class PlayCallback(
    val announcement: SpeechAnnouncement,
    val consumer: MapboxNavigationConsumer<SpeechAnnouncement>
)
