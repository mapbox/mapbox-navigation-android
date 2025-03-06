package com.mapbox.navigation.copilot

import androidx.annotation.Keep
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

@Keep
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal data class NavFeedbackSubmitted(
    val feedbackId: String,
    val type: String,
    val subtype: Set<String>,
    val location: HistoryPoint,
    val description: String,
) : EventDTO
